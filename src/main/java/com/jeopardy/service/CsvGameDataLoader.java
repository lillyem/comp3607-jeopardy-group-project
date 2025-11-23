package com.jeopardy.service;

import com.jeopardy.model.GameData;
import com.jeopardy.model.Question;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Loads Jeopardy questions from a CSV file.
 * Expected header:
 * Category,Value,Question,OptionA,OptionB,OptionC,OptionD,CorrectAnswer
 */
public class CsvGameDataLoader implements GameDataLoader {

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

                // This matches your Question constructor:
                // Question(String category, int value, String questionText,
                //          Map<String,String> options, String correctAnswer)
                Question q = new Question(category, value, questionText, options, correctAnswer);

                gameData.addQuestion(q);
            }
        }

        return gameData;
    }

    /**
     * Very small CSV parser that:
     * - Respects double quotes
     * - Splits on commas only when NOT inside quotes
     */
    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                // Toggle quote mode, but keep content (and handle escaped quotes)
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
        result.add(current.toString()); // last field

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