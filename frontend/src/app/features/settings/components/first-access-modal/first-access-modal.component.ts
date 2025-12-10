import { CommonModule } from '@angular/common'
import { Component, inject } from '@angular/core'
import { ModalService } from '../../../../core/services/modal.service'

@Component({
	selector: 'app-first-access-modal',
	standalone: true,
	imports: [CommonModule],
	templateUrl: './first-access-modal.component.html',
	styleUrl: './first-access-modal.component.scss',
})
export class FirstAccessModalComponent {
	private readonly modalService: ModalService = inject(ModalService)

	close(): void {
		this.modalService.close()
	}
}
