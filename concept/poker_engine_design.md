# Texas Hold'em Game Engine - Design Concept

## 1. Overview
This library provides a robust, event-driven engine for running Texas Hold'em Poker games. It handles the complete game lifecycle, including deck management, player actions, betting rounds, pot calculations, and hand evaluation. It is designed to be backend-agnostic, meaning it can be plugged into a CLI, a web server (Spring Boot), or a desktop application. It includes a modular event system to allow external logging of every transaction (fiscally relevant actions).

## 2. Core Architecture

The engine is built around a central `GameEngine` or `Table` class that manages the `GameState`.

### 2.1. Key Entities (Data Models)

*   **Card**: Immutable representation of a card (Rank, Suit).
*   **Deck**: Manages the 52 cards, shuffling, and dealing.
*   **Player**: Abstract base class that holds poker-specific state. User can subclass this to add casino-specific data (e.g., `CasinoUser` reference).
    *   `id`: Unique identifier (UUID or String).
    *   `name`: Display name.
    *   `chips`: Current stack size (managed by engine).
    *   `holeCards`: The 2 private cards.
    *   `status`: (ACTIVE, FOLDED, ALL_IN, SITTING_OUT).
    *   `currentBet`: Amount bet in the current round.
*   **Pot**: Manages the chips.
    *    Support for main pot and potential side pots (crucial for multiple all-ins).
*   **Board**: The community cards (Flop [3], Turn [1], River [1]).

### 2.2. Game State Management

The `GameState` holds the snapshot of the current game:
*   `players`: List of players.
*   `board`: Current community cards.
*   `pot`: Current pot state.
*   `dealerButtonPosition`: Index of the dealer button.
*   `currentActionPosition`: Index of the player who needs to act.
*   `phase`: (PRE_GAME, PRE_FLOP, FLOP, TURN, RIVER, SHOWDOWN, HAND_ENDED).
*   `minRaise`: The minimum amount a player must raise.

### 2.3. The Engine / Controller

*   **PokerGame**: The main entry point.
    *   `join(Player p)`
    *   `leave(Player p)`
    *   `startHand()`
    *   `performAction(PlayerId, Action)`

### 2.4. Actions

An `Action` enum or class hierarchy:
*   `FOLD`
*   `CHECK`
*   `CALL`
*   `RAISE` (with amount)
*   `ALL_IN`

### 2.5. Hand Evaluation

A dedicated `HandEvaluator` service is needed.
*   Input: 7 cards (2 hole + 5 board).
*   Output: `HandResult` (Rank: High Card... Royal Flush, Setup of best 5 cards).
*   Comparator: Capability to compare two HandResults to determine win/split.

### 2.6. Event System & Transaction Logging (Modularity)

To support modular transaction logging (e.g., for audit trails, analytics, or UI updates), the engine will use an **Observer Pattern**.

*   **GameEventListener**: Interface that clients implement.
    *   `onGameStarted(GameId)`
    *   `onRoundStarted(Phase)`
    *   `onPlayerAction(PlayerId, Action, Amount, ChipBalanceBefore, ChipBalanceAfter)`
    *   `onPotUpdate(PotSize)`
    *   `onHandEnded(Winners, Amounts)`
*   **GameEngine**: Will broadcast events to all registered listeners.

This allows you to implement a `DatabaseTransactionLogger` or `ConsoleLogger` separately from the game logic.

## 3. Game Flow (Finite State Machine)

1.  **Initialization**: Table created, players join.
2.  **Start Hand**:
    *   Blinds posted (Small/Big).
    *   Deck shuffled.
    *   Hole cards dealt.
    *   Phase -> `PRE_FLOP`.
    *   Action points to player left of Big Blind (UTG).
3.  **Betting Round**:
    *   Wait for action.
    *   Validate action (e.g., can't check if there's a bet).
    *   Update chips/pot.
    *   Move action to next active player.
    *   **Round End Condition**: All active players have matched the highest bet (or are all-in).
4.  **Phase Transition**:
    *   If round complete:
        *   Burn card (internal logic).
        *   Deal community cards (Flop/Turn/River).
        *   Phase updates.
        *   Action resets to first active player left of dealer.
5.  **Showdown**:
    *   If >1 player remains after River.
    *   Compare hands.
    *   Distribute pot (handle splits and side pots).
6.  **Hand End**:
    *   Move dealer button.
    *   Reset for next hand.

## 4. API Usage Example

```java
// 1. Initialize
PokerTable table = new PokerTable(Config.builder().smallBlind(10).bigBlind(20).build());

// 1.5 Register Logger (Modular)
table.addListener(new GameEventListener() {
    @Override
    public void onPlayerAction(String playerId, Action action, int amount, int balanceBefore, int balanceAfter) {
        System.out.println("LOG: Player " + playerId + " did " + action + " (" + amount + ")");
        // Save to DB here
    }
});

// 2. Add Players (Custom Implementation)
class CasinoPlayer extends Player {
    public Long databaseId;
    public CasinoPlayer(String id, String name, int chips, Long dbId) {
        super(id, name, chips);
        this.databaseId = dbId;
    }
}

table.join(new CasinoPlayer("p1", "Alice", 1000, 55L));

// 3. Start
table.startHand();

// 4. Play (Event Driven or Polling)
// Alice is UTG (assuming Heads up, actually dealer is SB in heads up but lets ignore edge cases for summary)
table.act("p1", Action.CALL); 
table.act("p2", Action.CHECK);

// 5. State inspection
GameState state = table.getState();
System.out.println(state.getPhase()); // FLOP
System.out.println(state.getBoard()); // [Ash, Kh, 2d]
```

## 5. Technology Stack Implementation Plan

*   **Language**: Java 17+ or 21.
*   **Build Tool**: Maven.
*   **Testing**: JUnit 5 + Mockito.
*   **No external database dependencies** for core logic (in-memory).

## 6. Implementation Stages

1.  **Phase 1: Basic Mechanics**: Card, Deck, HandEvaluator.
2.  **Phase 2: Game State & Flow**: Pot logic, Betting rounds, State machine.
3.  **Phase 3: Actions & Validation**: Strict rules enforcement.
4.  **Phase 4: Side Pots & Edge Cases**: The hardest part of poker engines.
