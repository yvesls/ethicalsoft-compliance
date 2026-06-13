import { Component, ChangeDetectionStrategy, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgxSpinnerModule } from 'ngx-spinner';
import { TranslateModule } from '@ngx-translate/core';

type ListStatus = 'loading' | 'loaded' | 'error';

@Component({
  selector: 'app-list',
  standalone: true,
  imports: [CommonModule, NgxSpinnerModule, TranslateModule],
  templateUrl: './list.component.html',
  styleUrls: ['./list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ListComponent {
  @Input() status: ListStatus = 'loading';
  @Input() error: string | null = null;
  @Input() noItemsMessage = 'common.no_items_found';
  @Input() bodyPadding = true;
  @Input() itemCount = 0;
}
