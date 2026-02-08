package de.simonaltschaeffl.poker.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameState {
    private final List<Player> players;
    private final List<Card> board;
    private final Pot pot;
    private int dealerButtonPosition;
    private int currentActionPosition;
    private GamePhase phase;

    private final java.util.Set<String> actedPlayers;

    public enum GamePhase {
        PRE_GAME,
        PRE_FLOP,
        FLOP,
        TURN,
        RIVER,
        SHOWDOWN,
        HAND_ENDED
    }

    public GameState() {
        this.players = new ArrayList<>();
        this.board = new ArrayList<>();
        this.pot = new Pot();
        this.phase = GamePhase.PRE_GAME;
        this.dealerButtonPosition = 0;
        this.currentActionPosition = 0;

        this.actedPlayers = new java.util.HashSet<>();
    }

    public void addActedPlayer(Player p) {
        actedPlayers.add(p.getId());
    }

    public void clearActedPlayers() {
        actedPlayers.clear();
    }

    public boolean hasActed(Player p) {
        return actedPlayers.contains(p.getId());
    }

    public void addPlayer(Player player) {
        players.add(player);
    }

    public void removePlayer(Player player) {
        players.remove(player);
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public List<Card> getBoard() {
        return Collections.unmodifiableList(board);
    }

    public void addToBoard(Card card) {
        board.add(card);
    }

    public void clearBoard() {
        board.clear();
    }

    public Pot getPot() {
        return pot;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    public int getDealerButtonPosition() {
        return dealerButtonPosition;
    }

    public void setDealerButtonPosition(int dealerButtonPosition) {
        this.dealerButtonPosition = dealerButtonPosition;
    }

    public int getCurrentActionPosition() {
        return currentActionPosition;
    }

    public void setCurrentActionPosition(int currentActionPosition) {
        this.currentActionPosition = currentActionPosition;
    }
}
