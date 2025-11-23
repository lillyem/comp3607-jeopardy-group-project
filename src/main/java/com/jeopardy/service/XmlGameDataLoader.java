package com.jeopardy.service;

import com.jeopardy.model.Category;
import com.jeopardy.model.GameData;
import com.jeopardy.model.Question;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads Jeopardy questions from an XML file.
 * Expected structure:
 *
 * <JeopardyQuestions>
 *   <QuestionItem>
 *     <Category>...</Category>
 *     <Value>100</Value>
 *     <QuestionText>...</QuestionText>
 *     <Options>
 *       <OptionA>...</OptionA>
 *       <OptionB>...</OptionB>
 *       <OptionC>...</OptionC>
 *       <OptionD>...</OptionD>
 *     </Options>
 *     <CorrectAnswer>A</CorrectAnswer>
 *   </QuestionItem>
 *   ...
 * </JeopardyQuestions>
 */
public class XmlGameDataLoader implements GameDataLoader {

    @Override
    public GameData load(Path path) throws IOException {
        GameData gameData = new GameData();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(path.toFile());
            doc.getDocumentElement().normalize();

            NodeList questionNodes = doc.getElementsByTagName("QuestionItem");
            for (int i = 0; i < questionNodes.getLength(); i++) {
                Node node = questionNodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                Element qElement = (Element) node;

                String categoryName = getTagText(qElement, "Category");
                String valueText = getTagText(qElement, "Value");
                String questionText = getTagText(qElement, "QuestionText");
                if (questionText == null) {
                    // Fallback if XML used <Question> instead
                    questionText = getTagText(qElement, "Question");
                }

                if (categoryName == null || valueText == null || questionText == null) {
                    throw new IOException("Missing required fields in QuestionItem at index " + i);
                }

                int value;
                try {
                    value = Integer.parseInt(valueText.trim());
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid value '" + valueText + "' in QuestionItem for category " +
                            categoryName, e);
                }

                // Read options
                Element optionsElement = (Element) qElement.getElementsByTagName("Options").item(0);
                if (optionsElement == null) {
                    throw new IOException("Missing <Options> element for question: " + questionText);
                }

                String optionA = getTagText(optionsElement, "OptionA");
                String optionB = getTagText(optionsElement, "OptionB");
                String optionC = getTagText(optionsElement, "OptionC");
                String optionD = getTagText(optionsElement, "OptionD");

                Map<String, String> options = new HashMap<String, String>();
                options.put("A", optionA);
                options.put("B", optionB);
                options.put("C", optionC);
                options.put("D", optionD);

                String correctAnswer = getTagText(qElement, "CorrectAnswer");
                if (correctAnswer == null) {
                    throw new IOException("Missing <CorrectAnswer> for question: " + questionText);
                }

                Question question = new Question(
                        categoryName,
                        value,
                        questionText,
                        options,
                        correctAnswer.trim().toUpperCase()
                );

                // Attach to GameData via Category
                Category category = gameData.getCategory(categoryName);
                if (category == null) {
                    category = new Category(categoryName);
                    gameData.addCategory(category);
                }
                category.addQuestion(question);
            }

            return gameData;

        } catch (Exception e) {
            throw new IOException("Error parsing XML file: " + e.getMessage(), e);
        }
    }

    private String getTagText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() == 0) {
            return null;
        }
        Node node = list.item(0);
        if (node == null) {
            return null;
        }
        return node.getTextContent() != null ? node.getTextContent().trim() : null;
    }
}
