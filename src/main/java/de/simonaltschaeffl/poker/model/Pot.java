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

    public Map<String, Integer> getContributions() {
        return new HashMap<>(playerContributions);
    }

    public void reset() {
        this.total = 0;
        this.playerContributions.clear();
    }
}
