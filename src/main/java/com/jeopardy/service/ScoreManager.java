package com.jeopardy.service;

import com.jeopardy.model.Player;

/**
 * Coordinates scoring for players using the Strategy pattern.
 * <p>
 * A {@link ScoringStrategy} implementation is used to determine how
 * many points to add or subtract for a given question outcome. This
 * allows scoring rules to be swapped at runtime (e.g. standard vs.
 * double-points mode).
 */
public class ScoreManager {

    /** Current scoring strategy used to evaluate answers. */
    private ScoringStrategy scoringStrategy;
    
    /**
     * Creates a {@code ScoreManager} with the default
     * {@link StandardScoringStrategy}.
     */
    public ScoreManager() {
        this.scoringStrategy = new StandardScoringStrategy(); // Default strategy
    }
    
    /**
     * Creates a {@code ScoreManager} with an explicit scoring strategy.
     *
     * @param scoringStrategy the strategy to use for all score calculations
     */
    public ScoreManager(ScoringStrategy scoringStrategy) {
        this.scoringStrategy = scoringStrategy;
    }
    
    /**
     * Sets the scoring strategy used to compute score changes.
     *
     * @param strategy the new {@link ScoringStrategy} to use.
     *                 If {@code null} is passed, the current strategy is unchanged.
     */
    public void setScoringStrategy(ScoringStrategy strategy) {
        if (strategy != null) {
            this.scoringStrategy = strategy;
        }
    }
    
    /**
     * Returns the currently active scoring strategy.
     *
     * @return the {@link ScoringStrategy} in use
     */
    public ScoringStrategy getScoringStrategy() {
        return scoringStrategy;
    }
    
     /**
     * Updates a player's score based on the given question value and result.
     * <p>
     * The actual number of points to add or subtract is delegated to the
     * configured {@link ScoringStrategy}. Positive results call
     * {@link Player#addPoints(int)}; negative results call
     * {@link Player#subtractPoints(int)}. A zero result leaves the score unchanged.
     *
     * @param player        the player whose score should be updated
     * @param questionValue the value of the question (typically positive)
     * @param isCorrect     {@code true} if the answer was correct, {@code false} otherwise
     * @throws IllegalArgumentException if {@code player} is {@code null}
     */
    public void updateScore(Player player, int questionValue, boolean isCorrect) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        
        int points = scoringStrategy.calculateScore(questionValue, isCorrect);
        
        if (points > 0) {
            player.addPoints(points);
        } else if (points < 0) {
            player.subtractPoints(Math.abs(points));
        }
        // If points == 0, no change
    }
    
    /**
     * Calculates the score delta that would be applied for the given
     * question value and correctness, without modifying any player.
     *
     * @param questionValue the value of the question
     * @param isCorrect     {@code true} if the hypothetical answer is correct
     * @return the number of points that would be added (positive),
     *         subtracted (negative), or zero
     */
    public int calculatePotentialScore(int questionValue, boolean isCorrect) {
        return scoringStrategy.calculateScore(questionValue, isCorrect);
    }
    
    /**
     * Returns the human-readable name of the active scoring strategy.
     *
     * @return the strategy name reported by {@link ScoringStrategy#getStrategyName()}
     */
    public String getScoringStrategyName() {
        return scoringStrategy.getStrategyName();
    }
}