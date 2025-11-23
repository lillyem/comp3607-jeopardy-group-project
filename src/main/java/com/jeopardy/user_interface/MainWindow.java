package com.jeopardy.user_interface;

import com.jeopardy.model.Category;
import com.jeopardy.model.GameData;
import com.jeopardy.model.GameState;
import com.jeopardy.model.Player;
import com.jeopardy.model.Question;
import com.jeopardy.service.GameController;
import com.jeopardy.service.GameDataLoader;
import com.jeopardy.service.GameDataLoaderFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/**
 * Main Swing window for the Jeopardy game.
 */
public class MainWindow extends JFrame {

    // Core game objects
    private final GameController controller;
    private GameData loadedGameData;

    // UI components
    private final JPanel boardPanel;
    private final JTextArea scoreArea;
    private final JLabel statusLabel;
    private final JButton startGameButton;
    private final JSpinner playerCountSpinner;
    private final List<JTextField> playerNameFields;

    public MainWindow() {
        super("COMP3607 Jeopardy Game");

        this.controller = new GameController();
        this.playerNameFields = new ArrayList<>();

        // Basic window settings
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // === Top panel: file load + player setup + start button ===
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton loadButton = new JButton("Load Questions (CSV/JSON/XML)");
        startGameButton = new JButton("Start Game");
        startGameButton.setEnabled(false);

        topPanel.add(loadButton);

        topPanel.add(new JLabel("Players:"));
        playerCountSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 4, 1));
        topPanel.add(playerCountSpinner);

        // Player name fields
        JPanel namesPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        for (int i = 0; i < 4; i++) {
            JTextField tf = new JTextField();
            tf.setToolTipText("Player " + (i + 1) + " name");
            namesPanel.add(tf);
            playerNameFields.add(tf);
        }

        topPanel.add(namesPanel);
        topPanel.add(startGameButton);

        add(topPanel, BorderLayout.NORTH);

        // === Center panel: Jeopardy board (categories × values) ===
        boardPanel = new JPanel();
        boardPanel.setLayout(new GridLayout(1, 1));
        boardPanel.add(new JLabel("Load questions and start the game to see the board.",
                SwingConstants.CENTER));
        add(boardPanel, BorderLayout.CENTER);

        // === Right panel: scores ===
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(250, 0));
        rightPanel.add(new JLabel("Scores", SwingConstants.CENTER), BorderLayout.NORTH);

        scoreArea = new JTextArea();
        scoreArea.setEditable(false);
        rightPanel.add(new JScrollPane(scoreArea), BorderLayout.CENTER);

        add(rightPanel, BorderLayout.EAST);

        // === Bottom: status bar ===
        statusLabel = new JLabel("Welcome! Load a question file to begin.");
        add(statusLabel, BorderLayout.SOUTH);

        // Wire up button actions
        loadButton.addActionListener(e -> onLoadQuestions());
        startGameButton.addActionListener(e -> onStartGame());

        // Ensure UI reflects initial state
        refreshScores();
    }

    // =============================
    //  File loading & game start
    // =============================

    private void onLoadQuestions() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            String path = chooser.getSelectedFile().getAbsolutePath();
            GameDataLoader loader = GameDataLoaderFactory.createLoader(path);
            loadedGameData = loader.load(Path.of(path));

            JOptionPane.showMessageDialog(this,
                    "Loaded " + loadedGameData.getTotalQuestions() +
                            " questions across " + loadedGameData.getTotalCategories() + " categories.",
                    "Questions Loaded",
                    JOptionPane.INFORMATION_MESSAGE);

            statusLabel.setText("Questions loaded. Enter player names and click Start Game.");
            startGameButton.setEnabled(true);

        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    "Unsupported file type: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("Error: unsupported file type.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error loading file: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("Error loading questions. Try again.");
        }
    }

    private void onStartGame() {
        if (loadedGameData == null || loadedGameData.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please load a question file first.",
                    "No Data",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int playerCount = (int) playerCountSpinner.getValue();
        List<String> names = new ArrayList<>();

        for (int i = 0; i < playerCount; i++) {
            String name = playerNameFields.get(i).getText().trim();
            if (name.isEmpty()) {
                name = "Player " + (i + 1);
            }
            names.add(name);
        }

        try {
            controller.initializeGame(names, loadedGameData);
            buildBoard();
            refreshScores();
            statusLabel.setText("Game started. Current player: " +
                    controller.getCurrentPlayer().getPlayerId());
            // Prevent re-starting mid-game
            startGameButton.setEnabled(false);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error starting game: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // =============================
    //  Board creation & interaction
    // =============================

    private void buildBoard() {
        boardPanel.removeAll();

        List<Category> categories = controller.getCategories();
        if (categories.isEmpty()) {
            boardPanel.add(new JLabel("No categories available.", SwingConstants.CENTER));
            boardPanel.revalidate();
            boardPanel.repaint();
            return;
        }

        // Determine distinct question values across all categories (e.g., 100,200,...)
        Set<Integer> valueSet = new TreeSet<>();
        for (Category cat : categories) {
            cat.getAllQuestions().forEach(q -> valueSet.add(q.getValue()));
        }
        List<Integer> values = new ArrayList<>(valueSet);

        int rows = values.size() + 1; // +1 for header row
        int cols = categories.size();

        boardPanel.setLayout(new GridLayout(rows, cols, 5, 5));

        // Header row: category names
        for (Category cat : categories) {
            JLabel label = new JLabel(cat.getName(), SwingConstants.CENTER);
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            boardPanel.add(label);
        }

        // Value rows: one button per (category, value)
        for (int val : values) {
            for (Category cat : categories) {
                if (cat.hasQuestion(val)) {
                    JButton btn = new JButton(String.valueOf(val));
                    btn.addActionListener(e -> onQuestionClicked(cat.getName(), val, btn));
                    boardPanel.add(btn);
                } else {
                    // Empty cell if category does not have this value
                    boardPanel.add(new JLabel("", SwingConstants.CENTER));
                }
            }
        }

        boardPanel.revalidate();
        boardPanel.repaint();
    }

    private void onQuestionClicked(String categoryName, int value, JButton button) {
        if (controller.getGameState() != GameState.IN_PROGRESS) {
            JOptionPane.showMessageDialog(this,
                    "Game is not in progress.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Question q = controller.getGame().getQuestion(categoryName, value);
        if (q == null) {
            JOptionPane.showMessageDialog(this,
                    "Question not found.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (q.isAnswered()) {
            JOptionPane.showMessageDialog(this,
                    "This question has already been answered.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Build question text + options
        Map<String, String> options = q.getOptions();
        StringBuilder msg = new StringBuilder();
        msg.append(q.getQuestionText()).append("\n\n");
        msg.append("A) ").append(options.get("A")).append("\n");
        msg.append("B) ").append(options.get("B")).append("\n");
        msg.append("C) ").append(options.get("C")).append("\n");
        msg.append("D) ").append(options.get("D")).append("\n");

        String[] choices = {"A", "B", "C", "D"};
        String answer = (String) JOptionPane.showInputDialog(
                this,
                msg.toString(),
                "Answer Question (" + categoryName + " - " + value + ")",
                JOptionPane.QUESTION_MESSAGE,
                null,
                choices,
                "A"
        );

        if (answer == null) {
            // User cancelled
            return;
        }

        boolean correct = controller.answerQuestion(categoryName, value, answer);

        if (correct) {
            JOptionPane.showMessageDialog(this,
                    "Correct!",
                    "Result",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Incorrect.\nCorrect answer: " + q.getCorrectAnswer() +
                            " → " + q.getCorrectAnswerText(),
                    "Result",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        // Disable the button for this question
        button.setEnabled(false);

        // Refresh scores and current player info
        refreshScores();

        // Check if game over
        boolean ended = controller.checkAndEndGame();
        if (ended || controller.isGameFinished()) {
            onGameFinished();
        } else {
            statusLabel.setText("Next player: " +
                    controller.getCurrentPlayer().getPlayerId());
        }
    }

    // =============================
    //  Scores & game end
    // =============================

    private void refreshScores() {
        StringBuilder sb = new StringBuilder();
        List<Player> players = controller.getPlayers();
        if (players.isEmpty()) {
            sb.append("No players yet.\n");
        } else {
            for (Player p : players) {
                sb.append(p.getPlayerId())
                        .append(" (")
                        .append(p.toString())
                        .append(")")
                        .append("\n");
            }
        }
        scoreArea.setText(sb.toString());
    }

    private void onGameFinished() {
        Player winner = controller.getWinner();
        String message;
        if (winner != null) {
            message = String.format("Game over! Winner: %s with %d points.",
                    winner.getPlayerId(), winner.getScore());
        } else {
            message = "Game over! No winner determined.";
        }

        JOptionPane.showMessageDialog(this,
                message,
                "Game Finished",
                JOptionPane.INFORMATION_MESSAGE);

        statusLabel.setText("Game finished. " + message);

        // Disable all question buttons
        Component[] components = boardPanel.getComponents();
        for (Component c : components) {
            if (c instanceof JButton ) {
                JButton btn = (JButton) c;
                btn.setEnabled(false);
            }
        }

        // Generate summary report
        try {
            java.nio.file.Path reportPath = controller.generateSummaryReport();
            JOptionPane.showMessageDialog(this,
                    "Summary report generated at:\n" + reportPath.toAbsolutePath(),
                    "Summary Report",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not generate summary report: " + ex.getMessage(),
                    "Report Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
