import { Routes } from '@angular/router';
import { ProjectListPageComponent } from './pages/project-list-page/project-list-page.component';
import { CreateProjectPageComponent } from './pages/create-project-page/create-project-page.component';
import { CascataQuestionnaireFormComponent } from './pages/cascata-questionnaire-form/cascata-questionnaire-form.component';
import { IterativoQuestionnaireFormComponent } from './pages/iterativo-questionnaire-form/iterativo-questionnaire-form.component';

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
  {
    path: 'questionnaire/cascata',
    component: CascataQuestionnaireFormComponent,
  },
  {
    path: 'questionnaire/iterativo',
    component: IterativoQuestionnaireFormComponent,
  },
  // {
  //   path: ':id',
  //   loadComponent: () => import('./pages/project-detail-page/project-detail-page.component')...
  // },
];
