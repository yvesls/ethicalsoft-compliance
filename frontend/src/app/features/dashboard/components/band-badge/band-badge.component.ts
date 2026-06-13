import { Component, Input } from '@angular/core';
import { NgClass } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { Band, BAND_META } from '../../interfaces/dashboard.interface';

@Component({
  selector: 'app-band-badge',
  standalone: true,
  imports: [NgClass, TranslateModule],
  templateUrl: './band-badge.component.html',
  styleUrl: './band-badge.component.scss',
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
