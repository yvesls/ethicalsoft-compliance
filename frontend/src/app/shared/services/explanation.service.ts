import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { map, tap, shareReplay } from 'rxjs/operators';

export interface ExplanationEntry {
  title: string;
  description: string;
  items?: string[];
}

@Injectable({
  providedIn: 'root',
})
export class ExplanationService {
  private readonly httpClient = inject(HttpClient);
  private cache$ = new BehaviorSubject<Record<string, ExplanationEntry> | null>(null);
  private openedPopover$ = new Subject<string | null>();

  constructor() {
    this.loadExplanations().subscribe();
  }

  private loadExplanations(): Observable<Record<string, ExplanationEntry>> {
    return this.httpClient
      .get<Record<string, ExplanationEntry>>('assets/i18n/explanations.json')
      .pipe(
        tap((data) => this.cache$.next(data)),
        shareReplay(1)
      );
  }

  getExplanation(key: string): Observable<ExplanationEntry | null> {
    return this.cache$.asObservable().pipe(
      map((cache) => {
        if (!cache) return null;
        return cache[key] || null;
      })
    );
  }

  getExplanationSync(key: string): ExplanationEntry | null {
    const cache = this.cache$.value;
    if (!cache) return null;
    return cache[key] || null;
  }

  notifyPopoverOpened(componentId: string): void {
    this.openedPopover$.next(componentId);
  }

  onOtherPopoverOpened(): Observable<string | null> {
    return this.openedPopover$.asObservable();
  }
}
