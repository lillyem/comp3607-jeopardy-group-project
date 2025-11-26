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
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Main Swing-based GUI window for the Jeopardy game.
 * <p>
 * Handles:
 * <ul>
 *   <li>Loading question data files (CSV, JSON, XML)</li>
 *   <li>Player setup and initialization</li>
 *   <li>Rendering category boards and question buttons</li>
 *   <li>Displaying and updating scoreboards</li>
 *   <li>Managing gameplay flow (turn rotation, answering questions)</li>
 *   <li>Producing summary reports and ending games</li>
 * </ul>
 */
public class MainWindow extends JFrame {

    /** Handles game logic, turn rotation, answer validation, and logging. */
    private GameController controller;
    
    /** Stores the loaded question dataset before starting a game. */
    private GameData loadedGameData;

    private final JPanel boardPanel;
    private final JPanel scorePanel;
    private final JLabel statusLabel;
    private final JButton startGameButton;
    private final JButton endGameButton;
    private final JButton loadButton;
    private JButton newGameButton;
    private final JSpinner playerCountSpinner;
    private final List<JTextField> playerNameFields;

    private boolean gameInProgress = false;

    /**
     * Creates and configures the main application window.
     * <p>
     * This includes:
     * <ul>
     *   <li>Setting up window layout and panels</li>
     *   <li>Building controls for loading data and starting a game</li>
     *   <li>Creating the scoreboard and board display panels</li>
     *   <li>Registering button event listeners</li>
     * </ul>
     */
    public MainWindow() {
        super("COMP3607 Jeopardy Game");

        this.controller = new GameController();
        this.playerNameFields = new ArrayList<JTextField>();

        // Basic window settings
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // ===== TOP: controls =====
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        loadButton = new JButton("Load Questions (CSV/JSON/XML)");
        leftControls.add(loadButton);

        leftControls.add(new JLabel("Players:"));
        playerCountSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 4, 1));
        leftControls.add(playerCountSpinner);

        topPanel.add(leftControls, BorderLayout.WEST);

        // Player name fields
        JPanel namesPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        for (int i = 0; i < 4; i++) {
            JTextField tf = new JTextField();
            tf.setToolTipText("Player " + (i + 1) + " name");
            namesPanel.add(tf);
            playerNameFields.add(tf);
        }
        topPanel.add(namesPanel, BorderLayout.CENTER);

        // Start / End / New game
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        startGameButton = new JButton("Start Game");
        startGameButton.setEnabled(false);
        endGameButton = new JButton("End Game");
        endGameButton.setEnabled(false);
        newGameButton = new JButton("New Game");
        newGameButton.setEnabled(false);
        rightControls.add(startGameButton);
        rightControls.add(endGameButton);
        rightControls.add(newGameButton);

        topPanel.add(rightControls, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // ===== CENTER: board =====
        boardPanel = new JPanel();
        boardPanel.setLayout(new GridLayout(1, 1));
        boardPanel.add(new JLabel("Load questions and start the game to see the board.",
                SwingConstants.CENTER));
        add(boardPanel, BorderLayout.CENTER);

        // ===== RIGHT: pretty score panel =====
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(260, 0));
        rightPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel scoreTitle = new JLabel("Scores", SwingConstants.CENTER);
        scoreTitle.setFont(scoreTitle.getFont().deriveFont(Font.BOLD, 18f));
        rightPanel.add(scoreTitle, BorderLayout.NORTH);

        scorePanel = new JPanel();
        scorePanel.setLayout(new BoxLayout(scorePanel, BoxLayout.Y_AXIS));
        scorePanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        JScrollPane scoreScroll = new JScrollPane(scorePanel);
        scoreScroll.setBorder(null);
        rightPanel.add(scoreScroll, BorderLayout.CENTER);

        add(rightPanel, BorderLayout.EAST);

        // ===== BOTTOM: status bar =====
        statusLabel = new JLabel("Welcome! Load a question file to begin.");
        statusLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
        add(statusLabel, BorderLayout.SOUTH);

        // Wire up actions
        wireActions();

        // Initial scores
        refreshScores();
    }

     /** 
     * Sends a system-level event to the process-mining log.
     * <p>
     * This is used for documenting interactions such as:
     * <ul>
     *   <li>Loading files</li>
     *   <li>Selecting player counts</li>
     *   <li>Entering player names</li>
     *   <li>Selecting categories/questions</li>
     *   <li>Generating reports</li>
     * </ul>
     *
     * @param activity Name of the system action (e.g., "Load File")
     * @param category Optional category string associated with the action
     * @param value    Optional question value tied to the action
     * @param extra    Additional info (e.g., number of players, success message)
     */
    private void logSystem(String activity, String category, Integer value, String extra) {
        controller.systemEvent(activity, category, value, extra);
    }

    /**
     * Registers all button listeners for file loading, game start,
     * game end, and game reset.
     */
    private void wireActions() {
        loadButton.addActionListener(e -> onLoadQuestions());
        startGameButton.addActionListener(e -> onStartGame());
        endGameButton.addActionListener(e -> onEndGame());
        newGameButton.addActionListener(e -> resetGame());
    }

    //  File loading & game setup

    /**
     * Opens a file chooser dialog and loads Jeopardy questions from
     * a CSV, JSON, or XML file using the appropriate GameDataLoader.
     * <p>
     * This method:
     * <ul>
     *   <li>Validates that no game is currently running</li>
     *   <li>Logs system events for process mining</li>
     *   <li>Displays errors if loading fails</li>
     *   <li>Enables the Start Game button upon success</li>
     * </ul>
     */
    private void onLoadQuestions() {
        if (gameInProgress) {
            JOptionPane.showMessageDialog(this,
                    "Cannot load new questions while a game is in progress.",
                    "Game In Progress",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        logSystem("Start Game", null, null, null);

        logSystem("Load File", null, null, null);

        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Jeopardy Data Files (CSV, JSON, XML)", "csv", "json", "xml"));

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            String path = chooser.getSelectedFile().getAbsolutePath();
            GameDataLoader loader = GameDataLoaderFactory.createLoader(path);
            loadedGameData = loader.load(Path.of(path));

            // Process log: File Loaded Successfully
            logSystem("File Loaded Successfully", null, null, "Success");

            JOptionPane.showMessageDialog(this,
                    "Loaded " + loadedGameData.getTotalQuestions() +
                            " questions across " + loadedGameData.getTotalCategories() + " categories.",
                    "Questions Loaded",
                    JOptionPane.INFORMATION_MESSAGE);

            statusLabel.setText("Questions loaded. Enter player names and click Start Game.");
            startGameButton.setEnabled(true);

        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error loading questions: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("Error loading questions. Check file format and data.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error reading file: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("Error loading questions. Try again.");
        }
    }

    /**
     * Initializes the Jeopardy game using:
     * <ul>
     *   <li>The loaded question data</li>
     *   <li>The number of players selected</li>
     *   <li>The entered player names</li>
     * </ul>
     * <p>
     * This method:
     * <ul>
     *   <li>Validates player names</li>
     *   <li>Logs system events for each player name entered</li>
     *   <li>Initializes the game in the GameController</li>
     *   <li>Builds the game board</li>
     *   <li>Locks UI elements to prevent configuration changes</li>
     * </ul>
     */
    private void onStartGame() {
        if (loadedGameData == null || loadedGameData.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please load a question file first.",
                    "No Data",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int playerCount = ((Integer) playerCountSpinner.getValue()).intValue();

        // Process log: Select Player Count
        logSystem("Select Player Count", null, null, String.valueOf(playerCount));

        List<String> names = new ArrayList<String>();

        for (int i = 0; i < playerCount; i++) {
            String name = playerNameFields.get(i).getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "You selected " + playerCount + " player(s),\n" +
                        "but one or more of the first " + playerCount + " name fields is empty.\n\n" +
                        "Please fill in ALL required player names.",
                        "Missing Player Names",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            // Process log: Enter Player Name
            logSystem("Enter Player Name", null, null, name);

            names.add(name);
        }

        try {
            controller.initializeGame(names, loadedGameData);
            gameInProgress = true;

            buildBoard();
            refreshScores();
            statusLabel.setText("Game started. Current player: " +
                    controller.getCurrentPlayer().getName());

            // Lock in config while playing
            startGameButton.setEnabled(false);
            loadButton.setEnabled(false);
            endGameButton.setEnabled(true);
            newGameButton.setEnabled(false);
            playerCountSpinner.setEnabled(false);
            for (JTextField tf : playerNameFields) {
                tf.setEnabled(false);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error starting game: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Forces the current game to end immediately.
     * This triggers the normal end-of-game processing.
     */
    private void onEndGame() {
        if (!gameInProgress) {
            return;
        }

        // Mark game as finished
        controller.forceEndGame();
        onGameFinished();
    }

    //  Board creation & interaction

    /**
     * Constructs the visual Jeopardy board based on loaded categories and
     * their associated question values.
     * <p>
     * This method:
     * <ul>
     *   <li>Clears the board panel</li>
     *   <li>Builds a grid layout using distinct point values</li>
     *   <li>Places category headers at the top</li>
     *   <li>Creates a JButton for each available question</li>
     *   <li>Leaves blank spaces if a category does not contain that value</li>
     * </ul>
     */
    private void buildBoard() {
        boardPanel.removeAll();

        List<Category> categories = controller.getCategories();
        if (categories.isEmpty()) {
            boardPanel.add(new JLabel("No categories available.", SwingConstants.CENTER));
            boardPanel.revalidate();
            boardPanel.repaint();
            return;
        }

        // Determine distinct question values across all categories (e.g., 100, 200,…)
        Set<Integer> valueSet = new TreeSet<Integer>();
        for (Category cat : categories) {
            for (Question q : cat.getAllQuestions()) {
                valueSet.add(Integer.valueOf(q.getValue()));
            }
        }
        List<Integer> values = new ArrayList<Integer>(valueSet);

        int rows = values.size() + 1; // +1 for header row
        int cols = categories.size();

        boardPanel.setLayout(new GridLayout(rows, cols, 5, 5));

        // Header row: category names
        for (Category cat : categories) {
            JLabel label = new JLabel(cat.getName(), SwingConstants.CENTER);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
            label.setOpaque(true);
            label.setBackground(new Color(30, 60, 120));
            label.setForeground(Color.WHITE);
            boardPanel.add(label);
        }

        // Value rows: one button per (category, value)
        for (int i = 0; i < values.size(); i++) {
            int val = values.get(i).intValue();
            for (Category cat : categories) {
                if (cat.hasQuestion(val)) {
                    JButton btn = new JButton(String.valueOf(val));
                    btn.setFont(btn.getFont().deriveFont(Font.BOLD, 14f));
                    btn.addActionListener(e -> onQuestionClicked(cat.getName(), val, btn));
                    boardPanel.add(btn);
                } else {
                    boardPanel.add(new JLabel("", SwingConstants.CENTER));
                }
            }
        }

        boardPanel.revalidate();
        boardPanel.repaint();
    }

   /**
     * Handles the full process of a player selecting and answering a question:
     * <ul>
     *   <li>Ensures game is in progress</li>
     *   <li>Logs system navigation events (Select Category / Select Question)</li>
     *   <li>Validates that the question exists and is unanswered</li>
     *   <li>Displays a multiple-choice dialog for the player</li>
     *   <li>Submits the answer to the controller</li>
     *   <li>Displays correct/incorrect feedback</li>
     *   <li>Disables the question button</li>
     *   <li>Advances the turn</li>
     *   <li>Checks if the game should end</li>
     * </ul>
     *
     * @param categoryName the category containing the question
     * @param value        the point value of the selected question
     * @param button       the UI button associated with this question
     */
   private void onQuestionClicked(String categoryName, int value, JButton button) {
    if (!GameState.IN_PROGRESS.equals(controller.getGameState().getStatus())) {
        JOptionPane.showMessageDialog(this,
                "Game is not in progress.",
                "Info",
                JOptionPane.INFORMATION_MESSAGE);
        return;
    }

    // Process log: Select Category & Select Question
    logSystem("Select Category", categoryName, null, null);
    logSystem("Select Question", categoryName, value, null);

    Question q = controller.getQuestion(categoryName, value);
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

    button.setEnabled(false);
    refreshScores();

    // FIX: Move to next player after each question
    controller.nextPlayer();

    boolean ended = controller.checkAndEndGame();
    if (ended || controller.isGameFinished()) {
        onGameFinished();
    } else {
        statusLabel.setText("Current player: " + controller.getCurrentPlayer().getName());
    }
}

    //  Scores & game end

     /**
     * Refreshes the right-hand scoreboard panel, clearing old entries
     * and recreating score cards for each active player.
     * <p>
     * Called after:
     * <ul>
     *   <li>Game start</li>
     *   <li>Each answered question</li>
     *   <li>Game reset</li>
     * </ul>
     */
    private void refreshScores() {
        scorePanel.removeAll();

        List<Player> players = controller.getPlayers();
        if (players.isEmpty()) {
            JLabel noPlayers = new JLabel("No players yet.", SwingConstants.CENTER);
            scorePanel.add(noPlayers);
        } else {
            for (Player p : players) {
                scorePanel.add(createPlayerScoreCard(p));
                scorePanel.add(Box.createVerticalStrut(8));
            }
        }

        scorePanel.revalidate();
        scorePanel.repaint();
    }

    /**
     * Creates a visual "score card" used in the sidebar,
     * showing a player's name and current score.
     *
     * @param player the player to display
     * @return a Swing component containing the formatted score card
     */
    private JComponent createPlayerScoreCard(Player player) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 200), 1),
                new EmptyBorder(8, 8, 8, 8)
        ));
        card.setBackground(new Color(245, 247, 252));

        JLabel nameLabel = new JLabel(player.getName());
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 14f));

        JLabel scoreLabel = new JLabel(String.valueOf(player.getScore()));
        scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.BOLD, 20f));
        scoreLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        card.add(nameLabel, BorderLayout.WEST);
        card.add(scoreLabel, BorderLayout.EAST);

        return card;
    }

    /**
     * Handles all end-of-game operations:
     * <ul>
     *   <li>Determines winner(s)</li>
     *   <li>Displays end-of-game message</li>
     *   <li>Updates status bar</li>
     *   <li>Generates summary report</li>
     *   <li>Logs report & exit events</li>
     *   <li>Disables all question buttons</li>
     *   <li>Unlocks UI for loading new game data</li>
     * </ul>
     * This method is triggered when:
     * <ul>
     *   <li>All questions have been answered</li>
     *   <li>The user manually ends the game</li>
     * </ul>
     */
    private void onGameFinished() {
        if (!gameInProgress) {
            return;
        }

        gameInProgress = false;

        // UPDATED: Use tie handling to get winners
        List<Player> winners = controller.getWinners();
        String message;
        
        if (winners.isEmpty()) {
            message = "Game over! No winner determined.";
        } else if (winners.size() == 1) {
            message = String.format("Game over! Winner: %s with %d points.",
                    winners.get(0).getName(), winners.get(0).getScore());
        } else {
            // Tie scenario
            StringBuilder tieMessage = new StringBuilder("Game over! It's a tie! Winners: ");
            for (int i = 0; i < winners.size(); i++) {
                tieMessage.append(winners.get(i).getName())
                         .append(" (").append(winners.get(i).getScore()).append(" points)");
                if (i < winners.size() - 1) {
                    tieMessage.append(", ");
                }
            }
            message = tieMessage.toString();
        }

        JOptionPane.showMessageDialog(this,
                message,
                "Game Finished",
                JOptionPane.INFORMATION_MESSAGE);

        statusLabel.setText("Game finished. " + message);

        // Generate summary report when game finishes
        try {
            Path reportPath = controller.generateSummaryReport();

            // Process log: Generate Report
            logSystem("Generate Report", null, null, null);

            logSystem("Generate Event Log", null, null, null);
            logSystem("Exit Game", null, null, null);

            JOptionPane.showMessageDialog(this,
                    "Summary report generated:\n" + reportPath.toAbsolutePath(),
                    "Summary Report",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not generate summary report:\n" + ex.getMessage(),
                    "Report Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        Component[] components = boardPanel.getComponents();
        for (int i = 0; i < components.length; i++) {
            Component c = components[i];
            if (c instanceof JButton) {
                JButton btn = (JButton) c;
                btn.setEnabled(false);
            }
        }

        endGameButton.setEnabled(false);
        loadButton.setEnabled(true);
        playerCountSpinner.setEnabled(true);
        for (JTextField tf : playerNameFields) {
            tf.setEnabled(true);
        }
        newGameButton.setEnabled(true);
    }

    /**
     * Resets the entire UI and game controller to allow starting a new game.
     * <p>
     * This method restores:
     * <ul>
     *   <li>A new GameController instance</li>
     *   <li>All questions reset to unanswered (if using same GameData)</li>
     *   <li>Board cleared to default message</li>
     *   <li>Player name fields re-enabled</li>
     *   <li>Buttons reset to initial enabled/disabled states</li>
     *   <li>Score panel cleared</li>
     * </ul>
     * <p>
     * Called after finishing a game when the user clicks "New Game".
     */
    private void resetGame() {
    // Create a completely new controller to reset everything
    this.controller = new GameController();
    this.gameInProgress = false;

    // If you're reusing the same GameData, reset all questions
    if (loadedGameData != null) {
        for (Category category : loadedGameData.getCategories()) {
            for (Question question : category.getAllQuestions()) {
                question.setAnswered(false); // Reset all questions to unanswered
            }
        }
    }

    // Clear board
    boardPanel.removeAll();
    boardPanel.add(new JLabel("Load questions and start the game to see the board.",
            SwingConstants.CENTER));

    // Reset player fields
    for (JTextField tf : playerNameFields) {
        tf.setText("");
        tf.setEnabled(true);
    }

    // Reset controls
    startGameButton.setEnabled(loadedGameData != null);
    endGameButton.setEnabled(false);
    newGameButton.setEnabled(false);
    loadButton.setEnabled(true);
    playerCountSpinner.setEnabled(true);

    // Reset status
    statusLabel.setText("Game reset. Load questions or start again.");

    // Reset scores panel
    scorePanel.removeAll();
    scorePanel.add(new JLabel("No players yet.", SwingConstants.CENTER));

    boardPanel.revalidate();
    boardPanel.repaint();
    scorePanel.revalidate();
    scorePanel.repaint();
}
}