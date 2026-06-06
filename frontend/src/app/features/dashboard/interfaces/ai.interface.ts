export interface AiInsightResult {
  available: boolean;

  content: string | null;

  model: string | null;

  generatedAt: string;

  fallbackMessage: string | null;
}

export interface AiStatusResponse {
  enabled: boolean;
  provider: string;
  model: string;
}

export interface AskRequest {
  question: string;
}
