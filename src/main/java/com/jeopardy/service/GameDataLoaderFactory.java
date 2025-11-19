package com.jeopardy.service;

public class GameDataLoaderFactory {

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
