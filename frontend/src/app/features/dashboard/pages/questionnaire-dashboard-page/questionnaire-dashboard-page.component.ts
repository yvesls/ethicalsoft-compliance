import { Component, inject, OnInit, signal, ChangeDetectionStrategy } from '@angular/core'
import { ActivatedRoute } from '@angular/router'
import { DecimalPipe, DatePipe } from '@angular/common'
import { forkJoin, of } from 'rxjs'
import { catchError } from 'rxjs/operators'
import { DashboardService } from '../../services/dashboard.service'
import {
	QuestionnaireIsepDashboardDTO,
	RoleStageComplianceDTO,
	WordCloudDTO,
	Band,
} from '../../interfaces/dashboard.interface'
import { BandBadgeComponent } from '../../components/band-badge/band-badge.component'
import { IsepKpiCardComponent } from '../../components/isep-kpi-card/isep-kpi-card.component'
import { BandDistributionChartComponent } from '../../components/band-distribution-chart/band-distribution-chart.component'
import { RoleStageHeatmapComponent } from '../../components/role-stage-heatmap/role-stage-heatmap.component'
import { RoleStageBarChartComponent } from '../../components/role-stage-bar-chart/role-stage-bar-chart.component'
import { GovernanceDimensionsChartComponent } from '../../components/governance-dimensions-chart/governance-dimensions-chart.component'
import { DebtIndicatorsWidgetComponent } from '../../components/debt-indicators-widget/debt-indicators-widget.component'
import { CategoryWordCloudWidgetComponent } from '../../components/category-word-cloud-widget/category-word-cloud-widget.component'
import { GovernanceInsightsWidgetComponent } from '../../components/governance-insights-widget/governance-insights-widget.component'
import { AiInsightsWidgetComponent } from '../../components/ai-insights-widget/ai-insights-widget.component'
import { AiExplainWidgetComponent } from '../../components/ai-explain-widget/ai-explain-widget.component'
import { AiRiskReportWidgetComponent } from '../../components/ai-risk-report-widget/ai-risk-report-widget.component'
import { AiChatWidgetComponent } from '../../components/ai-chat-widget/ai-chat-widget.component'
import { NotificationService } from '../../../../core/services/notification.service'
import { AuthenticationService } from '../../../../core/services/authentication.service'
import { ModalService } from '../../../../core/services/modal.service'
import { ResponseDetailModalComponent } from '../../components/response-detail-modal/response-detail-modal.component'
import { RouterService } from '../../../../core/services/router.service'
import { ProjectContextService } from '../../../../core/services/project-context.service'
import { RoleEnum } from '../../../../shared/enums/role.enum'
import {
	DocumentEmissionService,
	DocumentEmissionRecordDTO,
	BulletinEmissionResult,
} from '../../../../core/services/document-emission.service'
import { TranslateModule, TranslateService } from '@ngx-translate/core'
import { InfoExplainerComponent } from '../../../../shared/components/info-explainer/info-explainer.component'

@Component({
	selector: 'app-questionnaire-dashboard-page',
	standalone: true,
	imports: [
		DecimalPipe,
		DatePipe,
		TranslateModule,
		BandBadgeComponent,
		IsepKpiCardComponent,
		BandDistributionChartComponent,
		RoleStageHeatmapComponent,
		RoleStageBarChartComponent,
		GovernanceDimensionsChartComponent,
		DebtIndicatorsWidgetComponent,
		CategoryWordCloudWidgetComponent,
		GovernanceInsightsWidgetComponent,
		AiInsightsWidgetComponent,
		AiExplainWidgetComponent,
		AiRiskReportWidgetComponent,
		AiChatWidgetComponent,
		InfoExplainerComponent,
	],
	templateUrl: './questionnaire-dashboard-page.component.html',
	changeDetection: ChangeDetectionStrategy.Eager,
	styleUrl: './questionnaire-dashboard-page.component.scss',
})
export class QuestionnaireDashboardPageComponent implements OnInit {
	private readonly route = inject(ActivatedRoute)
	private readonly dashboardService = inject(DashboardService)
	private readonly notificationService = inject(NotificationService)
	private readonly authService = inject(AuthenticationService)
	private readonly modalService = inject(ModalService)
	private readonly routerService = inject(RouterService)
	private readonly projectContextService = inject(ProjectContextService)
	private readonly documentEmissionService = inject(DocumentEmissionService)
	private readonly translate = inject(TranslateService)

	projectId!: number
	questionnaireId!: number

	dashboard = signal<QuestionnaireIsepDashboardDTO | null>(null)
	roleStageData = signal<RoleStageComplianceDTO[]>([])
	wordCloud = signal<WordCloudDTO | null>(null)

	loading = signal(true)
	loadError = signal(false)
	forceClosing = signal(false)
	registeringBulletinEmission = signal(false)
	downloadingBulletinPdf = signal(false)
	emittingBulletin = signal(false)
	emissions = signal<DocumentEmissionRecordDTO[]>([])
	loadingEmissions = signal(false)

	isAdmin = this.authService.userRoles$.value.includes(RoleEnum.ADMIN)

	get bandDistribution() {
		return this.dashboard()?.bandDistribution ?? { A: 0, B: 0, C: 0, D: 0, E: 0 }
	}

	meetsMinimumBand(band: Band): boolean {
		return band === 'A' || band === 'B' || band === 'C'
	}

	readonly objectKeys = Object.keys

	ngOnInit(): void {
		this.projectId = Number(this.route.snapshot.paramMap.get('projectId'))
		this.questionnaireId = Number(this.route.snapshot.paramMap.get('questionnaireId'))
		this.projectContextService.setCurrentProjectId(String(this.projectId))
		this.load()
		this.loadEmissions()
	}

	loadEmissions(): void {
		this.loadingEmissions.set(true)
		this.documentEmissionService
			.getEmissions(this.projectId, 'NON_COMPLIANCE_BULLETIN', this.questionnaireId)
			.subscribe({
				next: (list) => {
					this.emissions.set(list)
					this.loadingEmissions.set(false)
				},
				error: () => this.loadingEmissions.set(false),
			})
	}

	load(): void {
		this.loading.set(true)
		this.loadError.set(false)
		forkJoin({
			dashboard: this.dashboardService
				.getQuestionnaireDashboard(this.projectId, this.questionnaireId)
				.pipe(catchError(() => of(null))),
			roleStage: this.dashboardService
				.getRoleStageDashboard(this.projectId, this.questionnaireId)
				.pipe(catchError(() => of([] as RoleStageComplianceDTO[]))),
			wordCloud: this.dashboardService
				.getWordCloud(this.projectId, this.questionnaireId)
				.pipe(catchError(() => of(null))),
		}).subscribe({
			next: ({ dashboard, roleStage, wordCloud }) => {
				if (dashboard && typeof dashboard === 'object' && 'questionnaireId' in dashboard) {
					this.dashboard.set(dashboard)
				} else {
					this.dashboard.set(null)
				}
				this.roleStageData.set(roleStage)
				this.wordCloud.set(wordCloud)
				this.loading.set(false)

				if (!this.dashboard()) {
					this.loadError.set(true)
					this.notificationService.showError(
						this.translate.instant('dashboard.errors.load_questionnaire_isep')
					)
				}
			},
			error: () => {
				this.loadError.set(true)
				this.notificationService.showError(this.translate.instant('dashboard.errors.load_questionnaire'))
				this.loading.set(false)
			},
		})
	}

	downloadCsv(): void {
		const url = this.dashboardService.getQuestionnaireCsvUrl(this.projectId, this.questionnaireId)
		window.open(url, '_blank')
	}

	downloadJson(): void {
		this.dashboardService.exportQuestionnaireJson(this.projectId, this.questionnaireId).subscribe({
			next: (data) => {
				const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
				const url = URL.createObjectURL(blob)
				const a = document.createElement('a')
				a.href = url
				a.download = `isep-questionario-${this.questionnaireId}.json`
				a.click()
				URL.revokeObjectURL(url)
			},
			error: () => this.notificationService.showError(this.translate.instant('dashboard.errors.export_json')),
		})
	}

	viewMemberResponses(representativeId: number, representativeName: string): void {
		this.modalService.open(ResponseDetailModalComponent, 'large-card', {
			projectId: this.projectId,
			questionnaireId: this.questionnaireId,
			representativeId,
			representativeName,
		})
	}

	navigateToConsolidatedResponses(): void {
		this.routerService.rawNavigate(`/projects/${this.projectId}/questionnaires/${this.questionnaireId}/responses`)
	}

	forceClose(): void {
		this.notificationService.showConfirm(
			this.translate.instant('dashboard.questionnaire.force_close_confirm'),
			() => {
				this.forceClosing.set(true)
				this.dashboardService.forceCloseQuestionnaire(this.projectId, this.questionnaireId).subscribe({
					next: () => {
						this.notificationService.showSuccess(
							this.translate.instant('dashboard.questionnaire.closed_success')
						)
						this.load()
						this.forceClosing.set(false)
					},
					error: () => {
						this.notificationService.showError(this.translate.instant('dashboard.errors.force_close'))
						this.forceClosing.set(false)
					},
				})
			}
		)
	}

	downloadBulletinPdf(): void {
		this.downloadingBulletinPdf.set(true)
		this.dashboardService.downloadBulletinPdf(this.projectId, this.questionnaireId).subscribe({
			next: (blob) => {
				const url = URL.createObjectURL(blob)
				const a = document.createElement('a')
				a.href = url
				a.download = `boletim-${this.questionnaireId}.pdf`
				a.click()
				URL.revokeObjectURL(url)
				this.downloadingBulletinPdf.set(false)
			},
			error: () => {
				this.notificationService.showError(this.translate.instant('dashboard.errors.bulletin_download'))
				this.downloadingBulletinPdf.set(false)
			},
		})
	}

	registerBulletinEmission(): void {
		const confirmMsg = this.translate.instant('dashboard.bulletin.register_emission_confirm')
		this.notificationService.showConfirm(confirmMsg, () => {
			this.registeringBulletinEmission.set(true)
			this.documentEmissionService.registerBulletinEmission(this.projectId, this.questionnaireId).subscribe({
				next: (record) => {
					const msg = this.translate.instant('dashboard.bulletin.emission_registered', {
						code: record.authenticityCode,
					})
					this.notificationService.showSuccess(msg)
					this.registeringBulletinEmission.set(false)
					this.loadEmissions()
				},
				error: () => {
					this.notificationService.showError(this.translate.instant('dashboard.errors.register_emission'))
					this.registeringBulletinEmission.set(false)
				},
			})
		})
	}

	emitBulletinToRepresentatives(): void {
		this.notificationService.showConfirm(this.translate.instant('dashboard.bulletin.emit_confirm'), () => {
			this.emittingBulletin.set(true)
			this.documentEmissionService.emitBulletinToRepresentatives(this.projectId, this.questionnaireId).subscribe({
				next: (result: BulletinEmissionResult) => {
					this.notificationService.showSuccess(
						this.translate.instant('dashboard.bulletin.emit_success', {
							count: result.sent,
							code: result.documentCode,
						})
					)
					this.emittingBulletin.set(false)
					this.loadEmissions()
				},
				error: () => {
					this.notificationService.showError(this.translate.instant('dashboard.errors.bulletin_emit'))
					this.emittingBulletin.set(false)
				},
			})
		})
	}
}
