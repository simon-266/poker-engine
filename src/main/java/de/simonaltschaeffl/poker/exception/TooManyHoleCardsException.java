package de.simonaltschaeffl.poker.exception;

/**
 * Thrown when attempting to deal a third hole card to a player in Texas
 * Hold'em.
 */
public class TooManyHoleCardsException extends PokerException {
    /**
     * Constructs a new TooManyHoleCardsException.
     * @param message The error message.
     */
    public TooManyHoleCardsException(String message) {
        super(message);
    }
}
