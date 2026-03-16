import { Component, Input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Band } from '../../interfaces/dashboard.interface';
import { BandBadgeComponent } from '../band-badge/band-badge.component';

@Component({
  selector: 'app-isep-kpi-card',
  standalone: true,
  imports: [DecimalPipe, BandBadgeComponent],
  template: `
    <div class="kpi-card">
      <p class="kpi-label">{{ label }}</p>
      @if (value !== null) {
        <p class="kpi-value">{{ value | number: '1.2-2' }}%</p>
      } @else {
        <p class="kpi-value kpi-value--empty">—</p>
      }
      @if (band) { <app-band-badge [band]="band" /> }
      @if (secondary) { <p class="kpi-secondary">{{ secondary }}</p> }
    </div>
  `,
  styles: [':host { display: block; min-width: 0; }'],
})
export class IsepKpiCardComponent {
  @Input({ required: true }) label!: string;
  @Input() value: number | null = null;
  @Input() band: Band | null = null;
  @Input() secondary: string | null = null;
}
