import { Component, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ModalService } from '../../../../core/services/modal.service';

@Component({
  selector: 'app-template-action-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './template-action-modal.component.html',
  styleUrls: ['./template-action-modal.component.scss']
})
export class TemplateActionModalComponent {
  @Output() actionSelected = new EventEmitter<'create' | 'import'>();

  private modalService = inject(ModalService);

  selectCreate(): void {
    this.actionSelected.emit('create');
    this.modalService.close();
  }

  selectImport(): void {
    this.actionSelected.emit('import');
    this.modalService.close();
  }
}
