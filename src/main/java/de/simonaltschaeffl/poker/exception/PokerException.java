package de.simonaltschaeffl.poker.exception;

/**
 * Base runtime exception for all poker-engine related errors.
 */
public class PokerException extends RuntimeException {
    /**
     * Constructs a new PokerException.
     * @param message The error message.
     */
    public PokerException(String message) {
        super(message);
    }

    public PokerException(String message, Throwable cause) {
        super(message, cause);
    }
}
