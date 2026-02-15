package de.simonaltschaeffl.poker.engine.component;

import de.simonaltschaeffl.poker.api.GameEventListener;
import de.simonaltschaeffl.poker.model.ActionType;
import de.simonaltschaeffl.poker.model.Deck;
import de.simonaltschaeffl.poker.model.GameState;
import de.simonaltschaeffl.poker.model.Player;
import de.simonaltschaeffl.poker.model.PlayerStatus;
import de.simonaltschaeffl.poker.service.PayoutCalculator;

import java.util.List;

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

    public void startHand() {
        if (gameState.getPlayers().size() < 2) {
            throw new IllegalStateException("Not enough players");
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
        if (numPlayers == 2) {
            gameState.setCurrentActionPosition(sbPos);
        } else {
            gameState.setCurrentActionPosition((bbPos + 1) % numPlayers);
        }
    }

    public void transitionPhase() {
        // Check Win by Fold
        long activeCount = gameState.getPlayers().stream()
                .filter(p -> p.getStatus() == PlayerStatus.ACTIVE || p.getStatus() == PlayerStatus.ALL_IN)
                .count();

        if (activeCount == 1) {
            Player winner = gameState.getPlayers().stream()
                    .filter(p -> p.getStatus() == PlayerStatus.ACTIVE || p.getStatus() == PlayerStatus.ALL_IN)
                    .findFirst().orElseThrow();
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
            case PRE_GAME -> throw new IllegalStateException("Cannot transition from PRE_GAME without startHand()");
        }
    }

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
    }

    private void checkAutoAdvance() {
        long activeCount = gameState.getPlayers().stream()
                .filter(p -> p.getStatus() == PlayerStatus.ACTIVE)
                .count();
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
}
