package de.simonaltschaeffl.poker.service;

import de.simonaltschaeffl.poker.model.Card;
import de.simonaltschaeffl.poker.model.HandResult;
import de.simonaltschaeffl.poker.model.Player;
import de.simonaltschaeffl.poker.model.PlayerStatus;
import de.simonaltschaeffl.poker.model.Pot;

import de.simonaltschaeffl.poker.api.GameEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Calculates the payouts for each player at the end of a round (showdown).
 * It evaluates hand strengths and distributes the pot correctly,
 * including logic for side pots when players go all-in with different amounts.
 */
public class PayoutCalculator {

    private final HandEvaluator handEvaluator;
    private final RakeStrategy rakeStrategy;
    private final List<GameEventListener> listeners;

    /**
     * Constructs a PayoutCalculator with the given HandEvaluator and RakeStrategy.
     *
     * @param handEvaluator the evaluator used to determine the strength of players'
     *                      hands
     * @param rakeStrategy  the strategy for deducting house commission
     * @param listeners     list of game event listeners
     */
    public PayoutCalculator(HandEvaluator handEvaluator, RakeStrategy rakeStrategy, List<GameEventListener> listeners) {
        this.handEvaluator = handEvaluator;
        this.rakeStrategy = rakeStrategy;
        this.listeners = listeners;
    }

    // Constructor for backward compatibility in tests
    public PayoutCalculator(HandEvaluator handEvaluator) {
        this(handEvaluator, new NoRakeStrategy(), new ArrayList<>());
    }

    public record ShowdownResult(List<Player> winners, Map<String, Integer> payouts) {
    }

    /**
     * Calculates the winners and their respective payouts given the players, the
     * board, and the pot.
     *
     * @param allPlayers the list of all players in the game (including active and
     *                   folded)
     * @param board      the community cards on the table
     * @param pot        the current pot with the contributions by each player
     * @return a ShowdownResult containing the winning players and their payout
     *         amounts
     */
    public ShowdownResult calculate(List<Player> allPlayers, List<Card> board, Pot pot) {
        // --- 0. Collect Rake ---
        int totalPotSize = pot.getTotal();
        int rakeAmount = rakeStrategy.calculateRake(totalPotSize);
        if (rakeAmount > 0) {
            pot.deductRake(rakeAmount); // We will need to add this method to Pot
            for (GameEventListener listener : listeners) {
                listener.onRakeCollected(rakeAmount);
            }
        }

        // 1. Filter Showdown Players
        List<Player> showdownPlayers = allPlayers.stream()
                .filter(p -> p.getStatus() != PlayerStatus.FOLDED && p.getStatus() != PlayerStatus.LEFT
                        && p.getStatus() != PlayerStatus.SITTING_OUT)
                .collect(Collectors.toList());

        // 2. Evaluate all hands
        record PlayerHand(Player player, HandResult hand) {
        }
        // PERFORMANCE-FIX: Parallele Stream-Evaluierung für CPU-intensive CactusKev
        // Evaluation
        List<PlayerHand> results = showdownPlayers.parallelStream()
                .map(p -> new PlayerHand(p, handEvaluator.evaluate(p.getHoleCards(), board)))
                .collect(Collectors.toCollection(ArrayList::new));

        // 3. Group by Strength (Highest first) -> Tiers
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
        Map<String, Integer> payouts = new HashMap<>();
        Map<String, Integer> contributions = pot.getContributions();

        // PERFORMANCE-FIX: Extrahiere Einsätze, aufsteigend sortieren für linearen
        // Durchlauf (O(N) statt iterativer Minimum-Suche O(N^2))
        List<Integer> uniqueAmounts = contributions.values().stream()
                .filter(v -> v > 0)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        int previouslyDeducted = 0;
        for (int amount : uniqueAmounts) {
            int sliceAmount = amount - previouslyDeducted;
            if (sliceAmount <= 0)
                continue;

            int potSlice = 0;
            List<String> involvedIds = new ArrayList<>();

            for (Map.Entry<String, Integer> entry : contributions.entrySet()) {
                if (entry.getValue() >= amount) {
                    potSlice += sliceAmount;
                    involvedIds.add(entry.getKey());
                }
            }

            previouslyDeducted = amount;

            // Determine winners for this slice
            List<Player> sliceWinners = new ArrayList<>();
            for (List<PlayerHand> tier : tiers) {
                List<Player> winnersInTier = new ArrayList<>();
                for (PlayerHand ph : tier) {
                    if (involvedIds.contains(ph.player.getId())) {
                        winnersInTier.add(ph.player);
                    }
                }
                if (!winnersInTier.isEmpty()) {
                    sliceWinners.addAll(winnersInTier);
                    break;
                }
            }

            if (!sliceWinners.isEmpty()) {
                int share = potSlice / sliceWinners.size();
                int remainder = potSlice % sliceWinners.size();

                for (Player winner : sliceWinners) {
                    int winAmount = share + (remainder > 0 ? 1 : 0);
                    if (remainder > 0)
                        remainder--;
                    payouts.merge(winner.getId(), winAmount, Integer::sum);
                }
            }
        }

        // 5. Build Result
        List<Player> winners = results.stream()
                .filter(ph -> payouts.containsKey(ph.player.getId()))
                .map(ph -> ph.player)
                .distinct()
                .toList();

        return new ShowdownResult(winners, payouts);
    }
}
