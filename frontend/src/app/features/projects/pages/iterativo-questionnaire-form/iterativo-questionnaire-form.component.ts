import { Component, inject, OnInit, OnDestroy, signal, WritableSignal, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, FormsModule, Validators } from '@angular/forms';

import { BasePageComponent } from '../../../../core/abstractions/base-page.component';
import { ModalService } from '../../../../core/services/modal.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { ActionType } from '../../../../shared/enums/action-type.enum';
import { QuestionModalComponent, QuestionData } from '../../components/question-modal/question-modal.component';
import { AccordionPanelComponent } from '../../../../shared/components/accordion-panel/accordion-panel.component';
import { InputComponent } from '../../../../shared/components/input/input.component';
import { SelectComponent, SelectOption } from '../../../../shared/components/select/select.component';
import { Subscription } from 'rxjs';

interface QuestionnaireFormData {
  projectId?: string;
  projectName?: string;
  name?: string;
  weight?: number;
  questions: QuestionData[];
}

@Component({
  selector: 'app-iterativo-questionnaire-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, AccordionPanelComponent, InputComponent, SelectComponent],
  templateUrl: './iterativo-questionnaire-form.component.html',
  styleUrls: ['./iterativo-questionnaire-form.component.scss']
})
export class IterativoQuestionnaireFormComponent extends BasePageComponent implements OnInit, OnDestroy {
  private modalService = inject(ModalService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);
  private notificationService = inject(NotificationService);
  private questionnaireIndex: number | null = null;
  private questionnaireMetadata: any = null;

  form!: FormGroup;
  questions: WritableSignal<QuestionData[]> = signal([]);
  projectName: WritableSignal<string> = signal('');

  questionnaireDataAccordionOpen = true;
  questionsAccordionOpen = true;

  searchTerm = '';
  selectedRole = '';

  roleFilterOptions: SelectOption[] = [
    { value: 'Cliente', label: 'Cliente' },
    { value: 'Desenvolvedor(a)', label: 'Desenvolvedor(a)' },
    { value: 'Líder de Equipe', label: 'Líder de Equipe' },
    { value: 'Analista de Qualidade', label: 'Analista de Qualidade' },
    { value: 'Gerente de Projeto', label: 'Gerente de Projeto' },
    { value: 'Arquiteto de Software', label: 'Arquiteto de Software' }
  ];

  private questionModalSubscription?: Subscription;

  constructor() {
    super();
  }

  override ngOnInit(): void {
    super.ngOnInit();
  }

  protected override onInit(): void {
    this.initializeForm();
  }

  protected override loadParams(params: any): void {
    if (!params) return;

    const data = params.p || params;
    console.log('Params recebidos:', data);

    this.questionnaireIndex = typeof data.questionnaireIndex === 'number' ? data.questionnaireIndex : null;
    this.questionnaireMetadata = data;

    if (data.projectName) {
      this.projectName.set(data.projectName);
    }
    if (data.questions && Array.isArray(data.questions)) {
      this.questions.set(data.questions);
      console.log('Perguntas carregadas:', data.questions);
    }

    this.form.patchValue({
      sequence: data.questionnaireIndex,
      projectName: data.projectName,
      name: data.name,
      weight: data.weight
    }, { emitEvent: false });

    this.cdr.detectChanges();
  }

  private initializeForm(): void {
    this.form = this.fb.group({
      sequence: [{ value: '', disabled: true }],
      projectName: [{ value: '', disabled: true }],
      name: ['', Validators.required],
      weight: ['', [Validators.required, Validators.min(0), Validators.max(100)]],
      questions: this.fb.array([])
    });
  }

  toggleQuestionnaireDataAccordion(): void {
    this.questionnaireDataAccordionOpen = !this.questionnaireDataAccordionOpen;
  }

  toggleQuestionsAccordion(): void {
    this.questionsAccordionOpen = !this.questionsAccordionOpen;
  }

  get filteredQuestions(): QuestionData[] {
    let filtered = this.questions();

    if (this.searchTerm) {
      filtered = filtered.filter(q =>
        q.value.toLowerCase().includes(this.searchTerm.toLowerCase())
      );
    }

    if (this.selectedRole) {
      filtered = filtered.filter(q =>
        q.roleNames.includes(this.selectedRole)
      );
    }

    return filtered;
  }

  openAddQuestionModal(): void {
    this.modalService.open(QuestionModalComponent, 'medium-card', {
      mode: ActionType.CREATE
    });

    setTimeout(() => {
      const modalInstance = (this.modalService as any).modalRef?.instance as QuestionModalComponent;
      if (modalInstance) {
        this.questionModalSubscription = modalInstance.questionCreated.subscribe((questionData: QuestionData) => {
          const currentQuestions = this.questions();
          questionData.id = Date.now().toString();
          this.questions.set([...currentQuestions, questionData]);
          this.cdr.detectChanges();
        });
      }
    });
  }

  openEditQuestionModal(question: QuestionData): void {
    this.modalService.open(QuestionModalComponent, 'medium-card', {
      mode: ActionType.EDIT,
      editData: question
    });

    setTimeout(() => {
      const modalInstance = (this.modalService as any).modalRef?.instance as QuestionModalComponent;
      if (modalInstance) {
        this.questionModalSubscription = modalInstance.questionUpdated.subscribe((updatedQuestion: QuestionData) => {
          const currentQuestions = this.questions();
          const index = currentQuestions.findIndex(q => q.id === updatedQuestion.id);
          if (index !== -1) {
            currentQuestions[index] = updatedQuestion;
            this.questions.set([...currentQuestions]);
            this.cdr.detectChanges();
          }
        });
      }
    });
  }

  deleteQuestion(question: QuestionData): void {
    if (confirm(`Tem certeza que deseja excluir a pergunta: "${question.value}"?`)) {
      const currentQuestions = this.questions();
      const filtered = currentQuestions.filter(q => q.id !== question.id);
      this.questions.set(filtered);
      this.cdr.detectChanges();
    }
  }

  protected override save(): any {
    return {
      name: this.form.get('name')?.value,
      weight: this.form.get('weight')?.value,
      questions: this.questions()
    };
  }

  protected override restore(restoreParameter: any): void {
    if (restoreParameter) {
      if (restoreParameter.questions) {
        this.questions.set(restoreParameter.questions);
      }
      if (restoreParameter.name) {
        this.form.patchValue({ name: restoreParameter.name });
      }
      if (restoreParameter.weight !== undefined) {
        this.form.patchValue({ weight: restoreParameter.weight });
      }
    }
  }

  getControl(controlName: string) {
    return this.form.get(controlName);
  }

  onSave(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (!this.questions().length) {
      this.notificationService.showWarning('Adicione ao menos uma pergunta antes de salvar o questionário.');
      return;
    }

    const formValue = this.form.getRawValue();
    const finalData = {
      ...formValue,
      iteration: this.questionnaireMetadata?.iteration,
      questionnaireIndex: this.questionnaireIndex,
      questions: this.questions()
    };

    console.log('Salvando questionário iterativo:', finalData);
    this.routerService.backToPrevious(0, true, { questionnaireUpdate: finalData });
  }

  onCancel(): void {
    this.routerService.backToPrevious(0, true);
  }

  override ngOnDestroy(): void {
    this.questionModalSubscription?.unsubscribe();
  }
}
