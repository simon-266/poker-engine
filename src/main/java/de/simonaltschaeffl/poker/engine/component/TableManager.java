package de.simonaltschaeffl.poker.engine.component;

import de.simonaltschaeffl.poker.api.GameEventListener;
import de.simonaltschaeffl.poker.model.GameState;
import de.simonaltschaeffl.poker.model.Player;
import de.simonaltschaeffl.poker.model.PlayerStatus;
import de.simonaltschaeffl.poker.exception.GameFullException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class TableManager {
    private final int maxPlayers;

    private final Queue<Player> waitingPlayers;
    private final List<GameEventListener> listeners;

    public TableManager(List<GameEventListener> listeners, int maxPlayers) {
        this.listeners = listeners;
        this.maxPlayers = maxPlayers;
        this.waitingPlayers = new LinkedList<>();
    }

    // Backwards compatible constructor
    public TableManager(List<GameEventListener> listeners) {
        this(listeners, 10);
    }

    public void join(Player player, GameState gameState) {
        if (gameState.getPhase() == GameState.GamePhase.PRE_GAME) {
            if (gameState.getPlayers().size() >= maxPlayers) {
                throw new GameFullException("Game is full (max " + maxPlayers + " players)");
            }
            gameState.addPlayer(player);
        } else {
            // Check total count (current + waiting)
            int total = gameState.getPlayers().size() + waitingPlayers.size();
            if (total >= maxPlayers) {
                throw new GameFullException("Game is full (max " + maxPlayers + " players)");
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
        while (!waitingPlayers.isEmpty() && gameState.getPlayers().size() < maxPlayers) {
            gameState.addPlayer(waitingPlayers.poll());
        }
    }

    private void notifyPlayerJoinedWaitingList(Player player) {
        listeners.forEach(l -> l.onPlayerJoinedWaitingList(player));
    }
}
