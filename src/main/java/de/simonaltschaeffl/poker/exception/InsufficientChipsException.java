package de.simonaltschaeffl.poker.exception;

/**
 * Thrown when a player attempts to bet or raise an amount that exceeds their
 * current chip stack.
 */
public class InsufficientChipsException extends PokerException {
    /**
     * Constructs a new InsufficientChipsException.
     * @param message The error message.
     */
    public InsufficientChipsException(String message) {
        super(message);
    }
}
