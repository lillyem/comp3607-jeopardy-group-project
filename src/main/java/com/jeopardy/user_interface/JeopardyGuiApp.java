package com.jeopardy.user_interface;

import javax.swing.SwingUtilities;

/**
 * Main application entry point for the Jeopardy game.
 * Launches the Swing GUI and handles application lifecycle.
 * 
 */
public class JeopardyGuiApp {

    /**
     * Main method that launches the Jeopardy game application.
     * Ensures proper Swing thread handling and application initialization.
     *
     * @param args Command line arguments (not used)
     */    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
