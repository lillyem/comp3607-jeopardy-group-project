package com.jeopardy.service;

import com.jeopardy.model.Category;
import com.jeopardy.model.GameData;
import com.jeopardy.model.GameEvent;
import com.jeopardy.model.GameState;
import com.jeopardy.model.Player;
import com.jeopardy.model.Question;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Central controller responsible for managing all gameplay operations in
 * the Jeopardy application. This class coordinates:
 * <ul>
 *     <li>Game state management (players, turn order, status)</li>
 *     <li>Loading and accessing questions and categories</li>
 *     <li>Answer evaluation and scoring</li>
 *     <li>Event creation and logging through {@link GameEventLogger}</li>
 *     <li>Summary report generation</li>
 * </ul>
 *
 * It serves as the primary API used by the UI layer and the integration tests.
 */
public class GameController {
    /** Encapsulates players, questions, and game status/turn order. */
    private GameState gameState;

    /** Container holding categories and questions parsed from files. */
    private GameData gameData;

    /** Local list of gameplay events used for report generation. */
    private final List<GameEvent> gameplayEvents = new ArrayList<>();

    /** Logger responsible for writing gameplay events to CSV. */
    private GameEventLogger eventLogger;

    /** Unique identifier for the current game session (Case ID). */
    private String caseId;

    /** Generates text-based summary reports at the end of the game. */
    private final SummaryReportGenerator reportGenerator = new TextSummaryReportGenerator();

    /**
     * Constructs a new {@code GameController}:
     * <ul>
     *     <li>Initializes a fresh {@link GameState}</li>
     *     <li>Generates a unique case ID based on the clock</li>
     *     <li>Creates a CSV event logger tied to this case</li>
     * </ul>
     */
    public GameController() {
        this.gameState = new GameState();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HHmmss");
        this.caseId = "GAME" + fmt.format(LocalDateTime.now());


        // CSV logger writes to game_event_log.csv
        this.eventLogger = new CsvGameEventLogger(caseId);
    }
    
    /** @return list of recorded gameplay events */
    public List<GameEvent> getGameplayEvents() {
        return gameplayEvents;
    }

    /** @return the session's unique Case ID */
    public String getCaseId() {
        return caseId;
    }

    /** @return the current {@link GameState} backing the controller */
    public GameState getGameState() {
        return gameState;
    }
    
    /** @return list of all players in the game */
    public List<Player> getPlayers() {
        return gameState.getPlayers();
    }
    
    /** @return the player whose turn it currently is */
    public Player getCurrentPlayer() {
        return gameState.getCurrentPlayer();
    }
    
    /** @return list of loaded categories or empty list if none loaded */
    public List<Category> getCategories() {
        return gameData != null ? gameData.getCategories() : List.of();
    }
    
    /** @return reference to this controller (used in report generator) */
    public Object getGame() {
        return this;
    }
    
    /**
     * Retrieves a question matching the given category and point value.
     *
     * @param categoryName the category label
     * @param value        the question's point value
     * @return the matching {@link Question}, or {@code null} if not found
     */
    public Question getQuestion(String categoryName, int value) {
        if (gameData == null) return null;
        for (Category category : gameData.getCategories()) {
            if (category.getName().equals(categoryName)) {
                for (Question question : category.getAllQuestions()) {
                    if (question.getValue() == value) {
                        return question;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Logs a system-level or UI-driven event (e.g., selecting categories,
     * choosing players, loading files). The formatting rules implemented here
     * are designed to match the required CSV event log specification.
     *
     * @param activity description of the event (e.g., "Start Game")
     * @param category optional category affected by event
     * @param value    optional question value
     * @param extra    optional supplemental detail (e.g., chosen name or count)
     */
    public void systemEvent(String activity, String category, Integer value, String extra) {
        if (eventLogger == null) {
            return;
        }

        GameEvent.Builder builder = new GameEvent.Builder(caseId, activity);

        switch (activity) {
        // Purely system-level events (no player)
        case "Load File":
        case "Start Game":
        case "Generate Report":
        case "Generate Event Log":
        case "Exit Game":
            builder.playerId("System");
            if (category != null && !category.isBlank()) {
                builder.category(category);
            }
            if (value != null) {
                builder.questionValue(value);
            }

            // If caller passes an explicit result (e.g. "Success"), use it.
            if (extra != null && !extra.isBlank()) {
                builder.result(extra);
            } else {
                // Otherwise default to "N/A" to match the sample log.
                builder.result("N/A");
            }
            break;

            
            case "Select Player Count":
                builder.playerId("System");
                if (extra != null && !extra.isBlank()) {
                    builder.answerGiven(extra); // the number of players, e.g. "2"
                }
                builder.result("N/A");
                break;

        
            case "Enter Player Name":
                String name = (extra != null) ? extra : "";
                builder.playerId(name);
                if (!name.isEmpty()) {
                    builder.answerGiven(name);
                }
                builder.result("N/A");
                break;

            
            case "Select Category":
            case "Select Question":
                Player current = getCurrentPlayer();
                if (current != null) {
                    builder.playerId(current.getName());
                    builder.scoreAfterPlay(current.getScore());
                } else {
                    builder.playerId("System");
                }

                if (category != null && !category.isBlank()) {
                    builder.category(category);
                }

                if ("Select Question".equals(activity) && value != null) {
                    builder.questionValue(value);
                }
                break;

            // Fallback 
            default:
                builder.playerId("System");
                if (category != null && !category.isBlank()) {
                    builder.category(category);
                }
                if (value != null) {
                    builder.questionValue(value);
                }
                if (extra != null && !extra.isBlank()) {
                    builder.result(extra);
                }
                break;
        }

        eventLogger.logEvent(builder.build());
    }

     /**
     * Initializes a new game session by:
     * <ul>
     *     <li>Storing the provided {@link GameData}</li>
     *     <li>Setting status to {@code IN_PROGRESS}</li>
     *     <li>Creating a {@link Player} object for each given name</li>
     * </ul>
     *
     * @param names    list of player display names
     * @param gameData structured category/question data
     */
    public void initializeGame(List<String> names, GameData gameData) {
        this.gameData = gameData;
        gameState.setStatus(GameState.IN_PROGRESS);
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            Player player = new Player("P" + (i + 1), name);
            gameState.addPlayer(player);
        }
    }
    
    /** Forces the game to end immediately by setting status to FINISHED. */
    public void forceEndGame() {
        gameState.setStatus(GameState.FINISHED);
    }
    
    /**
     * Checks whether all questions across all categories have been answered.
     * If so, the game is marked as finished.
     *
     * @return {@code true} if all questions are answered
     */
    public boolean checkAndEndGame() {
        boolean allAnswered = true;
        if (gameData != null) {
            for (Category category : gameData.getCategories()) {
                for (Question question : category.getAllQuestions()) {
                    if (!question.isAnswered()) {
                        allAnswered = false;
                        break;
                    }
                }
            }
        }
        if (allAnswered) {
            gameState.setStatus(GameState.FINISHED);
        }
        return allAnswered;
    }
    
    /** @return {@code true} if the game state is FINISHED */
    public boolean isGameFinished() {
        return GameState.FINISHED.equals(gameState.getStatus());
    }
    
    /**
     * Processes a player's attempt to answer a question.
     * <p>
     * Performs:
     * <ul>
     *     <li>Correct/incorrect scoring</li>
     *     <li>Marking the question as answered</li>
     *     <li>Logging an "Answer Question" event</li>
     *     <li>Storing the event for report building</li>
     * </ul>
     *
     * @param categoryName category containing the question
     * @param value        point value of question
     * @param answer       player's chosen answer (A/B/C/D)
     * @return true if the answer is correct
     */
    public boolean answerQuestion(String categoryName, int value, String answer) {
        Question question = getQuestion(categoryName, value);
        if (question != null && !question.isAnswered()) {
            Player currentPlayer = getCurrentPlayer();
            if (currentPlayer == null) {
                return false;
            }

            boolean correct = question.getCorrectAnswer().equalsIgnoreCase(answer);

            if (correct) {
                currentPlayer.addPoints(question.getValue());
            } else {
                // Deduct points for wrong answer (but not below 0)
                currentPlayer.subtractPoints(question.getValue());
            }

            question.setAnswered(true);

            // Get the actual text of the chosen option, e.g. "int num;" or "Random value"
            String answerText = question.getOption(answer);
            if (answerText == null) {
                // Fallback to the letter if something goes weird
                answerText = answer;
            }

            // Log the answer event in the sample format style
            if (eventLogger != null) {
                GameEvent event = new GameEvent.Builder(caseId, "Answer Question")
                        .playerId(currentPlayer.getName())                 
                        .category(categoryName)
                        .questionValue(question.getValue())
                        .questionText(question.getQuestionText())
                        .answerGiven(answerText)                           
                        .result(correct ? "Correct" : "Incorrect")
                        .scoreAfterPlay(currentPlayer.getScore())          
                        .build();
                
                gameplayEvents.add(event);
                eventLogger.logEvent(event);
            }

            return correct;
        }
        return false;
    }

    /**
     * Generates a text-based summary report using the configured
     * {@link SummaryReportGenerator}.
     *
     * @return path to the resulting report file
     * @throws IOException if writing the report fails
     */
    public Path generateSummaryReport() throws IOException {
        return reportGenerator.generate(this);
    }

    /** @return list of all players tied for highest score */
    public List<Player> getWinners() {
        return gameState.determineWinners();
    }
    
    /** @return true if two or more players share the top score */
    public boolean isTie() {
        return gameState.isTie();
    }
    
    /** @return textual summary of winning state or tie */
    public String getGameResult() {
        return gameState.getGameResult();
    }
    
    /** @return the highest-scoring player, or null if none */
    public Player getWinner() {
        return gameState.getWinner();
    }

    /** Advances to the next player's turn. */
    public void nextPlayer() {
    gameState.nextPlayer();
}
}