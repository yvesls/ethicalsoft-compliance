export interface AiTokenStatus {
  configured: boolean;
  provider: string;
  tokenHint: string | null;
  updatedAt: string | null; // ISO-8601
}

export interface UpdateAiTokenRequest {
  token: string;
}
