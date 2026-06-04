import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { RequestService } from './request.service';
import { environment } from '../../enviroments/environments';
import { UrlParameter } from '../interfaces/url-parameter.interface';

export interface DocumentEmissionRecordDTO {
  id?: string;
  authenticityCode: string;
  documentType: 'NON_COMPLIANCE_BULLETIN' | 'ETHICS_CERTIFICATE';
  projectId: number;
  questionnaireId?: number | null;
  emittedAt: string;
  emittedByUserId: number;
  emittedByName: string;
  dataHash: string;
  isepBand: string;
  templateVersion: string;
}

@Injectable({
  providedIn: 'root',
})
export class DocumentEmissionService {
  private readonly requestService = inject(RequestService);

  constructor() {
    this.requestService.apiUrl = environment.apiBaseUrl;
  }

  registerCertificateEmission(projectId: number): Observable<DocumentEmissionRecordDTO> {
    return this.requestService.makePost<DocumentEmissionRecordDTO>(
      `api/projects/${projectId}/certificate/register-emission`,
      { useAuth: true, data: {} }
    );
  }

  registerBulletinEmission(
    projectId: number,
    questionnaireId: number
  ): Observable<DocumentEmissionRecordDTO> {
    return this.requestService.makePost<DocumentEmissionRecordDTO>(
      `api/projects/${projectId}/questionnaires/${questionnaireId}/bulletin/register-emission`,
      { useAuth: true, data: {} }
    );
  }

  getEmissions(
    projectId: number,
    type?: 'NON_COMPLIANCE_BULLETIN' | 'ETHICS_CERTIFICATE',
    questionnaireId?: number
  ): Observable<DocumentEmissionRecordDTO[]> {
    const params: UrlParameter[] = [];
    if (type) params.push({ key: 'type', value: type });
    if (questionnaireId) params.push({ key: 'questionnaireId', value: questionnaireId });

    return this.requestService.makeGet<DocumentEmissionRecordDTO[]>(
      `api/projects/${projectId}/documents/emissions`,
      { useAuth: true },
      ...params
    );
  }
}
