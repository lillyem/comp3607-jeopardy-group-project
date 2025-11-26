package com.jeopardy.service;

import com.jeopardy.model.GameEvent;
import com.jeopardy.model.Player;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Text format implementation of SummaryReportGenerator that creates
 * human-readable game reports in plain text format. Follows the
 * Template Method pattern for consistent report structure.
 * 
 */
public class TextSummaryReportGenerator implements SummaryReportGenerator {

   /**
     * Generates a detailed text report with game summary, player scores,
     * turn-by-turn history, and final results including tie handling.
     *
     * @param controller The game controller with complete game data
     * @return Path to the generated text report file
     * @throws IOException if the report file cannot be written
     */
    @Override
    public Path generate(GameController controller) throws IOException {

        // Ensure report directory exists
        Path reportDir = Paths.get("report");
        if (!Files.exists(reportDir)) {
            Files.createDirectories(reportDir);
        }

        Path reportPath = reportDir.resolve("summary_report.txt");

        List<GameEvent> turns = controller.getGameplayEvents();
        List<Player> players = controller.getPlayers();

        try (PrintWriter writer = new PrintWriter(new FileWriter(reportPath.toFile()))) {

            // Header
            writer.println("JEOPARDY PROGRAMMING GAME REPORT");
            writer.println("================================");
            writer.println();
            writer.println("Case ID: " + controller.getCaseId());
            writer.println();

            // Players line
            String playerNames = players.stream()
                    .map(Player::getName)
                    .collect(Collectors.joining(", "));
            writer.println("Players: " + playerNames);
            writer.println();

            // Gameplay summary
            writer.println("Gameplay Summary:");
            writer.println("-----------------");

            int turnNumber = 1;
            for (GameEvent ev : turns) {

                writer.println("Turn " + turnNumber + ": " +
                        ev.getPlayerId() + " selected " +
                        ev.getCategory() + " for " +
                        ev.getQuestionValue() + " pts");

                writer.println("Question: " + ev.getQuestionText());
                writer.println("Answer: " + ev.getAnswerGiven() +
                        " — " + ev.getResult() +
                        (ev.getResult().equals("Correct")
                                ? " (+" + ev.getQuestionValue() + " pts)"
                                : " (-" + ev.getQuestionValue() + " pts)"));

                writer.println("Score after turn: " + ev.getPlayerId() +
                        " = " + ev.getScoreAfterPlay());
                writer.println();

                turnNumber++;
            }

            writer.println("Final Scores:");
            for (Player p : players) {
                writer.println(p.getName() + ": " + p.getScore());
            }
            writer.println();

            // Tie information only if there is a tie
            List<Player> winners = controller.getWinners();
            if (winners.size() > 1) {
                writer.println("It's a tie! Winners: " +
                        winners.stream()
                               .map(w -> w.getName() + " (" + w.getScore() + " points)")
                               .collect(Collectors.joining(", ")));
            }
        }

        return reportPath;
    }
}
