import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

import { ProjectStore } from '../../../../shared/stores/project.store';
import { ProjectType } from '../../../../shared/enums/project-type.enum';
import { RouterService } from '../../../../core/services/router.service';
import { ActionType } from '../../../../shared/enums/action-type.enum';

interface ViewRedirectParams {
  [key: string]: unknown;
  projectId: string;
  questionnaireId: number;
  mode: ActionType;
}

@Component({
  selector: 'app-questionnaire-view-redirect-page',
  standalone: true,
  template: `
    <div style="padding: 16px">
      <p>Carregando questionário...</p>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionnaireViewRedirectPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly projectStore = inject(ProjectStore);
  private readonly routerService = inject(RouterService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    const params$ = this.route.paramMap.pipe(
      map((params) => {
        const projectId = params.get('projectId');
        const questionnaireIdRaw = params.get('questionnaireId');

        return {
          projectId,
          questionnaireId: questionnaireIdRaw ? Number(questionnaireIdRaw) : null,
        };
      })
    );

    params$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ projectId, questionnaireId }) => {
        if (!projectId || questionnaireId === null || Number.isNaN(questionnaireId)) {
          void this.routerService.navigateTo('/projects');
          return;
        }
        this.projectStore
          .getProjectById(projectId)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: (project) => {
              const target =
                project.type === ProjectType.Cascata
                  ? '/projects/questionnaire/cascata'
                  : '/projects/questionnaire/iterativo';

              void this.routerService.navigateTo<ViewRedirectParams>(target, {
                params: {
                  projectId,
                  questionnaireId,
                  mode: ActionType.VIEW,
                },
              });
            },
            error: () => {
              void this.routerService.navigateTo('/projects');
            },
          });
      });
  }
}
