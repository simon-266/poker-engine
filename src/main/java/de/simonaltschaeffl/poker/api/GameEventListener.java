package de.simonaltschaeffl.poker.api;

import de.simonaltschaeffl.poker.model.ActionType;
import de.simonaltschaeffl.poker.model.GameState;
import de.simonaltschaeffl.poker.model.Player;
import java.util.List;
import java.util.Map;
import java.util.Set;

    /**
     * Documentation.
     */
public interface GameEventListener {

    /**
     * Called when the game starts.
     */
    void onGameStarted();

    /**
     * Called when a new round (street) starts.
     * 
     * @param roundName e.g., "PRE_FLOP", "FLOP", "TURN", "RIVER"
     */
    void onRoundStarted(String roundName);

    /**
     * Broadcasts whenever the state of the game has meaningfully changed (e.g.
     * board updated,
     * bets collected, hand finished). Highly useful for WebSocket clients to just
     * pull `GameStateDTO.from()`.
     *
     * @param gameState The updated {@link GameState}.
     */
    void onGameStateChanged(GameState gameState);

    /**
     * Called when it becomes a specific player's turn to act.
     * 
     * @param player         The player whose turn it is.
     * @param allowedActions The Set of actions this player is legally allowed to
     *                       take.
     */
    void onPlayerTurn(Player player, Set<ActionType> allowedActions);

    /**
     * Called when a player performs a specific action.
     * This is the critical method for transaction logging.
     * 
     * @param player            The player performing the action.
     * @param action            The type of action (BET, CALL, FOLD, etc.).
     * @param amount            The amount involved (0 for Check/Fold).
     * @param chipBalanceBefore Balance before the action.
     * @param chipBalanceAfter  Balance after the action.
     */
    void onPlayerAction(Player player, ActionType action, int amount, int chipBalanceBefore, int chipBalanceAfter);

    /**
     * Called when the pot is updated (e.g., after a round of betting is collected).
     * 
     * @param potTotal Total chips in the pot.
     */
    void onPotUpdate(int potTotal);

    /**
     * Called when the hand ends.
     * 
     * @param winners   List of winning players.
     * @param payoutMap Map of Player ID to amount won.
     */
    void onHandEnded(List<Player> winners, Map<String, Integer> payoutMap);

    /**
     * Called when a player is added to the waiting list.
     * 
     * @param player The player who joined the waiting list.
     */
    void onPlayerJoinedWaitingList(Player player);

    /**
     * Called when house commission (rake) is deducted from the pot before showdown
     * distribution.
     * 
     * @param amount The amount of chips collected.
     */
    void onRakeCollected(int amount);
}
