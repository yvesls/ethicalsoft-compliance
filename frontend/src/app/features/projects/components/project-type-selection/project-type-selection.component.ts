import { Component, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ProjectType } from '../../../../shared/enums/project-type.enum';
import { ModalService } from '../../../../core/services/modal.service';
import { RouterService } from '../../../../core/services/router.service';

@Component({
  selector: 'app-project-type-selection',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './project-type-selection.component.html',
  styleUrls: ['./project-type-selection.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectTypeSelectionComponent {
  private modalService = inject(ModalService);
  private routerService = inject(RouterService);

  public ProjectType = ProjectType;
  public selectedType = signal<ProjectType | null>(null);

  selectType(type: ProjectType): void {
    this.selectedType.set(type);
  }

  continue(): void {
    const type = this.selectedType();
    if (type) {
      this.routerService.navigateTo('/projects/create', { params: {}, queryParams: { type } });
      this.modalService.close();
    }
  }

  close(): void {
    this.modalService.close();
  }
}
