import { Component, inject, Input, signal } from '@angular/core';
import { AiPanelComponent } from '../ai-panel/ai-panel.component';
import { AiDashboardService } from '../../services/ai-dashboard.service';
import { AiInsightResult } from '../../interfaces/ai.interface';

@Component({
  selector: 'app-ai-explain-widget',
  standalone: true,
  imports: [AiPanelComponent],
  template: `
    <app-ai-panel
      title="Explicação dos Resultados ISEP"
      icon="bi-info-circle"
      [result]="result()"
      [loading]="loading()"
      buttonLabel="Explicar Resultados"
      loadingLabel="Gerando explicação com IA... (pode levar até 15s)"
      (generate)="generate()"
    />
  `,
})
export class AiExplainWidgetComponent {
  @Input({ required: true }) projectId!: number;
  @Input({ required: true }) questionnaireId!: number;

  private readonly aiService = inject(AiDashboardService);

  result = signal<AiInsightResult | null>(null);
  loading = signal(false);

  generate(): void {
    this.loading.set(true);
    this.result.set(null);

    this.aiService.getExplanation(this.projectId, this.questionnaireId).subscribe({
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
