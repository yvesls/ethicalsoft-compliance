import {
	Component,
	inject,
	OnInit,
	OnDestroy,
	signal,
	WritableSignal,
	ChangeDetectorRef,
	computed,
	ChangeDetectionStrategy,
} from '@angular/core'

import { ReactiveFormsModule, FormBuilder, FormGroup, FormsModule, Validators } from '@angular/forms'
import { TranslateModule, TranslateService } from '@ngx-translate/core'

import { BasePageComponent, RestoreParams } from '../../../../core/abstractions/base-page.component'
import { LoggerService } from '../../../../core/services/logger.service'
import { ModalService } from '../../../../core/services/modal.service'
import { NotificationService } from '../../../../core/services/notification.service'
import { ProjectType } from '../../../../shared/enums/project-type.enum'
import { ActionType } from '../../../../shared/enums/action-type.enum'
import { QuestionClassificationType } from '../../../../shared/enums/question-classification-type.enum'
import { QuestionType } from '../../../../shared/enums/question-type.enum'
import {
	QuestionModalComponent,
	QuestionData,
	QuestionStageConfig,
} from '../../components/question-modal/question-modal.component'
import { AccordionPanelComponent } from '../../../../shared/components/accordion-panel/accordion-panel.component'
import { InputComponent } from '../../../../shared/components/input/input.component'
import { SelectComponent, SelectOption } from '../../../../shared/components/select/select.component'
import { Subscription } from 'rxjs'
import { take } from 'rxjs/operators'
import { GenericParams, RouteParams } from '../../../../core/services/router.service'
import { RoleService } from '../../../../core/services/role.service'
import { QuestionnaireQueryStore } from '../../../../shared/stores/questionnaire-query.store'
import {
	QuestionnaireQuestionResponse,
	QuestionnaireRawResponse,
} from '../../../../shared/interfaces/questionnaire/questionnaire-query.interface'
import { Page } from '../../../../shared/interfaces/pageable.interface'
import { PaginationComponent } from '../../../../shared/components/pagination/pagination.component'
import { RoleSummary } from '../../../../shared/interfaces/role/role-summary.interface'
import { UnlinkedItemsPanelComponent } from '../../../../shared/components/unlinked-items-panel/unlinked-items-panel.component'
import { InfoExplainerComponent } from '../../../../shared/components/info-explainer/info-explainer.component'

interface IterativoQuestionnaireRouteParams extends GenericParams {
	questionnaireIndex?: number
	sequence?: number | string
	projectId?: string
	questionnaireId?: number
	mode?: ActionType
	projectName?: string
	name?: string
	weight?: number
	iteration?: string
	questions?: QuestionData[]
	stages?: string[]
	representativeRoleIds?: number[]
	otherQuestionnairesQuestions?: QuestionData[]
	returnTo?: string
	returnToEdit?: boolean
	editProjectId?: string
}

const PENDING_QUESTIONNAIRE_UPDATE_KEY = 'pendingQuestionnaireUpdate'

type IterativoQuestionnaireRestoreParams = RestoreParams<IterativoQuestionnaireRouteParams>

@Component({
	selector: 'app-iterativo-questionnaire-form',
	standalone: true,
	imports: [
		ReactiveFormsModule,
		FormsModule,
		AccordionPanelComponent,
		InputComponent,
		SelectComponent,
		PaginationComponent,
		TranslateModule,
		UnlinkedItemsPanelComponent,
		InfoExplainerComponent,
	],
	templateUrl: './iterativo-questionnaire-form.component.html',
	changeDetection: ChangeDetectionStrategy.Eager,
	styleUrls: ['./iterativo-questionnaire-form.component.scss'],
})
export class IterativoQuestionnaireFormComponent
	extends BasePageComponent<IterativoQuestionnaireRouteParams>
	implements OnInit, OnDestroy
{
	private static readonly _paginationComponent = PaginationComponent
	private modalService = inject(ModalService)
	private fb = inject(FormBuilder)
	private cdr = inject(ChangeDetectorRef)
	private notificationService = inject(NotificationService)
	private readonly translate = inject(TranslateService)
	private roleService = inject(RoleService)
	private questionnaireQueryStore = inject(QuestionnaireQueryStore)
	private questionnaireIndex: number | null = null
	private questionnaireMetadata: IterativoQuestionnaireRouteParams | null = null
	private skipStatePersistence = false
	private stageSelectionConfig?: QuestionStageConfig
	private returnTo: string | null = null
	private allowedRoleIds: number[] = []

	readonly mode = signal<ActionType>(ActionType.EDIT)
	readonly isViewMode = signal(false)
	readonly isLoadingQuestions = signal(false)
	readonly viewProjectId = signal<string | null>(null)
	readonly viewQuestionnaireId = signal<number | null>(null)
	readonly pagination = signal<Page<unknown> | null>(null)
	readonly currentPage = signal(0)
	readonly pageSize = signal(10)

	private roleNameById = new Map<number, string>()
	private _rolesReady = signal(false)
	private _projectStageNames = signal<string[]>([])
	private _otherProjectQuestions: QuestionData[] = []

	form!: FormGroup
	questions: WritableSignal<QuestionData[]> = signal([])
	projectName: WritableSignal<string> = signal('')

	questionnaireDataAccordionOpen = true
	questionsAccordionOpen = true

	searchTerm = ''
	selectedRole = ''
	selectedQuestionIds = signal<Set<string>>(new Set())

	roleFilterOptions: SelectOption[] = []
	readonly outOfProjectRoles = computed(() => {
		this._rolesReady()
		if (!this.allowedRoleIds.length) return []
		const covered = new Set([
			...this._otherProjectQuestions.flatMap((q) => q.roleIds ?? []),
			...this.questions().flatMap((q) => q.roleIds ?? []),
		])
		return this.allowedRoleIds.filter((id) => !covered.has(id)).map((id) => this.roleNameById.get(id) ?? `ID:${id}`)
	})
	readonly outOfProjectStages = computed(() => {
		const stages = this._projectStageNames()
		if (!stages.length) return []
		const covered = new Set([
			...this._otherProjectQuestions.flatMap((q) => q.stageNames ?? []),
			...this.questions().flatMap((q) => q.stageNames ?? []),
		])
		return stages.filter((s) => !covered.has(s))
	})

	readonly orphanedRolesInQuestions = computed(() => {
		this._rolesReady()
		if (!this.allowedRoleIds.length) return []
		const validRoleIds = new Set(this.allowedRoleIds)
		const orphanedNames = new Set<string>()
		this.questions().forEach((q) => {
			;(q.roleIds ?? []).forEach((id) => {
				if (!validRoleIds.has(id)) orphanedNames.add(this.roleNameById.get(id) ?? `ID:${id}`)
			})
		})
		return Array.from(orphanedNames)
	})

	readonly orphanedStagesInQuestions = computed(() => {
		const validStages = new Set(this._projectStageNames())
		if (!validStages.size) return []
		const orphanedStages = new Set<string>()
		this.questions().forEach((q) => {
			;(q.stageNames ?? []).forEach((s) => {
				if (!validStages.has(s)) orphanedStages.add(s)
			})
		})
		return Array.from(orphanedStages)
	})

	readonly invalidQuestionIds = computed(() => {
		this._rolesReady()
		const validRoleIds = new Set(this.allowedRoleIds)
		const validStages = new Set(this._projectStageNames())
		const invalidIds = new Set<string>()
		this.questions().forEach((q) => {
			const hasOrphanedRole = validRoleIds.size > 0 && (q.roleIds ?? []).some((id) => !validRoleIds.has(id))
			const hasOrphanedStage = validStages.size > 0 && (q.stageNames ?? []).some((s) => !validStages.has(s))
			if ((hasOrphanedRole || hasOrphanedStage) && q.id) invalidIds.add(q.id)
		})
		return invalidIds
	})

	private questionModalSubscription?: Subscription

	constructor() {
		super()
	}

	override ngOnInit(): void {
		super.ngOnInit()
		this.loadRoleFilterOptions()
	}

	protected override onInit(): void {
		this.initializeForm()
	}

	protected override loadParams(params: RouteParams<IterativoQuestionnaireRouteParams>): void {
		if (!params) return

		const data = (params.p ?? params) as IterativoQuestionnaireRouteParams

		const incomingMode = data.mode ?? ActionType.EDIT
		this.mode.set(incomingMode)
		this.isViewMode.set(incomingMode === ActionType.VIEW)
		this.viewProjectId.set(typeof data.projectId === 'string' ? data.projectId : null)
		this.viewQuestionnaireId.set(typeof data.questionnaireId === 'number' ? data.questionnaireId : null)
		if (typeof data.returnTo === 'string') {
			this.returnTo = data.returnTo
		} else if (data.returnToEdit && typeof data.editProjectId === 'string') {
			this.returnTo = `/projects/${data.editProjectId}/edit`
		}

		this.questionnaireIndex = typeof data.questionnaireIndex === 'number' ? data.questionnaireIndex : null
		this.questionnaireMetadata = data
		this.allowedRoleIds = Array.isArray(data.representativeRoleIds) ? data.representativeRoleIds : []
		this._otherProjectQuestions = Array.isArray(data.otherQuestionnairesQuestions)
			? data.otherQuestionnairesQuestions
			: []
		const stageNames = this.normalizeStageNamesList(data.stages)
		this.stageSelectionConfig = this.buildStageSelectionConfig(stageNames)

		if (data.projectName) {
			this.projectName.set(data.projectName)
		}
		this._projectStageNames.set(stageNames)
		if (data.questions && Array.isArray(data.questions)) {
			this.questions.set(this.normalizeQuestionList(data.questions))
		}

		this.form.patchValue(
			{
				sequence: data.questionnaireIndex,
				projectName: data.projectName,
				name: data.name,
				weight: data.weight,
			},
			{ emitEvent: false }
		)

		if (this.isViewMode()) {
			this.applyReadOnlyState()
			this.loadViewData()
		}

		this.cdr.detectChanges()
	}

	private applyReadOnlyState(): void {
		this.form.disable({ emitEvent: false })
	}

	private loadViewData(): void {
		const projectId = this.viewProjectId()
		const questionnaireId = this.viewQuestionnaireId()

		if (!projectId || questionnaireId === null) {
			return
		}

		this.isLoadingQuestions.set(true)
		this.questionnaireQueryStore
			.getQuestionnaireRaw(projectId, questionnaireId)
			.pipe(take(1))
			.subscribe({
				next: (raw: QuestionnaireRawResponse) => {
					this.form.patchValue(
						{
							sequence: raw.id,
							name: raw.name,
							weight: raw.weight,
						},
						{ emitEvent: false }
					)
					this.cdr.markForCheck()
				},
				error: () => {
					this.notificationService.showError(
						this.translate.instant('notifications.questionnaire_form.load_data_error')
					)
				},
			})

		this.fetchQuestionsPage(0)
	}

	private fetchQuestionsPage(page: number): void {
		const projectId = this.viewProjectId()
		const questionnaireId = this.viewQuestionnaireId()

		if (!projectId || questionnaireId === null) {
			this.isLoadingQuestions.set(false)
			return
		}

		this.currentPage.set(page)
		this.isLoadingQuestions.set(true)

		this.questionnaireQueryStore
			.listAllQuestions(projectId, questionnaireId, page, this.pageSize())
			.pipe(take(1))
			.subscribe({
				next: (result) => {
					this.pagination.set(result as unknown as Page<unknown>)
					const stageConfigNames = this.normalizeStageNamesList(this.questionnaireMetadata?.stages ?? [])
					const stageConfig = this.buildStageSelectionConfig(stageConfigNames)
					this.stageSelectionConfig = stageConfig

					const questions = (result.content ?? []).map((q: QuestionnaireQuestionResponse) => {
						const roleIds = q.roleIds ?? []
						const roleNames = this.mapRoleIdsToNames(roleIds)

						return this.enrichQuestionStageMetadata({
							id: String(q.id),
							value: q.text,
							roleIds,
							roleNames,
							stageNames: q.stageNames ?? [],
							type: q.type ?? QuestionType.Custom,
						} as unknown as QuestionData)
					})
					this.questions.set(questions)
					this.isLoadingQuestions.set(false)
					this.cdr.markForCheck()
				},
				error: () => {
					this.isLoadingQuestions.set(false)
					this.notificationService.showError(
						this.translate.instant('notifications.questionnaire_form.load_questions_error')
					)
				},
			})
	}

	onPageChange(page: number): void {
		const targetPage = Math.max(0, page - 1)
		this.fetchQuestionsPage(targetPage)
	}

	private initializeForm(): void {
		this.form = this.fb.group({
			sequence: [{ value: '', disabled: true }],
			projectName: [{ value: '', disabled: true }],
			name: ['', Validators.required],
			weight: ['', [Validators.required, Validators.min(0), Validators.max(100)]],
			questions: this.fb.array([]),
		})
	}

	private loadRoleFilterOptions(): void {
		this.roleService
			.getRoles()
			.pipe(take(1))
			.subscribe({
				next: (roles) => {
					const allRoles = roles ?? []
					this.roleNameById = new Map(allRoles.map((role: RoleSummary) => [role.id, role.name]))
					const filtered = this.allowedRoleIds.length
						? allRoles.filter((r) => this.allowedRoleIds.includes(r.id))
						: allRoles
					this.roleFilterOptions = filtered.map((role) => ({ value: role.name, label: role.name }))
					this.refreshQuestionRoleNames()
					this._rolesReady.set(true)
					this.cdr.markForCheck()
				},
				error: (error) => {
					console.error('Falha ao carregar roles para filtro de questionário iterativo', error)
				},
			})
	}

	private refreshQuestionRoleNames(): void {
		if (!this.roleNameById.size) return
		const current = this.questions()
		if (!current.length) return
		const updated = current.map((q) => ({
			...q,
			roleNames: this.mapRoleIdsToNames(q.roleIds, q.roleNames),
		}))
		this.questions.set(updated)
	}

	toggleQuestionnaireDataAccordion(): void {
		this.questionnaireDataAccordionOpen = !this.questionnaireDataAccordionOpen
	}

	toggleQuestionsAccordion(): void {
		this.questionsAccordionOpen = !this.questionsAccordionOpen
	}

	get filteredQuestions(): QuestionData[] {
		let filtered = this.questions()

		if (this.searchTerm) {
			filtered = filtered.filter((q) => q.value.toLowerCase().includes(this.searchTerm.toLowerCase()))
		}

		if (this.selectedRole) {
			filtered = filtered.filter((q) => q.roleNames.includes(this.selectedRole))
		}

		return filtered
	}

	getStageLabels(question: QuestionData): string[] {
		return this.resolveQuestionStageNames(question)
	}

	openAddQuestionModal(): void {
		if (this.isViewMode()) {
			return
		}
		if (!this.stageSelectionConfig) {
			this.notificationService.showWarning(
				this.translate.instant('notifications.questionnaire_form.add_stage_first')
			)
			return
		}

		this.modalService.open(QuestionModalComponent, 'medium-card', {
			mode: ActionType.CREATE,
			stageConfig: this.stageSelectionConfig,
			allowedRoleIds: this.allowedRoleIds.length ? this.allowedRoleIds : undefined,
		})

		const modalInstance = this.modalService.getActiveInstance<QuestionModalComponent>()
		if (modalInstance) {
			this.questionModalSubscription = modalInstance.questionCreated.subscribe((questionData: QuestionData) => {
				const currentQuestions = this.questions()
				questionData.id = 'temp_' + Date.now().toString()
				const enriched = this.enrichQuestionStageMetadata({ ...questionData })
				this.questions.set([...currentQuestions, enriched])
				this.cdr.detectChanges()
			})
		}
	}

	isBaseQuestion(question: QuestionData): boolean {
		return question.type === QuestionType.Base
	}

	isTypeAQuestion(question: QuestionData): boolean {
		return question.classification === QuestionClassificationType.ProjetoInteiro
	}

	openEditQuestionModal(question: QuestionData): void {
		if (this.isViewMode()) {
			return
		}
		this.modalService.open(QuestionModalComponent, 'medium-card', {
			mode: ActionType.EDIT,
			editData: question,
			stageConfig: this.stageSelectionConfig,
			allowedRoleIds: this.allowedRoleIds.length ? this.allowedRoleIds : undefined,
			textReadOnly: this.isBaseQuestion(question),
		})

		const modalInstance = this.modalService.getActiveInstance<QuestionModalComponent>()
		if (modalInstance) {
			this.questionModalSubscription = modalInstance.questionUpdated.subscribe(
				(updatedQuestion: QuestionData) => {
					const currentQuestions = this.questions()
					const index = currentQuestions.findIndex((q) => q.id === updatedQuestion.id)
					if (index !== -1) {
						currentQuestions[index] = this.enrichQuestionStageMetadata({ ...updatedQuestion })
						this.questions.set([...currentQuestions])
						this.cdr.detectChanges()
					}
				}
			)
		}
	}

	deleteQuestion(question: QuestionData): void {
		if (this.isViewMode() || this.isBaseQuestion(question)) {
			return
		}
		this.notificationService.showConfirm(`Tem certeza que deseja excluir a pergunta: "${question.value}"?`, () => {
			const currentQuestions = this.questions()
			const filtered = currentQuestions.filter((q) => q.id !== question.id)
			this.questions.set(filtered)
			this.clearSelection()
			this.cdr.detectChanges()
		})
	}

	toggleQuestionSelection(questionId: string): void {
		const target = this.questions().find((q) => q.id === questionId)
		if (target && this.isBaseQuestion(target)) {
			return
		}
		const current = new Set(this.selectedQuestionIds())
		if (current.has(questionId)) {
			current.delete(questionId)
		} else {
			current.add(questionId)
		}
		this.selectedQuestionIds.set(current)
	}

	isQuestionSelected(questionId: string): boolean {
		return this.selectedQuestionIds().has(questionId)
	}

	get isAllSelected(): boolean {
		const selectable = this.filteredQuestions.filter((q) => !this.isBaseQuestion(q))
		return selectable.length > 0 && selectable.every((q) => this.selectedQuestionIds().has(q.id!))
	}

	get hasSelectedQuestions(): boolean {
		return this.selectedQuestionIds().size > 0
	}

	get selectedCount(): number {
		return this.selectedQuestionIds().size
	}

	toggleSelectAll(): void {
		const selectable = this.filteredQuestions.filter((q) => !this.isBaseQuestion(q))
		if (this.isAllSelected) {
			this.selectedQuestionIds.set(new Set())
		} else {
			this.selectedQuestionIds.set(new Set(selectable.map((q) => q.id!)))
		}
	}

	deleteSelectedQuestions(): void {
		if (this.isViewMode()) return
		const count = this.selectedCount
		this.notificationService.showConfirm(
			`Tem certeza que deseja excluir ${count} pergunta${count > 1 ? 's' : ''} selecionada${count > 1 ? 's' : ''}?`,
			() => {
				const ids = this.selectedQuestionIds()
				this.questions.update((qs) => qs.filter((q) => !ids.has(q.id!)))
				this.clearSelection()
				this.cdr.detectChanges()
			}
		)
	}

	private clearSelection(): void {
		this.selectedQuestionIds.set(new Set())
	}

	protected override save(): RouteParams<IterativoQuestionnaireRouteParams> {
		return {
			name: this.form.get('name')?.value,
			weight: this.form.get('weight')?.value,
			questions: this.questions(),
		} satisfies IterativoQuestionnaireRouteParams
	}

	protected override restore(restoreParameter: IterativoQuestionnaireRestoreParams): void {
		if (!restoreParameter?.hasParams) {
			return
		}

		const savedData = (restoreParameter.p ?? restoreParameter) as IterativoQuestionnaireRouteParams

		if (savedData.questions) {
			this.questions.set(this.normalizeQuestionList(savedData.questions))
		}

		const patchValue: Partial<{ name: string; weight: number }> = {}
		if (savedData.name !== undefined) {
			patchValue.name = savedData.name
		}
		if (savedData.weight !== undefined) {
			patchValue.weight = savedData.weight
		}

		if (Object.keys(patchValue).length) {
			this.form.patchValue(patchValue)
		}
	}

	getControl(controlName: string) {
		return this.form.get(controlName)
	}

	onConfirmAndGoBack(): void {
		if (this.isViewMode()) {
			this.navigateBack()
			return
		}
		if (this.form.invalid) {
			this.form.markAllAsTouched()
			this.notificationService.showWarning(
				this.translate.instant('notifications.questionnaire_form.fill_required')
			)
			return
		}

		if (!this.questions().length) {
			this.notificationService.showWarning(
				this.translate.instant('notifications.questionnaire_form.add_question')
			)
			return
		}

		const hasInvalidStages = this.questions().some((question) => !this.hasValidStageSelection(question))
		if (hasInvalidStages) {
			this.notificationService.showWarning(this.translate.instant('notifications.questionnaire_form.link_stages'))
			return
		}

		const formValue = this.form.getRawValue()
		const finalData = {
			...formValue,
			iteration: this.questionnaireMetadata?.iteration,
			questionnaireIndex: this.questionnaireIndex,
			questions: this.questions(),
		}

		try {
			sessionStorage.setItem(PENDING_QUESTIONNAIRE_UPDATE_KEY, JSON.stringify(finalData))
		} catch (error) {
			LoggerService.error(
				'IterativoQuestionnaireForm: Erro ao persistir dados do questionário em sessionStorage',
				error
			)
		}

		this.navigateBack({ questionnaireUpdate: finalData })
	}

	onCancel(): void {
		this.navigateBack()
	}

	private normalizeStageNamesList(stageNames?: string[]): string[] {
		if (!Array.isArray(stageNames)) {
			return []
		}

		return Array.from(
			new Set(
				stageNames
					.map((stage) => (typeof stage === 'string' ? stage.trim() : ''))
					.filter((stage) => stage.length > 0)
			)
		)
	}

	private buildStageSelectionConfig(stageNames: string[]): QuestionStageConfig | undefined {
		if (!stageNames.length) {
			return undefined
		}

		const options = stageNames.map((stage) => ({ value: stage, label: stage }))
		return {
			options,
			required: true,
			allowMultiple: true,
			placeholder: this.translate.instant('notifications.questionnaire_form.stage_selection_placeholder'),
			label: this.translate.instant('notifications.questionnaire_form.stage_selection_label'),
		} satisfies QuestionStageConfig
	}

	private normalizeQuestionList(questions?: QuestionData[]): QuestionData[] {
		if (!questions?.length) {
			return []
		}

		return questions.map((question) => this.enrichQuestionStageMetadata({ ...question }))
	}

	private resolveQuestionStageNames(question: QuestionData | undefined): string[] {
		if (!question) {
			return []
		}

		const normalized = Array.isArray(question.stageNames)
			? question.stageNames
					.map((stage) => (typeof stage === 'string' ? stage.trim() : ''))
					.filter((stage) => stage.length > 0)
			: []

		if (normalized.length) {
			return Array.from(new Set(normalized))
		}

		if (typeof question.stageName === 'string' && question.stageName.trim().length) {
			return [question.stageName.trim()]
		}

		return []
	}

	private enrichQuestionStageMetadata(question: QuestionData): QuestionData {
		const stageNames = this.resolveQuestionStageNames(question)
		question.stageNames = stageNames
		question.stageName = stageNames[0] ?? null
		question.categoryStageName = stageNames.length === 1 ? stageNames[0] : (question.categoryStageName ?? null)
		question.roleNames = this.mapRoleIdsToNames(question.roleIds, question.roleNames)
		return question
	}

	private mapRoleIdsToNames(roleIds?: number[] | null, fallbackNames?: string[]): string[] {
		const ids = Array.isArray(roleIds) ? roleIds : []
		if (ids.length && this.roleNameById.size) {
			const names = ids
				.map((id) => this.roleNameById.get(Number(id)))
				.filter((name): name is string => Boolean(name))
			if (names.length) {
				return Array.from(new Set(names))
			}
		}

		return Array.isArray(fallbackNames) ? [...fallbackNames] : []
	}

	private hasValidStageSelection(question: QuestionData | undefined): boolean {
		return this.resolveQuestionStageNames(question).length > 0
	}

	private navigateBack(updatedParams?: GenericParams): void {
		this.skipStatePersistence = true
		if (this.returnTo) {
			this.routerService.navigateTo(this.returnTo, {
				params: {},
				queryParams: { type: ProjectType.Iterativo },
			})
			return
		}

		this.routerService.backToPrevious(0, true, updatedParams, '/projects')
	}

	override ngOnDestroy(): void {
		if (!this.skipStatePersistence) {
			super.ngOnDestroy()
		}
		this.questionModalSubscription?.unsubscribe()
	}
}
