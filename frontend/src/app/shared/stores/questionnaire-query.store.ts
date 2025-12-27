import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { BaseStore } from './base/base.store';
import { RequestInputOptions } from '../../core/interfaces/request-input-options.interface';
import { Page } from '../interfaces/pageable.interface';
import {
  QuestionSearchFilter,
  QuestionnaireQuestionResponse,
  QuestionnaireRawResponse,
} from '../interfaces/questionnaire/questionnaire-query.interface';
import { UrlParameter } from '../../core/interfaces/url-parameter.interface';

@Injectable({
  providedIn: 'root',
})
export class QuestionnaireQueryStore extends BaseStore {
  constructor() {
    super('api/projects');
  }

  getQuestionnaireRaw(projectId: string, questionnaireId: number): Observable<QuestionnaireRawResponse> {
    const options: RequestInputOptions = {
      useAuth: true,
      useCache: true,
    };

    return this.requestService.makeGet<QuestionnaireRawResponse>(
      this.getUrl(`${projectId}/questionnaires/${questionnaireId}/raw`),
      options
    );
  }

  searchQuestions(
    projectId: string,
    questionnaireId: number,
    filter: QuestionSearchFilter | null,
    page: number,
    size: number
  ): Observable<Page<QuestionnaireQuestionResponse>> {
    const options: RequestInputOptions = {
      useAuth: true,
      data: filter ?? {},
    };

    const params: UrlParameter[] = [
      { key: 'page', value: page },
      { key: 'size', value: size },
    ];

    return this.requestService.makePost<Page<QuestionnaireQuestionResponse>>(
      this.getUrl(`${projectId}/questionnaires/${questionnaireId}/questions/search`),
      options,
      ...params
    );
  }
}
