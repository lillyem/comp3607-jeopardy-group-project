package com.jeopardy.service;

/**
 * Factory for creating the appropriate {@link GameDataLoader} implementation
 * based on a game data file's extension.
 * <p>
 * Supported formats:
 * <ul>
 *     <li><code>.csv</code> → {@link CsvGameDataLoader}</li>
 *     <li><code>.json</code> → {@link JsonGameDataLoader}</li>
 *     <li><code>.xml</code> → {@link XmlGameDataLoader}</li>
 * </ul>
 */
public class GameDataLoaderFactory {

    /**
     * Creates a {@link GameDataLoader} suitable for the given file name.
     *
     * @param filename the name of the data file (used to inspect its extension)
     * @return a concrete {@link GameDataLoader} instance
     * @throws IllegalArgumentException if the file type is not supported
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
