package de.simonaltschaeffl.poker.exception;

/**
 * Thrown when attempting to deal a third hole card to a player in Texas
 * Hold'em.
 */
public class TooManyHoleCardsException extends PokerException {
    public TooManyHoleCardsException(String message) {
        super(message);
    }
}
