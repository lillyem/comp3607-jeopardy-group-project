package com.jeopardy.service;

import com.jeopardy.model.GameData;
import com.jeopardy.model.Question;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Loads Jeopardy-style game data from a CSV file.
 * <p>
 * Expected header (8 columns):
 * <pre>
 * Category,Value,Question,OptionA,OptionB,OptionC,OptionD,CorrectAnswer
 * </pre>
 *
 * Each subsequent row is parsed into a {@link Question} and added to a
 * {@link GameData} instance. The loader:
 * <ul>
 *     <li>Ignores blank lines</li>
 *     <li>Performs column-count validation</li>
 *     <li>Parses values and trims whitespace</li>
 *     <li>Accepts quoted fields and escaped quotes</li>
 * </ul>
 */

public class CsvGameDataLoader implements GameDataLoader {

     /**
     * Loads game data from the specified CSV file.
     *
     * @param path the path to the CSV file
     * @return a populated {@link GameData} instance containing all parsed categories and questions
     * @throws IOException if the file cannot be read or contains malformed rows
     */
    @Override
    public GameData load(Path path) throws IOException {
        GameData gameData = new GameData();

        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line = br.readLine(); // header row, discard

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                List<String> parts = parseCsvLine(line);
                if (parts.size() < 8) {
                    throw new IOException("Invalid CSV row (expected 8 columns): " + line);
                }

                String category = parts.get(0).trim();
                int value = Integer.parseInt(parts.get(1).trim());
                String questionText = parts.get(2).trim();
                String optionA = parts.get(3).trim();
                String optionB = parts.get(4).trim();
                String optionC = parts.get(5).trim();
                String optionD = parts.get(6).trim();
                String correctAnswer = parts.get(7).trim().toUpperCase(); // A/B/C/D

                Map<String, String> options = new HashMap<>();
                options.put("A", optionA);
                options.put("B", optionB);
                options.put("C", optionC);
                options.put("D", optionD);

                // This matches your Question constructor
                Question q = new Question(category, value, questionText, options, correctAnswer);

                gameData.addQuestion(q);
            }
        }

        return gameData;
    }

    /**
     * Parses a CSV line into individual fields.
     * <p>
     * This implementation:
     * <ul>
     *     <li>Handles quoted fields</li>
     *     <li>Handles escaped double quotes ("")</li>
     *     <li>Splits only on commas that are not inside quotes</li>
     *     <li>Trims outer quotes from each field</li>
     * </ul>
     *
     * @param line a single CSV line
     * @return a list of parsed values in order
     */

    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                // Toggle quote mode, but keep content 
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped quote
                    current.append('"');
                    i++; // skip next quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString()); 
        // Strip outer quotes from each field
        for (int i = 0; i < result.size(); i++) {
            String field = result.get(i).trim();
            if (field.length() >= 2 && field.startsWith("\"") && field.endsWith("\"")) {
                field = field.substring(1, field.length() - 1).replace("\"\"", "\"");
            }
            result.set(i, field);
        }

        return result;
    }
}