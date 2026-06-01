import { Component, Output, EventEmitter, inject, ChangeDetectorRef, ChangeDetectionStrategy, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
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
  imports: [CommonModule, ReactiveFormsModule, InputComponent, TranslateModule],
  templateUrl: './stage-iterative-modal.component.html',
  styleUrls: ['./stage-iterative-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
  export class StageIterativeModalComponent implements OnInit {
  @Input() editData?: StageIterativeData;
  @Input() mode: ActionType = ActionType.CREATE;
    @Input() existingStageNames: string[] = [];
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

    this.form.get('name')?.addValidators(this.stageNameUniquenessValidator());
    this.form.get('name')?.updateValueAndValidity({ emitEvent: false });

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
      name: ['', [Validators.required, this.nonBlankValidator()]],
      weight: [1, [Validators.required, Validators.min(1)]]
    });
  }

  private nonBlankValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const value = (control.value ?? '').toString();
      return value.trim().length === 0 ? { blankValue: true } : null;
    };
  }

  private stageNameUniquenessValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const normalizedName = (control.value ?? '').toString().trim().toLowerCase();

      if (!normalizedName) {
        return null;
      }

      if (this.actionType === ActionType.EDIT) {
        const currentName = (this.editData?.name ?? '').toString().trim().toLowerCase();
        if (currentName === normalizedName) {
          return null;
        }
      }

      const duplicated = this.existingStageNames.some(
        (name) => name.trim().toLowerCase() === normalizedName
      );

      return duplicated ? { duplicateStageName: true } : null;
    };
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
      name: (this.form.value.name ?? '').toString().trim(),
      weight: Number(this.form.value.weight)
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
