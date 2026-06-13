export interface AiTokenStatus {
  configured: boolean;
  provider: string;
  tokenHint: string | null;
  updatedAt: string | null;
}

export interface UpdateAiTokenRequest {
  token: string;
}
