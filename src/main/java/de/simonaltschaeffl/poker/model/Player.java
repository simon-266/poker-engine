package de.simonaltschaeffl.poker.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public abstract class Player {
    @NotBlank
    private final String id;
    @NotBlank
    private final String name;
    @Min(0)
    private int chips;
    @NotNull
    private final List<Card> holeCards;
    @NotNull
    private PlayerStatus status;
    @Min(0)
    private int currentBet;

    public Player(String id, String name, int chips) {
        this.id = id;
        this.name = name;
        this.chips = chips;
        this.holeCards = new ArrayList<>();
        this.status = PlayerStatus.ACTIVE;
        this.currentBet = 0;
    }

    public void addHoleCard(Card card) {
        if (holeCards.size() >= 2) {
            throw new IllegalStateException("Player already has 2 hole cards");
        }
        holeCards.add(card);
    }

    public void clearHoleCards() {
        holeCards.clear();
    }

    public List<Card> getHoleCards() {
        return new ArrayList<>(holeCards);
    }

    public void bet(int amount) {
        if (amount > chips) {
            throw new IllegalArgumentException("Not enough chips");
        }
        chips -= amount;
        currentBet += amount;
    }

    public void win(int amount) {
        chips += amount;
    }

    public void resetBet() {
        currentBet = 0;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getChips() {
        return chips;
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public void setStatus(PlayerStatus status) {
        this.status = status;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    // Abstract Lifecycle Hooks
    public abstract void onLeave();

    public abstract void onHandEnded(Map<String, Integer> payouts);
}
