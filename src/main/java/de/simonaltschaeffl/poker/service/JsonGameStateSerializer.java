package de.simonaltschaeffl.poker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.simonaltschaeffl.poker.api.GameStateRepository;
import de.simonaltschaeffl.poker.model.GameState;

import java.io.File;
import java.io.IOException;

/**
 * A simple implementation of {@link GameStateRepository} that serializes
 * the GameState to JSON files using Jackson.
 */
public class JsonGameStateSerializer implements GameStateRepository {

    private final ObjectMapper objectMapper;
    private final String storageDirectory;

    /**
     * Constructs a new JsonGameStateSerializer.
     *
     * @param storageDirectory The path to the directory where JSON files will be
     *                         saved.
     */
    public JsonGameStateSerializer(String storageDirectory) {
        this.objectMapper = new ObjectMapper();
        this.storageDirectory = storageDirectory;

        File dir = new File(storageDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @Override
    public void save(String gameId, GameState state) {
        try {
            File file = new File(storageDirectory, gameId + ".json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, state);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize GameState to JSON for gameId " + gameId, e);
        }
    }

    @Override
    public GameState load(String gameId) {
        try {
            File file = new File(storageDirectory, gameId + ".json");
            if (!file.exists()) {
                return null;
            }
            return objectMapper.readValue(file, GameState.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load GameState from JSON for gameId " + gameId, e);
        }
    }
}
