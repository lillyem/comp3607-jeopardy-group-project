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
 * Main controller class that orchestrates the Jeopardy game flow.
 * Manages game state, player turns, question answering, and coordinates
 * between the UI layer and game logic.
 *  
 */
public class GameController {
    private GameState gameState;
    private GameData gameData;
    private final List<GameEvent> gameplayEvents = new ArrayList<>();

    private GameEventLogger eventLogger;
    private String caseId;
    private final SummaryReportGenerator reportGenerator = new TextSummaryReportGenerator();

    public GameController() {
        this.gameState = new GameState();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HHmmss");
        this.caseId = "GAME" + fmt.format(LocalDateTime.now());


        // CSV logger writes to game_event_log.csv
        this.eventLogger = new CsvGameEventLogger(caseId);
    }
    
    /** 
     * @return List<GameEvent>
     */
    public List<GameEvent> getGameplayEvents() {
        return gameplayEvents;
    }

    /** 
     * @return String
     */
    public String getCaseId() {
        return caseId;
    }

    /** 
     * @return GameState
     */
    public GameState getGameState() {
        return gameState;
    }
    
    /** 
     * @return List<Player>
     */
    public List<Player> getPlayers() {
        return gameState.getPlayers();
    }
    
    /** 
     * @return Player
     */
    public Player getCurrentPlayer() {
        return gameState.getCurrentPlayer();
    }
    
    /** 
     * @return List<Category>
     */
    public List<Category> getCategories() {
        return gameData != null ? gameData.getCategories() : List.of();
    }
    
    /** 
     * @return Object
     */
    public Object getGame() {
        return this;
    }
    
    /** 
     * @param categoryName
     * @param value
     * @return Question
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
     * @param activity
     * @param category
     * @param value
     * @param extra
     */
    public void systemEvent(String activity, String category, Integer value, String extra) {
        if (eventLogger == null) {
            return;
        }

        GameEvent.Builder builder = new GameEvent.Builder(caseId, activity);

        switch (activity) {
        // ==========================================
        // Purely system-level events (no player)
        // ==========================================
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

            // ==========================================
            // Select Player Count
            // Sample: ...,System,Select Player Count,...,,,2,N/A,
            // ==========================================
            case "Select Player Count":
                builder.playerId("System");
                if (extra != null && !extra.isBlank()) {
                    builder.answerGiven(extra); // the number of players, e.g. "2"
                }
                builder.result("N/A");
                break;

            // ==========================================
            // Enter Player Name
            // Sample: ...,Alice,Enter Player Name,...,,,Alice,N/A,
            // ==========================================
            case "Enter Player Name":
                String name = (extra != null) ? extra : "";
                builder.playerId(name);
                if (!name.isEmpty()) {
                    builder.answerGiven(name);
                }
                builder.result("N/A");
                break;

            // ==========================================
            // Player-driven navigation
            // Sample:
            //   Alice,Select Category,...,Category,,,,0
            //   Alice,Select Question,...,Category,100,,,0
            // Score_After_Play = current score *before* answering.
            // ==========================================
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

            // ==========================================
            // Fallback 
            // ==========================================
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
     * Initializes a new game with the specified players and question data.
     * Sets up the game state and prepares for gameplay.
     *
     * @param playerNames List of player names to participate in the game
     * @param gameData The loaded game data containing categories and questions
     * @throws IllegalArgumentException if playerNames is empty or gameData is invalid
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
    
    public void forceEndGame() {
        gameState.setStatus(GameState.FINISHED);
    }
    
    /** 
     * @return boolean
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
    
    /** 
     * @return boolean
     */
    public boolean isGameFinished() {
        return GameState.FINISHED.equals(gameState.getStatus());
    }
    
    /**
     * Processes a player's answer to a question and updates scores accordingly.
     * Logs the attempt for process mining and advances to the next player.
     *
     * @param categoryName The category of the question being answered
     * @param value The point value of the question
     * @param answer The player's answer (A, B, C, or D)
     * @return true if the answer was correct, false otherwise
     * @throws IllegalStateException if the game is not in progress
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
     * Generates a comprehensive summary report of the completed game.
     * Includes player scores, gameplay history, and final results.
     *
     * @return Path to the generated report file
     * @throws IOException if report file cannot be created
     */
    public Path generateSummaryReport() throws IOException {
        return reportGenerator.generate(this);
    }

    
    /**
     * Determines all winners of the game, handling tie scenarios.
     * Returns multiple players if they have the same highest score.
     *
     * @return List of players with the highest score, empty list if no winners
     */
    public List<Player> getWinners() {
        return gameState.determineWinners();
    }
    
    /** 
     * @return boolean
     */
    public boolean isTie() {
        return gameState.isTie();
    }
    
    /** 
     * @return String
     */
    public String getGameResult() {
        return gameState.getGameResult();
    }
    
    /** 
     * @return Player
     */
    public Player getWinner() {
        return gameState.getWinner();
    }

     /**
     * Advances the game to the next player's turn.
     * Implements circular rotation through the player list.
     */
    public void nextPlayer() {
    gameState.nextPlayer();
}
}