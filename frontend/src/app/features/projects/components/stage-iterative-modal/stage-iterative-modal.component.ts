import { Component, Output, EventEmitter, inject, ChangeDetectorRef, ChangeDetectionStrategy, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { BasePageComponent, RestoreParams } from '../../../../core/abstractions/base-page.component';
import { ModalService } from '../../../../core/services/modal.service';
import { InputComponent } from '../../../../shared/components/input/input.component';
import { ActionType } from '../../../../shared/enums/action-type.enum';
import { GenericParams, RouteParams } from '../../../../core/services/router.service';

export interface StageIterativeData {
  id?: string;
  name: string;
  weight: number;
}

interface StageIterativeFormValue {
  name: string;
  weight: number;
}

interface StageIterativeRouteParams extends GenericParams {
  action?: ActionType;
  data?: StageIterativeData;
  formValue?: StageIterativeFormValue;
  actionType?: ActionType;
  stageData?: StageIterativeData;
}

@Component({
  selector: 'app-stage-iterative-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, InputComponent],
  templateUrl: './stage-iterative-modal.component.html',
  styleUrls: ['./stage-iterative-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StageIterativeModalComponent extends BasePageComponent<StageIterativeRouteParams> {
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
    super();
    this.initializeForm();
  }

  protected override onInit(): void {
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

  protected override save(): RouteParams<StageIterativeRouteParams> {
    const formValue = this.form.getRawValue() as StageIterativeFormValue;
    return {
      formValue,
      actionType: this.actionType,
      stageData: this.stageData,
    };
  }

  protected override restore(restoreParameter: RestoreParams<StageIterativeRouteParams>): void {
    if (!restoreParameter.hasParams) {
      return;
    }

    const savedFormValue = restoreParameter['formValue'] as StageIterativeFormValue | undefined;
    if (savedFormValue) {
      this.form.patchValue(savedFormValue, { emitEvent: false });
    }

    const savedActionType = restoreParameter['actionType'] as ActionType | undefined;
    this.actionType = savedActionType ?? ActionType.CREATE;
    this.stageData = restoreParameter['stageData'] as StageIterativeData | undefined;
    this.updateModalTitle();
    this.cdr.detectChanges();
  }

  protected override loadParams(params: RouteParams<StageIterativeRouteParams>): void {
    const routeAction = params?.['action'] as ActionType | undefined;
    if (routeAction) {
      this.actionType = routeAction;
    }

    const routeData = params?.['data'] as StageIterativeData | undefined;
    if (routeData) {
      this.stageData = routeData;
      this.form.patchValue(
        {
          name: routeData.name,
          weight: routeData.weight,
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
