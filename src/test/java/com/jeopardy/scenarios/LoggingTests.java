package com.jeopardy.scenarios;

import com.jeopardy.model.*;
import com.jeopardy.service.GameController;

import org.junit.jupiter.api.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests CSV event logging via CsvGameEventLogger.
 */
public class LoggingTests {

    private GameController controller;
    private GameData data;

    private static final Path LOG_FILE = Paths.get("logs", "game_event_log.csv");

    @BeforeEach
    void setup() throws IOException {

        // Delete any previous log file so tests start clean
        if (Files.exists(LOG_FILE)) {
            Files.delete(LOG_FILE);
        }
        Files.createDirectories(LOG_FILE.getParent());

        controller = new GameController();
        data = new GameData();

        // Build categories & questions
        Map<String, String> opts = Map.of(
                "A", "Correct",
                "B", "Wrong",
                "C", "Wrong",
                "D", "Wrong"
        );

        Category sci = new Category("Science");
        sci.addQuestion(new Question("Science", 100, "What is H2O?", opts, "A"));
        data.addCategory(sci);

        controller.initializeGame(List.of("Alice", "Bob"), data);
    }

    // -------------------------------------------------------
    @Test
    void logFileIsCreatedAfterFirstEvent() throws IOException {
        controller.answerQuestion("Science", 100, "A");

        assertTrue(Files.exists(LOG_FILE), "The CSV log file must exist after logging events");
    }

    // -------------------------------------------------------
    @Test
    void logFileContainsCorrectHeader() throws IOException {
        controller.answerQuestion("Science", 100, "A");

        List<String> lines = Files.readAllLines(LOG_FILE);
        assertFalse(lines.isEmpty());

        assertEquals(
                "Case_ID,Player_ID,Activity,Timestamp,Category,Question_Value,Answer_Given,Result,Score_After_Play",
                lines.get(0)
        );
    }

    // -------------------------------------------------------
    @Test
    void answerQuestionEventIsLoggedCorrectly() throws IOException {
        controller.answerQuestion("Science", 100, "A");

        List<String> lines = Files.readAllLines(LOG_FILE);
        assertTrue(lines.size() >= 2);

        String[] cols = lines.get(1).split(",", -1);

        assertEquals(9, cols.length);

        assertEquals(controller.getCaseId(), cols[0]);  // Case_ID
        assertEquals("Alice", cols[1]);                 // Player_ID
        assertEquals("Answer Question", cols[2]);       // Activity
        assertFalse(cols[3].isBlank());                 // Timestamp
        assertEquals("Science", cols[4]);               // Category
        assertEquals("100", cols[5]);                   // Question_Value
        assertEquals("Correct", cols[6]);               // Answer_Given
        assertEquals("Correct", cols[7]);               // Result
        assertEquals("100", cols[8]);                   // Score_After_Play
    }

    // -------------------------------------------------------
    @Test
    void multipleEventsLoggedInCorrectOrder() throws IOException {
        controller.answerQuestion("Science", 100, "A");
        controller.nextPlayer();
        controller.systemEvent("Start Game", null, null, "Success");

        List<String> lines = Files.readAllLines(LOG_FILE);
        assertTrue(lines.size() >= 3);

        // First logged event after header must be Answer Question
        assertTrue(lines.get(1).contains("Answer Question"));
        // Second event should be system event
        assertTrue(lines.get(2).contains("Start Game"));
    }

    // -------------------------------------------------------
    @Test
    void secondAttemptOnAnsweredQuestionDoesNotCreateDuplicateEvents() throws IOException {
        controller.answerQuestion("Science", 100, "A");
        controller.answerQuestion("Science", 100, "A"); // should NOT produce new event

        List<String> lines = Files.readAllLines(LOG_FILE);

        assertEquals(2, lines.size(), "Only one answer event + header expected");
    }

    // -------------------------------------------------------
    @Test
    void systemEventIsLoggedCorrectly() throws IOException {
        controller.systemEvent("Start Game", null, null, "Success");

        List<String> lines = Files.readAllLines(LOG_FILE);

        String[] cols = lines.get(1).split(",", -1);

        assertEquals("System", cols[1]);          // Player_ID
        assertEquals("Start Game", cols[2]);      // Activity
        assertEquals("Success", cols[7]);         // Result
    }
}
