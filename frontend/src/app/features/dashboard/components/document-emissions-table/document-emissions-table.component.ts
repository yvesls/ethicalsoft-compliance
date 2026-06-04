import { Component, Input, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { DocumentEmissionService, DocumentEmissionRecordDTO } from '../../../../core/services/document-emission.service';
import { DashboardService } from '../../services/dashboard.service';
import { NotificationService } from '../../../../core/services/notification.service';

@Component({
  selector: 'app-document-emissions-table',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './document-emissions-table.component.html',
  styleUrl: './document-emissions-table.component.scss',
})
export class DocumentEmissionsTableComponent implements OnInit {
  @Input() projectId!: number;

  private documentEmissionService = inject(DocumentEmissionService);
  private dashboardService = inject(DashboardService);
  private notificationService = inject(NotificationService);

  emissions = signal<DocumentEmissionRecordDTO[]>([]);
  loading = signal(true);
  selectedType = signal<'ALL' | 'NON_COMPLIANCE_BULLETIN' | 'ETHICS_CERTIFICATE'>('ALL');

  ngOnInit(): void {
    this.loadEmissions();
  }

  loadEmissions(): void {
    this.loading.set(true);
    const type = this.selectedType() === 'ALL' ? undefined : (this.selectedType() as any);

    this.documentEmissionService.getEmissions(this.projectId, type).subscribe({
      next: (data) => {
        this.emissions.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.notificationService.showError('Erro ao carregar emissões');
        this.loading.set(false);
      },
    });
  }

  onTypeChange(type: 'ALL' | 'NON_COMPLIANCE_BULLETIN' | 'ETHICS_CERTIFICATE'): void {
    this.selectedType.set(type);
    this.loadEmissions();
  }

  downloadDocument(emission: DocumentEmissionRecordDTO): void {
    if (emission.documentType === 'ETHICS_CERTIFICATE') {
      this.dashboardService.downloadCertificate(this.projectId).subscribe({
        next: (blob) => this.savePdf(blob, 'certificado'),
        error: () => this.notificationService.showError('Erro ao baixar certificado'),
      });
    } else {
      if (emission.questionnaireId) {
        this.dashboardService.downloadBulletin(this.projectId, emission.questionnaireId).subscribe({
          next: (blob) => this.savePdf(blob, 'boletim'),
          error: () => this.notificationService.showError('Erro ao baixar boletim'),
        });
      }
    }
  }

  private savePdf(blob: Blob, name: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${name}-${new Date().getTime()}.pdf`;
    a.click();
    URL.revokeObjectURL(url);
  }

  getDocumentTypeLabel(type: string): string {
    return type === 'ETHICS_CERTIFICATE' ? 'Certificado' : 'Boletim';
  }

  getEmittedAtFormatted(date: string): string {
    return new Date(date).toLocaleString('pt-BR');
  }
}
