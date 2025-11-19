package com.jeopardy.service;

import com.jeopardy.model.GameData;
import com.jeopardy.model.Question;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class XmlGameDataLoader implements GameDataLoader {

    @Override
    public GameData load(Path path) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(path.toFile());
            doc.getDocumentElement().normalize();

            GameData gameData = new GameData();

            NodeList items = doc.getElementsByTagName("QuestionItem");

            for (int i = 0; i < items.getLength(); i++) {
                Node node = items.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) continue;

                Element e = (Element) node;

                String category = text(e, "Category");
                int value = Integer.parseInt(text(e, "Value"));
                String questionText = text(e, "QuestionText");
                String correctAnswer = text(e, "CorrectAnswer").toUpperCase();

                Map<String, String> options = new HashMap<>();
                Element optionsElement = (Element) e.getElementsByTagName("Options").item(0);
                if (optionsElement != null) {
                    options.put("A", text(optionsElement, "OptionA"));
                    options.put("B", text(optionsElement, "OptionB"));
                    options.put("C", text(optionsElement, "OptionC"));
                    options.put("D", text(optionsElement, "OptionD"));
                }

                // Your constructor: (category, value, questionText, options, correctAnswer)
                Question q = new Question(category, value, questionText, options, correctAnswer);

                gameData.addQuestion(q);
            }

            return gameData;

        } catch (Exception e) {
            throw new IOException("Failed to load XML questions", e);
        }
    }

    private String text(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        if (list == null || list.getLength() == 0) return "";
        return list.item(0).getTextContent().trim();
    }
}
