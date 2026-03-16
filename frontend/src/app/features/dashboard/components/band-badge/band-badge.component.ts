import { Component, Input } from '@angular/core';
import { NgClass } from '@angular/common';
import { Band, BAND_META } from '../../interfaces/dashboard.interface';

@Component({
  selector: 'app-band-badge',
  standalone: true,
  imports: [NgClass],
  template: `
    <span
      class="band-badge"
      [ngClass]="bandClass"
      [title]="bandRange"
    >
      Faixa {{ band }}
    </span>
  `,
  styles: [`
    .band-badge {
      display: inline-block;
      padding: 0.25rem 0.75rem;
      border-radius: 1rem;
      font-weight: 700;
      font-size: 0.85rem;
      color: #fff;
    }
    .band-a { background-color: #2e7d32; }
    .band-b { background-color: #1565c0; }
    .band-c { background-color: #f57f17; }
    .band-d { background-color: #e65100; }
    .band-e { background-color: #c62828; }
  `],
})
export class BandBadgeComponent {
  @Input({ required: true }) band!: Band;

  get bandClass(): string {
    return BAND_META[this.band]?.colorClass ?? '';
  }

  get bandRange(): string {
    return BAND_META[this.band]?.range ?? '';
  }
}
