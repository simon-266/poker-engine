package de.simonaltschaeffl.poker.engine.component;

import de.simonaltschaeffl.poker.api.GameEventListener;
import de.simonaltschaeffl.poker.model.GameState;
import de.simonaltschaeffl.poker.model.Player;
import de.simonaltschaeffl.poker.model.PlayerStatus;

import java.util.ArrayList;
import java.util.List;

public class TableManager {
    private final List<Player> waitingPlayers;
    private final List<GameEventListener> listeners;

    public TableManager(List<GameEventListener> listeners) {
        this.listeners = listeners;
        this.waitingPlayers = new ArrayList<>();
    }

    public void join(Player player, GameState gameState) {
        if (gameState.getPhase() == GameState.GamePhase.PRE_GAME) {
            if (gameState.getPlayers().size() >= 10) {
                throw new IllegalStateException("Game is full (max 10 players)");
            }
            gameState.addPlayer(player);
        } else {
            // Check total count (current + waiting)
            int total = gameState.getPlayers().size() + waitingPlayers.size();
            if (total >= 10) {
                throw new IllegalStateException("Game is full (max 10 players)");
            }
            if (!gameState.getPlayers().contains(player) && !waitingPlayers.contains(player)) {
                waitingPlayers.add(player);
                notifyPlayerJoinedWaitingList(player);
            }
        }
    }

    public void leave(Player player, GameState gameState, Runnable onCurrentPlayerLeaving) {
        if (waitingPlayers.remove(player)) {
            player.onLeave();
            return; // Was only waiting
        }

        if (gameState.getPlayers().contains(player)) {
            if (gameState.getPhase() == GameState.GamePhase.PRE_GAME ||
                    gameState.getPhase() == GameState.GamePhase.HAND_ENDED) {
                gameState.removePlayer(player);
                player.onLeave();
            } else {
                // Active game
                player.setStatus(PlayerStatus.LEFT);
                player.onLeave();

                // If it was their turn, act to unblock
                if (gameState.getPlayers().size() > 0) { // Safety check
                    Player activePlayer = gameState.getPlayers().get(gameState.getCurrentActionPosition());
                    if (activePlayer.equals(player)) {
                        onCurrentPlayerLeaving.run();
                    }
                }
            }
        }
    }

    public void processSeatings(GameState gameState) {
        // Remove LEFT players
        List<Player> toRemove = new ArrayList<>();
        for (Player p : gameState.getPlayers()) {
            if (p.getStatus() == PlayerStatus.LEFT) {
                toRemove.add(p);
            }
        }
        toRemove.forEach(gameState::removePlayer);

        // Add Waiting Players
        while (!waitingPlayers.isEmpty() && gameState.getPlayers().size() < 10) {
            gameState.addPlayer(waitingPlayers.remove(0));
        }
    }

    private void notifyPlayerJoinedWaitingList(Player player) {
        listeners.forEach(l -> l.onPlayerJoinedWaitingList(player));
    }
}
