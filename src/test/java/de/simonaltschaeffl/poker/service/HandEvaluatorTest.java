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

class HandEvaluatorTest {

    static Stream<Arguments> provideHandsForTieBreak() {
        List<Arguments> cases = List.of(
                Arguments.of(
                        // Hand 1: pair of Aces with K, Q, J, 2, 3
                        List.of(
                                new Card(Card.Rank.ACE, Card.Suit.CLUBS), new Card(Card.Rank.ACE, Card.Suit.DIAMONDS),
                                new Card(Card.Rank.KING, Card.Suit.HEARTS), new Card(Card.Rank.QUEEN, Card.Suit.SPADES),
                                new Card(Card.Rank.JACK, Card.Suit.CLUBS),
                                new Card(Card.Rank.TWO, Card.Suit.SPADES), new Card(Card.Rank.THREE, Card.Suit.HEARTS)),
                        // Hand 2: pair of Aces with K, Q, T, 2, 3
                        List.of(
                                new Card(Card.Rank.ACE, Card.Suit.HEARTS), new Card(Card.Rank.ACE, Card.Suit.SPADES),
                                new Card(Card.Rank.KING, Card.Suit.DIAMONDS),
                                new Card(Card.Rank.QUEEN, Card.Suit.CLUBS),
                                new Card(Card.Rank.TEN, Card.Suit.HEARTS),
                                new Card(Card.Rank.TWO, Card.Suit.SPADES), new Card(Card.Rank.THREE, Card.Suit.HEARTS)),
                        // Expected result: Hand 1 > Hand 2
                        1),
                Arguments.of(
                        // Hand 1: pair of Aces with K, Q, J, 2, 3
                        List.of(
                                new Card(Card.Rank.ACE, Card.Suit.CLUBS), new Card(Card.Rank.ACE, Card.Suit.DIAMONDS),
                                new Card(Card.Rank.KING, Card.Suit.HEARTS), new Card(Card.Rank.QUEEN, Card.Suit.SPADES),
                                new Card(Card.Rank.JACK, Card.Suit.CLUBS),
                                new Card(Card.Rank.TWO, Card.Suit.SPADES), new Card(Card.Rank.THREE, Card.Suit.HEARTS)),
                        // Hand 2: exactly same ranks (different suits shouldn't matter for Split Pot)
                        List.of(
                                new Card(Card.Rank.ACE, Card.Suit.HEARTS), new Card(Card.Rank.ACE, Card.Suit.SPADES),
                                new Card(Card.Rank.KING, Card.Suit.CLUBS),
                                new Card(Card.Rank.QUEEN, Card.Suit.DIAMONDS),
                                new Card(Card.Rank.JACK, Card.Suit.SPADES),
                                new Card(Card.Rank.TWO, Card.Suit.SPADES), new Card(Card.Rank.THREE, Card.Suit.HEARTS)),
                        // Expected result: Tie
                        0));

        return Stream.of(new StandardHandEvaluator(), new CactusKevEvaluator())
                .flatMap(evaluator -> cases.stream()
                        .map(c -> Arguments.of(evaluator, c.get()[0], c.get()[1], c.get()[2])));
    }

    @ParameterizedTest
    @MethodSource("provideHandsForTieBreak")
    void testKickerTieBreaks(HandEvaluator evaluator, List<Card> holeAndCommunity1, List<Card> holeAndCommunity2,
            int expectedComparison) {
        HandResult result1 = evaluator.evaluate(holeAndCommunity1.subList(0, 2), holeAndCommunity1.subList(2, 7));
        HandResult result2 = evaluator.evaluate(holeAndCommunity2.subList(0, 2), holeAndCommunity2.subList(2, 7));

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
