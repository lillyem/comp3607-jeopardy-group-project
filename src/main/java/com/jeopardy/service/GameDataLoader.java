package com.jeopardy.service;

import com.jeopardy.model.GameData;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Strategy interface for loading game questions from various file formats.
 * Implementations provide format-specific parsing logic while maintaining
 * a consistent interface for the game controller.
 *  
 */
public interface GameDataLoader {
    /**
     * Loads and parses game questions from the specified file path.
     * Validates the data structure and returns a GameData object.
     *
     * @param filePath Path to the question file
     * @return GameData containing categories and questions
     * @throws IOException if the file cannot be read
     * @throws IllegalArgumentException if file format is invalid or data validation fails
     */
    GameData load(Path path) throws IOException;
}
