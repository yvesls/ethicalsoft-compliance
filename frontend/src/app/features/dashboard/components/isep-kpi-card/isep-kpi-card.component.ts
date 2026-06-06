import { Component, Input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Band } from '../../interfaces/dashboard.interface';
import { BandBadgeComponent } from '../band-badge/band-badge.component';

@Component({
  selector: 'app-isep-kpi-card',
  standalone: true,
  imports: [DecimalPipe, BandBadgeComponent],
  templateUrl: './isep-kpi-card.component.html',
  styleUrl: './isep-kpi-card.component.scss',
})
export class IsepKpiCardComponent {
  @Input({ required: true }) label!: string;
  @Input() value: number | null = null;
  @Input() band: Band | null = null;
  @Input() secondary: string | null = null;
}
