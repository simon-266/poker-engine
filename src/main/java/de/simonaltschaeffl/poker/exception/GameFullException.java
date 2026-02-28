package de.simonaltschaeffl.poker.exception;

/**
 * Thrown when attempting to join a game that has already reached its maximum
 * player capacity.
 */
public class GameFullException extends PokerException {
    public GameFullException(String message) {
        super(message);
    }
}
