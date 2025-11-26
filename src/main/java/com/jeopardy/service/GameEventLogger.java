package com.jeopardy.service;

import com.jeopardy.model.GameEvent;

/**
 * Interface representing a logging mechanism for {@link GameEvent} objects.
 * <p>
 * Implementations may log events to:
 * <ul>
 *     <li>CSV files</li>
 *     <li>Text files</li>
 *     <li>Databases</li>
 *     <li>In-memory structures</li>
 * </ul>
 * The logger is used throughout the game lifecycle to capture actions such as:
 * <ul>
 *     <li>Starting or ending the game</li>
 *     <li>Player interactions</li>
 *     <li>Question selection</li>
 *     <li>Answer submissions</li>
 * </ul>
 */

public interface GameEventLogger {

    /**
     * Writes a single {@link GameEvent} to the underlying logging destination.
     *
     * @param event the event to record (implementations may safely ignore null values)
     */
    void logEvent(GameEvent event);

    /**
     * Releases any resources held by the logger.
     * <p>
     * For file-based loggers this may close streams, while in-memory loggers
     * may perform no action. Provided to allow consistent cleanup regardless
     * of implementation.
     */
    void close();
}
