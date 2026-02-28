package de.simonaltschaeffl.poker.model;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class Pot {
    @Min(0)
    private int total;
    @NotNull
    private final Map<String, Integer> playerContributions;

    public Pot() {
        this.total = 0;
        this.playerContributions = new HashMap<>();
    }

    public void add(Player player, int amount) {
        this.total += amount;
        playerContributions.merge(player.getId(), amount, Integer::sum);
    }

    public int getTotal() {
        return total;
    }

    public int getPlayerContribution(String playerId) {
        return playerContributions.getOrDefault(playerId, 0);
    }

    public void deductRake(int amount) {
        if (amount > total) {
            throw new IllegalArgumentException("Cannot deduct rake larger than pot total");
        }
        total -= amount;

        // We technically don't strictly *have* to proportionally reduce
        // playerContributions
        // for the side-pot math because payout slices are done by sorting
        // contributions.
        // However, reducing them proportionally is more accurate for accounting.
        // For simplicity right now, since the payout logic uses absolute slice amounts,
        // the payout loop just distributes what's left. We don't touch contributions.
    }

    public Map<String, Integer> getContributions() {
        return new HashMap<>(playerContributions);
    }

    public void reset() {
        this.total = 0;
        this.playerContributions.clear();
    }
}
