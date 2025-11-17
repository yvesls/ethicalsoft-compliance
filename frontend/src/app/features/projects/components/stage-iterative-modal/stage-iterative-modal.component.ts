import { Component, Output, EventEmitter, inject, ChangeDetectorRef, ChangeDetectionStrategy, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BasePageComponent } from '../../../../core/abstractions/base-page.component';
import { ModalService } from '../../../../core/services/modal.service';
import { InputComponent } from '../../../../shared/components/input/input.component';
import { ActionType } from '../../../../shared/enums/action-type.enum';

export interface StageIterativeData {
  id?: string;
  name: string;
  weight: number;
}

@Component({
  selector: 'app-stage-iterative-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, InputComponent],
  templateUrl: './stage-iterative-modal.component.html',
  styleUrls: ['./stage-iterative-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StageIterativeModalComponent extends BasePageComponent implements OnInit {
  @Input() editData?: StageIterativeData;
  @Input() mode: ActionType = ActionType.CREATE;
  @Output() stageCreated = new EventEmitter<StageIterativeData>();
  @Output() stageUpdated = new EventEmitter<StageIterativeData>();

  private modalService = inject(ModalService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);

  form!: FormGroup;
  actionType: ActionType = ActionType.CREATE;
  stageData?: StageIterativeData;
  modalTitle: string = 'Criar nova etapa';

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
        weight: this.editData.weight
      }, { emitEvent: false });
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
      stageData: this.stageData
    };
  }

  protected restore(restoreParameter: any): void {
    if (restoreParameter?.hasParams && restoreParameter.formValue) {
      this.form.patchValue(restoreParameter.formValue, { emitEvent: false });
      this.actionType = restoreParameter.actionType || ActionType.CREATE;
      this.stageData = restoreParameter.stageData;
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
          weight: this.stageData.weight
        }, { emitEvent: false });
      }
    }

    this.updateModalTitle();
    this.cdr.detectChanges();
  }

  private initializeForm(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required]],
      weight: [1, [Validators.required, Validators.min(1)]]
    });
  }

  private updateModalTitle(): void {
    this.modalTitle = this.actionType === ActionType.EDIT
      ? 'Editar etapa'
      : 'Criar nova etapa';
  }

  confirm(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const stageFormData: StageIterativeData = {
      name: this.form.value.name,
      weight: this.form.value.weight
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
