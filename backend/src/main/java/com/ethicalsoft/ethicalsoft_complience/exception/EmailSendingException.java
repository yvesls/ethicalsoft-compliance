package com.ethicalsoft.ethicalsoft_complience.exception;


import lombok.Getter;

@Getter
public class EmailSendingException extends RuntimeException {

	private final String errorCode;

	public EmailSendingException(String message, Throwable cause) {
		super(message, cause);
		this.errorCode = null;
	}

	public EmailSendingException(String message, Throwable cause, String errorCode) {
		super(message, cause);
		this.errorCode = errorCode;
	}

}
