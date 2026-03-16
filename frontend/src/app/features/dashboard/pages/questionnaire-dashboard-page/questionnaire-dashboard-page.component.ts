import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DecimalPipe, DatePipe } from '@angular/common';
import { forkJoin } from 'rxjs';
import { DashboardService } from '../../services/dashboard.service';
import {
  QuestionnaireIsepDashboardDTO,
  RoleStageComplianceDTO,
  WordCloudDTO,
} from '../../interfaces/dashboard.interface';
import { BandBadgeComponent } from '../../components/band-badge/band-badge.component';
import { IsepKpiCardComponent } from '../../components/isep-kpi-card/isep-kpi-card.component';
import { BandDistributionChartComponent } from '../../components/band-distribution-chart/band-distribution-chart.component';
import { RoleStageHeatmapComponent } from '../../components/role-stage-heatmap/role-stage-heatmap.component';
import { RoleStageBarChartComponent } from '../../components/role-stage-bar-chart/role-stage-bar-chart.component';
import { WordCloudWidgetComponent } from '../../components/word-cloud-widget/word-cloud-widget.component';
import { NotificationService } from '../../../../core/services/notification.service';
import { AuthenticationService } from '../../../../core/services/authentication.service';
import { RouterService } from '../../../../core/services/router.service';
import { RoleEnum } from '../../../../shared/enums/role.enum';

@Component({
  selector: 'app-questionnaire-dashboard-page',
  standalone: true,
  imports: [
    DecimalPipe,
    DatePipe,
    BandBadgeComponent,
    IsepKpiCardComponent,
    BandDistributionChartComponent,
    RoleStageHeatmapComponent,
    RoleStageBarChartComponent,
    WordCloudWidgetComponent,
  ],
  templateUrl: './questionnaire-dashboard-page.component.html',
  styleUrl: './questionnaire-dashboard-page.component.scss',
})
export class QuestionnaireDashboardPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly dashboardService = inject(DashboardService);
  private readonly notificationService = inject(NotificationService);
  private readonly authService = inject(AuthenticationService);
  readonly routerService = inject(RouterService);

  projectId!: number;
  questionnaireId!: number;

  dashboard = signal<QuestionnaireIsepDashboardDTO | null>(null);
  roleStageData = signal<RoleStageComplianceDTO[]>([]);
  wordCloud = signal<WordCloudDTO | null>(null);

  loading = signal(true);
  forceClosing = signal(false);

  isAdmin = this.authService.userRoles$.value.includes(RoleEnum.ADMIN);

  get bandDistribution() {
    return this.dashboard()?.bandDistribution ?? { A: 0, B: 0, C: 0, D: 0, E: 0 };
  }

  ngOnInit(): void {
    this.projectId = Number(this.route.snapshot.paramMap.get('projectId'));
    this.questionnaireId = Number(this.route.snapshot.paramMap.get('questionnaireId'));
    this.load();
  }

  load(): void {
    this.loading.set(true);
    forkJoin({
      dashboard: this.dashboardService.getQuestionnaireDashboard(this.projectId, this.questionnaireId),
      roleStage: this.dashboardService.getRoleStageDashboard(this.projectId, this.questionnaireId),
      wordCloud: this.dashboardService.getWordCloud(this.projectId, this.questionnaireId),
    }).subscribe({
      next: ({ dashboard, roleStage, wordCloud }) => {
        this.dashboard.set(dashboard);
        this.roleStageData.set(roleStage);
        this.wordCloud.set(wordCloud);
        this.loading.set(false);
      },
      error: () => {
        this.notificationService.showError('Não foi possível carregar o dashboard do questionário.');
        this.loading.set(false);
      },
    });
  }

  downloadCsv(): void {
    const url = this.dashboardService.getQuestionnaireCsvUrl(this.projectId, this.questionnaireId);
    window.open(url, '_blank');
  }

  downloadJson(): void {
    this.dashboardService.exportQuestionnaireJson(this.projectId, this.questionnaireId).subscribe({
      next: (data) => {
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `isep-questionario-${this.questionnaireId}.json`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.notificationService.showError('Erro ao exportar JSON.'),
    });
  }

  forceClose(): void {
    if (!confirm('Tem certeza que deseja encerrar o questionário? O ISEP será calculado com as respostas existentes.')) return;
    this.forceClosing.set(true);
    this.dashboardService.forceCloseQuestionnaire(this.projectId, this.questionnaireId).subscribe({
      next: () => {
        this.notificationService.showSuccess('Questionário encerrado. ISEP será calculado.');
        this.load();
        this.forceClosing.set(false);
      },
      error: () => {
        this.notificationService.showError('Erro ao encerrar o questionário.');
        this.forceClosing.set(false);
      },
    });
  }

  goBack(): void {
    this.routerService.rawNavigate(`/projects/${this.projectId}/dashboard`);
  }
}
