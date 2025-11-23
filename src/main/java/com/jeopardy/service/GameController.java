package com.jeopardy.service;

import java.util.List;
import java.util.ArrayList;   // NEW

import com.jeopardy.model.Category;
import com.jeopardy.model.GameData;
import com.jeopardy.model.GameState;
import com.jeopardy.model.Player;
import com.jeopardy.model.Question;
import com.jeopardy.model.GameEvent;
import java.time.Instant;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Main controller that orchestrates the entire game flow
 */
public class GameController {
    private Game game;
    private ScoreManager scoreManager;
    private GameData gameData;

    private GameEventLogger eventLogger;
    private SummaryReportGenerator reportGenerator;

    // NEW: keep in-memory list of events for the summary report
    private final List<GameEvent> gameEvents = new ArrayList<>();

    public GameController() {
        this(new TextSummaryReportGenerator(), new CsvGameEventLogger());
    }

    public GameController(SummaryReportGenerator reportGenerator,
                          GameEventLogger eventLogger) {
        this.game = new Game();
        this.scoreManager = new ScoreManager();
        this.reportGenerator = reportGenerator;
        this.eventLogger = eventLogger;
    }

    /**
     * Initialize the game with players and data
     */
    public void initializeGame(List<String> playerNames, GameData gameData) {
        if (playerNames == null || playerNames.isEmpty()) {
            throw new IllegalArgumentException("Player names cannot be null or empty");
        }
        if (gameData == null || gameData.isEmpty()) {
            throw new IllegalArgumentException("Game data cannot be null or empty");
        }

        this.game = new Game();
        this.scoreManager = new ScoreManager();

        // Add players
        for (String name : playerNames) {
            game.addPlayer(name.trim());
        }

        // Set game data
        this.gameData = gameData;
        game.setCategories(gameData.getCategories());

        // Start the game
        game.startGame();

        // Log game start (no question text)
        logEvent("GAME_STARTED", null, null, null, null, null);
    }

    /**
     * Handle a player selecting and answering a question
     */
    public boolean answerQuestion(String categoryName, int questionValue, String playerAnswer) {
        if (game.getState() != GameState.IN_PROGRESS) {
            throw new IllegalStateException("Game is not in progress");
        }

        Player currentPlayer = game.getCurrentPlayer();
        Question question = game.getQuestion(categoryName, questionValue);

        if (question == null) {
            throw new IllegalArgumentException("Question not found: " + categoryName + " - " + questionValue);
        }
        if (question.isAnswered()) {
            throw new IllegalStateException("Question has already been answered");
        }
        if (playerAnswer == null || playerAnswer.trim().isEmpty()) {
            throw new IllegalArgumentException("Answer cannot be null or empty");
        }

        // Check if answer is correct
        boolean isCorrect = question.isCorrect(playerAnswer);

        // Update score
        scoreManager.updateScore(currentPlayer, questionValue, isCorrect);

        // Mark question as answered
        question.setAnswered(true);

        // Log the question result WITH question text
        logEvent(
                "QUESTION_ANSWERED",
                currentPlayer,
                categoryName,
                questionValue,
                playerAnswer,
                isCorrect ? "CORRECT" : "INCORRECT",
                question.getQuestionText() // NEW
        );

        // Move to next turn
        game.nextTurn();

        return isCorrect;
    }

    /**
     * Check if game should end and end it if conditions are met
     */
    public boolean checkAndEndGame() {
        if (!game.hasAvailableQuestions() || game.allQuestionsAnswered()) {
            game.endGame();

            // Log game finish
            logEvent("GAME_FINISHED", null, null, null, null, null);

            return true;
        }
        return false;
    }

    // NEW: force end from "End Game" button
    public void forceEndGame() {
        if (!isGameFinished()) {
            game.endGame();
            logEvent("GAME_FINISHED", null, null, null, null, null);
        }
    }

    // Getters for UI and other components
    public Game getGame() { return game; }
    public ScoreManager getScoreManager() { return scoreManager; }
    public GameData getGameData() { return gameData; }
    public Player getCurrentPlayer() { return game.getCurrentPlayer(); }
    public List<Player> getPlayers() { return game.getPlayers(); }
    public List<Category> getCategories() { return game.getCategories(); }
    public GameState getGameState() { return game.getState(); }
    public boolean isGameFinished() { return game.isGameFinished(); }
    public Player getWinner() { return game.getWinner(); }

    // NEW: expose immutable view of events for the report generator
    public List<GameEvent> getGameEvents() {
        return List.copyOf(gameEvents);
    }

    /**
     * Get game summary (no longer used by report but still useful elsewhere)
     */
    public String getGameSummary() {
        return String.format(
            "Game ID: %s\nPlayers: %d\nTotal Turns: %d\nGame State: %s\nScoring: %s",
            game.getGameId(),
            game.getPlayerCount(),
            game.getTotalTurns(),
            game.getState(),
            scoreManager.getScoringStrategyName()
        );
    }

    // ORIGINAL convenience signature
    private void logEvent(String activity,
                          Player player,
                          String category,
                          Integer questionValue,
                          String answerGiven,
                          String result) {
        logEvent(activity, player, category, questionValue, answerGiven, result, null);
    }

    // NEW: master logger with questionText
    private void logEvent(String activity,
                          Player player,
                          String category,
                          Integer questionValue,
                          String answerGiven,
                          String result,
                          String questionText) {

        if (eventLogger == null && gameEvents == null) {
            return;
        }

        GameEvent event = new GameEvent.Builder(
                game.getGameId(),          // caseId
                activity                   // activity
        )
                .playerId(player != null ? player.getPlayerId() : null)
                .timestamp(Instant.now())
                .category(category)
                .questionValue(questionValue)
                .answerGiven(answerGiven)
                .result(result)
                .scoreAfterPlay(player != null ? player.getScore() : null)
                .questionText(questionText)    // NEW
                .build();

        if (eventLogger != null) {
            eventLogger.logEvent(event);
        }
        // Always keep in-memory copy for reporting
        gameEvents.add(event);
    }

    public Path generateSummaryReport() throws IOException {
        if (reportGenerator == null) {
            throw new IllegalStateException("No report generator configured.");
        }
        return reportGenerator.generate(this);
    }
}
