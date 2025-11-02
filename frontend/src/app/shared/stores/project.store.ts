import { Observable } from 'rxjs';
import { Injectable } from '@angular/core';
import { BaseStore } from './base/base.store';
import { ProjectFilters } from '../interfaces/project-filters.interface';
import { Project } from '../interfaces/project.interface';
import { RequestInputOptions } from '../../core/interfaces/request-input-options.interface';
import { Page } from '../interfaces/pageable.interface';

@Injectable({
  providedIn: 'root',
})
export class ProjectStore extends BaseStore {
  constructor() {
    super('projects');
  }

  getProjects(filters: ProjectFilters): Observable<Page<Project>> {
    const { page, size, ...filterData } = filters;

    const options: RequestInputOptions = {
      useAuth: true,
      useCache: true,
      data: filterData,
    };

    const url = this.getUrl(`search?page=${page}&size=${size}`);
    return this.requestService.makePost<Page<Project>>(
      url,
      options
    );
  }
}
