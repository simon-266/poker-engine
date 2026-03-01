package de.simonaltschaeffl.poker.service;

import de.simonaltschaeffl.poker.model.Card;
import de.simonaltschaeffl.poker.model.HandResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandardHandEvaluatorTest {

    private final StandardHandEvaluator evaluator = new StandardHandEvaluator();

    static Stream<Arguments> provideHandsForTieBreak() {
        return Stream.of(
                Arguments.of(
                        // Hand 1: pair of Aces with K, Q, J
                        List.of(
                                new Card(Card.Rank.ACE, Card.Suit.CLUBS), new Card(Card.Rank.ACE, Card.Suit.DIAMONDS),
                                new Card(Card.Rank.KING, Card.Suit.HEARTS), new Card(Card.Rank.QUEEN, Card.Suit.SPADES),
                                new Card(Card.Rank.JACK, Card.Suit.CLUBS)),
                        // Hand 2: pair of Aces with K, Q, T
                        List.of(
                                new Card(Card.Rank.ACE, Card.Suit.HEARTS), new Card(Card.Rank.ACE, Card.Suit.SPADES),
                                new Card(Card.Rank.KING, Card.Suit.DIAMONDS),
                                new Card(Card.Rank.QUEEN, Card.Suit.CLUBS),
                                new Card(Card.Rank.TEN, Card.Suit.HEARTS)),
                        // Expected result: Hand 1 > Hand 2
                        1),
                Arguments.of(
                        // Hand 1: pair of Aces with K, Q, J
                        List.of(
                                new Card(Card.Rank.ACE, Card.Suit.CLUBS), new Card(Card.Rank.ACE, Card.Suit.DIAMONDS),
                                new Card(Card.Rank.KING, Card.Suit.HEARTS), new Card(Card.Rank.QUEEN, Card.Suit.SPADES),
                                new Card(Card.Rank.JACK, Card.Suit.CLUBS)),
                        // Hand 2: exactly same ranks (different suits shouldn't matter for Split Pot)
                        List.of(
                                new Card(Card.Rank.ACE, Card.Suit.HEARTS), new Card(Card.Rank.ACE, Card.Suit.SPADES),
                                new Card(Card.Rank.KING, Card.Suit.CLUBS),
                                new Card(Card.Rank.QUEEN, Card.Suit.DIAMONDS),
                                new Card(Card.Rank.JACK, Card.Suit.SPADES)),
                        // Expected result: Tie
                        0));
    }

    @ParameterizedTest
    @MethodSource("provideHandsForTieBreak")
    void testKickerTieBreaks(List<Card> holeAndCommunity1, List<Card> holeAndCommunity2, int expectedComparison) {
        HandResult result1 = evaluator.evaluate(holeAndCommunity1.subList(0, 2), holeAndCommunity1.subList(2, 5));
        HandResult result2 = evaluator.evaluate(holeAndCommunity2.subList(0, 2), holeAndCommunity2.subList(2, 5));

        int comparison = result1.compareTo(result2);

        if (expectedComparison > 0) {
            assertTrue(comparison > 0, "Hand 1 should win");
        } else if (expectedComparison < 0) {
            assertTrue(comparison < 0, "Hand 2 should win");
        } else {
            assertEquals(0, comparison, "Should be a split pot");
        }
    }
}
