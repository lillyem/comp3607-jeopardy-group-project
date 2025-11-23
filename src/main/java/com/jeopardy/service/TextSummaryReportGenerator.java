package com.jeopardy.service;

import com.jeopardy.model.GameEvent;
import com.jeopardy.model.Player;
import com.jeopardy.model.Question;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Plain-text summary report generator matching the sample report format.
 */
public class TextSummaryReportGenerator implements SummaryReportGenerator {

    @Override
    public Path generate(GameController controller) throws IOException {
        if (controller == null || !controller.isGameFinished()) {
            throw new IllegalStateException("Game must be finished before generating a report.");
        }

        var game = controller.getGame();
        List<Player> players = controller.getPlayers();
        List<GameEvent> events = controller.getGameEvents();

        Path reportsDir = Paths.get("reports");
        Files.createDirectories(reportsDir);

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path reportFile = reportsDir.resolve(
                "game_summary_" + game.getGameId() + "_" + timestamp + ".txt"
        );

        try (BufferedWriter writer = Files.newBufferedWriter(reportFile)) {

            // Title
            writer.write("JEOPARDY PROGRAMMING GAME REPORT");
            writer.newLine();
            writer.write("================================");
            writer.newLine();
            writer.newLine();

            // Case ID
            writer.write("Case ID: " + game.getGameId());
            writer.newLine();
            writer.newLine();

            // Players line
            writer.write("Players: " + formatPlayerList(players));
            writer.newLine();
            writer.newLine();

            // Gameplay Summary header
            writer.write("Gameplay Summary:");
            writer.newLine();
            writer.write("-----------------");
            writer.newLine();

            // Turn-by-turn log
            int turn = 1;
            boolean anyTurns = false;
            for (GameEvent ev : events) {
                if (!"QUESTION_ANSWERED".equalsIgnoreCase(ev.getActivity())) {
                    continue;
                }
                anyTurns = true;
                writer.newLine();

                String playerName = resolvePlayerName(players, ev.getPlayerId());
                String category = ev.getCategory() != null ? ev.getCategory() : "";
                int value = ev.getQuestionValue() != null ? ev.getQuestionValue() : 0;
                String questionText = ev.getQuestionText() != null ? ev.getQuestionText() : "";
                String answerGiven = ev.getAnswerGiven() != null ? ev.getAnswerGiven().trim() : "";
                String result = ev.getResult() != null ? ev.getResult().toUpperCase() : "";
                int scoreAfter = ev.getScoreAfterPlay() != null ? ev.getScoreAfterPlay() : 0;

                // Look up the full answer text from the Question model
                String displayAnswer = answerGiven;
                Question q = null;
                if (category != null && !category.isEmpty() && value > 0) {
                    q = controller.getGame().getQuestion(category, value);
                }
                if (q != null) {
                    String optionText = q.getOption(answerGiven);
                    if (optionText != null && !optionText.isBlank()) {
                        displayAnswer = optionText;
                    }
                }

                // Turn line
                writer.write(String.format(
                        "Turn %d: %s selected %s for %d pts",
                        turn, playerName, category, value
                ));
                writer.newLine();

                // Question line
                writer.write("Question: " + questionText);
                writer.newLine();

                // Answer line (with full text, em dash + score delta)
                String correctnessWord = result.equals("CORRECT") ? "Correct" : "Incorrect";
                String delta = (result.equals("CORRECT") ? "+" : "-") + value + " pts";
                writer.write("Answer: " + displayAnswer + " — " + correctnessWord + " (" + delta + ")");
                writer.newLine();

                // Score after turn
                writer.write("Score after turn: " + playerName + " = " + scoreAfter);
                writer.newLine();

                turn++;
            }

            if (!anyTurns) {
                writer.newLine();
                writer.write("(No questions were answered in this game.)");
                writer.newLine();
            }

            writer.newLine();
            writer.write("Final Scores:");
            writer.newLine();
            for (Player p : players) {
                writer.write(p.getName() + ": " + p.getScore());
                writer.newLine();
            }
        }

        return reportFile;
    }

    private String formatPlayerList(List<Player> players) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < players.size(); i++) {
            sb.append(players.get(i).getName());
            if (i < players.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private String resolvePlayerName(List<Player> players, String playerId) {
        if (playerId == null) {
            return "Unknown";
        }
        for (Player p : players) {
            if (playerId.equals(p.getPlayerId())) {
                return p.getName();
            }
        }
        // Fallback if IDs and names are the same
        return playerId;
    }
}
