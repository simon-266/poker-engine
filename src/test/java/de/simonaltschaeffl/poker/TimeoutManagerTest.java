package de.simonaltschaeffl.poker;

import de.simonaltschaeffl.poker.engine.component.TimeoutManager;
import de.simonaltschaeffl.poker.model.ActionType;
import de.simonaltschaeffl.poker.model.GameState;
import de.simonaltschaeffl.poker.model.Player;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class TimeoutManagerTest {

    @Test
    public void testTimeoutForcesFold() throws InterruptedException {
        AtomicReference<ActionType> forcedAction = new AtomicReference<>();
        AtomicReference<String> actedPlayer = new AtomicReference<>();

        TimeoutManager timeoutManager = new TimeoutManager(50, (playerId, type, amount) -> {
            actedPlayer.set(playerId);
            forcedAction.set(type);
        });

        GameState state = new GameState();
        state.setPhase(GameState.GamePhase.PRE_FLOP);

        Player p1 = new TestPlayer("p1", "Alice", 1000);
        Player p2 = new TestPlayer("p2", "Bob", 1000);
        p2.bet(20); // Bob is BB
        p1.bet(10); // Alice is SB

        state.addPlayer(p1);
        state.addPlayer(p2);
        state.setCurrentActionPosition(0); // Alice to act

        // Turn just started
        state.setCurrentTurnStartTime(System.currentTimeMillis());

        // Check immediately - no timeout
        timeoutManager.checkTimeouts(state);
        assertNull(forcedAction.get());

        // Wait to exceed timeout (50ms)
        Thread.sleep(60);

        timeoutManager.checkTimeouts(state);

        // Should force FOLD because highest bet is 20, p1 bet 10 (needs to call)
        assertEquals("p1", actedPlayer.get());
        assertEquals(ActionType.FOLD, forcedAction.get());
    }

    @Test
    public void testTimeoutForcesCheckWhenAllowed() throws InterruptedException {
        AtomicReference<ActionType> forcedAction = new AtomicReference<>();
        AtomicReference<String> actedPlayer = new AtomicReference<>();

        TimeoutManager timeoutManager = new TimeoutManager(50, (playerId, type, amount) -> {
            actedPlayer.set(playerId);
            forcedAction.set(type);
        });

        GameState state = new GameState();
        state.setPhase(GameState.GamePhase.FLOP); // Post-flop, no bets yet

        Player p1 = new TestPlayer("p1", "Alice", 1000);
        Player p2 = new TestPlayer("p2", "Bob", 1000);

        state.addPlayer(p1);
        state.addPlayer(p2);
        state.setCurrentActionPosition(0); // Alice to act

        // Turn just started
        state.setCurrentTurnStartTime(System.currentTimeMillis());

        // Wait to exceed timeout
        Thread.sleep(60);

        timeoutManager.checkTimeouts(state);

        // Should force CHECK because highest bet is 0 and p1 matched it
        assertEquals("p1", actedPlayer.get());
        assertEquals(ActionType.CHECK, forcedAction.get());
    }
}
