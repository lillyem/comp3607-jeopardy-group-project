package com.jeopardy.service;

import com.jeopardy.model.GameData;
import com.jeopardy.model.Question;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class JsonGameDataLoader implements GameDataLoader {

    @Override
    public GameData load(Path path) throws IOException {
        String jsonText = Files.readString(path);
        JSONArray array = new JSONArray(jsonText);

        GameData gameData = new GameData();

        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);

            String category = obj.getString("Category");
            int value = obj.getInt("Value");
            String questionText = obj.getString("Question");
            String correctAnswer = obj.getString("CorrectAnswer").toUpperCase();

            JSONObject optionsObj = obj.getJSONObject("Options");
            Map<String, String> options = new HashMap<>();
            for (Iterator<String> it = optionsObj.keys(); it.hasNext();) {
                String key = it.next();          // "A", "B", "C", "D"
                options.put(key.toUpperCase(), optionsObj.getString(key));
            }

            // Match your constructor: (category, value, text, options, correctAnswer)
            Question q = new Question(category, value, questionText, options, correctAnswer);

            gameData.addQuestion(q);
        }

        return gameData;
    }
}
