import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

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
  private http = inject(HttpClient);

  registerCertificateEmission(projectId: number): Observable<DocumentEmissionRecordDTO> {
    return this.http.post<DocumentEmissionRecordDTO>(
      `/api/projects/${projectId}/certificate/register-emission`,
      {}
    );
  }

  registerBulletinEmission(
    projectId: number,
    questionnaireId: number
  ): Observable<DocumentEmissionRecordDTO> {
    return this.http.post<DocumentEmissionRecordDTO>(
      `/api/projects/${projectId}/questionnaires/${questionnaireId}/bulletin/register-emission`,
      {}
    );
  }

  getEmissions(
    projectId: number,
    type?: 'NON_COMPLIANCE_BULLETIN' | 'ETHICS_CERTIFICATE',
    questionnaireId?: number
  ): Observable<DocumentEmissionRecordDTO[]> {
    let url = `/api/projects/${projectId}/documents/emissions`;
    const params = new URLSearchParams();

    if (type) {
      params.append('type', type);
    }
    if (questionnaireId) {
      params.append('questionnaireId', questionnaireId.toString());
    }

    if (params.toString()) {
      url += `?${params.toString()}`;
    }

    return this.http.get<DocumentEmissionRecordDTO[]>(url);
  }
}
