import { Component, Output, EventEmitter, inject, Input, ChangeDetectorRef, ChangeDetectionStrategy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { ModalService } from '../../../../core/services/modal.service';
import { InputComponent } from '../../../../shared/components/input/input.component';
import { BusinessDaysUtils } from '../../../../core/utils/business-days-utils';
import { ActionType } from '../../../../shared/enums/action-type.enum';

export interface StageCascataData {
  id?: string;
  name: string;
  weight: number;
  sequence: number;
  durationDays: number;
  applicationStartDate: string;
  applicationEndDate: string;
}

interface DateRange {
  startDate: string;
  endDate: string;
  exceedsDeadline?: boolean;
}

interface StageContextData {
  index: number;
  weight: number;
  durationDays: number;
  sequence: number;
}

@Component({
  selector: 'app-stage-cascata-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, InputComponent, TranslateModule],
  templateUrl: './stage-cascata-modal.component.html',
  styleUrls: ['./stage-cascata-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
  export class StageCascataModalComponent implements OnInit {
  @Input() editData?: StageCascataData;
  @Input() mode: ActionType = ActionType.CREATE;
  @Output() stageCreated = new EventEmitter<StageCascataData>();
  @Output() stageUpdated = new EventEmitter<StageCascataData>();
  @Input() projectStartDate?: string;
  @Input() projectDeadline?: string;
  @Input() projectDurationDays?: number;
  @Input() existingStages: StageContextData[] = [];
  @Input() currentStageIndex?: number;
  @Input() existingSequences: number[] = [];

  private modalService = inject(ModalService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);
  private readonly translate = inject(TranslateService);

  form!: FormGroup;
  calculatedDateRange: DateRange | null = null;
  actionType: ActionType = ActionType.CREATE;
  stageData?: StageCascataData;
  modalTitle = '';

  constructor() {
    this.initializeForm();
  }

  ngOnInit(): void {
    this.actionType = this.mode ?? ActionType.CREATE;

    this.form.get('sequence')?.addValidators(this.sequenceUniquenessValidator());
    this.form.get('sequence')?.updateValueAndValidity({ emitEvent: false });

    if (this.editData) {
      this.stageData = this.editData;
      this.form.patchValue(
        {
          name: this.editData.name,
          weight: this.editData.weight,
          sequence: this.editData.sequence,
          durationDays: this.editData.durationDays,
        },
        { emitEvent: false }
      );

      this.calculatedDateRange = {
        startDate: this.editData.applicationStartDate,
        endDate: this.editData.applicationEndDate,
      };
    }

    this.setupFormListeners();
    this.calculateApplicationRange();
    this.updateModalTitle();
    this.cdr.detectChanges();
  }

  private sequenceUniquenessValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const sequence = Number(control.value);

      if (!Number.isFinite(sequence) || sequence <= 0) {
        return null;
      }

      if (this.actionType === ActionType.EDIT && this.editData?.sequence === sequence) {
        return null;
      }

      return this.existingSequences.includes(sequence)
        ? { duplicateSequence: true }
        : null;
    };
  }

  private initializeForm(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required]],
      weight: [1, [Validators.required, Validators.min(1)]],
      sequence: [1, [Validators.required, Validators.min(1)]],
      durationDays: [0, [Validators.required, Validators.min(1)]]
    });
  }

  private setupFormListeners(): void {
    this.form.get('durationDays')?.valueChanges.subscribe(() => this.calculateApplicationRange());
    this.form.get('sequence')?.valueChanges.subscribe(() => this.calculateApplicationRange());
  }

  private calculateApplicationRange(): void {
    const durationDays = Number(this.form.get('durationDays')?.value);
    const sequence = Number(this.form.get('sequence')?.value);

    if (!this.projectStartDate || !Number.isFinite(durationDays) || durationDays <= 0 || !Number.isFinite(sequence) || sequence <= 0) {
      this.calculatedDateRange = null;
      this.cdr.markForCheck();
      return;
    }

    const projectStartDate = BusinessDaysUtils.parseISODate(this.projectStartDate);
    if (Number.isNaN(projectStartDate.getTime())) {
      this.calculatedDateRange = null;
      this.cdr.markForCheck();
      return;
    }

    const candidate: StageContextData = {
      index: this.currentStageIndex ?? this.existingStages.length,
      weight: Number(this.form.get('weight')?.value) || 0,
      durationDays,
      sequence,
    };

    const contextualStages = this.existingStages.map((stage) => ({
      ...stage,
      index: Number(stage.index),
    }));

    const stagesForSimulation = this.actionType === ActionType.EDIT && this.currentStageIndex !== undefined
      ? contextualStages.map((stage) =>
          stage.index === this.currentStageIndex
            ? { ...candidate }
            : stage
        )
      : [...contextualStages, candidate];

    const sortedStages = stagesForSimulation
      .filter((stage) => Number.isFinite(stage.sequence) && stage.sequence > 0)
      .sort((a, b) => (a.sequence - b.sequence) || (a.index - b.index));

    let stageStart = new Date(projectStartDate);
    let computedRange: DateRange | null = null;

    for (const stage of sortedStages) {
      const stageDuration = Math.max(Number(stage.durationDays) || 0, 0);
      const openingOffset = Math.max(Math.round(stageDuration * 0.1), 0);
      const closingOffset = Math.max(Math.round(stageDuration * 0.9), openingOffset);

      const openingDate = BusinessDaysUtils.addBusinessDays(stageStart, openingOffset);
      const closingDate = BusinessDaysUtils.addBusinessDays(stageStart, closingOffset);

      if (stage.index === candidate.index) {
        let exceedsDeadline = false;
        if (this.projectDeadline) {
          const deadlineDate = BusinessDaysUtils.parseISODate(this.projectDeadline);
          if (!Number.isNaN(deadlineDate.getTime())) {
            exceedsDeadline = closingDate > deadlineDate;
          }
        }

        computedRange = {
          startDate: BusinessDaysUtils.formatDateISO(openingDate),
          endDate: BusinessDaysUtils.formatDateISO(closingDate),
          exceedsDeadline,
        };
      }

      stageStart = BusinessDaysUtils.addBusinessDays(stageStart, stageDuration);
    }

    this.calculatedDateRange = computedRange;
    this.cdr.markForCheck();
  }

  formatDateBR(isoDate: string): string {
    return BusinessDaysUtils.formatDateBR(isoDate);
  }

  private updateModalTitle(): void {
    this.modalTitle = this.translate.instant(
      this.actionType === ActionType.EDIT
        ? 'projects.stage_cascata.edit_title'
        : 'projects.stage_cascata.create_title'
    );
  }

  confirm(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.calculateApplicationRange();

    if (!this.calculatedDateRange) {
      return;
    }

    if (this.calculatedDateRange.exceedsDeadline) {
      return;
    }

    const stageFormData: StageCascataData = {
      name: this.form.value.name,
      weight: this.form.value.weight,
      sequence: this.form.value.sequence,
      durationDays: this.form.value.durationDays,
      applicationStartDate: this.calculatedDateRange.startDate,
      applicationEndDate: this.calculatedDateRange.endDate
    };

    if (this.actionType === ActionType.EDIT && this.stageData?.id) {
      stageFormData.id = this.stageData.id;
      this.stageUpdated.emit(stageFormData);
    } else {
      this.stageCreated.emit(stageFormData);
    }

    this.modalService.close();
  }
}
