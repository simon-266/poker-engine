package de.simonaltschaeffl.poker.engine;

import de.simonaltschaeffl.poker.api.GameEventListener;
import de.simonaltschaeffl.poker.model.Deck;
import de.simonaltschaeffl.poker.model.GameState;
import de.simonaltschaeffl.poker.service.PayoutCalculator;

import java.util.List;

/**
 * Encapsulates the core dependencies and configuration values required
 * during the lifecycle of a poker round.
 */
record GameContext(
        GameState gameState,
        Deck deck,
        List<GameEventListener> listeners,
        PayoutCalculator payoutCalculator,
        TableManager tableManager,
        ActionHandler actionHandler,
        RuleEngine ruleEngine,
        int smallBlind,
        int bigBlind) {
}
