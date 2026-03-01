package de.simonaltschaeffl.poker.engine.component;

import de.simonaltschaeffl.poker.api.GameEventListener;
import de.simonaltschaeffl.poker.model.ActionType;
import de.simonaltschaeffl.poker.model.GameState;
import de.simonaltschaeffl.poker.model.Player;
import de.simonaltschaeffl.poker.model.PlayerStatus;
import de.simonaltschaeffl.poker.service.BettingRuleStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ActionHandlerTest {

    private ActionHandler actionHandler;
    private GameState gameState;
    private boolean listenerTriggered = false;

    @BeforeEach
    void setUp() {
        GameEventListener listener = new GameEventListener() {
            @Override
            public void onGameStarted() {
            }

            @Override
            public void onRoundStarted(String s) {
            }

            @Override
            public void onGameStateChanged(GameState g) {
            }

            @Override
            public void onPlayerTurn(Player p, java.util.Set<ActionType> a) {
            }

            @Override
            public void onPlayerAction(Player p, ActionType a, int amt, int b, int c) {
                if (a == ActionType.FOLD && p.getId().equals("1"))
                    listenerTriggered = true;
            }

            @Override
            public void onPotUpdate(int p) {
            }

            @Override
            public void onHandEnded(List<Player> w, java.util.Map<String, Integer> p) {
            }

            @Override
            public void onPlayerJoinedWaitingList(Player p) {
            }

            @Override
            public void onRakeCollected(int a) {
            }
        };

        BettingRuleStrategy mockStrategy = mock(BettingRuleStrategy.class);
        RuleEngine ruleEngine = new RuleEngine(mockStrategy);
        actionHandler = new ActionHandler(List.of(listener), ruleEngine);
        gameState = new GameState();
    }

    @Test
    void executeAction_whenFold_shouldUpdateStatusAndNotify() {
        Player player = new Player("1", "Alice", 1000) {
            @Override
            public void onLeave() {
            }

            @Override
            public void onHandEnded(java.util.Map<String, Integer> map) {
            }
        };
        player.setStatus(PlayerStatus.ACTIVE);

        actionHandler.executeAction(player, ActionType.FOLD, 0, gameState);

        assertEquals(PlayerStatus.FOLDED, player.getStatus());
        assertTrue(gameState.hasActed(player));
        assertTrue(listenerTriggered);
    }
}
