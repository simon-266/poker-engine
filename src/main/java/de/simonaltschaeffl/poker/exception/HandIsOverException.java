package de.simonaltschaeffl.poker.exception;

/**
 * Thrown when an action or phase transition is attempted after a hand has
 * already ended or reached showdown.
 */
public class HandIsOverException extends PokerException {
    /**
     * Constructs a new HandIsOverException.
     * @param message The error message.
     */
    public HandIsOverException(String message) {
        super(message);
    }
}
