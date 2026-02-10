package de.simonaltschaeffl.poker.client;

import de.simonaltschaeffl.poker.engine.PokerGame;
import de.simonaltschaeffl.poker.model.ActionType;
import de.simonaltschaeffl.poker.model.Card;
import de.simonaltschaeffl.poker.model.GameState;
import de.simonaltschaeffl.poker.model.Player;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BotPlayer extends Player {
    private final Random random = new Random();

    public BotPlayer(String id, String name, int chips) {
        super(id, name, chips);
    }

    public void decide(PokerGame game) {
        try {
            Thread.sleep(1000); // Simulate thinking
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        GameState state = game.getGameState();
        int currentBet = getCurrentBet();
        int highestBet = state.getPlayers().stream()
                .mapToInt(Player::getCurrentBet)
                .max().orElse(0);
        int callAmount = highestBet - currentBet;
        int chips = getChips();

        // Simple Logic
        // 1. If checking is free, check (90%) or small raise (10%)
        // 2. If calling is cheap (< 5% of stack), call.
        // 3. If calling is expensive, fold unless we have a pair+ (simulated randomness
        // for now).

        // For this simple bot, we'll just be passive/calling stations to keep the game
        // going.

        if (callAmount == 0) {
            // Can Check
            if (random.nextInt(100) < 10 && chips > game.getBigBlind()) {
                // Occasional Raise
                int raiseAmt = Math.min(chips, game.getBigBlind() * 2);
                System.out.println(getName() + " decides to RAISE " + raiseAmt);
                game.performAction(getId(), ActionType.RAISE, currentBet + raiseAmt);
            } else {
                System.out.println(getName() + " decides to CHECK");
                game.performAction(getId(), ActionType.CHECK, 0);
            }
        } else {
            // Must Call or Fold
            if (callAmount > chips) {
                // All in or Fold
                System.out.println(getName() + " decides to go ALL-IN (Call)");
                game.performAction(getId(), ActionType.CALL, 0); // Logic handles conversion to All-in
            } else {
                boolean goodHand = hasHighCard(); // Fake logic
                if (callAmount < chips * 0.1 || goodHand || random.nextBoolean()) {
                    System.out.println(getName() + " decides to CALL " + callAmount);
                    game.performAction(getId(), ActionType.CALL, 0);
                } else {
                    System.out.println(getName() + " decides to FOLD");
                    game.performAction(getId(), ActionType.FOLD, 0);
                }
            }
        }
    }

    private boolean hasHighCard() {
        // Very dumb "good hand" check
        List<Card> hole = getHoleCards();
        if (hole.size() < 2)
            return false;
        return hole.stream().anyMatch(c -> c.rank().getValue() >= 10); // 10, J, Q, K, A
    }

    @Override
    public void onLeave() {
        System.out.println("[BOT] " + getName() + " left the table.");
    }

    @Override
    public void onHandEnded(Map<String, Integer> payouts) {
        if (payouts.containsKey(getId())) {
            System.out.println("[BOT] " + getName() + " won " + payouts.get(getId()) + "!");
        }
    }
}
