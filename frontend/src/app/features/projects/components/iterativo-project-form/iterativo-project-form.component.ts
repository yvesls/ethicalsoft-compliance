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
import {
  BasePageComponent,
  RestoreParams,
} from '../../../../core/abstractions/base-page.component';

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
  iteration: string;
  weight: number;
  applicationStartDate: string;
  applicationEndDate: string;
}

@Component({
  selector: 'app-iterativo-project-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    AccordionPanelComponent,
    InputComponent,
    SelectComponent,
  ],
  templateUrl: './iterativo-project-form.component.html',
  styleUrls: ['./iterativo-project-form.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IterativoProjectFormComponent extends BasePageComponent implements OnInit {
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);

  public ProjectType = ProjectType;
  public projectForm!: FormGroup;
  public panelStates = {
    project: true,
    steps: false,
    representatives: false,
    questionnaires: false,
  };

  public iterativeSteps = [
    'Iniciação',
    'Desenvolvimento',
    'Requisitos',
    'Testes',
  ];

  public templateOptions: SelectOption[] = [
    { value: 'template2', label: 'Padrão - Iterativo' },
  ];
  public projectTypeOptions: SelectOption[] = [
    { value: ProjectType.Iterativo, label: 'Iterativo Incremental' },
  ];

  constructor() {
    super();
  }

  protected override onInit(): void {
    this.projectForm = this.fb.group(
      {
        template: [null, [Validators.required]],
        name: ['', [Validators.required]],
        type: [
          { value: ProjectType.Iterativo, disabled: true },
          [Validators.required],
        ],
        startDate: [null, [Validators.required]],
        deadline: [null, [CustomValidators.minDateToday()]],
        iterationDuration: [null, [Validators.required, Validators.min(1)]],
        iterationCount: [null, [Validators.required, Validators.min(1)]],
        steps: this.buildIterativoStepsForm(),
        representatives: this.buildRepresentativesForm(),
        questionnaires: this.buildQuestionnairesForm(),
      },
      {
        validators: [
          CustomValidators.dateRange(
            'startDate',
            'deadline',
            'A data de início não pode ser maior que o prazo limite.'
          ),
        ],
      }
    );
  }

  protected override loadParams(params: any, queryParams?: any): void {

  }

  private _initForm(): void {
    this.projectForm = this.fb.group(
      {
        template: [null, [Validators.required]],
        name: ['', [Validators.required]],
        type: [
          { value: ProjectType.Iterativo, disabled: true },
          [Validators.required],
        ],
        startDate: [null, [Validators.required]],
        deadline: [null, [CustomValidators.minDateToday()]],
        iterationDuration: [null, [Validators.required, Validators.min(1)]],
        iterationCount: [null, [Validators.required, Validators.min(1)]],
        steps: this.buildIterativoStepsForm(),
        representatives: this.buildRepresentativesForm(),
        questionnaires: this.buildQuestionnairesForm(),
      },
      {
        validators: [
          CustomValidators.dateRange(
            'startDate',
            'deadline',
            'A data de início não pode ser maior que o prazo limite.'
          ),
        ],
      }
    );
  }

  private buildIterativoStepsForm(data?: any): FormGroup {
    const defaults = {
      initiation: 3,
      development: 3,
      requirements: 5,
      tests: 1,
    };
    const formData = data || defaults;
    return this.fb.group({
      initiation: [formData.initiation, Validators.required],
      development: [formData.development, Validators.required],
      requirements: [formData.requirements, Validators.required],
      tests: [formData.tests, Validators.required],
    });
  }

  private buildRepresentativesForm(data?: Representative[]): FormArray {
    const defaults: Representative[] = [
      {
        firstName: 'Maria',
        lastName: 'Visconde',
        email: 'cliente1@projeto.com',
        inclusionDate: '21/12/2024',
        weight: 1,
        roles: 'Desenvolvedor(a), Líder de Equipe',
      },
      {
        firstName: 'João',
        lastName: 'Ferreira',
        email: 'cliente2@projeto.com',
        inclusionDate: '21/12/2024',
        weight: 2,
        roles: 'Desenvolvedor(a)',
      },
      {
        firstName: 'Carlos',
        lastName: 'Moreno',
        email: 'cliente3@projeto.com',
        inclusionDate: '21/12/2024',
        weight: 1,
        roles: 'Analista de Negócios',
      },
      {
        firstName: 'Samara',
        lastName: 'Alviverde',
        email: 'cliente4@projeto.com',
        inclusionDate: '21/12/2024',
        weight: 3,
        roles: 'Gerente de Projeto',
      },
    ];

    const representativesData = data || defaults;

    return this.fb.array(
      representativesData.map((rep) =>
        this.fb.group({
          firstName: [rep.firstName, [Validators.required]],
          lastName: [rep.lastName, [Validators.required]],
          email: [rep.email, [Validators.required, Validators.email]],
          inclusionDate: [rep.inclusionDate, [Validators.required]],
          weight: [rep.weight, [Validators.required, Validators.min(1)]],
          roles: [rep.roles, [Validators.required]],
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
        console.log('Restaurando dados do formulário:', restoreParameter['formValue']);
        const formValue = restoreParameter['formValue'];

        this.projectForm.setControl(
          'steps',
          this.buildIterativoStepsForm(formValue.steps || {})
        );

        this.projectForm.setControl(
          'representatives',
          this.buildRepresentativesForm(formValue.representatives || [])
        );

        this.projectForm.setControl(
          'questionnaires',
          this.buildQuestionnairesForm(formValue.questionnaires || [])
        );

        this.projectForm.patchValue(formValue);
      }

      if (restoreParameter['panelStates']) {
        console.log('Restaurando estados dos painéis:', restoreParameter['panelStates']);
        this.panelStates = restoreParameter['panelStates'];
      }
    }

    this.cdr.markForCheck();
  }

  getControl(name: string): AbstractControl | null {
    return this.projectForm.get(name);
  }

  get stepsFormGroup(): FormGroup {
    return this.getControl('steps') as FormGroup;
  }

  get representativesFormArray(): FormArray {
    return this.getControl('representatives') as FormArray;
  }

  get questionnairesFormArray(): FormArray {
    return this.getControl('questionnaires') as FormArray;
  }

  addRepresentative(): void {
    const newRep = this.fb.group({
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      inclusionDate: [new Date().toLocaleDateString('pt-BR'), [Validators.required]],
      weight: [1, [Validators.required, Validators.min(1)]],
      roles: ['', [Validators.required]],
    });
    this.representativesFormArray.push(newRep);
    this.cdr.markForCheck();
  }

  removeRepresentative(index: number): void {
    this.representativesFormArray.removeAt(index);
    this.cdr.markForCheck();
  }

  private buildQuestionnairesForm(data?: Questionnaire[]): FormArray {
    const iterationCount = this.projectForm?.get('iterationCount')?.value || 4;
    const defaults: Questionnaire[] = Array.from({ length: iterationCount }, (_, i) => ({
      name: `Questionário ${i + 1}`,
      iteration: `Sprint ${i + 1}`,
      weight: 1,
      applicationStartDate: new Date().toISOString().split('T')[0],
      applicationEndDate: new Date().toISOString().split('T')[0],
    }));

    const questionnairesData = data || defaults;

    return this.fb.array(
      questionnairesData.map((q) =>
        this.fb.group({
          name: [q.name, [Validators.required]],
          iteration: [q.iteration, [Validators.required]],
          weight: [q.weight, [Validators.required, Validators.min(1)]],
          applicationStartDate: [q.applicationStartDate, [Validators.required]],
          applicationEndDate: [q.applicationEndDate, [Validators.required]],
        })
      )
    );
  }

  addQuestionnaire(): void {
    const newQ = this.fb.group({
      name: ['', [Validators.required]],
      iteration: ['', [Validators.required]],
      weight: [1, [Validators.required, Validators.min(1)]],
      applicationStartDate: [new Date().toISOString().split('T')[0], [Validators.required]],
      applicationEndDate: [new Date().toISOString().split('T')[0], [Validators.required]],
    });
    this.questionnairesFormArray.push(newQ);
    this.cdr.markForCheck();
  }

  removeQuestionnaire(index: number): void {
    this.questionnairesFormArray.removeAt(index);
    this.cdr.markForCheck();
  }

  getIterativeStepWeight(stepName: string): number {
    const step = this.stepsFormGroup?.get(stepName.toLowerCase());
    return step ? step.get('weight')?.value || 0 : 0;
  }

  addIterativeStep(): void {
    const newStepName = prompt('Nome da nova etapa:');
    if (newStepName && newStepName.trim()) {
      this.iterativeSteps.push(newStepName.trim());
      this.stepsFormGroup.addControl(
        newStepName.toLowerCase().replace(/\s+/g, '_'),
        this.fb.group({
          weight: [0, [Validators.required, Validators.min(0)]],
        })
      );
      this.cdr.markForCheck();
    }
  }

  removeIterativeStep(stepName: string): void {
    if (this.iterativeSteps.length > 1) {
      const index = this.iterativeSteps.indexOf(stepName);
      if (index > -1) {
        this.iterativeSteps.splice(index, 1);
        this.stepsFormGroup.removeControl(
          stepName.toLowerCase().replace(/\s+/g, '_')
        );
        this.cdr.markForCheck();
      }
    }
  }

  onSubmit(): void {
    if (this.projectForm.invalid) {
      this.projectForm.markAllAsTouched();
      return;
    }
    console.log('Formulário ITERATIVO Válido:', this.projectForm.getRawValue());
  }
}
