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
 * Logs {@link GameEvent} instances to {@code logs/game_event_log.csv}
 * in the CSV format required by the project handout.
 * <p>
 * Each row in the file uses the following header:
 * <pre>
 * Case_ID,Player_ID,Activity,Timestamp,Category,Question_Value,
 * Answer_Given,Result,Score_After_Play
 * </pre>
 * <p>
 * If the log file already exists, entries are appended so multiple
 * game sessions can share a single log.
 */

public class CsvGameEventLogger implements GameEventLogger {

    /** CSV header row used when creating a new log file. */
    private static final String CSV_HEADER =
            "Case_ID,Player_ID,Activity,Timestamp,Category,Question_Value,Answer_Given,Result,Score_After_Play";

    /** Formatter used for converting {@link Instant} timestamps to strings. */        
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    /** Identifier of the game session (case) for which events are logged. */                
    private final String caseId;

    /** Path to the CSV log file on disk. */
    private final Path logFile;

    /**
     * Creates a new CSV-based game event logger for the given case ID.
     * <p>
     * Ensures that the {@code logs/} directory exists and initializes
     * the log file with the CSV header if it does not yet exist.
     *
     * @param caseId the identifier of the game session (case)
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

    /**
     * Initializes the log file if it does not already exist by
     * writing the CSV header row. If the file is present, it is
     * left untouched so that new entries can be appended.
     */
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

    /**
     * Appends a single {@link GameEvent} as a CSV row to the log file.
     * <p>
     * If the event has a {@code null} case ID, the logger's own case ID
     * is used. If the player ID is {@code null}, the value {@code "System"}
     * is used instead. All other {@code null} values are converted to
     * empty strings.
     *
     * @param event the event to log (ignored if {@code null})
     */
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
     * Formats the given {@link Instant} into a timestamp string using
     * {@link #TIMESTAMP_FORMAT}. If the instant is {@code null}, the
     * current time is used instead.
     *
     * @param instant the timestamp to format, or {@code null} to use now
     * @return a formatted timestamp string
     */
    private String formatTimestamp(Instant instant) {
        if (instant == null) {
            return TIMESTAMP_FORMAT.format(Instant.now());
        }
        return TIMESTAMP_FORMAT.format(instant);
    }

    /**
     * Converts the given value to a safe CSV field representation.
     * <p>
     * {@code null} values are converted to an empty string; all other
     * values use their {@link Object#toString()} representation.
     *
     * @param value the value to convert
     * @return a non-null string suitable for writing to CSV
     */
    private String safe(Object value) {
        return value == null ? "" : value.toString();
    }

    /**
     * Closes this logger. This implementation does not hold open resources,
     * so the method is a no-op but provided to satisfy the
     * {@link GameEventLogger} interface contract.
     */
    @Override
    public void close() {}
}
