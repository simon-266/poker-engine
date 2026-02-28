package de.simonaltschaeffl.poker.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PercentageRakeStrategyTest {

    @Test
    public void testBasicPercentage() {
        RakeStrategy strategy = new PercentageRakeStrategy(0.05, 100);

        // 5% of 1000 = 50
        assertEquals(50, strategy.calculateRake(1000));

        // 5% of 100 = 5
        assertEquals(5, strategy.calculateRake(100));
    }

    @Test
    public void testRakeCap() {
        RakeStrategy strategy = new PercentageRakeStrategy(0.10, 30); // 10%, cap at 30

        // 10% of 200 = 20 (under cap)
        assertEquals(20, strategy.calculateRake(200));

        // 10% of 500 = 50 -> Capped at 30
        assertEquals(30, strategy.calculateRake(500));

        // 10% of 5000 = 500 -> Capped at 30
        assertEquals(30, strategy.calculateRake(5000));
    }

    @Test
    public void testZeroPercentageOrCap() {
        RakeStrategy noRake = new PercentageRakeStrategy(0.0, 100);
        assertEquals(0, noRake.calculateRake(1000));

        RakeStrategy zeroCap = new PercentageRakeStrategy(0.5, 0);
        assertEquals(0, zeroCap.calculateRake(1000));
    }

    @Test
    public void testInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> new PercentageRakeStrategy(-0.1, 100));
        assertThrows(IllegalArgumentException.class, () -> new PercentageRakeStrategy(1.5, 100));
        assertThrows(IllegalArgumentException.class, () -> new PercentageRakeStrategy(0.5, -10));
    }
}
