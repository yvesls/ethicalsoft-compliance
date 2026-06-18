import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core'

import { TranslateModule } from '@ngx-translate/core'

@Component({
	selector: 'app-project-creation-confirm-modal',
	standalone: true,
	imports: [TranslateModule],
	templateUrl: './project-creation-confirm-modal.component.html',
	styleUrls: ['./project-creation-confirm-modal.component.scss'],
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectCreationConfirmModalComponent {
	@Input() title = 'projects.creation_confirm.title'
	@Input() message = 'projects.creation_confirm.message'
	@Input() confirmLabel = 'common.confirm'
	@Input() cancelLabel = 'common.cancel'

	@Output() confirmed = new EventEmitter<void>()
	@Output() canceled = new EventEmitter<void>()

	onConfirm(): void {
		this.confirmed.emit()
	}

	onCancel(): void {
		this.canceled.emit()
	}
}
