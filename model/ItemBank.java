// ItemBank.java

package model;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads and serves quiz items and scoring weights from JSON resources.
 * This class parses the configured resources into in-memory structures and
 * provides randomized item selection for different quiz-length modes.
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
public final class ItemBank
{
    /**
     * Represents the shape of the Likert items JSON file for GSON deserialization.
     */
    private static final class LikertFile
    {
        ArrayList<SortingHatModel.LikertItem> items;
        Forms forms;
        SortingHatModel.Timing timing;
    }

    /**
     * Represents the Likert form id lists (quick and standard) within the Likert file.
     */
    private static final class Forms
    {
        ArrayList<String> quick;
        ArrayList<String> standard;
    }

    /**
     * Represents the shape of the forced-choice items JSON file for GSON deserialization.
     */
    private static final class ForcedChoiceFile
    {
        ArrayList<SortingHatModel.ForcedChoiceItem> items;
        SortingHatModel.Timing timing;
    }

    /**
     * Represents the shape of the trait-to-house weights JSON file for GSON deserialization.
     */
    private static final class TraitHouseFile
    {
        ArrayList<String> traits;
        ArrayList<String> houses;
        Map<String, Map<String, Double>> weights;
    }

    private final ArrayList<SortingHatModel.LikertItem> likertItems;
    private final ArrayList<SortingHatModel.ForcedChoiceItem> forcedChoiceItems;
    private final Map<String, Map<String, Double>> traitToHouseWeights;
    private final ArrayList<String> traits;
    private final ArrayList<String> houses;

    // Lookup table used to map form IDs to Likert items quickly.
    private final Map<String, SortingHatModel.LikertItem> likertById;

    private final ArrayList<String> quickFormIds;
    private final ArrayList<String> standardFormIds;

    private ForcedChoice forcedChoice;

    /**
     * Constructs an item bank by loading all quiz resources from the classpath.
     *
     * @param traitToHouseWeightsPath Absolute classpath path for trait-to-house weights JSON.
     * @param likertItemsPath Absolute classpath path for Likert items JSON.
     * @param forcedChoiceItemsPath Absolute classpath path for forced-choice items JSON.
     *
     * @throws IllegalArgumentException If any provided path is not an absolute classpath path.
     * @throws IllegalStateException If required resources are missing or Likert ids are invalid.
     * @throws RuntimeException If a resource cannot be parsed or read successfully.
     */
    public ItemBank(
            String traitToHouseWeightsPath,
            String likertItemsPath,
            String forcedChoiceItemsPath
    )
    {
        Gson gson = new Gson();

        TraitHouseFile traitHouseFile =
                loadJson(gson, traitToHouseWeightsPath, TraitHouseFile.class);

        this.traitToHouseWeights = traitHouseFile.weights;
        this.traits = traitHouseFile.traits;
        this.houses = traitHouseFile.houses;

        LikertFile likertFile =
                loadJson(gson, likertItemsPath, LikertFile.class);

        this.likertItems = likertFile.items;

        SortingHatModel.Timing likertTiming = (likertFile.timing != null)
                ? likertFile.timing
                : defaultTiming();

        for (SortingHatModel.LikertItem item : this.likertItems)
        {
            if (item.timing == null) item.timing = likertTiming;
        }

        ForcedChoiceFile forcedFile =
                loadJson(gson, forcedChoiceItemsPath, ForcedChoiceFile.class);

        this.forcedChoiceItems = forcedFile.items;

        SortingHatModel.Timing forcedTiming = forcedFile.timing;
        if (forcedTiming == null) forcedTiming = likertTiming;

        for (SortingHatModel.ForcedChoiceItem item : this.forcedChoiceItems)
        {
            if (item.timing == null) item.timing = forcedTiming;
        }

        this.likertById = new HashMap<>();
        for (SortingHatModel.LikertItem item : likertItems)
        {
            String id = item.id.trim();
            if (id.isEmpty())
            {
                throw new IllegalStateException("Likert item has blank id");
            }
            if (likertById.put(id, item) != null)
            {
                throw new IllegalStateException("Duplicate Likert item id: " + id);
            }
        }

        this.quickFormIds = likertFile.forms.quick;
        this.standardFormIds = likertFile.forms.standard;
        this.forcedChoice = null;
    }

    /**
     * Attaches a scoring engine used to apply forced-choice tie-breaker results.
     *
     * @param scoring The scoring engine to receive forced-choice updates.
     */
    public void attachScoringEngine(ScoringEngine scoring)
    {
        this.forcedChoice = new ForcedChoice(forcedChoiceItems, scoring);
    }

    /**
     * Returns the trait-to-house weight matrix loaded from resources.
     *
     * @return A mapping from house name to trait weight mappings.
     */
    public Map<String, Map<String, Double>> getTraitToHouseWeights()
    {
        return traitToHouseWeights;
    }

    /**
     * Returns the list of trait names loaded from resources.
     *
     * @return The trait names in resource-defined order.
     */
    public ArrayList<String> getTraits()
    {
        return traits;
    }

    /**
     * Returns the list of house names loaded from resources.
     *
     * @return The house names in resource-defined order.
     */
    public ArrayList<String> getHouses()
    {
        return houses;
    }

    /**
     * Returns the number of Likert questions that will be asked for the selected mode.
     *
     * @param modeChoice Mode value from SortingHatModel (quick, standard, or thorough).
     *
     * @return The count of Likert items selected for the mode.
     */
    public int getLikertCountForMode(int modeChoice)
    {
        if (modeChoice == SortingHatModel.MODE_QUICK) return quickFormIds.size();
        if (modeChoice == SortingHatModel.MODE_STANDARD) return standardFormIds.size();
        return likertItems.size();
    }

    /**
     * Selects and shuffles Likert items appropriate for the selected quiz-length mode.
     *
     * @param modeChoice Mode value from SortingHatModel (quick, standard, or thorough).
     *
     * @return A shuffled list of Likert items for presentation.
     */
    public ArrayList<SortingHatModel.LikertItem> selectLikertItemsForMode(int modeChoice)
    {
        if (modeChoice == SortingHatModel.MODE_THOROUGH)
        {
            ArrayList<SortingHatModel.LikertItem> out = new ArrayList<>(likertItems);
            Collections.shuffle(out);
            return out;
        }

        ArrayList<String> ids = (modeChoice == SortingHatModel.MODE_QUICK)
                ? quickFormIds
                : standardFormIds;

        ArrayList<SortingHatModel.LikertItem> out = new ArrayList<>(ids.size());
        for (String id : ids)
        {
            out.add(likertById.get(id));
        }

        Collections.shuffle(out);
        return out;
    }

    /**
     * Selects the next forced-choice item associated with a specific pair of houses.
     *
     * @param houseA The first house name.
     * @param houseB The second house name.
     *
     * @return The next forced-choice question for the given house pair.
     *
     * @throws IllegalStateException If there are no forced-choice items for the house pair.
     */
    public SortingHatModel.ForcedChoiceItem selectNextForcedChoiceItemForPair(String houseA, String houseB)
    {
        return forcedChoice.selectNextForcedChoiceItemForPair(houseA, houseB);
    }

    /**
     * Applies the user's forced-choice selection to update house scores via the scoring engine.
     *
     * @param item The forced-choice item that was shown to the user.
     * @param optionKey The key for the selected option within the item.
     * @param responseTimeSec Time in seconds between item display and selection.
     *
     * @throws IllegalStateException If the option key does not match any option for the item.
     */
    public void applyForcedChoiceAnswer(
            SortingHatModel.ForcedChoiceItem item,
            String optionKey,
            double responseTimeSec
    )
    {
        forcedChoice.applyForcedChoiceAnswer(item, optionKey, responseTimeSec);
    }

    /**
     * Loads a JSON resource from the classpath and deserializes it into the requested type.
     *
     * @param gson The GSON instance used for parsing.
     * @param absoluteClasspathPath Absolute classpath path beginning with '/'.
     * @param type The class to deserialize into.
     * @param <T> The target type returned by this method.
     *
     * @return The parsed object instance.
     *
     * @throws IllegalArgumentException If the path is not an absolute classpath path.
     * @throws IllegalStateException If the classpath resource is missing.
     * @throws RuntimeException If parsing fails or an I/O error occurs.
     */
    private static <T> T loadJson(Gson gson, String absoluteClasspathPath, Class<T> type)
    {
        if (absoluteClasspathPath == null || !absoluteClasspathPath.startsWith("/"))
        {
            throw new IllegalArgumentException("Expected absolute classpath path like '/LikertItems.json': " + absoluteClasspathPath);
        }

        try (InputStream in = ItemBank.class.getResourceAsStream(absoluteClasspathPath))
        {
            if (in == null)
            {
                throw new IllegalStateException("Missing classpath resource: " + absoluteClasspathPath);
            }

            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8))
            {
                return gson.fromJson(r, type);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to load JSON resource: " + absoluteClasspathPath, e);
        }
    }

    /**
     * Builds a default timing configuration when a file does not provide timing values.
     *
     * @return A SortingHatModel.Timing instance with default values.
     */
    private static SortingHatModel.Timing defaultTiming()
    {
        SortingHatModel.Timing t = new SortingHatModel.Timing();
        t.expected_time_sec = 12;
        t.rapid_threshold_sec = 5;
        t.down_weight_factor = 0.5;
        return t;
    }

    /**
     * Provides per-house-pair forced-choice item rotation and applies choices via a scoring engine.
     */
    private static final class ForcedChoice
    {
        private final Map<String, ArrayList<SortingHatModel.ForcedChoiceItem>> forcedByPair;
        private final Map<String, Integer> forcedIndexByPair;
        private final ScoringEngine scoring;

        /**
         * Builds pair-indexed forced-choice item lists and shuffles each list for rotation.
         *
         * @param allItems The full set of forced-choice items available.
         * @param scoring The scoring engine to update when answers are applied.
         */
        ForcedChoice(ArrayList<SortingHatModel.ForcedChoiceItem> allItems, ScoringEngine scoring)
        {
            this.forcedByPair = new HashMap<>();
            this.forcedIndexByPair = new HashMap<>();
            this.scoring = scoring;

            for (SortingHatModel.ForcedChoiceItem item : allItems)
            {
                String h1 = item.house_pair.get(0);
                String h2 = item.house_pair.get(1);
                String key = canonicalPairKey(h1, h2);

                ArrayList<SortingHatModel.ForcedChoiceItem> itemsForHousePair = forcedByPair.get(key);
                if (itemsForHousePair == null)
                {
                    itemsForHousePair = new ArrayList<>();
                    forcedByPair.put(key, itemsForHousePair);
                }

                itemsForHousePair.add(item);
                forcedIndexByPair.putIfAbsent(key, 0);
            }

            for (ArrayList<SortingHatModel.ForcedChoiceItem> list : forcedByPair.values())
            {
                Collections.shuffle(list);
            }
        }

        /**
         * Returns the next forced-choice item for the given house pair, cycling and reshuffling as needed.
         *
         * @param houseA The first house name.
         * @param houseB The second house name.
         *
         * @return A forced-choice item for the pair.
         *
         * @throws IllegalStateException If no items exist for the given house pair.
         */
        SortingHatModel.ForcedChoiceItem selectNextForcedChoiceItemForPair(String houseA, String houseB)
        {
            String key = canonicalPairKey(houseA, houseB);

            ArrayList<SortingHatModel.ForcedChoiceItem> list = forcedByPair.get(key);
            if (list == null || list.isEmpty())
            {
                throw new IllegalStateException("No forced-choice items for house pair: " + key);
            }

            int index = forcedIndexByPair.getOrDefault(key, 0);
            if (index >= list.size())
            {
                Collections.shuffle(list);
                index = 0;
            }

            SortingHatModel.ForcedChoiceItem item = list.get(index);
            forcedIndexByPair.put(key, index + 1);
            return item;
        }

        /**
         * Resolves the selected option to a house outcome and applies it to the scoring engine.
         *
         * @param item The forced-choice item that was answered.
         * @param optionKey The key that identifies which option was chosen.
         * @param responseTimeSec Time in seconds between item display and selection.
         *
         * @throws IllegalStateException If the option key does not exist in the item options.
         */
        void applyForcedChoiceAnswer(
                SortingHatModel.ForcedChoiceItem item,
                String optionKey,
                double responseTimeSec
        )
        {
            String chosenHouse = null;
            for (SortingHatModel.ForcedChoiceOption opt : item.options)
            {
                if (opt.key.equalsIgnoreCase(optionKey))
                {
                    chosenHouse = opt.house;
                    break;
                }
            }

            if (chosenHouse == null)
            {
                throw new IllegalStateException("Unknown forced-choice option key: " + optionKey);
            }

            String h1 = item.house_pair.get(0);
            String h2 = item.house_pair.get(1);
            String otherHouse = h1.equals(chosenHouse) ? h2 : h1;

            double timeWeight = ResponseTimeWeight.compute(item.timing, responseTimeSec);
            scoring.applyForcedChoiceResult(chosenHouse, otherHouse, timeWeight);
        }

        /**
         * Produces a stable key for an unordered house pair by sorting lexicographically.
         *
         * @param a The first house name.
         * @param b The second house name.
         *
         * @return A canonical key in the form "A|B" independent of input order.
         */
        private String canonicalPairKey(String a, String b)
        {
            return (a.compareTo(b) <= 0) ? (a + "|" + b) : (b + "|" + a);
        }
    }
}
