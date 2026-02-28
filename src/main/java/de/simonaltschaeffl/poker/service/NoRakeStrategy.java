package de.simonaltschaeffl.poker.service;

/**
 * Standard implementation that does not collect any rake.
 */
public class NoRakeStrategy implements RakeStrategy {
    @Override
    public int calculateRake(int potTotal) {
        return 0; // No rake taken
    }
}
