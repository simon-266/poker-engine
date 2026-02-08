package de.simonaltschaeffl.poker.api;

import de.simonaltschaeffl.poker.model.ActionType;
import de.simonaltschaeffl.poker.model.Player;
import java.util.List;
import java.util.Map;

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
}
