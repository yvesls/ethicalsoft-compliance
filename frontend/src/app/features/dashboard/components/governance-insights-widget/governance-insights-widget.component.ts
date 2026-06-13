import { Component, Input, inject } from '@angular/core';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

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
  imports: [TranslateModule],
  templateUrl: './governance-insights-widget.component.html',
  styleUrl: './governance-insights-widget.component.scss',
})
export class GovernanceInsightsWidgetComponent {
  @Input({ required: true }) topThemeInsights: Record<string, string> = {};

  private readonly translate = inject(TranslateService);

  get insightEntries(): { domain: string; insight: string }[] {
    return Object.entries(this.topThemeInsights).map(([domain, insight]) => ({ domain, insight }));
  }

  domainLabel(key: string): string {
    const translated = this.translate.instant('question_domain.' + key);
    return translated !== 'question_domain.' + key ? translated : key;
  }

  domainColor(key: string): string {
    return DOMAIN_COLORS[key] ?? '#9e9e9e';
  }
}
