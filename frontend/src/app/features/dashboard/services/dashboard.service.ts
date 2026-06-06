import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { RequestService } from '../../../core/services/request.service';
import { environment } from '../../../enviroments/environments';
import {
  ConsolidatedAnswerDTO,
  ConsolidatedAnswerFilters,
  IndividualDashboardDTO,
  IsepDataExportDTO,
  Page,
  ProjectCloseResultDTO,
  ProjectIsepDashboardDTO,
  QuestionnaireIsepDashboardDTO,
  RepresentativeResponseDTO,
  RoleStageComplianceDTO,
  WordCloudDTO,
} from '../interfaces/dashboard.interface';
import { UrlParameter } from '../../../core/interfaces/url-parameter.interface';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly requestService = inject(RequestService);

  constructor() {
    this.requestService.apiUrl = environment.apiBaseUrl;
  }

  getProjectDashboard(projectId: number): Observable<ProjectIsepDashboardDTO> {
    return this.requestService.makeGet<ProjectIsepDashboardDTO>(
      `api/projects/${projectId}/dashboard`,
      { useAuth: true }
    );
  }

  getQuestionnaireDashboard(
    projectId: number,
    questionnaireId: number
  ): Observable<QuestionnaireIsepDashboardDTO> {
    return this.requestService.makeGet<QuestionnaireIsepDashboardDTO>(
      `api/projects/${projectId}/questionnaires/${questionnaireId}/dashboard`,
      { useAuth: true }
    );
  }

  getRoleStageDashboard(
    projectId: number,
    questionnaireId: number
  ): Observable<RoleStageComplianceDTO[]> {
    return this.requestService.makeGet<RoleStageComplianceDTO[]>(
      `api/projects/${projectId}/questionnaires/${questionnaireId}/dashboard/role-stage`,
      { useAuth: true }
    );
  }

  getWordCloud(
    projectId: number,
    questionnaireId: number
  ): Observable<WordCloudDTO> {
    return this.requestService.makeGet<WordCloudDTO>(
      `api/projects/${projectId}/questionnaires/${questionnaireId}/dashboard/word-cloud`,
      { useAuth: true }
    );
  }

  getIndividualDashboard(
    projectId: number,
    questionnaireId: number,
    representativeId: number
  ): Observable<IndividualDashboardDTO> {
    const repParam: UrlParameter = { key: 'representativeId', value: representativeId };
    return this.requestService.makeGet<IndividualDashboardDTO>(
      `api/projects/${projectId}/questionnaires/${questionnaireId}/dashboard/individual`,
      { useAuth: true },
      repParam
    );
  }

  exportProjectJson(projectId: number, anonymize = true): Observable<IsepDataExportDTO[]> {
    const anonParam: UrlParameter = { key: 'anonymize', value: anonymize };
    return this.requestService.makeGet<IsepDataExportDTO[]>(
      `api/projects/${projectId}/dashboard/export`,
      { useAuth: true },
      anonParam
    );
  }

  exportQuestionnaireJson(
    projectId: number,
    questionnaireId: number,
    anonymize = true
  ): Observable<IsepDataExportDTO> {
    const anonParam: UrlParameter = { key: 'anonymize', value: anonymize };
    return this.requestService.makeGet<IsepDataExportDTO>(
      `api/projects/${projectId}/questionnaires/${questionnaireId}/dashboard/export`,
      { useAuth: true },
      anonParam
    );
  }

  getProjectCsvUrl(projectId: number, anonymize = true): string {
    return `${environment.apiBaseUrl}/api/projects/${projectId}/dashboard/export/csv?anonymize=${anonymize}`;
  }

  getQuestionnaireCsvUrl(
    projectId: number,
    questionnaireId: number,
    anonymize = true
  ): string {
    return `${environment.apiBaseUrl}/api/projects/${projectId}/questionnaires/${questionnaireId}/dashboard/export/csv?anonymize=${anonymize}`;
  }

  forceCloseQuestionnaire(
    projectId: number,
    questionnaireId: number
  ): Observable<void> {
    return this.requestService.makePost<void>(
      `api/projects/${projectId}/questionnaires/${questionnaireId}/force-close`,
      { useAuth: true }
    );
  }

  closeProject(projectId: number): Observable<ProjectCloseResultDTO> {
    return this.requestService.makePost<ProjectCloseResultDTO>(
      `api/projects/${projectId}/close`,
      { useAuth: true }
    );
  }

  getRepresentativeResponses(
    projectId: number,
    questionnaireId: number,
    representativeId: number
  ): Observable<RepresentativeResponseDTO> {
    return this.requestService.makeGet<RepresentativeResponseDTO>(
      `api/projects/${projectId}/questionnaires/${questionnaireId}/responses/representative/${representativeId}`,
      { useAuth: true }
    );
  }

  getQuestionnaireConsolidatedResponses(
    projectId: number,
    questionnaireId: number,
    filters: ConsolidatedAnswerFilters
  ): Observable<Page<ConsolidatedAnswerDTO>> {
    const params = this.buildFilterParams(filters);
    return this.requestService.makeGet<Page<ConsolidatedAnswerDTO>>(
      `api/projects/${projectId}/questionnaires/${questionnaireId}/responses/consolidated`,
      { useAuth: true },
      ...params
    );
  }

  getProjectConsolidatedResponses(
    projectId: number,
    filters: ConsolidatedAnswerFilters
  ): Observable<Page<ConsolidatedAnswerDTO>> {
    const params = this.buildFilterParams(filters);
    return this.requestService.makeGet<Page<ConsolidatedAnswerDTO>>(
      `api/projects/${projectId}/responses/consolidated`,
      { useAuth: true },
      ...params
    );
  }

  private buildFilterParams(filters: ConsolidatedAnswerFilters): UrlParameter[] {
    const params: UrlParameter[] = [
      { key: 'page', value: filters.page },
      { key: 'size', value: filters.size },
    ];
    if (filters.sort) params.push({ key: 'sort', value: filters.sort });
    if (filters.representativeId != null) params.push({ key: 'representativeId', value: filters.representativeId });
    if (filters.questionId != null) params.push({ key: 'questionId', value: filters.questionId });
    if (filters.roleId != null) params.push({ key: 'roleId', value: filters.roleId });
    if (filters.response != null) params.push({ key: 'response', value: filters.response });
    if (filters.questionText) params.push({ key: 'questionText', value: filters.questionText });
    if (filters.questionnaireId != null) params.push({ key: 'questionnaireId', value: filters.questionnaireId });
    return params;
  }
}
