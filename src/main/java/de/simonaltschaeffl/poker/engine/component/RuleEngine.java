package de.simonaltschaeffl.poker.engine.component;

import de.simonaltschaeffl.poker.model.ActionType;
import de.simonaltschaeffl.poker.model.GameState;
import de.simonaltschaeffl.poker.model.Player;
import de.simonaltschaeffl.poker.model.PlayerStatus;

public class RuleEngine {

    public void validateAction(Player player, ActionType type, int amount, int bigBlind, GameState gameState) {
        // 1. Turn Check
        Player activePlayer = gameState.getPlayers().get(gameState.getCurrentActionPosition());
        if (!activePlayer.getId().equals(player.getId())) {
            throw new IllegalStateException("Not your turn! Waiting for " + activePlayer.getName());
        }

        // 2. Game Phase Check
        if (gameState.getPhase() == GameState.GamePhase.SHOWDOWN
                || gameState.getPhase() == GameState.GamePhase.HAND_ENDED) {
            throw new IllegalStateException("Hand is over");
        }

        // 3. Action Specific Rules
        int highestBet = getHighestRoundBet(gameState);

        switch (type) {
            case CHECK -> {
                if (player.getCurrentBet() < highestBet) {
                    throw new IllegalArgumentException(
                            "Cannot check, must call " + (highestBet - player.getCurrentBet()));
                }
            }
            case CALL -> {
                // Always valid if turn is correct (handles All-In logic elsewhere)
            }
            case RAISE -> {
                if (amount < highestBet + bigBlind) {
                    throw new IllegalArgumentException("Raise must be at least " + (highestBet + bigBlind));
                }

                int toAdd = amount - player.getCurrentBet();
                if (toAdd > player.getChips()) {
                    throw new IllegalArgumentException("Not enough chips to raise to " + amount);
                }
            }
            case ALL_IN -> {
                // Always valid if turn is correct
            }
            case FOLD -> {
                // Always valid
            }
            case SMALL_BLIND, BIG_BLIND -> throw new IllegalArgumentException("Blinds are posted automatically");
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
