package de.simonaltschaeffl.poker.service;

import de.simonaltschaeffl.poker.model.GameState;
import de.simonaltschaeffl.poker.model.Player;
import de.simonaltschaeffl.poker.model.ActionType;

import java.util.Set;

/**
 * Defines the betting rules for specific poker variants (e.g. No-Limit,
 * Pot-Limit).
 */
public interface BettingRuleStrategy {
    /**
     * Determines all legal actions a player can take given the current game state
     * according to the betting variant.
     * 
     * @param player          The acting player.
     * @param gameState       The current game state.
     * @param highestRoundBet The highest bet currently placed in this round.
     * @return A set of all allowable actions.
     */
    Set<ActionType> getAllowedActions(Player player, GameState gameState, int highestRoundBet);

    /**
     * Validates a specific raise attempt according to the betting limits (e.g.
     * checking min/max raise sizes).
     * 
     * @param player          The acting player.
     * @param amount          The target total bet amount the player wants to reach.
     * @param highestRoundBet The highest bet currently placed in this round.
     * @param bigBlind        The big blind amount.
     * @param potTotal        The current total pot size (required for Pot-Limit
     *                        calculations).
     * @throws de.simonaltschaeffl.poker.exception.InvalidActionException if the
     *                                                                    raise
     *                                                                    amount is
     *                                                                    illegal.
     */
    void validateRaise(Player player, int amount, int highestRoundBet, int bigBlind, int potTotal);
}
