import { QuestionnaireResponseStatus } from '../../enums/questionnaire-response-status.enum';
import { TimelineStatus } from '../../enums/timeline-status.enum';

export interface QuestionnaireRespondentStatus {
  representativeId: number;
  name: string;
  email: string;
  status: QuestionnaireResponseStatus;
  completedAt?: string | Date | null;
}

export interface ProjectQuestionnaireSummary {
  id: number;
  name: string;
  applicationStartDate?: string | Date | null;
  applicationEndDate?: string | Date | null;
  stageName?: string | null;
  iterationName?: string | null;
  totalRespondents: number;
  respondedRespondents: number;
  pendingRespondents: number;
  lastResponseAt?: string | Date | null;
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
