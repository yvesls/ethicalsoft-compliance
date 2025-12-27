import { ChangeDetectionStrategy, Component, Input, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputComponent } from '../../../../shared/components/input/input.component';
import { ModalService } from '../../../../core/services/modal.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { QuestionnaireAttachmentLink } from '../../../../shared/interfaces/questionnaire/questionnaire-response.interface';

export type AttachmentModalMode = 'positive' | 'negative';

export interface AttachmentModalValue {
  note: string;
  attachments: QuestionnaireAttachmentLink[];
}

@Component({
  selector: 'app-questionnaire-attachment-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, InputComponent],
  templateUrl: './questionnaire-attachment-modal.component.html',
  styleUrls: ['./questionnaire-attachment-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionnaireAttachmentModalComponent implements OnInit {
  @Input() mode: AttachmentModalMode = 'positive';
  @Input() title = 'Adicionar evidências';
  @Input() descriptionLabel = 'Observações / evidências';
  @Input() attachmentsLabel = 'Links de evidências';
  @Input() initialValue?: AttachmentModalValue;
  @Input() onSave?: (value: AttachmentModalValue) => void;

  private readonly fb = inject(FormBuilder);
  private readonly modalService = inject(ModalService);
  private readonly notification = inject(NotificationService);

  readonly descriptionMaxLength = 500;
  private readonly urlPattern = /^https?:\/\/[^\s]+$/i;
  readonly linkValidationMessages: Record<string, string> = {
    required: 'Informe um link ou remova o campo.',
    pattern: 'Digite um endereço iniciando com http:// ou https://.',
  };
  readonly descriptionValidationMessages: Record<string, string> = {
    required: 'Descreva o que será acessado por este link.',
    minlength: 'Use pelo menos 3 caracteres.',
  };

  readonly form: FormGroup = this.fb.group({
    note: this.fb.control('', [
      Validators.required,
      Validators.minLength(5),
      Validators.maxLength(this.descriptionMaxLength),
    ]),
    attachments: this.fb.array<FormGroup>([], [Validators.required, Validators.minLength(1)]),
  });

  get attachments(): FormArray<FormGroup> {
    return this.form.get('attachments') as FormArray<FormGroup>;
  }

  ngOnInit(): void {
    this.applyModeLabels();
    this.initializeFormValues();
  }

  addAttachment(link?: QuestionnaireAttachmentLink | null): void {
    this.attachments.push(this.createAttachmentGroup(link));
  }

  removeAttachment(index: number): void {
    if (this.attachments.length === 1) {
      this.attachments
        .at(0)
        .reset({ descricao: '', url: '' });
      return;
    }
    this.attachments.removeAt(index);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.attachments.markAllAsTouched();
      this.notification.showWarning('Preencha as evidências/justificativas antes de salvar.');
      return;
    }

    const value: AttachmentModalValue = {
      note: this.form.get('note')?.value ?? '',
      attachments: this.attachments.controls.map((control) => {
        const descricao = control.get('descricao')?.value ?? '';
        const url = control.get('url')?.value ?? '';
        return {
          descricao: descricao?.trim() || url?.trim(),
          url: url?.trim(),
        };
      }),
    };

    this.onSave?.(value);
    this.modalService.close();
  }

  cancel(): void {
    this.modalService.close();
  }

  private applyModeLabels(): void {
    if (this.mode === 'positive') {
      this.title = 'Adicionar evidências';
      this.descriptionLabel = 'Observações ou contexto adicional';
      this.attachmentsLabel = 'Links de evidências';
      return;
    }

    this.title = 'Adicionar justificativa';
    this.descriptionLabel = 'Justificativa ou observações';
    this.attachmentsLabel = 'Links para documentos de suporte';
  }

  private initializeFormValues(): void {
    const value = this.initialValue ?? {
      note: '',
      attachments: [{ descricao: '', url: '' }],
    };
    this.form.patchValue({ note: value.note ?? '' });

    if (!value.attachments?.length) {
      this.addAttachment();
      return;
    }

    value.attachments.forEach((attachment) => this.addAttachment(attachment));
  }

  private createAttachmentGroup(
    attachment?: QuestionnaireAttachmentLink | null
  ): FormGroup {
    return this.fb.group({
      descricao: this.fb.control(attachment?.descricao ?? '', [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(120),
      ]),
      url: this.fb.control(attachment?.url ?? '', [
        Validators.required,
        Validators.pattern(this.urlPattern),
      ]),
    });
  }
}
