import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AiTokenService } from '../../../../core/ai/ai-token.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { ModalService } from '../../../../core/services/modal.service';
import { signal } from '@angular/core';

@Component({
  selector: 'app-ai-token-settings',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslateModule],
  templateUrl: './ai-token-settings.component.html',
  styleUrl: './ai-token-settings.component.scss',
})
export class AiTokenSettingsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly aiTokenService = inject(AiTokenService);
  private readonly notificationService = inject(NotificationService);
  private readonly modalService = inject(ModalService);
  private readonly translate = inject(TranslateService);

  form!: FormGroup;
  status$ = this.aiTokenService.status$;
  showToken = signal(false);
  isLoading = signal(false);
  isSaving = signal(false);
  isRemoving = signal(false);

  ngOnInit(): void {
    this.initializeForm();
  }

  private initializeForm(): void {
    this.form = this.fb.group({
      token: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(512)]],
    });
  }

  toggleShowToken(): void {
    this.showToken.set(!this.showToken());
  }

  saveToken(): void {
    if (this.form.invalid) {
      this.notificationService.showWarning(
        this.translate.instant('ai.token.errors.empty')
      );
      return;
    }

    this.isSaving.set(true);
    const token = this.form.get('token')?.value.trim();

    this.aiTokenService.updateToken(token).subscribe({
      next: () => {
        this.notificationService.showSuccess(
          this.translate.instant('ai.token.saved')
        );
        this.form.reset();
        this.isSaving.set(false);
      },
      error: (error) => {
        const message =
          error.error?.translatedMessage ||
          error.error?.message ||
          this.translate.instant('ai.token.errors.invalid');
        this.notificationService.showError(message);
        this.isSaving.set(false);
      },
    });
  }

  removeToken(): void {
    this.modalService.openConfirm({
      title: this.translate.instant('ai.token.missing.title'),
      message: this.translate.instant('ai.token.missing.body'),
      okLabel: this.translate.instant('common.confirm'),
      cancelLabel: this.translate.instant('ai.token.missing.cancel'),
      onConfirm: () => {
        this.isRemoving.set(true);
        this.aiTokenService.deleteToken().subscribe({
          next: () => {
            this.notificationService.showSuccess(
              this.translate.instant('ai.token.removed')
            );
            this.form.reset();
            this.isRemoving.set(false);
          },
          error: (error) => {
            const message =
              error.error?.translatedMessage ||
              error.error?.message ||
              this.translate.instant('common.error');
            this.notificationService.showError(message);
            this.isRemoving.set(false);
          },
        });
      },
    });
  }

  get tokenControl() {
    return this.form.get('token');
  }

  get isFormValid(): boolean {
    return this.form.valid && this.form.dirty;
  }
}
