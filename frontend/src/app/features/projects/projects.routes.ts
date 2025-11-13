import { Routes } from '@angular/router';
import { ProjectListPageComponent } from './pages/project-list-page/project-list-page.component';
import { CreateProjectPageComponent } from './pages/create-project-page/create-project-page.component';

export const PROJECTS_ROUTES: Routes = [
  {
    path: '',
    component: ProjectListPageComponent,
    pathMatch: 'full',
  },
  {
    path: 'create',
    component: CreateProjectPageComponent,
  },
  // {
  //   path: ':id',
  //   loadComponent: () => import('./pages/project-detail-page/project-detail-page.component')...
  // },
];
