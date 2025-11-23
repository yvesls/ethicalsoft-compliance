import { Component, Input, forwardRef } from '@angular/core'
import { CommonModule } from '@angular/common'
import { ControlValueAccessor, NG_VALUE_ACCESSOR, AbstractControl } from '@angular/forms'
import { capitalizeWords } from '../../../core/utils/common-utils'

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
export class InputComponent implements ControlValueAccessor {
  @Input() label = ''
  @Input() type = 'text'
  @Input() id = ''
  @Input() placeholder = ''
  @Input() required = false
  @Input() inputClasses = ''
  @Input() labelClasses = ''
  @Input() validationMessages: { [key: string]: string } = {}
  @Input() control!: AbstractControl | null
  @Input() autoCapitalize = false
  @Input() readonly = false

  public currentType = 'text'
  public isPasswordVisible = false

  value: any = ''
  disabled = false
  touched = false

  private onChange = (value: any) => {}
  private onTouched = () => {}

  ngOnInit(): void {
    if (!this.id) {
      this.id = this.label.toLowerCase().replace(/\s/g, '-')
    }
    this.currentType = this.type;

    if (this.type === 'date') {
      this.currentType = 'text';
    }
  }

  writeValue(value: any): void {
    this.value = value
    if (this.type === 'date' && value) {
      this.currentType = 'date';
    } else if (this.type === 'date' && !value) {
      this.currentType = 'text';
    }
  }

  registerOnChange(fn: any): void {
    this.onChange = fn
  }

  registerOnTouched(fn: any): void {
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

    if (this.autoCapitalize && this.type === 'text' && this.value) {
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

      if (typeof errors[key] === 'string') {
        return errors[key];
      }

      if (typeof errors[key] === 'object' && errors[key]?.message) {
        return errors[key].message;
      }
      return `Campo inválido`;
    });
  }

  togglePasswordVisibility(): void {
    this.isPasswordVisible = !this.isPasswordVisible;
    this.currentType = this.isPasswordVisible ? 'text' : 'password';
  }
}
