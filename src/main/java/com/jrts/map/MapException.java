package com.jrts.map;

/**
 * Signals a problem loading or saving a map (missing file, malformed JSON, unsupported
 * version, invalid terrain data).
 */
public class MapException extends RuntimeException {

    public MapException(String message) {
        super(message);
    }

    public MapException(String message, Throwable cause) {
        super(message, cause);
    }
}
