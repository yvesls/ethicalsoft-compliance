import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { switchMap } from 'rxjs';
import { DashboardService } from '../../services/dashboard.service';
import { IndividualDashboardDTO } from '../../interfaces/dashboard.interface';
import { BandBadgeComponent } from '../../components/band-badge/band-badge.component';
import { IsepKpiCardComponent } from '../../components/isep-kpi-card/isep-kpi-card.component';
import { IsepEvolutionChartComponent } from '../../components/isep-evolution-chart/isep-evolution-chart.component';
import { IndividualRadarChartComponent } from '../../components/individual-radar-chart/individual-radar-chart.component';
import { NotificationService } from '../../../../core/services/notification.service';
import { AuthenticationService } from '../../../../core/services/authentication.service';
import { ProjectStore } from '../../../../shared/stores/project.store';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { InfoExplainerComponent } from '../../../../shared/components/info-explainer/info-explainer.component';

@Component({
  selector: 'app-individual-dashboard-page',
  standalone: true,
  imports: [
    DecimalPipe,
    TranslateModule,
    BandBadgeComponent,
    IsepKpiCardComponent,
    IsepEvolutionChartComponent,
    IndividualRadarChartComponent,
    InfoExplainerComponent,
  ],
  templateUrl: './individual-dashboard-page.component.html',
  styleUrl: './individual-dashboard-page.component.scss',
})
export class IndividualDashboardPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly dashboardService = inject(DashboardService);
  private readonly notificationService = inject(NotificationService);
  private readonly authService = inject(AuthenticationService);
  private readonly projectStore = inject(ProjectStore);
  private readonly translate = inject(TranslateService);

  projectId!: number;
  questionnaireId!: number;

  dashboard = signal<IndividualDashboardDTO | null>(null);
  loading = signal(true);

  ngOnInit(): void {
    this.projectId = Number(this.route.snapshot.paramMap.get('projectId'));
    this.questionnaireId = Number(this.route.snapshot.paramMap.get('questionnaireId'));
    this.load();
  }

  private load(): void {
    const currentEmail = this.authService.getCurrentUser()?.email;

    if (!currentEmail) {
      this.notificationService.showError(this.translate.instant('dashboard.errors.user_not_authenticated'));
      this.loading.set(false);
      return;
    }

    this.projectStore
      .getQuestionnaireSummary(String(this.projectId), this.questionnaireId)
      .pipe(
        switchMap((questionnaire) => {
          const respondent = questionnaire.respondents?.find(
            (r) => r.email?.trim().toLowerCase() === currentEmail.trim().toLowerCase()
          );

          if (!respondent) {
            throw new Error('Representante não encontrado no questionário.');
          }

          return this.dashboardService.getIndividualDashboard(
            this.projectId,
            this.questionnaireId,
            respondent.representativeId
          );
        })
      )
      .subscribe({
        next: (data) => {
          this.dashboard.set(data);
          this.loading.set(false);
        },
        error: (err: Error) => {
          const isRepNotFound = err?.message?.includes('Representante não encontrado');
          this.notificationService.showError(
            isRepNotFound
              ? this.translate.instant('dashboard.errors.representative_not_found')
              : this.translate.instant('dashboard.errors.load_individual')
          );
          this.loading.set(false);
        },
      });
  }
}
