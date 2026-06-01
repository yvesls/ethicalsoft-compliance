import { Component, Input } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { InfoExplainerComponent } from '../../../../shared/components/info-explainer/info-explainer.component';

const DOMAIN_LABELS: Record<string, string> = {
  ETHICS:   'Ética',
  PROCESS:  'Processo',
  QUALITY:  'Qualidade',
  SECURITY: 'Segurança',
  ESG:      'ESG',
  FAIRNESS: 'Fairness',
};

const DOMAIN_COLORS: Record<string, string> = {
  ETHICS:   '#1565c0',
  PROCESS:  '#2e7d32',
  QUALITY:  '#6a1b9a',
  SECURITY: '#c62828',
  ESG:      '#00838f',
  FAIRNESS: '#e65100',
};

@Component({
  selector: 'app-governance-insights-widget',
  standalone: true,
  imports: [TranslateModule, InfoExplainerComponent],
  templateUrl: './governance-insights-widget.component.html',
  styleUrl: './governance-insights-widget.component.scss',
})
export class GovernanceInsightsWidgetComponent {
  @Input({ required: true }) topThemeInsights: Record<string, string> = {};

  get insightEntries(): { domain: string; insight: string }[] {
    return Object.entries(this.topThemeInsights).map(([domain, insight]) => ({ domain, insight }));
  }

  domainLabel(key: string): string {
    return DOMAIN_LABELS[key] ?? key;
  }

  domainColor(key: string): string {
    return DOMAIN_COLORS[key] ?? '#9e9e9e';
  }
}
