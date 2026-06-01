import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpHeaders } from '@angular/common/http';
import { RequestService } from '../../../core/services/request.service';
import { AuthenticationService } from '../../../core/services/authentication.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { environment } from '../../../enviroments/environments';
import { AiInsightResult, AiStatusResponse } from '../interfaces/ai.interface';
import { UrlParameter } from '../../../core/interfaces/url-parameter.interface';

@Injectable({ providedIn: 'root' })
export class AiDashboardService {
  private readonly requestService = inject(RequestService);
  private readonly authService = inject(AuthenticationService);
  private readonly languageService = inject(LanguageService);

  private readonly _init = (() => {
    this.requestService.apiUrl = environment.apiBaseUrl;
  })();

  getInsights(projectId: number, questionnaireId: number): Observable<AiInsightResult> {
    const params: UrlParameter[] = [{ key: 'questionnaireId', value: questionnaireId }];
    return this.requestService.makeGet<AiInsightResult>(
      `api/projects/${projectId}/ai/insights`,
      { useAuth: true, headers: this.buildProjectHeader(projectId) },
      ...params
    );
  }

  getRiskReport(projectId: number, questionnaireId: number): Observable<AiInsightResult> {
    const params: UrlParameter[] = [{ key: 'questionnaireId', value: questionnaireId }];
    return this.requestService.makeGet<AiInsightResult>(
      `api/projects/${projectId}/ai/risk-report`,
      { useAuth: true, headers: this.buildProjectHeader(projectId) },
      ...params
    );
  }

  getExplanation(projectId: number, questionnaireId: number): Observable<AiInsightResult> {
    const params: UrlParameter[] = [{ key: 'questionnaireId', value: questionnaireId }];
    return this.requestService.makeGet<AiInsightResult>(
      `api/projects/${projectId}/ai/explain`,
      { useAuth: true, headers: this.buildProjectHeader(projectId) },
      ...params
    );
  }

  getAiStatus(projectId: number): Observable<AiStatusResponse> {
    return this.requestService.makeGet<AiStatusResponse>(
      `api/projects/${projectId}/ai/status`,
      { useAuth: true, headers: this.buildProjectHeader(projectId) }
    );
  }

  askQuestion(projectId: number, questionnaireId: number, question: string): Observable<string> {
    return new Observable(observer => {
      const token = this.authService.getToken() ?? '';
      const url = `${environment.apiBaseUrl}/api/projects/${projectId}/ai/ask?questionnaireId=${questionnaireId}`;
      const abortController = new AbortController();

      this.fetchSseStream(url, token, String(projectId), question, abortController, observer);

      return () => {
        abortController.abort();
      };
    });
  }

  private buildProjectHeader(projectId: number): HttpHeaders {
    return new HttpHeaders({
      'X-Project-Id': String(projectId),
      'Accept-Language': this.languageService.currentLanguage,
    });
  }

  private fetchSseStream(
    url: string,
    token: string,
    xProjectId: string,
    question: string,
    abortController: AbortController,
    observer: { next: (v: string) => void; error: (e: unknown) => void; complete: () => void }
  ): void {
    fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        'X-Project-Id': xProjectId,
        'Accept-Language': this.languageService.currentLanguage,
      },
      body: JSON.stringify({ question }),
      signal: abortController.signal,
    })
      .then(response => {
        if (!response.ok) {
          observer.error(new Error(`HTTP ${response.status}: ${response.statusText}`));
          return;
        }

        const reader = response.body?.getReader();
        if (!reader) {
          observer.error(new Error('ReadableStream not available'));
          return;
        }

        this.readSseChunks(reader, observer);
      })
      .catch(err => {
        if (err.name !== 'AbortError') {
          observer.error(err);
        }
      });
  }

  private readSseChunks(
    reader: ReadableStreamDefaultReader<Uint8Array>,
    observer: { next: (v: string) => void; error: (e: unknown) => void; complete: () => void }
  ): void {
    const decoder = new TextDecoder();
    let buffer = '';

    const processChunk = (): void => {
      reader
        .read()
        .then(({ done, value }) => {
          if (done) {
            this.flushSseBuffer(buffer, observer);
            observer.complete();
            return;
          }

          buffer += decoder.decode(value, { stream: true });

          const events = buffer.split('\n\n');
          buffer = events.pop() ?? '';

          for (const event of events) {
            this.flushSseBuffer(event, observer);
          }

          processChunk();
        })
        .catch(err => {
          if (err.name !== 'AbortError') {
            observer.error(err);
          }
        });
    };

    processChunk();
  }

  private flushSseBuffer(
    event: string,
    observer: { next: (v: string) => void }
  ): void {
    const dataLines: string[] = [];
    for (const line of event.split('\n')) {
      if (line.startsWith('data:')) {
        dataLines.push(line.substring(5).replace(/^ /, ''));
      }
    }
    if (dataLines.length === 0) return;

    const data = dataLines.join('\n');
    if (data === '[DONE]') return;

    observer.next(data);
  }
}
