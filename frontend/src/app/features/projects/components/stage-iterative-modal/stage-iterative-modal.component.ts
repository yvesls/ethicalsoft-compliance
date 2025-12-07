import { Component, Output, EventEmitter, inject, ChangeDetectorRef, ChangeDetectionStrategy, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
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
  export class StageIterativeModalComponent implements OnInit {
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
  modalTitle = 'Criar nova etapa';

  constructor() {
    this.initializeForm();
  }

  ngOnInit(): void {
    this.actionType = this.mode ?? ActionType.CREATE;

    if (this.editData) {
      this.stageData = this.editData;
      this.form.patchValue(
        {
          name: this.editData.name,
          weight: this.editData.weight,
        },
        { emitEvent: false }
      );
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
