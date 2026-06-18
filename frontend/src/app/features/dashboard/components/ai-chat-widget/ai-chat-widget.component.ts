import { LoggerService } from '../../../../core/services/logger.service'
import {
	Component,
	inject,
	Input,
	signal,
	ElementRef,
	ViewChild,
	AfterViewChecked,
	ChangeDetectionStrategy,
} from '@angular/core'
import { FormsModule } from '@angular/forms'
import { Subscription } from 'rxjs'
import { TranslateModule, TranslateService } from '@ngx-translate/core'
import { MarkdownPipe } from '../../../../shared/utils/markdown.pipe'
import { AiDashboardService } from '../../services/ai-dashboard.service'
import { AiTokenService } from '../../../../core/ai/ai-token.service'
import { ModalService } from '../../../../core/services/modal.service'
import { MissingAiTokenDialogComponent } from '../../../../shared/components/missing-ai-token-dialog/missing-ai-token-dialog.component'

interface ChatMessage {
	role: 'user' | 'assistant'
	content: string
	timestamp: Date
}

const SUGGESTED_QUESTION_KEYS = [
	'dashboard.ai_chat.suggested_1',
	'dashboard.ai_chat.suggested_2',
	'dashboard.ai_chat.suggested_3',
	'dashboard.ai_chat.suggested_4',
	'dashboard.ai_chat.suggested_5',
]

@Component({
	selector: 'app-ai-chat-widget',
	standalone: true,
	imports: [FormsModule, MarkdownPipe, TranslateModule],
	templateUrl: './ai-chat-widget.component.html',
	changeDetection: ChangeDetectionStrategy.Eager,
	styleUrl: './ai-chat-widget.component.scss',
})
export class AiChatWidgetComponent implements AfterViewChecked {
	@Input({ required: true }) projectId!: number
	@Input({ required: true }) questionnaireId!: number

	@ViewChild('messagesContainer') messagesContainer!: ElementRef

	private readonly aiService = inject(AiDashboardService)
	private readonly aiTokenService = inject(AiTokenService)
	private readonly modalService = inject(ModalService)
	private readonly translate = inject(TranslateService)

	isOpen = signal(false)
	messages = signal<ChatMessage[]>([])
	currentInput = ''
	streaming = signal(false)
	private streamSub: Subscription | null = null
	private shouldScroll = false

	get suggestedQuestions(): string[] {
		return SUGGESTED_QUESTION_KEYS.map((k) => this.translate.instant(k))
	}

	ngAfterViewChecked(): void {
		if (this.shouldScroll) {
			this.scrollToBottom()
			this.shouldScroll = false
		}
	}

	toggle(): void {
		if (!this.isOpen() && !this.aiTokenService.hasToken) {
			this.modalService.open(MissingAiTokenDialogComponent, 'small-card')
			return
		}
		this.isOpen.update((v) => !v)
	}

	close(): void {
		this.isOpen.set(false)
	}

	sendMessage(question?: string): void {
		const text = (question ?? this.currentInput).trim()
		if (!text || this.streaming()) return

		this.currentInput = ''

		this.messages.update((msgs) => [...msgs, { role: 'user', content: text, timestamp: new Date() }])

		this.streaming.set(true)
		this.shouldScroll = true

		this.messages.update((msgs) => [...msgs, { role: 'assistant', content: '', timestamp: new Date() }])

		this.streamSub = this.aiService.askQuestion(this.projectId, this.questionnaireId, text).subscribe({
			next: (chunk) => {
				this.messages.update((msgs) => {
					const updated = [...msgs]
					const lastMsg = updated[updated.length - 1]
					if (lastMsg.role === 'assistant') {
						updated[updated.length - 1] = {
							...lastMsg,
							content: lastMsg.content + chunk,
						}
					}
					return updated
				})
				this.shouldScroll = true
			},
			error: () => {
				this.messages.update((msgs) => {
					const updated = [...msgs]
					const lastMsg = updated[updated.length - 1]
					if (lastMsg.role === 'assistant' && !lastMsg.content) {
						updated[updated.length - 1] = {
							...lastMsg,
							content: this.translate.instant('dashboard.ai_chat.error_msg'),
						}
					}
					return updated
				})
				this.streaming.set(false)
			},
			complete: () => {
				this.streaming.set(false)
				this.shouldScroll = true
			},
		})
	}

	cancelStream(): void {
		if (this.streamSub) {
			this.streamSub.unsubscribe()
			this.streamSub = null
		}
		this.streaming.set(false)
	}

	clearChat(): void {
		this.cancelStream()
		this.messages.set([])
	}

	onKeydown(event: KeyboardEvent): void {
		if (event.key === 'Enter' && !event.shiftKey) {
			event.preventDefault()
			this.sendMessage()
		}
	}

	private scrollToBottom(): void {
		try {
			const el = this.messagesContainer?.nativeElement
			if (el) {
				el.scrollTop = el.scrollHeight
			}
		} catch (error) {
			LoggerService.warn('AiChatWidget: Erro ao rolar para o final do chat.', error)
		}
	}
}
