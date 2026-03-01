package de.simonaltschaeffl.poker.dto;

import de.simonaltschaeffl.poker.model.GameState;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

public record GameStateDTO(
                @Min(0) int potTotal,
                @NotNull List<CardDTO> board,
                @NotNull List<PlayerDTO> players,
                @NotNull GameState.GamePhase currentPhase,
                @Min(0) int dealerPosition,
                @Min(0) int actionPosition) {
        public static GameStateDTO from(GameState gameState) {
                boolean isShowdown = gameState.getPhase() == GameState.GamePhase.SHOWDOWN
                                || gameState.getPhase() == GameState.GamePhase.HAND_ENDED;

                List<PlayerDTO> playerDTOs = gameState.getPlayers().stream()
                                .map(p -> PlayerDTO.from(p, isShowdown || p.getId().equals("HERO"))) // Hide hole cards
                                                                                                     // unless it is
                                                                                                     // Showdown or the
                                                                                                     // player's own
                                                                                                     // view ("HERO").
                                .collect(Collectors.toList());

                return new GameStateDTO(
                                gameState.getPot().getTotal(),
                                gameState.getBoard().stream().map(CardDTO::from).collect(Collectors.toList()),
                                playerDTOs,
                                gameState.getPhase(),
                                gameState.getDealerButtonPosition(),
                                gameState.getCurrentActionPosition());
        }

        // Allow generating a view for a specific player (to see their own cards)
        public static GameStateDTO from(GameState gameState, String observerId) {
                boolean isShowdown = gameState.getPhase() == GameState.GamePhase.SHOWDOWN
                                || gameState.getPhase() == GameState.GamePhase.HAND_ENDED;

                List<PlayerDTO> playerDTOs = gameState.getPlayers().stream()
                                .map(p -> PlayerDTO.from(p, isShowdown || p.getId().equals(observerId)))
                                .collect(Collectors.toList());

                return new GameStateDTO(
                                gameState.getPot().getTotal(),
                                gameState.getBoard().stream().map(CardDTO::from).collect(Collectors.toList()),
                                playerDTOs,
                                gameState.getPhase(),
                                gameState.getDealerButtonPosition(),
                                gameState.getCurrentActionPosition());
        }
}
