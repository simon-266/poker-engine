package de.simonaltschaeffl.poker.service;

import de.simonaltschaeffl.poker.model.Card;
import de.simonaltschaeffl.poker.model.HandResult;
import de.simonaltschaeffl.poker.model.Player;
import de.simonaltschaeffl.poker.model.PlayerStatus;
import de.simonaltschaeffl.poker.model.Pot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PayoutCalculator {

    private final HandEvaluator handEvaluator;

    public PayoutCalculator(HandEvaluator handEvaluator) {
        this.handEvaluator = handEvaluator;
    }

    public record ShowdownResult(List<Player> winners, Map<String, Integer> payouts) {
    }

    public ShowdownResult calculate(List<Player> allPlayers, List<Card> board, Pot pot) {
        // 1. Filter Showdown Players
        List<Player> showdownPlayers = allPlayers.stream()
                .filter(p -> p.getStatus() != PlayerStatus.FOLDED && p.getStatus() != PlayerStatus.LEFT
                        && p.getStatus() != PlayerStatus.SITTING_OUT)
                .collect(Collectors.toList());

        // 2. Evaluate all hands
        record PlayerHand(Player player, HandResult hand) {
        }
        List<PlayerHand> results = new ArrayList<>();
        for (Player p : showdownPlayers) {
            results.add(new PlayerHand(p, handEvaluator.evaluate(p.getHoleCards(), board)));
        }

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
        Map<String, Integer> contributions = pot.getContributions(); // This returns a copy

        // Iterative Side Pot Logic
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

            for (Map.Entry<String, Integer> entry : contributions.entrySet()) {
                if (entry.getValue() > 0) {
                    potSlice += minContrib;
                    contributions.put(entry.getKey(), entry.getValue() - minContrib);
                    involvedIds.add(entry.getKey());
                }
            }

            // Determine winners for this slice
            List<Player> sliceWinners = new ArrayList<>();
            // Look through tiers
            for (List<PlayerHand> tier : tiers) {
                // Check if any player in this tier is involved
                List<Player> winnersInTier = tier.stream()
                        .map(ph -> ph.player)
                        .filter(p -> involvedIds.contains(p.getId()))
                        .collect(Collectors.toList());

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
                    if (remainder > 0) {
                        amount++;
                        remainder--;
                    }
                    payouts.merge(winner.getId(), amount, Integer::sum);
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
