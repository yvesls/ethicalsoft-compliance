import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { RequestService } from './request.service';
import { environment } from '../../enviroments/environments';

export interface DocumentEmissionRecordDTO {
  authenticityCode: string;
  documentType: 'NON_COMPLIANCE_BULLETIN' | 'ETHICS_CERTIFICATE';
  projectId: number;
  projectName: string;
  questionnaireId: number | null;
  questionnaireName: string | null;
  scopeLabel: string;
  emittedAt: string;
  emittedByUserId: number | null;
  emittedByName: string;
  isepPercent: number;
  band: string;
  dataHash: string;
  templateVersion: string;
}

export interface BulletinEmissionResult {
  documentCode: string;
  totalRecipients: number;
  sent: number;
  skipped: number;
}

@Injectable({ providedIn: 'root' })
export class DocumentEmissionService {
  private readonly requestService = inject(RequestService);

  constructor() {
    this.requestService.apiUrl = environment.apiBaseUrl;
  }

  registerCertificateEmission(projectId: number): Observable<DocumentEmissionRecordDTO> {
    return this.requestService.makePost<DocumentEmissionRecordDTO>(
      `api/projects/${projectId}/certificate/register-emission`,
      { useAuth: true }
    );
  }

  registerBulletinEmission(projectId: number, questionnaireId: number): Observable<DocumentEmissionRecordDTO> {
    return this.requestService.makePost<DocumentEmissionRecordDTO>(
      `api/projects/${projectId}/questionnaires/${questionnaireId}/bulletin/register-emission`,
      { useAuth: true }
    );
  }

  emitBulletinToRepresentatives(projectId: number, questionnaireId: number): Observable<BulletinEmissionResult> {
    return this.requestService.makePost<BulletinEmissionResult>(
      `api/projects/${projectId}/questionnaires/${questionnaireId}/bulletin/emit`,
      { useAuth: true }
    );
  }

  getEmissions(projectId: number, type?: string, questionnaireId?: number): Observable<DocumentEmissionRecordDTO[]> {
    const params: string[] = [];
    if (type) params.push(`type=${encodeURIComponent(type)}`);
    if (questionnaireId) params.push(`questionnaireId=${questionnaireId}`);
    const query = params.length ? `?${params.join('&')}` : '';
    return this.requestService.makeGet<DocumentEmissionRecordDTO[]>(
      `api/projects/${projectId}/documents/emissions${query}`,
      { useAuth: true }
    );
  }
}
