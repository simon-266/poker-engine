package de.simonaltschaeffl.poker.model;

/**
 * Represents the different types of actions a player can take during a betting
 * round.
 */
public enum ActionType {
    /**
     * The player gives up their hand and forfeits any bets already made.
     */
    FOLD,
    /**
     * The player declines to bet but keeps their cards. Only possible if no bets
     * have been made in the round.
     */
    CHECK,
    /**
     * The player matches the current highest bet.
     */
    CALL,
    /**
     * The player increases the current highest bet.
     */
    RAISE,
    /**
     * The player bets all their remaining chips.
     */
    ALL_IN,
    /**
     * The forced bet made by the player to the left of the dealer.
     */
    SMALL_BLIND,
    /**
     * The forced bet made by the player to the left of the small blind.
     */
    BIG_BLIND
}
