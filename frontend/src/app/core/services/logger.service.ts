import log from 'loglevel'

export class LoggerService {
	private static logger = log.getLogger('app')

	static init(): void {
		this.logger.setLevel('debug')
	}

	static info(message: string, ...params: unknown[]): void {
		this.logger.info(message, ...params)
	}

	static debug(message: string, ...params: unknown[]): void {
		this.logger.debug(message, ...params)
	}

	static warn(message: string, ...params: unknown[]): void {
		this.logger.warn(message, ...params)
	}

	static error(message: string, ...params: unknown[]): void {
		this.logger.error(message, ...params)
	}
}
