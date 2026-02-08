package de.simonaltschaeffl.poker.client;

import de.simonaltschaeffl.poker.engine.PokerGame;
import de.simonaltschaeffl.poker.model.ActionType;
import de.simonaltschaeffl.poker.model.Card;
import de.simonaltschaeffl.poker.model.GameState;
import de.simonaltschaeffl.poker.model.Player;
import de.simonaltschaeffl.poker.api.GameEventListener;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

public class ConsoleClient {

    public static void main(String[] args) {
        new ConsoleClient().run();
    }

    public void run() {
        System.out.println("Welcome to the Poker Engine Console Client!");

        // 1. Setup
        PokerGame game = new PokerGame(10, 20); // Standard Evaluator, Random Deck

        Player p1 = new ConsolePlayer("p1", "Alice", 1000);
        Player p2 = new ConsolePlayer("p2", "Bob", 1000);
        Player p3 = new ConsolePlayer("p3", "Charlie", 1000); // 3-handed for more fun

        game.join(p1);
        game.join(p2);
        game.join(p3);

        game.addListener(new ConsoleGameLogger());

        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;

            while (running) {
                System.out.println("\n--- NEW HAND ---");
                try {
                    game.startHand();
                } catch (Exception e) {
                    System.err.println("Failed to start hand: " + e.getMessage());
                    break;
                }

                // Game Loop for one hand
                while (game.getGameState().getPhase() != GameState.GamePhase.HAND_ENDED) {
                    GameState state = game.getGameState();
                    int currentPos = state.getCurrentActionPosition();
                    Player currentPlayer = state.getPlayers().get(currentPos);

                    // Check if we need to skip (e.g. if player is All-in/Folded? Engine should
                    // handle skip,
                    // but let's be safe. Engine's getCurrentActionPosition should point to ACTIVE
                    // player.)

                    System.out.printf("\n[Phase: %s] | Pot: %d | Board: %s\n",
                            state.getPhase(),
                            state.getPot().getTotal(),
                            formatCards(state.getBoard()));

                    System.out.printf("Action needed from: %s (%s) [Chips: %d, CurrentBet: %d]\n",
                            currentPlayer.getName(), currentPlayer.getId(), currentPlayer.getChips(),
                            currentPlayer.getCurrentBet());

                    System.out.printf("Your hand: %s\n", formatCards(currentPlayer.getHoleCards()));

                    System.out.print("> ");
                    String input = scanner.nextLine().trim();

                    if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                        running = false;
                        break;
                    }

                    try {
                        processInput(game, currentPlayer.getId(), input);
                    } catch (IllegalArgumentException | IllegalStateException e) {
                        System.out.println("Creating Action Failed: " + e.getMessage());
                    } catch (Exception e) {
                        System.out.println("Error: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                if (!running)
                    break;

                System.out.println("\nHand finished. Press ENTER to play another hand, or type 'quit' to exit.");
                String postHand = scanner.nextLine();
                if (postHand.equalsIgnoreCase("quit")) {
                    running = false;
                }
            }
        }

        System.out.println("Goodbye!");
    }

    private void processInput(PokerGame game, String playerId, String input) {
        String[] parts = input.split("\\s+");
        String command = parts[0].toLowerCase();

        switch (command) {
            case "fold" -> game.performAction(playerId, ActionType.FOLD, 0);
            case "check" -> game.performAction(playerId, ActionType.CHECK, 0);
            case "call" -> game.performAction(playerId, ActionType.CALL, 0);
            case "raise" -> {
                if (parts.length < 2) {
                    System.out.println("Usage: raise <amount>");
                    return;
                }
                int amount = Integer.parseInt(parts[1]);
                game.performAction(playerId, ActionType.RAISE, amount);
            }
            case "allin" -> game.performAction(playerId, ActionType.ALL_IN, 0); // Amount ignored for All-IN usually?
                                                                                // Logic says player.getChips()
            case "state" -> printDebugState(game);
            case "help" -> System.out.println("Commands: fold, check, call, raise <amount>, allin, state, quit");
            default -> System.out.println("Unknown command. Type 'help' for options.");
        }
    }

    private void printDebugState(PokerGame game) {
        GameState s = game.getGameState();
        System.out.println("DEBUG STATE:");
        for (Player p : s.getPlayers()) {
            System.out.printf("  %s (%s): %s | Bet: %d | Chips: %d\n",
                    p.getName(), p.getStatus(), formatCards(p.getHoleCards()), p.getCurrentBet(), p.getChips());
        }
    }

    private String formatCards(List<Card> cards) {
        if (cards == null || cards.isEmpty())
            return "[]";
        return cards.stream().map(Object::toString).collect(Collectors.joining(" "));
    }

    // Inner class for logging
    private class ConsoleGameLogger implements GameEventListener {
        @Override
        public void onGameStarted() {
            System.out.println("[EVENT] Game Started");
        }

        @Override
        public void onRoundStarted(String roundName) {
            System.out.println("\n[EVENT] Round Started: " + roundName);
        }

        @Override
        public void onPlayerAction(Player player, ActionType action, int amount, int chipBalanceBefore,
                int chipBalanceAfter) {
            String amountStr = (action == ActionType.CHECK || action == ActionType.FOLD) ? "" : " " + amount;
            System.out.printf("[EVENT] %s %s%s (Chips: %d -> %d)\n", player.getName(), action, amountStr,
                    chipBalanceBefore, chipBalanceAfter);
        }

        @Override
        public void onPotUpdate(int potTotal) {
            System.out.println("[EVENT] Pot is now: " + potTotal);
        }

        @Override
        public void onHandEnded(List<Player> winners, Map<String, Integer> payoutMap) {
            System.out.println("\n[EVENT] Hand Ended!");
            System.out.print("Winners: ");
            for (Player w : winners) {
                Integer amount = payoutMap.get(w.getId());
                System.out.printf("%s wins %d! ", w.getName(), amount);
            }
            System.out.println();
        }
    }
}
