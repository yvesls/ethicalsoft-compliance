import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { take } from 'rxjs';

import { QuestionnaireResponseService } from '../../services/questionnaire-response.service';
import { QuestionnaireAnswerCacheService } from '../../services/questionnaire-answer-cache.service';
import { QuestionnaireResponseStatus } from '../../../../shared/enums/questionnaire-response-status.enum';
import {
  QuestionnaireAnswerDocument,
  QuestionnaireAttachmentLink,
  QuestionnaireResponsePayload,
  QuestionnaireResponseSubmission,
} from '../../../../shared/interfaces/questionnaire/questionnaire-response.interface';
import { AttachmentModalValue, QuestionnaireAttachmentModalComponent } from '../../components/questionnaire-attachment-modal/questionnaire-attachment-modal.component';
import { QuestionnaireRespondentStatus } from '../../../../shared/interfaces/project/project-questionnaire.interface';
import { ModalService } from '../../../../core/services/modal.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { ProjectContextService } from '../../../../core/services/project-context.service';
import { AuthenticationService, UserInterface } from '../../../../core/services/authentication.service';
import { RoleEnum } from '../../../../shared/enums/role.enum';
import { DraftCacheService } from '../../../../core/services/draft-cache.service';
import { SessionExpirationService } from '../../../../core/services/session-expiration.service';
import { InfoExplainerComponent } from '../../../../shared/components/info-explainer/info-explainer.component';

interface QuestionnaireResponseState {
  status: 'loading' | 'loaded' | 'error';
  error: string | null;
  data: QuestionnaireResponsePayload | null;
}

type PageMode = 'respond' | 'view';

@Component({
  selector: 'app-questionnaire-response-page',
  standalone: true,
  imports: [CommonModule, TranslateModule, InfoExplainerComponent],
  templateUrl: './questionnaire-response-page.component.html',
  styleUrls: ['./questionnaire-response-page.component.scss'],
  providers: [QuestionnaireAnswerCacheService],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionnaireResponsePageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly responseService = inject(QuestionnaireResponseService);
  private readonly answerCache = inject(QuestionnaireAnswerCacheService);
  private readonly modalService = inject(ModalService);
  private readonly notification = inject(NotificationService);
  private readonly projectContext = inject(ProjectContextService);
  private readonly location = inject(Location);
  private readonly authService = inject(AuthenticationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly draftCacheService = inject(DraftCacheService);
  private readonly sessionExpirationService = inject(SessionExpirationService);
  private readonly translate = inject(TranslateService);

  private projectId: string | null = null;
  private questionnaireId: number | null = null;
  private requestedMode: PageMode = 'respond';
  private lastUserEmail: string | null = null;

  private readonly userRoles = signal<string[]>([]);
  private readonly currentUser = signal<UserInterface | null>(null);
  readonly isAdmin = computed(() => this.userRoles().includes(RoleEnum.ADMIN));

  readonly state = signal<QuestionnaireResponseState>({
    status: 'loading',
    error: null,
    data: null,
  });

  readonly pageMode = signal<PageMode>('respond');
  readonly isSavingDraft = signal(false);

  readonly questionnaire = computed(() => this.state().data?.questionnaire ?? null);
  readonly answers = computed(() => this.state().data?.response.answers ?? []);
  readonly totalQuestions = computed(() => this.answers().length);
  readonly estimatedResponseTime = computed(() => this.formatEstimatedResponseTime(this.totalQuestions()));
  readonly responseDeadline = computed(() => this.formatResponseDeadline());
  readonly answeredCount = computed(() =>
    this.answers().filter((answer) => answer.response !== null).length
  );

  readonly progressPercentage = computed(() => {
    if (!this.answers().length) {
      return 0;
    }
    return Math.round((this.answeredCount() / this.answers().length) * 100);
  });

  ngOnInit(): void {
    this.listenToAuthState();
    this.listenToRoute();

    this.sessionExpirationService.registerDraftSaver(() => this.saveDraftLocally());
    this.destroyRef.onDestroy(() => this.sessionExpirationService.unregisterDraftSaver());
  }

  onNavigateBack(): void {
    const fallback = this.projectId ? `/projects/${this.projectId}` : '/projects';
    const hasBrowserHistory =
      globalThis.window !== undefined && globalThis.window.history.length > 1;

    if (hasBrowserHistory) {
      this.location.back();
      return;
    }

    this.router.navigateByUrl(fallback);
  }

  onSelectAnswer(answer: QuestionnaireAnswerDocument, value: boolean): void {
    if (this.pageMode() === 'view') {
      return;
    }

    const partial: Partial<QuestionnaireAnswerDocument> = {
      response: value,
      justification: value ? null : answer.justification ?? null,
      evidence: value ? answer.evidence ?? null : null,
    };

    this.updateAnswer(answer.questionId, partial);
  }

  openAttachmentModal(answer: QuestionnaireAnswerDocument): void {
    if (answer.response === null) {
      this.notification.showWarning(this.translate.instant('notifications.response.select_answer_first'));
      return;
    }

    const initialValue: AttachmentModalValue = {
      note: (answer.response ? answer.evidence?.descricao : answer.justification?.descricao) ?? '',
      attachments: answer.attachments ?? [],
    };

    this.modalService.open(QuestionnaireAttachmentModalComponent, 'medium-card', {
      mode: answer.response ? 'positive' : 'negative',
      initialValue,
      onSave: (value: AttachmentModalValue) =>
        this.updateAnswer(answer.questionId, {
          justification: answer.response ? null : this.buildPrimaryNoteLink(value.note, value.attachments),
          evidence: answer.response ? this.buildPrimaryNoteLink(value.note, value.attachments) : null,
          attachments: value.attachments,
        }),
    });
  }

  submitResponses(): void {
    if (!this.canSubmit()) {
      return;
    }

    const currentAnswers = this.answers();

    const submission: QuestionnaireResponseSubmission = {
      status: QuestionnaireResponseStatus.Completed,
      answers: currentAnswers,
    };

    if (!this.projectId || this.questionnaireId === null) {
      this.notification.showError(this.translate.instant('notifications.response.invalid_ids'));
      return;
    }

    this.responseService
      .submitResponses(
        this.projectId,
        this.questionnaireId,
        submission,
        this.currentUser()?.email,
        this.isAdmin() ? this.getCurrentRespondent()?.representativeId : undefined
      )
      .pipe(take(1))
      .subscribe({
        next: () => {
          const submittedIds = currentAnswers.map((a) => a.questionId);
          this.answerCache.clearSubmitted(submittedIds);

          this.notification.showSuccess(this.translate.instant('notifications.response.submit_success'));
          this.loadResponse();
        },
        error: (msg) => this.notification.showError(msg),
      });
  }

  saveDraftResponses(): void {
    if (this.isSavingDraft() || this.pageMode() !== 'respond') {
      return;
    }

    const currentAnswers = this.answers();
    if (!this.projectId || this.questionnaireId === null) {
      this.notification.showError(this.translate.instant('notifications.response.draft_no_ids'));
      return;
    }

    const answersWithContent = currentAnswers.filter(
      (a) => a.response !== null || a.justification?.descricao || a.evidence?.descricao
    );

    if (!answersWithContent.length) {
      this.notification.showWarning(this.translate.instant('notifications.response.draft_empty'));
      return;
    }

    const submission: QuestionnaireResponseSubmission = {
      status: QuestionnaireResponseStatus.InProgress,
      answers: currentAnswers,
    };

    this.isSavingDraft.set(true);

    this.saveDraftLocally();

    this.responseService
      .submitResponses(
        this.projectId,
        this.questionnaireId,
        submission,
        this.currentUser()?.email,
        this.isAdmin() ? this.getCurrentRespondent()?.representativeId : undefined,
        true
      )
      .pipe(take(1))
      .subscribe({
        next: () => {
          this.isSavingDraft.set(false);
          const submittedIds = currentAnswers.map((a) => a.questionId);
          this.answerCache.clearSubmitted(submittedIds);

          if (this.projectId && this.questionnaireId !== null) {
            const draftKey = this.draftCacheService.responseDraftKey(
              this.projectId,
              this.questionnaireId,
              this.currentUser()?.email
            );
            this.draftCacheService.remove(draftKey);
          }

          this.notification.showSuccess(this.translate.instant('notifications.response.draft_saved'));
        },
        error: (msg) => {
          this.isSavingDraft.set(false);
          this.notification.showError(msg);
        },
      });
  }

  private saveDraftLocally(): void {
    if (!this.projectId || this.questionnaireId === null) return;

    const currentAnswers = this.answers();
    if (!currentAnswers.length) return;

    const draftKey = this.draftCacheService.responseDraftKey(
      this.projectId,
      this.questionnaireId,
      this.currentUser()?.email
    );

    this.draftCacheService.save(
      draftKey,
      { answers: currentAnswers },
      'questionnaire-response',
      {
        projectId: this.projectId,
        questionnaireId: this.questionnaireId,
        email: this.currentUser()?.email,
      }
    );
  }

  getQuestionOrdinal(index: number): number {
    return index + 1;
  }

  getRespondentStatusLabel(): string {
    const respondent = this.getCurrentRespondent();
    if (!respondent) {
      return this.isAdmin()
        ? this.translate.instant('questionnaire.respondent_status.admin')
        : this.translate.instant('questionnaire.respondent_status.not_associated');
    }

    switch (respondent.status) {
      case QuestionnaireResponseStatus.Completed:
        return this.translate.instant('questionnaire.respondent_status.completed');
      case QuestionnaireResponseStatus.InProgress:
        return this.translate.instant('questionnaire.respondent_status.in_progress');
      default:
        return this.translate.instant('questionnaire.respondent_status.pending');
    }
  }

  canSubmit(): boolean {
    if (this.pageMode() !== 'respond') {
      return false;
    }

    if (!this.answers().length) {
      return false;
    }

    if (this.state().data?.completed) {
      return false;
    }

    return this.answers().every((answer) => {
      if (answer.response === null) {
        return false;
      }

      if (answer.response === false) {
        return this.hasRequiredJustification(answer);
      }

      if (answer.response === true) {
        return this.hasRequiredEvidence(answer);
      }

      return true;
    });
  }

  private hasRequiredJustification(answer: QuestionnaireAnswerDocument): boolean {
    const descricao = answer.justification?.descricao?.trim();
    return !!descricao;
  }

  private hasRequiredEvidence(answer: QuestionnaireAnswerDocument): boolean {
    const descricao = answer.evidence?.descricao?.trim();
    return !!descricao;
  }

  get referenceLabel(): string {
    const questionnaire = this.questionnaire();
    if (!questionnaire) {
      return '';
    }

    if (questionnaire.stageName) {
      return `${this.translate.instant('projects.detail.stage_label')}: ${questionnaire.stageName}`;
    }

    if (questionnaire.iterationName) {
      return `${this.translate.instant('projects.detail.iteration_label')}: ${questionnaire.iterationName}`;
    }

    return '';
  }

  get applicationRange(): string {
    const questionnaire = this.questionnaire();
    if (!questionnaire) {
      return '';
    }

    const start = questionnaire.applicationStartDate
      ? new Date(questionnaire.applicationStartDate).toLocaleDateString('pt-BR')
      : this.translate.instant('questionnaire.response.no_start_date');
    const end = questionnaire.applicationEndDate
      ? new Date(questionnaire.applicationEndDate).toLocaleDateString('pt-BR')
      : this.translate.instant('questionnaire.response.no_end_date');

    return `${start} - ${end}`;
  }

  get isReadOnly(): boolean {
    return this.pageMode() === 'view';
  }

  private formatEstimatedResponseTime(totalQuestions: number): string {
    const totalMinutes = totalQuestions * 3;

    if (totalMinutes < 60) {
      return this.translate.instant('questionnaire.response.estimated_time_minutes', {
        value: totalMinutes,
      });
    }

    const hours = Math.floor(totalMinutes / 60);
    const minutes = totalMinutes % 60;

    if (minutes === 0) {
      return this.translate.instant('questionnaire.response.estimated_time_hours', {
        value: hours,
      });
    }

    return this.translate.instant('questionnaire.response.estimated_time_hours_minutes', {
      hours,
      minutes,
    });
  }

  private formatResponseDeadline(): string {
    const endDate = this.questionnaire()?.applicationEndDate;

    if (!endDate) {
      return this.translate.instant('questionnaire.response.no_end_date');
    }

    return new Date(endDate).toLocaleDateString('pt-BR');
  }

  private listenToAuthState(): void {
    this.authService.userRoles$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((roles) => this.userRoles.set(roles ?? []));

    this.authService.currentUser$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((user) => {
        this.currentUser.set(user);

        const currentEmail = user?.email?.trim().toLowerCase() ?? null;
        const emailChanged = currentEmail !== this.lastUserEmail;
        this.lastUserEmail = currentEmail;

        if (
          emailChanged &&
          !!currentEmail &&
          !!this.projectId &&
          this.questionnaireId !== null
        ) {
          this.loadResponse();
        }
      });
  }

  private listenToRoute(): void {
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        this.projectId = params.get('projectId');
        const questionnaireId = params.get('questionnaireId');
        this.questionnaireId = questionnaireId ? Number(questionnaireId) : null;
        const modeFromQuery = this.route.snapshot.queryParamMap.get('mode');
        this.requestedMode = modeFromQuery === 'view' ? 'view' : 'respond';

        if (!this.projectId || this.questionnaireId === null || Number.isNaN(this.questionnaireId)) {
          this.notification.showError(this.translate.instant('notifications.response.invalid_questionnaire_id'));
          this.onNavigateBack();
          return;
        }

        this.projectContext.setCurrentProjectId(this.projectId);
        this.answerCache.reset();
        this.loadResponse();
      });
  }

  loadResponse(): void {
    if (!this.projectId || this.questionnaireId === null) {
      return;
    }

    this.state.update((current) => ({ ...current, status: 'loading', error: null }));

    this.responseService
      .loadResponses(
        this.projectId,
        this.questionnaireId,
        this.isAdmin() ? this.currentUser()?.email : undefined
      )
      .pipe(take(1))
      .subscribe({
        next: (payload: QuestionnaireResponsePayload) => {
          const resolvedMode = this.resolvePageMode(payload);
          this.pageMode.set(resolvedMode);

          const mergedAnswers = this.answerCache.applyCache(payload.response.answers);
          const mergedPayload: QuestionnaireResponsePayload = {
            ...payload,
            response: { ...payload.response, answers: mergedAnswers },
          };

          this.state.set({ status: 'loaded', error: null, data: mergedPayload });
        },
        error: () => {
          this.state.set({ status: 'error', error: this.translate.instant('notifications.response.load_error'), data: null });
        },
      });
  }

  private resolvePageMode(payload: QuestionnaireResponsePayload): PageMode {
    const respondent = this.getCurrentRespondent(payload);
    if (!respondent) {
      this.notification.showWarning(this.translate.instant('notifications.response.not_associated'));
      this.onNavigateBack();
      return 'view';
    }

    if (respondent.status === QuestionnaireResponseStatus.Completed) {
      return 'view';
    }

    return this.requestedMode;
  }

  private getCurrentRespondent(payload?: QuestionnaireResponsePayload): QuestionnaireRespondentStatus | null {
    const source = payload ?? this.state().data;
    if (!source?.questionnaire.respondents?.length) {
      return null;
    }

    const email = this.currentUser()?.email?.toLowerCase() ?? '';
    return (
      source.questionnaire.respondents.find((respondent) =>
        respondent.email?.toLowerCase() === email
      ) ?? null
    );
  }

  private updateAnswer(questionId: number, partial: Partial<QuestionnaireAnswerDocument>): void {
    this.state.update((current) => {
      if (!current.data) {
        return current;
      }

      const answers = current.data.response.answers.map((answer) =>
        answer.questionId === questionId
          ? {
              ...answer,
              ...partial,
            }
          : answer
      );

      const status = answers.every((answer) => answer.response !== null)
        ? QuestionnaireResponseStatus.Completed
        : QuestionnaireResponseStatus.InProgress;

      const updatedAnswer = answers.find((a) => a.questionId === questionId);
      if (updatedAnswer) {
        this.answerCache.markDirty(questionId, updatedAnswer);
      }

      return {
        ...current,
        data: {
          ...current.data,
          response: {
            ...current.data.response,
            status,
            answers,
          },
        },
      };
    });
  }

  private buildPrimaryNoteLink(
    note: string,
    attachments: QuestionnaireAttachmentLink[] = []
  ): QuestionnaireAttachmentLink | null {
    const descricao = note?.trim();
    if (!descricao) {
      return null;
    }

    const primaryUrl =
      attachments
        .map((attachment) => attachment?.url?.trim() ?? '')
        .find((url) => !!url) ?? '';

    return {
      descricao,
      url: primaryUrl,
    };
  }
}
