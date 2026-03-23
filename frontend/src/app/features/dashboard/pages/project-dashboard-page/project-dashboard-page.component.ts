import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DecimalPipe, DatePipe } from '@angular/common';
import { DashboardService } from '../../services/dashboard.service';
import {
  ProjectIsepDashboardDTO,
  ProjectCloseResultDTO,
} from '../../interfaces/dashboard.interface';
import { BandBadgeComponent } from '../../components/band-badge/band-badge.component';
import { IsepKpiCardComponent } from '../../components/isep-kpi-card/isep-kpi-card.component';
import { IsepEvolutionChartComponent } from '../../components/isep-evolution-chart/isep-evolution-chart.component';
import { DebtEvolutionChartComponent } from '../../components/debt-evolution-chart/debt-evolution-chart.component';
import { GovernanceDimensionsChartComponent } from '../../components/governance-dimensions-chart/governance-dimensions-chart.component';
import { DebtIndicatorsWidgetComponent } from '../../components/debt-indicators-widget/debt-indicators-widget.component';
import { NotificationService } from '../../../../core/services/notification.service';
import { AuthenticationService } from '../../../../core/services/authentication.service';
import { RouterService } from '../../../../core/services/router.service';
import { RoleEnum } from '../../../../shared/enums/role.enum';

@Component({
  selector: 'app-project-dashboard-page',
  standalone: true,
  imports: [
    DecimalPipe,
    DatePipe,
    BandBadgeComponent,
    IsepKpiCardComponent,
    IsepEvolutionChartComponent,
    DebtEvolutionChartComponent,
    GovernanceDimensionsChartComponent,
    DebtIndicatorsWidgetComponent,
  ],
  templateUrl: './project-dashboard-page.component.html',
  styleUrl: './project-dashboard-page.component.scss',
})
export class ProjectDashboardPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly dashboardService = inject(DashboardService);
  private readonly notificationService = inject(NotificationService);
  private readonly authService = inject(AuthenticationService);
  readonly routerService = inject(RouterService);

  isAdmin = this.authService.userRoles$.value.includes(RoleEnum.ADMIN);

  projectId!: number;
  dashboard = signal<ProjectIsepDashboardDTO | null>(null);
  loading = signal(true);
  closing = signal(false);
  closeResult = signal<ProjectCloseResultDTO | null>(null);

  get completionPercent(): number {
    const d = this.dashboard();
    if (!d || !d.totalQuestionnaires) return 0;
    return (d.completedQuestionnaires / d.totalQuestionnaires) * 100;
  }

  ngOnInit(): void {
    this.projectId = Number(this.route.snapshot.paramMap.get('projectId'));
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.dashboardService.getProjectDashboard(this.projectId).subscribe({
      next: (data) => {
        this.dashboard.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.notificationService.showError('Não foi possível carregar o dashboard do projeto.');
        this.loading.set(false);
      },
    });
  }

  navigateToQuestionnaire(questionnaireId: number): void {
    this.routerService.rawNavigate(`/projects/${this.projectId}/questionnaires/${questionnaireId}/dashboard`);
  }

  downloadCsv(): void {
    const url = this.dashboardService.getProjectCsvUrl(this.projectId);
    window.open(url, '_blank');
  }

  closeProject(): void {
    if (!confirm('Tem certeza que deseja encerrar o projeto? O ISEP consolidado será calculado com os questionários disponíveis.')) return;
    this.closing.set(true);
    this.dashboardService.closeProject(this.projectId).subscribe({
      next: (result) => {
        this.closeResult.set(result);
        this.notificationService.showSuccess(`Projeto encerrado. ISEP: ${result.isepPercent}% – Faixa ${result.band}`);
        this.load();
        this.closing.set(false);
      },
      error: () => {
        this.notificationService.showError('Erro ao encerrar o projeto.');
        this.closing.set(false);
      },
    });
  }
}
