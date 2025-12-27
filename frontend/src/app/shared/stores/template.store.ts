import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { TemplateListDTO, ProjectTemplate } from '../interfaces/template/template.interface';
import { BaseStore } from './base/base.store';

@Injectable({
  providedIn: 'root'
})
export class TemplateStore extends BaseStore {

  constructor() {
    super('api/templates');
  }

  getAllTemplates(): Observable<TemplateListDTO[]> {
    return this.requestService.makeGet<TemplateListDTO[]>(this.getUrl(''), { useAuth: true });
  }

  getTemplateHeader(templateId: string): Observable<ProjectTemplate> {
    return this.requestService.makeGet<ProjectTemplate>(this.getUrl(`${templateId}/header`), { useAuth: true });
  }

  getFullTemplate(templateId: string): Observable<ProjectTemplate> {
    return this.requestService.makeGet<ProjectTemplate>(this.getUrl(`${templateId}/full`), { useAuth: true });
  }

  createTemplateFromProject(projectId: number, request: {
    name: string;
    description: string;
    visibility: 'PUBLIC' | 'PRIVATE';
  }): Observable<ProjectTemplate> {
    return this.requestService.makePost<ProjectTemplate>(
      this.getUrl(`from-project/${projectId}`),
      { useAuth: true, data: request }
    );
  }
}
