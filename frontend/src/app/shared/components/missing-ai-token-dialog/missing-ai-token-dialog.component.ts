import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { ModalService } from '../../../core/services/modal.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-missing-ai-token-dialog',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  template: `
    <div class="missing-token-dialog">
      <h2>{{ 'ai.token.missing.title' | translate }}</h2>
      <p>{{ 'ai.token.missing.body' | translate }}</p>
      <div class="dialog-actions">
        <button class="btn btn-secondary" (click)="onCancel()">
          {{ 'ai.token.missing.cancel' | translate }}
        </button>
        <button class="btn btn-primary" (click)="onConfigureNow()">
          {{ 'ai.token.missing.configureNow' | translate }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    .missing-token-dialog {
      padding: 24px;
      h2 { margin: 0 0 12px; font-size: 18px; color: #1a1a1a; }
      p { margin: 0 0 24px; font-size: 14px; color: #555; line-height: 1.6; }
    }
    .dialog-actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
    }
  `],
})
export class MissingAiTokenDialogComponent {
  private readonly modalService = inject(ModalService);
  private readonly router = inject(Router);

  onCancel(): void {
    this.modalService.close();
  }

  onConfigureNow(): void {
    this.modalService.close();
    this.router.navigate(['/settings/ai-token']);
  }
}
