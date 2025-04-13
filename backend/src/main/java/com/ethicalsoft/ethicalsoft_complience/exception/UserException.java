package com.ethicalsoft.ethicalsoft_complience.exception;

import java.io.Serial;

public class UserException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	public UserException( String message ) {
		super( message );
	}

	public UserException( String message, Throwable cause ) {
		super( message, cause );
	}

}
