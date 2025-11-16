import { Component, ChangeDetectionStrategy, OnInit, ChangeDetectorRef, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ModalService } from '../../../../core/services/modal.service';
import { TemplateListDTO, TemplatePartType } from '../../../../shared/interfaces/template/template.interface';
import { ProjectType } from '../../../../shared/enums/project-type.enum';
import { SelectComponent, SelectOption } from '../../../../shared/components/select/select.component';
import { TemplateStore } from '../../../../shared/stores/template.store';

@Component({
  selector: 'app-template-selector-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SelectComponent],
  templateUrl: './template-selector-modal.component.html',
  styleUrls: ['./template-selector-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TemplateSelectorModalComponent implements OnInit {
  @Input() projectType?: ProjectType;
  @Input() partType!: TemplatePartType;
  @Output() templateSelected = new EventEmitter<string>();

  templates: TemplateListDTO[] = [];
  templateOptions: SelectOption[] = [];
  templateControl = new FormControl<string | null>(null);
  isLoading = true;

  constructor(
    private modalService: ModalService,
    private templateStore: TemplateStore,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadTemplates();
  }

  private loadTemplates(): void {
    this.isLoading = true;
    this.templateStore.getAllTemplates().subscribe({
      next: (templates) => {
        this.templates = this.projectType
          ? templates.filter(t => t.type === this.projectType)
          : templates;

        this.templateOptions = this.templates.map(t => ({
          value: t.id,
          label: t.name
        }));

        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Erro ao carregar templates:', error);
        this.isLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  confirm(): void {
    const selectedTemplateId = this.templateControl.value;
    if (selectedTemplateId) {
      this.templateSelected.emit(selectedTemplateId);
      this.modalService.close();
    }
  }

  importFromExternal(): void {
    // TODO: Implementar importação de arquivo externo
  }

  close(): void {
    this.modalService.close();
  }
}
