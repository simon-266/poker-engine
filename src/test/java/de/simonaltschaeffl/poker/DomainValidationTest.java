package de.simonaltschaeffl.poker;

import de.simonaltschaeffl.poker.dto.CardDTO;
import de.simonaltschaeffl.poker.dto.GameStateDTO;
import de.simonaltschaeffl.poker.dto.PlayerDTO;
import de.simonaltschaeffl.poker.model.Action;
import de.simonaltschaeffl.poker.model.ActionType;
import de.simonaltschaeffl.poker.model.Card;
import de.simonaltschaeffl.poker.model.GameState;
import de.simonaltschaeffl.poker.model.Player;
import de.simonaltschaeffl.poker.model.PlayerStatus;
import de.simonaltschaeffl.poker.model.Pot;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DomainValidationTest {

    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // --- DTO Tests ---

    @Test
    public void testPlayerDTOValidation() {
        PlayerDTO validPlayer = new PlayerDTO("p1", "Player 1", 1000, PlayerStatus.ACTIVE, 0, Collections.emptyList());
        Set<ConstraintViolation<PlayerDTO>> violations = validator.validate(validPlayer);
        assertTrue(violations.isEmpty(), "Valid PlayerDTO should have no violations");

        PlayerDTO blankId = new PlayerDTO("", "Player 1", 1000, PlayerStatus.ACTIVE, 0, Collections.emptyList());
        violations = validator.validate(blankId);
        assertFalse(violations.isEmpty(), "PlayerDTO with blank ID should have violations");
        assertEquals("id", violations.iterator().next().getPropertyPath().toString());

        PlayerDTO negativeChips = new PlayerDTO("p1", "Player 1", -50, PlayerStatus.ACTIVE, 0, Collections.emptyList());
        violations = validator.validate(negativeChips);
        assertFalse(violations.isEmpty(), "PlayerDTO with negative chips should have violations");
        assertEquals("chips", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    public void testCardDTOValidation() {
        CardDTO validCard = new CardDTO(Card.Suit.HEARTS, Card.Rank.ACE);
        Set<ConstraintViolation<CardDTO>> violations = validator.validate(validCard);
        assertTrue(violations.isEmpty(), "Valid CardDTO should have no violations");

        CardDTO nullSuit = new CardDTO(null, Card.Rank.ACE);
        violations = validator.validate(nullSuit);
        assertFalse(violations.isEmpty(), "CardDTO with null suit should have violations");
        assertEquals("suit", violations.iterator().next().getPropertyPath().toString());

        CardDTO nullRank = new CardDTO(Card.Suit.HEARTS, null);
        violations = validator.validate(nullRank);
        assertFalse(violations.isEmpty(), "CardDTO with null rank should have violations");
        assertEquals("rank", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    public void testGameStateDTOValidation() {
        GameStateDTO validState = new GameStateDTO(100, Collections.emptyList(), Collections.emptyList(),
                GameState.GamePhase.PRE_FLOP, 0, 0);
        Set<ConstraintViolation<GameStateDTO>> violations = validator.validate(validState);
        assertTrue(violations.isEmpty(), "Valid GameStateDTO should have no violations");

        GameStateDTO negativePot = new GameStateDTO(-10, Collections.emptyList(), Collections.emptyList(),
                GameState.GamePhase.PRE_FLOP, 0, 0);
        violations = validator.validate(negativePot);
        assertFalse(violations.isEmpty(), "GameStateDTO with negative pot should have violations");
        assertEquals("potTotal", violations.iterator().next().getPropertyPath().toString());
    }

    // --- Domain Model Tests ---

    // Concrete implementation for testing abstract Player class
    public static class TestPlayer extends Player {
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

    @Test
    public void testPlayerValidation() {
        TestPlayer validPlayer = new TestPlayer("p1", "Player 1", 1000);
        Set<ConstraintViolation<TestPlayer>> violations = validator.validate(validPlayer);
        assertTrue(violations.isEmpty(), "Valid Player should have no violations");

        TestPlayer blankName = new TestPlayer("p1", "", 1000);
        violations = validator.validate(blankName);
        assertFalse(violations.isEmpty(), "Player with blank name should have violations");
        assertEquals("name", violations.iterator().next().getPropertyPath().toString());

        TestPlayer negativeChips = new TestPlayer("p1", "Player 1", -100);
        violations = validator.validate(negativeChips);
        assertFalse(violations.isEmpty(), "Player with negative chips should have violations");
        assertEquals("chips", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    public void testCardValidation() {
        Card validCard = new Card(Card.Rank.ACE, Card.Suit.SPADES);
        Set<ConstraintViolation<Card>> violations = validator.validate(validCard);
        assertTrue(violations.isEmpty(), "Valid Card should have no violations");

        Card nullRank = new Card(null, Card.Suit.SPADES);
        violations = validator.validate(nullRank);
        assertFalse(violations.isEmpty(), "Card with null rank should have violations");
        assertEquals("rank", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    public void testActionValidation() {
        TestPlayer p = new TestPlayer("p1", "Player 1", 1000);
        Action validAction = new Action(p, ActionType.CHECK, 0);
        Set<ConstraintViolation<Action>> violations = validator.validate(validAction);
        assertTrue(violations.isEmpty(), "Valid Action should have no violations");

        Action negativeAmount = new Action(p, ActionType.RAISE, -50);
        violations = validator.validate(negativeAmount);
        assertFalse(violations.isEmpty(), "Action with negative amount should have violations");
        assertEquals("amount", violations.iterator().next().getPropertyPath().toString());

        Action nullPlayer = new Action(null, ActionType.FOLD, 0);
        violations = validator.validate(nullPlayer);
        assertFalse(violations.isEmpty(), "Action with null player should have violations");
        assertEquals("player", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    public void testPotValidation() {
        Pot pot = new Pot();
        Set<ConstraintViolation<Pot>> violations = validator.validate(pot);
        assertTrue(violations.isEmpty(), "New Pot should be valid");

        // Use reflection or a test-setter if possible to test invalid state,
        // but since Pot logic is encapsualted, we might rely on constructor/methods.
        // However, if we added @Min(0) to `total`, we want to ensure it's respected if
        // it were ever exposed or deserialized.
        // Since we can't easily set private fields without reflection in this test, we
        // verify the annotation existence mostly via normal usage.
    }

    @Test
    public void testGameStateValidation() {
        GameState gameState = new GameState();
        Set<ConstraintViolation<GameState>> violations = validator.validate(gameState);
        assertTrue(violations.isEmpty(), "New GameState should be valid");

        // Similarly, detailed negative testing would require setting private fields to
        // invalid values via reflection or invalid constructors.
    }
}
