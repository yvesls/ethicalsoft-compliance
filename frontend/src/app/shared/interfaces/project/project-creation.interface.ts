import { ProjectType } from '../../enums/project-type.enum';

export type ProjectStatusCode = 'ABERTO' | 'RASCUNHO' | 'CONCLUIDO' | 'ARQUIVADO';

export interface StagePayload {
	name: string;
	weight: number;
	sequence?: number;
	durationDays?: number;
	applicationStartDate?: string | null;
	applicationEndDate?: string | null;
}

export interface IterationPayload {
	name: string;
	weight?: number;
	order?: number;
	applicationStartDate: string;
	applicationEndDate: string;
}

export interface QuestionPayload {
	value: string;
	roleIds: number[];
	roleNames: string[];
}

export interface QuestionnairePayload {
	name: string;
	sequence?: number;
	iterationName?: string | null;
	stageName?: string | null;
	weight: number;
	applicationStartDate?: string | null;
	applicationEndDate?: string | null;
	questions?: QuestionPayload[];
}

export interface RepresentativePayload {
	id?: number | string | null;
	firstName: string;
	lastName: string;
	email: string;
	userId?: number | null;
	weight: number;
	roleIds: number[];
	projectId?: number | null;
}

export interface ProjectCreationPayload {
	name: string;
	type: ProjectType;
	startDate: string;
	deadline: string | null;
	templateId: string | number | null;
	status: ProjectStatusCode;
	iterationDuration?: number;
	iterationCount?: number;
	stages?: StagePayload[];
	iterations?: IterationPayload[];
	questionnaires?: QuestionnairePayload[];
	representatives?: RepresentativePayload[];
}

export interface ProjectCreationResponse {
	id: number;
	name: string;
	type: ProjectType;
	stageCount?: number;
	iterationCount?: number;
	representativeCount?: number;
	startDate?: string;
}
