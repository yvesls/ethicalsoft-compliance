import { Injectable, inject, OnDestroy } from '@angular/core';
import { Subject, Subscription, timer } from 'rxjs';
import { NotificationService } from './notification.service';
import { LoggerService } from './logger.service';
import { TranslateService } from '@ngx-translate/core';
import { AuthStore } from '../../shared/stores/auth.store';

@Injectable({ providedIn: 'root' })
export class SessionExpirationService implements OnDestroy {
  private readonly notification = inject(NotificationService);
  private readonly translate = inject(TranslateService);
  private readonly authStore = inject(AuthStore);

  private readonly WARNING_BEFORE_EXPIRY_MS = 2 * 60 * 1000;

  private readonly GRACE_PERIOD_MS = 60 * 1000;

  private warningTimerSub: Subscription | null = null;
  private graceTimerSub: Subscription | null = null;
  private isWarningVisible = false;

  private readonly _sessionExpiring$ = new Subject<void>();
  readonly sessionExpiring$ = this._sessionExpiring$.asObservable();

  private readonly _sessionExpired$ = new Subject<void>();
  readonly sessionExpired$ = this._sessionExpired$.asObservable();

  private readonly _sessionExtended$ = new Subject<void>();
  readonly sessionExtended$ = this._sessionExtended$.asObservable();

  private pendingDraftSaver: (() => Promise<void> | void) | null = null;

  private logoutHandler: (() => void) | null = null;
  private sessionExtendHandler: ((callback: (expirationTime: number) => void) => void) | null = null;

  ngOnDestroy(): void {
    this.cancelTimers();
    this._sessionExpiring$.complete();
    this._sessionExpired$.complete();
  }

  registerLogoutHandler(handler: () => void): void {
    this.logoutHandler = handler;
  }

  registerSessionExtendHandler(handler: (callback: (expirationTime: number) => void) => void): void {
    this.sessionExtendHandler = handler;
  }

  scheduleWarning(tokenExpirationMs: number): void {
    this.cancelTimers();

    const now = Date.now();
    const timeUntilExpiry = tokenExpirationMs - now;

    if (timeUntilExpiry <= 0) {
      LoggerService.warn('SessionExpirationService: Token já expirado.');
      return;
    }

    const warningDelay = Math.max(timeUntilExpiry - this.WARNING_BEFORE_EXPIRY_MS, 0);

    LoggerService.info(
      `SessionExpirationService: Aviso de expiração agendado para ${Math.round(warningDelay / 1000)}s.`
    );

    this.warningTimerSub = timer(warningDelay).subscribe(() => {
      this.showExpirationWarning();
    });
  }

  registerDraftSaver(saver: () => Promise<void> | void): void {
    this.pendingDraftSaver = saver;
  }

  unregisterDraftSaver(): void {
    this.pendingDraftSaver = null;
  }

  cancelWarning(): void {
    this.cancelTimers();
    if (this.isWarningVisible) {
      this.notification.closeModal();
      this.isWarningVisible = false;
    }
  }

  private showExpirationWarning(): void {
    this.isWarningVisible = true;
    this._sessionExpiring$.next();

    LoggerService.warn('SessionExpirationService: Exibindo aviso de expiração de sessão.');

    if (this.pendingDraftSaver) {
      this.notification.showConfirm(
        this.translate.instant('notifications.session.expiring_confirm'),
        () => this.onUserAcceptedSave(),
        () => this.onUserDeclinedSave()
      );
    } else {
      this.notification.showSessionExpiration(
        this.translate.instant('notifications.session.expired_redirect'),
        () => this.onUserExtendedSession(),
        () => this.onUserDeclinedExtension()
      );
    }

    this.graceTimerSub = timer(this.GRACE_PERIOD_MS).subscribe(() => {
      LoggerService.warn('SessionExpirationService: Grace period expirado. Forçando logout.');
      this.forceLogout();
    });
  }

  private async onUserAcceptedSave(): Promise<void> {
    this.cancelGraceTimer();

    try {
      if (this.pendingDraftSaver) {
        await this.pendingDraftSaver();
        this.notification.showSuccess(this.translate.instant('notifications.session.draft_saved_redirect'));
      }
    } catch (error) {
      LoggerService.error('SessionExpirationService: Erro ao salvar rascunho.', error);
      this.notification.showWarning(this.translate.instant('notifications.session.draft_save_failed'));
    }

    setTimeout(() => this.forceLogout(), 2000);
  }

  private onUserDeclinedSave(): void {
    this.cancelGraceTimer();
    this.forceLogout();
  }

  private onUserExtendedSession(): void {
    this.cancelGraceTimer();
    this.cancelWarningTimer();
    this.isWarningVisible = false;

    LoggerService.info('SessionExpirationService: Usuário estendeu a sessão.');

    if (this.sessionExtendHandler) {
      // The handler accepts a callback that will be called with the new expiration time
      this.sessionExtendHandler((newExpirationTime: number) => {
        LoggerService.info(`SessionExpirationService: Sessão estendida até ${new Date(newExpirationTime)}`);
        this.scheduleWarning(newExpirationTime);
        this._sessionExtended$.next();
      });
    } else {
      LoggerService.error('SessionExpirationService: Nenhum handler de extensão de sessão registrado!');
    }
  }

  private onUserDeclinedExtension(): void {
    this.cancelGraceTimer();
    this.forceLogout();
  }

  private forceLogout(): void {
    this.cancelTimers();
    this.isWarningVisible = false;
    this._sessionExpired$.next();

    if (this.logoutHandler) {
      this.logoutHandler();
    } else {
      LoggerService.error('SessionExpirationService: Nenhum handler de logout registrado!');
    }
  }

  private cancelTimers(): void {
    this.cancelWarningTimer();
    this.cancelGraceTimer();
  }

  private cancelWarningTimer(): void {
    if (this.warningTimerSub) {
      this.warningTimerSub.unsubscribe();
      this.warningTimerSub = null;
    }
  }

  private cancelGraceTimer(): void {
    if (this.graceTimerSub) {
      this.graceTimerSub.unsubscribe();
      this.graceTimerSub = null;
    }
  }
}
