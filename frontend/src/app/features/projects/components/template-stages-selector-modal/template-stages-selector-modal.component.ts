import { Component, ChangeDetectionStrategy, OnInit, ChangeDetectorRef, Input, Output, EventEmitter, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ModalService } from '../../../../core/services/modal.service';
import { TemplateStore } from '../../../../shared/stores/template.store';
import { TemplateStageDTO } from '../../../../shared/interfaces/template/template.interface';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export interface SelectedStage extends TemplateStageDTO {
  selected: boolean;
  dateRange?: string;
}

@Component({
  selector: 'app-template-stages-selector-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './template-stages-selector-modal.component.html',
  styleUrls: ['./template-stages-selector-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TemplateStagesSelectorModalComponent implements OnInit {
  @Input() templateId!: string;
  @Input() templateName!: string;
  @Output() stagesSelected = new EventEmitter<SelectedStage[]>();

  stages: SelectedStage[] = [];
  form!: FormGroup;
  isLoading = true;
  private readonly modalService = inject(ModalService);
  private readonly templateStore = inject(TemplateStore);
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.form = this.fb.group({
      stages: this.fb.array([])
    });
    this.loadStages();
  }

  get stagesFormArray(): FormArray {
    return this.form.get('stages') as FormArray;
  }

  private loadStages(): void {
    this.isLoading = true;
    this.templateStore.getTemplateStages(this.templateId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
      next: (stages) => {
        this.stages = stages.map(stage => ({
          ...stage,
          selected: false
        }));

        this.buildFormArray();
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Erro ao carregar etapas:', error);
        this.isLoading = false;
        this.cdr.markForCheck();
        }
      });
  }

  private buildFormArray(): void {
    for (const stage of this.stages) {
      this.stagesFormArray.push(
        this.fb.group({
          selected: [stage.selected],
          name: [stage.name],
          weight: [stage.weight]
        })
      );
    }
  }

  toggleSelection(index: number): void {
    const control = this.stagesFormArray.at(index);
    const currentValue = control.get('selected')?.value;
    control.get('selected')?.setValue(!currentValue);
  }

  selectAll(event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    for (const control of this.stagesFormArray.controls) {
      control.get('selected')?.setValue(checked);
    }
  }

  get hasSelection(): boolean {
    return this.stagesFormArray.controls.some(
      control => control.get('selected')?.value === true
    );
  }

  confirm(): void {
    const selectedStages: SelectedStage[] = this.stagesFormArray.controls
      .filter(control => control.get('selected')?.value)
      .map(control => ({
        name: control.get('name')?.value,
        weight: control.get('weight')?.value,
        selected: true
      }));

    this.stagesSelected.emit(selectedStages);
    this.modalService.close();
  }

  close(): void {
    this.modalService.close();
  }
}
