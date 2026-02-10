package de.simonaltschaeffl.poker.model;

/**
 * Represents an action taken by a player.
 */
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Represents an action taken by a player.
 */
public record Action(@NotNull Player player, @NotNull ActionType type, @Min(0) int amount) {
    public Action(Player player, ActionType type) {
        this(player, type, 0);
    }
}
