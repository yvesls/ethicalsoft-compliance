import { Routes } from '@angular/router';
import { ProjectListPageComponent } from './pages/project-list-page/project-list-page.component';
import { CreateProjectPageComponent } from './pages/create-project-page/create-project-page.component';
import { CascataQuestionnaireFormComponent } from './pages/cascata-questionnaire-form/cascata-questionnaire-form.component';
import { IterativoQuestionnaireFormComponent } from './pages/iterativo-questionnaire-form/iterativo-questionnaire-form.component';
import { QuestionnaireViewRedirectPageComponent } from './pages/questionnaire-view-redirect-page/questionnaire-view-redirect-page.component';

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

  // ── Dashboard ─────────────────────────────────────────────────────

  {
    path: ':projectId/questionnaires/:questionnaireId/dashboard/individual',
    loadComponent: () =>
      import('../../features/dashboard/pages/individual-dashboard-page/individual-dashboard-page.component').then(
        (m) => m.IndividualDashboardPageComponent
      ),
  },

  {
    path: ':projectId/questionnaires/:questionnaireId/dashboard',
    loadComponent: () =>
      import('../../features/dashboard/pages/questionnaire-dashboard-page/questionnaire-dashboard-page.component').then(
        (m) => m.QuestionnaireDashboardPageComponent
      ),
  },

  {
    path: ':projectId/dashboard',
    loadComponent: () =>
      import('../../features/dashboard/pages/project-dashboard-page/project-dashboard-page.component').then(
        (m) => m.ProjectDashboardPageComponent
      ),
  },

  {
    path: ':projectId/questionnaires/:questionnaireId',
    loadComponent: () =>
      import('./pages/questionnaire-response-page/questionnaire-response-page.component').then(
        (m) => m.QuestionnaireResponsePageComponent
      ),
  },
  {
    path: ':projectId/questionnaires/:questionnaireId/view',
    component: QuestionnaireViewRedirectPageComponent,
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/project-detail-page/project-detail-page.component').then(
        (m) => m.ProjectDetailPageComponent
      ),
  },
];

