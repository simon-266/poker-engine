package de.simonaltschaeffl.poker.engine.component;

import de.simonaltschaeffl.poker.api.GameEventListener;
import de.simonaltschaeffl.poker.model.ActionType;
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
    private final GameContext context;

    public RoundLifecycle(GameContext context) {
        this.context = context;
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
        if (context.gameState().getPlayers().size() < 2) {
            throw new NotEnoughPlayersException("Not enough players");
        }

        // 0. Process Joins and Leaves
        context.tableManager().processSeatings(context.gameState());

        if (context.gameState().getPlayers().size() < 2) {
            context.gameState().setPhase(GameState.GamePhase.PRE_GAME);
            return;
        }

        notifyGameStarted();

        // 1. Reset
        context.deck().reset();
        context.gameState().clearBoard();
        context.gameState().getPot().reset();
        context.gameState().setPhase(GameState.GamePhase.PRE_FLOP);

        // 2. Deal Hole Cards
        for (Player p : context.gameState().getPlayers()) {
            p.clearHoleCards();
            p.setStatus(PlayerStatus.ACTIVE);
            context.deck().deal().ifPresent(p::addHoleCard);
            context.deck().deal().ifPresent(p::addHoleCard);
        }

        // 3. Post Blinds
        int sbPos, bbPos;
        int numPlayers = context.gameState().getPlayers().size();

        if (numPlayers == 2) {
            sbPos = context.gameState().getDealerButtonPosition();
            bbPos = (sbPos + 1) % numPlayers;
        } else {
            sbPos = (context.gameState().getDealerButtonPosition() + 1) % numPlayers;
            bbPos = (sbPos + 1) % numPlayers;
        }

        context.actionHandler().postBlind(context.gameState().getPlayers().get(sbPos), context.smallBlind(),
                ActionType.SMALL_BLIND, context.gameState());
        context.actionHandler().postBlind(context.gameState().getPlayers().get(bbPos), context.bigBlind(),
                ActionType.BIG_BLIND, context.gameState());

        notifyRoundStarted("PRE_FLOP");

        // Action starts left of BB
        context.gameState().clearActedPlayers();
        int firstActionPos = (numPlayers == 2) ? sbPos : (bbPos + 1) % numPlayers;
        context.gameState().setCurrentActionPosition(firstActionPos);

        notifyGameStateChanged();
        notifyPlayerTurn(context.gameState().getPlayers().get(firstActionPos));
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
        for (Player p : context.gameState().getPlayers()) {
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

        context.gameState().getPlayers().forEach(Player::resetBet);
        context.gameState().clearActedPlayers();
        context.gameState().setCurrentActionPosition(
                (context.gameState().getDealerButtonPosition() + 1) % context.gameState().getPlayers().size());

        // Reset action to first ACTIVE player
        if (context.gameState().getPlayers().get(context.gameState().getCurrentActionPosition())
                .getStatus() != PlayerStatus.ACTIVE) {
            moveActionToNextPlayer();
        }

        switch (context.gameState().getPhase()) {
            case PRE_FLOP -> {
                context.gameState().setPhase(GameState.GamePhase.FLOP);
                context.gameState().addToBoard(context.deck().deal().orElseThrow());
                context.gameState().addToBoard(context.deck().deal().orElseThrow());
                context.gameState().addToBoard(context.deck().deal().orElseThrow());
                notifyRoundStarted("FLOP");
                checkAutoAdvance();
            }
            case FLOP -> {
                context.gameState().setPhase(GameState.GamePhase.TURN);
                context.gameState().addToBoard(context.deck().deal().orElseThrow());
                notifyRoundStarted("TURN");
                checkAutoAdvance();
            }
            case TURN -> {
                context.gameState().setPhase(GameState.GamePhase.RIVER);
                context.gameState().addToBoard(context.deck().deal().orElseThrow());
                notifyRoundStarted("RIVER");
                checkAutoAdvance();
            }
            case RIVER -> {
                context.gameState().setPhase(GameState.GamePhase.SHOWDOWN);
                handleShowdown();
            }
            case SHOWDOWN, HAND_ENDED -> {
            }
            case PRE_GAME -> throw new HandIsOverException("Cannot transition from PRE_GAME without startHand()");
        }
        notifyGameStateChanged();
        if (context.gameState().getPhase() != GameState.GamePhase.SHOWDOWN
                && context.gameState().getPhase() != GameState.GamePhase.HAND_ENDED) {
            notifyPlayerTurn(context.gameState().getPlayers().get(context.gameState().getCurrentActionPosition()));
        }
    }

    /**
     * Advances the game logic after a player has taken an action.
     * If the betting round is complete according to the {@link RuleEngine},
     * the game transitions to the next phase. Otherwise, it moves the action
     * pointer to the next active player who needs to act.
     */
    public void advanceGame() {
        if (context.ruleEngine().isRoundComplete(context.gameState())) {
            transitionPhase();
        } else {
            moveActionToNextPlayer();
        }
    }

    private void moveActionToNextPlayer() {
        int startPos = context.gameState().getCurrentActionPosition();
        int nextIdx = (startPos + 1) % context.gameState().getPlayers().size();

        int attempts = 0;
        while (context.gameState().getPlayers().get(nextIdx).getStatus() != PlayerStatus.ACTIVE
                && attempts < context.gameState().getPlayers().size()) {
            nextIdx = (nextIdx + 1) % context.gameState().getPlayers().size();
            attempts++;
        }
        context.gameState().setCurrentActionPosition(nextIdx);
        notifyPlayerTurn(context.gameState().getPlayers().get(nextIdx));
    }

    private void checkAutoAdvance() {
        // Count active players without stream overhead
        long activeCount = 0;
        for (Player p : context.gameState().getPlayers()) {
            if (p.getStatus() == PlayerStatus.ACTIVE) {
                activeCount++;
            }
        }
        if (activeCount <= 1) {
            transitionPhase();
        }
    }

    private void handleWinByFold(Player winner) {
        int pot = context.gameState().getPot().getTotal();
        winner.win(pot);
        notifyHandEnded(List.of(winner), java.util.Map.of(winner.getId(), pot));
        context.gameState().setPhase(GameState.GamePhase.HAND_ENDED);
    }

    private void handleShowdown() {
        PayoutCalculator.ShowdownResult result = context.payoutCalculator()
                .calculate(context.gameState().getPlayers(), context.gameState().getBoard(),
                        context.gameState().getPot());

        // Apply payouts
        for (java.util.Map.Entry<String, Integer> entry : result.payouts().entrySet()) {
            context.gameState().getPlayers().stream()
                    .filter(pl -> pl.getId().equals(entry.getKey()))
                    .findFirst()
                    .ifPresent(p -> p.win(entry.getValue()));
        }

        notifyHandEnded(result.winners(), result.payouts());
        context.gameState().setPhase(GameState.GamePhase.HAND_ENDED);
    }

    private void notifyGameStarted() {
        context.listeners().forEach(GameEventListener::onGameStarted);
    }

    private void notifyRoundStarted(String round) {
        context.listeners().forEach(l -> l.onRoundStarted(round));
    }

    private void notifyHandEnded(List<Player> winners, java.util.Map<String, Integer> payouts) {
        context.listeners().forEach(l -> l.onHandEnded(winners, payouts));
    }

    private void notifyGameStateChanged() {
        context.listeners().forEach(l -> l.onGameStateChanged(context.gameState()));
    }

    private void notifyPlayerTurn(Player player) {
        context.gameState().setCurrentTurnStartTime(System.currentTimeMillis());
        context.listeners().forEach(
                l -> l.onPlayerTurn(player, context.ruleEngine().getAllowedActions(player, context.gameState())));
    }
}
