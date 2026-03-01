package de.simonaltschaeffl.poker.engine.component;

import de.simonaltschaeffl.poker.api.GameEventListener;
import de.simonaltschaeffl.poker.model.ActionType;
import de.simonaltschaeffl.poker.model.Deck;
import de.simonaltschaeffl.poker.model.GameState;
import de.simonaltschaeffl.poker.model.Player;
import de.simonaltschaeffl.poker.model.PlayerStatus;
import de.simonaltschaeffl.poker.service.PayoutCalculator;
import de.simonaltschaeffl.poker.exception.NotEnoughPlayersException;
import de.simonaltschaeffl.poker.exception.HandIsOverException;

import java.util.List;

/**
 * Manages the lifecycle and phases of a single poker round (hand).
 * Handles dealing cards, managing community cards, posting blinds, and
 * transitioning through the game phases
 * (PRE_FLOP, FLOP, TURN, RIVER, SHOWDOWN).
 */
public class RoundLifecycle {
    private final GameState gameState;
    private final Deck deck;
    private final List<GameEventListener> listeners;
    private final PayoutCalculator payoutCalculator;
    private final TableManager tableManager;
    private final ActionHandler actionHandler;
    private final RuleEngine ruleEngine;
    private final int smallBlind;
    private final int bigBlind;

    public RoundLifecycle(GameState gameState, Deck deck, List<GameEventListener> listeners,
            PayoutCalculator payoutCalculator, TableManager tableManager,
            ActionHandler actionHandler, RuleEngine ruleEngine,
            int smallBlind, int bigBlind) {
        this.gameState = gameState;
        this.deck = deck;
        this.listeners = listeners;
        this.payoutCalculator = payoutCalculator;
        this.tableManager = tableManager;
        this.actionHandler = actionHandler;
        this.ruleEngine = ruleEngine;
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
    }

    /**
     * Starts a new hand. Initializes the deck, clears the board, deals hole cards,
     * posts small and big blinds, and transitions the game to the {@code PRE_FLOP}
     * phase.
     * <p>
     * Before starting, it processes any pending seatings (players joining or
     * leaving).
     * If fewer than 2 players remain after processing, the hand is aborted and the
     * game returns to {@code PRE_GAME}.
     *
     * @throws NotEnoughPlayersException If there are fewer than 2 players before
     *                                   attempting to start.
     */
    public void startHand() {
        if (gameState.getPlayers().size() < 2) {
            throw new NotEnoughPlayersException("Not enough players");
        }

        // 0. Process Joins and Leaves
        tableManager.processSeatings(gameState);

        if (gameState.getPlayers().size() < 2) {
            gameState.setPhase(GameState.GamePhase.PRE_GAME);
            return;
        }

        notifyGameStarted();

        // 1. Reset
        deck.reset();
        gameState.clearBoard();
        gameState.getPot().reset();
        gameState.setPhase(GameState.GamePhase.PRE_FLOP);

        // 2. Deal Hole Cards
        for (Player p : gameState.getPlayers()) {
            p.clearHoleCards();
            p.setStatus(PlayerStatus.ACTIVE);
            deck.deal().ifPresent(p::addHoleCard);
            deck.deal().ifPresent(p::addHoleCard);
        }

        // 3. Post Blinds
        int sbPos, bbPos;
        int numPlayers = gameState.getPlayers().size();

        if (numPlayers == 2) {
            sbPos = gameState.getDealerButtonPosition();
            bbPos = (sbPos + 1) % numPlayers;
        } else {
            sbPos = (gameState.getDealerButtonPosition() + 1) % numPlayers;
            bbPos = (sbPos + 1) % numPlayers;
        }

        actionHandler.postBlind(gameState.getPlayers().get(sbPos), smallBlind, ActionType.SMALL_BLIND, gameState);
        actionHandler.postBlind(gameState.getPlayers().get(bbPos), bigBlind, ActionType.BIG_BLIND, gameState);

        notifyRoundStarted("PRE_FLOP");

        // Action starts left of BB
        gameState.clearActedPlayers();
        int firstActionPos = (numPlayers == 2) ? sbPos : (bbPos + 1) % numPlayers;
        gameState.setCurrentActionPosition(firstActionPos);

        notifyGameStateChanged();
        notifyPlayerTurn(gameState.getPlayers().get(firstActionPos));
    }

    /**
     * Evaluates the current game state to transition to the next game phase.
     * <p>
     * If all but one player have folded, the hand ends immediately and the pot
     * is awarded to the remaining active player without showing cards.
     * <p>
     * Otherwise, it advances the game (e.g., PRE_FLOP to FLOP), deals the required
     * community cards to the board, resets current bets, and determines which
     * player acts first in the new betting round.
     *
     * @throws HandIsOverException If attempting to transition from an inactive
     *                             PRE_GAME state without starting a hand first.
     */
    public void transitionPhase() {
        // Check Win by Fold
        // Find the active player and count simultaneously for optimization
        long activeCount = 0;
        Player winner = null;
        for (Player p : gameState.getPlayers()) {
            if (p.getStatus() == PlayerStatus.ACTIVE || p.getStatus() == PlayerStatus.ALL_IN) {
                activeCount++;
                if (winner == null) {
                    winner = p;
                }
            }
        }

        if (activeCount == 1) {
            handleWinByFold(winner);
            return;
        }

        gameState.getPlayers().forEach(Player::resetBet);
        gameState.clearActedPlayers();
        gameState.setCurrentActionPosition((gameState.getDealerButtonPosition() + 1) % gameState.getPlayers().size());

        // Reset action to first ACTIVE player
        if (gameState.getPlayers().get(gameState.getCurrentActionPosition()).getStatus() != PlayerStatus.ACTIVE) {
            moveActionToNextPlayer();
        }

        switch (gameState.getPhase()) {
            case PRE_FLOP -> {
                gameState.setPhase(GameState.GamePhase.FLOP);
                gameState.addToBoard(deck.deal().orElseThrow());
                gameState.addToBoard(deck.deal().orElseThrow());
                gameState.addToBoard(deck.deal().orElseThrow());
                notifyRoundStarted("FLOP");
                checkAutoAdvance();
            }
            case FLOP -> {
                gameState.setPhase(GameState.GamePhase.TURN);
                gameState.addToBoard(deck.deal().orElseThrow());
                notifyRoundStarted("TURN");
                checkAutoAdvance();
            }
            case TURN -> {
                gameState.setPhase(GameState.GamePhase.RIVER);
                gameState.addToBoard(deck.deal().orElseThrow());
                notifyRoundStarted("RIVER");
                checkAutoAdvance();
            }
            case RIVER -> {
                gameState.setPhase(GameState.GamePhase.SHOWDOWN);
                handleShowdown();
            }
            case SHOWDOWN, HAND_ENDED -> {
            }
            case PRE_GAME -> throw new HandIsOverException("Cannot transition from PRE_GAME without startHand()");
        }
        notifyGameStateChanged();
        if (gameState.getPhase() != GameState.GamePhase.SHOWDOWN
                && gameState.getPhase() != GameState.GamePhase.HAND_ENDED) {
            notifyPlayerTurn(gameState.getPlayers().get(gameState.getCurrentActionPosition()));
        }
    }

    /**
     * Advances the game logic after a player has taken an action.
     * If the betting round is complete according to the {@link RuleEngine},
     * the game transitions to the next phase. Otherwise, it moves the action
     * pointer to the next active player who needs to act.
     */
    public void advanceGame() {
        if (ruleEngine.isRoundComplete(gameState)) {
            transitionPhase();
        } else {
            moveActionToNextPlayer();
        }
    }

    private void moveActionToNextPlayer() {
        int startPos = gameState.getCurrentActionPosition();
        int nextIdx = (startPos + 1) % gameState.getPlayers().size();

        int attempts = 0;
        while (gameState.getPlayers().get(nextIdx).getStatus() != PlayerStatus.ACTIVE
                && attempts < gameState.getPlayers().size()) {
            nextIdx = (nextIdx + 1) % gameState.getPlayers().size();
            attempts++;
        }
        gameState.setCurrentActionPosition(nextIdx);
        notifyPlayerTurn(gameState.getPlayers().get(nextIdx));
    }

    private void checkAutoAdvance() {
        // Count active players without stream overhead
        long activeCount = 0;
        for (Player p : gameState.getPlayers()) {
            if (p.getStatus() == PlayerStatus.ACTIVE) {
                activeCount++;
            }
        }
        if (activeCount <= 1) {
            transitionPhase();
        }
    }

    private void handleWinByFold(Player winner) {
        int pot = gameState.getPot().getTotal();
        winner.win(pot);
        notifyHandEnded(List.of(winner), java.util.Map.of(winner.getId(), pot));
        gameState.setPhase(GameState.GamePhase.HAND_ENDED);
    }

    private void handleShowdown() {
        PayoutCalculator.ShowdownResult result = payoutCalculator
                .calculate(gameState.getPlayers(), gameState.getBoard(), gameState.getPot());

        // Apply payouts
        for (java.util.Map.Entry<String, Integer> entry : result.payouts().entrySet()) {
            gameState.getPlayers().stream()
                    .filter(pl -> pl.getId().equals(entry.getKey()))
                    .findFirst()
                    .ifPresent(p -> p.win(entry.getValue()));
        }

        notifyHandEnded(result.winners(), result.payouts());
        gameState.setPhase(GameState.GamePhase.HAND_ENDED);
    }

    private void notifyGameStarted() {
        listeners.forEach(GameEventListener::onGameStarted);
    }

    private void notifyRoundStarted(String round) {
        listeners.forEach(l -> l.onRoundStarted(round));
    }

    private void notifyHandEnded(List<Player> winners, java.util.Map<String, Integer> payouts) {
        listeners.forEach(l -> l.onHandEnded(winners, payouts));
    }

    private void notifyGameStateChanged() {
        listeners.forEach(l -> l.onGameStateChanged(gameState));
    }

    private void notifyPlayerTurn(Player player) {
        gameState.setCurrentTurnStartTime(System.currentTimeMillis());
        listeners.forEach(l -> l.onPlayerTurn(player, ruleEngine.getAllowedActions(player, gameState)));
    }
}
