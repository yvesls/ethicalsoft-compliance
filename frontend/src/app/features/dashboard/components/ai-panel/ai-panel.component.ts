import { Component, Input, Output, EventEmitter, signal, ChangeDetectionStrategy } from '@angular/core'
import { DatePipe } from '@angular/common'
import { TranslateModule } from '@ngx-translate/core'
import { MarkdownPipe } from '../../../../shared/utils/markdown.pipe'
import { AiInsightResult } from '../../interfaces/ai.interface'

@Component({
	selector: 'app-ai-panel',
	standalone: true,
	imports: [DatePipe, MarkdownPipe, TranslateModule],
	templateUrl: './ai-panel.component.html',
	changeDetection: ChangeDetectionStrategy.Eager,
	styleUrl: './ai-panel.component.scss',
})
export class AiPanelComponent {
	@Input({ required: true }) title = ''
	@Input() icon = 'bi-robot'
	@Input() result: AiInsightResult | null = null
	@Input() loading = false
	@Input() buttonLabel = 'dashboard.ai_widgets.insights_btn'
	@Input() loadingLabel = 'dashboard.ai_widgets.insights_loading'

	@Output() generate = new EventEmitter<void>()

	expanded = signal(false)

	toggleExpanded(): void {
		this.expanded.update((v) => !v)
	}

	onGenerate(): void {
		this.expanded.set(true)
		this.generate.emit()
	}
}
