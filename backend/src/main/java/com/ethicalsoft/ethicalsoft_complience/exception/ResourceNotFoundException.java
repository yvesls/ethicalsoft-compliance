package com.ethicalsoft.ethicalsoft_complience.exception;

import java.io.Serial;

public class ResourceNotFoundException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	public ResourceNotFoundException() {
		super();
	}

	public ResourceNotFoundException(String message) {
		super(message);
	}

	public ResourceNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}
