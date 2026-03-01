package de.simonaltschaeffl.poker.exception;

/**
 * Thrown when a hand is attempted to start, but there are fewer than the
 * required number of players (typically 2).
 */
public class NotEnoughPlayersException extends PokerException {
    /**
     * Constructs a new NotEnoughPlayersException.
     * @param message The error message.
     */
    public NotEnoughPlayersException(String message) {
        super(message);
    }
}
