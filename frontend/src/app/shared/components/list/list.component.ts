import { Component, ChangeDetectionStrategy, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgxSpinnerModule } from 'ngx-spinner';

type ListStatus = 'loading' | 'loaded' | 'error';

@Component({
  selector: 'app-list',
  standalone: true,
  imports: [CommonModule, NgxSpinnerModule],
  templateUrl: './list.component.html',
  styleUrls: ['./list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ListComponent {
  @Input() status: ListStatus = 'loading';
  @Input() error: string | null = null;
  @Input() noItemsMessage = 'Nenhum item encontrado.';
  @Input() bodyPadding = true;
  @Input() itemCount = 0;
}
