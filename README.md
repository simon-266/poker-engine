# Poker Engine

A robust, Java-based Poker Engine for Texas Hold'em. This engine handles the core game logic, including state management, betting rounds, side pots, and hand evaluation.

## Features
- **Texas Hold'em Logic**: Pre-flop, Flop, Turn, River, Showdown.
- **Hand Evaluation**: Uses a high-performance evaluator (Cactus Kev variant) to determine hand strength.
- **Side Pots**: Correctly calculates main and side pots for multi-way all-in scenarios.
- **Modular Configuration**: Code-based configuration via Builder pattern.
- **Time Bank**: Configurable turn timeouts with auto-fold/check.
- **Rake System**: Pluggable strategies for house commission (Rake).
- **Event Driven**: Emits events (`onPlayerAction`, `onRoundStarted`, etc.) for easy UI or Logger integration.
- **Flexible**: Supports custom decks, betting rules, and hand evaluators for testing.

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.6+

### Build
```bash
mvn clean install
```

### Run CLI Client
A built-in command-line client is available for manual interaction and testing.
```bash
mvn test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass="de.simonaltschaeffl.poker.client.ConsoleClient"
```

---

## 📚 Engine Manual

### Core Concepts

#### 1. PokerGame & Configuration
The main entry point. It manages the `GameState` and the flow of the game. Configure it using `PokerGameConfiguration`.

```java
// Configure the game
PokerGameConfiguration config = new PokerGameConfiguration.Builder()
    .smallBlind(10)
    .bigBlind(20)
    .maxPlayers(10)
    .actionTimeoutMs(15000) // 15 seconds time bank
    .rakeStrategy(new PercentageRakeStrategy(0.05, 60)) // 5% rake, capped at 3 BB
    .bettingRuleStrategy(new NoLimitBettingStrategy())
    .build();

// Initialize the game
PokerGame game = new PokerGame(config);

// Join Players
game.join(new ConsolePlayer("p1", "Alice", 1000));
game.join(new ConsolePlayer("p2", "Bob", 1000));

// Start a Hand
game.startHand();
```

If you are using the Time Bank feature, your backend application should periodically call `game.checkTimeouts()` (e.g. every second) to enforce the time limits.

#### 2. Player
Extend the strict `Player` abstract class to create your own player types (e.g., `BotPlayer`, `NetworkPlayer`).

```java
public class MyPlayer extends Player {
    public MyPlayer(String id, String name, int chips) {
        super(id, name, chips);
    }
}
```

#### 3. Performing Actions
Use `performAction` to interact with the game. The engine validates turn order and betting rules.

```java
// Check
game.performAction("p1", ActionType.CHECK, 0);

// Call
game.performAction("p1", ActionType.CALL, 0);

// Raise (Amount is the TOTAL bet you want to establish)
// Example: Current bet is 20. Raise to 50.
game.performAction("p1", ActionType.RAISE, 50);

// Fold
game.performAction("p1", ActionType.FOLD, 0);
```

#### 4. Event Listening
Register a `GameEventListener` to react to game state changes.

```java
game.addListener(new GameEventListener() {
    @Override
    public void onPlayerAction(Player p, ActionType type, int amount, int before, int after) {
         System.out.println(p.getName() + " did " + type);
    }
    // ... implement other methods
});
```

### Game Flow
1.  **Start Hand**: Blinds are posted automatically.
2.  **Betting Loop**: The game waits for `performAction`.
3.  **Phase Transition**: When all active players match the highest bet and have acted, the engine automatically advances to the next street (Flop -> Turn -> River).
4.  **Showdown**: Hands are evaluated, pots are distributed, and `onHandEnded` is triggered.

---

## 🎮 CLI Client Manual

The included Console Client allows you to simulate a game between 3 players (`p1` Alice, `p2` Bob, `p3` Charlie) right in your terminal.

### How to Run
```bash
mvn test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass="de.simonaltschaeffl.poker.client.ConsoleClient"
```

### Commands

| Command | Description |
| :--- | :--- |
| `call` | Match the current highest bet. |
| `check` | Pass action if no bet is actively pending. |
| `fold` | Forfeit the hand. |
| `raise <amount>` | Raise the bet to `amount`. **Note**: This is the *total* amount. If bet is 20 and you want to raise by 30, type `raise 50`. |
| `allin` | Bet all your remaining chips. |
| `state` | (Debug) Print current hole cards and status for all players. |
| `quit` | Exit the application. |

### Example Session
```text
[Phase: PRE_FLOP] | Pot: 30
Action needed from: Alice (p1)
> call
[EVENT] Alice CALL 20

Action needed from: Bob (p2)
> raise 60
[EVENT] Bob RAISE 60
```
