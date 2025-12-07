import { Component, Output, EventEmitter, inject, Input, ChangeDetectorRef, ChangeDetectionStrategy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { BasePageComponent, RestoreParams } from '../../../../core/abstractions/base-page.component';
import { ModalService } from '../../../../core/services/modal.service';
import { InputComponent } from '../../../../shared/components/input/input.component';
import { MultiSelectComponent, MultiSelectOption } from '../../../../shared/components/multi-select/multi-select.component';
import { ActionType } from '../../../../shared/enums/action-type.enum';
import { capitalizeWords } from '../../../../core/utils/common-utils';
import { GenericParams, RouteParams } from '../../../../core/services/router.service';
import { RoleService } from '../../../../core/services/role.service';
import { RoleSummary } from '../../../../shared/interfaces/role/role-summary.interface';
import { take } from 'rxjs/operators';

export interface RepresentativeData {
  id?: number | string | null;
  firstName: string;
  lastName: string;
  email: string;
  weight: number;
  roleIds: number[];
  roleNames?: string[];
  userId?: number | null;
  projectId?: number | null;
}

interface RepresentativeFormValue {
  firstName: string;
  lastName: string;
  email: string;
  weight: number;
  roleIds: number[];
}

type RepresentativeRouteState = RouteParams<GenericParams> & {
  formValue: RepresentativeFormValue;
  actionType: ActionType;
  representativeData?: RepresentativeData;
};

type RepresentativeRestoreState = RestoreParams<GenericParams> & Partial<RepresentativeRouteState>;

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
  private roleService = inject(RoleService);

  form!: FormGroup;
  actionType: ActionType = ActionType.CREATE;
  representativeData?: RepresentativeData;
  modalTitle = 'Criar novo representante';
  roleOptions: MultiSelectOption[] = [];
  private rolesLookup = new Map<number, string>();

  constructor() {
    super();
  }

  override ngOnInit(): void {
    this.initializeForm();
    super.ngOnInit();

    this.loadRoleOptions();

    if (this.mode) {
      this.actionType = this.mode;
    }

    if (this.editData) {
      this.representativeData = this.editData;
      this.patchFormWithRepresentative(this.editData);
    }

    this.updateModalTitle();
    this.cdr.detectChanges();
  }

  protected override onInit(): void {
    return;
  }

  protected override save(): RepresentativeRouteState {
    return {
      formValue: this.getFormValue(),
      actionType: this.actionType,
      representativeData: this.representativeData
    };
  }

  protected override restore(restoreParameter: RepresentativeRestoreState): void {
    if (restoreParameter?.hasParams && restoreParameter.formValue) {
      this.form.patchValue(restoreParameter.formValue, { emitEvent: false });
      this.actionType = restoreParameter.actionType || ActionType.CREATE;
      this.representativeData = restoreParameter.representativeData;
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
      this.representativeData = dataParam as RepresentativeData;
      this.patchFormWithRepresentative(this.representativeData);
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
      roleIds: [[], [Validators.required, Validators.minLength(1)]]
    });
  }

  private loadRoleOptions(): void {
    this.roleService
      .getRoles()
      .pipe(take(1))
      .subscribe({
        next: (roles) => {
          const resolvedRoles = roles ?? [];
          this.roleOptions = resolvedRoles.map(this.mapRoleToOption);
          this.rolesLookup = new Map(resolvedRoles.map((role) => [role.id, role.name]));
          this.applyPendingRoleNameConversion();
          this.cdr.markForCheck();
        },
        error: (error) => {
          console.error('Falha ao carregar roles', error);
        }
      });
  }

  private mapRoleToOption(role: RoleSummary): MultiSelectOption {
    return {
      value: role.id,
      label: role.name,
    };
  }

  private patchFormWithRepresentative(data: RepresentativeData): void {
    const roleIds = this.ensureRoleIds(data);
    this.form.patchValue({
      firstName: capitalizeWords(data.firstName),
      lastName: capitalizeWords(data.lastName),
      email: data.email,
      weight: data.weight,
      roleIds,
    }, { emitEvent: false });
  }

  private pendingRoleNames?: string[];

  private ensureRoleIds(data: RepresentativeData): number[] {
    if (Array.isArray(data.roleIds) && data.roleIds.length > 0) {
      return data.roleIds
        .map(Number)
        .filter((id) => Number.isFinite(id) && id > 0);
    }

    if (data.roleNames?.length) {
      const ids = this.mapRoleNamesToIds(data.roleNames);
      if (ids.length) {
        data.roleIds = ids;
        return ids;
      }

      this.pendingRoleNames = data.roleNames;
    }

    return [];
  }

  private applyPendingRoleNameConversion(): void {
    if (!this.pendingRoleNames?.length) {
      return;
    }

    const roleIds = this.mapRoleNamesToIds(this.pendingRoleNames);
    if (roleIds.length) {
      this.form.patchValue({ roleIds }, { emitEvent: false });
      if (this.representativeData) {
        this.representativeData.roleIds = roleIds;
      }
    }
    this.pendingRoleNames = undefined;
  }

  private mapRoleNamesToIds(roleNames: string[]): number[] {
    if (!roleNames?.length || this.roleOptions.length === 0) {
      return [];
    }

    const normalizedNames = roleNames.map((name) => name?.trim().toLowerCase()).filter(Boolean);
    if (!normalizedNames.length) {
      return [];
    }

    const ids: number[] = [];
    for (const option of this.roleOptions) {
      const optionLabel = option.label?.toLowerCase();
      if (normalizedNames.includes(optionLabel)) {
        ids.push(Number(option.value));
      }
    }

    return Array.from(new Set(ids));
  }

  private mapRoleIdsToNames(roleIds: number[]): string[] {
    if (!roleIds?.length || this.rolesLookup.size === 0) {
      return [];
    }

    return roleIds
      .map((id) => this.rolesLookup.get(Number(id)))
      .filter(Boolean) as string[];
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

    const formValue = this.getFormValue();
    const representativeFormData: RepresentativeData = {
      ...formValue,
      firstName: capitalizeWords(formValue.firstName),
      lastName: capitalizeWords(formValue.lastName),
      userId: this.representativeData?.userId ?? null,
      projectId: this.representativeData?.projectId ?? null,
    };

    representativeFormData.roleNames = this.mapRoleIdsToNames(representativeFormData.roleIds);

    if (this.actionType === ActionType.EDIT) {
      representativeFormData.id = this.representativeData?.id ?? representativeFormData.id ?? null;
      this.representativeUpdated.emit(representativeFormData);
    } else {
      this.representativeCreated.emit(representativeFormData);
    }

    this.modalService.close();
  }

  cancel(): void {
    this.modalService.close();
  }

  private getFormValue(): RepresentativeFormValue {
    return this.form.getRawValue() as RepresentativeFormValue;
  }
}
