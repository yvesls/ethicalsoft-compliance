import { Injectable } from '@angular/core';
import { QuestionnaireAnswerDocument } from '../../../shared/interfaces/questionnaire/questionnaire-response.interface';

@Injectable()
export class QuestionnaireAnswerCacheService {
  private readonly answerCache = new Map<number, QuestionnaireAnswerDocument>();

  private readonly dirtyIds = new Set<number>();

  saveAnswers(answers: readonly QuestionnaireAnswerDocument[]): void {
    for (const answer of answers) {
      this.answerCache.set(answer.questionId, { ...answer });
    }
  }

  markDirty(questionId: number, answer: QuestionnaireAnswerDocument): void {
    this.answerCache.set(questionId, { ...answer });
    this.dirtyIds.add(questionId);
  }

  applyCache(serverAnswers: readonly QuestionnaireAnswerDocument[]): QuestionnaireAnswerDocument[] {
    return serverAnswers.map((serverAnswer) => {
      if (this.dirtyIds.has(serverAnswer.questionId)) {
        const cached = this.answerCache.get(serverAnswer.questionId);
        return cached ? { ...cached } : serverAnswer;
      }
      return serverAnswer;
    });
  }

  clearSubmitted(questionIds: readonly number[]): void {
    for (const id of questionIds) {
      this.dirtyIds.delete(id);
    }
  }

  hasDirtyAnswers(): boolean {
    return this.dirtyIds.size > 0;
  }

  reset(): void {
    this.answerCache.clear();
    this.dirtyIds.clear();
  }
}
