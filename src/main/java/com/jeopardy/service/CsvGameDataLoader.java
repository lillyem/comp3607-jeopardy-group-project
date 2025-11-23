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

    private static final String[] REQUIRED_HEADERS = {
            "Category", "Value", "Question",
            "OptionA", "OptionB", "OptionC", "OptionD", "CorrectAnswer"
    };

    @Override
    public GameData load(Path filePath) throws IOException {
        if (filePath == null) {
            throw new IllegalArgumentException("CSV file path cannot be null.");
        }

        if (!Files.exists(filePath)) {
            throw new IOException("CSV file does not exist: " + filePath);
        }

        GameData gameData = new GameData();

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("CSV file is empty: " + filePath);
            }

            validateHeader(headerLine);

            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.trim().isEmpty()) {
                    continue; // skip blank lines
                }

                List<String> fields = parseCsvLine(line);

                if (fields.size() != REQUIRED_HEADERS.length) {
                    throw new IOException("Invalid number of fields on line " + lineNumber
                            + ". Expected " + REQUIRED_HEADERS.length + " but found " + fields.size());
                }

                String categoryName = fields.get(0).trim();
                int value = parseIntSafely(fields.get(1), "Value", lineNumber);
                String questionText = fields.get(2).trim();
                String optionA = fields.get(3).trim();
                String optionB = fields.get(4).trim();
                String optionC = fields.get(5).trim();
                String optionD = fields.get(6).trim();
                String correct = fields.get(7).trim().toUpperCase();

                Map<String, String> options = new LinkedHashMap<>();
                options.put("A", optionA);
                options.put("B", optionB);
                options.put("C", optionC);
                options.put("D", optionD);

                // Use your actual Question constructor
                Question q = new Question(
                        categoryName,
                        value,
                        questionText,
                        options,
                        correct
                );

                // Let GameData handle putting it in the right Category
                gameData.addQuestion(q);
            }
        }

        // Validate the fully built data structure
        DataValidator.validateCategories(gameData.getCategories());

        return gameData;
    }

    /** Validate CSV header format */
    private void validateHeader(String headerLine) throws IOException {
        String[] headers = headerLine.split(",");

        if (headers.length != REQUIRED_HEADERS.length) {
            throw new IOException("CSV header is invalid. Expected fields: "
                    + Arrays.toString(REQUIRED_HEADERS));
        }

        for (int i = 0; i < REQUIRED_HEADERS.length; i++) {
            if (!headers[i].trim().equalsIgnoreCase(REQUIRED_HEADERS[i])) {
                throw new IOException("CSV header mismatch at column " + (i + 1)
                        + ": expected '" + REQUIRED_HEADERS[i]
                        + "' but found '" + headers[i] + "'");
            }
        }
    }

    /** Safely parse an integer */
    private int parseIntSafely(String s, String fieldName, int lineNumber) throws IOException {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new IOException("Invalid integer value for '" + fieldName
                    + "' on line " + lineNumber + ": '" + s + "'");
        }
    }

    /**
     * Parses a CSV line into fields while handling quoted values.
     */
    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();

        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        fields.add(cur.toString());

        // Strip outer quotes & unescape
        for (int i = 0; i < fields.size(); i++) {
            String field = fields.get(i).trim();
            if (field.startsWith("\"") && field.endsWith("\"")) {
                field = field.substring(1, field.length() - 1);
            }
            fields.set(i, field.replace("\"\"", "\""));
        }

        return fields;
    }
}
