// ResponseTimeWeight.java

package model;

/**
 * Computes a weight multiplier from a user's response time.
 * The returned value is used to scale scoring contributions so that very rapid
 * responses can be down-weighted and slower responses ramp up toward full weight.
 *
 * @author Mauricio Gidi
 * @version Last modified 13_Dec_2025
 */
public final class ResponseTimeWeight
{
    /**
     * Prevents instantiation; this class provides only static utilities.
     */
    private ResponseTimeWeight(){}

    /**
     * Computes a response-time weight in the range [0, 1] using the configured timing thresholds.
     * If the response time is not a valid non-negative number, this method returns 0.
     *
     * @param timing Timing configuration that defines thresholds and scaling.
     * @param responseTimeSeconds Measured response time in seconds.
     * @return A weight multiplier derived from response time.
     * @throws NullPointerException If timing is null.
     */
    public static double compute(SortingHatModel.Timing timing, double responseTimeSeconds)
    {
        // Read thresholds and scaling from configuration.
        int rapidThresholdSeconds = timing.rapid_threshold_sec;
        int expectedTimeSeconds = timing.expected_time_sec;
        double downWeightFactor = timing.down_weight_factor;

        // Invalid timings contribute no weight.
        if (Double.isNaN(responseTimeSeconds) || responseTimeSeconds < 0.0)
        {
            return 0;
        }

        double d = downWeightFactor;

        // Linear ramp from 0..d for responses up to the rapid threshold.
        if (responseTimeSeconds <= rapidThresholdSeconds)
        {
            return d * (responseTimeSeconds / (double) rapidThresholdSeconds);
        }

        // Full weight at or beyond the expected time.
        if (responseTimeSeconds >= expectedTimeSeconds)
        {
            return 1.0;
        }

        // Ramp from d..1.0 between rapid threshold and expected time.
        double progress = (responseTimeSeconds - rapidThresholdSeconds)
                / (double) (expectedTimeSeconds - rapidThresholdSeconds);
        return d + (1.0 - d) * progress;
    }
}
