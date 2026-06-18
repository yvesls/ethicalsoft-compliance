import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core'

import { ActivatedRoute } from '@angular/router'
import { take } from 'rxjs/operators'

import { ProjectType } from '../../../../shared/enums/project-type.enum'
import { RouterService } from '../../../../core/services/router.service'
import { CascataProjectFormComponent } from '../../components/cascata-project-form/cascata-project-form.component'
import { IterativoProjectFormComponent } from '../../components/iterativo-project-form/iterativo-project-form.component'

@Component({
	selector: 'app-create-project-page',
	standalone: true,
	imports: [CascataProjectFormComponent, IterativoProjectFormComponent],
	templateUrl: './create-project-page.component.html',
	styleUrls: ['./create-project-page.component.scss'],
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateProjectPageComponent implements OnInit {
	private routerService = inject(RouterService)
	private route = inject(ActivatedRoute)

	public ProjectType = ProjectType
	public projectType = signal<ProjectType | null>(null)

	ngOnInit(): void {
		this.route.queryParams.pipe(take(1)).subscribe((queryParams) => {
			const type = queryParams['type']

			if (type === ProjectType.Cascata || type === ProjectType.Iterativo) {
				this.projectType.set(type as ProjectType)
			} else {
				this.routerService.navigateTo('/projects')
			}
		})
	}
}
