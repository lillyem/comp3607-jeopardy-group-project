package com.jeopardy.scenarios;

import com.jeopardy.model.*;
import com.jeopardy.service.GameController;

import org.junit.jupiter.api.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests summary report generation via TextSummaryReportGenerator.
 * Fully aligned with the exact formatting rules inside generate().
 */
public class ReportingTests {

    private GameController controller;
    private Category sci;
    private GameData data;
    private Path reportPath;

    @BeforeEach
    void setup() throws IOException {
        controller = new GameController();
        data = new GameData();

        // Build simple category with 1 question for controlled testing
        Map<String, String> opts = Map.of(
                "A", "Correct",
                "B", "Wrong",
                "C", "Wrong",
                "D", "Wrong"
        );

        sci = new Category("Science");
        sci.addQuestion(new Question("Science", 100, "What is H2O?", opts, "A"));
        data.addCategory(sci);

        controller.initializeGame(List.of("Alice", "Bob"), data);

        // Ensure clean report directory
        Path reportDir = Paths.get("report");
        if (Files.exists(reportDir)) {
            Files.walk(reportDir).filter(Files::isRegularFile).forEach(f -> {
                try { Files.deleteIfExists(f); } catch (IOException ignored) {}
            });
        }
    }

    // -------------------------------------------------------
    // 1. REPORT FILE IS GENERATED
    // -------------------------------------------------------
    @Test
    void reportFileIsCreated() throws IOException {
        controller.answerQuestion("Science", 100, "A");
        reportPath = controller.generateSummaryReport();

        assertNotNull(reportPath);
        assertTrue(Files.exists(reportPath), "Report file must exist");
        assertTrue(reportPath.getFileName().toString().equals("summary_report.txt"));
    }

    // -------------------------------------------------------
    // 2. REPORT CONTAINS CASE ID
    // -------------------------------------------------------
    @Test
    void reportContainsCaseId() throws IOException {
        controller.answerQuestion("Science", 100, "A");
        reportPath = controller.generateSummaryReport();

        String text = Files.readString(reportPath);
        assertTrue(text.contains("Case ID: " + controller.getCaseId()));
    }

    // -------------------------------------------------------
    // 3. CONTAINS PLAYER LIST
    // -------------------------------------------------------
    @Test
    void reportListsPlayers() throws IOException {
        controller.answerQuestion("Science", 100, "A");
        reportPath = controller.generateSummaryReport();

        String text = Files.readString(reportPath);
        assertTrue(text.contains("Players: Alice, Bob"));
    }

    // -------------------------------------------------------
    // 4. TURN-BY-TURN SUMMARY IS CORRECT
    // -------------------------------------------------------
    @Test
    void reportContainsTurnSummary() throws IOException {
        controller.answerQuestion("Science", 100, "A");

        reportPath = controller.generateSummaryReport();
        String text = Files.readString(reportPath);

        assertTrue(text.contains("Gameplay Summary:"));
        assertTrue(text.contains("Turn 1: Alice selected Science for 100 pts"));
        assertTrue(text.contains("Question: What is H2O?"));
        assertTrue(text.contains("Answer: Correct — Correct (+100 pts)"));
        assertTrue(text.contains("Score after turn: Alice = 100"));
    }

    // -------------------------------------------------------
    // 5. FINAL SCORES SECTION
    // -------------------------------------------------------
    @Test
    void reportShowsFinalScores() throws IOException {
        controller.answerQuestion("Science", 100, "A");

        reportPath = controller.generateSummaryReport();
        String text = Files.readString(reportPath);

        assertTrue(text.contains("Final Scores:"));
        assertTrue(text.contains("Alice: 100"));
        assertTrue(text.contains("Bob: 0"));
    }

    // -------------------------------------------------------
    // 6. TIE MESSAGE WHEN MULTIPLE WINNERS
    // -------------------------------------------------------
    @Test
    void reportShowsTieMessage() throws IOException {
        // Make both players equal score
        controller.answerQuestion("Science", 100, "A"); // Alice +100
        controller.nextPlayer();
        controller.getCurrentPlayer().addPoints(100);   // Bob manual tie

        reportPath = controller.generateSummaryReport();
        String text = Files.readString(reportPath);

        assertTrue(text.contains("It's a tie! Winners:"), "Tie message must be present when scores equal");
        assertTrue(text.contains("Alice (100 points)"));
        assertTrue(text.contains("Bob (100 points)"));
    }
}
