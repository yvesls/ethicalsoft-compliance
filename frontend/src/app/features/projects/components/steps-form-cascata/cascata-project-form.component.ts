import {
  Component,
  ChangeDetectionStrategy,
  inject,
  DestroyRef,
  ChangeDetectorRef,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
  AbstractControl,
  FormArray,
} from '@angular/forms';
import { ProjectType } from '../../../../shared/enums/project-type.enum';
import { SelectOption } from '../../../../shared/components/select/select.component';
import { AccordionPanelComponent } from '../../../../shared/components/accordion-panel/accordion-panel.component';
import { InputComponent } from '../../../../shared/components/input/input.component';
import { SelectComponent } from '../../../../shared/components/select/select.component';

import { CustomValidators } from '../../../../shared/validators/custom.validator';
import { ProjectDatesValidators } from '../../../../shared/validators/project-dates.validator';
import { StagesDeadlineValidator } from '../../../../shared/validators/stages-deadline.validator';
import { FormUtils } from '../../../../shared/utils/form-utils';
import {
  BasePageComponent,
  RestoreParams,
} from '../../../../core/abstractions/base-page.component';
import { TemplateStore } from '../../../../shared/stores/template.store';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ModalService } from '../../../../core/services/modal.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { TemplateActionModalComponent } from '../template-action-modal/template-action-modal.component';
import { StageCascataModalComponent, StageCascataData } from '../stage-cascata-modal/stage-cascata-modal.component';
import { ActionType } from '../../../../shared/enums/action-type.enum';

export interface Representative {
  firstName: string;
  lastName: string;
  email: string;
  inclusionDate: string;
  weight: number;
  roles: string;
}

export interface Questionnaire {
  name: string;
  sequence: number;
  applicationStartDate: string;
  applicationEndDate: string;
}

@Component({
  selector: 'app-cascata-project-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    AccordionPanelComponent,
    InputComponent,
    SelectComponent,
  ],
  templateUrl: './cascata-project-form.component.html',
  styleUrls: ['./cascata-project-form.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CascataProjectFormComponent extends BasePageComponent implements OnInit {
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);
  private destroyRef = inject(DestroyRef);
  private templateStore = inject(TemplateStore);
  private modalService = inject(ModalService);
  private notificationService = inject(NotificationService);

  public ProjectType = ProjectType;
  public projectForm!: FormGroup;
  public panelStates = {
    project: true,
    steps: false,
    representatives: false,
    questionnaires: false,
  };

  public templateOptions: SelectOption[] = [];
  public projectTypeOptions: SelectOption[] = [
    { value: ProjectType.Cascata, label: 'Cascata' },
  ];

  private hasRestoredSteps = false;
  private lastValidPanelIndex = 0; // Controla qual painel pode ser aberto

  // Mapeamento dos painéis para controle sequencial
  private readonly PANEL_ORDER = ['project', 'steps', 'representatives', 'questionnaires'] as const;

  constructor() {
    super();
  }

  protected override onInit(): void {
    this.projectForm = this.fb.group(
      {
        template: [null, [Validators.required]],
        name: ['', [Validators.required]],
        type: [
          { value: ProjectType.Cascata, disabled: true },
          [Validators.required],
        ],
        startDate: [
          null,
          [
            Validators.required,
            ProjectDatesValidators.startDateAllowsExistingStages()
          ]
        ],
        deadline: [
          null,
          [
            Validators.required,
            CustomValidators.minDateToday(),
            ProjectDatesValidators.deadlineAllowsExistingStages()
          ]
        ],
        steps: this.buildCascataStepsForm(),
        representatives: this.buildRepresentativesForm(),
        questionnaires: this.fb.array([]), // Inicializa vazio
      },
      {
        validators: [
          CustomValidators.dateRange(
            'startDate',
            'deadline',
            'A data de início não pode ser maior que o prazo limite.'
          ),
          ProjectDatesValidators.stageApplicationRangeWithinDeadline(),
          StagesDeadlineValidator.stagesFitWithinDeadline()
        ],
      }
    );

    this.syncQuestionnairesWithSteps();
    this.loadTemplates();
    this.setupTemplateListener();
    this.setupDateListeners();
  }

  protected override loadParams(params: any, queryParams?: any): void {
  }

  private loadTemplates(): void {
    this.templateStore.getAllTemplates()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (templates) => {
          const cascataTemplates = templates.filter(t => t.type === ProjectType.Cascata);
          this.templateOptions = cascataTemplates.map(t => ({
            value: t.id,
            label: t.name
          }));
          this.cdr.markForCheck();
        },
        error: (error) => {
          console.error('Erro ao carregar templates:', error);
        }
      });
  }

  private setupTemplateListener(): void {
    this.projectForm.get('template')?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((templateId) => {
        if (templateId) {
          this.loadTemplateData(templateId);
        }
      });
  }

  private hasUserModifiedSteps(): boolean {
    // Similar ao iterativo: verifica se já foi restaurado OU se tem itens no array
    const stepsArray = this.projectForm.get('steps') as FormArray;
    return this.hasRestoredSteps || (stepsArray && stepsArray.length > 0);
  }

  private setupDateListeners(): void {
    this.projectForm.get('startDate')?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((startDate) => {
        if (startDate) {
          this.recalculateAllStageRanges();
        }
      });

    this.projectForm.get('deadline')?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.projectForm.get('startDate')?.updateValueAndValidity({ emitEvent: false });
        this.stepsFormArray.controls.forEach(stepControl => {
          stepControl.updateValueAndValidity({ emitEvent: false });
        });
      });
  }

  /**
   * Recalcula as faixas de aplicação de todas as etapas baseado na sequence.
   * REGRA CENTRAL (BR08):
   * 1. Ordena as etapas por sequence (1, 2, 3...)
   * 2. Calcula as datas de forma sequencial:
   *    - Sequência 1: inicia na startDate do projeto
   *    - Sequência N: inicia no fim da Sequência N-1
   * 3. Aplica regra de 10% (abertura) e 90% (limite) sobre a durationDays
   */
  private recalculateAllStageRanges(): void {
    const startDate = this.projectForm.get('startDate')?.value;
    if (!startDate) return;

    // 1. Ordena as etapas por sequence
    const sortedSteps = this.getSortedStepsBySequence();

    let previousStageEndDate: Date = new Date(startDate);

    // 2. Itera sobre as etapas ordenadas e calcula as datas
    sortedSteps.forEach((stepInfo) => {
      const stepControl = this.stepsFormArray.at(stepInfo.index);
      const durationDays = stepControl.get('durationDays')?.value || 0;

      if (durationDays > 0) {
        // A etapa inicia onde a anterior terminou
        const stageStartDate = new Date(previousStageEndDate);

        // Calcula a faixa de aplicação (BR08: 10% a 90%)
        const openingOffset = Math.round(durationDays * 0.1);
        const closingOffset = Math.round(durationDays * 0.9);

        const applicationStartDate = FormUtils.addBusinessDays(stageStartDate, openingOffset);
        const applicationEndDate = FormUtils.addBusinessDays(stageStartDate, closingOffset);

        // Atualiza o FormGroup da etapa
        stepControl.patchValue({
          applicationStartDate: FormUtils.formatDateISO(applicationStartDate),
          applicationEndDate: FormUtils.formatDateISO(applicationEndDate),
          dateRange: `${FormUtils.formatDateBR(FormUtils.formatDateISO(applicationStartDate))} - ${FormUtils.formatDateBR(FormUtils.formatDateISO(applicationEndDate))}`
        }, { emitEvent: false });

        // Atualiza a data de término para a próxima etapa
        previousStageEndDate = FormUtils.addBusinessDays(stageStartDate, durationDays);
      }
    });

    this.syncQuestionnairesWithSteps();
    this.cdr.detectChanges();
  }

  /**
   * Retorna as etapas ordenadas por sequence (crescente).
   * Útil para processamento sequencial.
   */
  private getSortedStepsBySequence(): { index: number; sequence: number }[] {
    return this.stepsFormArray.controls
      .map((control, index) => ({
        index,
        sequence: control.get('sequence')?.value || 999
      }))
      .sort((a, b) => a.sequence - b.sequence);
  }

  private loadTemplateData(templateId: string): void {
    this.templateStore.getFullTemplate(templateId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (template) => {
          if (template.stages && template.stages.length > 0 && !this.hasUserModifiedSteps()) {
            const stepsData = template.stages.map(stage => ({
              name: stage.name,
              weight: stage.weight,
              sequence: stage.sequence || 1, // Lê sequence do template
              dateRange: '',
              durationDays: stage.durationDays || 0, // Se disponível no template
              applicationStartDate: '',
              applicationEndDate: ''
            }));
            this.projectForm.setControl('steps', this.buildCascataStepsForm(stepsData));

            // Após carregar do template, recalcula as datas se startDate já estiver definido
            const startDate = this.projectForm.get('startDate')?.value;
            if (startDate) {
              this.recalculateAllStageRanges();
            } else {
              // Apenas sincroniza questionários sem datas calculadas
              this.syncQuestionnairesWithSteps();
            }
          }

          if (template.representatives && template.representatives.length > 0) {
            const repsData = template.representatives.map(rep => ({
              firstName: rep.firstName,
              lastName: rep.lastName,
              email: rep.email,
              inclusionDate: new Date().toLocaleDateString('pt-BR'),
              weight: rep.weight,
              roles: rep.roleNames.join(', ')
            }));
            this.projectForm.setControl('representatives', this.buildRepresentativesForm(repsData));
          }

          this.cdr.detectChanges();
        },
        error: (error) => {
          console.error('Erro ao carregar template completo:', error);
        }
      });
  }

  private buildCascataStepsForm(data?: any[]): FormArray {
    const stepsData = data || [];
    const stepGroups = stepsData.map((step: any) => {
      return this.fb.group({
        name: [step.name, Validators.required],
        weight: [step.weight, [Validators.required, Validators.min(0)]],
        sequence: [step.sequence || 1, [Validators.required, Validators.min(1)]],
        dateRange: [step.dateRange || ''],
        durationDays: [step.durationDays || 0],
        applicationStartDate: [step.applicationStartDate || ''],
        applicationEndDate: [step.applicationEndDate || ''],
      });
    });

    const stepsArray = this.fb.array(stepGroups, Validators.required);

    return stepsArray;
  }

  private buildRepresentativesForm(data?: Representative[]): FormArray {
    const representativesData = data || [];
    const repGroups = representativesData.map((rep: any) => {
      return this.fb.group({
        firstName: [rep.firstName, Validators.required],
        lastName: [rep.lastName, Validators.required],
        email: [rep.email, [Validators.required, Validators.email]],
        inclusionDate: [rep.inclusionDate, Validators.required],
        weight: [rep.weight, [Validators.required, Validators.min(0)]],
        roles: [rep.roles, Validators.required],
      });
    });
    return this.fb.array(repGroups);
  }

  addStep(): void {
    this.modalService.open(TemplateActionModalComponent, 'small-card');
    const modalRef = (this.modalService as any).modalRef?.instance as TemplateActionModalComponent;

    const actionSubscription = modalRef.actionSelected.subscribe((action: 'create' | 'import') => {
      if (action === 'create') {
        const closeSubscription = this.modalService.modalClosed$
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe(() => {
            this.openCreateStageModal();
            closeSubscription.unsubscribe();
          });
      } else if (action === 'import') {
        this.notificationService.showWarning('Funcionalidade em desenvolvimento');
      }
      actionSubscription.unsubscribe();
    });
  }

  private openCreateStageModal(): void {
    const projectStart = this.projectForm.get('startDate')?.value;
    const projectDeadline = this.projectForm.get('deadline')?.value;
    const existingStages = this.getExistingStagesData();

    this.modalService.open(StageCascataModalComponent, 'small-card', {
      projectStartDate: projectStart,
      projectDeadline: projectDeadline,
      projectDurationDays: this.calculateProjectDuration(),
      existingStages: existingStages
    });

    const modalRef = (this.modalService as any).modalRef?.instance as StageCascataModalComponent;

    const createdSubscription = modalRef.stageCreated.subscribe((newStage: StageCascataData) => {
      this.addNewStage(newStage);
      createdSubscription.unsubscribe();
      this.cdr.detectChanges();
    });
  }

  editStep(index: number): void {
    const stepFormGroup = this.stepsFormArray.at(index) as FormGroup;
    const projectStart = this.projectForm.get('startDate')?.value;
    const projectDeadline = this.projectForm.get('deadline')?.value;
    const existingStages = this.getExistingStagesDataExcluding(index);

    this.modalService.open(StageCascataModalComponent, 'small-card', {
      mode: ActionType.EDIT,
      editData: {
        id: String(index),
        name: stepFormGroup.get('name')?.value,
        weight: stepFormGroup.get('weight')?.value,
        sequence: stepFormGroup.get('sequence')?.value,
        durationDays: stepFormGroup.get('durationDays')?.value,
        applicationStartDate: stepFormGroup.get('applicationStartDate')?.value,
        applicationEndDate: stepFormGroup.get('applicationEndDate')?.value
      },
      projectStartDate: projectStart,
      projectDeadline: projectDeadline,
      projectDurationDays: this.calculateProjectDuration(),
      existingStages: existingStages
    });

    const modalRef = (this.modalService as any).modalRef?.instance as StageCascataModalComponent;

    const updatedSubscription = modalRef.stageUpdated.subscribe((updatedStage: StageCascataData) => {
      this.updateStage(index, updatedStage);
      updatedSubscription.unsubscribe();
      this.cdr.detectChanges();
    });
  }

  private getExistingStagesDataExcluding(excludeIndex: number): { weight: number; durationDays: number; sequence: number }[] {
    return this.stepsFormArray.controls
      .map((control, idx) => ({
        weight: control.get('weight')?.value || 0,
        durationDays: control.get('durationDays')?.value || 0,
        sequence: control.get('sequence')?.value || 1,
        index: idx
      }))
      .filter(stage => stage.index !== excludeIndex)
      .map(stage => ({
        weight: stage.weight,
        durationDays: stage.durationDays,
        sequence: stage.sequence
      }));
  }

  /**
   * Gerencia a lógica de sequências ao criar uma nova etapa.
   * Empurra as sequências existentes que são >= à nova sequência.
   *
   * @param newSequence - A sequência desejada para a nova etapa
   */
  private handleSequenceOnCreate(newSequence: number): void {
    this.stepsFormArray.controls.forEach((control) => {
      const currentSequence = control.get('sequence')?.value;

      if (currentSequence >= newSequence) {
        control.patchValue({ sequence: currentSequence + 1 }, { emitEvent: false });
      }
    });
  }

  /**
   * Gerencia a lógica de sequências ao editar uma etapa existente.
   * Implementa a lógica de "troca": se a nova sequência já existe,
   * a etapa que a possui recebe a sequência antiga da etapa editada.
   *
   * @param editIndex - Índice da etapa sendo editada
   * @param newSequence - A nova sequência desejada
   */
  private handleSequenceOnEdit(editIndex: number, newSequence: number): void {
    const editedStepControl = this.stepsFormArray.at(editIndex);
    const oldSequence = editedStepControl.get('sequence')?.value;

    // Se a sequência não mudou, não faz nada
    if (oldSequence === newSequence) {
      return;
    }

    // Encontra a etapa que atualmente tem a nova sequência desejada
    const conflictIndex = this.stepsFormArray.controls.findIndex(
      (control, idx) => idx !== editIndex && control.get('sequence')?.value === newSequence
    );

    if (conflictIndex !== -1) {
      // Troca: a etapa em conflito recebe a sequência antiga da etapa editada
      const conflictControl = this.stepsFormArray.at(conflictIndex);
      conflictControl.patchValue({ sequence: oldSequence }, { emitEvent: false });
    }

    // Atualiza a sequência da etapa editada
    editedStepControl.patchValue({ sequence: newSequence }, { emitEvent: false });
  }

  private updateStage(index: number, updatedStage: StageCascataData): void {
    const stepFormGroup = this.stepsFormArray.at(index) as FormGroup;
    const oldStepName = stepFormGroup.get('name')?.value;

    // Gerencia troca de sequência antes de atualizar
    this.handleSequenceOnEdit(index, updatedStage.sequence);

    stepFormGroup.patchValue({
      name: updatedStage.name,
      weight: updatedStage.weight,
      sequence: updatedStage.sequence,
      durationDays: updatedStage.durationDays,
      dateRange: `${FormUtils.formatDateBR(updatedStage.applicationStartDate)} - ${FormUtils.formatDateBR(updatedStage.applicationEndDate)}`,
      applicationStartDate: updatedStage.applicationStartDate,
      applicationEndDate: updatedStage.applicationEndDate
    });

    // Recalcula todas as etapas e sincroniza questionários automaticamente
    this.recalculateAllStageRanges();
    this.cdr.detectChanges();
  }

  private addNewStage(newStage: StageCascataData): void {
    // Empurra sequências existentes para frente antes de adicionar
    this.handleSequenceOnCreate(newStage.sequence);

    this.stepsFormArray.push(
      this.fb.group({
        name: [newStage.name, Validators.required],
        weight: [newStage.weight, [Validators.required, Validators.min(1)]],
        sequence: [newStage.sequence, [Validators.required, Validators.min(1)]],
        dateRange: [`${FormUtils.formatDateBR(newStage.applicationStartDate)} - ${FormUtils.formatDateBR(newStage.applicationEndDate)}`],
        durationDays: [newStage.durationDays],
        applicationStartDate: [newStage.applicationStartDate],
        applicationEndDate: [newStage.applicationEndDate],
      })
    );

    // Recalcula todas as etapas e sincroniza questionários
    this.recalculateAllStageRanges();
    this.cdr.detectChanges();
  }

  removeStep(index: number): void {
    if (this.stepsFormArray.length > 1) {
      const stepName = this.stepsFormArray.at(index).get('name')?.value;

      this.stepsFormArray.removeAt(index);
      this.recalculateAllStageRanges(); // Recalcula para manter sincronização
      this.cdr.detectChanges();
    }
  }

  /**
   * Sincroniza o array de questionários com o array de etapas.
   * REGRA DE NEGÓCIO: Cada etapa representa um questionário.
   * Os questionários são ordenados por sequence e refletem as datas das etapas.
   */
  private syncQuestionnairesWithSteps(): void {
    // Limpa o array de questionários
    while (this.questionnairesFormArray.length > 0) {
      this.questionnairesFormArray.removeAt(0);
    }

    // Obtém as etapas ordenadas por sequence
    const sortedSteps = this.getSortedStepsBySequence();

    // Cria um questionário para cada etapa, na ordem correta
    sortedSteps.forEach((stepInfo) => {
      const stepControl = this.stepsFormArray.at(stepInfo.index);
      const stepName = stepControl.get('name')?.value;
      const startDate = stepControl.get('applicationStartDate')?.value || '';
      const endDate = stepControl.get('applicationEndDate')?.value || '';

      // Adiciona o questionário (sem passar weight, conforme solicitado)
      this.questionnairesFormArray.push(
        this.fb.group({
          name: [stepName, [Validators.required]],
          stageName: [stepName],
          sequence: [stepInfo.sequence],
          applicationStartDate: [startDate],
          applicationEndDate: [endDate],
        })
      );
    });

    this.cdr.detectChanges();
  }

  addRepresentative(): void {
    const newRep = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      inclusionDate: [new Date().toLocaleDateString('pt-BR'), Validators.required],
      weight: [1, [Validators.required, Validators.min(0)]],
      roles: ['', Validators.required],
    });
    this.representativesFormArray.push(newRep);
    this.cdr.detectChanges();
  }

  removeRepresentative(index: number): void {
    this.representativesFormArray.removeAt(index);
    this.cdr.detectChanges();
  }

  private buildQuestionnairesForm(data?: Questionnaire[]): FormArray {
    if (!data || data.length === 0) {
      return this.fb.array([]);
    }

    return this.fb.array(
      data.map((q) =>
        this.fb.group({
          name: [q.name, [Validators.required]],
          sequence: [q.sequence, [Validators.required, Validators.min(1)]],
          applicationStartDate: [q.applicationStartDate || ''],
          applicationEndDate: [q.applicationEndDate || ''],
        })
      )
    );
  }

  protected override save(): any {
    return {
      formValue: this.projectForm.getRawValue(),
      panelStates: this.panelStates,
    };
  }

  protected override restore(restoreParameter: RestoreParams<any>): void {
    if (restoreParameter.hasParams) {
      if (restoreParameter['formValue']) {
        const formValue = restoreParameter['formValue'];

        if (formValue.steps && formValue.steps.length > 0) {
          this.hasRestoredSteps = true;
        }

        this.projectForm.setControl(
          'steps',
          this.buildCascataStepsForm(formValue.steps || [])
        );

        this.projectForm.setControl(
          'representatives',
          this.buildRepresentativesForm(formValue.representatives || [])
        );

        if (formValue.questionnaires && formValue.questionnaires.length > 0) {
          this.projectForm.setControl('questionnaires', this.buildQuestionnairesForm(formValue.questionnaires));
        } else {
          this.syncQuestionnairesWithSteps();
        }

        this.projectForm.patchValue(formValue);
      }

      if (restoreParameter['panelStates']) {
        this.panelStates = restoreParameter['panelStates'];
      }
    }

    this.cdr.detectChanges();
  }

  getControl(name: string): AbstractControl | null {
    return this.projectForm.get(name);
  }

  get stepsFormArray(): FormArray {
    return this.getControl('steps') as FormArray;
  }

  get representativesFormArray(): FormArray {
    return this.getControl('representatives') as FormArray;
  }

  get questionnairesFormArray(): FormArray {
    return this.getControl('questionnaires') as FormArray;
  }

  trackByIndex(index: number): number {
    return index;
  }

  private calculateProjectDuration(): number {
    const startDate = this.projectForm.get('startDate')?.value;
    const deadline = this.projectForm.get('deadline')?.value;

    if (!startDate || !deadline) {
      return 0;
    }

    return FormUtils.calculateBusinessDays(new Date(startDate), new Date(deadline));
  }

  private getExistingStagesData(): { weight: number; durationDays: number; sequence: number }[] {
    return this.stepsFormArray.controls.map(control => ({
      weight: control.get('weight')?.value || 0,
      durationDays: control.get('durationDays')?.value || 0,
      sequence: control.get('sequence')?.value || 1
    }));
  }

  getDeadlineErrorMessage(): string {
    const error = this.projectForm.errors?.['deadlineTooEarly'];
    if (!error) return '';

    const { stageName, latestStageEnd, deadline } = error;

    return `O prazo limite (${deadline}) não permite acomodar a etapa "${stageName}" que termina em ${latestStageEnd}. Por favor, estenda o prazo limite ou ajuste os pesos das etapas.`;
  }

  getStartDateErrorMessage(): string {
    const error = this.projectForm.errors?.['startDateTooLate'];
    if (!error) return '';

    const { startDate, deadline, projectedEndDate, requiredDays } = error;

    return `Com a data de início atual (${startDate}), as etapas terminariam em ${projectedEndDate}, ultrapassando o prazo limite (${deadline}). As etapas requerem ${requiredDays} dias úteis. Por favor, antecipe a data de início ou ajuste os pesos das etapas.`;
  }

  getStageExceedsDeadlineMessage(): string {
    const error = this.projectForm.errors?.['stageExceedsDeadline'];
    if (!error) return '';

    const { stageName, stageEndDate, deadline } = error;

    return `A etapa "${stageName}" tem data de término (${stageEndDate}) que ultrapassa o prazo limite do projeto (${deadline}). Ajuste o prazo limite do projeto ou reduza o peso desta etapa.`;
  }

  /**
   * Retorna a mensagem de erro quando etapas ultrapassam o deadline.
   * Esta é a validação principal da tabela de etapas.
   */
  getStagesExceedDeadlineMessage(): string {
    const error = this.projectForm.errors?.['stagesExceedDeadline'];
    if (!error) return '';

    const { worstCase, count } = error;
    const plural = count > 1 ? 's' : '';

    return `${count} etapa${plural} ultrapassa${count === 1 ? '' : 'm'} o prazo limite do projeto. A etapa "${worstCase.stageName}" (Sequência ${worstCase.sequence}) excede em aproximadamente ${worstCase.daysOver} dia${worstCase.daysOver > 1 ? 's' : ''} úteis. Ajuste a data de início, estenda o prazo limite ou reduza a duração das etapas.`;
  }

  /**
   * Verifica se há erro de etapas excedendo o deadline.
   * Usado para controlar a exibição do erro na tabela.
   */
  hasStagesExceedDeadlineError(): boolean {
    return !!this.projectForm.errors?.['stagesExceedDeadline'];
  }

  /**
   * Avança para o próximo painel se o atual for válido.
   * Implementa a lógica de navegação sequencial entre accordions.
   *
   * @param currentPanelKey - A chave do painel atual ('project', 'steps', etc.)
   */
  continueToNextPanel(currentPanelKey: keyof typeof this.panelStates): void {
    const currentIndex = this.PANEL_ORDER.indexOf(currentPanelKey as any);

    if (!this.isPanelValid(currentPanelKey)) {
      this.projectForm.markAllAsTouched();
      this.notificationService.showWarning('Por favor, preencha todos os campos obrigatórios antes de continuar.');
      return;
    }

    console.log('=== CONTINUE TO NEXT PANEL ===');
    console.log('Current panel:', currentPanelKey);
    console.log('Panel states BEFORE:', { ...this.panelStates });

    this.panelStates[currentPanelKey] = false;

    this.lastValidPanelIndex = Math.max(this.lastValidPanelIndex, currentIndex + 1);

    const nextPanelKey = this.PANEL_ORDER[currentIndex + 1];
    if (nextPanelKey) {
      this.panelStates[nextPanelKey] = true;
      console.log('Next panel:', nextPanelKey);
    }

    console.log('Panel states AFTER:', { ...this.panelStates });
    console.log('==============================');

    this.cdr.detectChanges();
  }

  /**
   * Verifica se um painel específico está válido.
   *
   * @param panelKey - A chave do painel a validar
   * @returns true se o painel estiver válido
   */
  private isPanelValid(panelKey: keyof typeof this.panelStates): boolean {
    switch (panelKey) {
      case 'project':
        const requiredControls = ['template', 'name', 'startDate', 'deadline'];
        const allRequiredValid = requiredControls.every(controlName => this.projectForm.get(controlName)?.valid);
        const groupErrors = this.projectForm.errors;
        const hasRelevantGroupError = groupErrors?.['dateRange'] || groupErrors?.['deadlineTooEarly'] || groupErrors?.['startDateTooLate'];
        return allRequiredValid && !hasRelevantGroupError;

      case 'steps':
        const stepsArray = this.projectForm.get('steps') as FormArray;
        return stepsArray && stepsArray.valid && stepsArray.length > 0 && !this.hasStagesExceedDeadlineError();

      case 'representatives':
        const repsArray = this.projectForm.get('representatives') as FormArray;
        return repsArray && repsArray.valid && repsArray.length > 0;

      case 'questionnaires':
        return true;

      default:
        return false;
    }
  }

  /**
   * Verifica se um painel pode ser aberto pelo usuário.
   * Apenas painéis até o lastValidPanelIndex podem ser abertos.
   *
   * @param panelKey - A chave do painel
   * @returns true se o painel pode ser aberto
   */
  canOpenPanel(panelKey: keyof typeof this.panelStates): boolean {
    const panelIndex = this.PANEL_ORDER.indexOf(panelKey as any);
    return panelIndex <= this.lastValidPanelIndex;
  }

  onPanelToggled(panelKey: keyof typeof this.panelStates, newState: boolean): void {
    console.log(`Panel ${panelKey} toggled to:`, newState);
    this.panelStates[panelKey] = newState;
    this.cdr.detectChanges();
  }

  onAttemptedToggle(panelKey: keyof typeof this.panelStates): void {
    this.notificationService.showWarning('Complete os painéis anteriores antes de acessar este.');
  }

  onSubmit(): void {
    if (this.projectForm.invalid) {
      this.projectForm.markAllAsTouched();
      return;
    }
  }
}
