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

public class RuleEngine {

    private final BettingRuleStrategy bettingRuleStrategy;

    public RuleEngine(BettingRuleStrategy bettingRuleStrategy) {
        this.bettingRuleStrategy = bettingRuleStrategy;
    }

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

    public int getHighestRoundBet(GameState gameState) {
        return gameState.getPlayers().stream()
                .mapToInt(Player::getCurrentBet)
                .max().orElse(0);
    }
}
