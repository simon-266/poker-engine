package de.simonaltschaeffl.poker.api;

import de.simonaltschaeffl.poker.model.GameState;

/**
 * Repository interface for saving and loading the Poker GameState to an
 * external
 * storage mechanism. Enables state persistence and crash recovery.
 */
public interface GameStateRepository {

    /**
     * Persists the current state of a poker game.
     *
     * @param gameId The unique identifier for the game.
     * @param state  The {@link GameState} instance representing the current state.
     */
    void save(String gameId, GameState state);

    /**
     * Retrieves the persisted state of a poker game.
     *
     * @param gameId The unique identifier for the game to load.
     * @return The loaded {@link GameState}, or {@code null} if no state exists
     *         for the given ID.
     */
    GameState load(String gameId);
}
