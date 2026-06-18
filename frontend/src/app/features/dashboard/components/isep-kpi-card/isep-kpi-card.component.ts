import { Component, Input, ChangeDetectionStrategy } from '@angular/core'
import { DecimalPipe } from '@angular/common'
import { Band } from '../../interfaces/dashboard.interface'
import { BandBadgeComponent } from '../band-badge/band-badge.component'
import { InfoExplainerComponent } from '../../../../shared/components/info-explainer/info-explainer.component'

@Component({
	selector: 'app-isep-kpi-card',
	standalone: true,
	imports: [DecimalPipe, BandBadgeComponent, InfoExplainerComponent],
	templateUrl: './isep-kpi-card.component.html',
	changeDetection: ChangeDetectionStrategy.Eager,
	styleUrl: './isep-kpi-card.component.scss',
})
export class IsepKpiCardComponent {
	@Input({ required: true }) label!: string
	@Input() value: number | null = null
	@Input() band: Band | null = null
	@Input() secondary: string | null = null
	@Input() infoKey?: string
	@Input() bandInfoKey?: string
}
