package de.simonaltschaeffl.poker.engine;

import de.simonaltschaeffl.poker.api.GameEventListener;
import de.simonaltschaeffl.poker.model.*;
import de.simonaltschaeffl.poker.service.HandEvaluator;
import de.simonaltschaeffl.poker.service.StandardHandEvaluator;

import java.util.ArrayList;
import java.util.List;

public class PokerGame {
    private final GameState gameState;

    private final List<GameEventListener> listeners;
    private final int smallBlind;
    private final int bigBlind;

    private final de.simonaltschaeffl.poker.engine.component.TableManager tableManager;
    private final de.simonaltschaeffl.poker.engine.component.RuleEngine ruleEngine;
    private final de.simonaltschaeffl.poker.engine.component.ActionHandler actionHandler;
    private final de.simonaltschaeffl.poker.engine.component.RoundLifecycle roundLifecycle;

    public PokerGame(int smallBlind, int bigBlind, HandEvaluator handEvaluator, Deck deck) {
        this.gameState = new GameState();
        this.listeners = new ArrayList<>();
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;

        de.simonaltschaeffl.poker.service.PayoutCalculator payoutCalculator = new de.simonaltschaeffl.poker.service.PayoutCalculator(
                handEvaluator);
        this.tableManager = new de.simonaltschaeffl.poker.engine.component.TableManager(listeners);
        this.ruleEngine = new de.simonaltschaeffl.poker.engine.component.RuleEngine();
        this.actionHandler = new de.simonaltschaeffl.poker.engine.component.ActionHandler(listeners, ruleEngine);
        this.roundLifecycle = new de.simonaltschaeffl.poker.engine.component.RoundLifecycle(
                gameState, deck, listeners, payoutCalculator, tableManager, actionHandler, ruleEngine,
                smallBlind, bigBlind);
    }

    public PokerGame(int smallBlind, int bigBlind, HandEvaluator handEvaluator) {
        this(smallBlind, bigBlind, handEvaluator, new Deck());
    }

    // Legacy Constructor for backward compatibility (uses Standard)
    public PokerGame(int smallBlind, int bigBlind) {
        this(smallBlind, bigBlind, new StandardHandEvaluator());
    }

    public void join(Player player) {
        tableManager.join(player, gameState);
    }

    public void leave(Player player) {
        tableManager.leave(player, gameState, () -> performAction(player.getId(), ActionType.FOLD, 0));
    }

    public GameState getGameState() {
        return gameState;
    }

    public int getSmallBlind() {
        return smallBlind;
    }

    public int getBigBlind() {
        return bigBlind;
    }

    public void addListener(GameEventListener listener) {
        listeners.add(listener);
    }

    public void startHand() {
        roundLifecycle.startHand();
    }

    // --- Game Play Actions ---

    public void performAction(String playerId, ActionType type, int amount) {
        // 1. Validation
        Player player = gameState.getPlayers().stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));

        ruleEngine.validateAction(player, type, amount, bigBlind, gameState);

        // 2. Execution
        actionHandler.executeAction(player, type, amount, gameState);

        // 3. Move Game Forward
        roundLifecycle.advanceGame();
    }

}
