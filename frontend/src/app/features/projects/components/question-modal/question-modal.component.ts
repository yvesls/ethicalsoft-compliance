import { Component, Output, EventEmitter, inject, Input, ChangeDetectionStrategy, OnInit } from '@angular/core'

import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms'
import { TranslateModule, TranslateService } from '@ngx-translate/core'

import { ModalService } from '../../../../core/services/modal.service'
import { InputComponent } from '../../../../shared/components/input/input.component'
import {
	MultiSelectComponent,
	MultiSelectOption,
} from '../../../../shared/components/multi-select/multi-select.component'
import { ActionType } from '../../../../shared/enums/action-type.enum'
import { RoleService } from '../../../../core/services/role.service'
import { RoleSummary } from '../../../../shared/interfaces/role/role-summary.interface'
import { QuestionClassificationType } from '../../../../shared/enums/question-classification-type.enum'
import { QuestionType } from '../../../../shared/enums/question-type.enum'
import { take } from 'rxjs/operators'

export interface QuestionData {
	id?: string
	value: string
	roleIds: number[]
	roleNames: string[]
	stageNames?: string[]
	stageName?: string | null
	categoryStageName?: string | null
	type?: QuestionType
	classification?: QuestionClassificationType | null
}

export interface QuestionStageConfig {
	options: MultiSelectOption[]
	required?: boolean
	allowMultiple?: boolean
	maxSelectedItems?: number
	lockedValues?: (string | number)[]
	disabled?: boolean
	defaultStageNames?: string[]
	label?: string
	placeholder?: string
}

@Component({
	selector: 'app-question-modal',
	standalone: true,
	imports: [ReactiveFormsModule, InputComponent, MultiSelectComponent, TranslateModule],
	templateUrl: './question-modal.component.html',
	styleUrls: ['./question-modal.component.scss'],
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionModalComponent implements OnInit {
	@Input() editData?: QuestionData
	@Input() mode: ActionType = ActionType.CREATE
	@Input() stageConfig?: QuestionStageConfig
	@Input() allowedRoleIds?: number[]
	@Input() textReadOnly = false
	@Input() allowWholeProjectClassification = true
	@Output() questionCreated = new EventEmitter<QuestionData>()
	@Output() questionUpdated = new EventEmitter<QuestionData>()

	readonly QuestionClassificationType = QuestionClassificationType

	private readonly allClassificationOptions: { value: QuestionClassificationType | null; labelKey: string }[] = [
		{ value: null, labelKey: 'questionnaire.question_modal.classification_none' },
		{ value: QuestionClassificationType.WholeProject, labelKey: 'questionnaire.question_modal.classification_whole_project' },
		{ value: QuestionClassificationType.Recurring, labelKey: 'questionnaire.question_modal.classification_recurring' },
		{ value: QuestionClassificationType.CurrentStage, labelKey: 'questionnaire.question_modal.classification_current_stage' },
	]

	classificationOptions: { value: QuestionClassificationType | null; labelKey: string }[] =
		this.allClassificationOptions

	private modalService = inject(ModalService)
	private fb = inject(FormBuilder)
	private roleService = inject(RoleService)
	private readonly translate = inject(TranslateService)

	form!: FormGroup
	actionType: ActionType = ActionType.CREATE
	modalTitle = ''
	roleOptions: MultiSelectOption[] = []
	private rolesLookup = new Map<number, string>()
	private pendingRoleNames?: string[]
	showStageSelector = false
	stageOptions: MultiSelectOption[] = []
	stageLabel = ''
	stagePlaceholder = ''
	stageMaxSelectedItems?: number
	stageLockedValues: (string | number)[] = []

	constructor() {
		this.initializeForm()
	}

	ngOnInit(): void {
		this.configureStageSelector()
		this.loadRoleOptions()
		this.configureClassificationOptions()

		if (this.editData && this.mode === ActionType.EDIT) {
			this.actionType = ActionType.EDIT
			this.modalTitle = this.textReadOnly
				? this.translate.instant('questionnaire.question_modal.edit_links_title')
				: this.translate.instant('questionnaire.question_modal.edit_title')
			this.populateForm(this.editData)
			if (this.textReadOnly) {
				this.form.get('value')?.disable({ emitEvent: false })
			}
		} else {
			this.actionType = ActionType.CREATE
			this.modalTitle = this.translate.instant('questionnaire.question_modal.create_title')
			this.applyDefaultStageSelection()
		}
	}

	private initializeForm(): void {
		this.form = this.fb.group({
			value: ['', [Validators.required, Validators.minLength(10)]],
			roleIds: [[], [Validators.required, Validators.minLength(1)]],
			stageNames: [[]],
			classification: [null as QuestionClassificationType | null],
		})
	}

	private loadRoleOptions(): void {
		this.roleService
			.getRoles()
			.pipe(take(1))
			.subscribe({
				next: (roles) => {
					const resolvedRoles = roles ?? []
					this.rolesLookup = new Map(resolvedRoles.map((role) => [role.id, role.name]))
					const filtered = this.allowedRoleIds?.length
						? resolvedRoles.filter((r) => this.allowedRoleIds!.includes(r.id))
						: resolvedRoles
					this.roleOptions = filtered.map(this.mapRoleToOption)
					this.applyPendingRoleNameConversion()
				},
				error: (error) => {
					console.error('Falha ao carregar roles para o questionário', error)
				},
			})
	}

	private mapRoleToOption(role: RoleSummary): MultiSelectOption {
		return {
			value: role.id,
			label: role.name,
		}
	}

	private populateForm(data: QuestionData): void {
		const roleIds = this.ensureRoleIds(data)
		this.form.patchValue({
			value: data.value,
			roleIds,
			stageNames: this.ensureStageNames(data),
			classification: data.classification ?? null,
		})
	}

	private ensureRoleIds(data: QuestionData): number[] {
		if (Array.isArray(data.roleIds) && data.roleIds.length) {
			return data.roleIds.map(Number).filter((id) => Number.isFinite(id) && id > 0)
		}

		if (Array.isArray(data.roleNames) && data.roleNames.length) {
			const ids = this.mapRoleNamesToIds(data.roleNames)
			if (ids.length) {
				data.roleIds = ids
				return ids
			}

			this.pendingRoleNames = data.roleNames
		}

		return []
	}

	private ensureStageNames(data: QuestionData | undefined): string[] {
		if (!data) {
			return []
		}

		if (Array.isArray(data.stageNames) && data.stageNames.length) {
			return this.normalizeStageNames(data.stageNames)
		}

		if (typeof data.stageName === 'string' && data.stageName.trim().length) {
			return [data.stageName.trim()]
		}

		return []
	}

	private applyPendingRoleNameConversion(): void {
		if (!this.pendingRoleNames?.length) {
			return
		}

		const roleIds = this.mapRoleNamesToIds(this.pendingRoleNames)
		if (roleIds.length) {
			this.form.patchValue({ roleIds }, { emitEvent: false })
			if (this.editData) {
				this.editData.roleIds = roleIds
			}
		}

		this.pendingRoleNames = undefined
	}

	private mapRoleNamesToIds(roleNames: string[]): number[] {
		if (!roleNames?.length || this.rolesLookup.size === 0) {
			return []
		}

		const normalized = roleNames.map((name) => name?.trim().toLowerCase()).filter(Boolean)

		if (!normalized.length) {
			return []
		}

		const ids: number[] = []
		for (const [id, name] of this.rolesLookup.entries()) {
			if (normalized.includes(name.trim().toLowerCase())) {
				ids.push(Number(id))
			}
		}

		return Array.from(new Set(ids))
	}

	private mapRoleIdsToNames(roleIds: number[]): string[] {
		if (!roleIds?.length || this.rolesLookup.size === 0) {
			return []
		}

		return roleIds.map((id) => this.rolesLookup.get(Number(id))).filter(Boolean) as string[]
	}

	private configureClassificationOptions(): void {
		const canSelectWholeProject =
			this.allowWholeProjectClassification || this.editData?.classification === QuestionClassificationType.WholeProject

		this.classificationOptions = canSelectWholeProject
			? this.allClassificationOptions
			: this.allClassificationOptions.filter((option) => option.value !== QuestionClassificationType.WholeProject)
	}

	private configureStageSelector(): void {
		const stageControl = this.form?.get('stageNames')
		if (!stageControl) {
			return
		}

		const config = this.stageConfig
		if (!config || !Array.isArray(config.options) || config.options.length === 0) {
			stageControl.clearValidators()
			stageControl.enable({ emitEvent: false })
			stageControl.updateValueAndValidity({ emitEvent: false })
			this.showStageSelector = false
			this.stageOptions = []
			this.stageLockedValues = []
			this.stageMaxSelectedItems = undefined
			return
		}

		this.showStageSelector = true
		this.stageOptions = config.options
		this.stageLabel = config.label ?? this.translate.instant('questionnaire.question_modal.stage_label')
		this.stagePlaceholder =
			config.placeholder ?? this.translate.instant('questionnaire.question_modal.stage_placeholder')
		this.stageMaxSelectedItems = config.maxSelectedItems ?? (config.allowMultiple === false ? 1 : undefined)
		this.stageLockedValues = config.lockedValues ?? []

		if (config.required) {
			stageControl.setValidators([Validators.required, Validators.minLength(1)])
		} else {
			stageControl.clearValidators()
		}

		if (config.disabled) {
			stageControl.disable({ emitEvent: false })
		} else {
			stageControl.enable({ emitEvent: false })
		}

		stageControl.updateValueAndValidity({ emitEvent: false })
	}

	private applyDefaultStageSelection(): void {
		if (!this.showStageSelector || !this.stageConfig?.defaultStageNames?.length) {
			return
		}

		const stageControl = this.form.get('stageNames')
		const normalized = this.normalizeStageNames(this.stageConfig.defaultStageNames)
		if (!stageControl || !normalized.length) {
			return
		}

		stageControl.setValue(normalized, { emitEvent: false })
		stageControl.markAsPristine()
		stageControl.markAsUntouched()
	}

	onSubmit(): void {
		if (this.form.valid) {
			const rawValue = this.form.getRawValue()
			const roleIds = rawValue.roleIds ?? []
			const stageNames = this.normalizeStageNames(rawValue.stageNames)
			const categoryStageName = this.resolveCategoryStageName(stageNames)
			const questionData: QuestionData = {
				id: this.editData?.id,
				value: this.textReadOnly ? (this.editData?.value ?? rawValue.value) : rawValue.value,
				roleIds,
				roleNames: this.mapRoleIdsToNames(roleIds),
				stageNames,
				stageName: stageNames[0] ?? null,
				categoryStageName,
				type: this.editData?.type,
				classification: this.textReadOnly
					? this.editData?.classification
					: (rawValue.classification ?? null),
			}

			if (this.actionType === ActionType.EDIT) {
				this.questionUpdated.emit(questionData)
			} else {
				this.questionCreated.emit(questionData)
			}

			this.close()
		} else {
			this.form.markAllAsTouched()
		}
	}

	close(): void {
		this.modalService.close()
	}

	private normalizeStageNames(value: unknown): string[] {
		if (!Array.isArray(value)) {
			return []
		}

		return Array.from(
			new Set(
				value
					.map((stage) => (typeof stage === 'string' ? stage.trim() : ''))
					.filter((stage) => stage.length > 0)
			)
		)
	}

	private resolveCategoryStageName(stageNames: string[]): string | null {
		if (stageNames.length === 1) {
			return stageNames[0]
		}

		const singleSelection = Boolean(
			this.stageConfig && (this.stageConfig.allowMultiple === false || this.stageConfig.maxSelectedItems === 1)
		)
		if (singleSelection) {
			return stageNames[0] ?? this.editData?.categoryStageName ?? null
		}

		if (typeof this.editData?.categoryStageName === 'string' && this.editData.categoryStageName.trim().length) {
			return this.editData.categoryStageName.trim()
		}

		return null
	}
}
