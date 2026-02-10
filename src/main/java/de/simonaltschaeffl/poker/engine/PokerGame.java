package de.simonaltschaeffl.poker.engine;

import de.simonaltschaeffl.poker.api.GameEventListener;
import de.simonaltschaeffl.poker.model.*;
import de.simonaltschaeffl.poker.service.HandEvaluator;
import de.simonaltschaeffl.poker.service.StandardHandEvaluator;

import java.util.ArrayList;
import java.util.List;

public class PokerGame {
    private final GameState gameState;
    private final Deck deck;
    private final HandEvaluator handEvaluator;
    private final List<GameEventListener> listeners;
    private final int smallBlind;
    private final int bigBlind;
    private final List<Player> waitingPlayers;

    public PokerGame(int smallBlind, int bigBlind, HandEvaluator handEvaluator, Deck deck) {
        this.gameState = new GameState();
        this.deck = deck;
        this.handEvaluator = handEvaluator;
        this.listeners = new ArrayList<>();
        this.waitingPlayers = new ArrayList<>();
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
    }

    public PokerGame(int smallBlind, int bigBlind, HandEvaluator handEvaluator) {
        this(smallBlind, bigBlind, handEvaluator, new Deck());
    }

    // Legacy Constructor for backward compatibility (uses Standard)
    public PokerGame(int smallBlind, int bigBlind) {
        this(smallBlind, bigBlind, new StandardHandEvaluator());
    }

    public void join(Player player) {
        if (gameState.getPhase() == GameState.GamePhase.PRE_GAME) {
            if (gameState.getPlayers().size() >= 10) {
                throw new IllegalStateException("Game is full (max 10 players)");
            }
            gameState.addPlayer(player);
        } else {
            // Check total count (current + waiting)
            int total = gameState.getPlayers().size() + waitingPlayers.size();
            if (total >= 10) {
                throw new IllegalStateException("Game is full (max 10 players)");
            }
            if (!gameState.getPlayers().contains(player) && !waitingPlayers.contains(player)) {
                waitingPlayers.add(player);
                System.out.println("Player " + player.getName() + " added to waiting list.");
            }
        }
    }

    public void leave(Player player) {
        if (waitingPlayers.remove(player)) {
            player.onLeave();
            return; // Was only waiting
        }

        if (gameState.getPlayers().contains(player)) {
            if (gameState.getPhase() == GameState.GamePhase.PRE_GAME ||
                    gameState.getPhase() == GameState.GamePhase.HAND_ENDED) {
                gameState.removePlayer(player);
                player.onLeave();
            } else {
                // Active game
                player.setStatus(PlayerStatus.LEFT);
                player.onLeave();

                // If it was their turn, act to unblock
                Player activePlayer = gameState.getPlayers().get(gameState.getCurrentActionPosition());
                if (activePlayer.equals(player)) {
                    performAction(player.getId(), ActionType.FOLD, 0);
                }
                // Actually performAction(FOLD) sets status to FOLDED.
                // We want LEFT to persist so we can remove them later?
                // But performAction sets FOLDED.
                // Let's refine performAction to override validation if we are calling it
                // internally?
                // Or just let them be FOLDED for the rest of the hand, and check status
                // LEFT/FOLDED when removing.
                // But if we set LEFT before calling FOLD, FOLD execution sets FOLDED.
                // So we should call FOLD first, then set LEAVE?
                // If we call FOLD, they become FOLDED.
                // We need a flag or just check if they are in 'leaving' state.
                // Simplest: Check 'leave' calls.
                // Let's trust startHand to remove players who are 'LEFT'.
                // Problem: performAction sets FOLDED.

                // Strategy:
                // 1. If active, FOLD.
                // 2. Set status LEFT (overwriting FOLDED).
            }
        }
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
        if (gameState.getPlayers().size() < 2) {
            throw new IllegalStateException("Not enough players");
        }

        // 0. Process Joins and Leaves
        // Remove LEFT players
        List<Player> toRemove = new ArrayList<>();
        for (Player p : gameState.getPlayers()) {
            if (p.getStatus() == PlayerStatus.LEFT) {
                toRemove.add(p);
            }
        }
        toRemove.forEach(gameState::removePlayer);

        // Add Waiting Players
        while (!waitingPlayers.isEmpty() && gameState.getPlayers().size() < 10) {
            gameState.addPlayer(waitingPlayers.remove(0));
        }

        if (gameState.getPlayers().size() < 2) {
            // Not enough players to start/continue
            // notify?
            gameState.setPhase(GameState.GamePhase.PRE_GAME);
            return; // Logic loop break?
        }

        notifyGameStarted(); // Or notifyHandStarted if tracking per hand

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

        // 3. Post Blinds (Dynamic Rotation)
        int sbPos, bbPos;
        int numPlayers = gameState.getPlayers().size();

        if (numPlayers == 2) {
            // Heads Up: Dealer is SB, Other is BB
            sbPos = gameState.getDealerButtonPosition();
            bbPos = (sbPos + 1) % numPlayers;
        } else {
            // Normal: Dealer -> SB -> BB
            sbPos = (gameState.getDealerButtonPosition() + 1) % numPlayers;
            bbPos = (sbPos + 1) % numPlayers;
        }

        postBlind(gameState.getPlayers().get(sbPos), smallBlind, ActionType.SMALL_BLIND);
        postBlind(gameState.getPlayers().get(bbPos), bigBlind, ActionType.BIG_BLIND);

        notifyRoundStarted("PRE_FLOP");

        // Action starts left of BB (UTG)
        gameState.clearActedPlayers(); // Reset for new round

        if (numPlayers == 2) {
            gameState.setCurrentActionPosition(sbPos);
        } else {
            gameState.setCurrentActionPosition((bbPos + 1) % numPlayers);
        }
    }

    // Core Action method
    private void postBlind(Player p, int amount, ActionType type) {
        int balanceBefore = p.getChips();
        p.bet(amount);
        gameState.getPot().add(p, amount);
        // Blinds do NOT count as "acting" for the purpose of ending the round
        // voluntarily,
        // but they establish the bet.
        // We do NOT add to actedPlayers here because they must get a chance to act
        // again if raised,
        // OR in the case of BB, the "Option".
        notifyPlayerAction(p, type, amount, balanceBefore, p.getChips());
        notifyPotUpdate(gameState.getPot().getTotal());
    }

    // --- Game Play Actions ---

    public void performAction(String playerId, ActionType type, int amount) {
        // 1. Validation
        Player player = gameState.getPlayers().stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));

        Player activePlayer = gameState.getPlayers().get(gameState.getCurrentActionPosition());
        if (!activePlayer.getId().equals(playerId)) {
            throw new IllegalStateException("Not your turn! Waiting for " + activePlayer.getName());
        }

        if (gameState.getPhase() == GameState.GamePhase.SHOWDOWN
                || gameState.getPhase() == GameState.GamePhase.HAND_ENDED) {
            throw new IllegalStateException("Hand is over");
        }

        // 2. Logic & Rules
        int balanceBefore = player.getChips();
        int potTotalBefore = gameState.getPot().getTotal();

        switch (type) {
            case FOLD -> {
                if (player.getStatus() != PlayerStatus.LEFT) {
                    player.setStatus(PlayerStatus.FOLDED);
                }
            }
            case CHECK -> {
                int highestBet = getHighestRoundBet();
                if (player.getCurrentBet() < highestBet) {
                    throw new IllegalArgumentException(
                            "Cannot check, must call " + (highestBet - player.getCurrentBet()));
                }
            }
            case CALL -> {
                int highestBet = getHighestRoundBet();
                int toCall = highestBet - player.getCurrentBet();
                if (toCall > player.getChips()) {
                    // Treat as All-In (Partial Call)
                    amount = player.getChips();
                    type = ActionType.ALL_IN;
                    player.bet(amount);
                    player.setStatus(PlayerStatus.ALL_IN);
                } else {
                    player.bet(toCall);
                    amount = toCall; // For event logging
                }
                gameState.getPot().add(player, amount);
            }
            case RAISE -> {
                int highestBet = getHighestRoundBet();

                // If amount is total bet, we calculate diff.
                // Let's assume input 'amount' is "Total Amount To Bet" (Raise To).
                if (amount < highestBet + bigBlind) {
                    throw new IllegalArgumentException("Raise must be at least " + (highestBet + bigBlind));
                }

                int toAdd = amount - player.getCurrentBet();
                if (toAdd > player.getChips()) {
                    throw new IllegalArgumentException("Not enough chips to raise to " + amount);
                }

                player.bet(toAdd);
                gameState.getPot().add(player, toAdd);
            }
            case ALL_IN -> {
                amount = player.getChips();
                player.bet(amount);
                gameState.getPot().add(player, amount);
                player.setStatus(PlayerStatus.ALL_IN);
            }
            case SMALL_BLIND, BIG_BLIND -> throw new IllegalArgumentException("Blinds are posted automatically");
        }

        // 3. Notify
        notifyPlayerAction(player, type, amount, balanceBefore, player.getChips());
        if (gameState.getPot().getTotal() != potTotalBefore) {
            notifyPotUpdate(gameState.getPot().getTotal());
        }

        // Mark as acted
        gameState.addActedPlayer(player);

        // If raise occurred, we might need to reset acted status for others?
        // Strictly, if someone raises, everyone else must act again.
        // Simplified: We rely on 'allMatched' check. But 'acted' helps ensure everyone
        // got at least ONE chance.
        // Real engine: if raise, clear acted for everyone EXCEPT raiser.
        if (type == ActionType.RAISE || type == ActionType.ALL_IN) { // And All-in is a raise? Not always.
            // If this was a raise (new highest bet), everyone else needs to act again.
            // Implementation: if currentBet > previousHighest, clear properly.
            // For now: The "All Matched" check handles the requirement to call.
            // "Acked" is only to ensure the first loop happens.
        }

        // 4. Move Game Forward
        advanceGame();
    }

    private void advanceGame() {
        if (judgeRoundEnd()) {
            transitionPhase();
        } else {
            moveActionToNextPlayer();
        }
    }

    private void moveActionToNextPlayer() {
        int startPos = gameState.getCurrentActionPosition();
        int nextIdx = (startPos + 1) % gameState.getPlayers().size();

        // Safety: ensure we don't loop forever if everyone is folded (should be caught
        // by judgeRoundEnd)
        int attempts = 0;
        while (gameState.getPlayers().get(nextIdx).getStatus() != PlayerStatus.ACTIVE
                && attempts < gameState.getPlayers().size()) {
            nextIdx = (nextIdx + 1) % gameState.getPlayers().size();
            attempts++;
        }
        gameState.setCurrentActionPosition(nextIdx);
    }

    private boolean judgeRoundEnd() {
        // Round ends when all ACTIVE players have matched the highest bet and acted.
        // We do NOT check "Win by Fold" here; that is handled by transitionPhase or
        // explicit check.
        // Actually, if everyone checks, allMatched=true (0==0).

        int highestBet = getHighestRoundBet();
        boolean allMatched = gameState.getPlayers().stream()
                .filter(p -> p.getStatus() == PlayerStatus.ACTIVE)
                .allMatch(p -> p.getCurrentBet() == highestBet);

        boolean everyoneActed = gameState.getPlayers().stream()
                .filter(p -> p.getStatus() == PlayerStatus.ACTIVE)
                .allMatch(gameState::hasActed);

        return allMatched && everyoneActed;
    }

    private int getHighestRoundBet() {
        return gameState.getPlayers().stream()
                .mapToInt(Player::getCurrentBet)
                .max().orElse(0);
    }

    private void transitionPhase() {
        // Check if only one player left (Win by Fold)
        // Count players who are NOT (Folded OR Left OR Sitting Out) -> Effectively
        // ACTIVE or ALL_IN
        long activeCount = gameState.getPlayers().stream()
                .filter(p -> p.getStatus() == PlayerStatus.ACTIVE || p.getStatus() == PlayerStatus.ALL_IN)
                .count();

        if (activeCount == 1) {
            Player winner = gameState.getPlayers().stream()
                    .filter(p -> p.getStatus() == PlayerStatus.ACTIVE || p.getStatus() == PlayerStatus.ALL_IN)
                    .findFirst().get();
            handleWinByFold(winner);
            return;
        }

        gameState.getPlayers().forEach(Player::resetBet);
        gameState.clearActedPlayers(); // Reset for new street
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
                // Should not happen as startHand resets to PRE_FLOP
                // and handleShowdown sets to HAND_ENDED
            }
            case PRE_GAME -> throw new IllegalStateException("Cannot transition from PRE_GAME without startHand()");
        }
    }

    private void handleWinByFold(Player winner) {
        int pot = gameState.getPot().getTotal();
        winner.win(pot);
        notifyHandEnded(List.of(winner), java.util.Map.of(winner.getId(), pot));
        gameState.setPhase(GameState.GamePhase.HAND_ENDED);
    }

    private void handleShowdown() {
        // Advanced Showdown Logic for Side Pots & Split Pots
        List<Player> showdownPlayers = gameState.getPlayers().stream()
                .filter(p -> p.getStatus() != PlayerStatus.FOLDED && p.getStatus() != PlayerStatus.LEFT
                        && p.getStatus() != PlayerStatus.SITTING_OUT)
                .collect(java.util.stream.Collectors.toList());

        // 2. Evaluate all hands
        record PlayerHand(Player player, HandResult hand) {
        }
        List<PlayerHand> results = new ArrayList<>();
        for (Player p : showdownPlayers) {
            results.add(new PlayerHand(p, handEvaluator.evaluate(p.getHoleCards(), gameState.getBoard())));
        }

        // 3. Group by Strength (Highest first) -> Tiers
        // Map<HandResult, List<PlayerHand>> ? HandResult must implement equals
        // properly.
        // HandResult uses Record equals, which uses List equals. Cards must equal.
        // But if I have Ace High Flush (Cards A,K,Q,J,9), another player has SAME cards
        // (Community).
        // Yes, Record equals works.
        // However, we want to group by *Comparison Value*.
        // Only if compareTo == 0.

        results.sort((a, b) -> b.hand.compareTo(a.hand)); // Winner first

        // Group tied players
        List<List<PlayerHand>> tiers = new ArrayList<>();
        if (!results.isEmpty()) {
            List<PlayerHand> currentTier = new ArrayList<>();
            currentTier.add(results.get(0));
            tiers.add(currentTier);

            for (int i = 1; i < results.size(); i++) {
                PlayerHand current = results.get(i);
                PlayerHand prev = results.get(i - 1);
                if (current.hand.compareTo(prev.hand) == 0) {
                    currentTier.add(current);
                } else {
                    currentTier = new ArrayList<>();
                    currentTier.add(current);
                    tiers.add(currentTier);
                }
            }
        }

        // 4. Distribute Chips
        java.util.Map<String, Integer> payouts = new java.util.HashMap<>();
        Pot pot = gameState.getPot();
        java.util.Map<String, Integer> contributions = pot.getContributions();

        // New Iterative Side Pot Logic (Robust):
        while (true) {
            // Find smallest non-zero contribution
            int minContrib = contributions.values().stream()
                    .mapToInt(Integer::intValue)
                    .filter(v -> v > 0)
                    .min().orElse(0);

            if (minContrib == 0)
                break;

            // Collect this "slice" from everyone
            int potSlice = 0;
            List<String> involvedIds = new ArrayList<>();

            for (java.util.Map.Entry<String, Integer> entry : contributions.entrySet()) {
                if (entry.getValue() > 0) {
                    potSlice += minContrib;
                    contributions.put(entry.getKey(), entry.getValue() - minContrib);
                    involvedIds.add(entry.getKey());
                }
            }

            // Determine winners for this slice
            // Who among 'involvedIds' has the best hand?
            // Use our sorted 'results' to find best match.

            List<Player> sliceWinners = new ArrayList<>();
            // Look through tiers
            for (List<PlayerHand> tier : tiers) {
                // Check if any player in this tier is involved
                List<Player> winnersInTier = tier.stream()
                        .map(ph -> ph.player)
                        .filter(p -> involvedIds.contains(p.getId()))
                        .collect(java.util.stream.Collectors.toList());

                if (!winnersInTier.isEmpty()) {
                    sliceWinners.addAll(winnersInTier);
                    break; // Found the best hands for this slice
                }
            }

            if (!sliceWinners.isEmpty()) {
                int share = potSlice / sliceWinners.size();
                int remainder = potSlice % sliceWinners.size();

                for (Player winner : sliceWinners) {
                    int amount = share;
                    if (remainder > 0) { // Distribute odd chips 1 by 1?
                        // Ideally by position (left of dealer). Simplified: First in list.
                        amount++;
                        remainder--;
                    }
                    winner.win(amount);
                    payouts.merge(winner.getId(), amount, Integer::sum);
                }
            } else {
                // No winners? Should not happen if involvedIds exist (unless they folded? but
                // showdownPlayers filters folds)
                // If involved but folded, they can't win.
                // But involvedIds implies they put money in.
                // We only check showdownPlayers. If player folded, they are not in 'results'.
                // So expected behavior: Folded players money goes to sliceWinners.
            }
        }

        // Notify
        List<Player> winners = results.stream()
                .filter(ph -> payouts.containsKey(ph.player.getId()))
                .map(ph -> ph.player)
                .distinct()
                .toList();

        notifyHandEnded(winners, payouts);
        gameState.setPhase(GameState.GamePhase.HAND_ENDED);
    }

    private void checkAutoAdvance() {
        long activeCount = gameState.getPlayers().stream()
                .filter(p -> p.getStatus() == PlayerStatus.ACTIVE)
                .count();
        if (activeCount <= 1) {
            transitionPhase();
        }
    }

    private void notifyHandEnded(List<Player> winners, java.util.Map<String, Integer> payouts) {
        listeners.forEach(l -> l.onHandEnded(winners, payouts));
    }

    // --- Notifiers ---

    private void notifyGameStarted() {
        listeners.forEach(GameEventListener::onGameStarted);
    }

    private void notifyRoundStarted(String round) {
        listeners.forEach(l -> l.onRoundStarted(round));
    }

    private void notifyPlayerAction(Player p, ActionType type, int amount, int before, int after) {
        listeners.forEach(l -> l.onPlayerAction(p, type, amount, before, after));
    }

    private void notifyPotUpdate(int total) {
        listeners.forEach(l -> l.onPotUpdate(total));
    }
}
