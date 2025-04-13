import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { HomeComponent } from './features/home/home.component';
import { NotFoundComponent } from './shared/components/not-found/not-found.component';
import { AuthGuard } from './core/guards/auth.guard';
import { LayoutGuard } from './core/guards/layout.guard';
import { RecoverComponent } from './features/auth/recover/recover.component';
import { CodeVerificationComponent } from './features/auth/code-verification/code-verification.component';
import { ResetPasswordComponent } from './features/auth/reset-password/reset-password.component';
import { NavigationSourceGuard } from './core/guards/navigation-source.guard';
import { RegisterComponent } from './features/auth/register/register.component';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'home',
    pathMatch: 'full'
  },
  {
    path: 'login',
    component: LoginComponent,
    canActivate: [LayoutGuard],
    data: { showLayout: false }
  },
  {
    path: 'home',
    component: HomeComponent,
    canActivate: [LayoutGuard, AuthGuard]
  },
  {
    path: 'recover',
    component: RecoverComponent,
    canActivate: [LayoutGuard],
    data: { showLayout: false }
  },
  {
    path: 'code-verification',
    component: CodeVerificationComponent,
    canActivate: [LayoutGuard, NavigationSourceGuard],
    data: { showLayout: false }
  },
  {
    path: 'reset-password',
    component: ResetPasswordComponent,
    canActivate: [LayoutGuard, NavigationSourceGuard],
    data: { showLayout: false }
  },
  {
    path: 'register',
    component: RegisterComponent,
    canActivate: [LayoutGuard],
    data: { showLayout: false }
  },
  {
    path: '**',
    component: NotFoundComponent,
    canActivate: [LayoutGuard],
    data: { showLayout: false }
  },
];
