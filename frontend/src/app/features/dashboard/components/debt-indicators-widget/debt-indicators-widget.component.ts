import { Component, Input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

export type TrafficLightColor = 'green' | 'yellow' | 'red' | 'gray';

@Component({
  selector: 'app-debt-indicators-widget',
  standalone: true,
  imports: [DecimalPipe, TranslateModule],
  templateUrl: './debt-indicators-widget.component.html',
  styleUrl: './debt-indicators-widget.component.scss',
})
export class DebtIndicatorsWidgetComponent {
  @Input() ethicsDebtPercent: number | null = null;
  @Input() techDebtPercent: number | null = null;

  get ethicsColor(): TrafficLightColor {
    if (this.ethicsDebtPercent === null) return 'gray';
    if (this.ethicsDebtPercent < 10) return 'green';
    if (this.ethicsDebtPercent <= 30) return 'yellow';
    return 'red';
  }

  get techColor(): TrafficLightColor {
    if (this.techDebtPercent === null) return 'gray';
    if (this.techDebtPercent < 15) return 'green';
    if (this.techDebtPercent <= 35) return 'yellow';
    return 'red';
  }
}
