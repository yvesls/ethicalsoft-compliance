import { Component, inject, Input, signal } from '@angular/core';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AiPanelComponent } from '../ai-panel/ai-panel.component';
import { AiDashboardService } from '../../services/ai-dashboard.service';
import { AiInsightResult } from '../../interfaces/ai.interface';

@Component({
  selector: 'app-ai-risk-report-widget',
  standalone: true,
  imports: [AiPanelComponent, TranslateModule],
  template: `
    <app-ai-panel
      [title]="'dashboard.ai_widgets.risk_title' | translate"
      icon="bi-shield-exclamation"
      [result]="result()"
      [loading]="loading()"
      [buttonLabel]="'dashboard.ai_widgets.risk_btn' | translate"
      [loadingLabel]="'dashboard.ai_widgets.risk_loading' | translate"
      (generate)="generate()"
    />
  `,
})
export class AiRiskReportWidgetComponent {
  @Input({ required: true }) projectId!: number;
  @Input({ required: true }) questionnaireId!: number;

  private readonly aiService = inject(AiDashboardService);
  private readonly translate = inject(TranslateService);

  result = signal<AiInsightResult | null>(null);
  loading = signal(false);

  generate(): void {
    this.loading.set(true);
    this.result.set(null);

    this.aiService.getRiskReport(this.projectId, this.questionnaireId).subscribe({
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
          fallbackMessage: this.translate.instant('dashboard.ai_widgets.error_msg'),
        });
        this.loading.set(false);
      },
    });
  }
}
