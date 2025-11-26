package com.jeopardy.service;

/**
 * Strategy interface for different scoring algorithms.
 * <p>
 * Implementations define how many points should be gained or lost for a
 * given question value and whether the answer was correct. This allows
 * the scoring behavior to be swapped without changing game logic.
 */
public interface ScoringStrategy {
    
    /**
     * Calculates the score delta for a question attempt.
     *
     * @param questionValue the base value of the question (typically positive)
     * @param isCorrect     {@code true} if the answer is correct,
     *                      {@code false} if it is incorrect
     * @return the number of points to apply:
     *         positive to add, negative to subtract, zero for no change
     */
    int calculateScore(int questionValue, boolean isCorrect);

    /**
     * Returns a human-readable name for this scoring strategy.
     *
     * @return the strategy name (e.g. "Standard Scoring")
     */
    String getStrategyName();
}