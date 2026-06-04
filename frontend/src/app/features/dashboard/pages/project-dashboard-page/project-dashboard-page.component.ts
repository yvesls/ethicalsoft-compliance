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
import { ProjectContextService } from '../../../../core/services/project-context.service';
import { DocumentEmissionService } from '../../../../core/services/document-emission.service';
import { RoleEnum } from '../../../../shared/enums/role.enum';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

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
    TranslateModule,
  ],
  templateUrl: './project-dashboard-page.component.html',
  styleUrl: './project-dashboard-page.component.scss',
})
export class ProjectDashboardPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly dashboardService = inject(DashboardService);
  private readonly notificationService = inject(NotificationService);
  private readonly authService = inject(AuthenticationService);
  private readonly documentEmissionService = inject(DocumentEmissionService);
  readonly routerService = inject(RouterService);
  private readonly projectContextService = inject(ProjectContextService);

  private readonly translate = inject(TranslateService);

  isAdmin = this.authService.userRoles$.value.includes(RoleEnum.ADMIN);

  projectId!: number;
  dashboard = signal<ProjectIsepDashboardDTO | null>(null);
  loading = signal(true);
  closing = signal(false);
  closeResult = signal<ProjectCloseResultDTO | null>(null);
  downloadingCertificate = signal(false);
  registeringCertificateEmission = signal(false);

  get completionPercent(): number {
    const d = this.dashboard();
    if (!d || !d.totalQuestionnaires) return 0;
    return (d.completedQuestionnaires / d.totalQuestionnaires) * 100;
  }

  ngOnInit(): void {
    this.projectId = Number(this.route.snapshot.paramMap.get('projectId'));
    this.projectContextService.setCurrentProjectId(String(this.projectId));
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
        this.notificationService.showError(this.translate.instant('dashboard.errors.load_project'));
        this.loading.set(false);
      },
    });
  }

  navigateToQuestionnaire(questionnaireId: number): void {
    this.routerService.rawNavigate(`/projects/${this.projectId}/questionnaires/${questionnaireId}/dashboard`);
  }

  navigateToConsolidatedResponses(): void {
    this.routerService.rawNavigate(`/projects/${this.projectId}/responses`);
  }

  downloadCsv(): void {
    this.dashboardService.exportProjectCsv(this.projectId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `isep-projeto-${this.projectId}.csv`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.notificationService.showError(this.translate.instant('dashboard.errors.export_csv')),
    });
  }

  downloadCertificate(): void {
    this.downloadingCertificate.set(true);
    this.dashboardService.downloadCertificate(this.projectId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `certificado-conformidade-${this.projectId}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.downloadingCertificate.set(false);
      },
      error: (err) => {
        this.notificationService.showError(err?.message ?? this.translate.instant('dashboard.errors.certificate'));
        this.downloadingCertificate.set(false);
      },
    });
  }

  registerCertificateEmission(): void {
    const confirmMessage = this.translate.instant('dashboard.certificate.register_emission_confirm');
    this.notificationService.showConfirm(
      confirmMessage,
      () => {
        this.registeringCertificateEmission.set(true);
        this.documentEmissionService.registerCertificateEmission(this.projectId).subscribe({
          next: (record) => {
            this.notificationService.showSuccess(
              this.translate.instant('dashboard.certificate.emission_registered', {
                code: record.authenticityCode,
              })
            );
            this.registeringCertificateEmission.set(false);
          },
          error: (err) => {
            const serverMsg = err?.error instanceof SyntaxError ? null : err?.error?.message;
            this.notificationService.showError(
              serverMsg ?? this.translate.instant('dashboard.errors.register_emission')
            );
            this.registeringCertificateEmission.set(false);
          },
        });
      }
    );
  }

  closeProject(): void {
    this.notificationService.showConfirm(
      this.translate.instant('dashboard.project.close_confirm'),
      () => {
        this.closing.set(true);
        this.dashboardService.closeProject(this.projectId).subscribe({
          next: (result) => {
            this.closeResult.set(result);
            this.notificationService.showSuccess(
              this.translate.instant('dashboard.project.closed_success', { isep: result.isepPercent, band: result.band })
            );
            this.load();
            this.closing.set(false);
          },
          error: () => {
            this.notificationService.showError(this.translate.instant('dashboard.errors.close_project'));
            this.closing.set(false);
          },
        });
      }
    );
  }
}
