package com.ethicalsoft.ethicalsoft_complience.exception;


public class EmailSendingException extends RuntimeException {
    public EmailSendingException(String message, Throwable cause) {
        super(message, cause);
    }
}