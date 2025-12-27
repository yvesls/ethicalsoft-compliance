import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { BehaviorSubject, Observable } from 'rxjs';
import { LoggerService } from './logger.service';

@Injectable({
  providedIn: 'root',
})
export class ProjectContextService {
  private readonly storageKey = 'current_project_id';
  private readonly platformId = inject(PLATFORM_ID);

  private readonly projectIdSubject = new BehaviorSubject<string | null>(
    this.loadFromStorage()
  );

  readonly projectId$: Observable<string | null> = this.projectIdSubject.asObservable();

  setCurrentProjectId(projectId: string | null): void {
    this.projectIdSubject.next(projectId);
    this.persist(projectId);
    LoggerService.info('ProjectContextService: Current project set.', { projectId });
  }

  clearCurrentProjectId(): void {
    this.setCurrentProjectId(null);
  }

  getCurrentProjectId(): string | null {
    return this.projectIdSubject.value;
  }

  private loadFromStorage(): string | null {
    if (isPlatformBrowser(this.platformId)) {
      try {
        return sessionStorage.getItem(this.storageKey);
      } catch (error) {
        LoggerService.error('ProjectContextService: Failed to load project id from storage.', error);
      }
    }
    return null;
  }

  private persist(projectId: string | null): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    try {
      if (projectId) {
        sessionStorage.setItem(this.storageKey, projectId);
      } else {
        sessionStorage.removeItem(this.storageKey);
      }
    } catch (error) {
      LoggerService.error('ProjectContextService: Failed to persist project id to storage.', error);
    }
  }
}
