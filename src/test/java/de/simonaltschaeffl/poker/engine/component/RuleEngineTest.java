package de.simonaltschaeffl.poker.engine.component;

import de.simonaltschaeffl.poker.model.ActionType;
import de.simonaltschaeffl.poker.model.GameState;
import de.simonaltschaeffl.poker.model.Player;
import de.simonaltschaeffl.poker.service.BettingRuleStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

class RuleEngineTest {

    private RuleEngine ruleEngine;
    private BettingRuleStrategy mockStrategy;

    @BeforeEach
    void setUp() {
        mockStrategy = mock(BettingRuleStrategy.class);
        ruleEngine = new RuleEngine(mockStrategy);
    }

    @Test
    void getAllowedActions_shouldReturnStrategyActions() {
        Player p = new Player("1", "Alice", 1000) {
            @Override
            public void onLeave() {
            }

            @Override
            public void onHandEnded(java.util.Map<String, Integer> map) {
            }
        };
        GameState state = new GameState();
        state.addPlayer(p);
        state.setPhase(GameState.GamePhase.PRE_FLOP);
        when(mockStrategy.getAllowedActions(eq(p), eq(state), anyInt()))
                .thenReturn(Set.of(ActionType.CALL, ActionType.FOLD));

        Set<ActionType> allowed = ruleEngine.getAllowedActions(p, state);
        assertEquals(2, allowed.size());
    }
}
