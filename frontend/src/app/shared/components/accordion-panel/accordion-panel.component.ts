import {
  CommonModule
} from '@angular/common';
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnInit,
  OnChanges,
  SimpleChanges,
  signal,
  Output,
  EventEmitter,
  inject
} from '@angular/core';

@Component({
  selector: 'app-accordion-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './accordion-panel.component.html',
  styleUrls: ['./accordion-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccordionPanelComponent implements OnInit, OnChanges {
  private cdr = inject(ChangeDetectorRef);
  private isUpdatingFromExternal = false;

  @Input() title: string = '';
  @Input() required: boolean = false;
  @Input() startOpen: boolean = false;
  @Input() disabled: boolean = false;
  @Input() isOpenExternal?: boolean;

  @Output() toggled = new EventEmitter<boolean>();
  @Output() attemptedToggle = new EventEmitter<void>();

  public isOpen = signal(false);

  ngOnInit(): void {
    if (this.isOpenExternal !== undefined) {
      this.isOpen.set(this.isOpenExternal);
    } else {
      this.isOpen.set(this.startOpen);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpenExternal'] && this.isOpenExternal !== undefined) {
      this.isUpdatingFromExternal = true;
      this.isOpen.set(this.isOpenExternal);
      this.isUpdatingFromExternal = false;
      this.cdr.markForCheck();
    }
  }

  toggle(): void {
    if (this.disabled) {
      this.attemptedToggle.emit();
      return;
    }

    if (this.isUpdatingFromExternal) {
      return;
    }

    const newState = !this.isOpen();
    this.isOpen.set(newState);
    this.toggled.emit(newState);
  }
}
