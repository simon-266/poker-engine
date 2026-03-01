package de.simonaltschaeffl.poker;

import de.simonaltschaeffl.poker.engine.PokerGame;
import de.simonaltschaeffl.poker.engine.PokerGameConfiguration;
import de.simonaltschaeffl.poker.model.*;
import de.simonaltschaeffl.poker.service.CactusKevEvaluator;
import de.simonaltschaeffl.poker.service.StandardHandEvaluator;
import de.simonaltschaeffl.poker.exception.InvalidActionException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

public class PokerEngineTest {

    // --- Helper Classes ---

    static class TestPlayer extends Player {
        public TestPlayer(String id, String name, int chips) {
            super(id, name, chips);
        }

        @Override
        public void onLeave() {
        }

        @Override
        public void onHandEnded(Map<String, Integer> payouts) {
        }
    }

    static class StackedDeck extends Deck {
        private final LinkedList<Card> stashedCards = new LinkedList<>();

        @Override
        public void reset() {
            // Retain stashed cards, just perform standard reset logic if necessary.
            super.reset();
        }

        // Push to TOP of deck (next to be dealt)
        public void push(Card c) {
            stashedCards.addFirst(c);
        }

        public void push(Card.Rank r, Card.Suit s) {
            push(new Card(r, s));
        }

        @Override
        public Optional<Card> deal() {
            if (!stashedCards.isEmpty()) {
                return Optional.of(stashedCards.removeFirst());
            }
            return super.deal(); // Fallback to random if stack empty
        }

        public void clearStash() {
            stashedCards.clear();
        }
    }

    // --- Tests ---

    @Test
    public void testStandardGameFlow() {
        System.out.println("TEST START: testStandardGameFlow");
        StackedDeck deck = new StackedDeck();
        PokerGameConfiguration config = new PokerGameConfiguration.Builder().smallBlind(10).bigBlind(20).build();
        PokerGame game = new PokerGame(config, new StandardHandEvaluator(), deck);
        System.out.println("Setup: Blinds 10/20. Players P1, P2.");

        TestPlayer p1 = new TestPlayer("p1", "Alice", 1000);
        TestPlayer p2 = new TestPlayer("p2", "Bob", 1000);

        game.join(p1);
        game.join(p2);

        // Stack deck for predictable outcome.
        // The StackedDeck operates on a LIFO basis for dealing.
        // Cards are pushed in reverse order of how they will be dealt.

        // P1 (Dealer/SB) and P2 (BB)
        // Deal order: P1, P2, P1, P2

        // Push board cards in reverse order (River to Flop):
        // River: 2h
        deck.push(Card.Rank.TWO, Card.Suit.HEARTS);
        // Turn: 3h
        deck.push(Card.Rank.THREE, Card.Suit.HEARTS);
        // Flop: 4h, 5h, 6h
        deck.push(Card.Rank.SIX, Card.Suit.HEARTS);
        deck.push(Card.Rank.FIVE, Card.Suit.HEARTS);
        deck.push(Card.Rank.FOUR, Card.Suit.HEARTS);

        // Hole Cards:
        // P2 (BB) Second Card: 7c
        deck.push(Card.Rank.SEVEN, Card.Suit.CLUBS);
        // P1 (SB) Second Card: As
        deck.push(Card.Rank.ACE, Card.Suit.SPADES);
        // P2 (BB) First Card: 2c
        deck.push(Card.Rank.TWO, Card.Suit.CLUBS);
        // P1 (SB) First Card: Ks
        deck.push(Card.Rank.KING, Card.Suit.SPADES);

        game.startHand();

        // Pre-flop
        // P1 (SB) posted 10. P2 (BB) posted 20.
        // Action on P1.
        assertEquals(GameState.GamePhase.PRE_FLOP, getPhase(game));
        game.performAction("p1", ActionType.CALL, 0); // Calls 10 more (total 20)
        game.performAction("p2", ActionType.CHECK, 0); // Checks

        // Flop
        assertEquals(GameState.GamePhase.FLOP, getPhase(game));
        // Postflop action starts with BB

        game.performAction("p2", ActionType.CHECK, 0);
        game.performAction("p1", ActionType.CHECK, 0);

        // Turn
        assertEquals(GameState.GamePhase.TURN, getPhase(game));
        game.performAction("p2", ActionType.CHECK, 0);
        game.performAction("p1", ActionType.CHECK, 0);

        // River
        System.out.println("Phase: RIVER");
        assertEquals(GameState.GamePhase.RIVER, getPhase(game));
        game.performAction("p2", ActionType.CHECK, 0);
        game.performAction("p1", ActionType.CHECK, 0);
        System.out.println("Actions: Check-Check on River.");

        // Showdown
        // The community cards form a 6-high Straight Flush (2h,3h,4h,5h,6h).
        // Both players play the board, resulting in a split pot.

        assertEquals(GameState.GamePhase.HAND_ENDED, getPhase(game));
        assertEquals(1000, p1.getChips());
        assertEquals(1000, p2.getChips());
        System.out.println("Result: HAND_ENDED. Pot Split verified. P1:1000, P2:1000.");
        System.out.println("TEST PASSED: testStandardGameFlow\n--------------------------------------------------");
    }

    @Test
    public void testSidePots() {
        System.out.println("TEST START: testSidePots");
        // 3 Players. P1(100), P2(200), P3(500).
        // P1 All-in (100). P2 All-in (200). P3 Calls (200).
        // Pot Structure:
        // Main Pot: 100*3 = 300. (Eligible: P1, P2, P3)
        // Side Pot 1: 100*2 = 200. (Eligible: P2, P3)
        // Total Pot = 500.
        System.out.println("Setup: 3-way All-in Scenario. P1(100, RF), P2(200, Flush), P3(500, Trash).");

        // We omit the random deck initialization because we configure a stacked deck
        // immediately.
        StackedDeck deck = new StackedDeck();
        PokerGameConfiguration config2 = new PokerGameConfiguration.Builder().smallBlind(10).bigBlind(20).build();
        PokerGame game = new PokerGame(config2, new StandardHandEvaluator(), deck);

        TestPlayer p1 = new TestPlayer("p1", "Shorty", 100);
        TestPlayer p2 = new TestPlayer("p2", "Mid", 200);
        TestPlayer p3 = new TestPlayer("p3", "Big", 500);

        game.join(p1);
        game.join(p2);
        game.join(p3);

        // Rig Deck:
        // P1: Royal Flush (Best) -> Wins Main Pot (300)
        // P2: King High Flush (2nd) -> Wins Side Pot (200)
        // P3: 2-7 Offsuit (Trash) -> Loses everything

        // Board: Ah, Kh, Qh, Jh, 2s
        // P1: Th, 2c (Royal Flush Hearts)
        // Set up the board and player hands to enforce specific winners:
        // Board: Ah, Kh, Qh, Jh, 2s
        // P1 (Th, 2c) forms a Royal Flush (wins main pot).
        // P2 (9h, 3c) forms an Ace-High Flush (wins side pot).
        // P3 (2d, 7d) has a pair of 2s (loses).

        // Push cards in reverse dealing order
        // River: 2s
        deck.push(Card.Rank.TWO, Card.Suit.SPADES);
        // Turn: Jh
        deck.push(Card.Rank.JACK, Card.Suit.HEARTS);
        // Flop: Ah, Kh, Qh
        deck.push(Card.Rank.QUEEN, Card.Suit.HEARTS);
        deck.push(Card.Rank.KING, Card.Suit.HEARTS);
        deck.push(Card.Rank.ACE, Card.Suit.HEARTS);

        // Hole Cards (3 players)
        // P1 (Th, 2c), P2 (9h, 3c), P3 (2d, 7d)
        // Deal: P1, P2, P3, P1, P2, P3
        deck.push(Card.Rank.SEVEN, Card.Suit.DIAMONDS); // P3
        deck.push(Card.Rank.THREE, Card.Suit.CLUBS); // P2
        deck.push(Card.Rank.TWO, Card.Suit.CLUBS); // P1
        deck.push(Card.Rank.TWO, Card.Suit.DIAMONDS); // P3
        deck.push(Card.Rank.NINE, Card.Suit.HEARTS); // P2
        deck.push(Card.Rank.TEN, Card.Suit.HEARTS); // P1

        game.startHand();

        // Pre-flop Actions
        // P1 (Button) acts first (3 players).
        game.performAction("p1", ActionType.ALL_IN, 0); // 100
        game.performAction("p2", ActionType.ALL_IN, 0); // 200
        game.performAction("p3", ActionType.CALL, 0); // Matches 200 (Has 500)

        // With all opposing players either All-In or calling the maximum bet,
        // the engine automatically advances through the remaining phases to SHOWDOWN.

        assertEquals(GameState.GamePhase.HAND_ENDED, getPhase(game));

        // Payout evaluation:
        // Main Pot (300) -> Won by P1 (100 stack prior + 200 winnings) -> 300 total
        // Side Pot (200) -> Won by P2 (200 stack prior, finishes with 200 total)
        // P3 finishes with 300 total (lost 200 of their 500 initial stack).

        assertEquals(300, p1.getChips());
        assertEquals(200, p2.getChips());
        assertEquals(300, p3.getChips()); // P3 started 500, bet 200, Lost everything. 500-200=300.
        System.out.println("Result: Payouts Verified. P1(+300)=300, P2(+0)=200, P3(-200)=300.");
        System.out.println("TEST PASSED: testSidePots\n--------------------------------------------------");
    }

    @Test
    public void testCactusKevEvaluator() {
        System.out.println("TEST START: testCactusKevEvaluator");
        CactusKevEvaluator ck = new CactusKevEvaluator();

        // Helper to quick create cards
        // Royal Flush
        System.out.println("Testing Royal Flush...");
        List<Card> royal = parseCards("As Ks Qs Js Ts 2h 3d");
        HandResult res = ck.evaluate(royal.subList(0, 2), royal.subList(2, 7));
        assertEquals(HandRank.STRAIGHT_FLUSH, res.rank());
        // Verify Top Card is Ace to confirm correct sorting
        assertEquals(Card.Rank.ACE, res.bestFive().get(0).rank());

        // Straight Flush (lower than Royal)
        System.out.println("Testing Straight Flush (King High)...");
        List<Card> strFlush = parseCards("Ks Qs Js Ts 9s 2h 3d");
        HandResult resStrFlush = ck.evaluate(strFlush.subList(0, 2), strFlush.subList(2, 7));
        assertEquals(HandRank.STRAIGHT_FLUSH, resStrFlush.rank());

        // Four of a Kind
        System.out.println("Testing Four of a Kind...");
        List<Card> quads = parseCards("As Ah Ad Ac Ks 2h 3d");
        HandResult res2 = ck.evaluate(quads.subList(0, 2), quads.subList(2, 7));
        assertEquals(HandRank.FOUR_OF_A_KIND, res2.rank());

        // Full House
        System.out.println("Testing Full House...");
        List<Card> fullHouse = parseCards("Ks Kh Kd Qs Qh 2d 3c"); // KKK QQ
        HandResult resFH = ck.evaluate(fullHouse.subList(0, 2), fullHouse.subList(2, 7));
        assertEquals(HandRank.FULL_HOUSE, resFH.rank());

        // Flush
        System.out.println("Testing Flush...");
        List<Card> flush = parseCards("As 2s 4s 6s 8s Kd Qd"); // Spades
        HandResult resFlush = ck.evaluate(flush.subList(0, 2), flush.subList(2, 7));
        assertEquals(HandRank.FLUSH, resFlush.rank());

        // Straight
        System.out.println("Testing Straight...");
        List<Card> straight = parseCards("9s 8c 7d 6h 5s 2d 3c"); // 9-8-7-6-5
        HandResult resStraight = ck.evaluate(straight.subList(0, 2), straight.subList(2, 7));
        assertEquals(HandRank.STRAIGHT, resStraight.rank());

        // Wheel Straight (A-2-3-4-5)
        System.out.println("Testing Wheel Straight (5-High)...");
        List<Card> wheel = parseCards("As 2d 3c 4h 5s 9d Tc");
        HandResult resWheel = ck.evaluate(wheel.subList(0, 2), wheel.subList(2, 7));
        assertEquals(HandRank.STRAIGHT, resWheel.rank());
        // Verify wheel straight evaluation (A-2-3-4-5) where Ace acts as low card.

        // Three of a Kind
        System.out.println("Testing Three of a Kind...");
        List<Card> trips = parseCards("Ks Kh Kd Qs Js 2d 3c");
        HandResult resTrips = ck.evaluate(trips.subList(0, 2), trips.subList(2, 7));
        assertEquals(HandRank.THREE_OF_A_KIND, resTrips.rank());

        // Two Pair
        System.out.println("Testing Two Pair...");
        List<Card> twoPair = parseCards("Ks Kh Qs Qh Js 2d 3c");
        HandResult resTwoPair = ck.evaluate(twoPair.subList(0, 2), twoPair.subList(2, 7));
        assertEquals(HandRank.TWO_PAIR, resTwoPair.rank());

        // One Pair
        System.out.println("Testing One Pair...");
        List<Card> onePair = parseCards("Ks Kh Qs Js Ts 2d 3c");
        HandResult resOnePair = ck.evaluate(onePair.subList(0, 2), onePair.subList(2, 7));
        assertEquals(HandRank.ONE_PAIR, resOnePair.rank());

        // High Card
        System.out.println("Testing High Card...");
        List<Card> highCard = parseCards("As Ks Qs Js 9c 2d 3h"); // Broken Royal (9c instead of Ts) -> A K Q J 9
                                                                  // Unsuited? No suits mixed.
        // A(s) K(s) Q(s) J(s) 9(c). Not flush.
        HandResult resHigh = ck.evaluate(highCard.subList(0, 2), highCard.subList(2, 7));
        assertEquals(HandRank.HIGH_CARD, resHigh.rank());

        System.out.println("Verified: All Hand Ranks identified correctly.");
        System.out.println("TEST PASSED: testCactusKevEvaluator\n--------------------------------------------------");
    }

    // Helper to parse "As Kh" etc.
    private List<Card> parseCards(String input) {
        String[] parts = input.split(" ");
        List<Card> cards = new ArrayList<>();
        for (String p : parts) {
            String rStr = p.substring(0, 1);
            String sStr = p.substring(1, 2);

            Card.Rank r = switch (rStr) {
                case "A" -> Card.Rank.ACE;
                case "K" -> Card.Rank.KING;
                case "Q" -> Card.Rank.QUEEN;
                case "J" -> Card.Rank.JACK;
                case "T" -> Card.Rank.TEN;
                case "9" -> Card.Rank.NINE;
                case "8" -> Card.Rank.EIGHT;
                case "7" -> Card.Rank.SEVEN;
                case "6" -> Card.Rank.SIX;
                case "5" -> Card.Rank.FIVE;
                case "4" -> Card.Rank.FOUR;
                case "3" -> Card.Rank.THREE;
                case "2" -> Card.Rank.TWO;
                default -> throw new IllegalArgumentException("Bad rank: " + rStr);
            };

            Card.Suit s = switch (sStr.toLowerCase()) {
                case "s" -> Card.Suit.SPADES;
                case "h" -> Card.Suit.HEARTS;
                case "d" -> Card.Suit.DIAMONDS;
                case "c" -> Card.Suit.CLUBS;
                default -> throw new IllegalArgumentException("Bad suit: " + sStr);
            };
            cards.add(new Card(r, s));
        }
        return cards;
    }

    @Test
    public void testMinRaiseAndReRaise() {
        System.out.println("TEST START: testMinRaiseAndReRaise");
        // P1, P2. Blinds 10/20.
        PokerGameConfiguration config = new PokerGameConfiguration.Builder().smallBlind(10).bigBlind(20).build();
        PokerGame game = new PokerGame(config, new StandardHandEvaluator(), new StackedDeck());
        TestPlayer p1 = new TestPlayer("p1", "A", 1000);
        TestPlayer p2 = new TestPlayer("p2", "B", 1000);
        game.join(p1);
        game.join(p2);
        game.startHand();

        // Preflop: P1(SB) 10, P2(BB) 20. Action P1.
        // Min raise is usually Big Blind (20), or previous raise amount.
        // P1 Raises to 40 (Min valid raise: Call 20 + Raise 20 = 40).
        game.performAction("p1", ActionType.RAISE, 40);

        // Action P2. Pot is 10+40 = 50. P2 put 20. Needs 20 to call.
        // P2 Re-raises. Min raise is +20. Total 60.
        // Let's try invalid raise (under min).
        assertThrows(InvalidActionException.class, () -> {
            game.performAction("p2", ActionType.RAISE, 50); // Raise to 50 (Increase by 10 only) - should fail
        });

        game.performAction("p2", ActionType.RAISE, 100); // Valid re-raise
        assertEquals(100, p2.getCurrentBet());
        System.out.println("Verified: Valid Re-raise accepted.");
        System.out.println("TEST PASSED: testMinRaiseAndReRaise\n--------------------------------------------------");
    }

    // Accessor for the game phase to verify correct transitions
    private GameState.GamePhase getPhase(PokerGame game) {
        return game.getGameState().getPhase();
    }
}
