package com.jeopardy.service;

import com.jeopardy.model.GameData;
import com.jeopardy.model.Question;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads Jeopardy game data from an XML file.
 *
 * Expected format:
 *
 * <Questions>
 *     <Question>
 *         <Category>Science</Category>
 *         <Value>100</Value>
 *         <QuestionText>What is H2O?</QuestionText>
 *
 *         <Options>
 *             <OptionA>Hydrogen</OptionA>
 *             <OptionB>Oxygen</OptionB>
 *             <OptionC>Water</OptionC>
 *             <OptionD>Helium</OptionD>
 *         </Options>
 *
 *         <CorrectAnswer>C</CorrectAnswer>
 *     </Question>
 * </Questions>
 */
public class XmlGameDataLoader implements GameDataLoader {

    @Override
    public GameData load(Path filePath) throws IOException {

        if (filePath == null) {
            throw new IllegalArgumentException("XML file path cannot be null.");
        }

        if (!Files.exists(filePath)) {
            throw new IOException("XML file does not exist: " + filePath);
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(filePath.toFile());

            doc.getDocumentElement().normalize();

            NodeList questionNodes = doc.getElementsByTagName("Question");

            if (questionNodes == null || questionNodes.getLength() == 0) {
                throw new IOException("No <Question> entries found in XML file.");
            }

            GameData gameData = new GameData();

            for (int i = 0; i < questionNodes.getLength(); i++) {
                Node node = questionNodes.item(i);

                if (node.getNodeType() != Node.ELEMENT_NODE) continue;

                Element elem = (Element) node;

                // -------- Extract Required Fields --------
                String category = getRequiredText(elem, "Category", i);
                int value = getRequiredInt(elem, "Value", i);
                String questionText = getRequiredText(elem, "QuestionText", i);
                String correctAnswer = getRequiredText(elem, "CorrectAnswer", i).toUpperCase();

                if (!"ABCD".contains(correctAnswer)) {
                    throw new IOException("Invalid <CorrectAnswer> '" + correctAnswer
                            + "' in question index " + i + ". Must be A, B, C, or D.");
                }

                // -------- Extract Options --------
                Element optionsElem = getRequiredElement(elem, "Options", i);

                Map<String, String> options = new HashMap<>();
                options.put("A", getRequiredText(optionsElem, "OptionA", i));
                options.put("B", getRequiredText(optionsElem, "OptionB", i));
                options.put("C", getRequiredText(optionsElem, "OptionC", i));
                options.put("D", getRequiredText(optionsElem, "OptionD", i));

                // -------- Create Question Object --------
                Question q = new Question(
                        category,
                        value,
                        questionText,
                        options,
                        correctAnswer
                );

                gameData.addQuestion(q);
            }

            // -------- FINAL VALIDATION --------
            DataValidator.validateCategories(gameData.getCategories());

            return gameData;

        } catch (IOException ex) {
            throw ex;  // preserve clean IO errors
        } catch (Exception e) {
            throw new IOException("Failed to load XML questions: " + e.getMessage(), e);
        }
    }

    // ============= Helper Validation Methods =============

    private String getRequiredText(Element parent, String tag, int index) throws IOException {
        NodeList list = parent.getElementsByTagName(tag);
        if (list == null || list.getLength() == 0) {
            throw new IOException("Missing <" + tag + "> in question index " + index);
        }
        String text = list.item(0).getTextContent().trim();
        if (text.isEmpty()) {
            throw new IOException("Empty <" + tag + "> in question index " + index);
        }
        return text;
    }

    private int getRequiredInt(Element parent, String tag, int index) throws IOException {
        String text = getRequiredText(parent, tag, index);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid numeric value for <" + tag + "> in question index "
                    + index + ": '" + text + "'");
        }
    }

    private Element getRequiredElement(Element parent, String tag, int index) throws IOException {
        NodeList list = parent.getElementsByTagName(tag);
        if (list == null || list.getLength() == 0) {
            throw new IOException("Missing <" + tag + "> element in question index " + index);
        }
        Node n = list.item(0);
        if (n.getNodeType() != Node.ELEMENT_NODE) {
            throw new IOException("<" + tag + "> is not a valid element in question index " + index);
        }
        return (Element) n;
    }
}
