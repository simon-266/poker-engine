package de.simonaltschaeffl.poker.exception;

/**
 * Thrown when attempting to join a game that has already reached its maximum
 * player capacity.
 */
public class GameFullException extends PokerException {
    /**
     * Constructs a new GameFullException.
     * @param message The error message.
     */
    public GameFullException(String message) {
        super(message);
    }
}
