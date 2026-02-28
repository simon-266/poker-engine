package de.simonaltschaeffl.poker.engine.component;

import de.simonaltschaeffl.poker.api.GameEventListener;
import de.simonaltschaeffl.poker.model.ActionType;
import de.simonaltschaeffl.poker.model.GameState;
import de.simonaltschaeffl.poker.model.Player;
import de.simonaltschaeffl.poker.model.PlayerStatus;
import de.simonaltschaeffl.poker.exception.InvalidActionException;

import java.util.List;

public class ActionHandler {
    private final List<GameEventListener> listeners;
    private final RuleEngine ruleEngine;

    public ActionHandler(List<GameEventListener> listeners, RuleEngine ruleEngine) {
        this.listeners = listeners;
        this.ruleEngine = ruleEngine;
    }

    public void executeAction(Player player, ActionType type, int amount, GameState gameState) {
        int balanceBefore = player.getChips();
        int potTotalBefore = gameState.getPot().getTotal();

        // Execution Logic
        switch (type) {
            case FOLD -> {
                if (player.getStatus() != PlayerStatus.LEFT) {
                    player.setStatus(PlayerStatus.FOLDED);
                }
            }
            case CHECK -> {
                // No chip movement
            }
            case CALL -> {
                int highestBet = ruleEngine.getHighestRoundBet(gameState);
                int toCall = highestBet - player.getCurrentBet();
                if (toCall > player.getChips()) {
                    // Treat as All-In (Partial Call)
                    amount = player.getChips();
                    type = ActionType.ALL_IN;
                    player.bet(amount);
                    player.setStatus(PlayerStatus.ALL_IN);
                } else {
                    player.bet(toCall);
                    amount = toCall; // Actual amount bet
                }
                gameState.getPot().add(player, amount);
            }
            case RAISE -> {
                int toAdd = amount - player.getCurrentBet();
                player.bet(toAdd);
                gameState.getPot().add(player, toAdd);
            }
            case ALL_IN -> {
                amount = player.getChips();
                player.bet(amount);
                gameState.getPot().add(player, amount);
                player.setStatus(PlayerStatus.ALL_IN);
            }
            case SMALL_BLIND, BIG_BLIND -> throw new InvalidActionException("Blinds should be posted via postBlind");
        }

        // Notify
        notifyPlayerAction(player, type, amount, balanceBefore, player.getChips());
        if (gameState.getPot().getTotal() != potTotalBefore) {
            notifyPotUpdate(gameState.getPot().getTotal());
        }
        notifyGameStateChanged(gameState);

        // Mark as acted
        gameState.addActedPlayer(player);
    }

    public void postBlind(Player player, int amount, ActionType type, GameState gameState) {
        int balanceBefore = player.getChips();
        player.bet(amount);
        gameState.getPot().add(player, amount);

        notifyPlayerAction(player, type, amount, balanceBefore, player.getChips());
        notifyPotUpdate(gameState.getPot().getTotal());
        notifyGameStateChanged(gameState);
    }

    private void notifyPlayerAction(Player p, ActionType type, int amount, int before, int after) {
        listeners.forEach(l -> l.onPlayerAction(p, type, amount, before, after));
    }

    private void notifyPotUpdate(int total) {
        listeners.forEach(l -> l.onPotUpdate(total));
    }

    private void notifyGameStateChanged(GameState gameState) {
        listeners.forEach(l -> l.onGameStateChanged(gameState));
    }
}
