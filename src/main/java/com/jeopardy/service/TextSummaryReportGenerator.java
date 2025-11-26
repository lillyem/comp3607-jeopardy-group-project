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
 * Generates a human-readable text summary report of a completed game.
 * <p>
 * The report is written to <code>report/summary_report.txt</code> and includes:
 * <ul>
 *     <li>Case ID</li>
 *     <li>Player list</li>
 *     <li>Turn-by-turn gameplay summary</li>
 *     <li>Final scores</li>
 *     <li>Tie information (if applicable)</li>
 * </ul>
 * <p>
 * This implementation relies on the {@link GameController} to provide:
 * <ul>
 *     <li>Recorded {@link GameEvent} objects for each turn</li>
 *     <li>Player states and final scores</li>
 * </ul>
 */
public class TextSummaryReportGenerator implements SummaryReportGenerator {

    /**
     * Generates a textual summary report for the game.
     * <p>
     * The method:
     * <ol>
     *     <li>Ensures the <code>report/</code> directory exists</li>
     *     <li>Creates or overwrites <code>summary_report.txt</code></li>
     *     <li>Writes metadata including Case ID and player list</li>
     *     <li>Iterates through all logged {@link GameEvent} turns</li>
     *     <li>Calculates and prints final scores and tie information</li>
     * </ol>
     *
     * @param controller the game controller that contains events, players, and metadata
     * @return the path to <code>summary_report.txt</code>
     * @throws IOException if the file cannot be created or written
     */
    @Override
    public Path generate(GameController controller) throws IOException {

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
