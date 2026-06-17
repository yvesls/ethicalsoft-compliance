import {
  Component,
  Input,
  inject,
  ElementRef,
  HostListener,
  OnInit,
  OnDestroy,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { ExplanationService, ExplanationEntry } from '../../services/explanation.service';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-info-explainer',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './info-explainer.component.html',
  styleUrl: './info-explainer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InfoExplainerComponent implements OnInit, OnDestroy {
  @Input({ required: true }) key!: string;

  private readonly explanationService = inject(ExplanationService);
  private readonly elementRef = inject(ElementRef<HTMLElement>);
  private readonly destroy$ = new Subject<void>();

  isOpen = signal(false);
  entry = signal<ExplanationEntry | null>(null);
  popoverPosition = signal<'right' | 'left'>('right');
  popoverStyle = signal<Record<string, string>>({});

  ngOnInit(): void {
    this.explanationService
      .getExplanation(this.key)
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => this.entry.set(e));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  togglePopover(): void {
    if (!this.isOpen()) {
      this.computePosition();
    }
    this.isOpen.update((v) => !v);
  }

  private computePosition(): void {
    const rect = this.elementRef.nativeElement.getBoundingClientRect();
    const spaceRight = window.innerWidth - rect.right;
    const centerY = rect.top + rect.height / 2;

    if (spaceRight >= 340) {
      this.popoverPosition.set('right');
      this.popoverStyle.set({
        top: `${centerY}px`,
        left: `${rect.right + 10}px`,
        right: 'auto',
      });
    } else {
      this.popoverPosition.set('left');
      this.popoverStyle.set({
        top: `${centerY}px`,
        right: `${window.innerWidth - rect.left + 10}px`,
        left: 'auto',
      });
    }
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.isOpen()) return;
    const target = event.target as Node | null;
    if (target && !this.elementRef.nativeElement.contains(target)) {
      this.isOpen.set(false);
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.isOpen()) this.isOpen.set(false);
  }
}
