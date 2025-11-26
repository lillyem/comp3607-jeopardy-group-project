package com.jeopardy.service;

import com.jeopardy.model.GameState;
import com.jeopardy.model.Player;
import com.jeopardy.model.Question;
import com.jeopardy.model.GameEvent;
import java.util.List;

/**
 * High-level game controller that manages:
 * <ul>
 *     <li>Game state (players, questions, status)</li>
 *     <li>Event logging via {@link GameEventLogger}</li>
 *     <li>Summary report generation</li>
 * </ul>
 *
 * This class provides a simple wrapper around {@link GameState} and ties
 * together core actions such as starting the game, answering questions,
 * determining winners, and ending the session.
 */
public class Game {

    /** Holds players, questions, turn order, and game status. */
    private GameState gameState;

    /** Logs all gameplay events for analysis. */
    private GameEventLogger eventLogger;

    /** Generates a summary report when the game ends. */
    private SummaryReportGenerator reportGenerator;

    /** Unique identifier (Case ID) used for all logged events. */
    private String caseId;
    
    /**
     * Creates a new game instance with:
     * <ul>
     *     <li>Fresh {@link GameState}</li>
     *     <li>A unique case ID based on system time</li>
     *     <li>CSV event logger</li>
     *     <li>Text-based summary report generator</li>
     * </ul>
     */
    public Game() {
        this.gameState = new GameState();
        this.caseId = "GAME" + System.currentTimeMillis();
        this.eventLogger = new CsvGameEventLogger(caseId);
        this.reportGenerator = new TextSummaryReportGenerator();
    }
    
    /** @return the active {@link GameState} instance for this session. */
    public GameState getGameState() {
        return gameState;
    }
    
    /**
     * Begins the game by setting state to {@code IN_PROGRESS}
     * and logging a "Start Game" event.
     */
    public void startGame() {
        gameState.setStatus(GameState.IN_PROGRESS);
        GameEvent event = new GameEvent.Builder(caseId, "Start Game")
            .result("Success")
            .build();
        eventLogger.logEvent(event);
    }
    
    /**
     * Loads all questions into the {@link GameState} and logs the action.
     *
     * @param questions list of question objects to attach to the game
     */
    public void loadQuestions(List<Question> questions) {
        gameState.setQuestions(questions);
        GameEvent event = new GameEvent.Builder(caseId, "Load Questions")
            .result("Success")
            .build();
        eventLogger.logEvent(event);
    }
    
    /**
     * Adds a player to the game and logs a "Join Game" event.
     *
     * @param player the player joining the session
     */
    public void addPlayer(Player player) {
        gameState.addPlayer(player);
        GameEvent event = new GameEvent.Builder(caseId, "Join Game")
            .playerId(player.getPlayerId())
            .result("Success")
            .build();
        eventLogger.logEvent(event);
    }
    
    /**
     * Processes a player's answer to a question.
     * <p>
     * Updates:
     * <ul>
     *     <li>Player score (only if correct)</li>
     *     <li>Marked state of the question</li>
     * </ul>
     * Also logs an "Answer Question" event including:
     * category, value, selected answer, result, and updated score.
     *
     * @param player the player answering
     * @param question the question being answered
     * @param answer the player's chosen option (A/B/C/D)
     * @return {@code true} if the answer was correct
     */
    public boolean answerQuestion(Player player, Question question, String answer) {
        boolean isCorrect = question.getCorrectAnswer().equals(answer);
        int oldScore = player.getScore();
        
        if (isCorrect) {
            player.setScore(oldScore + question.getValue());
            question.setAnswered(true);
        }
        
        GameEvent event = new GameEvent.Builder(caseId, "Answer Question")
            .playerId(player.getPlayerId())
            .category(question.getCategory())
            .questionValue(question.getValue())
            .answerGiven(answer)
            .result(isCorrect ? "Correct" : "Incorrect")
            .scoreAfterPlay(player.getScore())
            .build();
        eventLogger.logEvent(event);
        
        return isCorrect;
    }
    
    /**
     * Computes and returns the list of players with the highest score.
     * Supports tie scenarios.
     *
     * @return list of winner(s)
     */
    public List<Player> getWinners() {
        return gameState.determineWinners();
    }
    
    /**
     * Ends the game by:
     * <ul>
     *     <li>Setting status to {@code FINISHED}</li>
     *     <li>Logging a "Game End" event with the final outcome</li>
     *     <li>Generating a summary report</li>
     * </ul>
     */
    public void endGame() {
        gameState.setStatus(GameState.FINISHED);
        
        List<Player> winners = getWinners();
        String result;
        
        if (winners.isEmpty()) {
            result = "No winners";
        } else if (winners.size() == 1) {
            result = "Winner: " + winners.get(0).getName();
        } else {
            result = "Tie: " + winners.stream()
                    .map(Player::getName)
                    .collect(java.util.stream.Collectors.joining(", "));
        }
        
        GameEvent event = new GameEvent.Builder(caseId, "Game End")
            .result(result)
            .build();
        eventLogger.logEvent(event);
        
        generateReport();
    }
    
    /**
     * Generates the post-game summary report.
     * <p>
     * NOTE: This currently constructs a new {@link GameController},
     * which may or may not reflect the intended final design.
     * Exceptions are printed to stderr.
     */
    private void generateReport() {
        try {
            reportGenerator.generate(new GameController());
        } catch (Exception e) {
            System.err.println("Error generating report: " + e.getMessage());
        }
    }
    
    /** @return true if the top score is shared by two or more players. */
    public boolean isTie() {
        return gameState.isTie();
    }
    
    /** @return a formatted message summarizing the winning state or tie. */
    public String getGameResult() {
        return gameState.getGameResult();
    }
    
     /** @return the player whose turn it currently is. */
    public Player getCurrentPlayer() {
        return gameState.getCurrentPlayer();
    }
    
    /** Advances the turn to the next player in the rotation. */
    public void nextPlayer() {
        gameState.nextPlayer();
    }
}