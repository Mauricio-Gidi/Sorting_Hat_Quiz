// Config.java

package config;

/**
 * Centralizes application configuration constants.
 *
 * Provides classpath resource paths and numeric tuning parameters used across the
 * application. This class is not instantiable.
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
public final class Config
{
    /**
     * Prevents instantiation of this utility class.
     */
    private Config(){}

    // Classpath resource locations.
    public static final String TRAIT_TO_HOUSE_WEIGHTS_PATH =
            "/resources/TraitToHouseWeights.json";
    public static final String LIKERT_ITEMS_PATH =
            "/resources/LikertItems.json";
    public static final String FORCED_CHOICE_ITEMS_PATH =
            "/resources/ForcedChoiceItems.json";
    public static final String HAT_ICON_PATH =
            "/resources/HatIcon.png";
    public static final String PARCHMENT_BACKGROUND_PATH =
            "/resources/ParchmentBackground.png";
    public static final String QUICK_ICON_PATH =
            "/resources/QuickIcon.png";
    public static final String STANDARD_ICON_PATH =
            "/resources/StandardIcon.png";
    public static final String THOROUGH_ICON_PATH =
            "/resources/ThoroughIcon.png";

    // Scoring and model tuning parameters.
    public static final double TIE_THRESHOLD = 0.25;
    public static final double FC_LEARNING_RATE = 0.1;
    public static final double THETA_MIN = -3.0;
    public static final double THETA_MAX = 3.0;
}
