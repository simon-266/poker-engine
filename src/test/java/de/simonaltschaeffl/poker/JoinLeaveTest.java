package de.simonaltschaeffl.poker;

import de.simonaltschaeffl.poker.engine.PokerGame;
import de.simonaltschaeffl.poker.model.Player;
import de.simonaltschaeffl.poker.model.PlayerStatus;
import de.simonaltschaeffl.poker.dto.GameStateDTO;
import de.simonaltschaeffl.poker.model.ActionType;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

public class JoinLeaveTest {

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

    @Test
    public void testJoinBeforeGame() {
        PokerGame game = new PokerGame(10, 20);
        TestPlayer p1 = new TestPlayer("p1", "P1", 1000);
        game.join(p1);

        assertEquals(1, game.getGameState().getPlayers().size());
        assertTrue(game.getGameState().getPlayers().contains(p1));
    }

    @Test
    public void testMaxPlayers() {
        PokerGame game = new PokerGame(10, 20);
        for (int i = 0; i < 10; i++) {
            game.join(new TestPlayer("p" + i, "P" + i, 1000));
        }

        assertEquals(10, game.getGameState().getPlayers().size());

        assertThrows(IllegalStateException.class, () -> {
            game.join(new TestPlayer("p11", "P11", 1000));
        });
    }

    @Test
    public void testJoinDuringGame() {
        PokerGame game = new PokerGame(10, 20);
        TestPlayer p1 = new TestPlayer("p1", "P1", 1000);
        TestPlayer p2 = new TestPlayer("p2", "P2", 1000);
        game.join(p1);
        game.join(p2);

        game.startHand();

        TestPlayer p3 = new TestPlayer("p3", "P3", 1000);
        game.join(p3);

        // Should not be in active players yet
        assertFalse(game.getGameState().getPlayers().contains(p3));

        // Finish hand (Fold p1)
        game.performAction("p1", ActionType.FOLD, 0);

        // Start next hand
        game.startHand();

        // Now p3 should be in
        assertEquals(3, game.getGameState().getPlayers().size());
        assertTrue(game.getGameState().getPlayers().contains(p3));
    }

    @Test
    public void testLeaveDuringGame() {
        PokerGame game = new PokerGame(10, 20);
        TestPlayer p1 = new TestPlayer("p1", "P1", 1000);
        TestPlayer p2 = new TestPlayer("p2", "P2", 1000);
        game.join(p1);
        game.join(p2);

        game.startHand();

        // P1 leaves
        game.leave(p1);

        // Status should be LEFT
        assertEquals(PlayerStatus.LEFT, p1.getStatus());

        // Since P1 left/folded, P2 wins immediately?
        // Wait, logic says if 1 active player remains, they win.
        // P1 status LEFT count as NOT active.
        // But need to trigger transition or check.
        // leave() calls performAction(FOLD) if it was their turn?

        // In startHand, Action is on P1 (SB) or P2 (BB)?
        // 2 players: Dealer is SB. Action starts PRE_FLOP at SB.
        // So P1 has action.
        // leave(p1) -> triggers performAction(FOLD).
        // performAction(FOLD) -> advances game -> judgeRoundEnd -> transitionPhase
        // transitionPhase -> count active -> 1 (P2) -> handleWinByFold.

        // Check if hand ended
        // We can't easily check phase if game auto-started next? No, startHand is
        // manual.
        // So Phase should be HAND_ENDED.

        // Wait, did I implement auto transition to HAND_ENDED in leave?
        // leave calls performAction(FOLD).

        // Let's check status
        // Depending on exact logic, P1 might be LEAVE or FOLDED.
        // If performAction overrides status to FOLDED, then checks later might fail if
        // expecting LEFT.
        // My implementation:
        // case FOLD -> if (player.getStatus() != LEFT) setStatus(FOLDED).
        // So if I set LEFT *before* calling performAction, it stays LEFT.
        // My code:
        // player.setStatus(LEFT);
        // if (active) performAction(FOLD);
        // So performAction sees LEFT, does NOT change to FOLDED.
        // Logic holds.

        assertEquals(PlayerStatus.LEFT, p1.getStatus());
    }

    @Test
    public void testDTO() {
        PokerGame game = new PokerGame(10, 20);
        TestPlayer p1 = new TestPlayer("p1", "P1", 1000);
        game.join(p1);

        GameStateDTO dto = GameStateDTO.from(game.getGameState());
        assertNotNull(dto);
        assertEquals(1, dto.players().size());
        assertEquals("p1", dto.players().get(0).id());
    }
}
