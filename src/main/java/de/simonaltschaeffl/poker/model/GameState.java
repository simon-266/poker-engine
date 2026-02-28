package de.simonaltschaeffl.poker.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class GameState {
    @NotNull
    private final List<Player> players;
    @NotNull
    private final List<Card> board;
    @NotNull
    private final Pot pot;
    @Min(0)
    private int dealerButtonPosition;
    @Min(0)
    private int currentActionPosition;
    @NotNull
    private GamePhase phase;
    private long currentTurnStartTime;

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

    public long getCurrentTurnStartTime() {
        return currentTurnStartTime;
    }

    public void setCurrentTurnStartTime(long currentTurnStartTime) {
        this.currentTurnStartTime = currentTurnStartTime;
    }
}
