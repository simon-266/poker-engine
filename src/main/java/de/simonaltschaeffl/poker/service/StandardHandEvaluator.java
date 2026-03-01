package de.simonaltschaeffl.poker.service;

import de.simonaltschaeffl.poker.model.Card;
import de.simonaltschaeffl.poker.model.HandRank;
import de.simonaltschaeffl.poker.model.HandResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class StandardHandEvaluator implements HandEvaluator {

    public HandResult evaluate(List<Card> holeCards, List<Card> communityCards) {
        List<Card> allCards = new ArrayList<>();
        allCards.addAll(holeCards);
        allCards.addAll(communityCards);

        if (allCards.size() < 5) {
            throw new IllegalArgumentException("Need at least 5 cards to evaluate");
        }

        // Sort descending (Ace to 2) - Critical for logic
        Collections.sort(allCards, Collections.reverseOrder());

        // Check Flush
        List<Card> flushCards = getFlushCards(allCards);

        // Check Straight
        List<Card> straightCards = getStraightCards(allCards);

        // Check Straight Flush (including Royal)
        if (flushCards != null) {
            // Re-check straight only on flush cards
            List<Card> straightFlushCards = getStraightCards(flushCards);
            if (straightFlushCards != null) {
                // Check Royal
                if (straightFlushCards.get(0).rank() == Card.Rank.ACE
                        && straightFlushCards.get(4).rank() == Card.Rank.TEN) {
                    return new HandResult(HandRank.ROYAL_FLUSH, straightFlushCards);
                }
                return new HandResult(HandRank.STRAIGHT_FLUSH, straightFlushCards);
            }
        }

        // Four of a Kind
        var groups = groupByRank(allCards);
        var quads = findGroup(groups, 4);
        if (quads != null) {
            List<Card> bestFive = new ArrayList<>(quads);
            fillKickers(bestFive, allCards, 1);
            return new HandResult(HandRank.FOUR_OF_A_KIND, bestFive);
        }

        // Full House
        var trips = findGroup(groups, 3);
        if (trips != null) {
            // Find pair (or another trips turned pair)
            // Remove used trips from consideration for pair
            List<Card> remaining = new ArrayList<>(allCards);
            remaining.removeAll(trips);
            var remainingGroups = groupByRank(remaining);
            var pair = findGroup(remainingGroups, 2); // Priority to pair (>=2 actually, could be another trips)

            // If we have two 3-of-a-kinds, the lower one becomes the pair part of full
            // house.
            // The removeAll logic handles this implicitly due to descending sort order.

            // Special case: 2 sets of trips.
            if (pair == null) {
                pair = findGroup(remainingGroups, 3); // Another trips treated as pair
                if (pair != null) {
                    pair = pair.subList(0, 2);
                }
            }

            if (pair != null) {
                List<Card> bestFive = new ArrayList<>(trips);
                bestFive.addAll(pair);
                return new HandResult(HandRank.FULL_HOUSE, bestFive);
            }
        }

        if (flushCards != null) {
            return new HandResult(HandRank.FLUSH, flushCards.subList(0, 5));
        }

        if (straightCards != null) {
            return new HandResult(HandRank.STRAIGHT, straightCards);
        }

        if (trips != null) {
            List<Card> bestFive = new ArrayList<>(trips);
            fillKickers(bestFive, allCards, 2);
            return new HandResult(HandRank.THREE_OF_A_KIND, bestFive);
        }

        // Two Pair
        var pair1 = findGroup(groups, 2);
        if (pair1 != null) {
            List<Card> remaining = new ArrayList<>(allCards);
            remaining.removeAll(pair1);
            var groups2 = groupByRank(remaining);
            var pair2 = findGroup(groups2, 2);

            if (pair2 != null) {
                List<Card> bestFive = new ArrayList<>(pair1);
                bestFive.addAll(pair2);
                fillKickers(bestFive, allCards, 1);
                return new HandResult(HandRank.TWO_PAIR, bestFive);
            }

            // One Pair
            List<Card> bestFive = new ArrayList<>(pair1);
            fillKickers(bestFive, allCards, 3);
            return new HandResult(HandRank.ONE_PAIR, bestFive);
        }

        // High Card
        return new HandResult(HandRank.HIGH_CARD, allCards.subList(0, 5));
    }

    // --- Helpers ---

    private Map<Card.Rank, List<Card>> groupByRank(List<Card> cards) {
        Map<Card.Rank, List<Card>> map = new java.util.TreeMap<>(Collections.reverseOrder()); // Highest rank first
        for (Card c : cards) {
            map.computeIfAbsent(c.rank(), k -> new ArrayList<>()).add(c);
        }
        return map;
    }

    // Find FIRST group of size >= N (Since map is sorted desc, this is the highest
    // rank group)
    private List<Card> findGroup(Map<Card.Rank, List<Card>> groups, int size) {
        for (List<Card> group : groups.values()) {
            if (group.size() >= size) {
                return group;
            }
        }
        return null;
    }

    private void fillKickers(List<Card> currentHand, List<Card> allCards, int count) {
        int added = 0;
        for (Card c : allCards) {
            if (added >= count)
                break;
            if (!currentHand.contains(c)) {
                currentHand.add(c);
                added++;
            }
        }
    }

    private List<Card> getFlushCards(List<Card> cards) {
        Map<Card.Suit, List<Card>> suits = new java.util.EnumMap<>(Card.Suit.class);
        for (Card c : cards) {
            suits.computeIfAbsent(c.suit(), k -> new ArrayList<>()).add(c);
        }
        for (List<Card> suitCards : suits.values()) {
            if (suitCards.size() >= 5) {
                // Already sorted desc
                return suitCards;
            }
        }
        return null;
    }

    private List<Card> getStraightCards(List<Card> sortedCards) {
        // Remove duplicates for straight logic
        List<Card> uniqueRank = new ArrayList<>();
        if (sortedCards.isEmpty())
            return null;
        uniqueRank.add(sortedCards.get(0));
        for (int i = 1; i < sortedCards.size(); i++) {
            if (sortedCards.get(i).rank() != uniqueRank.get(uniqueRank.size() - 1).rank()) {
                uniqueRank.add(sortedCards.get(i));
            }
        }

        if (uniqueRank.size() < 5)
            return null;

        // Normal Straight
        for (int i = 0; i <= uniqueRank.size() - 5; i++) {
            if (isSequential(uniqueRank.subList(i, i + 5))) {
                return uniqueRank.subList(i, i + 5);
            }
        }

        // Wheel (Ace-5-4-3-2)
        // Since sorted Desc, Ace is at 0. We need to check if we have A, 5, 4, 3, 2.
        if (uniqueRank.get(0).rank() == Card.Rank.ACE) {
            // Look for 5,4,3,2
            List<Card> wheel = new ArrayList<>();
            wheel.add(uniqueRank.get(0)); // Ace

            // Check for 5-4-3-2
            boolean has5 = false, has4 = false, has3 = false, has2 = false;
            Card c5 = null, c4 = null, c3 = null, c2 = null;

            for (Card c : uniqueRank) {
                if (c.rank() == Card.Rank.FIVE) {
                    has5 = true;
                    c5 = c;
                }
                if (c.rank() == Card.Rank.FOUR) {
                    has4 = true;
                    c4 = c;
                }
                if (c.rank() == Card.Rank.THREE) {
                    has3 = true;
                    c3 = c;
                }
                if (c.rank() == Card.Rank.TWO) {
                    has2 = true;
                    c2 = c;
                }
            }

            if (has5 && has4 && has3 && has2) {
                // Assemble the wheel straight in the order 5-4-3-2-A.
                // This ensures correct index 0 evaluation for tie-breaking.
                return List.of(c5, c4, c3, c2, uniqueRank.get(0));
            }
        }

        return null;
    }

    private boolean isSequential(List<Card> fiveCards) {
        int first = fiveCards.get(0).rank().getValue();
        for (int i = 1; i < 5; i++) {
            if (fiveCards.get(i).rank().getValue() != first - i)
                return false;
        }
        return true;
    }
}
