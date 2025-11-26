package com.jeopardy.service;

import java.io.IOException;

/**
     * Loads and parses game questions from the specified file path.
     * Validates the data structure and returns a GameData object.
     *
     * @param filePath Path to the question file
     * @return GameData containing categories and questions
     * @throws IOException if the file cannot be read
     * @throws IllegalArgumentException if file format is invalid or data validation fails
     */
public class GameDataLoaderFactory {

   /**
     * Creates a GameDataLoader implementation based on the file extension.
     * Supports CSV, JSON, and XML formats.
     *
     * @param filePath Path to the question file
     * @return Appropriate GameDataLoader implementation for the file format
     * @throws IllegalArgumentException if file format is not supported
     */
    public static GameDataLoader createLoader(String filename) {
        String lower = filename.toLowerCase();

        if (lower.endsWith(".csv")) {
            return new CsvGameDataLoader();
        }
        if (lower.endsWith(".json")) {
            return new JsonGameDataLoader();
        }
        if (lower.endsWith(".xml")) {
            return new XmlGameDataLoader();
        }

        throw new IllegalArgumentException("Unsupported file type: " + filename);
    }
}
