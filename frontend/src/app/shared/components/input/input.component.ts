import { Component, Input, forwardRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, FormControl, AbstractControl } from '@angular/forms';

@Component({
  selector: 'app-input',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './input.component.html',
  styleUrls: ['./input.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => InputComponent),
      multi: true
    }
  ]
})
export class InputComponent implements ControlValueAccessor {
  @Input() label = '';
  @Input() type = 'text';
  @Input() id = '';
  @Input() placeholder = '';
  @Input() required = false;
  @Input() inputClasses = '';
  @Input() labelClasses = '';
  @Input() validationMessages: { [key: string]: string } = {};
  @Input() control!: AbstractControl | null;

  value: any = '';
  disabled = false;
  touched = false;

  private onChange = (value: any) => {};
  private onTouched = () => {};

  ngOnInit(): void {
    if (!this.id) {
      this.id = this.label.toLowerCase().replace(/\s/g, '-');
    }
  }

  writeValue(value: any): void {
    this.value = value;
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

  onInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.value = value;
    this.onChange(value);
  }

  onBlur(): void {
    if (!this.touched) {
      this.touched = true;
      this.onTouched();
    }
  }

  getErrorMessages(): string[] {
    if (!this.control?.errors) return [];
    return Object.keys(this.control.errors)
      .map(key => this.validationMessages[key] || `Campo inválido`);
  }
}
