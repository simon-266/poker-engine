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

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public static void main(String[] args) {
        new ConsoleClient().run();
    }

    public void run() {
        System.out.println(ANSI_GREEN + "Welcome to the Poker Engine Console Client!" + ANSI_RESET);

        try (Scanner scanner = new Scanner(System.in)) {
            // 1. Setup
            int numHumans = promptInt(scanner, "Enter number of human players: ");
            int numBots = promptInt(scanner, "Enter number of bots: ");
            int startChips = promptInt(scanner, "Enter starting chips (e.g. 1000): ");

            PokerGame game = new PokerGame(10, 20); // Blinds 10/20
            game.addListener(new ConsoleGameLogger());

            for (int i = 1; i <= numHumans; i++) {
                System.out.print("Enter name for Human " + i + ": ");
                String name = scanner.nextLine().trim();
                game.join(new ConsolePlayer("p" + i, name, startChips));
            }

            for (int i = 1; i <= numBots; i++) {
                game.join(new BotPlayer("b" + i, "Bot " + i, startChips));
            }

            boolean running = true;

            while (running) {
                System.out.println("\n" + ANSI_YELLOW + "--- NEW HAND ---" + ANSI_RESET);
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

                    if (currentPlayer instanceof BotPlayer bot) {
                        // Bot Logic
                        System.out.println(ANSI_BLUE + "Bot " + bot.getName() + " is thinking..." + ANSI_RESET);
                        bot.decide(game);
                    } else {
                        // Human Logic
                        printTableStatus(state);
                        System.out.printf(
                                ANSI_GREEN + "Action needed from: %s (%s) [Chips: %d, CurrentBet: %d]\n" + ANSI_RESET,
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
                            System.out.println(ANSI_RED + "Action Failed: " + e.getMessage() + ANSI_RESET);
                        } catch (Exception e) {
                            System.out.println(ANSI_RED + "Error: " + e.getMessage() + ANSI_RESET);
                            e.printStackTrace();
                        }
                    }

                    // Small delay to make it readable if bots are playing
                    if (currentPlayer instanceof BotPlayer) {
                        try {
                            Thread.sleep(500);
                        } catch (Exception e) {
                        }
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

    private int promptInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            try {
                return Integer.parseInt(input.trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid number.");
            }
        }
    }

    private void processInput(PokerGame game, String playerId, String input) {
        String[] parts = input.split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty())
            return;

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
            case "allin" -> game.performAction(playerId, ActionType.ALL_IN, 0);
            case "state" -> printDebugState(game);
            case "help" -> System.out.println("Commands: fold, check, call, raise <amount>, allin, state, quit");
            default -> System.out.println("Unknown command. Type 'help' for options.");
        }
    }

    private void printTableStatus(GameState state) {
        System.out.printf("\n[Phase: %s] | Pot: %d | Board: %s\n",
                state.getPhase(),
                state.getPot().getTotal(),
                formatCards(state.getBoard()));
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
        return cards.stream().map(this::colorCard).collect(Collectors.joining(" "));
    }

    private String colorCard(Card c) {
        String suitDetails = switch (c.suit()) {
            case HEARTS -> ANSI_RED + c.toString() + ANSI_RESET;
            case DIAMONDS -> ANSI_RED + c.toString() + ANSI_RESET; // Use Red for Diamonds too
            case CLUBS -> ANSI_WHITE + c.toString() + ANSI_RESET;
            case SPADES -> ANSI_WHITE + c.toString() + ANSI_RESET;
        };
        return suitDetails;
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

        @Override
        public void onPlayerJoinedWaitingList(Player player) {
            System.out.println("Player " + player.getName() + " added to waiting list.");
        }
    }
}
