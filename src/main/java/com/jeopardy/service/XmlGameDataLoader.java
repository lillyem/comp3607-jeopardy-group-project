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
 * Loads Jeopardy-style question data from an XML file.
 * <p>
 * Expected XML structure:
 * <pre>
 * &lt;JeopardyQuestions&gt;
 *     &lt;QuestionItem&gt;
 *         &lt;Category&gt;Science&lt;/Category&gt;
 *         &lt;Value&gt;100&lt;/Value&gt;
 *         &lt;QuestionText&gt;What is H2O?&lt;/QuestionText&gt;
 *         &lt;Options&gt;
 *             &lt;OptionA&gt;Water&lt;/OptionA&gt;
 *             &lt;OptionB&gt;Oxygen&lt;/OptionB&gt;
 *             &lt;OptionC&gt;Hydrogen&lt;/OptionC&gt;
 *             &lt;OptionD&gt;Helium&lt;/OptionD&gt;
 *         &lt;/Options&gt;
 *         &lt;CorrectAnswer&gt;A&lt;/CorrectAnswer&gt;
 *     &lt;/QuestionItem&gt;
 * &lt;/JeopardyQuestions&gt;
 * </pre>
 * <p>
 * Each {@code QuestionItem} is converted into a {@link Question}
 * and inserted into the appropriate {@link Category} within a {@link GameData}.
 */

public class XmlGameDataLoader implements GameDataLoader {

    /**
     * Parses an XML file from the specified path and converts it into a populated
     * {@link GameData} object. Validation includes checking for:
     * <ul>
     *     <li>Presence of required tags (Category, Value, QuestionText, Options, CorrectAnswer)</li>
     *     <li>Valid numeric question values</li>
     *     <li>Presence of all four multiple-choice options (A–D)</li>
     * </ul>
     *
     * @param path the path to the XML file to load
     * @return a fully populated {@link GameData} object
     * @throws IOException if the file cannot be parsed or required fields are missing
     */
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

    /**
     * Utility method for retrieving the text content of the first occurrence
     * of a given tag inside a parent element.
     *
     * @param parent  the element containing the tag
     * @param tagName the name of the tag to read
     * @return the trimmed text inside the tag, or {@code null} if missing
     */
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
