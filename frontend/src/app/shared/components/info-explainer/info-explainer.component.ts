import {
  Component,
  Input,
  inject,
  ViewChild,
  ElementRef,
  HostListener,
  OnInit,
  OnDestroy,
  signal,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ExplanationService, ExplanationEntry } from '../../services/explanation.service';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-info-explainer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './info-explainer.component.html',
  styleUrl: './info-explainer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InfoExplainerComponent implements OnInit, OnDestroy {
  @Input({ required: true }) key!: string;

  @ViewChild('popover') popoverRef?: ElementRef<HTMLDivElement>;
  @ViewChild('icon') iconRef?: ElementRef<HTMLImageElement>;

  private readonly explanationService = inject(ExplanationService);
  private readonly elementRef = inject(ElementRef);
  private readonly destroy$ = new Subject<void>();
  private componentId = `info-explainer-${Math.random().toString(36).substr(2, 9)}`;

  isOpen = signal(false);
  explanation = signal<ExplanationEntry | null>(null);
  popoverPosition = signal<'right' | 'left'>('right');
  fallbackMessage = 'Sem descrição disponível.';

  ngOnInit(): void {
    this.explanationService
      .getExplanation(this.key)
      .pipe(takeUntil(this.destroy$))
      .subscribe((explanation) => {
        this.explanation.set(explanation);
      });

    this.explanationService
      .onOtherPopoverOpened()
      .pipe(takeUntil(this.destroy$))
      .subscribe((openedComponentId) => {
        if (openedComponentId !== this.componentId && this.isOpen()) {
          this.isOpen.set(false);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  togglePopover(): void {
    const newState = !this.isOpen();
    this.isOpen.set(newState);

    if (newState) {
      this.explanationService.notifyPopoverOpened(this.componentId);
      setTimeout(() => this.checkPopoverPosition(), 0);
    }
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.isOpen() && !this.elementRef.nativeElement.contains(event.target)) {
      this.isOpen.set(false);
    }
  }

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (this.isOpen()) {
      this.isOpen.set(false);
    }
  }

  private checkPopoverPosition(): void {
    if (!this.popoverRef) return;

    const popoverRect = this.popoverRef.nativeElement.getBoundingClientRect();
    const viewportWidth = window.innerWidth;
    const rightEdge = popoverRect.right;

    if (rightEdge > viewportWidth - 16) {
      this.popoverPosition.set('left');
    } else {
      this.popoverPosition.set('right');
    }
  }
}
