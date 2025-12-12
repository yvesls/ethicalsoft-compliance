import { ProjectStatus } from '../../enums/project-status.enum';
import { ProjectType } from '../../enums/project-type.enum';
import { TimelineStatus } from '../../enums/timeline-status.enum';

export interface Project {
  id: string;
  name: string;
  status: ProjectStatus;
  timelineStatus?: TimelineStatus | string | null;
  type: ProjectType;
  startDate: Date | null;
  deadline: Date | null;
  closingDate: Date | null;

  iterationDuration?: number | null;
  configuredIterationCount?: number | null;

  currentSituation?: string | null;
  currentStage?: string;
  currentIteration?: number;

  representativeCount: number;
  stageCount: number;
  iterationCount: number;
}
