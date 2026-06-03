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

      h2 {
        margin: 0 0 16px 0;
        font-size: 18px;
        font-weight: 600;
        color: #1a1a1a;
      }

      p {
        margin: 0 0 24px 0;
        font-size: 13px;
        color: #555555;
        line-height: 1.6;
      }
    }

    .dialog-actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;

      .btn {
        padding: 10px 16px;
        font-size: 13px;
        font-weight: 500;
        border: none;
        border-radius: 4px;
        cursor: pointer;
        transition: all 200ms ease;

        &.btn-secondary {
          background-color: #f0f0f0;
          color: #1a1a1a;

          &:hover {
            background-color: #e0e0e0;
          }
        }

        &.btn-primary {
          background-color: #1565c0;
          color: #ffffff;

          &:hover {
            background-color: #0d47a1;
          }
        }
      }
    }
  `],
})
export class MissingAiTokenDialogComponent {
  private readonly modalService = inject(ModalService);
  private readonly router = inject(Router);

  onCancel(): void {
    this.modalService.closeDialog();
  }

  onConfigureNow(): void {
    this.modalService.closeDialog();
    this.router.navigate(['/settings/ai-token']);
  }
}
