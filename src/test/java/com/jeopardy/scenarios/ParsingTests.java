package com.jeopardy.scenarios;

import com.jeopardy.model.Category;
import com.jeopardy.model.GameData;
import com.jeopardy.model.Question;
import com.jeopardy.service.CsvGameDataLoader;
import com.jeopardy.service.DataValidator;
import com.jeopardy.service.JsonGameDataLoader;
import com.jeopardy.service.XmlGameDataLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Scenario-based parsing tests for:
 *  - CSV / JSON / XML game data loaders
 *  - Validation of malformed inputs and duplicates
 *
 * Aligned with the Parsing Tests section of the JUnit test plan.
 */
public class ParsingTests {

    @TempDir
    Path tempDir;

    // ---------- Helper Methods ----------

    private Path createTempFile(String filename, String content) throws IOException {
        Path path = tempDir.resolve(filename);
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        return path;
    }

    private Map<String, String> sampleOptions() {
        Map<String, String> options = new HashMap<>();
        options.put("A", "Option A");
        options.put("B", "Option B");
        options.put("C", "Option C");
        options.put("D", "Option D");
        return options;
    }

    // ---------- 1. VALID PARSING TESTS ----------

    @Test
    void csvLoader_loadsValidFile() throws IOException {
        String csv =
                "Category,Value,Question,OptionA,OptionB,OptionC,OptionD,CorrectAnswer\n" +
                "Science,100,What is H2O?,Water,Oxygen,Hydrogen,Carbon,A\n" +
                "History,200,Who was the first US president?,George Washington,Abraham Lincoln,John Adams,Thomas Jefferson,A\n";

        Path file = createTempFile("questions.csv", csv);

        CsvGameDataLoader loader = new CsvGameDataLoader();
        GameData data = loader.load(file);

        assertNotNull(data, "GameData should not be null");

        assertEquals(2, data.getTotalCategories(), "There should be 2 categories loaded");
        assertEquals(2, data.getTotalQuestions(), "There should be 2 questions loaded");

        Category science = data.getCategory("Science");
        assertNotNull(science, "Science category should exist");

        // Category exposes a Map<Integer, Question> via getQuestion()
        Question q = science.getQuestion().get(100);
        assertNotNull(q, "Science 100 question should exist");
        assertEquals("What is H2O?", q.getQuestionText());
        assertEquals("Water", q.getOption("A"));
        assertEquals("A", q.getCorrectAnswer());
    }

    @Test
    void jsonLoader_loadsValidFile() throws IOException {
        String json =
                "[\n" +
                "  {\n" +
                "    \"Category\": \"Science\",\n" +
                "    \"Value\": 100,\n" +
                "    \"Question\": \"What is H2O?\",\n" +
                "    \"Options\": {\n" +
                "      \"A\": \"Water\",\n" +
                "      \"B\": \"Oxygen\",\n" +
                "      \"C\": \"Hydrogen\",\n" +
                "      \"D\": \"Helium\"\n" +
                "    },\n" +
                "    \"CorrectAnswer\": \"A\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"Category\": \"Math\",\n" +
                "    \"Value\": 200,\n" +
                "    \"Question\": \"What is 2 + 2?\",\n" +
                "    \"Options\": {\n" +
                "      \"A\": \"3\",\n" +
                "      \"B\": \"4\",\n" +
                "      \"C\": \"5\",\n" +
                "      \"D\": \"22\"\n" +
                "    },\n" +
                "    \"CorrectAnswer\": \"B\"\n" +
                "  }\n" +
                "]\n";

        Path file = createTempFile("questions.json", json);

        JsonGameDataLoader loader = new JsonGameDataLoader();
        GameData data = loader.load(file);

        assertNotNull(data);
        assertEquals(2, data.getTotalCategories());
        assertEquals(2, data.getTotalQuestions());

        Category math = data.getCategory("Math");
        assertNotNull(math);
        Question q = math.getQuestion().get(200);
        assertNotNull(q);
        assertEquals("What is 2 + 2?", q.getQuestionText());
        assertEquals("4", q.getOption("B"));
        assertEquals("B", q.getCorrectAnswer());
    }

    @Test
    void xmlLoader_loadsValidFile() throws IOException {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<JeopardyQuestions>\n" +
                "  <QuestionItem>\n" +
                "    <Category>Science</Category>\n" +
                "    <Value>100</Value>\n" +
                "    <QuestionText>What is H2O?</QuestionText>\n" +
                "    <Options>\n" +
                "      <OptionA>Water</OptionA>\n" +
                "      <OptionB>Oxygen</OptionB>\n" +
                "      <OptionC>Hydrogen</OptionC>\n" +
                "      <OptionD>Helium</OptionD>\n" +
                "    </Options>\n" +
                "    <CorrectAnswer>A</CorrectAnswer>\n" +
                "  </QuestionItem>\n" +
                "  <QuestionItem>\n" +
                "    <Category>History</Category>\n" +
                "    <Value>200</Value>\n" +
                "    <QuestionText>Who was the first US president?</QuestionText>\n" +
                "    <Options>\n" +
                "      <OptionA>George Washington</OptionA>\n" +
                "      <OptionB>Abraham Lincoln</OptionB>\n" +
                "      <OptionC>John Adams</OptionC>\n" +
                "      <OptionD>Thomas Jefferson</OptionD>\n" +
                "    </Options>\n" +
                "    <CorrectAnswer>A</CorrectAnswer>\n" +
                "  </QuestionItem>\n" +
                "</JeopardyQuestions>\n";

        Path file = createTempFile("questions.xml", xml);

        XmlGameDataLoader loader = new XmlGameDataLoader();
        GameData data = loader.load(file);

        assertNotNull(data);
        assertEquals(2, data.getTotalCategories());
        assertEquals(2, data.getTotalQuestions());

        Category history = data.getCategory("History");
        assertNotNull(history);
        Question q = history.getQuestion().get(200);
        assertNotNull(q);
        assertEquals("Who was the first US president?", q.getQuestionText());
        assertEquals("George Washington", q.getOption("A"));
    }

    // ---------- 2. MALFORMED INPUT TESTS ----------

    @Test
    void csvLoader_throwsOnMalformedRow() throws IOException {
        String csv =
                "Category,Value,Question,OptionA,OptionB,OptionC,OptionD,CorrectAnswer\n" +
                // Only 3 columns instead of 8
                "Science,100,Not enough columns\n";

        Path file = createTempFile("bad.csv", csv);

        CsvGameDataLoader loader = new CsvGameDataLoader();

        assertThrows(IOException.class, () -> loader.load(file),
                "Malformed CSV row should cause IOException");
    }

    @Test
    void jsonLoader_throwsOnMalformedJson() throws IOException {
        // Intentionally broken JSON (missing closing bracket)
        String badJson =
                "[\n" +
                "  {\n" +
                "    \"Category\": \"Science\",\n" +
                "    \"Value\": 100,\n" +
                "    \"Question\": \"What is H2O?\",\n" +
                "    \"Options\": { \"A\": \"Water\", \"B\": \"Oxygen\", \"C\": \"Hydrogen\", \"D\": \"Helium\" },\n" +
                "    \"CorrectAnswer\": \"A\"\n" +
                "  }\n";

        Path file = createTempFile("bad.json", badJson);

        JsonGameDataLoader loader = new JsonGameDataLoader();

        assertThrows(IOException.class, () -> loader.load(file),
                "Malformed JSON should cause IOException");
    }

    // ---------- 3. MISSING FIELDS TESTS ----------

    @Test
    void jsonLoader_throwsWhenRequiredFieldMissing() throws IOException {
        // Missing the "Category" field
        String jsonMissingCategory =
                "[\n" +
                "  {\n" +
                "    \"Value\": 100,\n" +
                "    \"Question\": \"What is H2O?\",\n" +
                "    \"Options\": {\n" +
                "      \"A\": \"Water\",\n" +
                "      \"B\": \"Oxygen\",\n" +
                "      \"C\": \"Hydrogen\",\n" +
                "      \"D\": \"Helium\"\n" +
                "    },\n" +
                "    \"CorrectAnswer\": \"A\"\n" +
                "  }\n" +
                "]\n";

        Path file = createTempFile("missing-category.json", jsonMissingCategory);

        JsonGameDataLoader loader = new JsonGameDataLoader();

        IOException ex = assertThrows(IOException.class, () -> loader.load(file),
                "Missing required JSON fields should cause IOException");
        assertTrue(ex.getMessage().toLowerCase().contains("missing"),
                "Error message should mention missing field");
    }

    // ---------- 4. DUPLICATE CATEGORY / QUESTION VALUE VALIDATION ----------

    @Test
    void dataValidator_throwsOnDuplicateCategoryNames() {
        Map<String, String> options = sampleOptions();

        Question q1 = new Question("Science", 100, "Q1", options, "A");
        Question q2 = new Question("Science", 200, "Q2", options, "B");

        Category c1 = new Category("Science");
        c1.addQuestion(q1);

        Category c2 = new Category("Science");
        c2.addQuestion(q2);

        List<Category> categories = Arrays.asList(c1, c2);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> DataValidator.validateCategories(categories),
                "Duplicate category names should be rejected");

        assertTrue(ex.getMessage().contains("Duplicate category name"),
                "Error message should mention duplicate category name");
    }

    @Test
    void dataValidator_throwsOnRepeatedQuestionValueWithinCategory() throws Exception {
        Map<String, String> options = sampleOptions();

        Question q1 = new Question("Science", 100, "Q1", options, "A");
        Question q2 = new Question("Science", 200, "Q2", options, "B");

        Category category = new Category("Science");
        category.addQuestion(q1);
        category.addQuestion(q2);

        // Force q2's value to 100 via reflection so the category
        // has two questions with the same value.
        java.lang.reflect.Field valueField = Question.class.getDeclaredField("value");
        valueField.setAccessible(true);
        valueField.setInt(q2, 100);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> DataValidator.validateCategory(category),
                "Repeated question values within a category should be rejected");

        assertTrue(ex.getMessage().contains("repeated question value"),
                "Error message should mention repeated question value");
    }
}
