package de.simonaltschaeffl.poker.model;

import java.util.List;

public record HandResult(HandRank rank, List<Card> bestFive) implements Comparable<HandResult> {
    @Override
    public int compareTo(HandResult other) {
        int rankComp = this.rank.compareTo(other.rank); // Enums are ordered from HIGH_CARD to ROYAL.

        if (rankComp != 0)
            return rankComp;

        // Compare kickers dynamically by grouping cards by frequency
        java.util.Map<Integer, Integer> freq1 = new java.util.HashMap<>();
        for (Card c : this.bestFive)
            freq1.merge(c.rank().getValue(), 1, Integer::sum);
        List<Card> sorted1 = new java.util.ArrayList<>(this.bestFive);
        sorted1.sort((c1, c2) -> {
            int f1 = freq1.get(c1.rank().getValue());
            int f2 = freq1.get(c2.rank().getValue());
            if (f1 != f2)
                return Integer.compare(f2, f1);
            return Integer.compare(c2.rank().getValue(), c1.rank().getValue());
        });

        java.util.Map<Integer, Integer> freq2 = new java.util.HashMap<>();
        for (Card c : other.bestFive)
            freq2.merge(c.rank().getValue(), 1, Integer::sum);
        List<Card> sorted2 = new java.util.ArrayList<>(other.bestFive);
        sorted2.sort((c1, c2) -> {
            int f1 = freq2.get(c1.rank().getValue());
            int f2 = freq2.get(c2.rank().getValue());
            if (f1 != f2)
                return Integer.compare(f2, f1);
            return Integer.compare(c2.rank().getValue(), c1.rank().getValue());
        });

        for (int i = 0; i < Math.min(sorted1.size(), sorted2.size()); i++) {
            int c1 = sorted1.get(i).rank().getValue();
            int c2 = sorted2.get(i).rank().getValue();
            if (c1 != c2)
                return Integer.compare(c1, c2);
        }
        return 0;
    }
}
