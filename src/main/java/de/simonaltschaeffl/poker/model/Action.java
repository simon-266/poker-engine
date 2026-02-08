package de.simonaltschaeffl.poker.model;

/**
 * Represents an action taken by a player.
 */
public record Action(Player player, ActionType type, int amount) {
    public Action(Player player, ActionType type) {
        this(player, type, 0);
    }
}
