import { ProjectStatus } from '../enums/project-status.enum';
import { ProjectType } from '../enums/project-type.enum';

export interface Project {
  id: string;
  name: string;
  code: string;
  status: ProjectStatus;
  type: ProjectType;
  situation: string;
  startDate: string | null;
  endDate: string | null;
}
