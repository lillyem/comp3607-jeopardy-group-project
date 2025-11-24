package com.jeopardy.scenarios;

import com.jeopardy.model.Player;
import com.jeopardy.service.ScoreManager;
import com.jeopardy.service.ScoringStrategy;
import com.jeopardy.service.StandardScoringStrategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the scoring system independently of gameplay.
 * Covers:
 *  - Standard scoring strategy
 *  - ScoreManager update logic
 *  - Non-negative scores
 *  - calculatePotentialScore()
 *  - Switching scoring strategies (Strategy pattern)
 */
public class ScoringTests {

    private ScoreManager scoreManager;
    private Player p;

    @BeforeEach
    void setup() {
        scoreManager = new ScoreManager(); // Uses StandardScoringStrategy by default
        p = new Player("P1", "Alice");
    }

    // -----------------------------------------------------
    // 1. CORRECT ANSWER → +value
    // -----------------------------------------------------
    @Test
    void correctAnswerAddsPoints() {
        int before = p.getScore();

        scoreManager.updateScore(p, 200, true);

        assertEquals(before + 200, p.getScore());
    }

    // -----------------------------------------------------
    // 2. INCORRECT ANSWER → -value (but not below zero)
    // -----------------------------------------------------
    @Test
    void wrongAnswerSubtractsPoints() {
        p.setScore(300);

        scoreManager.updateScore(p, 200, false);

        assertEquals(100, p.getScore());
    }

    @Test
    void scoreDoesNotGoNegative() {
        p.setScore(100);

        scoreManager.updateScore(p, 200, false);

        assertEquals(0, p.getScore(), "Score must never go below zero");
    }

    // -----------------------------------------------------
    // 3. POTENTIAL SCORE CALCULATION
    // -----------------------------------------------------
    @Test
    void calculatePotentialScoreWorks() {
        int potentialCorrect = scoreManager.calculatePotentialScore(300, true);
        int potentialWrong = scoreManager.calculatePotentialScore(300, false);

        assertEquals(300, potentialCorrect);
        assertEquals(-300, potentialWrong);
    }

    // -----------------------------------------------------
    // 4. STRATEGY PATTERN SUPPORT
    // -----------------------------------------------------
    @Test
    void scoreManagerUsesInjectedStrategy() {

        // Custom mock strategy for testing
        ScoringStrategy doubleStrategy = new ScoringStrategy() {
            @Override
            public int calculateScore(int value, boolean correct) {
                if (correct) return value * 2;
                return -50;
            }

            @Override
            public String getStrategyName() {
                return "DoublePoints";
            }
        };

        scoreManager.setScoringStrategy(doubleStrategy);

        p.setScore(0);

        // Correct answer should give double
        scoreManager.updateScore(p, 100, true);
        assertEquals(200, p.getScore());

        // Wrong answer should subtract 50 (but not below zero)
        scoreManager.updateScore(p, 100, false);
        assertEquals(150, p.getScore());

        assertEquals("DoublePoints", scoreManager.getScoringStrategyName());
    }

    // -----------------------------------------------------
    // 5. STANDARD STRATEGY NAME CHECK
    // -----------------------------------------------------
    @Test
    void standardStrategyReportsCorrectName() {
        StandardScoringStrategy strat = new StandardScoringStrategy();
        assertEquals("Standard Scoring", strat.getStrategyName());
    }
}
