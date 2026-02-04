// MainWindow.java

package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import view.SortingHatTheme.Colors;

/**
 * Main application window that hosts all quiz screens and routes UI events to a listener.
 * This class owns the top-level Swing frame, the card-based screen container, and
 * convenience methods for showing each screen while tracking response timing.
 * 
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
public final class MainWindow extends JFrame
{
    /**
     * Identifiers for the screens managed by this window's CardLayout.
     */
    public enum ScreenIdentifier
    {
        WELCOME,
        MODE_SELECTION,
        LIKERT_QUESTION,
        FORCED_CHOICE,
        RESULTS
    }

    /**
     * Supported quiz length modes offered to the user.
     */
    public enum QuizLengthMode
    {
        QUICK,
        STANDARD,
        THOROUGH
    }

    /**
     * Callback interface for user actions originating from the UI.
     */
    public interface UserActionListener
    {
        /**
         * Notifies that the welcome screen has finished and the user can proceed.
         */
        default void onWelcomeScreenFinished()
        {
        }

        /**
         * Notifies that the user selected a quiz mode.
         *
         * @param selectedMode Quiz length mode selected by the user.
         */
        default void onQuizModeSelected(QuizLengthMode selectedMode)
        {
        }

        /**
         * Notifies that the user submitted a Likert answer.
         *
         * @param selectedLikertValue Numeric Likert value selected by the user.
         * @param responseTimeSeconds Seconds elapsed since the question was shown.
         */
        default void onLikertAnswerSubmitted(double selectedLikertValue, double responseTimeSeconds)
        {
        }

        /**
         * Notifies that the user selected an option during a forced-choice question.
         *
         * @param selectedOptionKey Key identifying the selected option.
         * @param responseTimeSeconds Seconds elapsed since the question was shown.
         */
        default void onForcedChoiceOptionSelected(String selectedOptionKey, double responseTimeSeconds)
        {
        }

        /**
         * Notifies that the user requested to view their trait profile details.
         */
        default void onTraitProfileRequested()
        {
        }

        /**
         * Notifies that the user explicitly requested to quit the application.
         */
        default void onQuitRequested()
        {
        }

        /**
         * Notifies that the window close gesture was used (e.g., clicking the close button).
         */
        default void onWindowCloseRequested()
        {
        }
    }

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardContainer = new JPanel(cardLayout);
    private final ParchmentBackgroundPanel background = new ParchmentBackgroundPanel();

    private final WelcomeScreenPanel welcomeScreen = new WelcomeScreenPanel();
    private final ModeSelectionScreenPanel modeSelectionScreen = new ModeSelectionScreenPanel();
    private final LikertQuestionScreenPanel likertScreen = new LikertQuestionScreenPanel();
    private final ForcedChoiceQuestionScreenPanel forcedChoiceScreen = new ForcedChoiceQuestionScreenPanel();
    private final ResultsScreenPanel resultsScreen = new ResultsScreenPanel();

    // Listener provided by the controller; used to dispatch UI events.
    private volatile UserActionListener userActionListener;

    // Tracks response time for the currently visible question screen.
    private final QuestionTimer questionTimer = new QuestionTimer();

    // Aspect ratio lock state for the window resize handler.
    private boolean aspectRatioLockEnabled = false;
    private double widthToHeightAspectRatio = 0.0;
    private Dimension lastUserResizeSize = null;
    private boolean isProgrammaticResizeInProgress = false;
    private boolean aspectRatioHandlerInstalled = false;

    /**
     * Creates the main application window, registers screens, and wires UI events.
     */
    public MainWindow()
    {
        super("Sorting Hat");

        setBackground(Colors.MIDNIGHT_GRADIENT_BOTTOM);

        background.setLayout(new BorderLayout());
        cardContainer.setOpaque(false);

        registerScreens();

        background.add(cardContainer, BorderLayout.CENTER);
        setContentPane(background);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                dispatch(UserActionListener::onWindowCloseRequested);
            }
        });

        wireScreenEvents();

        setMinimumSize(new Dimension(900, 560));
        setPreferredSize(new Dimension(1100, 700));
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Sets the listener that receives callbacks for user actions.
     *
     * @param listener Listener to receive user action callbacks.
     *
     * @throws NullPointerException If listener is null.
     */
    public void setUserActionListener(UserActionListener listener)
    {
        this.userActionListener = Objects.requireNonNull(listener, "listener");
    }

    /**
     * Sets the window icon image.
     *
     * @param windowIconImage Image used as the window icon.
     *
     * @throws NullPointerException If windowIconImage is null.
     */
    public void setWindowIconImage(Image windowIconImage)
    {
        Objects.requireNonNull(windowIconImage, "windowIconImage");
        runOnEdt(() ->
        {
            setIconImage(windowIconImage);
        });
    }

    /**
     * Locks the window to a fixed width-to-height aspect ratio during user resizes.
     *
     * @param widthToHeightAspectRatio Desired width divided by height aspect ratio.
     *
     * @throws IllegalArgumentException If widthToHeightAspectRatio is not greater than 0.
     */
    public void setFixedWindowAspectRatio(double widthToHeightAspectRatio)
    {
        if (widthToHeightAspectRatio <= 0.0)
        {
            throw new IllegalArgumentException("widthToHeightAspectRatio must be > 0");
        }

        this.widthToHeightAspectRatio = widthToHeightAspectRatio;
        this.aspectRatioLockEnabled = true;
        this.lastUserResizeSize = getSize();

        if (!aspectRatioHandlerInstalled)
        {
            aspectRatioHandlerInstalled = true;
            installAspectRatioResizeHandler();
        }
    }

    /**
     * Shows the window on the Event Dispatch Thread and maximizes it.
     */
    public void showWindow()
    {
        runOnEdt(() ->
        {
            setExtendedState(getExtendedState() | Frame.MAXIMIZED_BOTH);
            setVisible(true);
        });
    }

    /**
     * Closes and disposes the window on the Event Dispatch Thread.
     */
    public void closeWindow()
    {
        runOnEdt(this::dispose);
    }

    /**
     * Displays the welcome screen with the provided title content.
     *
     * @param welcomeTitleHtml HTML to display as the welcome title content.
     *
     * @throws NullPointerException If welcomeTitleHtml is null.
     */
    public void showWelcomeScreen(String welcomeTitleHtml)
    {
        Objects.requireNonNull(welcomeTitleHtml, "welcomeTitleHtml");

        runOnEdt(() ->
        {
            welcomeScreen.displayWelcomeTitle(welcomeTitleHtml);
            showScreen(ScreenIdentifier.WELCOME);
            welcomeScreen.startEntranceAnimation();
        });
    }

    /**
     * Displays the quiz mode selection screen.
     */
    public void showModeSelectionScreen()
    {
        runOnEdt(() ->
        {
            background.clearBackgroundTint();
            showScreen(ScreenIdentifier.MODE_SELECTION);
            modeSelectionScreen.requestFocusForDefaultControl();
        });
    }

    /**
     * Displays a Likert question screen and starts response-time tracking.
     *
     * @param questionText Text of the question being asked.
     * @param questionIndexOneBased One-based index of the current question.
     * @param totalQuestionCount Total number of questions for this session.
     *
     * @throws NullPointerException If questionText is null.
     */
    public void showLikertQuestionScreen(String questionText, int questionIndexOneBased, int totalQuestionCount)
    {
        Objects.requireNonNull(questionText, "questionText");

        runOnEdt(() ->
        {
            likertScreen.displayQuestion(questionText, questionIndexOneBased, totalQuestionCount);
            showScreen(ScreenIdentifier.LIKERT_QUESTION);
            questionTimer.markShownNow();
            likertScreen.requestFocusForDefaultControl();
        });
    }

    /**
     * Displays an interlude message on the forced-choice screen (no answer buttons).
     *
     * @param messageText Interlude message text to display.
     *
     * @throws NullPointerException If messageText is null.
     */
    public void showTieBreakerInterlude(String messageText)
    {
        Objects.requireNonNull(messageText, "messageText");

        runOnEdt(() ->
        {
            forcedChoiceScreen.displayInterlude(messageText);
            showScreen(ScreenIdentifier.FORCED_CHOICE);
        });
    }

    /**
     * Displays a forced-choice question screen and starts response-time tracking.
     *
     * @param promptText Prompt text to show above the options.
     * @param optionKeyLeft Key identifying the left option.
     * @param optionTextLeft Display text for the left option.
     * @param optionKeyRight Key identifying the right option.
     * @param optionTextRight Display text for the right option.
     * @param questionIndexOneBased One-based index of the current question within the tie-breaker flow.
     * @param totalQuestionCount Total number of forced-choice questions planned for the round.
     *
     * @throws NullPointerException If any required string argument is null.
     */
    public void showForcedChoiceQuestionScreen(
            String promptText,
            String optionKeyLeft,
            String optionTextLeft,
            String optionKeyRight,
            String optionTextRight,
            int questionIndexOneBased,
            int totalQuestionCount
    )
    {
        Objects.requireNonNull(promptText, "promptText");
        Objects.requireNonNull(optionKeyLeft, "optionKeyLeft");
        Objects.requireNonNull(optionTextLeft, "optionTextLeft");
        Objects.requireNonNull(optionKeyRight, "optionKeyRight");
        Objects.requireNonNull(optionTextRight, "optionTextRight");

        runOnEdt(() ->
        {
            forcedChoiceScreen.displayQuestion(
                    promptText,
                    optionKeyLeft,
                    optionTextLeft,
                    optionKeyRight,
                    optionTextRight,
                    questionIndexOneBased,
                    totalQuestionCount
            );
            showScreen(ScreenIdentifier.FORCED_CHOICE);
            questionTimer.markShownNow();
            forcedChoiceScreen.requestFocusForDefaultControl();
        });
    }

    /**
     * Displays the results screen and fades the parchment background tint to match the winning house.
     *
     * @param winningHouseName House name chosen as the winner.
     * @param houseProbabilitiesByHouseName Map of house name to probability value.
     * @param winningHouseTintColor Color used to tint the background.
     *
     * @throws NullPointerException If winningHouseName is null.
     * @throws NullPointerException If houseProbabilitiesByHouseName is null.
     * @throws NullPointerException If winningHouseTintColor is null.
     */
    public void showResultsScreen(
            String winningHouseName,
            Map<String, Double> houseProbabilitiesByHouseName,
            Color winningHouseTintColor
    )
    {
        Objects.requireNonNull(winningHouseName, "winningHouseName");
        Objects.requireNonNull(houseProbabilitiesByHouseName, "houseProbabilitiesByHouseName");
        Objects.requireNonNull(winningHouseTintColor, "winningHouseTintColor");

        runOnEdt(() ->
        {
            background.fadeBackgroundTintToColor(winningHouseTintColor);
            resultsScreen.displayResults(winningHouseName, houseProbabilitiesByHouseName);
            showScreen(ScreenIdentifier.RESULTS);
            resultsScreen.requestFocusForDefaultControl();
        });
    }

    /**
     * Shows a dialog describing trait scores using the results screen as the host.
     *
     * @param traitScoresByTraitName Map of trait name to score.
     *
     * @throws NullPointerException If traitScoresByTraitName is null.
     */
    public void showTraitProfileDialog(Map<String, Double> traitScoresByTraitName)
    {
        Objects.requireNonNull(traitScoresByTraitName, "traitScoresByTraitName");

        runOnEdt(() ->
        {
            resultsScreen.showTraitProfileDialog(MainWindow.this, traitScoresByTraitName);
        });
    }

    /**
     * Shows an error dialog on the Event Dispatch Thread.
     *
     * @param dialogTitleText Title text for the dialog.
     * @param dialogMessageText Message body text for the dialog.
     *
     * @throws NullPointerException If dialogTitleText is null.
     * @throws NullPointerException If dialogMessageText is null.
     */
    public void showErrorDialog(String dialogTitleText, String dialogMessageText)
    {
        Objects.requireNonNull(dialogTitleText, "dialogTitleText");
        Objects.requireNonNull(dialogMessageText, "dialogMessageText");

        runOnEdt(() ->
        {
            JOptionPane.showMessageDialog(
                    MainWindow.this,
                    dialogMessageText,
                    dialogTitleText,
                    JOptionPane.ERROR_MESSAGE
            );
        });
    }

    /**
     * Registers each screen panel with the CardLayout container.
     */
    private void registerScreens()
    {
        addScreen(ScreenIdentifier.WELCOME, welcomeScreen);
        addScreen(ScreenIdentifier.MODE_SELECTION, modeSelectionScreen);
        addScreen(ScreenIdentifier.LIKERT_QUESTION, likertScreen);
        addScreen(ScreenIdentifier.FORCED_CHOICE, forcedChoiceScreen);
        addScreen(ScreenIdentifier.RESULTS, resultsScreen);
    }

    /**
     * Adds a screen component to the card container under the given identifier.
     *
     * @param id Identifier used to reference the screen.
     * @param component Swing component that renders the screen.
     *
     * @throws NullPointerException If id is null.
     * @throws NullPointerException If component is null.
     */
    private void addScreen(ScreenIdentifier id, JComponent component)
    {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(component, "component");

        cardContainer.add(component, id.name());
    }

    /**
     * Switches the visible card to the requested screen.
     *
     * @param id Identifier of the screen to show.
     *
     * @throws NullPointerException If id is null.
     */
    private void showScreen(ScreenIdentifier id)
    {
        Objects.requireNonNull(id, "id");

        cardLayout.show(cardContainer, id.name());
        cardContainer.revalidate();
        cardContainer.repaint();
    }

    /**
     * Wires events from the screen panels to callbacks on the UserActionListener.
     */
    private void wireScreenEvents()
    {
        welcomeScreen.setWelcomeScreenFinishedListener(() ->
                dispatch(UserActionListener::onWelcomeScreenFinished)
        );

        modeSelectionScreen.setQuizModeSelectionListener(event ->
        {
            QuizLengthMode mode = tryParseQuizLengthMode(event);
            if (mode == null)
            {
                return;
            }
            dispatch(l -> l.onQuizModeSelected(mode));
        });

        likertScreen.setLikertAnswerSubmissionListener(event ->
        {
            Double value = tryParseDouble(event.getActionCommand());
            if (value == null)
            {
                return;
            }
            dispatch(l -> l.onLikertAnswerSubmitted(value, questionTimer.elapsedSeconds()));
        });

        forcedChoiceScreen.setForcedChoiceAnswerSubmissionListener(event ->
        {
            String key = event.getActionCommand();
            if (key == null || key.isBlank())
            {
                return;
            }
            dispatch(l -> l.onForcedChoiceOptionSelected(key, questionTimer.elapsedSeconds()));
        });

        resultsScreen.setTraitProfileRequestListener(() ->
                dispatch(UserActionListener::onTraitProfileRequested)
        );

        resultsScreen.setQuitRequestListener(() ->
                dispatch(UserActionListener::onQuitRequested)
        );
    }

    /**
     * Executes a callback against the current listener if one is installed.
     *
     * @param call Consumer that invokes one listener callback.
     *
     * @throws NullPointerException If call is null.
     */
    private void dispatch(Consumer<UserActionListener> call)
    {
        UserActionListener listener = userActionListener;
        if (listener == null)
        {
            return;
        }
        call.accept(listener);
    }

    /**
     * Attempts to parse a quiz length mode from an ActionEvent command string.
     *
     * @param event Action event whose command identifies the selected mode.
     * @return Parsed quiz length mode, or null if the event cannot be interpreted.
     */
    private static QuizLengthMode tryParseQuizLengthMode(ActionEvent event)
    {
        if (event == null)
        {
            return null;
        }

        String cmd = event.getActionCommand();
        if (cmd == null)
        {
            return null;
        }

        try
        {
            return QuizLengthMode.valueOf(cmd);
        }
        catch (RuntimeException ignored)
        {
            return null;
        }
    }

    /**
     * Attempts to parse a Double from the provided text.
     *
     * @param text Text to parse as a double.
     * @return Parsed value, or null if parsing fails.
     */
    private static Double tryParseDouble(String text)
    {
        if (text == null)
        {
            return null;
        }

        try
        {
            return Double.parseDouble(text);
        }
        catch (RuntimeException ignored)
        {
            return null;
        }
    }

    /**
     * Runs a task on the Swing Event Dispatch Thread.
     *
     * @param task Runnable to execute.
     *
     * @throws NullPointerException If task is null.
     */
    private static void runOnEdt(Runnable task)
    {
        Objects.requireNonNull(task, "task");

        if (SwingUtilities.isEventDispatchThread())
        {
            task.run();
        }
        else
        {
            SwingUtilities.invokeLater(task);
        }
    }

    /**
     * Tracks the time elapsed between showing a question and submitting an answer.
     */
    private static final class QuestionTimer
    {
        private long startNanos = 0L;

        /**
         * Marks the current time as the question shown timestamp.
         */
        void markShownNow()
        {
            startNanos = System.nanoTime();
        }

        /**
         * Returns the elapsed time since the last #markShownNow() call.
         *
         * @return Elapsed time in seconds, or 0.0 if the timer was never started.
         */
        double elapsedSeconds()
        {
            long s = startNanos;
            if (s <= 0L)
            {
                return 0.0;
            }
            return (System.nanoTime() - s) / 1_000_000_000.0;
        }
    }

    /**
     * Installs a component resize handler that enforces the configured aspect ratio lock.
     */
    private void installAspectRatioResizeHandler()
    {
        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                if (!aspectRatioLockEnabled)
                {
                    return;
                }
                if (isProgrammaticResizeInProgress)
                {
                    return;
                }
                maintainWindowAspectRatio(widthToHeightAspectRatio);
            }
        });
    }

    /**
     * Adjusts the window size to maintain the requested aspect ratio while respecting minimum size.
     *
     * @param ratio Width-to-height ratio to maintain.
     */
    private void maintainWindowAspectRatio(double ratio)
    {
        Dimension current = getSize();
        Dimension prev = (lastUserResizeSize != null) ? lastUserResizeSize : current;

        int currentW = Math.max(1, current.width);
        int currentH = Math.max(1, current.height);

        int dw = Math.abs(currentW - prev.width);
        int dh = Math.abs(currentH - prev.height);

        boolean keepWidth = dw >= dh;
        int adjustedW = keepWidth ? currentW : (int) Math.round(currentH * ratio);
        int adjustedH = keepWidth ? (int) Math.round(currentW / ratio) : currentH;

        Dimension min = getMinimumSize();
        if (min != null)
        {
            if (adjustedW < min.width)
            {
                adjustedW = min.width;
                adjustedH = (int) Math.round(adjustedW / ratio);
            }
            if (adjustedH < min.height)
            {
                adjustedH = min.height;
                adjustedW = (int) Math.round(adjustedH * ratio);
            }
        }

        Dimension adjusted = new Dimension(Math.max(1, adjustedW), Math.max(1, adjustedH));
        lastUserResizeSize = adjusted;

        if (adjusted.equals(current))
        {
            return;
        }

        isProgrammaticResizeInProgress = true;
        try
        {
            setSize(adjusted);
        }
        finally
        {
            isProgrammaticResizeInProgress = false;
        }
    }
}
