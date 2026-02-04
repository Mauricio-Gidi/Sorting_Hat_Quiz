// ScoringEngine.java

package model;

import config.Config;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Computes and maintains trait scores and house probabilities during a quiz run.
 * Uses Likert responses to accumulate trait scores (with response-time weighting
 * and an IRT-based latent-level estimate), then maps traits to houses and
 * normalizes house scores into a probability distribution.
 *
 * Also supports forced-choice updates that directly adjust house scores using a
 * logistic predicted win probability and a configurable learning rate.
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
public final class ScoringEngine
{
    // Bounds used when mapping Likert category responses to a latent trait level (theta).
    private static final double LOWEST_THETA = Config.THETA_MIN;
    private static final double HIGHEST_THETA = Config.THETA_MAX;

    private final ArrayList<String> traitNames;
    private final ArrayList<String> houseNames;

    // Per-house weights used to convert trait scores into house scores.
    private final Map<String, Map<String, Double>> traitWeightByHouse;

    // Mutable scoring state for the current quiz attempt.
    private final Map<String, Double> traitScoreByTrait = new HashMap<>();
    private final Map<String, Double> houseScoreByHouse = new HashMap<>();
    private final Map<String, Double> houseProbabilityByHouse = new LinkedHashMap<>();

    /**
     * Creates a new scoring engine initialized with zero scores for all traits and houses.
     * House probabilities are computed immediately based on the initial (all-zero) scores.
     *
     * @param traits
     *     Ordered list of trait names that define the trait score vector.
     * @param houses
     *     Ordered list of house names that define the house score/probability vectors.
     * @param traitToHouseWeights
     *     Mapping from house name to a mapping of trait name to weight used when
     *     aggregating trait scores into a house score.
     *
     * @throws NullPointerException
     *     If traits, houses, or traitToHouseWeights is null.
     */
    public ScoringEngine(
            ArrayList<String> traits,
            ArrayList<String> houses,
            Map<String, Map<String, Double>> traitToHouseWeights
    )
    {
        this.traitNames = Objects.requireNonNull(traits, "traits");
        this.houseNames = Objects.requireNonNull(houses, "houses");
        this.traitWeightByHouse = Objects.requireNonNull(traitToHouseWeights, "traitToHouseWeights");

        for (String trait : traitNames)
        {
            traitScoreByTrait.put(trait, 0.0);
        }

        for (String house : houseNames)
        {
            houseScoreByHouse.put(house, 0.0);
            houseProbabilityByHouse.put(house, 0.0);
        }

        recomputeHouseScores();
    }

    /**
     * Records a single Likert response by updating internal trait scores.
     * This method does not directly recompute house scores; callers can trigger
     * a recomputation when appropriate.
     *
     * @param item
     *     The Likert item definition (including IRT parameters, timing, and trait weights).
     * @param likertResponseValue
     *     The response value on the 1..5 scale, supporting fractional increments.
     * @param responseTimeSeconds
     *     Time taken to answer in seconds, used to compute a response-time multiplier.
     */
    public void recordLikertResponse(
            SortingHatModel.LikertItem item,
            double likertResponseValue,
            double responseTimeSeconds
    )
    {
        addTraitPointsFromLikertResponse(item, likertResponseValue, responseTimeSeconds);
    }

    /**
     * Recomputes house scores from current trait scores and updates house probabilities.
     */
    public void recomputeHouseScores()
    {
        recomputeHouseScoresFromTraitScores();
        recomputeHouseProbabilitiesFromScores();
    }

    /**
     * Applies the result of a forced-choice comparison between two houses.
     * The method updates the two involved house scores in opposite directions
     * and then refreshes the house probability distribution.
     *
     * @param chosenHouse
     *     The house selected by the user for the forced-choice prompt.
     * @param otherHouse
     *     The non-selected house from the same forced-choice prompt.
     * @param timeWeight
     *     Response-time weight (typically in [0, 1]) that scales the score adjustment.
     */
    public void applyForcedChoiceResult(
            String chosenHouse,
            String otherHouse,
            double timeWeight
    )
    {
        // Compute the predicted chance the chosen house wins using a logistic model
        // over the current score difference, then apply an error-scaled update.
        double chosenScore = houseScoreByHouse.get(chosenHouse);
        double otherScore = houseScoreByHouse.get(otherHouse);
        double scoreDifference = chosenScore - otherScore;
        double predictedChosenWins = 1.0 / (1.0 + Math.exp(-scoreDifference));
        double error = 1.0 - predictedChosenWins;

        double scoreChange = Config.FC_LEARNING_RATE * timeWeight * error;
        houseScoreByHouse.put(chosenHouse, chosenScore + scoreChange);
        houseScoreByHouse.put(otherHouse, otherScore - scoreChange);

        recomputeHouseProbabilitiesFromScores();
    }

    /**
     * Returns a copy of the current house probability distribution.
     *
     * @return
     *     A new map from house name to probability, preserving the original insertion order.
     */
    public Map<String, Double> getHouseProbabilities()
    {
        return new LinkedHashMap<>(houseProbabilityByHouse);
    }

    /**
     * Returns the house with the highest current probability.
     *
     * @return
     *     The name of the house with the greatest probability value.
     */
    public String getTopHouse()
    {
        String bestHouse = houseNames.get(0);
        double bestProbability = houseProbabilityByHouse.get(bestHouse);

        for (int i = 1; i < houseNames.size(); i++)
        {
            String house = houseNames.get(i);
            double p = houseProbabilityByHouse.get(house);
            if (p > bestProbability)
            {
                bestProbability = p;
                bestHouse = house;
            }
        }

        return bestHouse;
    }

    /**
     * Computes the set of houses whose probability is within a threshold of the maximum.
     *
     * @param threshold
     *     Maximum allowed difference from the highest probability for a house to be included.
     *
     * @return
     *     A list of house names that fall within the tie threshold across all houses.
     */
    public ArrayList<String> getTieGroup(double threshold)
    {
        return getTieGroup(threshold, new ArrayList<>(houseNames));
    }

    /**
     * Computes the set of houses within a provided subset whose probability is within a
     * threshold of the maximum probability found in that subset.
     *
     * @param threshold
     *     Maximum allowed difference from the subset maximum for a house to be included.
     * @param subset
     *     The houses to consider when computing the subset maximum and tie membership.
     *
     * @return
     *     A list of house names from subset that fall within the tie threshold.
     */
    public ArrayList<String> getTieGroup(double threshold, ArrayList<String> subset)
    {
        double maxProbability = -1.0;
        for (String house : subset)
        {
            double p = houseProbabilityByHouse.get(house);
            if (p > maxProbability) maxProbability = p;
        }

        ArrayList<String> ties = new ArrayList<>();
        for (String house : subset)
        {
            double p = houseProbabilityByHouse.get(house);
            if ((maxProbability - p) <= threshold) ties.add(house);
        }

        return ties;
    }

    /**
     * Returns a snapshot of current trait scores in the configured trait order.
     *
     * @return
     *     A new map from trait name to its current score value.
     */
    public Map<String, Double> getTraitScoresSnapshot()
    {
        Map<String, Double> snapshot = new LinkedHashMap<>();
        for (String trait : traitNames) snapshot.put(trait, traitScoreByTrait.get(trait));
        return snapshot;
    }

    /**
     * Updates trait scores based on a Likert response, incorporating response time and IRT.
     *
     * @param item
     *     The Likert item used to determine trait weights, IRT parameters, and timing.
     * @param likertResponseValue
     *     The response value on the 1..5 scale, supporting fractional increments.
     * @param responseTimeSeconds
     *     Time taken to answer in seconds, used to compute a response-time multiplier.
     */
    private void addTraitPointsFromLikertResponse(
            SortingHatModel.LikertItem item,
            double likertResponseValue,
            double responseTimeSeconds
    )
    {
        double timeMultiplier = ResponseTimeWeight.compute(item.timing, responseTimeSeconds);
        double latentLevel = estimateLatentLevelFromFractionalLikertResponse(item, likertResponseValue);

        // Base contribution is proportional to IRT discrimination (a) and the inferred latent level.
        double basePoints = latentLevel * item.irt.a;
        addWeightedTraitPoints(item, basePoints, timeMultiplier);
    }

    /**
     * Estimates a latent trait level (theta) by mapping a 1..5 response to midpoint values
     * between IRT category thresholds, interpolating when the response is fractional.
     *
     * @param item
     *     The Likert item whose IRT thresholds define category boundaries.
     * @param response1to5
     *     Response value on the 1..5 scale (may include fractional increments).
     *
     * @return
     *     An estimated latent trait level corresponding to the response position.
     */
    private static double estimateLatentLevelFromFractionalLikertResponse(
            SortingHatModel.LikertItem item,
            double response1to5
    )
    {
        SortingHatModel.IrtThresholds t = item.irt.thresholds;
        double b1 = t.b1, b2 = t.b2, b3 = t.b3, b4 = t.b4;

        // Midpoints for the five Likert categories, bounded by global theta limits.
        double m1 = (LOWEST_THETA + b1) * 0.5;
        double m2 = (b1 + b2) * 0.5;
        double m3 = (b2 + b3) * 0.5;
        double m4 = (b3 + b4) * 0.5;
        double m5 = (b4 + HIGHEST_THETA) * 0.5;

        int lo = (int) response1to5;
        double frac = response1to5 - lo;

        double loMid;
        if (lo == 1) loMid = m1;
        else if (lo == 2) loMid = m2;
        else if (lo == 3) loMid = m3;
        else if (lo == 4) loMid = m4;
        else loMid = m5;

        if (frac == 0.0) return loMid;

        double hiMid;
        if (lo == 1) hiMid = m2;
        else if (lo == 2) hiMid = m3;
        else if (lo == 3) hiMid = m4;
        else hiMid = m5;

        return loMid + frac * (hiMid - loMid);
    }

    /**
     * Adds a scaled contribution to each trait score based on the item's trait weights.
     *
     * @param item
     *     The Likert item providing the trait weight map.
     * @param basePoints
     *     Base points derived from the latent-level estimate and IRT discrimination.
     * @param multiplier
     *     Additional multiplier (for example, derived from response time).
     */
    private void addWeightedTraitPoints(
            SortingHatModel.LikertItem item,
            double basePoints,
            double multiplier
    )
    {
        for (Map.Entry<String, Double> entry : item.trait_weights.entrySet())
        {
            String traitName = entry.getKey();
            double traitWeight = entry.getValue();

            double current = traitScoreByTrait.get(traitName);
            traitScoreByTrait.put(traitName, current + (basePoints * multiplier * traitWeight));
        }
    }

    /**
     * Recomputes every house score by aggregating current trait scores using per-house weights.
     */
    private void recomputeHouseScoresFromTraitScores()
    {
        for (String house : houseNames)
        {
            houseScoreByHouse.put(house, weightedSumForHouse(house));
        }
    }

    /**
     * Computes the weighted sum of trait scores for a single house.
     *
     * @param house
     *     The house whose trait weights should be applied.
     *
     * @return
     *     The resulting weighted sum of trait scores for the house.
     */
    private double weightedSumForHouse(String house)
    {
        double sum = 0.0;
        Map<String, Double> weightsForHouse = traitWeightByHouse.get(house);

        for (Map.Entry<String, Double> entry : weightsForHouse.entrySet())
        {
            String trait = entry.getKey();
            double weight = entry.getValue();
            sum += traitScoreByTrait.get(trait) * weight;
        }

        return sum;
    }

    /**
     * Recomputes house probabilities using a softmax over current house scores.
     * Uses a max-subtraction trick for improved numerical stability.
     */
    private void recomputeHouseProbabilitiesFromScores()
    {
        double maxScore = Double.NEGATIVE_INFINITY;
        for (String house : houseNames)
        {
            double score = houseScoreByHouse.get(house);
            if (score > maxScore) maxScore = score;
        }

        double total = 0.0;
        for (String house : houseNames)
        {
            double expValue = Math.exp(houseScoreByHouse.get(house) - maxScore);
            houseProbabilityByHouse.put(house, expValue);
            total += expValue;
        }

        for (String house : houseNames)
        {
            houseProbabilityByHouse.put(house, houseProbabilityByHouse.get(house) / total);
        }
    }
}
