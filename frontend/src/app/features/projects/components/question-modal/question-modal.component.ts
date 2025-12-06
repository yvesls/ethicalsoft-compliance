import { Component, Output, EventEmitter, inject, Input, ChangeDetectorRef, ChangeDetectionStrategy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ModalService } from '../../../../core/services/modal.service';
import { InputComponent } from '../../../../shared/components/input/input.component';
import { MultiSelectComponent } from '../../../../shared/components/multi-select/multi-select.component';
import { ActionType } from '../../../../shared/enums/action-type.enum';
import { REPRESENTATIVE_ROLE_OPTIONS } from '../../../../shared/enums/representative-role.enum';

export interface QuestionData {
  id?: string;
  value: string;
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

  form!: FormGroup;
  actionType: ActionType = ActionType.CREATE;
  modalTitle: string = 'Criar nova pergunta';
  roleOptions = REPRESENTATIVE_ROLE_OPTIONS;

  constructor() {
    this.initializeForm();
  }

  ngOnInit(): void {
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
      roleNames: [[], [Validators.required]],
    });
  }

  private populateForm(data: QuestionData): void {
    this.form.patchValue({
      value: data.value,
      roleNames: data.roleNames,
    });
  }

  onSubmit(): void {
    if (this.form.valid) {
      const formValue = this.form.value;
      const questionData: QuestionData = {
        id: this.editData?.id,
        value: formValue.value,
        roleNames: formValue.roleNames
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
