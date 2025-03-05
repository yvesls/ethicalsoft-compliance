import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { HomeComponent } from './features/home/home.component';
import { NotFoundComponent } from './shared/components/not-found/not-found.component';
import { AuthGuard } from './core/guards/auth.guard';
import { LayoutGuard } from './core/guards/layout.guard';

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
    canActivate: [LayoutGuard, AuthGuard],
    data: { showLayout: true }
  },
  {
    path: '**',
    component: NotFoundComponent,
    canActivate: [LayoutGuard],
    data: { showLayout: false }
  },
];
