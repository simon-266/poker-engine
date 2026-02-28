package de.simonaltschaeffl.poker.service;

/**
 * Implementation that takes a percentage of the pot as rake, up to a maximum
 * cap.
 */
public class PercentageRakeStrategy implements RakeStrategy {
    private final double percentage;
    private final int cap;

    /**
     * @param percentage The percentage to rake (e.g. 0.05 for 5%)
     * @param cap        The maximum amount of chips to rake (e.g. 3 * bigBlind)
     */
    public PercentageRakeStrategy(double percentage, int cap) {
        if (percentage < 0 || percentage >= 1) {
            throw new IllegalArgumentException("Rake percentage must be between 0 and 1");
        }
        if (cap < 0) {
            throw new IllegalArgumentException("Rake cap must be non-negative");
        }
        this.percentage = percentage;
        this.cap = cap;
    }

    @Override
    public int calculateRake(int potTotal) {
        int calculated = (int) (potTotal * percentage);
        return Math.min(calculated, cap);
    }
}
