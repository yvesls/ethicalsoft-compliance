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

export interface MultiSelectOption {
  value: any;
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
  @Input() validationMessages: { [key: string]: string } = {};
  @Input() control!: AbstractControl | null;
  @Input() options: MultiSelectOption[] = [];
  @Input() maxSelectedItems?: number;
  @Input() itemsNotRemovable: any[] = [];

  @ViewChild('dropdown') dropdownRef!: ElementRef;
  @ViewChild('selectContainer') selectContainerRef!: ElementRef;

  isOpen = signal(false);
  searchTerm = signal('');
  selectedValues: any[] = [];
  disabled = false;
  touched = false;

  private onChange = (value: any[]) => {};
  private onTouched = () => {};

  ngOnInit(): void {
    if (!this.id) {
      this.id = this.label.toLowerCase().replace(/\s/g, '-');
    }
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (
      this.selectContainerRef &&
      !this.selectContainerRef.nativeElement.contains(event.target)
    ) {
      this.closeDropdown();
    }
  }

  writeValue(value: any[]): void {
    this.selectedValues = value || [];
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
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

  isSelected(value: any): boolean {
    return this.selectedValues.includes(value);
  }

  isRemovable(value: any): boolean {
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
    } else {
      if (
        !this.maxSelectedItems ||
        this.selectedValues.length < this.maxSelectedItems
      ) {
        this.selectedValues = [...this.selectedValues, option.value];
      }
    }

    this.onChange(this.selectedValues);
  }

  removeSelectedItem(value: any, event: Event): void {
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

  getLabelForValue(value: any): string {
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

      if (typeof errors[key] === 'string') {
        return errors[key];
      }

      if (typeof errors[key] === 'object' && errors[key]?.message) {
        return errors[key].message;
      }
      return `Campo inválido`;
    });
  }
}
