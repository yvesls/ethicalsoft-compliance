import {
  Component,
  Input,
  forwardRef,
  ViewChild,
  ElementRef,
  HostListener,
  OnInit,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  AbstractControl,
} from '@angular/forms';
import { noop } from 'rxjs';

type MultiSelectValue = string | number;

export interface MultiSelectOption {
  value: MultiSelectValue;
  label: string;
}

@Component({
  selector: 'app-multi-select',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './multi-select.component.html',
  styleUrls: ['./multi-select.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MultiSelectComponent),
      multi: true,
    },
  ],
})
export class MultiSelectComponent implements ControlValueAccessor, OnInit {
  @Input() label = '';
  @Input() id = '';
  @Input() placeholder = 'Selecione...';
  @Input() required = false;
  @Input() labelClasses = '';
  @Input() validationMessages: Record<string, string> = {};
  @Input() control!: AbstractControl | null;
  @Input() options: MultiSelectOption[] = [];
  @Input() maxSelectedItems?: number;
  @Input() itemsNotRemovable: MultiSelectValue[] = [];

  @ViewChild('dropdown') dropdownRef!: ElementRef<HTMLDivElement>;
  @ViewChild('selectContainer') selectContainerRef!: ElementRef<HTMLDivElement>;

  isOpen = signal(false);
  searchTerm = signal('');
  selectedValues: MultiSelectValue[] = [];
  disabled = false;
  touched = false;

  private onChange: (value: MultiSelectValue[]) => void = () => { noop() };
  private onTouched: () => void = () => { noop() };

  ngOnInit(): void {
    if (!this.id) {
      this.id = this.label.toLowerCase().replaceAll(/\s/g, '-');
    }
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as Node | null;
    if (
      this.selectContainerRef &&
      target &&
      !this.selectContainerRef.nativeElement.contains(target)
    ) {
      this.closeDropdown();
    }
  }

  writeValue(value: MultiSelectValue[] | null): void {
    this.selectedValues = value ?? [];
  }

  registerOnChange(fn: (value: MultiSelectValue[]) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  toggleDropdown(): void {
    if (this.disabled) return;

    this.isOpen.set(!this.isOpen());

    if (!this.touched) {
      this.touched = true;
      this.onTouched();
    }
  }

  closeDropdown(): void {
    this.isOpen.set(false);
    this.searchTerm.set('');
  }

  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchTerm.set(value);
  }

  get filteredOptions(): MultiSelectOption[] {
    const search = this.searchTerm().toLowerCase();
    if (!search) {
      return this.options;
    }
    return this.options.filter((option) =>
      option.label.toLowerCase().includes(search)
    );
  }

  isSelected(value: MultiSelectValue): boolean {
    return this.selectedValues.includes(value);
  }

  isRemovable(value: MultiSelectValue): boolean {
    return !this.itemsNotRemovable.includes(value);
  }

  toggleOption(option: MultiSelectOption): void {
    const index = this.selectedValues.indexOf(option.value);

    if (index > -1) {
      if (this.isRemovable(option.value)) {
        this.selectedValues = this.selectedValues.filter(
          (v) => v !== option.value
        );
      }
    } else if (
      !this.maxSelectedItems ||
      this.selectedValues.length < this.maxSelectedItems
    ) {
      this.selectedValues = [...this.selectedValues, option.value];
    }

    this.onChange(this.selectedValues);
  }

  removeSelectedItem(value: MultiSelectValue, event: Event): void {
    event.stopPropagation();

    if (!this.isRemovable(value) || this.disabled) return;

    this.selectedValues = this.selectedValues.filter((v) => v !== value);
    this.onChange(this.selectedValues);
  }

  clearAll(event: Event): void {
    event.stopPropagation();

    if (this.disabled) return;

    this.selectedValues = this.selectedValues.filter(
      (v) => !this.isRemovable(v)
    );
    this.onChange(this.selectedValues);
  }

  getSelectedOptions(): MultiSelectOption[] {
    return this.options.filter((option) =>
      this.selectedValues.includes(option.value)
    );
  }

  get displayText(): string {
    const count = this.selectedValues.length;
    if (count === 0) return this.placeholder;
    if (count === 1) {
      const option = this.options.find(
        (o) => o.value === this.selectedValues[0]
      );
      return option?.label || this.placeholder;
    }
    return `${count} ${count === 1 ? 'item selecionado' : 'itens selecionados'}`;
  }

  get hasRemovableItems(): boolean {
    return this.selectedValues.some((v) => this.isRemovable(v));
  }

  getLabelForValue(value: MultiSelectValue): string {
    return this.options.find(o => o.value === value)?.label || '';
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
      return `Campo inválido`;
    });
  }
}
