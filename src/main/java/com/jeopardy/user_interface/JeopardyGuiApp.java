package com.jeopardy.user_interface;

import javax.swing.SwingUtilities;

/**
 * Entry point for the Swing GUI version of the Jeopardy game.
 * <p>
 * This class is responsible for bootstrapping the UI by creating and
 * showing the {@link MainWindow} on the Swing Event Dispatch Thread.
 */
public class JeopardyGuiApp {

    /**
     * Launches the Jeopardy game GUI.
     *
     * @param args command-line arguments (currently ignored)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
