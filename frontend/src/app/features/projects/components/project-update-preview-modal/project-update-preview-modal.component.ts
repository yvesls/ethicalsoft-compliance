import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ChangesSummary } from '../../../../shared/interfaces/project/project-update.interface';

interface SummaryLine {
  label: string;
  added: number;
  updated: number;
  removed: number;
}

@Component({
  selector: 'app-project-update-preview-modal',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './project-update-preview-modal.component.html',
  styleUrls: ['./project-update-preview-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectUpdatePreviewModalComponent {
  @Input() title = 'projects.update_preview.title';
  @Input() summary: ChangesSummary | null = null;
  @Input() confirmLabel = 'projects.update_preview.confirm_label';
  @Input() cancelLabel = 'common.cancel';
  @Input() isBlocked = false;

  private readonly translate = inject(TranslateService);

  @Output() confirmed = new EventEmitter<void>();
  @Output() canceled = new EventEmitter<void>();

  get summaryLines(): SummaryLine[] {
    if (!this.summary) {
      return [];
    }

    return [
      {
        label: this.translate.instant('projects.sections.stages'),
        added: this.summary.stagesAdded,
        updated: this.summary.stagesUpdated,
        removed: this.summary.stagesRemoved,
      },
      {
        label: this.translate.instant('projects.detail.iterations'),
        added: this.summary.iterationsAdded,
        updated: this.summary.iterationsUpdated,
        removed: this.summary.iterationsRemoved,
      },
      {
        label: this.translate.instant('projects.sections.questionnaires'),
        added: this.summary.questionnairesAdded,
        updated: this.summary.questionnairesUpdated,
        removed: this.summary.questionnairesRemoved,
      },
      {
        label: this.translate.instant('projects.sections.questions'),
        added: this.summary.questionsAdded,
        updated: this.summary.questionsUpdated,
        removed: this.summary.questionsRemoved,
      },
      {
        label: this.translate.instant('projects.sections.representatives'),
        added: this.summary.representativesAdded,
        updated: this.summary.representativesUpdated,
        removed: this.summary.representativesRemoved,
      },
    ].filter((line) => line.added > 0 || line.updated > 0 || line.removed > 0);
  }

  get hasResponseChanges(): boolean {
    if (!this.summary) {
      return false;
    }
    return (
      this.summary.responsesCreated > 0 ||
      this.summary.responsesUpdated > 0 ||
      this.summary.responsesDeleted > 0
    );
  }

  get hasNotifications(): boolean {
    return !!this.summary && this.summary.notificationsSent > 0;
  }

  get warnings(): string[] {
    return this.summary?.warnings ?? [];
  }

  get blockedReasons(): string[] {
    return this.summary?.blockedReasons ?? [];
  }

  get hasNoChanges(): boolean {
    return this.summaryLines.length === 0 && !this.hasResponseChanges;
  }

  onConfirm(): void {
    this.confirmed.emit();
  }

  onCancel(): void {
    this.canceled.emit();
  }
}
