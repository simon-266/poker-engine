package de.simonaltschaeffl.poker.service;

/**
 * Defines the strategy for calculating the house rake (commission) from a pot.
 */
public interface RakeStrategy {
    /**
     * Calculates the amount of chips to be taken as rake from the given pot total.
     * 
     * @param potTotal The total amount of chips in the pot.
     * @return The amount of chips to rake. Must be between 0 and potTotal.
     */
    int calculateRake(int potTotal);
}
