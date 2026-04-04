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
import {
  UpdateProjectRequest,
  UpdateProjectResponse,
  ProjectEditData,
} from '../interfaces/project/project-update.interface';
import { RoleSummary } from '../interfaces/role/role-summary.interface';
import {
  ProjectQuestionnaireFilters,
  ProjectQuestionnaireSummary,
  QuestionnaireReminderRequest,
  RescheduleQuestionnairePayload,
  RescheduleQuestionnaireResponse,
} from '../interfaces/project/project-questionnaire.interface';
import { UrlParameter } from '../../core/interfaces/url-parameter.interface';

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

  getProjectById(projectId: string): Observable<Project> {
    return this.requestService.makeGet<Project>(this.getUrl(projectId), {
      useAuth: true,
    });
  }

  getProjectQuestionnaires(
    projectId: string,
    filters: ProjectQuestionnaireFilters
  ): Observable<Page<ProjectQuestionnaireSummary>> {
    const params: UrlParameter[] = [
      { key: 'page', value: filters.page },
      { key: 'size', value: filters.size },
    ];

    if (filters.name) {
      params.push({ key: 'name', value: filters.name });
    }

    if (filters.stage) {
      params.push({ key: 'stage', value: filters.stage });
    }

    if (filters.iteration) {
      params.push({ key: 'iteration', value: filters.iteration });
    }

    if (filters.status) {
      params.push({ key: 'status', value: filters.status });
    }

    return this.requestService.makeGet<Page<ProjectQuestionnaireSummary>>(
      this.getUrl(`${projectId}/questionnaires`),
      { useAuth: true },
      ...params
    );
  }

  getQuestionnaireSummary(
    projectId: string,
    questionnaireId: number
  ): Observable<ProjectQuestionnaireSummary> {
    return this.requestService.makeGet<ProjectQuestionnaireSummary>(
      this.getUrl(`${projectId}/questionnaires/${questionnaireId}`),
      { useAuth: true }
    );
  }

  sendQuestionnaireReminder(
    projectId: string,
    questionnaireId: number,
    payload: QuestionnaireReminderRequest
  ): Observable<void> {
    return this.requestService.makePost<void>(
      this.getUrl(`${projectId}/questionnaires/${questionnaireId}/reminders`),
      {
        useAuth: true,
        data: payload,
      }
    );
  }

  updateDraft(projectId: string | number, payload: ProjectCreationPayload): Observable<ProjectCreationResponse> {
    const normalizedPayload: ProjectCreationPayload = {
      ...payload,
      templateId: this.normalizeTemplateId(payload.templateId),
      status: 'RASCUNHO',
    };

    return this.requestService.makePut<ProjectCreationResponse>(
      this.getUrl(`${projectId}/draft`),
      {
        useAuth: true,
        data: normalizedPayload,
      }
    );
  }

  publishProject(projectId: string | number): Observable<ProjectCreationResponse> {
    return this.requestService.makePost<ProjectCreationResponse>(
      this.getUrl(`${projectId}/publish`),
      { useAuth: true }
    );
  }

  getProjectForEdit(projectId: string | number): Observable<ProjectEditData> {
    return this.requestService.makeGet<ProjectEditData>(this.getUrl(`${projectId}/edit`), {
      useAuth: true,
    });
  }

  updateProject(projectId: string | number, payload: UpdateProjectRequest): Observable<UpdateProjectResponse> {
    return this.requestService.makePut<UpdateProjectResponse>(
      this.getUrl(`${projectId}`),
      {
        useAuth: true,
        data: payload,
      }
    );
  }

  deleteProject(projectId: string | number): Observable<{ projectId: number; status: string; message: string }> {
    return this.requestService.makeDelete<{ projectId: number; status: string; message: string }>(
      this.getUrl(`${projectId}`),
      { useAuth: true }
    );
  }

  rescheduleQuestionnaire(
    projectId: string | number,
    questionnaireId: number,
    payload: RescheduleQuestionnairePayload
  ): Observable<RescheduleQuestionnaireResponse> {
    return this.requestService.makePut<RescheduleQuestionnaireResponse>(
      this.getUrl(`${projectId}/questionnaires/${questionnaireId}/reschedule`),
      {
        useAuth: true,
        data: payload,
      }
    );
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
