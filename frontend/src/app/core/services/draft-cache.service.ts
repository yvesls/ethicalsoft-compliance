import { inject, Injectable, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { LoggerService } from './logger.service';

export interface DraftEntry<T = unknown> {
  key: string;
  data: T;
  savedAt: string;
  type: DraftType;
  meta?: Record<string, unknown>;
}

export type DraftType = 'project-creation' | 'questionnaire-response';

const DRAFT_PREFIX = 'draft_';
const DRAFT_INDEX_KEY = 'draft_index';

@Injectable({ providedIn: 'root' })
export class DraftCacheService {
  private readonly platformId = inject(PLATFORM_ID);

  save<T>(key: string, data: T, type: DraftType, meta?: Record<string, unknown>): void {
    if (!this.isBrowser()) return;

    const entry: DraftEntry<T> = {
      key,
      data,
      savedAt: new Date().toISOString(),
      type,
      meta,
    };

    try {
      const storageKey = `${DRAFT_PREFIX}${key}`;
      localStorage.setItem(storageKey, JSON.stringify(entry));
      this.addToIndex(key);
      LoggerService.info(`DraftCacheService: Rascunho salvo [${key}]`);
    } catch (error) {
      LoggerService.error('DraftCacheService: Erro ao salvar rascunho.', error);
    }
  }

  load<T>(key: string): DraftEntry<T> | null {
    if (!this.isBrowser()) return null;

    try {
      const storageKey = `${DRAFT_PREFIX}${key}`;
      const raw = localStorage.getItem(storageKey);
      if (!raw) return null;

      return JSON.parse(raw) as DraftEntry<T>;
    } catch (error) {
      LoggerService.error('DraftCacheService: Erro ao carregar rascunho.', error);
      return null;
    }
  }

  has(key: string): boolean {
    if (!this.isBrowser()) return false;
    return localStorage.getItem(`${DRAFT_PREFIX}${key}`) !== null;
  }

  remove(key: string): void {
    if (!this.isBrowser()) return;

    localStorage.removeItem(`${DRAFT_PREFIX}${key}`);
    this.removeFromIndex(key);
    LoggerService.info(`DraftCacheService: Rascunho removido [${key}]`);
  }

  listByType(type: DraftType): DraftEntry[] {
    if (!this.isBrowser()) return [];

    const index = this.getIndex();
    const entries: DraftEntry[] = [];

    for (const key of index) {
      const entry = this.load(key);
      if (entry && entry.type === type) {
        entries.push(entry);
      }
    }

    return entries;
  }

  clearByType(type: DraftType): void {
    const entries = this.listByType(type);
    for (const entry of entries) {
      this.remove(entry.key);
    }
  }

  clearAll(): void {
    if (!this.isBrowser()) return;

    const index = this.getIndex();
    for (const key of index) {
      localStorage.removeItem(`${DRAFT_PREFIX}${key}`);
    }
    localStorage.removeItem(DRAFT_INDEX_KEY);
    LoggerService.info('DraftCacheService: Todos os rascunhos removidos.');
  }

  projectDraftKey(projectType: string, projectId?: number | string | null): string {
    if (projectId) {
      return `project-${projectType.toLowerCase()}-${projectId}`;
    }
    return `project-${projectType.toLowerCase()}-new`;
  }

  responseDraftKey(projectId: string | number, questionnaireId: number, email?: string): string {
    const emailSuffix = email ? `-${email.replaceAll(/[^a-zA-Z0-9]/g, '')}` : '';
    return `response-${projectId}-${questionnaireId}${emailSuffix}`;
  }

  private getIndex(): string[] {
    try {
      const raw = localStorage.getItem(DRAFT_INDEX_KEY);
      return raw ? JSON.parse(raw) : [];
    } catch {
      return [];
    }
  }

  private addToIndex(key: string): void {
    const index = this.getIndex();
    if (!index.includes(key)) {
      index.push(key);
      localStorage.setItem(DRAFT_INDEX_KEY, JSON.stringify(index));
    }
  }

  private removeFromIndex(key: string): void {
    const index = this.getIndex().filter((k) => k !== key);
    localStorage.setItem(DRAFT_INDEX_KEY, JSON.stringify(index));
  }

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }
}
