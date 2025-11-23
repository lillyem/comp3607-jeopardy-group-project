package com.jeopardy.service;

import com.jeopardy.model.GameEvent;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;

/**
 * Writes game events to a CSV file for process mining.
 * Columns:
 * Case_ID, Player_ID, Activity, Timestamp,
 * Category, Question_Value, Answer_Given, Result, Score_After_Play
 */

public class CsvGameEventLogger implements GameEventLogger {

    private static final String[] HEADER = {
            "Case_ID", "Player_ID", "Activity", "Timestamp",
            "Category", "Question_Value", "Answer_Given", "Result", "Score_After_Play"
    };

    private final Path logFile;
    private BufferedWriter writer;
    private boolean headerWritten;

    public CsvGameEventLogger() {
        this(Paths.get("logs", "game_event_log.csv"));
    }

    public CsvGameEventLogger(Path logFile) {
        this.logFile = logFile;
        initWriter();
    }

    private void initWriter() {
        try {
            if (logFile.getParent() != null) {
                Files.createDirectories(logFile.getParent());
            }
            boolean exists = Files.exists(logFile);
            writer = Files.newBufferedWriter(
                    logFile,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
            headerWritten = exists && Files.size(logFile) > 0;
            if (!headerWritten) {
                writeHeader();
            }
        } catch (IOException e) {
            System.err.println("Failed to initialize event logger: " + e.getMessage());
            writer = null;
        }
    }

    private void writeHeader() throws IOException {
        writeLine(String.join(",", HEADER));
        headerWritten = true;
    }

    private void writeLine(String line) throws IOException {
        writer.write(line);
        writer.newLine();
        writer.flush();
    }

    private String escape(String value) {
        if (value == null) return "";
        String v = value.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v + "\"";
        }
        return v;
    }

    @Override
    public void logEvent(GameEvent event) {
        if (writer == null || event == null) return;

        try {
            String line = String.join(",",
                    escape(event.getCaseId()),
                    escape(event.getPlayerId()),
                    escape(event.getActivity()),
                    escape(event.getTimestamp() != null ? event.getTimestamp().toString() : null),
                    escape(event.getCategory()),
                    event.getQuestionValue() != null ? event.getQuestionValue().toString() : "",
                    escape(event.getAnswerGiven()),
                    escape(event.getResult()),
                    event.getScoreAfterPlay() != null ? event.getScoreAfterPlay().toString() : ""
            );
            writeLine(line);
        } catch (IOException e) {
            System.err.println("Failed to log event: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {
            }
        }
    }
}
