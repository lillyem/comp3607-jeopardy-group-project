package com.jeopardy.service;

import com.jeopardy.model.GameEvent;

/**
 * Observer interface for logging game events for process mining.
 * Implementations handle the storage and formatting of game events
 * for analysis and compliance tracking.
 */

public interface GameEventLogger {
    /**
     * Logs a game event with all relevant details for process mining.
     * Events include player actions, system events, and game state changes.
     *
     * @param event The GameEvent to log, containing all event details
     */
    void logEvent(GameEvent event);

    /**
     * Closes the logger and releases any associated resources.
     * Ensures all pending events are flushed and stored properly.
     */
    void close();
}
