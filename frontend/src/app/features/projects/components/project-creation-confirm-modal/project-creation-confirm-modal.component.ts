import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
	selector: 'app-project-creation-confirm-modal',
	standalone: true,
	imports: [CommonModule],
	templateUrl: './project-creation-confirm-modal.component.html',
	styleUrls: ['./project-creation-confirm-modal.component.scss'],
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectCreationConfirmModalComponent {
	@Input() title = 'Confirmar criação do projeto';
	@Input()
	message =
		'Um template será criado para este projeto e um e-mail será enviado com as instruções de acesso. Deseja confirmar a criação do projeto?';
	@Input() confirmLabel = 'Confirmar';
	@Input() cancelLabel = 'Cancelar';

	@Output() confirmed = new EventEmitter<void>();
	@Output() canceled = new EventEmitter<void>();

	onConfirm(): void {
		this.confirmed.emit();
	}

	onCancel(): void {
		this.canceled.emit();
	}
}
