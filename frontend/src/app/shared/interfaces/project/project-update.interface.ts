import { ProjectType } from '../../enums/project-type.enum';

export interface UpdateStagePayload {
  id: number | null;
  name: string;
  weight: number;
  sequence?: number;
  durationDays?: number;
  applicationStartDate?: string | null;
  applicationEndDate?: string | null;
}

export interface UpdateIterationPayload {
  id: number | null;
  name: string;
  weight?: number;
  order?: number;
  applicationStartDate: string;
  applicationEndDate: string;
}

export interface UpdateQuestionPayload {
  id: number | null;
  value: string;
  roleIds: number[];
  stageNames?: string[];
}

export interface UpdateQuestionnairePayload {
  id: number | null;
  name: string;
  weight: number;
  sequence?: number;
  applicationStartDate?: string | null;
  applicationEndDate?: string | null;
  domain?: string | null;
  description?: string | null;
  stageName?: string | null;
  iterationName?: string | null;
  questions?: UpdateQuestionPayload[];
}

export interface UpdateRepresentativePayload {
  id: number | null;
  firstName: string;
  lastName: string;
  email: string;
  weight: number;
  roleIds: number[];
}

export interface UpdateProjectRequest {
  name: string;
  startDate: string;
  deadline: string | null;
  iterationDuration?: number;
  iterationCount?: number;
  aiUsageScopes?: string[];
  dryRun: boolean;
  stages?: UpdateStagePayload[];
  iterations?: UpdateIterationPayload[];
  questionnaires?: UpdateQuestionnairePayload[];
  representatives?: UpdateRepresentativePayload[];
}

export interface ChangesSummary {
  stagesAdded: number;
  stagesRemoved: number;
  stagesUpdated: number;
  iterationsAdded: number;
  iterationsRemoved: number;
  iterationsUpdated: number;
  questionnairesAdded: number;
  questionnairesRemoved: number;
  questionnairesUpdated: number;
  questionsAdded: number;
  questionsRemoved: number;
  questionsUpdated: number;
  representativesAdded: number;
  representativesRemoved: number;
  representativesUpdated: number;
  responsesCreated: number;
  responsesUpdated: number;
  responsesDeleted: number;
  notificationsSent: number;
  warnings: string[];
  blockedReasons: string[];
}

export interface UpdateProjectResponse {
  id: number;
  name: string;
  type: ProjectType;
  status: string;
  startDate: string;
  deadline: string;
  timelineStatus: string;
  changesSummary: ChangesSummary;
}

export interface UpdateProjectConflictError {
  errors: string[];
}

export interface ProjectStageDetail {
  id: number;
  name: string;
  weight: number;
  sequence: number;
  durationDays?: number;
  applicationStartDate?: string | null;
  applicationEndDate?: string | null;
}

export interface ProjectIterationDetail {
  id: number;
  name: string;
  weight: number;
  order?: number;
  applicationStartDate: string;
  applicationEndDate: string;
}

export interface ProjectQuestionDetail {
  id: number;
  text: string;
  roleIds: number[];
  roleNames?: string[];
  stageNames?: string[];
  order?: number;
}

export interface ProjectQuestionnaireDetail {
  id: number;
  name: string;
  weight: number;
  sequence?: number;
  applicationStartDate?: string | null;
  applicationEndDate?: string | null;
  domain?: string | null;
  description?: string | null;
  stageName?: string | null;
  iterationName?: string | null;
  questions: ProjectQuestionDetail[];
}

export interface ProjectRepresentativeDetail {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  weight: number;
  roleIds: number[];
  roleNames?: string[];
  userId?: number | null;
}

export interface ProjectEditData {
  id: number;
  name: string;
  type: ProjectType;
  status: string;
  startDate: string;
  deadline: string | null;
  iterationDuration?: number | null;
  configuredIterationCount?: number | null;
  aiUsageScopes?: string[];
  stages: ProjectStageDetail[];
  iterations: ProjectIterationDetail[];
  questionnaires: ProjectQuestionnaireDetail[];
  representatives: ProjectRepresentativeDetail[];
}
