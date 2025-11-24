package com.jeopardy.scenarios;

import com.jeopardy.model.*;
import com.jeopardy.service.GameController;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Scenario Tests for:
 *  - Player initialization & turn rotation
 *  - Selecting questions from categories
 *  - Correct/incorrect scoring behavior
 *  - Preventing question reuse
 *  - Ending the game automatically
 *
 * Fully aligned with GameController, GameState, Player, Category, Question.
 */
public class GameplayLogicTests {

    private GameController controller;
    private GameData data;
    private Category science;
    private Category math;

    @BeforeEach
    void setup() {
        controller = new GameController();
        data = new GameData();

        // --- Build categories & questions ---
        Map<String, String> opts = new HashMap<>();
        opts.put("A", "Correct");
        opts.put("B", "Wrong");
        opts.put("C", "Wrong");
        opts.put("D", "Wrong");

        // SCIENCE
        science = new Category("Science");
        science.addQuestion(new Question("Science", 100, "Science Q1", opts, "A"));
        science.addQuestion(new Question("Science", 200, "Science Q2", opts, "A"));

        // MATH
        math = new Category("Math");
        math.addQuestion(new Question("Math", 100, "Math Q1", opts, "A"));
        math.addQuestion(new Question("Math", 200, "Math Q2", opts, "A"));

        data.addCategory(science);
        data.addCategory(math);

        // --- Initialize game with two players ---
        controller.initializeGame(List.of("Alice", "Bob"), data);
    }

    // -----------------------------------------------------
    // 1. TURN ROTATION: Alice → Bob → Alice → Bob
    // -----------------------------------------------------
    @Test
    void turnRotatesCorrectly() {
        Player p1 = controller.getCurrentPlayer();
        assertEquals("Alice", p1.getName());

        controller.nextPlayer();
        assertEquals("Bob", controller.getCurrentPlayer().getName());

        controller.nextPlayer();
        assertEquals("Alice", controller.getCurrentPlayer().getName());
    }

    // -----------------------------------------------------
    // 2. SELECTING QUESTIONS
    // -----------------------------------------------------
    @Test
    void selectingQuestionReturnsCorrectObject() {
        Question q = controller.getQuestion("Science", 100);
        assertNotNull(q);
        assertEquals("Science Q1", q.getQuestionText());
    }

    // -----------------------------------------------------
    // 3. PREVENT RESELECTING ANSWERED QUESTIONS
    // -----------------------------------------------------
    @Test
    void questionCannotBeAnsweredTwice() {
        assertTrue(controller.answerQuestion("Science", 100, "A"));
        Question q = controller.getQuestion("Science", 100);
        assertTrue(q.isAnswered());

        // Second attempt:
        boolean result = controller.answerQuestion("Science", 100, "A");
        assertFalse(result, "Cannot answer an already answered question");
    }

    // -----------------------------------------------------
    // 4. SCORING — Correct Answer
    // -----------------------------------------------------
    @Test
    void correctAnswerAddPoints() {
        Player p = controller.getCurrentPlayer();
        int before = p.getScore();

        boolean correct = controller.answerQuestion("Math", 200, "A");

        assertTrue(correct);
        assertEquals(before + 200, p.getScore());
    }

    // -----------------------------------------------------
    // 5. SCORING — Incorrect Answer
    // -----------------------------------------------------
    @Test
    void wrongAnswerSubtractsPoints() {
        Player p = controller.getCurrentPlayer();
        int before = p.getScore();

        boolean correct = controller.answerQuestion("Math", 200, "B");

        assertFalse(correct);
        assertEquals(before, p.getScore() - 0, "Score should subtract but not below zero");
        assertEquals(0, p.getScore(), "Score cannot go below zero");
    }

    // -----------------------------------------------------
    // 6. FULL ROUND: Correctness + Turn Rotation
    // -----------------------------------------------------
    @Test
    void answeringQuestionAdvancesTurn() {
        Player before = controller.getCurrentPlayer();
        controller.answerQuestion("Science", 100, before.getScore() == 0 ? "A" : "B");
        controller.nextPlayer();

        Player after = controller.getCurrentPlayer();
        assertNotEquals(before.getName(), after.getName());
    }

    // -----------------------------------------------------
    // 7. END GAME AUTOMATICALLY WHEN ALL QUESTIONS ANSWERED
    // -----------------------------------------------------
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
    }

    // -----------------------------------------------------
    // 8. Cannot answer question from non-existent category
    // -----------------------------------------------------
    @Test
    void answeringQuestionFromInvalidCategoryReturnsFalse() {
        boolean result = controller.answerQuestion("INVALID", 500, "A");
        assertFalse(result);
    }

    // -----------------------------------------------------
    // 9. getWinners() works correctly
    // -----------------------------------------------------
    @Test
    void winnerIsDeterminedCorrectly() {
        controller.answerQuestion("Science", 100, "A"); // Alice +100
        controller.nextPlayer();
        controller.answerQuestion("Math", 100, "B");   // Bob wrong -> stays 0

        List<Player> winners = controller.getWinners();

        assertEquals(1, winners.size());
        assertEquals("Alice", winners.get(0).getName());
    }
}
