import { ProjectTypeSelectionComponent } from './../../components/project-type-selection/project-type-selection.component';
import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  OnInit,
  DestroyRef,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
} from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  catchError,
  debounceTime,
  distinctUntilChanged,
  EMPTY,
  map,
  switchMap,
  tap,
} from 'rxjs';

import {
  Project
} from '../../../../shared/interfaces/project/project.interface';
import { ProjectStatus } from '../../../../shared/enums/project-status.enum';
import { ProjectType } from '../../../../shared/enums/project-type.enum';
import { PaginationComponent } from '../../../../shared/components/pagination/pagination.component';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';
import { ProjectStore } from '../../../../shared/stores/project.store';
import { ProjectFilters } from '../../../../shared/interfaces/project/project-filters.interface';
import { Page } from '../../../../shared/interfaces/pageable.interface';
import { InputComponent } from '../../../../shared/components/input/input.component';
import { SelectComponent, SelectOption } from '../../../../shared/components/select/select.component';
import { FilterBarComponent } from '../../../../shared/components/filter-bar/filter-bar.component';
import { ListComponent } from '../../../../shared/components/list/list.component';
import { ListItemComponent } from '../../../../shared/components/list-item/list-item.component';
import { ModalService } from '../../../../core/services/modal.service';
import { AuthenticationService } from '../../../../core/services/authentication.service';
import { RoleEnum } from '../../../../shared/enums/role.enum';

const getEnumKeys = (enumObject: Record<string, string | number>): string[] =>
  Object.keys(enumObject).filter((key) => typeof enumObject[key] === 'string');

interface ProjectFilterFormValue {
  name: string;
  type: ProjectType | null;
  status: ProjectStatus | null;
  code: string;
}

interface ProjectListState {
  projects: Project[];
  pagination: {
    currentPage: number;
    totalItems: number;
    pageSize: number;
  };
  status: 'loading' | 'loaded' | 'error';
  error: string | null;
}

@Component({
  selector: 'app-project-list-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    PaginationComponent,
    NgxSpinnerModule,
    InputComponent,
    SelectComponent,
    FilterBarComponent,
    ListComponent,
    ListItemComponent,
  ],
  templateUrl: './project-list-page.component.html',
  styleUrl: './project-list-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectListPageComponent implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private destroyRef = inject(DestroyRef);
  private projectStore = inject(ProjectStore);
  private spinner = inject(NgxSpinnerService);
  private modalService = inject(ModalService);
  private authService = inject(AuthenticationService);

  projectTypes = getEnumKeys(ProjectType);
  projectStatuses = getEnumKeys(ProjectStatus);

  projectTypeOptions: SelectOption[];
  projectStatusOptions: SelectOption[];

  public ProjectType = ProjectType;
  filterForm: FormGroup;

  private initialState: ProjectListState = {
    projects: [],
    pagination: { currentPage: 1, totalItems: 0, pageSize: 10 },
    status: 'loading',
    error: null,
  };
  state = signal<ProjectListState>(this.initialState);

  projects = computed(() => this.state().projects);
  pagination = computed(() => this.state().pagination);
  status = computed(() => this.state().status);
  error = computed(() => this.state().error);

  private readonly userRoles = signal<string[]>([]);
  readonly isAdmin = computed(() => this.userRoles().includes(RoleEnum.ADMIN));

  statusIconMap = {
    [ProjectStatus.Aberto]: 'assets/icons/status-aberto.svg',
    [ProjectStatus.Rascunho]: 'assets/icons/status-rascunho.svg',
    [ProjectStatus.Concluido]: 'assets/icons/status-concluido.svg',
    [ProjectStatus.Arquivado]: 'assets/icons/status-arquivado.svg',
  };

  constructor() {
    this.filterForm = this.fb.group({
      name: [''],
      type: [null],
      status: [null],
      code: [''],
    });

    this.projectTypeOptions = this.projectTypes.map(key => ({
      value: key,
      label: key
    }));

    this.projectStatusOptions = this.projectStatuses.map(key => ({
      value: key,
      label: key
    }));
  }

  ngOnInit(): void {
    this.listenToUserRoles();
    this.listenToQueryChanges();
  }

  private listenToUserRoles(): void {
    this.authService.userRoles$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((roles) => this.userRoles.set(roles ?? []));
  }

  private listenToQueryChanges(): void {
    this.route.queryParamMap
      .pipe(
        map((params) => {
          const uiPage = Number(params.get('page') || '1');
          const filters: ProjectFilters = {
            name: params.get('name') || undefined,
            code: params.get('code') || undefined,
            type: (params.get('type') as ProjectType) || undefined,
            status: (params.get('status') as ProjectStatus) || undefined,
            page: uiPage - 1,
            size: this.initialState.pagination.pageSize,
          };

          this.filterForm.patchValue({
            name: filters.name || '',
            code: filters.code || '',
            type: filters.type || null,
            status: filters.status || null
          }, { emitEvent: false });

          return filters;
        }),
        tap(() => {
          this.state.update((s) => ({ ...s, status: 'loading' }));
          this.spinner.show();
        }),
        debounceTime(300),
        distinctUntilChanged(
          (prev, curr) => JSON.stringify(prev) === JSON.stringify(curr)
        ),
        switchMap((filters) =>
          this.projectStore.getProjects(filters).pipe(
            catchError((err) => {
              this.state.update((s) => ({
                ...s,
                status: 'error',
                error: err.message || 'Falha ao carregar projetos.',
              }));
              this.spinner.hide();
              return EMPTY;
            })
          )
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((result: Page<Project>) => {
        this.state.set({
          projects: result.content,
          pagination: {
            currentPage: result.pageable.pageNumber + 1,
            totalItems: result.totalElements,
            pageSize: result.size,
          },
          status: 'loaded',
          error: null,
        });
        this.spinner.hide();
      });
  }

  onSearch(): void {
    this.updateQueryParams(this.getFilterFormValue(), 1);
  }

  onPageChange(page: number): void {
    this.updateQueryParams(this.getFilterFormValue(), page);
  }

  goToCreateProject(): void {
    if (!this.isAdmin()) {
      return;
    }
    this.modalService.open(ProjectTypeSelectionComponent, 'small-card');
  }

  private updateQueryParams(filters: ProjectFilterFormValue, page: number): void {
    const queryParams: Record<string, string | number> = {
      page,
    };
    for (const [key, value] of Object.entries(filters)) {
      if (value) {
        queryParams[key] = value;
      }
    }

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
    });
  }

  private getFilterFormValue(): ProjectFilterFormValue {
    const rawValue = this.filterForm.getRawValue() as ProjectFilterFormValue;
    return {
      name: rawValue.name ?? '',
      code: rawValue.code ?? '',
      type: rawValue.type ?? null,
      status: rawValue.status ?? null,
    };
  }

  trackByProjectId(index: number, project: Project): string {
    return project.id;
  }
}
