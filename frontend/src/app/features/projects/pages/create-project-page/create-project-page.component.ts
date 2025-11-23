import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  OnInit,
  CUSTOM_ELEMENTS_SCHEMA,
  Type,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { take } from 'rxjs/operators';

import { ProjectType } from '../../../../shared/enums/project-type.enum';
import { RouterService } from '../../../../core/services/router.service';

@Component({
  selector: 'app-create-project-page',
  standalone: true,
  imports: [CommonModule],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './create-project-page.component.html',
  styleUrls: ['./create-project-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateProjectPageComponent implements OnInit {
  private routerService = inject(RouterService);
  private route = inject(ActivatedRoute);

  public ProjectType = ProjectType;
  public projectType = signal<ProjectType | null>(null);
  public formComponent = signal<Type<any> | null>(null);

  ngOnInit(): void {
    this.route.queryParams.pipe(take(1)).subscribe((queryParams) => {
      const type = queryParams['type'];

      if (type === ProjectType.Cascata || type === ProjectType.Iterativo) {
        this.projectType.set(type as ProjectType);
        this.loadComponent(type);
      } else {
        this.routerService.navigateTo('/projects');
      }
    });
  }

  private async loadComponent(type: ProjectType): Promise<void> {
    if (type === ProjectType.Cascata) {
      const { CascataProjectFormComponent } = await import(
        '../../components/cascata-project-form/cascata-project-form.component'
      );
      this.formComponent.set(CascataProjectFormComponent);
    } else if (type === ProjectType.Iterativo) {
      const { IterativoProjectFormComponent } = await import(
        '../../components/iterativo-project-form/iterativo-project-form.component'
      );
      this.formComponent.set(IterativoProjectFormComponent);
    }
  }
}
