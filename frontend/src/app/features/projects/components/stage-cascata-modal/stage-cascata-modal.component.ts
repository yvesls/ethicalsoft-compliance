import { Component, Output, EventEmitter, inject, Input, ChangeDetectorRef, ChangeDetectionStrategy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { BasePageComponent } from '../../../../core/abstractions/base-page.component';
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

@Component({
  selector: 'app-stage-cascata-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, InputComponent],
  templateUrl: './stage-cascata-modal.component.html',
  styleUrls: ['./stage-cascata-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StageCascataModalComponent extends BasePageComponent implements OnInit {
  @Input() editData?: StageCascataData;
  @Input() mode: ActionType = ActionType.CREATE;
  @Output() stageCreated = new EventEmitter<StageCascataData>();
  @Output() stageUpdated = new EventEmitter<StageCascataData>();
  @Input() projectStartDate?: string;
  @Input() projectDeadline?: string;
  @Input() projectDurationDays?: number;
  @Input() existingStages: { weight: number; durationDays: number; sequence: number }[] = [];
  @Input() existingSequences: number[] = [];

  private modalService = inject(ModalService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);

  form!: FormGroup;
  calculatedDateRange: DateRange | null = null;
  actionType: ActionType = ActionType.CREATE;
  stageData?: StageCascataData;
  modalTitle: string = 'Criar nova etapa - Cascata';

  constructor() {
    super();
    this.initializeForm();
  }

  override ngOnInit(): void {
    super.ngOnInit();

    if (this.mode) {
      this.actionType = this.mode;
    }

    if (this.editData) {
      this.stageData = this.editData;
      this.form.patchValue({
        name: this.editData.name,
        weight: this.editData.weight,
        sequence: this.editData.sequence,
        durationDays: this.editData.durationDays
      }, { emitEvent: false });

      this.calculatedDateRange = {
        startDate: this.editData.applicationStartDate,
        endDate: this.editData.applicationEndDate
      };
    }

    this.updateModalTitle();
    this.cdr.detectChanges();
  }

  protected onInit(): void {
  }

  protected save(): any {
    return {
      formValue: this.form.value,
      actionType: this.actionType,
      stageData: this.stageData,
      calculatedDateRange: this.calculatedDateRange
    };
  }

  protected restore(restoreParameter: any): void {
    if (restoreParameter?.hasParams && restoreParameter.formValue) {
      this.form.patchValue(restoreParameter.formValue, { emitEvent: false });
      this.actionType = restoreParameter.actionType || ActionType.CREATE;
      this.stageData = restoreParameter.stageData;
      this.calculatedDateRange = restoreParameter.calculatedDateRange;
      this.updateModalTitle();
      this.cdr.detectChanges();
    }
  }

  protected loadParams(params: any): void {
    if (params?.action) {
      this.actionType = params.action;
    }

    if (params?.data) {
      this.stageData = params.data;
      if (this.stageData) {
        this.form.patchValue({
          name: this.stageData.name,
          weight: this.stageData.weight,
          sequence: this.stageData.sequence,
          durationDays: this.stageData.durationDays
        }, { emitEvent: false });

        this.calculatedDateRange = {
          startDate: this.stageData.applicationStartDate,
          endDate: this.stageData.applicationEndDate
        };
      }
    }

    this.updateModalTitle();
    this.cdr.detectChanges();
  }

  private initializeForm(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required]],
      weight: [1, [Validators.required, Validators.min(1)]],
      sequence: [1, [Validators.required, Validators.min(1)]],
      durationDays: [0, [Validators.required, Validators.min(1)]]
    });

    this.form.get('weight')?.valueChanges.subscribe(() => {
      this.calculateApplicationRange();
    });

    this.form.get('durationDays')?.valueChanges.subscribe(() => {
      this.calculateApplicationRange();
    });

    this.form.get('sequence')?.valueChanges.subscribe(() => {
      this.calculateApplicationRange();
    });
  }

  private updateModalTitle(): void {
    this.modalTitle = this.actionType === ActionType.EDIT
      ? 'Editar etapa - Cascata'
      : 'Criar nova etapa - Cascata';
  }

  private calculateApplicationRange(): void {
    const weight = this.form.get('weight')?.value;
    const durationDays = this.form.get('durationDays')?.value;

    if (!this.projectStartDate || !weight || !durationDays || durationDays <= 0) {
      this.calculatedDateRange = null;
      this.cdr.detectChanges();
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

    this.cdr.detectChanges();
  }

  private calculateStageStartDate(): Date {
    const projectStart = new Date(this.projectStartDate!);
    const currentSequence = this.form.get('sequence')?.value || 1;

    // Calcula o total de dias das etapas com sequence menor que a atual
    let totalPreviousDays = 0;
    for (const stage of this.existingStages) {
      // Apenas soma etapas que vêm ANTES na sequência
      if (stage.sequence < currentSequence) {
        totalPreviousDays += stage.durationDays || 0;
      }
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
