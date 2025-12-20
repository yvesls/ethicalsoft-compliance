import { TimelineStatus } from '../../enums/timeline-status.enum';

export interface QuestionnaireRawResponse {
  id: number;
  name: string;
  iteration: string | null;
  weight: number;
  applicationStartDate: string | null;
  applicationEndDate: string | null;
  projectId: number;
  stageId: number | null;
  iterationId: number | null;
  status: TimelineStatus | string | null;
}

export interface QuestionnaireQuestionResponse {
  id: string | number;
  text: string;
  stageIds: number[];
  stageNames: string[];
  roleIds: number[];
  order: number;
}

export interface QuestionSearchFilter {
  questionText?: string | null;
  roleName?: string | null;
}
