package de.simonaltschaeffl.poker.exception;

/**
 * Thrown when a player attempts to take an action, but the action handler
 * identifies that it is another player's turn.
 */
public class NotYourTurnException extends PokerException {
    public NotYourTurnException(String message) {
        super(message);
    }
}
