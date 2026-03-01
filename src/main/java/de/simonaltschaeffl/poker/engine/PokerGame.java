package de.simonaltschaeffl.poker.engine;

import de.simonaltschaeffl.poker.api.GameEventListener;
import de.simonaltschaeffl.poker.model.*;
import de.simonaltschaeffl.poker.service.HandEvaluator;
import de.simonaltschaeffl.poker.service.StandardHandEvaluator;
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
    private final TableManager tableManager;
    private final RuleEngine ruleEngine;
    private final ActionHandler actionHandler;
    private final RoundLifecycle roundLifecycle;
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

        this.tableManager = new TableManager(listeners,
                config.getMaxPlayers());
        this.ruleEngine = new RuleEngine(config.getBettingRuleStrategy());
        this.actionHandler = new ActionHandler(listeners, ruleEngine);

        GameContext context = new GameContext(
                gameState, deck, listeners, payoutCalculator, tableManager, actionHandler, ruleEngine,
                config.getSmallBlind(), config.getBigBlind());
        this.roundLifecycle = new RoundLifecycle(context);
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

    /**
     * Removes a player from the game.
     * If the player is currently in an active hand, they automatically fold.
     *
     * @param player The player to remove.
     */
    public void leave(Player player) {
        lock.lock();
        try {
            tableManager.leave(player, gameState, () -> performAction(player.getId(), ActionType.FOLD, 0));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gets the current observable state of the game.
     *
     * @return The {@link GameState}.
     */
    public GameState getGameState() {
        return gameState;
    }

    /**
     * Gets the current configuration of the poker game.
     *
     * @return The {@link PokerGameConfiguration}.
     */
    public PokerGameConfiguration getConfig() {
        return config;
    }

    /**
     * Retrieves the amount of the small blind for the game.
     *
     * @return The small blind amount.
     */
    public int getSmallBlind() {
        return smallBlind;
    }

    /**
     * Retrieves the amount of the big blind for the game.
     *
     * @return The big blind amount.
     */
    public int getBigBlind() {
        return bigBlind;
    }

    /**
     * Adds an event listener to observe game events.
     *
     * @param listener The {@link GameEventListener} to add.
     */
    public void addListener(GameEventListener listener) {
        listeners.add(listener);
    }

    /**
     * Triggers the start of a new poker hand.
     * Deals cards and posts blinds if there are enough players.
     */
    public void startHand() {
        lock.lock();
        try {
            roundLifecycle.startHand();
        } finally {
            lock.unlock();
        }
    }

    // --- Game Play Actions ---

    /**
     * Performs a player action (e.g., BET, CALL, FOLD, CHECK).
     *
     * @param playerId The ID of the player attempting to perform the action.
     * @param type     The {@link ActionType} being performed.
     * @param amount   The amount of chips involved in the action (can be 0 for
     *                 CHECK/FOLD).
     * @throws IllegalArgumentException if the player is not found.
     */
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

    /**
     * Actively checks if the current acting player's turn has timed out.
     * If they have timed out, a default action (Check or Fold) is forced.
     */
    public void checkTimeouts() {
        lock.lock();
        try {
            timeoutManager.checkTimeouts(gameState);
        } finally {
            lock.unlock();
        }
    }
}
