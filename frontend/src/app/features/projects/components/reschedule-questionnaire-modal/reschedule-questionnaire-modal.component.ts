import {
  Component,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Input,
  Output,
  EventEmitter,
  OnInit,
  OnDestroy,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { merge, Subject, takeUntil } from 'rxjs';

import { ModalService } from '../../../../core/services/modal.service';
import { InputComponent } from '../../../../shared/components/input/input.component';
import { BusinessDaysUtils } from '../../../../core/utils/business-days-utils';
import { RescheduleQuestionnairePayload } from '../../../../shared/interfaces/project/project-questionnaire.interface';
import { RescheduleDateValidators } from '../../../../shared/validators/reschedule-date.validator';

export interface RescheduleModalInput {
  questionnaireName: string;
  currentStartDate: string | null;
  currentEndDate: string | null;
  projectStartDate: string | null;
}

@Component({
  selector: 'app-reschedule-questionnaire-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, InputComponent],
  templateUrl: './reschedule-questionnaire-modal.component.html',
  styleUrls: ['./reschedule-questionnaire-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RescheduleQuestionnaireModalComponent implements OnInit, OnDestroy {
  @Input() questionnaireName = '';
  @Input() currentStartDate: string | null = null;
  @Input() currentEndDate: string | null = null;
  @Input() projectStartDate: string | null = null;

  @Output() confirmed = new EventEmitter<RescheduleQuestionnairePayload>();

  private readonly modalService = inject(ModalService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroy$ = new Subject<void>();

  form!: FormGroup;
  warningMessage = '';

  get formattedCurrentStart(): string {
    return this.formatDate(this.currentStartDate);
  }

  get formattedCurrentEnd(): string {
    return this.formatDate(this.currentEndDate);
  }

  ngOnInit(): void {
    this.form = new FormGroup(
      {
        newApplicationStartDate: new FormControl(null, [Validators.required]),
        newApplicationEndDate: new FormControl(null, [Validators.required]),
      },
      {
        validators: [
          RescheduleDateValidators.endDateAfterStartDate(
            'newApplicationStartDate',
            'newApplicationEndDate'
          ),
          RescheduleDateValidators.startDateNotBefore(
            'newApplicationStartDate',
            () => this.projectStartDate,
            'data de início do projeto'
          ),
        ],
      }
    );

    const startDateCtrl = this.form.get('newApplicationStartDate');
    const endDateCtrl = this.form.get('newApplicationEndDate');

    if (startDateCtrl && endDateCtrl) {
      merge(startDateCtrl.valueChanges, endDateCtrl.valueChanges)
        .pipe(takeUntil(this.destroy$))
        .subscribe(() => {
          this.updateWarning();
          this.cdr.markForCheck();
        });
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  confirm(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload: RescheduleQuestionnairePayload = {
      newApplicationStartDate: this.form.value.newApplicationStartDate,
      newApplicationEndDate: this.form.value.newApplicationEndDate,
    };

    this.confirmed.emit(payload);
  }

  cancel(): void {
    this.modalService.close();
  }

  private updateWarning(): void {
    const start = this.form.get('newApplicationStartDate')?.value;
    if (!start) {
      this.warningMessage = '';
      return;
    }

    const today = new Date().toISOString().split('T')[0];
    this.warningMessage = start <= today
      ? 'Ao definir a data de início para hoje ou uma data passada, o status do questionário será alterado para "Em Andamento" imediatamente.'
      : '';
  }

  private formatDate(date: string | null): string {
    return date ? BusinessDaysUtils.formatDateBR(date) : '---';
  }
}
