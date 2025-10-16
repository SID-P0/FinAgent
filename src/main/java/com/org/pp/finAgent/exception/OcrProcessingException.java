package com.org.pp.finAgent.exception;

// A custom exception provides better context for errors.
public class OcrProcessingException extends RuntimeException {
    public OcrProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
    public OcrProcessingException(String message) {
        super(message);
    }
}
