package de.simonaltschaeffl.poker.engine.component;

import de.simonaltschaeffl.poker.model.ActionType;
import de.simonaltschaeffl.poker.model.GameState;
import de.simonaltschaeffl.poker.model.Player;
import de.simonaltschaeffl.poker.model.PlayerStatus;

/**
 * Monitors the elapsed time since a player's turn started.
 * If the configured timeout is exceeded, automatically forces a check or fold.
 */
public class TimeoutManager {
    private final long actionTimeoutMs;
    // Functional interface for performAction to safely trigger actions without
    // circular dependencies
    private final ActionTrigger actionTrigger;

    public interface ActionTrigger {
        void trigger(String playerId, ActionType type, int amount);
    }

    public TimeoutManager(long actionTimeoutMs, ActionTrigger actionTrigger) {
        this.actionTimeoutMs = actionTimeoutMs;
        this.actionTrigger = actionTrigger;
    }

    /**
     * Checks if the active player's turn has exceeded the time bank.
     * Needs to be called periodically (e.g. by a background task / scheduler of the
     * embedding app).
     * 
     * @param gameState The current state.
     */
    public void checkTimeouts(GameState gameState) {
        if (actionTimeoutMs <= 0) {
            return; // Timeouts disabled
        }

        if (gameState.getPhase() == GameState.GamePhase.SHOWDOWN ||
                gameState.getPhase() == GameState.GamePhase.PRE_GAME ||
                gameState.getPhase() == GameState.GamePhase.HAND_ENDED) {
            return;
        }

        if (gameState.getPlayers().isEmpty()) {
            return;
        }

        Player activePlayer = gameState.getPlayers().get(gameState.getCurrentActionPosition());
        if (activePlayer.getStatus() != PlayerStatus.ACTIVE) {
            return;
        }

        long now = System.currentTimeMillis();
        long turnStart = gameState.getCurrentTurnStartTime();

        if (turnStart > 0 && (now - turnStart) > actionTimeoutMs) {
            // Determine whether to fold or check
            // Generally, a timeout should CHECK if free, otherwise FOLD.
            // Since validateAction logic is somewhat hidden here, we can try to find
            // highest bet.
            int highestRoundBet = gameState.getPlayers().stream()
                    .mapToInt(Player::getCurrentBet)
                    .max().orElse(0);

            ActionType forcedType = (activePlayer.getCurrentBet() == highestRoundBet) ? ActionType.CHECK
                    : ActionType.FOLD;

            // Execute forced action
            actionTrigger.trigger(activePlayer.getId(), forcedType, 0);
        }
    }
}
