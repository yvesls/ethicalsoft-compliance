import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { DashboardService } from '../../services/dashboard.service';
import {
  ConsolidatedAnswerDTO,
  ConsolidatedAnswerFilters,
  Page,
} from '../../interfaces/dashboard.interface';
import { NotificationService } from '../../../../core/services/notification.service';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { FilterBarComponent } from '../../../../shared/components/filter-bar/filter-bar.component';
import { PaginationComponent } from '../../../../shared/components/pagination/pagination.component';
import { SelectComponent, SelectOption } from '../../../../shared/components/select/select.component';
import { InputComponent } from '../../../../shared/components/input/input.component';
import { ResponseDetailModalComponent } from '../../components/response-detail-modal/response-detail-modal.component';
import { ModalService } from '../../../../core/services/modal.service';
import { RoleService } from '../../../../core/services/role.service';

interface UniqueMember {
  representativeId: number;
  representativeName: string;
  questionnaireId: number;
}

@Component({
  selector: 'app-consolidated-answers-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslateModule,
    FilterBarComponent,
    PaginationComponent,
    SelectComponent,
    InputComponent,
  ],
  templateUrl: './consolidated-answers-page.component.html',
  styleUrl: './consolidated-answers-page.component.scss',
})
export class ConsolidatedAnswersPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly dashboardService = inject(DashboardService);
  private readonly notificationService = inject(NotificationService);
  private readonly modalService = inject(ModalService);
  private readonly roleService = inject(RoleService);
  private readonly translate = inject(TranslateService);

  projectId!: number;
  questionnaireId: number | null = null;

  mode: 'questionnaire' | 'project' = 'project';

  page = signal<Page<ConsolidatedAnswerDTO>>({
    content: [],
    totalElements: 0,
    totalPages: 0,
    size: 20,
    number: 0,
  });
  loading = signal(true);
  currentPage = signal(1);
  pageSize = 20;

  uniqueMembers = computed(() => {
    const seen = new Map<number, UniqueMember>();
    for (const row of this.page().content) {
      if (!seen.has(row.representativeId)) {
        seen.set(row.representativeId, {
          representativeId: row.representativeId,
          representativeName: row.representativeName,
          questionnaireId: row.questionnaireId,
        });
      }
    }
    return Array.from(seen.values());
  });

  roleOptions = signal<SelectOption[]>([]);

  filterForm!: FormGroup;

  responseOptions: SelectOption[] = [
    { value: 'true', label: 'SIM' },
    { value: 'false', label: 'NÃO' },
  ];

  get gridColumns(): string {
    return this.mode === 'project' ? '1fr 1fr 1fr 1fr 1fr' : '1fr 1fr 1fr 1fr';
  }

  get pageTitle(): string {
    return this.mode === 'project'
      ? 'Respostas Consolidadas do Projeto'
      : 'Respostas Consolidadas do Questionário';
  }

  ngOnInit(): void {
    this.projectId = Number(this.route.snapshot.paramMap.get('projectId'));
    const qId = this.route.snapshot.paramMap.get('questionnaireId');
    if (qId) {
      this.questionnaireId = Number(qId);
      this.mode = 'questionnaire';
    }

    this.filterForm = this.fb.group({
      questionText: [null],
      representativeId: [null],
      roleId: [null],
      response: [null],
      ...(this.mode === 'project' ? { questionnaireId: [null] } : {}),
    });

    this.loadRoles();
    this.load();
  }

  private loadRoles(): void {
    this.roleService.getRoles().subscribe({
      next: (roles) => {
        this.roleOptions.set(
          roles.map(r => ({ value: r.id.toString(), label: r.name }))
        );
      },
    });
  }

  load(): void {
    this.loading.set(true);
    const filters = this.buildFilters();

    const request$ =
      this.mode === 'questionnaire'
        ? this.dashboardService.getQuestionnaireConsolidatedResponses(
            this.projectId,
            this.questionnaireId!,
            filters
          )
        : this.dashboardService.getProjectConsolidatedResponses(
            this.projectId,
            filters
          );

    request$.subscribe({
      next: (result) => {
        this.page.set(result);
        this.loading.set(false);
      },
      error: () => {
        this.notificationService.showError(this.translate.instant('dashboard.consolidated.load_error'));
        this.loading.set(false);
      },
    });
  }

  onFilter(): void {
    this.currentPage.set(1);
    this.load();
  }

  clearFilters(): void {
    this.filterForm.reset();
    this.currentPage.set(1);
    this.load();
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
    this.load();
  }

  viewMemberResponses(member: UniqueMember): void {
    this.modalService.open(ResponseDetailModalComponent, 'large-card', {
      projectId: this.projectId,
      questionnaireId: member.questionnaireId,
      representativeId: member.representativeId,
      representativeName: member.representativeName,
    });
  }

  private buildFilters(): ConsolidatedAnswerFilters {
    const f = this.filterForm.value;
    return {
      page: this.currentPage() - 1,
      size: this.pageSize,
      questionText: f.questionText || null,
      representativeId: f.representativeId ? Number(f.representativeId) : null,
      roleId: f.roleId ? Number(f.roleId) : null,
      response: f.response != null ? f.response === 'true' : null,
      questionnaireId: f.questionnaireId ? Number(f.questionnaireId) : null,
    };
  }
}
