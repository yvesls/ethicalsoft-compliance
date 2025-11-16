import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  TemplateListDTO,
  ProjectTemplate,
  TemplateStageDTO,
  TemplateIterationDTO,
  TemplateQuestionnaireDTO,
  TemplateRepresentativeDTO
} from '../interfaces/template/template.interface';
import { BaseStore } from './base/base.store';

@Injectable({
  providedIn: 'root'
})
export class TemplateStore extends BaseStore {

  constructor() {
    super('api/templates');
  }

  /**
   * Lista todos os templates disponíveis (públicos e privados do usuário)
   */
  getAllTemplates(): Observable<TemplateListDTO[]> {
    return this.requestService.makeGet<TemplateListDTO[]>(this.getUrl(''), { useAuth: true });
  }

  /**
   * Busca apenas o header (informações básicas) do template
   */
  getTemplateHeader(templateId: string): Observable<ProjectTemplate> {
    return this.requestService.makeGet<ProjectTemplate>(this.getUrl(`${templateId}/header`), { useAuth: true });
  }

  /**
   * Busca apenas as etapas do template
   */
  getTemplateStages(templateId: string): Observable<TemplateStageDTO[]> {
    return this.requestService.makeGet<TemplateStageDTO[]>(this.getUrl(`${templateId}/stages`), { useAuth: true });
  }

  /**
   * Busca apenas as iterações do template
   */
  getTemplateIterations(templateId: string): Observable<TemplateIterationDTO[]> {
    return this.requestService.makeGet<TemplateIterationDTO[]>(this.getUrl(`${templateId}/iterations`), { useAuth: true });
  }

  /**
   * Busca apenas os questionários do template
   */
  getTemplateQuestionnaires(templateId: string): Observable<TemplateQuestionnaireDTO[]> {
    return this.requestService.makeGet<TemplateQuestionnaireDTO[]>(this.getUrl(`${templateId}/questionnaires`), { useAuth: true });
  }

  /**
   * Busca apenas os representantes do template
   */
  getTemplateRepresentatives(templateId: string): Observable<TemplateRepresentativeDTO[]> {
    return this.requestService.makeGet<TemplateRepresentativeDTO[]>(this.getUrl(`${templateId}/representatives`), { useAuth: true });
  }

  /**
   * Busca o template completo
   */
  getFullTemplate(templateId: string): Observable<ProjectTemplate> {
    return this.requestService.makeGet<ProjectTemplate>(this.getUrl(`${templateId}/full`), { useAuth: true });
  }

  /**
   * Cria um template a partir de um projeto existente
   */
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
