package com.jeopardy.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PlayerTest {
    @Test
    public void testPlayerCreation() {
        Player player = new Player("p1", "Daniel");
        assertEquals("p1", player.getPlayerId());
        assertEquals("Daniel", player.getName());
        assertEquals(0, player.getScore());
    }
    
    @Test
    public void testScoreManagement() {
        Player player = new Player("p1", "Daniel");
        player.addPoints(100);
        assertEquals(100, player.getScore());
        player.subtractPoints(50);
        assertEquals(50, player.getScore());
    }
}
