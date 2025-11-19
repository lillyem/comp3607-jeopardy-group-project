package com.jeopardy.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GameState enum
 */
public class GameStateTest {

    @Test
    public void testGameStateValues() {
        // Test that all expected enum values exist
        GameState[] states = GameState.values();
        
        assertEquals(3, states.length);
        assertTrue(containsState(states, "SETUP"));
        assertTrue(containsState(states, "IN_PROGRESS"));
        assertTrue(containsState(states, "FINISHED"));
    }

    @Test
    public void testGameStateValueOf() {
        // Test valueOf method works correctly
        assertEquals(GameState.SETUP, GameState.valueOf("SETUP"));
        assertEquals(GameState.IN_PROGRESS, GameState.valueOf("IN_PROGRESS"));
        assertEquals(GameState.FINISHED, GameState.valueOf("FINISHED"));
    }

    @Test
    public void testGameStateOrdinal() {
        // Test ordinal positions
        assertEquals(0, GameState.SETUP.ordinal());
        assertEquals(1, GameState.IN_PROGRESS.ordinal());
        assertEquals(2, GameState.FINISHED.ordinal());
    }

    private boolean containsState(GameState[] states, String stateName) {
        for (GameState state : states) {
            if (state.name().equals(stateName)) {
                return true;
            }
        }
        return false;
    }
}
