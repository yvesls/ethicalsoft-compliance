import { Component, Output, EventEmitter, inject, Input, ChangeDetectionStrategy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ModalService } from '../../../../core/services/modal.service';
import { InputComponent } from '../../../../shared/components/input/input.component';
import { MultiSelectComponent, MultiSelectOption } from '../../../../shared/components/multi-select/multi-select.component';
import { ActionType } from '../../../../shared/enums/action-type.enum';
import { RoleService } from '../../../../core/services/role.service';
import { RoleSummary } from '../../../../shared/interfaces/role/role-summary.interface';
import { take } from 'rxjs/operators';

export interface QuestionData {
  id?: string;
  value: string;
  roleIds: number[];
  roleNames: string[];
}

@Component({
  selector: 'app-question-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, InputComponent, MultiSelectComponent],
  templateUrl: './question-modal.component.html',
  styleUrls: ['./question-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuestionModalComponent implements OnInit {
  @Input() editData?: QuestionData;
  @Input() mode: ActionType = ActionType.CREATE;
  @Output() questionCreated = new EventEmitter<QuestionData>();
  @Output() questionUpdated = new EventEmitter<QuestionData>();

  private modalService = inject(ModalService);
  private fb = inject(FormBuilder);
  private roleService = inject(RoleService);

  form!: FormGroup;
  actionType: ActionType = ActionType.CREATE;
  modalTitle = 'Criar nova pergunta';
  roleOptions: MultiSelectOption[] = [];
  private rolesLookup = new Map<number, string>();
  private pendingRoleNames?: string[];

  constructor() {
    this.initializeForm();
  }

  ngOnInit(): void {
    this.loadRoleOptions();

    if (this.editData && this.mode === ActionType.EDIT) {
      this.actionType = ActionType.EDIT;
      this.modalTitle = 'Editar pergunta';
      this.populateForm(this.editData);
    } else {
      this.actionType = ActionType.CREATE;
    }
  }

  private initializeForm(): void {
    this.form = this.fb.group({
      value: ['', [Validators.required, Validators.minLength(10)]],
      roleIds: [[], [Validators.required, Validators.minLength(1)]],
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
        },
        error: (error) => {
          console.error('Falha ao carregar roles para o questionário', error);
        }
      });
  }

  private mapRoleToOption(role: RoleSummary): MultiSelectOption {
    return {
      value: role.id,
      label: role.name,
    };
  }

  private populateForm(data: QuestionData): void {
    const roleIds = this.ensureRoleIds(data);
    this.form.patchValue({
      value: data.value,
      roleIds,
    });
  }

  private ensureRoleIds(data: QuestionData): number[] {
    if (Array.isArray(data.roleIds) && data.roleIds.length) {
      return data.roleIds
        .map(Number)
        .filter((id) => Number.isFinite(id) && id > 0);
    }

    if (Array.isArray(data.roleNames) && data.roleNames.length) {
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
      if (this.editData) {
        this.editData.roleIds = roleIds;
      }
    }

    this.pendingRoleNames = undefined;
  }

  private mapRoleNamesToIds(roleNames: string[]): number[] {
    if (!roleNames?.length || this.rolesLookup.size === 0) {
      return [];
    }

    const normalized = roleNames
      .map((name) => name?.trim().toLowerCase())
      .filter(Boolean);

    if (!normalized.length) {
      return [];
    }

    const ids: number[] = [];
    for (const [id, name] of this.rolesLookup.entries()) {
      if (normalized.includes(name.trim().toLowerCase())) {
        ids.push(Number(id));
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

  onSubmit(): void {
    if (this.form.valid) {
      const formValue = this.form.value;
      const roleIds = formValue.roleIds ?? [];
      const questionData: QuestionData = {
        id: this.editData?.id,
        value: formValue.value,
        roleIds,
        roleNames: this.mapRoleIdsToNames(roleIds)
      };

      if (this.actionType === ActionType.EDIT) {
        this.questionUpdated.emit(questionData);
      } else {
        this.questionCreated.emit(questionData);
      }

      this.close();
    } else {
      this.form.markAllAsTouched();
    }
  }

  close(): void {
    this.modalService.close();
  }
}
