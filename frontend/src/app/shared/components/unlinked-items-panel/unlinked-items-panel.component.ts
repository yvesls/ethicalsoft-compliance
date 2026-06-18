import { Component, Input, ChangeDetectionStrategy } from '@angular/core'

import { TranslateModule } from '@ngx-translate/core'

@Component({
	selector: 'app-unlinked-items-panel',
	standalone: true,
	imports: [TranslateModule],
	templateUrl: './unlinked-items-panel.component.html',
	styleUrls: ['./unlinked-items-panel.component.scss'],
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UnlinkedItemsPanelComponent {
	@Input() unlinkedRoles: string[] = []
	@Input() unlinkedStages: string[] = []
	@Input() orphanedRoles: string[] = []
	@Input() orphanedStages: string[] = []

	isOpen = true

	get hasItems(): boolean {
		return (
			this.unlinkedRoles.length > 0 ||
			this.unlinkedStages.length > 0 ||
			this.orphanedRoles.length > 0 ||
			this.orphanedStages.length > 0
		)
	}

	get totalCount(): number {
		return (
			this.unlinkedRoles.length +
			this.unlinkedStages.length +
			this.orphanedRoles.length +
			this.orphanedStages.length
		)
	}

	toggle(): void {
		this.isOpen = !this.isOpen
	}
}
