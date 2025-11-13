import {
  CommonModule
} from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  Input,
  OnInit,
  signal,
  Output,
  EventEmitter
} from '@angular/core';

@Component({
  selector: 'app-accordion-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './accordion-panel.component.html',
  styleUrls: ['./accordion-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccordionPanelComponent implements OnInit {
  @Input() title: string = '';
  @Input() required: boolean = false;
  @Input() startOpen: boolean = false;

  @Output() toggled = new EventEmitter<void>();

  public isOpen = signal(false);

  ngOnInit(): void {
    this.isOpen.set(this.startOpen);
  }

  toggle(): void {
    this.isOpen.update(open => !open);

    this.toggled.emit();
  }
}
