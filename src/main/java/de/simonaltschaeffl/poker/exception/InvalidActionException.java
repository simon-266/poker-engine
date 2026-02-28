package de.simonaltschaeffl.poker.exception;

/**
 * Thrown when an action is fundamentally invalid for the current game state
 * (e.g. attempting to CHECK when a player has an outstanding call amount).
 */
public class InvalidActionException extends PokerException {
    public InvalidActionException(String message) {
        super(message);
    }
}
