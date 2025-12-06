import {
  Component,
  ChangeDetectionStrategy,
  Input,
  Output,
  EventEmitter,
  signal,
  computed,
  OnChanges,
  SimpleChanges,
} from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pagination.component.html',
  styleUrl: './pagination.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginationComponent implements OnChanges {
  @Input({ required: true }) currentPage = 1;
  @Input({ required: true }) totalItems = 0;
  @Input({ required: true }) pageSize = 10;
  @Input() pagesToShow = 5;

  @Output() pageChange = new EventEmitter<number>();

  totalPages = signal(0);

  pages = computed(() => {
    const total = this.totalPages();
    const current = this.currentPage;
    const half = Math.floor(this.pagesToShow / 2);

    let start = Math.max(current - half, 1);
  const end = Math.min(start + this.pagesToShow - 1, total);

    if (end - start + 1 < this.pagesToShow) {
      start = Math.max(end - this.pagesToShow + 1, 1);
    }

    const pagesArray: (number | string)[] = Array.from(
      { length: end - start + 1 },
      (_, i) => start + i
    );

    if (start > 1) {
      pagesArray.unshift('...');
      pagesArray.unshift(1);
    }
    if (end < total) {
      pagesArray.push('...', total);
    }
    return pagesArray;
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['totalItems'] || changes['pageSize']) {
      this.totalPages.set(Math.ceil(this.totalItems / this.pageSize));
    }
  }

  onPageChange(page: number | string): void {
    if (typeof page === 'number' && page !== this.currentPage) {
      this.pageChange.emit(page);
    }
  }

  onPrevious(): void {
    if (this.currentPage > 1) {
      this.pageChange.emit(this.currentPage - 1);
    }
  }

  onNext(): void {
    if (this.currentPage < this.totalPages()) {
      this.pageChange.emit(this.currentPage + 1);
    }
  }
}
