package de.simonaltschaeffl.poker.service;

import de.simonaltschaeffl.poker.model.Card;
import de.simonaltschaeffl.poker.model.Card.Rank;
import de.simonaltschaeffl.poker.model.Card.Suit;

public class CactusKevCommon {

    // Primes corresponding to 2, 3, 4, 5, 6, 7, 8, 9, T, J, Q, K, A
    public static final int[] PRIMES = { 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41 };

    public static int toInt(Card card) {
        // Standard Cactus Kev card integer format:
        // +--------+--------+--------+--------+
        // |xxxbbbbb|bbbbbbbb|cdhsrrrr|xxpppppp|
        // +--------+--------+--------+--------+
        // p = prime number of rank (deuce=2, trey=3... ace=41)
        // r = rank of card (deuce=0, trey=1... ace=12)
        // cdhs = suit bit set
        // b = bit set of rank

        int r = getRankIndex(card.rank());
        int p = PRIMES[r];
        int s = getSuitMask(card.suit());
        int b = 1 << (16 + r);

        return b | (s << 12) | (r << 8) | p;
    }

    private static int getRankIndex(Rank rank) {
        // Ordinal is likely correct if enum is ordered 2..A.
        // My enum: TWO(2), ... ACE(14).
        // Index needs to be 0..12
        return rank.getValue() - 2;
    }

    private static int getSuitMask(Suit suit) {
        // 8=Spades, 4=Hearts, 2=Diamonds, 1=Clubs (Arbitrary, just needs to be
        // distinct)
        return switch (suit) {
            case SPADES -> 0x8;
            case HEARTS -> 0x4;
            case DIAMONDS -> 0x2;
            case CLUBS -> 0x1;
        };
    }
}
