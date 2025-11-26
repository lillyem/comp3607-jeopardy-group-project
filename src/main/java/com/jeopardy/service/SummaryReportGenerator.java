package com.jeopardy.service;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface for components that generate a summary report
 * for a completed or in-progress game.
 * <p>
 * Implementations can decide:
 * <ul>
 *     <li>Output format (e.g. plain text, HTML, PDF)</li>
 *     <li>Where the report is written to disk</li>
 *     <li>What details from {@link GameController} are included</li>
 * </ul>
 */
public interface SummaryReportGenerator {

    /**
     * Generates a report describing the game managed by the given controller.
     *
     * @param controller the {@link GameController} from which to pull game data
     * @return the {@link Path} to the generated report file
     * @throws IOException if the report cannot be written
     */
    Path generate(GameController controller) throws IOException;
}
