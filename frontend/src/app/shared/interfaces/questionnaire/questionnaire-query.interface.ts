import { TimelineStatus } from '../../enums/timeline-status.enum';
import { QuestionClassificationType } from '../../enums/question-classification-type.enum';
import { QuestionType } from '../../enums/question-type.enum';

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
  type?: QuestionType;
  classification?: QuestionClassificationType | null;
}

export interface QuestionSearchFilter {
  questionText?: string | null;
  roleName?: string | null;
}
