package com.jeopardy.service;

import com.jeopardy.model.GameData;
import com.jeopardy.model.Question;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class CsvGameDataLoader implements GameDataLoader {

    @Override
    public GameData load(Path path) throws IOException {
        GameData gameData = new GameData();

        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line = br.readLine(); // header

            while ((line = br.readLine()) != null) {
                // 8 columns: Category,Value,Question,OptionA,OptionB,OptionC,OptionD,CorrectAnswer
                String[] parts = line.split(",", 8);
                if (parts.length < 8) continue;

                String category = parts[0].trim();
                int value = Integer.parseInt(parts[1].trim());
                String questionText = parts[2].trim();
                String optionA = parts[3].trim();
                String optionB = parts[4].trim();
                String optionC = parts[5].trim();
                String optionD = parts[6].trim();
                String correctAnswer = parts[7].trim().toUpperCase(); // A/B/C/D

                Map<String, String> options = new HashMap<>();
                options.put("A", optionA);
                options.put("B", optionB);
                options.put("C", optionC);
                options.put("D", optionD);

                // Match your constructor exactly
                Question q = new Question(category, value, questionText, options, correctAnswer);

                gameData.addQuestion(q);
            }
        }

        return gameData;
    }
}
