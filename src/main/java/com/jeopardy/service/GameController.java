package com.jeopardy.service;

import com.jeopardy.model.Category;
import com.jeopardy.model.GameData;
import com.jeopardy.model.GameState;
import com.jeopardy.model.Player;
import com.jeopardy.model.Question;
import java.util.List;

public class GameController {
    private GameState gameState;
    private GameData gameData;
    
    public GameController() {
        this.gameState = new GameState();
    }
    
    public GameState getGameState() {
        return gameState;
    }
    
    public List<Player> getPlayers() {
        return gameState.getPlayers();
    }
    
    public Player getCurrentPlayer() {
        return gameState.getCurrentPlayer();
    }
    
    public List<Category> getCategories() {
        return gameData != null ? gameData.getCategories() : List.of();
    }
    
    public Object getGame() {
        return this;
    }
    
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
    
    public void systemEvent(String activity, String category, Integer value, String extra) {
    }
    
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
    
    public boolean isGameFinished() {
        return GameState.FINISHED.equals(gameState.getStatus());
    }
    
    public boolean answerQuestion(String categoryName, int value, String answer) {
    Question question = getQuestion(categoryName, value);
    if (question != null && !question.isAnswered()) {
        boolean correct = question.getCorrectAnswer().equals(answer);
        Player currentPlayer = getCurrentPlayer();
        
        if (correct) {
            currentPlayer.addPoints(question.getValue());
        } else {
            // Deduct points for wrong answer (but not below 0)
            currentPlayer.subtractPoints(question.getValue());
        }
        
        question.setAnswered(true);
        return correct;
    }
    return false;
}
    
    public java.nio.file.Path generateSummaryReport() {
        return java.nio.file.Path.of("report.txt");
    }
    
    public List<Player> getWinners() {
        return gameState.determineWinners();
    }
    
    public boolean isTie() {
        return gameState.isTie();
    }
    
    public String getGameResult() {
        return gameState.getGameResult();
    }
    
    public Player getWinner() {
        return gameState.getWinner();
    }

    public void nextPlayer() {
    gameState.nextPlayer();
}
}