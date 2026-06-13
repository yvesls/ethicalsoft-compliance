export enum ErrorMessageEnum {
	UNAUTHORIZED = 'errors.http.401',
	FORBIDDEN = 'errors.http.403',
	NOT_FOUND = 'errors.http.404',
	INTERNAL_SERVER_ERROR = 'errors.http.500',
	BAD_REQUEST = 'errors.http.400',
	CONFLICT = 'errors.http.409',
	UNPROCESSABLE_ENTITY = 'errors.http.422',
	TOO_MANY_REQUESTS = 'errors.http.429',
	SERVICE_UNAVAILABLE = 'errors.http.503',
	UNKNOWN_ERROR = 'errors.unknown',
}

export function getErrorMessage(status: number): string {
	switch (status) {
		case 400:
			return ErrorMessageEnum.BAD_REQUEST
		case 401:
			return ErrorMessageEnum.UNAUTHORIZED
		case 403:
			return ErrorMessageEnum.FORBIDDEN
		case 404:
			return ErrorMessageEnum.NOT_FOUND
		case 409:
			return ErrorMessageEnum.CONFLICT
		case 422:
			return ErrorMessageEnum.UNPROCESSABLE_ENTITY
		case 429:
			return ErrorMessageEnum.TOO_MANY_REQUESTS
		case 500:
			return ErrorMessageEnum.INTERNAL_SERVER_ERROR
		case 503:
			return ErrorMessageEnum.SERVICE_UNAVAILABLE
		default:
			return ErrorMessageEnum.UNKNOWN_ERROR
	}
}
