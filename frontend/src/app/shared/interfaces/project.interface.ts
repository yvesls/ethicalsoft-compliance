import { ProjectStatus } from '../enums/project-status.enum';
import { ProjectType } from '../enums/project-type.enum';

export interface Project {
  id: string;
  name: string;
  status: ProjectStatus;
  type: ProjectType;
  startDate: Date | null;
  endDate: Date | null;

  situation?: string;
  currentStage?: string;
  currentIteration?: number;

  representativeCount: number;
  stageCount: number;
  iterationCount: number;
}
