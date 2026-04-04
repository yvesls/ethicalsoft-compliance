import { QuestionnaireResponseStatus } from '../../enums/questionnaire-response-status.enum';
import { TimelineStatus } from '../../enums/timeline-status.enum';

export interface QuestionnaireRespondentStatus {
  representativeId: number;
  name: string;
  email: string;
  status: QuestionnaireResponseStatus;
  completedAt?: Date | null;
}

export interface ProjectQuestionnaireSummary {
  id: number;
  name: string;
  applicationStartDate?: Date | null;
  applicationEndDate?: Date | null;
  stageName?: string | null;
  iterationName?: string | null;
  totalRespondents: number;
  respondedRespondents: number;
  pendingRespondents: number;
  lastResponseAt?: Date | null;
  progressStatus: QuestionnaireResponseStatus;
  status: TimelineStatus | string;
  respondents: QuestionnaireRespondentStatus[];
}

export interface ProjectQuestionnaireFilters {
  name?: string | null;
  stage?: string | null;
  iteration?: string | null;
  status?: QuestionnaireResponseStatus | null;
  page: number;
  size: number;
}

export interface QuestionnaireReminderRequest {
  emails: string[];
}

export interface RescheduleQuestionnairePayload {
  newApplicationStartDate: string;
  newApplicationEndDate: string;
}

export interface RescheduleQuestionnaireResponse {
  questionnaireId: number;
  questionnaireName: string;
  oldStartDate: string;
  oldEndDate: string;
  newStartDate: string;
  newEndDate: string;
  newStatus: string;
  stageOrIterationName: string | null;
  stageOrIterationNewStart: string | null;
  stageOrIterationNewEnd: string | null;
  projectDeadlineExceeded: boolean;
  projectDeadlineWarning: string | null;
  notificationsSent: string[];
}
