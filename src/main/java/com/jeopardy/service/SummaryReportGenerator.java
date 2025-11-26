package com.jeopardy.service;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Strategy interface for generating game summary reports in various formats.
 * Implementations provide format-specific reporting while maintaining
 * consistent content structure.
 * 
 */
public interface SummaryReportGenerator {

    /**
     * Generates a comprehensive summary report for the completed game.
     * Includes player information, gameplay history, and final results.
     *
     * @param controller The game controller containing game state and results
     * @return Path to the generated report file
     * @throws IOException if report file cannot be created
     */
    Path generate(GameController controller) throws IOException;
}
