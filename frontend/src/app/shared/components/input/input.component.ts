import { Component, Input, forwardRef, OnInit } from '@angular/core'
import { CommonModule } from '@angular/common'
import { ControlValueAccessor, NG_VALUE_ACCESSOR, AbstractControl } from '@angular/forms'
import { noop } from 'rxjs'
import { capitalizeWords } from '../../../core/utils/common-utils'

type InputValue = string | number | null

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
      multi: true,
    },
  ],
})
export class InputComponent implements ControlValueAccessor, OnInit {
  @Input() label = ''
  @Input() type = 'text'
  @Input() id = ''
  @Input() placeholder = ''
  @Input() required = false
  @Input() inputClasses = ''
  @Input() labelClasses = ''
  @Input() validationMessages: Record<string, string> = {}
  @Input() control!: AbstractControl | null
  @Input() autoCapitalize = false
  @Input() readonly = false

  public currentType = 'text'
  public isPasswordVisible = false

  value: InputValue = null
  disabled = false
  touched = false

  private onChange: (value: InputValue) => void = () => { noop() }
  private onTouched: () => void = () => { noop() }

  ngOnInit(): void {
    if (!this.id) {
  this.id = this.label.toLowerCase().replaceAll(/\s/g, '-')
    }
    this.currentType = this.type;

    if (this.type === 'date') {
      this.currentType = 'text';
    }
  }

  writeValue(value: unknown): void {
    this.value = (value as InputValue) ?? null
    if (this.type === 'date' && value) {
      this.currentType = 'date';
    } else if (this.type === 'date' && !value) {
      this.currentType = 'text';
    }
  }

  registerOnChange(fn: (value: InputValue) => void): void {
    this.onChange = fn
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled
  }

  onInput(event: Event): void {
    if (this.disabled || this.readonly) {
      return;
    }

    let value = (event.target as HTMLInputElement).value

    if (this.autoCapitalize && this.type === 'text') {
      value = capitalizeWords(value);
      (event.target as HTMLInputElement).value = value;
    }

    this.value = value
    this.onChange(value)
  }

  onFocus(): void {
    if (this.type === 'date') {
      this.currentType = 'date';
    }
  }

  onBlur(): void {
    if (!this.touched) {
      this.touched = true
      this.onTouched()
    }

    if (this.autoCapitalize && this.type === 'text' && typeof this.value === 'string' && this.value) {
      const capitalizedValue = capitalizeWords(this.value);
      if (capitalizedValue !== this.value) {
        this.value = capitalizedValue;
        this.onChange(capitalizedValue);
      }
    }

    if (this.type === 'date' && !this.value) {
      this.currentType = 'text';
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
      return `Campo inválido`;
    });
  }

  togglePasswordVisibility(): void {
    this.isPasswordVisible = !this.isPasswordVisible;
    this.currentType = this.isPasswordVisible ? 'text' : 'password';
  }
}
