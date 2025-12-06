import { Component, inject, OnInit, OnDestroy, signal, WritableSignal, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormsModule } from '@angular/forms';

import { BasePageComponent, RestoreParams } from '../../../../core/abstractions/base-page.component';
import { ModalService } from '../../../../core/services/modal.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { ActionType } from '../../../../shared/enums/action-type.enum';
import { QuestionModalComponent, QuestionData } from '../../components/question-modal/question-modal.component';
import { AccordionPanelComponent } from '../../../../shared/components/accordion-panel/accordion-panel.component';
import { InputComponent } from '../../../../shared/components/input/input.component';
import { SelectComponent, SelectOption } from '../../../../shared/components/select/select.component';
import { Subscription } from 'rxjs';
import { GenericParams, RouteParams } from '../../../../core/services/router.service';

interface CascataQuestionnaireRouteParams extends GenericParams {
  questionnaireIndex?: number;
  stageName?: string;
  sequence?: number | string;
  name?: string;
  applicationStartDate?: string;
  applicationEndDate?: string;
  questions?: QuestionData[];
}

type CascataQuestionnaireRestoreParams = RestoreParams<CascataQuestionnaireRouteParams>;

@Component({
  selector: 'app-cascata-questionnaire-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, AccordionPanelComponent, InputComponent, SelectComponent],
  templateUrl: './cascata-questionnaire-form.component.html',
  styleUrls: ['./cascata-questionnaire-form.component.scss']
})
export class CascataQuestionnaireFormComponent extends BasePageComponent<CascataQuestionnaireRouteParams> implements OnInit, OnDestroy {
  private modalService = inject(ModalService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);
  private notificationService = inject(NotificationService);
  private questionnaireIndex: number | null = null;
  private questionnaireMetadata: CascataQuestionnaireRouteParams | null = null;

  form!: FormGroup;
  questions: WritableSignal<QuestionData[]> = signal([]);

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

  protected override onInit(): void {
    this.initializeForm();
  }

  private initializeForm(): void {
    this.form = this.fb.group({
      sequence: [{ value: '', disabled: true }],
      name: ['', Validators.required],
      applicationStartDate: [{ value: '', disabled: true }],
      applicationEndDate: [{ value: '', disabled: true }],
    });
  }

  protected override loadParams(params: RouteParams<CascataQuestionnaireRouteParams>): void {
    if (!params) return;

    const data = (params.p ?? params) as CascataQuestionnaireRouteParams;
    this.questionnaireIndex = typeof data.questionnaireIndex === 'number' ? data.questionnaireIndex : null;
    this.questionnaireMetadata = data;

    console.log('Dados do Questionário Recebidos:', data);

    if (data.questions && Array.isArray(data.questions)) {
      this.questions.set(data.questions);
    }

    this.form.patchValue({
      sequence: data.sequence,
      name: data.name,
      applicationStartDate: data.applicationStartDate,
      applicationEndDate: data.applicationEndDate
    });
    this.cdr.detectChanges();
  }

  getControl(controlName: string) {
    return this.form.get(controlName);
  }

  get filteredQuestions(): QuestionData[] {
    let filtered = this.questions();
    const term = this.searchTerm.toLowerCase().trim();
    const role = this.selectedRole;

    if (term) {
      filtered = filtered.filter(q => q.value.toLowerCase().includes(term));
    }

    if (role) {
      filtered = filtered.filter(q => q.roleNames.includes(role));
    }

    return filtered;
  }

  toggleQuestionnaireDataAccordion(): void {
    this.questionnaireDataAccordionOpen = !this.questionnaireDataAccordionOpen;
  }

  toggleQuestionsAccordion(): void {
    this.questionsAccordionOpen = !this.questionsAccordionOpen;
  }

  openAddQuestionModal(): void {
    this.modalService.open(QuestionModalComponent, 'medium-card', {
      mode: ActionType.CREATE
    });

    setTimeout(() => {
      const modalInstance = this.modalService.getActiveInstance<QuestionModalComponent>();
      if (modalInstance) {
        this.questionModalSubscription = modalInstance.questionCreated.subscribe((newQuestion: QuestionData) => {
          if (!newQuestion.id) newQuestion.id = Date.now().toString();

          this.questions.update(qs => [...qs, newQuestion]);
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
      const modalInstance = this.modalService.getActiveInstance<QuestionModalComponent>();
      if (modalInstance) {
        this.questionModalSubscription = modalInstance.questionUpdated.subscribe((updatedQuestion: QuestionData) => {
          this.replaceQuestion(updatedQuestion);
          this.cdr.detectChanges();
        });
      }
    });
  }

  deleteQuestion(question: QuestionData): void {
    this.questions.update(qs => qs.filter(q => q.id !== question.id));
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

    const formData = this.form.getRawValue();

    const finalData = {
      ...formData,
      stageName: this.questionnaireMetadata?.stageName || formData.name,
      questionnaireIndex: this.questionnaireIndex,
      questions: this.questions()
    };

    console.log('Salvando dados do Questionário:', finalData);
    this.routerService.backToPrevious(0, true, { questionnaireUpdate: finalData });
  }

  onCancel(): void {
    this.routerService.backToPrevious(0, true);
  }

  protected override save(): RouteParams<CascataQuestionnaireRouteParams> {
    return {
      formValue: this.form.getRawValue(),
      questions: this.questions()
    } satisfies CascataQuestionnaireRouteParams;
  }

  protected override restore(restoreParameter: CascataQuestionnaireRestoreParams): void {
    if (!restoreParameter?.hasParams) {
      return;
    }

    const formValue = restoreParameter['formValue'];
    if (formValue) {
      this.form.patchValue(formValue);
    }

    const savedQuestions = (restoreParameter['questions'] ?? restoreParameter.p?.['questions']) as QuestionData[] | undefined;
    if (savedQuestions) {
      this.questions.set(savedQuestions);
    }
  }

  private replaceQuestion(updatedQuestion: QuestionData): void {
    this.questions.update((qs) => qs.map((q) => (q.id === updatedQuestion.id ? updatedQuestion : q)));
  }

  override ngOnDestroy(): void {
    super.ngOnDestroy();
    this.questionModalSubscription?.unsubscribe();
  }
}
