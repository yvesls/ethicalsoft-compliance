import { Injectable, inject } from '@angular/core';
import { forkJoin, map, Observable } from 'rxjs';
import { QuestionnaireResponseStatus } from '../../../shared/enums/questionnaire-response-status.enum';
import {
  QuestionnaireAttachmentLink,
  QuestionnaireAnswerDocument,
  QuestionnaireAnswerPageRequest,
  QuestionnaireAnswerPageResponse,
  QuestionnaireAnswerRequest,
  QuestionnaireAnswerResponse,
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

@Injectable({ providedIn: 'root' })
export class QuestionnaireResponseService {
  private readonly requestService = inject(RequestService);
  private readonly projectStore = inject(ProjectStore);
  private readonly defaultPageSize = 10;

  constructor() {
    this.requestService.apiUrl = environment.apiBaseUrl;
  }

  loadResponsePage(
    projectId: string,
    questionnaireId: number,
    page: number,
    size: number = this.defaultPageSize,
    representativeEmail?: string | null
  ): Observable<QuestionnaireResponsePayload> {
    return forkJoin({
      questionnaire: this.projectStore.getQuestionnaireSummary(projectId, questionnaireId),
      questionsPage: this.listQuestions(projectId, questionnaireId, page, size),
      answersPage: this.getAnswerPage(projectId, questionnaireId, page, size),
    }).pipe(
      map(({ questionnaire, questionsPage, answersPage }) => {
        const answerDocuments = this.mergeQuestionsAndAnswers(
          questionsPage.content,
          answersPage.answers
        );

        const status = answersPage.completed
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
          pagination: {
            pageNumber: answersPage.pageNumber,
            pageSize: answersPage.pageSize,
            totalPages: answersPage.totalPages,
            totalElements: questionsPage.totalElements,
            completed: answersPage.completed,
          },
        };
      })
    );
  }

  loadAdminView(
    projectId: string,
    questionnaireId: number
  ): Observable<Pick<QuestionnaireResponsePayload, 'questionnaire'>> {
    return this.projectStore
      .getQuestionnaireSummary(projectId, questionnaireId)
      .pipe(map((questionnaire) => ({ questionnaire })));
  }

  submitPage(
    projectId: string,
    questionnaireId: number,
    payload: QuestionnaireResponseSubmission,
    pagination: { pageNumber: number; pageSize: number },
    representativeEmail?: string | null
  ): Observable<QuestionnaireResponseDocument> {
    return this.submitAnswerPage(projectId, questionnaireId, {
      pageNumber: pagination.pageNumber,
      pageSize: pagination.pageSize,
      answers: payload.answers.map((answer) => this.mapToAnswerRequest(answer)),
    }).pipe(
      map((responsePage) => ({
        projectId: Number(projectId),
        questionnaireId,
        representativeEmail: representativeEmail ?? null,
        status: responsePage.completed
          ? QuestionnaireResponseStatus.Completed
          : QuestionnaireResponseStatus.InProgress,
        submissionDate: new Date().toISOString(),
        answers: payload.answers,
      }))
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
    page: number,
    size: number,
    representativeId?: number | null
  ): Observable<Page<QuestionnaireQuestion>> {
    const params: UrlParameter[] = [
      { key: 'page', value: page },
      { key: 'size', value: size },
    ];

    if (representativeId) {
      params.push({ key: 'representativeId', value: representativeId });
    }

    return this.requestService.makeGet<Page<QuestionnaireQuestion>>(
      this.buildUrl(projectId, questionnaireId, 'questions'),
      { useAuth: true },
      ...params
    );
  }

  private getAnswerPage(
    projectId: string,
    questionnaireId: number,
    page: number,
    size: number,
    representativeId?: number | null
  ): Observable<QuestionnaireAnswerPageResponse> {
    const params: UrlParameter[] = [
      { key: 'page', value: page },
      { key: 'size', value: size },
    ];

    if (representativeId) {
      params.push({ key: 'representativeId', value: representativeId });
    }

    return this.requestService.makeGet<QuestionnaireAnswerPageResponse>(
      this.buildUrl(projectId, questionnaireId, 'responses/page'),
      { useAuth: true },
      ...params
    );
  }

  submitAnswerPage(
    projectId: string,
    questionnaireId: number,
    payload: QuestionnaireAnswerPageRequest
  ): Observable<QuestionnaireAnswerPageResponse> {
    return this.requestService.makePost<QuestionnaireAnswerPageResponse>(
      this.buildUrl(projectId, questionnaireId, 'responses/page'),
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
        pageNumber: answer?.pageNumber ?? undefined,
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
