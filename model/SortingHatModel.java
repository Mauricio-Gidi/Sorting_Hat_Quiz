// SortingHatModel.java

package model;

import java.util.ArrayList;
import java.util.Map;

/**
 * Facade model that coordinates item selection and scoring for the Sorting Hat quiz.
 * 
 * This class loads item data from JSON resources via ItemBank, delegates scoring to
 * ScoringEngine, and exposes a simplified API for controllers and views.
 * 
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
public final class SortingHatModel
{
    /**
     * Quick mode selection identifier.
     */
    public static final int MODE_QUICK = 1;

    /**
     * Standard mode selection identifier.
     */
    public static final int MODE_STANDARD = 2;

    /**
     * Thorough mode selection identifier.
     */
    public static final int MODE_THOROUGH = 3;

    // Loads and serves items (Likert and forced-choice) from classpath JSON resources.
    private final ItemBank itemBank;

    // Maintains and updates trait/house scores and probabilities.
    private final ScoringEngine scoringEngine;

    /**
     * Creates a model by loading resources and initializing scoring.
     * 
     * The supplied paths are expected to be absolute classpath resource paths (for example,
     * "/LikertItems.json"). Resource loading and validation are performed by the underlying ItemBank.
     * 
     * @param traitToHouseWeightsPath Absolute classpath path to the trait-to-house weights JSON.
     * @param likertItemsPath         Absolute classpath path to the Likert items JSON.
     * @param forcedChoiceItemsPath   Absolute classpath path to the forced-choice items JSON.
     * @throws IllegalArgumentException If any provided path is null or not an absolute classpath
     *                                 path (does not start with "/").
     * @throws IllegalStateException    If required resources are missing or contain invalid data
     *                                 (for example, blank or duplicate item identifiers).
     * @throws RuntimeException         If a resource cannot be read or parsed as JSON.
     */
    public SortingHatModel(
            String traitToHouseWeightsPath,
            String likertItemsPath,
            String forcedChoiceItemsPath
    )
    {
        this.itemBank = new ItemBank(traitToHouseWeightsPath, likertItemsPath, forcedChoiceItemsPath);
        this.scoringEngine = new ScoringEngine(
                itemBank.getTraits(),
                itemBank.getHouses(),
                itemBank.getTraitToHouseWeights()
        );

        // Provide the scoring engine to the item bank so forced-choice answers can update scores.
        this.itemBank.attachScoringEngine(this.scoringEngine);
    }

    /**
     * Selects a randomized list of Likert items appropriate for the chosen mode.
     * 
     * This delegates to ItemBank to choose the correct form (quick/standard) or
     * the full set (thorough), and returns the resulting list.
     * 
     * @param modeChoice One of #MODE_QUICK, #MODE_STANDARD, or #MODE_THOROUGH.
     * @return A randomized list of Likert items for the requested mode.
     */
    public ArrayList<LikertItem> selectLikertItemsForMode(int modeChoice)
    {
        return itemBank.selectLikertItemsForMode(modeChoice);
    }

    /**
     * Records a Likert response and updates internal scoring state.
     * 
     * @param item            The item that was answered.
     * @param responseValue   The numeric response value (typically in the 1–5 range, possibly fractional).
     * @param responseTimeSec The response time in seconds.
     */
    public void recordLikertResponse(LikertItem item, double responseValue, double responseTimeSec)
    {
        scoringEngine.recordLikertResponse(item, responseValue, responseTimeSec);
    }

    /**
     * Recomputes house scores and probabilities, then returns the current probabilities.
     * 
     * @return A snapshot of house probabilities after recomputation.
     */
    public Map<String, Double> recomputeAndGetHouseProbabilities()
    {
        scoringEngine.recomputeHouseScores();
        return scoringEngine.getHouseProbabilities();
    }

    /**
     * Returns the current house probability snapshot without forcing recomputation.
     * 
     * @return A snapshot of the current house probabilities.
     */
    public Map<String, Double> getHouseProbabilities()
    {
        return scoringEngine.getHouseProbabilities();
    }

    /**
     * Returns the house name with the highest current probability.
     * 
     * @return The name of the most probable house.
     */
    public String getTopHouse()
    {
        return scoringEngine.getTopHouse();
    }

    /**
     * Returns the set of houses whose probability is within a threshold of the current maximum.
     * 
     * @param threshold Maximum allowed difference from the highest probability to be considered tied.
     * @return A list of house names that fall within the threshold of the top probability.
     */
    public ArrayList<String> getTieGroup(double threshold)
    {
        return scoringEngine.getTieGroup(threshold);
    }

    /**
     * Returns the set of houses within a threshold of the maximum, restricted to a subset.
     * 
     * @param threshold Maximum allowed difference from the highest probability to be considered tied.
     * @param subset    The candidate house names to consider when computing ties.
     * @return A list of house names from the subset that fall within the threshold of the top probability.
     */
    public ArrayList<String> getTieGroup(double threshold, ArrayList<String> subset)
    {
        return scoringEngine.getTieGroup(threshold, subset);
    }

    /**
     * Returns a snapshot of the current trait scores.
     * 
     * @return A map from trait name to its current score.
     */
    public Map<String, Double> getTraitScoresSnapshot()
    {
        return scoringEngine.getTraitScoresSnapshot();
    }

    /**
     * Selects the next forced-choice item for the given pair of houses.
     * 
     * @param houseA The first house in the comparison pair.
     * @param houseB The second house in the comparison pair.
     * @return The next forced-choice item applicable to the house pair.
     * @throws IllegalStateException If there are no forced-choice items available for the requested pair.
     */
    public ForcedChoiceItem selectNextForcedChoiceItemForPair(String houseA, String houseB)
    {
        return itemBank.selectNextForcedChoiceItemForPair(houseA, houseB);
    }

    /**
     * Applies a forced-choice answer and updates scoring based on the selected option and timing.
     * 
     * @param item            The forced-choice item that was answered.
     * @param optionKey       The key identifying the chosen option.
     * @param responseTimeSec The response time in seconds.
     * @throws IllegalStateException If optionKey does not match any option on the provided item.
     */
    public void applyForcedChoiceAnswer(ForcedChoiceItem item, String optionKey, double responseTimeSec)
    {
        itemBank.applyForcedChoiceAnswer(item, optionKey, responseTimeSec);
    }

    /**
     * Returns how many Likert items will be presented for a given mode selection.
     * 
     * @param modeChoice One of #MODE_QUICK, #MODE_STANDARD, or #MODE_THOROUGH.
     * @return The number of Likert items that will be used for the chosen mode.
     */
    public int getLikertCountForMode(int modeChoice)
    {
        return itemBank.getLikertCountForMode(modeChoice);
    }

    /**
     * Container for graded response model (GRM) threshold parameters.
     * 
     * These values are typically populated from JSON and used during Likert scoring.
     */
    public static final class IrtThresholds
    {
        // Category boundary thresholds (b1..b4) used by the IRT model.
        public double b1;
        public double b2;
        public double b3;
        public double b4;
    }

    /**
     * Container for item response theory (IRT) parameters used by a Likert item.
     * 
     * Instances are populated from JSON and interpreted by the scoring engine.
     */
    public static final class LikertIrt
    {
        // IRT model identifier string (e.g., model name).
        public String model;

        // Discrimination parameter for the item.
        public double a;

        // GRM thresholds for category boundaries.
        public IrtThresholds thresholds;
    }

    /**
     * Timing configuration used to weight responses based on speed.
     * 
     * Instances are populated from JSON and passed into ResponseTimeWeight.
     */
    public static final class Timing
    {
        // Expected response time in seconds.
        public int expected_time_sec;

        // Response times below this threshold are considered rapid.
        public int rapid_threshold_sec;

        // Down-weight factor applied for rapid responses.
        public double down_weight_factor;
    }

    /**
     * Data model for a single Likert item.
     * 
     * Fields are populated from JSON and used for item presentation and scoring.
     */
    public static final class LikertItem
    {
        // Unique identifier for the item.
        public String id;

        // Display text presented to the user.
        public String text;

        // Trait weights applied when the item is answered.
        public Map<String, Double> trait_weights;

        // IRT parameters for mapping response values to latent trait level.
        public LikertIrt irt;

        // Timing configuration for response time weighting.
        public Timing timing;
    }

    /**
     * Data model for a single forced-choice option within a forced-choice item.
     * 
     * Fields are populated from JSON and used for display and scoring.
     */
    public static final class ForcedChoiceOption
    {
        // Option key used to identify the selection.
        public String key;

        // Display text for the option.
        public String text;

        // The house associated with selecting this option.
        public String house;
    }

    /**
     * Data model for a forced-choice comparison item.
     * 
     * Fields are populated from JSON and used for display and scoring.
     */
    public static final class ForcedChoiceItem
    {
        // Unique identifier for the item.
        public String id;

        // Two-house comparison pair.
        public ArrayList<String> house_pair;

        // Prompt/stem shown above the options.
        public String stem;

        // Available options for the comparison.
        public ArrayList<ForcedChoiceOption> options;

        // Timing configuration for response time weighting.
        public Timing timing;
    }
}
