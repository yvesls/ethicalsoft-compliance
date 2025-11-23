import { Component, Output, EventEmitter, inject, Input, ChangeDetectorRef, ChangeDetectionStrategy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { BasePageComponent } from '../../../../core/abstractions/base-page.component';
import { ModalService } from '../../../../core/services/modal.service';
import { InputComponent } from '../../../../shared/components/input/input.component';
import { MultiSelectComponent } from '../../../../shared/components/multi-select/multi-select.component';
import { ActionType } from '../../../../shared/enums/action-type.enum';
import { REPRESENTATIVE_ROLE_OPTIONS } from '../../../../shared/enums/representative-role.enum';
import { capitalizeWords } from '../../../../core/utils/common-utils';

export interface RepresentativeData {
  id?: string;
  firstName: string;
  lastName: string;
  email: string;
  weight: number;
  roles: string[];
}

@Component({
  selector: 'app-representative-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, InputComponent, MultiSelectComponent],
  templateUrl: './representative-modal.component.html',
  styleUrls: ['./representative-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RepresentativeModalComponent extends BasePageComponent implements OnInit {
  @Input() editData?: RepresentativeData;
  @Input() mode: ActionType = ActionType.CREATE;
  @Output() representativeCreated = new EventEmitter<RepresentativeData>();
  @Output() representativeUpdated = new EventEmitter<RepresentativeData>();

  private modalService = inject(ModalService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);

  form!: FormGroup;
  actionType: ActionType = ActionType.CREATE;
  representativeData?: RepresentativeData;
  modalTitle: string = 'Criar novo representante';
  roleOptions = REPRESENTATIVE_ROLE_OPTIONS;

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
      this.representativeData = this.editData;
      this.form.patchValue({
        firstName: capitalizeWords(this.editData.firstName),
        lastName: capitalizeWords(this.editData.lastName),
        email: this.editData.email,
        weight: this.editData.weight,
        roles: this.editData.roles
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
      representativeData: this.representativeData
    };
  }

  protected restore(restoreParameter: any): void {
    if (restoreParameter?.hasParams && restoreParameter.formValue) {
      this.form.patchValue(restoreParameter.formValue, { emitEvent: false });
      this.actionType = restoreParameter.actionType || ActionType.CREATE;
      this.representativeData = restoreParameter.representativeData;
      this.updateModalTitle();
      this.cdr.detectChanges();
    }
  }

  protected loadParams(params: any): void {
    if (params?.action) {
      this.actionType = params.action;
    }

    if (params?.data) {
      this.representativeData = params.data;
      if (this.representativeData) {
        this.form.patchValue({
          firstName: capitalizeWords(this.representativeData.firstName),
          lastName: capitalizeWords(this.representativeData.lastName),
          email: this.representativeData.email,
          weight: this.representativeData.weight,
          roles: this.representativeData.roles
        }, { emitEvent: false });
      }
    }

    this.updateModalTitle();
    this.cdr.detectChanges();
  }

  private initializeForm(): void {
    this.form = this.fb.group({
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      weight: [1, [Validators.required, Validators.min(1)]],
      roles: [[], [Validators.required, Validators.minLength(1)]]
    });
  }

  private updateModalTitle(): void {
    this.modalTitle = this.actionType === ActionType.EDIT
      ? 'Editar representante'
      : 'Criar novo representante';
  }

  confirm(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const representativeFormData: RepresentativeData = {
      firstName: capitalizeWords(this.form.value.firstName),
      lastName: capitalizeWords(this.form.value.lastName),
      email: this.form.value.email,
      weight: this.form.value.weight,
      roles: this.form.value.roles
    };

    if (this.actionType === ActionType.EDIT && this.representativeData?.id) {
      representativeFormData.id = this.representativeData.id;
      this.representativeUpdated.emit(representativeFormData);
    } else {
      this.representativeCreated.emit(representativeFormData);
    }

    this.modalService.close();
  }

  cancel(): void {
    this.modalService.close();
  }
}
