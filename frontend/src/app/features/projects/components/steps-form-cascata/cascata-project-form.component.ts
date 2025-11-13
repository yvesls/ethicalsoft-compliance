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
  weight: number;
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

  public ProjectType = ProjectType;
  public projectForm!: FormGroup;
  public panelStates = {
    project: true,
    steps: false,
    representatives: false,
    questionnaires: false,
  };

  public templateOptions: SelectOption[] = [
    { value: 'template1', label: 'Padrão - Cascata' },
  ];
  public projectTypeOptions: SelectOption[] = [
    { value: ProjectType.Cascata, label: 'Cascata' },
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
          { value: ProjectType.Cascata, disabled: true },
          [Validators.required],
        ],
        startDate: [null, [Validators.required]],
        deadline: [null, [CustomValidators.minDateToday()]],
        steps: this.buildCascataStepsForm(),
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
          { value: ProjectType.Cascata, disabled: true },
          [Validators.required],
        ],
        startDate: [null, [Validators.required]],
        deadline: [null, [CustomValidators.minDateToday()]],
        steps: this.buildCascataStepsForm(),
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

  private buildCascataStepsForm(data?: any[]): FormArray {
    const defaults = [
      { name: 'Iniciação', weight: 3, dateRange: '' },
      { name: 'Requisitos', weight: 5, dateRange: '' },
      { name: 'Testes', weight: 1, dateRange: '' },
    ];
    const stepsData = data || defaults;
    const stepGroups = stepsData.map((step: any) => {
      return this.fb.group({
        name: [step.name, Validators.required],
        weight: [step.weight, [Validators.required, Validators.min(0)]],
        dateRange: [step.dateRange || ''],
      });
    });
    return this.fb.array(stepGroups, Validators.required);
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
        weight: 1,
        roles: 'Cliente',
      },
      {
        firstName: 'Carlos',
        lastName: 'Moreno',
        email: 'cliente3@projeto.com',
        inclusionDate: '21/12/2024',
        weight: 1,
        roles: 'Gerente de Projeto',
      },
      {
        firstName: 'Samara',
        lastName: 'Alviverde',
        email: 'cliente4@projeto.com',
        inclusionDate: '21/12/2024',
        weight: 1,
        roles: 'Analista de Qualidade',
      },
    ];
    const representativesData = data || defaults;
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
    this.stepsFormArray.push(
      this.fb.group({
        name: ['', Validators.required],
        weight: [0, [Validators.required, Validators.min(0)]],
        dateRange: [''],
      })
    );
    this.cdr.markForCheck();
  }

  removeStep(index: number): void {
    if (this.stepsFormArray.length > 1) {
      this.stepsFormArray.removeAt(index);
      this.cdr.markForCheck();
    }
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
    this.cdr.markForCheck();
  }

  removeRepresentative(index: number): void {
    this.representativesFormArray.removeAt(index);
    this.cdr.markForCheck();
  }

  private buildQuestionnairesForm(data?: Questionnaire[]): FormArray {
    const defaults: Questionnaire[] = [
      {
        name: 'Questionário 1',
        weight: 1,
        applicationStartDate: new Date().toISOString().split('T')[0],
        applicationEndDate: new Date().toISOString().split('T')[0],
      },
    ];

    const questionnairesData = data || defaults;

    return this.fb.array(
      questionnairesData.map((q) =>
        this.fb.group({
          name: [q.name, [Validators.required]],
          weight: [q.weight, [Validators.required, Validators.min(1)]],
          applicationStartDate: [q.applicationStartDate, [Validators.required]],
          applicationEndDate: [q.applicationEndDate, [Validators.required]],
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

        this.projectForm.setControl(
          'steps',
          this.buildCascataStepsForm(formValue.steps || [])
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
        this.panelStates = restoreParameter['panelStates'];
      }
    }

    this.cdr.markForCheck();
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

  onSubmit(): void {
    if (this.projectForm.invalid) {
      this.projectForm.markAllAsTouched();
      return;
    }
  }
}
