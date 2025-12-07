import { Observable } from 'rxjs';
import { Injectable } from '@angular/core';
import { BaseStore } from './base/base.store';
import { ProjectFilters } from '../interfaces/project/project-filters.interface';
import { Project } from '../interfaces/project/project.interface';
import { RequestInputOptions } from '../../core/interfaces/request-input-options.interface';
import { Page } from '../interfaces/pageable.interface';
import {
  ProjectCreationPayload,
  ProjectCreationResponse,
} from '../interfaces/project/project-creation.interface';
import { RoleSummary } from '../interfaces/role/role-summary.interface';

@Injectable({
  providedIn: 'root',
})
export class ProjectStore extends BaseStore {
  constructor() {
    super('api/projects');
  }

  getProjects(filters: ProjectFilters): Observable<Page<Project>> {
    const { page, size, ...filterData } = filters;

    const options: RequestInputOptions = {
      useAuth: true,
      useCache: true,
      data: filterData,
    };

    const url = this.getUrl(`search?page=${page}&size=${size}`);
    return this.requestService.makePost<Page<Project>>(
      url,
      options
    );
  }

  createProject(payload: ProjectCreationPayload): Observable<ProjectCreationResponse> {
    const normalizedPayload: ProjectCreationPayload = {
      ...payload,
      templateId: this.normalizeTemplateId(payload.templateId)
    };

    return this.requestService.makePost<ProjectCreationResponse>(this.getUrl(''), {
      useAuth: true,
      data: normalizedPayload,
    });
  }

  listRoles(): Observable<RoleSummary[]> {
    return this.requestService.makeGet<RoleSummary[]>(this.getUrl('roles'), {
      useAuth: true,
    });
  }

  private normalizeTemplateId(templateId: ProjectCreationPayload['templateId']): number | null {
    if (typeof templateId === 'number' && Number.isFinite(templateId)) {
      return templateId;
    }

    if (typeof templateId === 'string') {
      const parsed = Number(templateId);

      if (Number.isFinite(parsed)) {
        return parsed;
      }

      console.warn('[ProjectStore] templateId recebido não é numérico e será enviado como null.', templateId);
      return null;
    }

    return null;
  }
}
