import { Routes } from '@angular/router';
import { ProjectListPageComponent } from './pages/project-list-page/project-list-page.component';

export const PROJECTS_ROUTES: Routes = [
  {
    path: '',
    component: ProjectListPageComponent,
    pathMatch: 'full',
  },
  // {
  //   path: 'new',
  //   loadComponent: () => import('./pages/project-create-page/project-create-page.component')...
  // },
  // {
  //   path: ':id',
  //   loadComponent: () => import('./pages/project-detail-page/project-detail-page.component')...
  // },
];
