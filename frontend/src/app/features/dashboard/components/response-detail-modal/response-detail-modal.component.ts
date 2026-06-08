import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { DashboardService } from '../../services/dashboard.service';
import { RepresentativeResponseDTO } from '../../interfaces/dashboard.interface';
import { ModalService } from '../../../../core/services/modal.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-response-detail-modal',
  standalone: true,
  imports: [CommonModule, DatePipe, TranslateModule],
  templateUrl: './response-detail-modal.component.html',
  styleUrl: './response-detail-modal.component.scss',
})
export class ResponseDetailModalComponent implements OnInit {
  @Input() projectId!: number;
  @Input() questionnaireId!: number;
  @Input() representativeId!: number;
  @Input() representativeName = '';

  private readonly dashboardService = inject(DashboardService);
  private readonly modalService = inject(ModalService);
  private readonly notificationService = inject(NotificationService);
  private readonly translate = inject(TranslateService);

  data = signal<RepresentativeResponseDTO | null>(null);
  loading = signal(true);

  ngOnInit(): void {
    this.loadResponses();
  }

  private loadResponses(): void {
    this.loading.set(true);
    this.dashboardService
      .getRepresentativeResponses(this.projectId, this.questionnaireId, this.representativeId)
      .subscribe({
        next: (dto) => {
          this.data.set(dto);
          this.loading.set(false);
        },
        error: () => {
          this.notificationService.showError(this.translate.instant('notifications.dashboard.load_responses_error'));
          this.loading.set(false);
        },
      });
  }

  close(): void {
    this.modalService.close();
  }

  getStatusLabel(status: string): string {
    const map: Record<string, string> = {
      COMPLETED: this.translate.instant('questionnaire.timeline_status.concluido'),
      PENDING: this.translate.instant('questionnaire.timeline_status.pendente'),
      IN_PROGRESS: this.translate.instant('questionnaire.timeline_status.em_andamento'),
    };
    return map[status] ?? status;
  }
}
