import { Component, inject, Input, signal, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { MarkdownPipe } from '../../../../shared/utils/markdown.pipe';
import { AiDashboardService } from '../../services/ai-dashboard.service';

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

const SUGGESTED_QUESTIONS = [
  'Qual membro tem o menor ICP e por quê?',
  'Quais domínios estão abaixo de 70%?',
  'Resuma as justificativas de respostas NÃO',
  'O que significa estar na Faixa C?',
  'Compare o desempenho ético com o de processo',
];

@Component({
  selector: 'app-ai-chat-widget',
  standalone: true,
  imports: [FormsModule, MarkdownPipe, TranslateModule],
  templateUrl: './ai-chat-widget.component.html',
  styleUrl: './ai-chat-widget.component.scss',
})
export class AiChatWidgetComponent implements AfterViewChecked {
  @Input({ required: true }) projectId!: number;
  @Input({ required: true }) questionnaireId!: number;

  @ViewChild('messagesContainer') messagesContainer!: ElementRef;

  private readonly aiService = inject(AiDashboardService);

  isOpen = signal(false);
  messages = signal<ChatMessage[]>([]);
  currentInput = '';
  streaming = signal(false);
  private streamSub: Subscription | null = null;
  private shouldScroll = false;

  readonly suggestedQuestions = SUGGESTED_QUESTIONS;

  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  toggle(): void {
    this.isOpen.update(v => !v);
  }

  close(): void {
    this.isOpen.set(false);
  }

  sendMessage(question?: string): void {
    const text = (question ?? this.currentInput).trim();
    if (!text || this.streaming()) return;

    this.currentInput = '';

    this.messages.update(msgs => [
      ...msgs,
      { role: 'user', content: text, timestamp: new Date() },
    ]);

    this.streaming.set(true);
    this.shouldScroll = true;

    // Add empty assistant message that will be filled by streaming
    this.messages.update(msgs => [
      ...msgs,
      { role: 'assistant', content: '', timestamp: new Date() },
    ]);

    this.streamSub = this.aiService
      .askQuestion(this.projectId, this.questionnaireId, text)
      .subscribe({
        next: (chunk) => {
          this.messages.update(msgs => {
            const updated = [...msgs];
            const lastMsg = updated[updated.length - 1];
            if (lastMsg.role === 'assistant') {
              updated[updated.length - 1] = {
                ...lastMsg,
                content: lastMsg.content + chunk,
              };
            }
            return updated;
          });
          this.shouldScroll = true;
        },
        error: () => {
          this.messages.update(msgs => {
            const updated = [...msgs];
            const lastMsg = updated[updated.length - 1];
            if (lastMsg.role === 'assistant' && !lastMsg.content) {
              updated[updated.length - 1] = {
                ...lastMsg,
                content: 'Erro ao obter resposta da IA. Tente novamente.',
              };
            }
            return updated;
          });
          this.streaming.set(false);
        },
        complete: () => {
          this.streaming.set(false);
          this.shouldScroll = true;
        },
      });
  }

  cancelStream(): void {
    if (this.streamSub) {
      this.streamSub.unsubscribe();
      this.streamSub = null;
    }
    this.streaming.set(false);
  }

  clearChat(): void {
    this.cancelStream();
    this.messages.set([]);
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  private scrollToBottom(): void {
    try {
      const el = this.messagesContainer?.nativeElement;
      if (el) {
        el.scrollTop = el.scrollHeight;
      }
    } catch {
      // ignore
    }
  }
}
