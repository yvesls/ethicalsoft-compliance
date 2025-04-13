import { NotificationService } from './../../../core/services/notification.service';
import { createPasswordRecovery, PasswordRecoveryInterface } from '../../../shared/interfaces/auth/password-recovery.interface';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthStore } from '../../../shared/stores/auth.store';
import { CommonModule } from '@angular/common';
import { InputComponent } from '../../../shared/components/input/input.component';
import { BasePageComponent, RestoreParams } from '../../../core/abstractions/base-page.component';

@Component({
  selector: 'app-recover',
  imports: [CommonModule, ReactiveFormsModule, InputComponent],
  templateUrl: './recover.component.html',
  styleUrl: './recover.component.scss'
})
export class RecoverComponent extends BasePageComponent {
  form!: FormGroup;
  private passwordRecovery: PasswordRecoveryInterface = createPasswordRecovery();

  constructor(
    private formBuilder: FormBuilder,
    private authStore: AuthStore,
    private notificationService: NotificationService
  ) {
    super()
  }

  protected override onInit(): void {
    this._initForm();
  }

  protected override save() {
    return {
      formValue: this.form.getRawValue(),
    }
  }

  protected override restore(restoreParameter: RestoreParams<any>): void {
    if (!restoreParameter.hasParams) {
      return;
    }
    if (!!restoreParameter['formValue']) {
      this.form.patchValue(restoreParameter['formValue'])
    }
  }

  protected override loadParams(params: any, queryParams?: any): void {
  }

  private _initForm() {
    this.form = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  sendRecoveryEmail() {
    if (this.form.valid) {
      const email = this.form.getRawValue().email;
      this.passwordRecovery.email = email;

      this.authStore.recover(this.passwordRecovery).subscribe({
        next: () => {
          this.notificationService.showSuccess('Código de recuperação enviado com sucesso. Cheque seu email e siga as instruções.')
          this.routerService.navigateTo('code-verification', {
            params: {
              email: email,
            }
          })
        }, error: error => {
          this.notificationService.showError(error);
        }
      });
    }
  }
}
