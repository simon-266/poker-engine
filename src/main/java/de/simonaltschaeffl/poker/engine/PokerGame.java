package de.simonaltschaeffl.poker.engine;

import de.simonaltschaeffl.poker.api.GameEventListener;
import de.simonaltschaeffl.poker.model.*;
import de.simonaltschaeffl.poker.service.HandEvaluator;
import de.simonaltschaeffl.poker.service.StandardHandEvaluator;
import de.simonaltschaeffl.poker.engine.component.TimeoutManager;

import java.util.ArrayList;
import java.util.List;

/**
 * The main facade and entry point for the Poker Engine.
 * Manages the {@link GameState}, players, and overall flow of a Texas Hold'em
 * game.
 * Designed to be modular and configurable via {@link PokerGameConfiguration}.
 * Emits events to registered {@link GameEventListener}s.
 */
public class PokerGame {
    private final GameState gameState;

    private final List<GameEventListener> listeners;
    private final int smallBlind;
    private final int bigBlind;

    private final PokerGameConfiguration config;
    private final de.simonaltschaeffl.poker.engine.component.TableManager tableManager;
    private final de.simonaltschaeffl.poker.engine.component.RuleEngine ruleEngine;
    private final de.simonaltschaeffl.poker.engine.component.ActionHandler actionHandler;
    private final de.simonaltschaeffl.poker.engine.component.RoundLifecycle roundLifecycle;
    private final TimeoutManager timeoutManager;

    private final java.util.concurrent.locks.ReentrantLock lock = new java.util.concurrent.locks.ReentrantLock();

    /**
     * Constructs a new PokerGame with the specified configuration, hand evaluator,
     * and deck.
     *
     * @param config        The game configuration (blinds, maximum players,
     *                      timeouts, strategies).
     * @param handEvaluator The evaluator to use for determining hand strengths at
     *                      showdown.
     * @param deck          The deck to use for dealing cards.
     */
    public PokerGame(PokerGameConfiguration config, HandEvaluator handEvaluator, Deck deck) {
        this.gameState = new GameState();
        this.listeners = new ArrayList<>();
        this.config = config;
        this.smallBlind = config.getSmallBlind();
        this.bigBlind = config.getBigBlind();

        de.simonaltschaeffl.poker.service.PayoutCalculator payoutCalculator = new de.simonaltschaeffl.poker.service.PayoutCalculator(
                handEvaluator, config.getRakeStrategy(), listeners);

        this.tableManager = new de.simonaltschaeffl.poker.engine.component.TableManager(listeners,
                config.getMaxPlayers());
        this.ruleEngine = new de.simonaltschaeffl.poker.engine.component.RuleEngine(config.getBettingRuleStrategy());
        this.actionHandler = new de.simonaltschaeffl.poker.engine.component.ActionHandler(listeners, ruleEngine);

        de.simonaltschaeffl.poker.engine.component.GameContext context = new de.simonaltschaeffl.poker.engine.component.GameContext(
                gameState, deck, listeners, payoutCalculator, tableManager, actionHandler, ruleEngine,
                config.getSmallBlind(), config.getBigBlind());
        this.roundLifecycle = new de.simonaltschaeffl.poker.engine.component.RoundLifecycle(context);
        this.timeoutManager = new TimeoutManager(config.getActionTimeoutMs(), this::performAction);
    }

    /**
     * Constructs a new PokerGame with the specified configuration using default
     * hand evaluator and deck.
     *
     * @param config The game configuration.
     */
    public PokerGame(PokerGameConfiguration config) {
        this(config, new StandardHandEvaluator(), new Deck());
    }

    /**
     * Adds a player to the game or waiting list.
     * 
     * @param player The player joining.
     */
    public void join(Player player) {
        lock.lock();
        try {
            tableManager.join(player, gameState);
        } finally {
            lock.unlock();
        }
    }

    public void leave(Player player) {
        lock.lock();
        try {
            tableManager.leave(player, gameState, () -> performAction(player.getId(), ActionType.FOLD, 0));
        } finally {
            lock.unlock();
        }
    }

    public GameState getGameState() {
        return gameState;
    }

    public PokerGameConfiguration getConfig() {
        return config;
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
        lock.lock();
        try {
            roundLifecycle.startHand();
        } finally {
            lock.unlock();
        }
    }

    // --- Game Play Actions ---

    public void performAction(String playerId, ActionType type, int amount) {
        lock.lock();
        try {
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
        } finally {
            lock.unlock();
        }
    }

    public void checkTimeouts() {
        lock.lock();
        try {
            timeoutManager.checkTimeouts(gameState);
        } finally {
            lock.unlock();
        }
    }
}
