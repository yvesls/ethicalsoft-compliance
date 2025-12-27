import { ProjectStatus } from "../../enums/project-status.enum";
import { ProjectType } from "../../enums/project-type.enum";

export interface ProjectFilters {
  name?: string | null;
  code?: string | null;
  type?: ProjectType | null;
  status?: ProjectStatus | null;
  page: number;
  size: number;
}
