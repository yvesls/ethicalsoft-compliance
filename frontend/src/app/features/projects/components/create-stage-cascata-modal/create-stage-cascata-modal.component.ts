import { Component, OnInit, Output, EventEmitter, inject, Input, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ModalService } from '../../../../core/services/modal.service';
import { InputComponent } from '../../../../shared/components/input/input.component';
import { BusinessDaysUtils } from '../../../../core/utils/business-days-utils';

export interface NewStageCascataData {
  name: string;
  weight: number;
  durationDays: number;
  applicationStartDate: string;
  applicationEndDate: string;
}

interface DateRange {
  startDate: string;
  endDate: string;
  exceedsDeadline?: boolean;
}

@Component({
  selector: 'app-create-stage-cascata-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, InputComponent],
  templateUrl: './create-stage-cascata-modal.component.html',
  styleUrls: ['./create-stage-cascata-modal.component.scss']
})
export class CreateStageCascataModalComponent implements OnInit {
  @Output() stageCreated = new EventEmitter<NewStageCascataData>();
  @Input() projectStartDate?: string;
  @Input() projectDeadline?: string;
  @Input() projectDurationDays?: number;
  @Input() existingStages: { weight: number; durationDays: number }[] = [];

  private modalService = inject(ModalService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);

  form!: FormGroup;
  calculatedDateRange: DateRange | null = null;

  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required]],
      weight: [1, [Validators.required, Validators.min(1)]],
      durationDays: [0, [Validators.required, Validators.min(1)]]
    });

    this.form.get('weight')?.valueChanges.subscribe(() => {
      this.calculateApplicationRange();
    });

    this.form.get('durationDays')?.valueChanges.subscribe(() => {
      this.calculateApplicationRange();
    });
  }

  private calculateApplicationRange(): void {
    const weight = this.form.get('weight')?.value;
    const durationDays = this.form.get('durationDays')?.value;

    if (!this.projectStartDate || !weight || !durationDays || durationDays <= 0) {
      this.calculatedDateRange = null;
      this.cdr.markForCheck();
      return;
    }

    const stageStartDate = this.calculateStageStartDate();

    const { openingDate, closingDate } = BusinessDaysUtils.calculateApplicationRange(
      stageStartDate,
      durationDays
    );

    const startDateISO = BusinessDaysUtils.formatDateISO(openingDate);
    const endDateISO = BusinessDaysUtils.formatDateISO(closingDate);

    let exceedsDeadline = false;
    if (this.projectDeadline) {
      const deadlineDate = new Date(this.projectDeadline);
      exceedsDeadline = closingDate > deadlineDate;
    }

    this.calculatedDateRange = {
      startDate: startDateISO,
      endDate: endDateISO,
      exceedsDeadline
    };

    this.cdr.markForCheck();
  }

  private calculateStageStartDate(): Date {
    const projectStart = new Date(this.projectStartDate!);

    let totalPreviousDays = 0;
    for (const stage of this.existingStages) {
      totalPreviousDays += stage.durationDays || 0;
    }

    return BusinessDaysUtils.addBusinessDays(projectStart, totalPreviousDays);
  }

  formatDateBR(isoDate: string): string {
    if (!isoDate) return '';
    return BusinessDaysUtils.formatDateBR(isoDate);
  }

  confirm(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (!this.calculatedDateRange) {
      return;
    }

    if (this.calculatedDateRange.exceedsDeadline) {
      return;
    }

    const newStage: NewStageCascataData = {
      name: this.form.value.name,
      weight: this.form.value.weight,
      durationDays: this.form.value.durationDays,
      applicationStartDate: this.calculatedDateRange.startDate,
      applicationEndDate: this.calculatedDateRange.endDate
    };

    this.stageCreated.emit(newStage);
    this.modalService.close();
  }
}
