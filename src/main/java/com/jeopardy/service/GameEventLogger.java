package com.jeopardy.service;

import com.jeopardy.model.GameEvent;

/**
 * Abstraction for logging game events (process mining CSV log).
 */
public interface GameEventLogger {
    void logEvent(GameEvent event);
    void close();
}
