package com.jeopardy.scenarios;

import com.jeopardy.model.*;
import com.jeopardy.service.*;

import org.junit.jupiter.api.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full system integration test:
 *  - Builds GameData manually (simulating parsed data)
 *  - Initializes GameController
 *  - Runs a short game flow
 *  - Logs events
 *  - Generates summary report
 *  - Ensures final winners and state are correct
 */
public class FullIntegrationTest {

    private GameController controller;
    private GameData data;

    private static final Path LOG_FILE = Paths.get("logs", "game_event_log.csv");

    @BeforeEach
    void setup() throws IOException {

        // Clear old logs if any
        if (Files.exists(LOG_FILE)) {
            Files.delete(LOG_FILE);
        }
        Files.createDirectories(LOG_FILE.getParent());

        controller = new GameController();
        data = new GameData();

        // Build a 2-category dataset (similar to a real parsed file)
        Map<String, String> opts = Map.of(
                "A", "Correct",
                "B", "Wrong",
                "C", "Wrong",
                "D", "Wrong"
        );

        Category science = new Category("Science");
        science.addQuestion(new Question("Science", 100, "What is H2O?", opts, "A"));
        science.addQuestion(new Question("Science", 200, "Boiling point of water?", opts, "A"));

        Category math = new Category("Math");
        math.addQuestion(new Question("Math", 100, "2 + 2?", opts, "A"));
        math.addQuestion(new Question("Math", 200, "Square root of 16?", opts, "A"));

        data.addCategory(science);
        data.addCategory(math);

        controller.initializeGame(List.of("Alice", "Bob"), data);
    }

    // ---------------------------------------------------------
    // 1. PARSING + INITIALIZATION → GAME IN PROGRESS
    // ---------------------------------------------------------
    @Test
    void gameStartsWithValidData() {
        assertEquals(2, controller.getPlayers().size());
        assertEquals(2, data.getTotalCategories());
        assertEquals(4, data.getTotalQuestions());
        assertEquals("IN_PROGRESS", controller.getGameState().getStatus());
    }

    // ---------------------------------------------------------
    // 2. RUN A SHORT GAME SEQUENCE
    // ---------------------------------------------------------
    @Test
    void fullGameFlowRunsCorrectly() throws IOException {
        // Alice answers 1 question correctly
        assertTrue(controller.answerQuestion("Science", 100, "A"));
        controller.nextPlayer();

        // Bob answers incorrectly
        assertFalse(controller.answerQuestion("Math", 100, "B"));
        controller.nextPlayer();

        // Alice answers another correctly
        assertTrue(controller.answerQuestion("Math", 200, "A"));

        // -----------------------------------------------------
        // LOG FILE MUST CONTAIN ALL EVENTS
        // -----------------------------------------------------
        List<String> lines = Files.readAllLines(LOG_FILE);
        assertTrue(lines.size() >= 4, "Header + 3 answer events expected");

        // Check first event correctness
        String[] cols = lines.get(1).split(",", -1);
        assertEquals("Answer Question", cols[2]);

        // Alice should now be winning
        List<Player> winners = controller.getWinners();
        assertEquals(1, winners.size());
        assertEquals("Alice", winners.get(0).getName());

        // -----------------------------------------------------
        // GENERATE SUMMARY REPORT
        // -----------------------------------------------------
        Path report = controller.generateSummaryReport();
        assertTrue(Files.exists(report));

        String rep = Files.readString(report);

        // Check presence of key items
        assertTrue(rep.contains("JEOPARDY PROGRAMMING GAME REPORT"));
        assertTrue(rep.contains("Players: Alice, Bob"));
        assertTrue(rep.contains("Science")); // category names
        assertTrue(rep.contains("Math"));
        assertTrue(rep.contains("Final Scores"));
        assertTrue(rep.contains("Alice:"));
        assertTrue(rep.contains("Bob:"));
    }

    // ---------------------------------------------------------
    // 3. FULL GAME END CONDITION
    // ---------------------------------------------------------
    @Test
    void gameEndsWhenAllQuestionsAnswered() {
        // 4 questions total
        controller.answerQuestion("Science", 100, "A");
        controller.answerQuestion("Science", 200, "A");
        controller.answerQuestion("Math", 100, "A");
        controller.answerQuestion("Math", 200, "A");

        boolean ended = controller.checkAndEndGame();

        assertTrue(ended);
        assertTrue(controller.isGameFinished());
        assertEquals("FINISHED", controller.getGameState().getStatus());
    }
}
