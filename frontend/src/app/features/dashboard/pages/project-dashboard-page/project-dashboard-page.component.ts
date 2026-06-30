import { Component, inject, OnInit, signal, ChangeDetectionStrategy } from '@angular/core'
import { ActivatedRoute } from '@angular/router'
import { DecimalPipe, DatePipe } from '@angular/common'
import { DashboardService } from '../../services/dashboard.service'
import { ProjectIsepDashboardDTO, ProjectCloseResultDTO } from '../../interfaces/dashboard.interface'
import { BandBadgeComponent } from '../../components/band-badge/band-badge.component'
import { IsepKpiCardComponent } from '../../components/isep-kpi-card/isep-kpi-card.component'
import { IsepEvolutionChartComponent } from '../../components/isep-evolution-chart/isep-evolution-chart.component'
import { DebtEvolutionChartComponent } from '../../components/debt-evolution-chart/debt-evolution-chart.component'
import { GovernanceDimensionsChartComponent } from '../../components/governance-dimensions-chart/governance-dimensions-chart.component'
import { DebtIndicatorsWidgetComponent } from '../../components/debt-indicators-widget/debt-indicators-widget.component'
import { NotificationService } from '../../../../core/services/notification.service'
import { AuthenticationService } from '../../../../core/services/authentication.service'
import { RouterService } from '../../../../core/services/router.service'
import { ProjectContextService } from '../../../../core/services/project-context.service'
import { RoleEnum } from '../../../../shared/enums/role.enum'
import { DocumentEmissionService, DocumentEmissionRecordDTO } from '../../../../core/services/document-emission.service'
import { TranslateModule, TranslateService } from '@ngx-translate/core'
import { InfoExplainerComponent } from '../../../../shared/components/info-explainer/info-explainer.component'

@Component({
	selector: 'app-project-dashboard-page',
	standalone: true,
	imports: [
		DecimalPipe,
		DatePipe,
		TranslateModule,
		BandBadgeComponent,
		IsepKpiCardComponent,
		IsepEvolutionChartComponent,
		DebtEvolutionChartComponent,
		GovernanceDimensionsChartComponent,
		DebtIndicatorsWidgetComponent,
		InfoExplainerComponent,
	],
	templateUrl: './project-dashboard-page.component.html',
	changeDetection: ChangeDetectionStrategy.Eager,
	styleUrl: './project-dashboard-page.component.scss',
})
export class ProjectDashboardPageComponent implements OnInit {
	private readonly route = inject(ActivatedRoute)
	private readonly dashboardService = inject(DashboardService)
	private readonly notificationService = inject(NotificationService)
	private readonly authService = inject(AuthenticationService)
	readonly routerService = inject(RouterService)
	private readonly projectContextService = inject(ProjectContextService)

	private readonly documentEmissionService = inject(DocumentEmissionService)
	private readonly translate = inject(TranslateService)

	isAdmin = this.authService.userRoles$.value.includes(RoleEnum.ADMIN)

	projectId!: number
	dashboard = signal<ProjectIsepDashboardDTO | null>(null)
	loading = signal(true)
	closing = signal(false)
	closeResult = signal<ProjectCloseResultDTO | null>(null)
	registeringCertEmission = signal(false)
	downloadingCertPdf = signal(false)
	emissions = signal<DocumentEmissionRecordDTO[]>([])
	loadingEmissions = signal(false)

	get completionPercent(): number {
		const d = this.dashboard()
		if (!d || !d.totalQuestionnaires) return 0
		return (d.completedQuestionnaires / d.totalQuestionnaires) * 100
	}

	ngOnInit(): void {
		this.projectId = Number(this.route.snapshot.paramMap.get('projectId'))
		this.projectContextService.setCurrentProjectId(String(this.projectId))
		this.load()
		this.loadEmissions()
	}

	loadEmissions(): void {
		this.loadingEmissions.set(true)
		this.documentEmissionService.getEmissions(this.projectId, 'ETHICS_CERTIFICATE').subscribe({
			next: (list) => {
				this.emissions.set(list)
				this.loadingEmissions.set(false)
			},
			error: () => this.loadingEmissions.set(false),
		})
	}

	load(): void {
		this.loading.set(true)
		this.dashboardService.getProjectDashboard(this.projectId).subscribe({
			next: (data) => {
				this.dashboard.set(data)
				this.loading.set(false)
			},
			error: () => {
				this.notificationService.showError(this.translate.instant('dashboard.errors.load_project'))
				this.loading.set(false)
			},
		})
	}

	navigateToQuestionnaire(questionnaireId: number): void {
		this.routerService.rawNavigate(`/projects/${this.projectId}/questionnaires/${questionnaireId}/dashboard`)
	}

	navigateToConsolidatedResponses(): void {
		this.routerService.rawNavigate(`/projects/${this.projectId}/responses`)
	}

	downloadCsv(): void {
		this.dashboardService.exportProjectCsv(this.projectId).subscribe({
			next: (blob) => {
				const url = URL.createObjectURL(blob)
				const a = document.createElement('a')
				a.href = url
				a.download = `isep-projeto-${this.projectId}.csv`
				a.click()
				URL.revokeObjectURL(url)
			},
			error: () => this.notificationService.showError(this.translate.instant('dashboard.errors.export_csv')),
		})
	}

	closeProject(): void {
		this.notificationService.showConfirm(this.translate.instant('dashboard.project.close_confirm'), () => {
			this.closing.set(true)
			this.dashboardService.closeProject(this.projectId).subscribe({
				next: (result) => {
					this.closeResult.set(result)
					this.notificationService.showSuccess(
						this.translate.instant('dashboard.project.closed_success', {
							isep: result.isepPercent,
							band: result.band,
						})
					)
					this.load()
					this.closing.set(false)
				},
				error: () => {
					this.notificationService.showError(this.translate.instant('dashboard.errors.close_project'))
					this.closing.set(false)
				},
			})
		})
	}

	downloadCertificatePdf(): void {
		this.downloadingCertPdf.set(true)
		this.dashboardService.downloadCertificatePdf(this.projectId).subscribe({
			next: (blob) => {
				const url = URL.createObjectURL(blob)
				const a = document.createElement('a')
				a.href = url
				a.download = `certificado-${this.projectId}.pdf`
				a.click()
				URL.revokeObjectURL(url)
				this.downloadingCertPdf.set(false)
			},
			error: () => {
				this.notificationService.showError(this.translate.instant('dashboard.errors.certificate'))
				this.downloadingCertPdf.set(false)
			},
		})
	}

	registerCertificateEmission(): void {
		const confirmMsg = this.translate.instant('dashboard.certificate.register_emission_confirm')
		this.notificationService.showConfirm(confirmMsg, () => {
			this.registeringCertEmission.set(true)
			this.documentEmissionService.registerCertificateEmission(this.projectId).subscribe({
				next: (record) => {
					const msg = this.translate.instant('dashboard.certificate.emission_registered', {
						code: record.authenticityCode,
					})
					this.notificationService.showSuccess(msg)
					this.registeringCertEmission.set(false)
					this.loadEmissions()
				},
				error: () => {
					this.notificationService.showError(this.translate.instant('dashboard.errors.register_emission'))
					this.registeringCertEmission.set(false)
				},
			})
		})
	}
}
