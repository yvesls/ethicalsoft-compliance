import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, forwardRef, HostListener, inject, Input, OnInit } from '@angular/core';
import { AbstractControl, ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { noop } from 'rxjs';

type SelectValue = string | number;

export interface SelectOption {
  value: SelectValue;
  label: string;
}

@Component({
  selector: 'app-select',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './select.component.html',
  styleUrls: ['./select.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SelectComponent),
      multi: true,
    },
  ],
})
export class SelectComponent implements ControlValueAccessor, OnInit {
  @Input() label = '';
  @Input() id = '';
  @Input() placeholder = 'Selecione';
  @Input() required = false;
  @Input() labelClasses = '';
  @Input({ transform: (value: boolean | string) => (typeof value === 'string' ? value === '' : !!value) })
  allowClear = false;
  @Input() validationMessages: Record<string, string> = {};
  @Input() control!: AbstractControl | null;

  private _options: SelectOption[] = [];
  @Input()
  set options(value: SelectOption[]) {
    this._options = value || [];
    this.updateSelectedLabel();
  }
  get options(): SelectOption[] {
    return this._options;
  }

  value: SelectValue | null = null;
  selectedLabel: string | null = null;
  disabled = false;
  touched = false;
  isOpen = false;

  private readonly el = inject(ElementRef<HTMLElement>);
  private readonly cdr = inject(ChangeDetectorRef);

  private onChange: (value: SelectValue | null) => void = () => { noop() };
  private onTouched: () => void = () => { noop() };

  ngOnInit(): void {
    if (!this.id) {
      this.id = this.label.toLowerCase().replaceAll(/\s/g, '-');
    }
  }

  writeValue(value: SelectValue | null): void {
    this.value = value ?? null;
    this.updateSelectedLabel();
    this.cdr.markForCheck();
  }

  registerOnChange(fn: (value: SelectValue | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.isOpen = false;
    }
    this.cdr.markForCheck();
  }

  private updateSelectedLabel(): void {
    if (this.value === null) {
      this.selectedLabel = null;
      return;
    }

    const selected = this.options.find((opt) => opt.value === this.value);
    this.selectedLabel = selected ? selected.label : null;
  }

  toggleDropdown(event: MouseEvent | KeyboardEvent): void {
    event.stopPropagation();
    if (event instanceof KeyboardEvent) {
      event.preventDefault();
    }
    if (this.disabled) return;
    this.isOpen = !this.isOpen;

    if (!this.isOpen && !this.touched) {
      this.onTouched();
    }
    this.cdr.markForCheck();
  }

  selectOption(option: SelectOption): void {
    if (this.disabled) return;

    this.value = option.value;
    this.selectedLabel = option.label;
    this.onChange(this.value);

    if (!this.touched) {
      this.onTouched();
    }

    this.isOpen = false;
    this.cdr.markForCheck();
  }

  clearSelection(event: Event): void {
    event.stopPropagation();
    if (this.disabled) return;

    this.value = null;
    this.selectedLabel = null;
    this.onChange(null);
    this.onTouched();
    this.isOpen = false;
    this.cdr.markForCheck();
  }

  onBlur(): void {
    if (!this.touched) {
      this.touched = true;
      this.onTouched();
    }
  }

  @HostListener('document:click', ['$event'])
  clickOutside(event: MouseEvent): void {
    const target = event.target as Node | null;
    if (this.isOpen && target && !this.el.nativeElement.contains(target)) {
      this.isOpen = false;
      this.onBlur();
      this.cdr.markForCheck();
    }
  }

  getErrorMessages(): string[] {
    if (!this.control?.errors) {
      return [];
    }
    const errors = this.control.errors;
    return Object.keys(errors).map((key) => {
      if (this.validationMessages[key]) {
        return this.validationMessages[key];
      }

      const errorValue = errors[key as keyof typeof errors];

      if (typeof errorValue === 'string') {
        return errorValue;
      }

      if (typeof errorValue === 'object' && errorValue && 'message' in errorValue) {
        return (errorValue as { message?: string }).message ?? 'Campo inválido';
      }

      return 'Campo inválido';
    });
  }
}
