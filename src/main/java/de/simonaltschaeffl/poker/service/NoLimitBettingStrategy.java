package de.simonaltschaeffl.poker.service;

import de.simonaltschaeffl.poker.model.ActionType;
import de.simonaltschaeffl.poker.model.GameState;
import de.simonaltschaeffl.poker.model.Player;
import de.simonaltschaeffl.poker.exception.InvalidActionException;

import java.util.EnumSet;
import java.util.Set;

/**
 * Betting rules for No-Limit Texas Hold'em.
 * Allows a player to bet any amount up to their entire stack (All-In).
 */
public class NoLimitBettingStrategy implements BettingRuleStrategy {

    @Override
    public Set<ActionType> getAllowedActions(Player player, GameState gameState, int highestRoundBet) {
        Set<ActionType> allowed = EnumSet.noneOf(ActionType.class);

        allowed.add(ActionType.FOLD);
        allowed.add(ActionType.ALL_IN);

        if (player.getCurrentBet() == highestRoundBet) {
            allowed.add(ActionType.CHECK);
        } else if (player.getChips() >= (highestRoundBet - player.getCurrentBet())) {
            allowed.add(ActionType.CALL);
        }

        // Raise is allowed as long as the player has room to raise past the current
        // highest bet
        if (player.getChips() > (highestRoundBet - player.getCurrentBet())) {
            allowed.add(ActionType.RAISE);
        }

        return allowed;
    }

    @Override
    public void validateRaise(Player player, int amount, int highestRoundBet, int bigBlind, int potTotal) {
        if (amount < highestRoundBet + bigBlind) {
            throw new InvalidActionException("Raise must be at least the previous highest bet plus the big blind ("
                    + (highestRoundBet + bigBlind) + ")");
        }
    }
}
