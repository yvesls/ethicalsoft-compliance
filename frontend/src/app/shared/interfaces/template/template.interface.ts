import { ProjectType } from '../../enums/project-type.enum';

export interface TemplateListDTO {
  id: string;
  name: string;
  description: string;
  type: ProjectType;
}

export interface TemplateStageDTO {
  name: string;
  weight: number;
  sequence?: number;
  durationDays?: number;
}

export interface TemplateIterationDTO {
  name: string;
  weight: number;
}

export interface TemplateRepresentativeDTO {
  email: string;
  firstName: string;
  lastName: string;
  weight: number;
  roleNames: string[];
}

export interface TemplateQuestionDTO {
  value: string;
  stageName?: string;
  roleNames: string[];
}

export interface TemplateQuestionnaireDTO {
  name: string;
  stageName?: string;
  iterationRefName?: string;
  questions: TemplateQuestionDTO[];
}

export interface ProjectTemplate {
  id?: string;
  name: string;
  description: string;
  visibility: 'PUBLIC' | 'PRIVATE';
  userId?: number;
  type: ProjectType;
  defaultIterationCount?: number;
  defaultIterationDuration?: number;
  stages?: TemplateStageDTO[];
  iterations?: TemplateIterationDTO[];
  representatives?: TemplateRepresentativeDTO[];
  questionnaires?: TemplateQuestionnaireDTO[];
}

export enum TemplatePartType {
  STAGES = 'stages',
  ITERATIONS = 'iterations',
  REPRESENTATIVES = 'representatives',
  QUESTIONNAIRES = 'questionnaires',
  FULL = 'full'
}
