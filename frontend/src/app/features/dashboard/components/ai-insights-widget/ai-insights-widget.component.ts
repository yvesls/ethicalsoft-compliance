import { Component, inject, Input, signal } from '@angular/core';
import { AiPanelComponent } from '../ai-panel/ai-panel.component';
import { AiDashboardService } from '../../services/ai-dashboard.service';
import { AiInsightResult } from '../../interfaces/ai.interface';
import { AiTokenService } from '../../../../core/ai/ai-token.service';
import { ModalService } from '../../../../core/services/modal.service';
import { MissingAiTokenDialogComponent } from '../../../../shared/components/missing-ai-token-dialog/missing-ai-token-dialog.component';

@Component({
  selector: 'app-ai-insights-widget',
  standalone: true,
  imports: [AiPanelComponent],
  template: `
    <app-ai-panel
      title="Análise de IA — Justificativas"
      icon="bi-lightbulb"
      [result]="result()"
      [loading]="loading()"
      buttonLabel="Gerar Análise"
      loadingLabel="Analisando justificativas com IA... (pode levar até 15s)"
      (generate)="generate()"
    />
  `,
})
export class AiInsightsWidgetComponent {
  @Input({ required: true }) projectId!: number;
  @Input({ required: true }) questionnaireId!: number;

  private readonly aiService = inject(AiDashboardService);
  private readonly aiTokenService = inject(AiTokenService);
  private readonly modalService = inject(ModalService);

  result = signal<AiInsightResult | null>(null);
  loading = signal(false);

  generate(): void {
    if (!this.aiTokenService.hasToken) {
      this.modalService.open(MissingAiTokenDialogComponent, 'small-card');
      return;
    }

    this.loading.set(true);
    this.result.set(null);

    this.aiService.getInsights(this.projectId, this.questionnaireId).subscribe({
      next: (res) => {
        this.result.set(res);
        this.loading.set(false);
      },
      error: () => {
        this.result.set({
          available: false,
          content: null,
          model: null,
          generatedAt: '',
          fallbackMessage: 'Erro ao conectar com o serviço de IA. Tente novamente.',
        });
        this.loading.set(false);
      },
    });
  }
}
