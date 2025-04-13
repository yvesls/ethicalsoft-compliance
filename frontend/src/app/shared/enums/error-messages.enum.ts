export enum ErrorMessageEnum {
  UNAUTHORIZED = 'Credenciais inválidas. Verifique seu usuário e senha.',
  FORBIDDEN = 'Você não tem permissão para acessar este recurso.',
  NOT_FOUND = 'O recurso solicitado não foi encontrado.',
  INTERNAL_SERVER_ERROR = 'Erro interno do servidor. Tente novamente mais tarde.',
  BAD_REQUEST = 'A requisição está mal formada ou contém dados inválidos.',
  CONFLICT = 'Ocorreu um conflito com os dados enviados.',
  UNPROCESSABLE_ENTITY = 'Os dados enviados não podem ser processados.',
  TOO_MANY_REQUESTS = 'Muitas requisições. Tente novamente mais tarde.',
  SERVICE_UNAVAILABLE = 'O serviço está temporariamente indisponível.',
  UNKNOWN_ERROR = 'Ocorreu um erro desconhecido. Tente novamente mais tarde.'
}

export function getErrorMessage(status: number): string {
  switch (status) {
    case 400: return ErrorMessageEnum.BAD_REQUEST;
    case 401: return ErrorMessageEnum.UNAUTHORIZED;
    case 403: return ErrorMessageEnum.FORBIDDEN;
    case 404: return ErrorMessageEnum.NOT_FOUND;
    case 409: return ErrorMessageEnum.CONFLICT;
    case 422: return ErrorMessageEnum.UNPROCESSABLE_ENTITY;
    case 429: return ErrorMessageEnum.TOO_MANY_REQUESTS;
    case 500: return ErrorMessageEnum.INTERNAL_SERVER_ERROR;
    case 503: return ErrorMessageEnum.SERVICE_UNAVAILABLE;
    default: return ErrorMessageEnum.UNKNOWN_ERROR;
  }
}
