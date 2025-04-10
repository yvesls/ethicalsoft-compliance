import { RouterService } from './../../../core/services/router.service';
import { AuthInterface } from '../../../shared/interfaces/auth/auth.interface';
import { AuthenticationService } from '../../../core/services/authentication.service';
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputComponent } from '../../../shared/components/input/input.component';
import { BasePageComponent, RestoreParams } from '../../../core/abstractions/base-page.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, InputComponent],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent extends BasePageComponent {

  form!: FormGroup;
  authInterface = {} as AuthInterface

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthenticationService
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

    if (restoreParameter['formValue']) {
      this.form.patchValue(restoreParameter['formValue'])
    }
  }

  protected override loadParams(params: any, queryParams?: any): void {
  }

  private _initForm() {
    this.form = this.formBuilder.group({
      username: [, Validators.compose([Validators.email, Validators.required])],
      password: [,Validators.required],
      keepSession: [false],
    })
  }

  login() {
    if(this.form.valid) {
      var form = this.form.getRawValue();
      this.authInterface.username = form.username;
      this.authInterface.password = form.password;
      this.authService.login(this.authInterface);
    }
  }

  goToRecover() {
    this.routerService.navigateTo('recover');
  }

}
