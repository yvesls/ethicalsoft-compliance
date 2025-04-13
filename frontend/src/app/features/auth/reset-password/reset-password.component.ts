import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { InputComponent } from '../../../shared/components/input/input.component';
import { BasePageComponent, RestoreParams } from '../../../core/abstractions/base-page.component';
import { AuthStore } from '../../../shared/stores/auth.store';
import { NotificationService } from '../../../core/services/notification.service';
import { createResetPassword, ResetPasswordInterface } from '../../../shared/interfaces/auth/reset-password.interface';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, InputComponent],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.scss',
})
export class ResetPasswordComponent extends BasePageComponent {
  form!: FormGroup;
  resetPassword: ResetPasswordInterface = createResetPassword();

  constructor(
    private formBuilder: FormBuilder,
    private authStore: AuthStore,
    private notificationService: NotificationService
  ) {
    super();
  }

  protected override onInit(): void {
    this._initForm();
  }

  protected override save() {
    return {
      formValue: this.form.getRawValue(),
      resetPassword: this.resetPassword,
    };
  }

  protected override restore(restoreParameter: RestoreParams<any>): void {
    if (!restoreParameter.hasParams) return;

    if (restoreParameter['formValue']) {
      this.form.patchValue(restoreParameter['formValue']);
    }

    if (restoreParameter['resetPassword']) {
      this.resetPassword = restoreParameter['resetPassword'];
    }
  }

  protected override loadParams(params: any, queryParams?: any): void {
    if (!params) return;

    if (!this.resetPassword) {
        this.resetPassword = createResetPassword();
      }

    if (!!params.email) {
      this.resetPassword.email = params.email;
    }
    console.log(this.resetPassword, params)
  }

  private _initForm() {
    this.form = this.formBuilder.group({
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]],
    }, {
      validators: this._passwordsMatchValidator
    });
  }

  private _passwordsMatchValidator(group: FormGroup) {
    const password = group.get('password')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    return password === confirmPassword ? null : { passwordsMismatch: true };
  }

  reset() {
    if (this.form.valid) {
      const { password } = this.form.getRawValue();
      this.resetPassword.newPassword = password;

      this.authStore.resetPassword(this.resetPassword).subscribe({
        next: () => {
          this.notificationService.showSuccess('Senha redefinida com sucesso!');
          this.routerService.navigateTo('login');
        }, error: error => {
          this.notificationService.showError(error);
        }
      });
    }
  }
}
