import { QuestionnaireResponseStatus } from '../../enums/questionnaire-response-status.enum';
import { ProjectQuestionnaireSummary } from '../project/project-questionnaire.interface';

export interface QuestionnaireQuestion {
  id: number;
  text: string;
  stageIds: number[];
  stageNames: string[];
  roleIds: number[];
  order?: number;
}

export interface QuestionnaireAnswerDocument {
  questionId: number;
  questionText: string;
  stageIds: number[];
  roleIds: number[];
  response: boolean | null;
  justification?: QuestionnaireAttachmentLink | null;
  evidence?: QuestionnaireAttachmentLink | null;
  attachments: QuestionnaireAttachmentLink[];
  pageNumber?: number;
}

export interface QuestionnaireAnswerRequest {
  questionId: number;
  response: boolean | null;
  justification?: QuestionnaireAttachmentLink | null;
  evidence?: QuestionnaireAttachmentLink | null;
  attachments: QuestionnaireAttachmentLink[];
}

export interface QuestionnaireAnswerResponse {
  questionId: number;
  response: boolean | null;
  justification?: QuestionnaireAttachmentLink | string | null;
  evidence?: QuestionnaireAttachmentLink | string | null;
  attachments: QuestionnaireAttachmentLink[] | string[];
  pageNumber: number;
}

export interface QuestionnaireAnswerPageResponse {
  pageNumber: number;
  pageSize: number;
  totalPages: number;
  completed: boolean;
  answers: QuestionnaireAnswerResponse[];
}

export interface QuestionnaireAnswerPageRequest {
  representativeId?: number | null;
  pageNumber: number;
  pageSize: number;
  answers: QuestionnaireAnswerRequest[];
}

export interface QuestionnaireResponseSummary {
  representativeId: number;
  status: QuestionnaireResponseStatus;
  submissionDate: string | Date | null;
}

export interface QuestionnaireResponseDocument {
  id?: string;
  projectId: number;
  questionnaireId: number;
  representativeId?: number | null;
  representativeEmail?: string | null;
  stageId?: number | null;
  status: QuestionnaireResponseStatus;
  submissionDate?: string | Date | null;
  answers: QuestionnaireAnswerDocument[];
}

export interface QuestionnaireResponsePagination {
  pageNumber: number;
  pageSize: number;
  totalPages: number;
  totalElements: number;
  completed: boolean;
}

export interface QuestionnaireResponsePayload {
  questionnaire: ProjectQuestionnaireSummary;
  response: QuestionnaireResponseDocument;
  pagination: QuestionnaireResponsePagination;
}

export interface QuestionnaireResponseSubmission {
  status: QuestionnaireResponseStatus;
  answers: QuestionnaireAnswerDocument[];
}

export interface QuestionnaireAttachmentLink {
  descricao: string;
  url: string;
}
