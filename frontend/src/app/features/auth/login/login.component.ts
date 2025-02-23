import { AuthInterface } from './../../../shared/interfaces/auth.interface';
import { AuthenticationService } from './../../../core/sevices/authentication.service';
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputComponent } from '../../../shared/components/input/input.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, InputComponent],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {

  form!: FormGroup;
  authInterface = {} as AuthInterface

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthenticationService
  ) { }

  ngOnInit() {
    this._initForm();
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

}
