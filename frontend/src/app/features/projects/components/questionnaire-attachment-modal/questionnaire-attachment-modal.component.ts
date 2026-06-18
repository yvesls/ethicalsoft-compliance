import { ChangeDetectionStrategy, Component, Input, OnInit, inject } from '@angular/core'

import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms'
import { TranslateModule, TranslateService } from '@ngx-translate/core'
import { InputComponent } from '../../../../shared/components/input/input.component'
import { ModalService } from '../../../../core/services/modal.service'
import { NotificationService } from '../../../../core/services/notification.service'
import { QuestionnaireAttachmentLink } from '../../../../shared/interfaces/questionnaire/questionnaire-response.interface'

export type AttachmentModalMode = 'positive' | 'negative'

export interface AttachmentModalValue {
	note: string
	attachments: QuestionnaireAttachmentLink[]
}

@Component({
	selector: 'app-questionnaire-attachment-modal',
	standalone: true,
	imports: [ReactiveFormsModule, InputComponent, TranslateModule],
	templateUrl: './questionnaire-attachment-modal.component.html',
	styleUrls: ['./questionnaire-attachment-modal.component.scss'],
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionnaireAttachmentModalComponent implements OnInit {
	@Input() mode: AttachmentModalMode = 'positive'
	@Input() title = 'questionnaire.attachment.add_evidence'
	@Input() descriptionLabel = 'questionnaire.attachment.observations_label'
	@Input() attachmentsLabel = 'questionnaire.attachment.links_label'
	@Input() initialValue?: AttachmentModalValue
	@Input() onSave?: (value: AttachmentModalValue) => void

	private readonly fb = inject(FormBuilder)
	private readonly modalService = inject(ModalService)
	private readonly notification = inject(NotificationService)
	private readonly translate = inject(TranslateService)

	readonly descriptionMaxLength = 500
	private readonly urlPattern = /^https?:\/\/[^\s]+$/i

	get linkValidationMessages(): Record<string, string> {
		return {
			required: this.translate.instant('questionnaire.attachment.link_required'),
			pattern: this.translate.instant('questionnaire.attachment.link_pattern'),
		}
	}

	get descriptionValidationMessages(): Record<string, string> {
		return {
			required: this.translate.instant('questionnaire.attachment.desc_required'),
			minlength: this.translate.instant('questionnaire.attachment.desc_minlength'),
		}
	}

	readonly form: FormGroup = this.fb.group({
		note: this.fb.control('', [
			Validators.required,
			Validators.minLength(5),
			Validators.maxLength(this.descriptionMaxLength),
		]),
		attachments: this.fb.array<FormGroup>([], [Validators.required, Validators.minLength(1)]),
	})

	get attachments(): FormArray<FormGroup> {
		return this.form.get('attachments') as FormArray<FormGroup>
	}

	ngOnInit(): void {
		this.applyModeLabels()
		this.initializeFormValues()
	}

	addAttachment(link?: QuestionnaireAttachmentLink | null): void {
		this.attachments.push(this.createAttachmentGroup(link))
	}

	removeAttachment(index: number): void {
		if (this.attachments.length === 1) {
			this.attachments.at(0).reset({ descricao: '', url: '' })
			return
		}
		this.attachments.removeAt(index)
	}

	submit(): void {
		if (this.form.invalid) {
			this.form.markAllAsTouched()
			this.attachments.markAllAsTouched()
			this.notification.showWarning(this.translate.instant('questionnaire.attachment.incomplete_error'))
			return
		}

		const value: AttachmentModalValue = {
			note: this.form.get('note')?.value ?? '',
			attachments: this.attachments.controls.map((control) => {
				const descricao = control.get('descricao')?.value ?? ''
				const url = control.get('url')?.value ?? ''
				return {
					descricao: descricao?.trim() || url?.trim(),
					url: url?.trim(),
				}
			}),
		}

		this.onSave?.(value)
		this.modalService.close()
	}

	cancel(): void {
		this.modalService.close()
	}

	private applyModeLabels(): void {
		if (this.mode === 'positive') {
			this.title = 'questionnaire.attachment.add_evidence'
			this.descriptionLabel = 'questionnaire.attachment.observations_context'
			this.attachmentsLabel = 'questionnaire.attachment.links_label'
			return
		}

		this.title = 'questionnaire.attachment.add_justification'
		this.descriptionLabel = 'questionnaire.attachment.justification_label'
		this.attachmentsLabel = 'questionnaire.attachment.links_support'
	}

	private initializeFormValues(): void {
		const value = this.initialValue ?? {
			note: '',
			attachments: [{ descricao: '', url: '' }],
		}
		this.form.patchValue({ note: value.note ?? '' })

		if (!value.attachments?.length) {
			this.addAttachment()
			return
		}

		value.attachments.forEach((attachment) => this.addAttachment(attachment))
	}

	private createAttachmentGroup(attachment?: QuestionnaireAttachmentLink | null): FormGroup {
		return this.fb.group({
			descricao: this.fb.control(attachment?.descricao ?? '', [
				Validators.required,
				Validators.minLength(3),
				Validators.maxLength(120),
			]),
			url: this.fb.control(attachment?.url ?? '', [Validators.required, Validators.pattern(this.urlPattern)]),
		})
	}
}
