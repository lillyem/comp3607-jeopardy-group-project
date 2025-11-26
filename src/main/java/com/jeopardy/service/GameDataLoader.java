package com.jeopardy.service;

import com.jeopardy.model.GameData;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Strategy interface for loading {@link GameData} from an external source.
 * <p>
 * Implementations typically read from files (e.g. CSV, JSON, XML) and
 * construct a populated {@code GameData} instance.
 */
public interface GameDataLoader {

    /**
     * Loads game data from the specified file path.
     *
     * @param path the path to the data file to load
     * @return a {@link GameData} instance containing all parsed categories and questions
     * @throws IOException if the file cannot be read or the content is malformed
     */
    GameData load(Path path) throws IOException;
}
