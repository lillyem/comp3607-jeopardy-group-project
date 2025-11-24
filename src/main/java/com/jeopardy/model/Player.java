package com.jeopardy.model;

public class Player{
    private String playerId;
    private String name;
    private int score;

    public Player(String playerId, String name){
        this.playerId = playerId;
        this.name = name;
        this.score = 0;  
    }

    public String getName(){
        return this.name;
    }
    public String getPlayerId(){
        return this.playerId;
    }

    public int getScore(){
        return this.score;
    }

    public void setScore(int score){
        if(score >= 0)
            this.score = score;
    }

    public void addPoints(int points){
        if(points > 0)
            this.score += points;
    }

     public void subtractPoints(int points) {
    if (points > 0) {
        this.score = Math.max(0, this.score - points); // Prevent negative scores
    }
}
    
    public void resetScore() {
        this.score = 0;
    }
    
    @Override
    public String toString() {
        return String.format("Player{id='%s', name='%s', score=%d}", playerId, name, score);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Player player = (Player) obj;
        return playerId.equals(player.playerId);
    }
    
    @Override
    public int hashCode() {
        return playerId.hashCode();
    }
}
