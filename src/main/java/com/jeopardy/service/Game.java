package com.jeopardy.service;

import com.jeopardy.model.GameState;
import com.jeopardy.model.Player;
import com.jeopardy.model.Question;
import com.jeopardy.model.GameEvent;
import java.util.List;

/** Manages game logic and event logging. */
public class Game {
    private GameState gameState;
    private GameEventLogger eventLogger;
    private SummaryReportGenerator reportGenerator;
    private String caseId;
    
    /** Creates a new Game instance. */
    public Game() {
        this.gameState = new GameState();
        this.caseId = "GAME" + System.currentTimeMillis();
        this.eventLogger = new CsvGameEventLogger(caseId);
        this.reportGenerator = new TextSummaryReportGenerator();
    }
    
    /** 
     * @return GameState
     */
    public GameState getGameState() {
        return gameState;
    }
    
    public void startGame() {
        gameState.setStatus(GameState.IN_PROGRESS);
        GameEvent event = new GameEvent.Builder(caseId, "Start Game")
            .result("Success")
            .build();
        eventLogger.logEvent(event);
    }
    
    /** 
     * @param questions
     */
    public void loadQuestions(List<Question> questions) {
        gameState.setQuestions(questions);
        GameEvent event = new GameEvent.Builder(caseId, "Load Questions")
            .result("Success")
            .build();
        eventLogger.logEvent(event);
    }
    
    /** 
     * @param player
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
     * @param player
     * @param question
     * @param answer
     * @return boolean
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
     * @return List<Player>
     */
    public List<Player> getWinners() {
        return gameState.determineWinners();
    }
    
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
    
    private void generateReport() {
        try {
            reportGenerator.generate(new GameController());
        } catch (Exception e) {
            System.err.println("Error generating report: " + e.getMessage());
        }
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
    public Player getCurrentPlayer() {
        return gameState.getCurrentPlayer();
    }
    
    public void nextPlayer() {
        gameState.nextPlayer();
    }
}