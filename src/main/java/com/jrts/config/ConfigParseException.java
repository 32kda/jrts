package com.jrts.config;

/**
 * Exception for configuration parsing errors.
 * Includes a reference to the problematic file and field.
 */
public class ConfigParseException extends RuntimeException {

    public ConfigParseException(String message) {
        super(message);
    }

    public ConfigParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
