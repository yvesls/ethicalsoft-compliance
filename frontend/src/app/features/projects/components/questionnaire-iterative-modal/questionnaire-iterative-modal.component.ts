import { Component, Output, EventEmitter, inject, Input, ChangeDetectorRef, ChangeDetectionStrategy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BasePageComponent, RestoreParams } from '../../../../core/abstractions/base-page.component';
import { ModalService } from '../../../../core/services/modal.service';
import { InputComponent } from '../../../../shared/components/input/input.component';
import { SelectComponent, SelectOption } from '../../../../shared/components/select/select.component';
import { FormUtils } from '../../../../shared/utils/form-utils';
import { ActionType } from '../../../../shared/enums/action-type.enum';
import { GenericParams, RouteParams } from '../../../../core/services/router.service';

export interface QuestionnaireIterativeData {
  id?: string;
  name: string;
  iteration: string;
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

interface QuestionnaireFormValue {
  name: string;
  iteration: string;
  weight: number;
  durationDays: number;
}

type QuestionnaireRouteState = RouteParams<GenericParams> & {
  formValue: QuestionnaireFormValue;
  actionType: ActionType;
  questionnaireData?: QuestionnaireIterativeData;
  calculatedDateRange: DateRange | null;
};

type QuestionnaireRestoreState = RestoreParams<GenericParams> & Partial<QuestionnaireRouteState>;

@Component({
  selector: 'app-questionnaire-iterative-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, InputComponent, SelectComponent],
  templateUrl: './questionnaire-iterative-modal.component.html',
  styleUrls: ['./questionnaire-iterative-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuestionnaireIterativeModalComponent extends BasePageComponent implements OnInit {
  @Input() editData?: QuestionnaireIterativeData;
  @Input() mode: ActionType = ActionType.CREATE;
  @Output() questionnaireCreated = new EventEmitter<QuestionnaireIterativeData>();
  @Output() questionnaireUpdated = new EventEmitter<QuestionnaireIterativeData>();
  @Input() projectStartDate?: string;
  @Input() projectDeadline?: string;
  @Input() iterationDuration?: number;
  @Input() iterationCount?: number;
  @Input() existingQuestionnaires: { weight: number; iteration: string }[] = [];
  @Input() iterationOptions: SelectOption[] = [];

  private modalService = inject(ModalService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);

  form!: FormGroup;
  calculatedDateRange: DateRange | null = null;
  actionType: ActionType = ActionType.CREATE;
  questionnaireData?: QuestionnaireIterativeData;
  modalTitle = 'Criar novo questionário';

  constructor() {
    super();
  }

  override ngOnInit(): void {
    this.initializeForm();
    super.ngOnInit();

    if (this.mode) {
      this.actionType = this.mode;
    }

    if (this.editData) {
      this.questionnaireData = this.editData;
      this.form.patchValue({
        name: this.editData.name,
        iteration: this.editData.iteration,
        weight: this.editData.weight,
        durationDays: this.editData.durationDays
      }, { emitEvent: false });

      this.calculatedDateRange = {
        startDate: this.editData.applicationStartDate,
        endDate: this.editData.applicationEndDate
      };
    }

    this.setupFormListeners();
    this.updateModalTitle();
    this.cdr.detectChanges();
  }

  protected override onInit(): void {
    return;
  }

  protected override save(): QuestionnaireRouteState {
    return {
      formValue: this.getFormValue(),
      actionType: this.actionType,
      questionnaireData: this.questionnaireData,
      calculatedDateRange: this.calculatedDateRange
    };
  }

  protected override restore(restoreParameter: QuestionnaireRestoreState): void {
    if (restoreParameter?.hasParams && restoreParameter.formValue) {
      this.form.patchValue(restoreParameter.formValue, { emitEvent: false });
      this.actionType = restoreParameter.actionType || ActionType.CREATE;
      this.questionnaireData = restoreParameter.questionnaireData;
      this.calculatedDateRange = restoreParameter.calculatedDateRange ?? null;
      this.updateModalTitle();
      this.cdr.detectChanges();
    }
  }

  protected override loadParams(params: RouteParams<GenericParams>): void {
    const actionParam = params['action'];
    if (actionParam) {
      this.actionType = actionParam as ActionType;
    }

    const dataParam = params['data'];
    if (dataParam) {
      this.questionnaireData = dataParam as QuestionnaireIterativeData;
      if (this.questionnaireData) {
        this.form.patchValue({
          name: this.questionnaireData.name,
          iteration: this.questionnaireData.iteration,
          weight: this.questionnaireData.weight,
          durationDays: this.questionnaireData.durationDays
        }, { emitEvent: false });

        this.calculatedDateRange = {
          startDate: this.questionnaireData.applicationStartDate,
          endDate: this.questionnaireData.applicationEndDate
        };
      }
    }

    this.updateModalTitle();
    this.cdr.detectChanges();
  }

  private initializeForm(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required]],
      iteration: ['', [Validators.required]],
      weight: [1, [Validators.required, Validators.min(1)]],
      durationDays: [0, [Validators.required, Validators.min(1)]]
    });
  }

  private setupFormListeners(): void {
    this.form.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        this.calculateDateRange();
      });
  }

  private calculateDateRange(): void {
    const { iteration, durationDays } = this.getFormValue();

    if (!this.projectStartDate || !iteration || !this.iterationDuration || !durationDays) {
      this.calculatedDateRange = null;
      this.cdr.detectChanges();
      return;
    }

  const iterationNumber = Number.parseInt(iteration.replaceAll(/\D/g, ''));
  if (Number.isNaN(iterationNumber)) {
      this.calculatedDateRange = null;
      this.cdr.detectChanges();
      return;
    }

    const iterationStartOffset = (iterationNumber - 1) * this.iterationDuration;
    const projectStart = new Date(this.projectStartDate);
    const iterationStartDate = FormUtils.addBusinessDays(projectStart, iterationStartOffset);

    const openingOffset = Math.round(durationDays * 0.1);
    const closingOffset = Math.round(durationDays * 0.9);

    const applicationStartDate = FormUtils.addBusinessDays(iterationStartDate, openingOffset);
    const applicationEndDate = FormUtils.addBusinessDays(iterationStartDate, closingOffset);

    let exceedsDeadline = false;
    if (this.projectDeadline) {
      const deadline = new Date(this.projectDeadline);
      exceedsDeadline = applicationEndDate > deadline;
    }

    this.calculatedDateRange = {
      startDate: FormUtils.formatDateISO(applicationStartDate),
      endDate: FormUtils.formatDateISO(applicationEndDate),
      exceedsDeadline
    };

    this.cdr.detectChanges();
  }

  private updateModalTitle(): void {
    this.modalTitle = this.actionType === ActionType.EDIT
      ? 'Editar questionário'
      : 'Criar novo questionário';
  }

  confirm(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (!this.calculatedDateRange) {
      return;
    }

    const formValue = this.getFormValue();

    const questionnaireFormData: QuestionnaireIterativeData = {
      name: formValue.name,
      iteration: formValue.iteration,
      weight: formValue.weight,
      durationDays: formValue.durationDays,
      applicationStartDate: this.calculatedDateRange.startDate,
      applicationEndDate: this.calculatedDateRange.endDate
    };

    if (this.actionType === ActionType.EDIT && this.questionnaireData?.id) {
      questionnaireFormData.id = this.questionnaireData.id;
      this.questionnaireUpdated.emit(questionnaireFormData);
    } else {
      this.questionnaireCreated.emit(questionnaireFormData);
    }

    this.modalService.close();
  }

  cancel(): void {
    this.modalService.close();
  }

  getFormattedDateRange(): string {
    if (!this.calculatedDateRange) {
      return 'Preencha os campos para calcular';
    }

    return `${FormUtils.formatDateBR(this.calculatedDateRange.startDate)} - ${FormUtils.formatDateBR(this.calculatedDateRange.endDate)}`;
  }

  private getFormValue(): QuestionnaireFormValue {
    return this.form.getRawValue() as QuestionnaireFormValue;
  }
}
