import { Component, Input, Output, EventEmitter, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MarkdownPipe } from '../../../../shared/utils/markdown.pipe';
import { AiInsightResult } from '../../interfaces/ai.interface';

@Component({
  selector: 'app-ai-panel',
  standalone: true,
  imports: [DatePipe, MarkdownPipe],
  templateUrl: './ai-panel.component.html',
  styleUrl: './ai-panel.component.scss',
})
export class AiPanelComponent {
  @Input({ required: true }) title = '';
  @Input() icon = 'bi-robot';
  @Input() result: AiInsightResult | null = null;
  @Input() loading = false;
  @Input() buttonLabel = 'Gerar Análise';
  @Input() loadingLabel = 'Analisando com IA...';

  @Output() generate = new EventEmitter<void>();

  expanded = signal(false);

  toggleExpanded(): void {
    this.expanded.update(v => !v);
  }

  onGenerate(): void {
    this.expanded.set(true);
    this.generate.emit();
  }
}
