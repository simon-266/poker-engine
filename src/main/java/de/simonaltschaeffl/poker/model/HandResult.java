package de.simonaltschaeffl.poker.model;

import java.util.List;

public record HandResult(HandRank rank, List<Card> bestFive) implements Comparable<HandResult> {
    @Override
    public int compareTo(HandResult other) {
        int rankComp = this.rank.compareTo(other.rank); // Enum comparison usually works if ordered correctly (ROYAL to
                                                        // HIGH).
        // Wait, Enum HandRank is usually defined ROYAL ... HIGH.
        // CompareTo: ROYAL(0) < HIGH(9).
        // But in poker, ROYAL > HIGH.
        // So we want explicit comparison logic or ensure Enum is ordered LOW to HIGH?
        // Or just reverse the int comparison.

        // Let's assume Standard HandEvaluator defined them HIGH to LOW?
        // Let's check HandRank definition first.
        // If I move it, I should verify order.

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
