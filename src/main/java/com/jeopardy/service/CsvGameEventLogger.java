package com.jeopardy.service;

import com.jeopardy.model.GameEvent;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * CSV implementation of GameEventLogger that writes game events to a CSV file
 * for process mining analysis. Formats events according to project requirements
 * with proper timestamps and all required columns.
 * 
 */

public class CsvGameEventLogger implements GameEventLogger {

    private static final String CSV_HEADER =
            "Case_ID,Player_ID,Activity,Timestamp,Category,Question_Value,Answer_Given,Result,Score_After_Play";

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private final String caseId;
    private final Path logFile;

     /**
     * Constructs a new CSV logger for the specified game case.
     *
     * @param caseId Unique identifier for the game session
     */
    public CsvGameEventLogger(String caseId) {
        this.caseId = caseId;

        // Ensure logs/ directory exists
        Path logsDir = Paths.get("logs");
        try {
            Files.createDirectories(logsDir);
        } catch (IOException e) {
            System.err.println("Could not create logs directory: " + e.getMessage());
        }

        this.logFile = logsDir.resolve("game_event_log.csv");
        initializeLogFile();
    }
    /** Initialises the CSV log file with header if it does not exist. */
    private void initializeLogFile() {
        // If file already exists, keep appending (don’t overwrite previous games)
        if (Files.exists(logFile)) {
            return;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile.toFile(), true))) {
            writer.println(CSV_HEADER);
        } catch (IOException e) {
            System.err.println("Could not initialize game event log: " + e.getMessage());
        }
    }

    @Override
    public synchronized void logEvent(GameEvent event) {
        if (event == null) {
            return;
        }

        String timestamp = formatTimestamp(event.getTimestamp());
        String playerId = event.getPlayerId() != null ? event.getPlayerId() : "System";

        String line = String.join(",",
                safe(event.getCaseId() != null ? event.getCaseId() : this.caseId),
                safe(playerId),
                safe(event.getActivity()),
                safe(timestamp),
                safe(event.getCategory()),
                safe(event.getQuestionValue()),
                safe(event.getAnswerGiven()),
                safe(event.getResult()),
                safe(event.getScoreAfterPlay())
        );

        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile.toFile(), true))) {
            writer.println(line);
        } catch (IOException e) {
            System.err.println("Error logging event: " + e.getMessage());
        }
    }
    /**
     * Formats the given timestamp to the required CSV format.
     * @param instant
     * @return
     */
    private String formatTimestamp(Instant instant) {
        if (instant == null) {
            return TIMESTAMP_FORMAT.format(Instant.now());
        }
        return TIMESTAMP_FORMAT.format(instant);
    }

    /**
     * Safely converts a value to string, returning empty string if null.
     * @param value
     * @return
     */
    private String safe(Object value) {
        return value == null ? "" : value.toString();
    }

    @Override
    public void close() {}
}
