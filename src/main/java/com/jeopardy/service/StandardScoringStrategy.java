package com.jeopardy.service;

/**
 * Default Jeopardy-style scoring strategy:
 * <ul>
 *     <li>Correct answer → <strong>+value</strong></li>
 *     <li>Incorrect answer → <strong>-value</strong></li>
 * </ul>
 * This strategy is used by default in {@link ScoreManager}.
 */

public class StandardScoringStrategy implements ScoringStrategy {
    
    /**
     * Applies the standard Jeopardy scoring rule.
     *
     * @param questionValue the base point value of the question
     * @param isCorrect     whether the player's answer was correct
     * @return +value if correct, -value if incorrect
     */
    @Override
    public int calculateScore(int questionValue, boolean isCorrect) {
        return isCorrect ? questionValue : -questionValue;
    }
    
    /**
     * Returns the display name of this scoring strategy.
     *
     * @return the string "Standard Scoring"
     */
    @Override
    public String getStrategyName() {
        return "Standard Scoring";
    }
}