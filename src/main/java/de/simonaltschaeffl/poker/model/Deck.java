package de.simonaltschaeffl.poker.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.validation.constraints.NotNull;

public class Deck {
    @NotNull
    private final List<Card> cards;

    public Deck() {
        this.cards = new ArrayList<>();
        reset();
    }

    public void reset() {
        cards.clear();
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                cards.add(new Card(rank, suit));
            }
        }
        shuffle();
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public Optional<Card> deal() {
        if (cards.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(cards.remove(cards.size() - 1));
    }

    public int remainingCards() {
        return cards.size();
    }
}
