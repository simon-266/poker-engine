package de.simonaltschaeffl.poker.exception;

/**
 * Thrown when a hand is attempted to start, but there are fewer than the
 * required number of players (typically 2).
 */
public class NotEnoughPlayersException extends PokerException {
    public NotEnoughPlayersException(String message) {
        super(message);
    }
}
