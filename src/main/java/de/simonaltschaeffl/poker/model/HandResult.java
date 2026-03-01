package de.simonaltschaeffl.poker.model;

import java.util.List;

public record HandResult(HandRank rank, List<Card> bestFive) implements Comparable<HandResult> {
    @Override
    public int compareTo(HandResult other) {
        int rankComp = this.rank.compareTo(other.rank); // Enums are ordered from ROYAL to HIGH_CARD.

        if (rankComp != 0)
            return rankComp; // This assumes consistency with previous logic

        // Compare kickers
        for (int i = 0; i < Math.min(this.bestFive.size(), other.bestFive.size()); i++) {
            int c1 = this.bestFive.get(i).rank().getValue();
            int c2 = other.bestFive.get(i).rank().getValue();
            if (c1 != c2)
                return Integer.compare(c1, c2);
        }
        return 0;
    }
}
