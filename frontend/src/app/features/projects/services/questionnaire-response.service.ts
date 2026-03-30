import { Injectable, inject } from '@angular/core';
import { forkJoin, map, Observable, switchMap } from 'rxjs';
import { QuestionnaireResponseStatus } from '../../../shared/enums/questionnaire-response-status.enum';
import {
  QuestionnaireAttachmentLink,
  QuestionnaireAnswerDocument,
  QuestionnaireAnswerListResponse,
  QuestionnaireAnswerRequest,
  QuestionnaireAnswerResponse,
  QuestionnaireAnswerSubmitRequest,
  QuestionnaireQuestion,
  QuestionnaireResponseDocument,
  QuestionnaireResponsePayload,
  QuestionnaireResponseSubmission,
  QuestionnaireResponseSummary,
} from '../../../shared/interfaces/questionnaire/questionnaire-response.interface';
import { RequestService } from '../../../core/services/request.service';
import { environment } from '../../../enviroments/environments';
import { UrlParameter } from '../../../core/interfaces/url-parameter.interface';
import { Page } from '../../../shared/interfaces/pageable.interface';
import { ProjectStore } from '../../../shared/stores/project.store';
import { ProjectQuestionnaireSummary } from '../../../shared/interfaces/project/project-questionnaire.interface';

@Injectable({ providedIn: 'root' })
export class QuestionnaireResponseService {
  private readonly requestService = inject(RequestService);
  private readonly projectStore = inject(ProjectStore);

  constructor() {
    this.requestService.apiUrl = environment.apiBaseUrl;
  }

  loadResponses(
    projectId: string,
    questionnaireId: number,
    representativeEmail?: string | null
  ): Observable<QuestionnaireResponsePayload> {
    return this.projectStore.getQuestionnaireSummary(projectId, questionnaireId).pipe(
      switchMap((questionnaire) => {
        const representativeId = this.resolveRepresentativeId(questionnaire, representativeEmail);

        return forkJoin({
          questions: this.listQuestions(projectId, questionnaireId, representativeId),
          answersData: this.getAnswers(projectId, questionnaireId, representativeId),
        }).pipe(
          map(({ questions, answersData }) => {
            const answerDocuments = this.mergeQuestionsAndAnswers(
              questions,
              answersData.answers
            );

            const status = answersData.completed
              ? QuestionnaireResponseStatus.Completed
              : QuestionnaireResponseStatus.InProgress;

            const response: QuestionnaireResponseDocument = {
              projectId: Number(projectId),
              questionnaireId,
              representativeEmail: representativeEmail ?? null,
              status,
              submissionDate: null,
              answers: answerDocuments,
            };

            return {
              questionnaire,
              response,
              completed: answersData.completed,
            };
          })
        );
      })
    );
  }

  private resolveRepresentativeId(
    questionnaire: ProjectQuestionnaireSummary,
    representativeEmail?: string | null
  ): number | undefined {
    const normalizedEmail = representativeEmail?.trim().toLowerCase();
    if (!normalizedEmail) {
      return undefined;
    }

    const respondent = questionnaire.respondents?.find(
      (item) => item.email?.trim().toLowerCase() === normalizedEmail
    );

    return respondent?.representativeId;
  }

  loadAdminView(
    projectId: string,
    questionnaireId: number
  ): Observable<Pick<QuestionnaireResponsePayload, 'questionnaire'>> {
    return this.projectStore
      .getQuestionnaireSummary(projectId, questionnaireId)
      .pipe(map((questionnaire) => ({ questionnaire })));
  }

  submitResponses(
    projectId: string,
    questionnaireId: number,
    payload: QuestionnaireResponseSubmission,
    representativeEmail?: string | null,
    representativeId?: number | null,
    draft = false
  ): Observable<QuestionnaireResponseDocument> {
    const requestPayload: QuestionnaireAnswerSubmitRequest = {
      answers: payload.answers.map((answer) => this.mapToAnswerRequest(answer)),
      draft,
    };

    if (typeof representativeId === 'number' && Number.isFinite(representativeId)) {
      requestPayload.representativeId = representativeId;
    }

    return this.submitAllAnswers(projectId, questionnaireId, requestPayload).pipe(
      map((responseData) => {
        let resolvedStatus: QuestionnaireResponseStatus;
        if (draft) {
          resolvedStatus = QuestionnaireResponseStatus.InProgress;
        } else {
          resolvedStatus = responseData.completed
            ? QuestionnaireResponseStatus.Completed
            : QuestionnaireResponseStatus.InProgress;
        }

        return {
          projectId: Number(projectId),
          questionnaireId,
          representativeEmail: representativeEmail ?? null,
          status: resolvedStatus,
          submissionDate: new Date().toISOString(),
          answers: payload.answers,
        };
      })
    );
  }

  listSummaries(
    projectId: string,
    questionnaireId: number
  ): Observable<QuestionnaireResponseSummary[]> {
    return this.requestService.makeGet<QuestionnaireResponseSummary[]>(
      this.buildUrl(projectId, questionnaireId, 'responses/summaries'),
      { useAuth: true }
    );
  }

  private listQuestions(
    projectId: string,
    questionnaireId: number,
    representativeId?: number | null
  ): Observable<QuestionnaireQuestion[]> {
    const params: UrlParameter[] = [
      { key: 'page', value: 0 },
      { key: 'size', value: 10000 },
    ];

    if (representativeId) {
      params.push({ key: 'representativeId', value: representativeId });
    }

    return this.requestService.makeGet<Page<QuestionnaireQuestion>>(
      this.buildUrl(projectId, questionnaireId, 'questions'),
      { useAuth: true },
      ...params
    ).pipe(map((page) => page.content));
  }

  private getAnswers(
    projectId: string,
    questionnaireId: number,
    representativeId?: number | null
  ): Observable<QuestionnaireAnswerListResponse> {
    const params: UrlParameter[] = [];

    if (representativeId) {
      params.push({ key: 'representativeId', value: representativeId });
    }

    return this.requestService.makeGet<QuestionnaireAnswerListResponse>(
      this.buildUrl(projectId, questionnaireId, 'responses'),
      { useAuth: true },
      ...params
    );
  }

  private submitAllAnswers(
    projectId: string,
    questionnaireId: number,
    payload: QuestionnaireAnswerSubmitRequest
  ): Observable<QuestionnaireAnswerListResponse> {
    return this.requestService.makePost<QuestionnaireAnswerListResponse>(
      this.buildUrl(projectId, questionnaireId, 'responses'),
      {
        useAuth: true,
        data: payload,
      }
    );
  }

  private buildUrl(projectId: string, questionnaireId: number, suffix: string): string {
    return `api/projects/${projectId}/questionnaires/${questionnaireId}/${suffix}`;
  }

  private mergeQuestionsAndAnswers(
    questions: QuestionnaireQuestion[],
    answers: QuestionnaireAnswerResponse[]
  ): QuestionnaireAnswerDocument[] {
    const answerMap = new Map<number, QuestionnaireAnswerResponse>(
      answers.map((answer) => [answer.questionId, answer])
    );

    return questions.map((question) => {
      const answer = answerMap.get(question.id);

      return {
        questionId: question.id,
        questionText: question.text,
        stageIds: question.stageIds ?? [],
        roleIds: question.roleIds ?? [],
        response: answer?.response ?? null,
        justification: this.normalizeLink(answer?.justification),
        evidence: this.normalizeLink(answer?.evidence),
        attachments: this.normalizeAttachments(answer?.attachments),
      };
    });
  }

  private mapToAnswerRequest(answer: QuestionnaireAnswerDocument): QuestionnaireAnswerRequest {
    return {
      questionId: answer.questionId,
      response: answer.response,
      justification: this.normalizeLink(answer.justification),
      evidence: this.normalizeLink(answer.evidence),
      attachments: this.normalizeAttachments(answer.attachments),
    };
  }

  private normalizeAttachments(
    attachments?: QuestionnaireAttachmentLink[] | string[] | null
  ): QuestionnaireAttachmentLink[] {
    if (!attachments || !attachments.length) {
      return [];
    }

    return attachments
      .map((item) => this.normalizeLink(item, { allowEmptyUrl: false }))
      .filter(this.isValidLink);
  }

  private normalizeLink(
    link?: QuestionnaireAttachmentLink | string | null,
    options: { allowEmptyUrl?: boolean } = {}
  ): QuestionnaireAttachmentLink | null {
    if (!link) {
      return null;
    }

    const { allowEmptyUrl = true } = options;

    if (typeof link === 'string') {
      const value = link.trim();
      if (!value) {
        return null;
      }
      return {
        descricao: value,
        url: allowEmptyUrl ? '' : value,
      };
    }

    const descricao = (link.descricao ?? (link as unknown as { description?: string }).description ?? '')
      .toString()
      .trim();
    const url = link.url?.trim() ?? '';

    if (!descricao && !url) {
      return null;
    }

    if (!url && !allowEmptyUrl) {
      return null;
    }

    return {
      descricao: descricao || url,
      url,
    };
  }

  private isValidLink(
    link: QuestionnaireAttachmentLink | null
  ): link is QuestionnaireAttachmentLink {
    return Boolean(link && (link.descricao || link.url));
  }
}
