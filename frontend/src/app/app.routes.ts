import { Routes } from '@angular/router';
import { LoginComponent } from './modules/auth/views/login/login.component';
import { HomeComponent } from './views/home/home.component';
import { AuthGuard } from './modules/auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'home',
    pathMatch: 'full'
  },
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'home',
    component: HomeComponent,
    canActivate: [AuthGuard]
  },
  {
    path: '**',
    redirectTo: 'home'
  }
];
