package de.simonaltschaeffl.poker;

import de.simonaltschaeffl.poker.api.GameEventListener;
import de.simonaltschaeffl.poker.engine.PokerGame;
import de.simonaltschaeffl.poker.engine.PokerGameConfiguration;
import de.simonaltschaeffl.poker.model.ActionType;
import de.simonaltschaeffl.poker.model.GameState;
import de.simonaltschaeffl.poker.model.Player;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

public class WaitingListTest {

    // Local implementation of Player for testing purposes
    static class TestPlayer extends Player {
        public TestPlayer(String id, String name, int chips) {
            super(id, name, chips);
        }

        @Override
        public void onLeave() {
        }

        @Override
        public void onHandEnded(Map<String, Integer> payouts) {
        }
    }

    static class TestGameListener implements GameEventListener {
        public Player lastPlayerJoined;

        @Override
        public void onGameStarted() {
        }

        @Override
        public void onRoundStarted(String roundName) {
        }

        @Override
        public void onGameStateChanged(GameState gameState) {
        }

        @Override
        public void onPlayerTurn(Player player, Set<ActionType> allowedActions) {
        }

        @Override
        public void onPlayerAction(Player player, ActionType action, int amount, int chipBalanceBefore,
                int chipBalanceAfter) {
        }

        @Override
        public void onPotUpdate(int potTotal) {
        }

        @Override
        public void onHandEnded(List<Player> winners, Map<String, Integer> payoutMap) {
        }

        @Override
        public void onPlayerJoinedWaitingList(Player player) {
            this.lastPlayerJoined = player;
        }

        @Override
        public void onRakeCollected(int amount) {
        }
    }

    @Test
    public void testPlayerJoinedWaitingListEvent() {
        // Arrange
        PokerGameConfiguration config = new PokerGameConfiguration.Builder()
                .smallBlind(10)
                .bigBlind(20)
                .maxPlayers(3)
                .build();
        PokerGame game = new PokerGame(config);
        TestGameListener listener = new TestGameListener();
        game.addListener(listener);

        // Join 2 players and start the game to change phase from PRE_GAME
        game.join(new TestPlayer("p1", "Player 1", 1000));
        game.join(new TestPlayer("p2", "Player 2", 1000));
        game.startHand();

        TestPlayer waitingPlayer = new TestPlayer("w1", "Waiting Player", 1000);

        // Act
        game.join(waitingPlayer);

        // Assert
        assertNotNull(listener.lastPlayerJoined, "Listener should have been notified");
        assertEquals(waitingPlayer, listener.lastPlayerJoined, "Notified player should match waiting player");
    }
}
