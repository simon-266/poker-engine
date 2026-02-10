package de.simonaltschaeffl.poker.dto;

import de.simonaltschaeffl.poker.model.Card;
import jakarta.validation.constraints.NotNull;

public record CardDTO(@NotNull Card.Suit suit, @NotNull Card.Rank rank) {
    public static CardDTO from(Card card) {
        return new CardDTO(card.suit(), card.rank());
    }
}
