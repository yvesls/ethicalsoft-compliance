import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { take } from 'rxjs/operators';

import { ProjectType } from '../../../../shared/enums/project-type.enum';
import { RouterService } from '../../../../core/services/router.service';
import { EditCascataProjectFormComponent } from '../../components/edit-cascata-project-form/edit-cascata-project-form.component';
import { EditIterativoProjectFormComponent } from '../../components/edit-iterativo-project-form/edit-iterativo-project-form.component';

@Component({
  selector: 'app-edit-project-page',
  standalone: true,
  imports: [CommonModule, EditCascataProjectFormComponent, EditIterativoProjectFormComponent],
  templateUrl: './edit-project-page.component.html',
  styleUrls: ['./edit-project-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditProjectPageComponent implements OnInit {
  private routerService = inject(RouterService);
  private route = inject(ActivatedRoute);

  public ProjectType = ProjectType;
  public projectType = signal<ProjectType | null>(null);
  public projectId = signal<string | null>(null);

  ngOnInit(): void {
    this.route.params.pipe(take(1)).subscribe((params) => {
      const id = params['projectId'];
      if (!id) {
        this.routerService.navigateTo('/projects');
        return;
      }
      this.projectId.set(id);
    });

    this.route.queryParams.pipe(take(1)).subscribe((queryParams) => {
      const type = queryParams['type'];
      if (type === ProjectType.Cascata || type === ProjectType.Iterativo) {
        this.projectType.set(type as ProjectType);
      } else {
        this.projectType.set(ProjectType.Cascata);
      }
    });
  }
}
