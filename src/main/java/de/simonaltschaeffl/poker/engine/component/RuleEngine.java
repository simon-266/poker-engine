package de.simonaltschaeffl.poker.engine.component;

import de.simonaltschaeffl.poker.model.ActionType;
import de.simonaltschaeffl.poker.model.GameState;
import de.simonaltschaeffl.poker.model.Player;
import de.simonaltschaeffl.poker.model.PlayerStatus;
import de.simonaltschaeffl.poker.service.BettingRuleStrategy;

import de.simonaltschaeffl.poker.exception.NotYourTurnException;
import de.simonaltschaeffl.poker.exception.HandIsOverException;
import de.simonaltschaeffl.poker.exception.InvalidActionException;
import de.simonaltschaeffl.poker.exception.InsufficientChipsException;

import java.util.Set;

/**
 * Responsible for enforcing the rules of the game during a betting round.
 * This engine validates whether a player's attempted action is legal based on
 * the current {@link GameState}, the player's turn, and the active
 * {@link BettingRuleStrategy}.
 * It also determines when a betting round is complete.
 */
public class RuleEngine {

    private final BettingRuleStrategy bettingRuleStrategy;

    public RuleEngine(BettingRuleStrategy bettingRuleStrategy) {
        this.bettingRuleStrategy = bettingRuleStrategy;
    }

    /**
     * Determines the set of legal actions a player can take in the current game
     * state.
     * Returns an empty set if it is not the player's turn or if the game is active.
     *
     * @param player    The player requesting allowed actions.
     * @param gameState The current state of the game.
     * @return A set of {@link ActionType} representing the actions the player is
     *         allowed to perform.
     */
    public Set<ActionType> getAllowedActions(Player player, GameState gameState) {
        Player activePlayer = gameState.getPlayers().get(gameState.getCurrentActionPosition());
        if (!activePlayer.getId().equals(player.getId())) {
            return java.util.EnumSet.noneOf(ActionType.class); // Not their turn
        }

        if (gameState.getPhase() == GameState.GamePhase.SHOWDOWN
                || gameState.getPhase() == GameState.GamePhase.HAND_ENDED
                || gameState.getPhase() == GameState.GamePhase.PRE_GAME) {
            return java.util.EnumSet.noneOf(ActionType.class); // Game not active
        }

        int highestBet = getHighestRoundBet(gameState);

        return bettingRuleStrategy.getAllowedActions(player, gameState, highestBet);
    }

    /**
     * Validates if a specific action by a player is legal.
     * Throws specific runtime exceptions depending on the rule violation.
     *
     * @param player    The player attempting the action.
     * @param type      The type of action being attempted.
     * @param amount    The amount associated with the action (relevant for RAISE).
     * @param bigBlind  The current big blind amount.
     * @param gameState The current state of the game.
     * @throws NotYourTurnException       If the player attempts to act out of turn.
     * @throws HandIsOverException        If the action is attempted after the hand
     *                                    has concluded.
     * @throws InvalidActionException     If the action violates basic poker rules
     *                                    (e.g., checking when facing a bet).
     * @throws InsufficientChipsException If the player lacks the chips to complete
     *                                    the requested raise.
     */
    public void validateAction(Player player, ActionType type, int amount, int bigBlind, GameState gameState) {
        // 1. Turn Check
        Player activePlayer = gameState.getPlayers().get(gameState.getCurrentActionPosition());
        if (!activePlayer.getId().equals(player.getId())) {
            throw new NotYourTurnException("Not your turn! Waiting for " + activePlayer.getName());
        }

        // 2. Game Phase Check
        if (gameState.getPhase() == GameState.GamePhase.SHOWDOWN
                || gameState.getPhase() == GameState.GamePhase.HAND_ENDED) {
            throw new HandIsOverException("Hand is over");
        }

        // 3. Action Specific Rules
        int highestBet = getHighestRoundBet(gameState);

        switch (type) {
            case CHECK -> {
                if (player.getCurrentBet() < highestBet) {
                    throw new InvalidActionException(
                            "Cannot check, must call " + (highestBet - player.getCurrentBet()));
                }
            }
            case CALL -> {
                // Always valid if turn is correct (handles All-In logic elsewhere)
            }
            case RAISE -> {
                int potTotal = gameState.getPot().getTotal();
                bettingRuleStrategy.validateRaise(player, amount, highestBet, bigBlind, potTotal);

                int toAdd = amount - player.getCurrentBet();
                if (toAdd > player.getChips()) {
                    throw new InsufficientChipsException("Not enough chips to raise to " + amount);
                }
            }
            case ALL_IN -> {
                // Always valid if turn is correct
            }
            case FOLD -> {
                // Always valid
            }
            case SMALL_BLIND, BIG_BLIND -> throw new InvalidActionException("Blinds are posted automatically");
        }
    }

    /**
     * Checks whether the current betting round has concluded.
     * A round is complete when all active players have acted at least once,
     * and all active players have matched the highest bet (or are all-in).
     *
     * @param gameState The current state of the game.
     * @return {@code true} if the round is complete, {@code false} otherwise.
     */
    public boolean isRoundComplete(GameState gameState) {
        int highestBet = getHighestRoundBet(gameState);
        boolean allMatched = gameState.getPlayers().stream()
                .filter(p -> p.getStatus() == PlayerStatus.ACTIVE)
                .allMatch(p -> p.getCurrentBet() == highestBet);

        boolean everyoneActed = gameState.getPlayers().stream()
                .filter(p -> p.getStatus() == PlayerStatus.ACTIVE)
                .allMatch(gameState::hasActed);

        return allMatched && everyoneActed;
    }

    /**
     * Calculates the highest bet placed by any player in the current betting round.
     *
     * @param gameState The current state of the game.
     * @return The highest current bet amount, or 0 if no bets have been placed.
     */
    public int getHighestRoundBet(GameState gameState) {
        return gameState.getPlayers().stream()
                .mapToInt(Player::getCurrentBet)
                .max().orElse(0);
    }
}
