// MainController.java

package controller;

import config.Config;
import model.SortingHatModel;
import view.MainWindow;
import view.SortingHatTheme;
import javax.swing.Timer;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

/**
 * Coordinates user interactions between the main window and the sorting model.
 * This controller drives the quiz flow: welcome, mode selection, Likert items,
 * optional forced-choice tie breakers, and results display.
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
public final class MainController implements MainWindow.UserActionListener
{
    private static final double TIE_THRESHOLD = Config.TIE_THRESHOLD;
    private static final int TIEBREAKER_INTERLUDE_DELAY_MS = 6000;

    private static final String WELCOME_TITLE_HTML =
            "<html><div style='text-align:center;'>"
                    + "<div style='font-size:42px; font-weight:800;'>The Sorting Hat</div>"
                    + "<div style='margin-top:10px; font-size:16px; opacity:0.9;'>Answer honestly. Your House will find you.</div>"
                    + "</div></html>";

    private final SortingHatModel sortingHatModel;
    private final MainWindow mainWindow;

    // Active quiz session state.
    private LikertSession likertSession = null;
    private TieBreakerSession tieBreakerSession = null;

    // Single-shot timer used to delay the tie-breaker screen after the interlude message.
    private Timer tieBreakerInterludeTimer = null;

    /**
     * Creates a controller that connects the provided model and main window.
     *
     * @param sortingHatModel Model used to compute scores and select items.
     * @param mainWindow Main application window used for rendering and input.
     *
     * @throws NullPointerException If sortingHatModel or mainWindow is null.
     */
    public MainController(SortingHatModel sortingHatModel, MainWindow mainWindow)
    {
        this.sortingHatModel = Objects.requireNonNull(sortingHatModel, "sortingHatModel");
        this.mainWindow = Objects.requireNonNull(mainWindow, "mainWindow");
    }

    /**
     * Starts the application UI and displays the initial welcome screen.
     */
    public void start()
    {
        mainWindow.setUserActionListener(this);
        mainWindow.setWindowIconImage(SortingHatTheme.ImageAssets.SORTING_HAT_ICON_IMAGE);

        double ratio = SortingHatTheme.ImageAssets.getParchmentBackgroundAspectRatioOrDefault(16.0 / 9.0);
        mainWindow.setFixedWindowAspectRatio(ratio);

        mainWindow.showWindow();
        mainWindow.showWelcomeScreen(WELCOME_TITLE_HTML);
    }

    /**
     * Handles the welcome screen completion by returning to quiz mode selection.
     */
    @Override
    public void onWelcomeScreenFinished()
    {
        resetToModeSelection();
    }

    /**
     * Handles quiz mode selection by starting a new Likert session.
     *
     * @param selectedMode Selected quiz length mode from the UI.
     */
    @Override
    public void onQuizModeSelected(MainWindow.QuizLengthMode selectedMode)
    {
        cancelTieBreakerInterludeTimerIfRunning();

        likertSession = new LikertSession(convertQuizLengthModeToModelModeChoice(selectedMode));
        tieBreakerSession = null;

        likertSession.showCurrentOrFinish();
    }

    /**
     * Handles a submitted Likert response by recording it and moving to the next item.
     *
     * @param selectedLikertValue Likert response value selected by the user.
     * @param responseTimeSeconds Time between the question being shown and submission, in seconds.
     */
    @Override
    public void onLikertAnswerSubmitted(double selectedLikertValue, double responseTimeSeconds)
    {
        likertSession.recordAnswerAndAdvance(selectedLikertValue, responseTimeSeconds);
    }

    /**
     * Handles a forced-choice selection during tie breaking by recording it and advancing the flow.
     *
     * @param selectedOptionKey Key of the chosen forced-choice option.
     * @param responseTimeSeconds Time between the scenario being shown and selection, in seconds.
     */
    @Override
    public void onForcedChoiceOptionSelected(String selectedOptionKey, double responseTimeSeconds)
    {
        tieBreakerSession.recordAnswerAndAdvance(selectedOptionKey, responseTimeSeconds);
    }

    /**
     * Displays a trait profile dialog based on the current model scores.
     */
    @Override
    public void onTraitProfileRequested()
    {
        Map<String, Double> traitScoresByTraitName = sortingHatModel.getTraitScoresSnapshot();
        mainWindow.showTraitProfileDialog(traitScoresByTraitName);
    }

    /**
     * Exits the application in response to an explicit quit request.
     */
    @Override
    public void onQuitRequested()
    {
        shutdownNow();
    }

    /**
     * Exits the application when the window close request is received.
     */
    @Override
    public void onWindowCloseRequested()
    {
        shutdownNow();
    }

    /**
     * Clears current session state and returns the UI to quiz mode selection.
     */
    private void resetToModeSelection()
    {
        cancelTieBreakerInterludeTimerIfRunning();

        likertSession = null;
        tieBreakerSession = null;

        mainWindow.showModeSelectionScreen();
    }

    /**
     * Shuts down the application immediately.
     */
    private void shutdownNow()
    {
        cancelTieBreakerInterludeTimerIfRunning();
        mainWindow.closeWindow();
        System.exit(0);
    }

    /**
     * Stops the tie-breaker interlude timer if it is currently running.
     */
    private void cancelTieBreakerInterludeTimerIfRunning()
    {
        Timer t = tieBreakerInterludeTimer;
        if (t != null)
        {
            t.stop();
            tieBreakerInterludeTimer = null;
        }
    }

    /**
     * Shows the tie-breaker interlude message and then runs the provided next step.
     *
     * @param nextStep Action to run after the interlude delay.
     */
    private void showTieBreakerInterludeThen(Runnable nextStep)
    {
        cancelTieBreakerInterludeTimerIfRunning();

        mainWindow.showTieBreakerInterlude(
                "Oh my, difficult — very difficult indeed…\n\nHmm… where you will truly thrive\nis not obvious at all..."
        );

        tieBreakerInterludeTimer = new Timer(TIEBREAKER_INTERLUDE_DELAY_MS, e ->
        {
            cancelTieBreakerInterludeTimerIfRunning();
            nextStep.run();
        });
        tieBreakerInterludeTimer.setRepeats(false);
        tieBreakerInterludeTimer.start();
    }

    /**
     * Finalizes the Likert phase by recomputing probabilities and determining whether tie
     * breaking is required.
     */
    private void finishLikertPhaseAndDecideNextStep()
    {
        sortingHatModel.recomputeAndGetHouseProbabilities();

        ArrayList<String> tieGroup = sortingHatModel.getTieGroup(TIE_THRESHOLD);
        if (tieGroup.size() <= 1)
        {
            showResults();
            return;
        }

        int tieBreakerRounds = (likertSession != null) ? likertSession.modeChoice : SortingHatModel.MODE_STANDARD;
        tieBreakerSession = new TieBreakerSession(tieGroup, tieBreakerRounds);
        tieBreakerSession.advanceFlow();
    }

    /**
     * Displays the results screen using the model's top house and probability distribution.
     */
    private void showResults()
    {
        String winningHouseName = sortingHatModel.getTopHouse();
        Map<String, Double> probabilities = sortingHatModel.getHouseProbabilities();
        Color tint = getHouseColorForTint(winningHouseName);

        mainWindow.showResultsScreen(
                winningHouseName,
                probabilities,
                tint
        );
    }

    /**
     * Chooses an accent color used to tint the results background for the winning house.
     *
     * @param houseName Name of the house to map to an accent color.
     * @return A color suitable for a background tint.
     */
    private static Color getHouseColorForTint(String houseName)
    {
        if (houseName == null || houseName.isBlank())
        {
            return SortingHatTheme.Colors.TEXT_PRIMARY;
        }
        return SortingHatTheme.Colors.getHouseAccentColorByHouseName(houseName);
    }

    /**
     * Converts a UI quiz length selection into the corresponding model mode choice.
     *
     * @param selectedMode Selected quiz length mode from the UI.
     * @return Model mode choice constant used to select items.
     */
    private static int convertQuizLengthModeToModelModeChoice(MainWindow.QuizLengthMode selectedMode)
    {
        if (selectedMode == null) return SortingHatModel.MODE_STANDARD;

        switch (selectedMode)
        {
            case QUICK:
                return SortingHatModel.MODE_QUICK;

            case THOROUGH:
                return SortingHatModel.MODE_THOROUGH;

            case STANDARD:
            default:
                return SortingHatModel.MODE_STANDARD;
        }
    }

    /**
     * Represents a single Likert questionnaire run for a chosen mode.
     */
    private final class LikertSession
    {
        private final int modeChoice;
        private final ArrayList<SortingHatModel.LikertItem> items;
        private int index = 0;

        /**
         * Creates a Likert session by selecting items for the chosen mode.
         *
         * @param modeChoice Model mode choice constant used to select Likert items.
         */
        LikertSession(int modeChoice)
        {
            this.modeChoice = modeChoice;
            this.items = sortingHatModel.selectLikertItemsForMode(modeChoice);
        }

        /**
         * Shows the current Likert item, or finishes the Likert phase if all items are complete.
         */
        void showCurrentOrFinish()
        {
            if (index >= items.size())
            {
                finishLikertPhaseAndDecideNextStep();
                return;
            }

            SortingHatModel.LikertItem item = items.get(index);
            mainWindow.showLikertQuestionScreen(item.text, index + 1, items.size());
        }

        /**
         * Records the current answer in the model, advances the index, and shows the next item.
         *
         * @param selectedLikertValue Likert response value selected by the user.
         * @param responseTimeSeconds Time between the question being shown and submission, in seconds.
         */
        void recordAnswerAndAdvance(double selectedLikertValue, double responseTimeSeconds)
        {
            SortingHatModel.LikertItem item = items.get(index);
            sortingHatModel.recordLikertResponse(item, selectedLikertValue, responseTimeSeconds);

            index++;
            showCurrentOrFinish();
        }
    }

    /**
     * Handles forced-choice tie-breaking rounds when the top house probabilities are too close.
     */
    private final class TieBreakerSession
    {
        private ArrayList<String> tieGroup;
        private int roundsRemaining;
        private boolean interludeShown = false;

        private ArrayList<HousePair> roundPairs = new ArrayList<>();
        private int pairIndex = 0;
        private int questionsPerPair = 0;
        private int remainingQuestionsForCurrentPair = 0;

        private int roundQuestionsAnswered = 0;
        private int roundQuestionsPlannedTotal = 0;

        private SortingHatModel.ForcedChoiceItem pendingForcedChoiceItem = null;

        /**
         * Creates a tie-breaker session and initializes the first round for the given tie group.
         *
         * @param initialTieGroup Houses included in the tie group for the first round.
         * @param rounds Number of tie-breaker rounds to run at most.
         *
         * @throws NullPointerException If initialTieGroup is null.
         */
        TieBreakerSession(ArrayList<String> initialTieGroup, int rounds)
        {
            this.tieGroup = new ArrayList<>(Objects.requireNonNull(initialTieGroup, "initialTieGroup"));
            this.roundsRemaining = Math.max(0, rounds);

            prepareNextRound();
        }

        /**
         * Records a forced-choice answer and advances the tie-breaker flow.
         *
         * @param selectedOptionKey Key of the chosen forced-choice option.
         * @param responseTimeSeconds Time between the scenario being shown and selection, in seconds.
         */
        void recordAnswerAndAdvance(String selectedOptionKey, double responseTimeSeconds)
        {
            sortingHatModel.applyForcedChoiceAnswer(pendingForcedChoiceItem, selectedOptionKey, responseTimeSeconds);
            pendingForcedChoiceItem = null;

            roundQuestionsAnswered++;
            remainingQuestionsForCurrentPair--;

            advanceFlow();
        }

        /**
         * Advances the tie-breaker state machine until a screen is shown or results are reached.
         */
        void advanceFlow()
        {
            while (true)
            {
                if (tieGroup.size() <= 1 || roundsRemaining <= 0)
                {
                    showResults();
                    return;
                }

                if (!interludeShown)
                {
                    interludeShown = true;
                    showTieBreakerInterludeThen(this::advanceFlow);
                    return;
                }

                if (remainingQuestionsForCurrentPair > 0)
                {
                    showNextForcedChoiceQuestionForCurrentPair();
                    return;
                }

                pairIndex++;
                if (pairIndex < roundPairs.size())
                {
                    remainingQuestionsForCurrentPair = questionsPerPair;
                    continue;
                }

                roundsRemaining--;
                if (roundsRemaining <= 0)
                {
                    showResults();
                    return;
                }

                tieGroup = sortingHatModel.getTieGroup(TIE_THRESHOLD, new ArrayList<>(tieGroup));
                if (tieGroup.size() <= 1)
                {
                    showResults();
                    return;
                }

                prepareNextRound();
            }
        }

        /**
         * Prepares the next forced-choice round by building house pairs and resetting counters.
         */
        private void prepareNextRound()
        {
            this.roundPairs = buildAllUniqueHousePairs(tieGroup);
            this.questionsPerPair = (tieGroup.size() >= 4) ? 1 : 2;

            this.pairIndex = 0;
            this.remainingQuestionsForCurrentPair = (roundPairs.isEmpty() ? 0 : questionsPerPair);

            this.roundQuestionsAnswered = 0;
            this.roundQuestionsPlannedTotal = roundPairs.size() * questionsPerPair;

            this.pendingForcedChoiceItem = null;
        }

        /**
         * Selects and shows the next forced-choice question for the current house pair.
         */
        private void showNextForcedChoiceQuestionForCurrentPair()
        {
            HousePair pair = roundPairs.get(pairIndex);

            SortingHatModel.ForcedChoiceItem item =
                    sortingHatModel.selectNextForcedChoiceItemForPair(pair.houseA, pair.houseB);
            pendingForcedChoiceItem = item;

            SortingHatModel.ForcedChoiceOption left = item.options.get(0);
            SortingHatModel.ForcedChoiceOption right = item.options.get(1);

            int questionIndexOneBased = roundQuestionsAnswered + 1;

            mainWindow.showForcedChoiceQuestionScreen(
                    item.stem,
                    left.key,
                    left.text,
                    right.key,
                    right.text,
                    questionIndexOneBased,
                    roundQuestionsPlannedTotal
            );
        }
    }

    /**
     * Builds all unique ordered pairs from the provided list of house names.
     *
     * @param houseNames House names to pair for a round.
     * @return List of unique house pairs.
     */
    private static ArrayList<HousePair> buildAllUniqueHousePairs(ArrayList<String> houseNames)
    {
        ArrayList<HousePair> out = new ArrayList<>();
        if (houseNames == null) return out;

        int n = houseNames.size();
        for (int i = 0; i < n; i++)
        {
            for (int j = i + 1; j < n; j++)
            {
                out.add(new HousePair(houseNames.get(i), houseNames.get(j)));
            }
        }
        return out;
    }

    /**
     * Represents a specific pair of house names for a forced-choice comparison.
     */
    private static final class HousePair
    {
        final String houseA;
        final String houseB;

        /**
         * Creates a house pair for comparing two houses in a forced-choice round.
         *
         * @param houseA First house name.
         * @param houseB Second house name.
         *
         * @throws NullPointerException If houseA or houseB is null.
         */
        HousePair(String houseA, String houseB)
        {
            this.houseA = Objects.requireNonNull(houseA, "houseA");
            this.houseB = Objects.requireNonNull(houseB, "houseB");
        }
    }
}
