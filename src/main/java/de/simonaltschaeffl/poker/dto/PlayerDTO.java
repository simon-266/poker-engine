package de.simonaltschaeffl.poker.dto;

import de.simonaltschaeffl.poker.model.Player;
import de.simonaltschaeffl.poker.model.PlayerStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.stream.Collectors;

public record PlayerDTO(
                @NotBlank String id,
                @NotBlank String name,
                @Min(0) int chips,
                PlayerStatus status,
                int currentBet,
                List<CardDTO> holeCards) {
        public static PlayerDTO from(Player player, boolean showCards) {
                List<CardDTO> cards = showCards
                                ? player.getHoleCards().stream().map(CardDTO::from).collect(Collectors.toList())
                                : List.of();

                return new PlayerDTO(
                                player.getId(),
                                player.getName(),
                                player.getChips(),
                                player.getStatus(),
                                player.getCurrentBet(),
                                cards);
        }
}
