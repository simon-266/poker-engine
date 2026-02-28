package de.simonaltschaeffl.poker.engine;

import de.simonaltschaeffl.poker.service.BettingRuleStrategy;
import de.simonaltschaeffl.poker.service.NoLimitBettingStrategy;
import de.simonaltschaeffl.poker.service.NoRakeStrategy;
import de.simonaltschaeffl.poker.service.RakeStrategy;

import jakarta.validation.constraints.Min;

/**
 * Configuration class for the {@link PokerGame}.
 * Instances are created using the {@link Builder} pattern.
 * Allows configuration of blinds, max players, turn timeouts, and game
 * strategies (like Rake and Betting Rules).
 */
public class PokerGameConfiguration {
    @Min(1)
    private final int smallBlind;
    @Min(2)
    private final int bigBlind;
    @Min(2)
    private final int maxPlayers;
    @Min(0)
    private final long actionTimeoutMs;
    private final RakeStrategy rakeStrategy;
    private final BettingRuleStrategy bettingRuleStrategy;

    private PokerGameConfiguration(Builder builder) {
        this.smallBlind = builder.smallBlind;
        this.bigBlind = builder.bigBlind;
        this.maxPlayers = builder.maxPlayers;
        this.actionTimeoutMs = builder.actionTimeoutMs;
        this.rakeStrategy = builder.rakeStrategy;
        this.bettingRuleStrategy = builder.bettingRuleStrategy;
    }

    public int getSmallBlind() {
        return smallBlind;
    }

    public int getBigBlind() {
        return bigBlind;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public long getActionTimeoutMs() {
        return actionTimeoutMs;
    }

    public RakeStrategy getRakeStrategy() {
        return rakeStrategy;
    }

    public BettingRuleStrategy getBettingRuleStrategy() {
        return bettingRuleStrategy;
    }

    /**
     * Builder for {@link PokerGameConfiguration}.
     */
    public static class Builder {
        private int smallBlind = 10;
        private int bigBlind = 20;
        private int maxPlayers = 10;
        private long actionTimeoutMs = 0; // 0 means disabled
        private RakeStrategy rakeStrategy = new NoRakeStrategy();
        private BettingRuleStrategy bettingRuleStrategy = new NoLimitBettingStrategy();

        public Builder smallBlind(int smallBlind) {
            this.smallBlind = smallBlind;
            return this;
        }

        public Builder bigBlind(int bigBlind) {
            this.bigBlind = bigBlind;
            return this;
        }

        public Builder maxPlayers(int maxPlayers) {
            this.maxPlayers = maxPlayers;
            return this;
        }

        public Builder actionTimeoutMs(long actionTimeoutMs) {
            this.actionTimeoutMs = actionTimeoutMs;
            return this;
        }

        public Builder rakeStrategy(RakeStrategy rakeStrategy) {
            this.rakeStrategy = rakeStrategy;
            return this;
        }

        public Builder bettingRuleStrategy(BettingRuleStrategy bettingRuleStrategy) {
            this.bettingRuleStrategy = bettingRuleStrategy;
            return this;
        }

        public PokerGameConfiguration build() {
            if (bigBlind <= smallBlind) {
                throw new IllegalArgumentException("Big blind must be greater than small blind");
            }
            if (maxPlayers < 2) {
                throw new IllegalArgumentException("Max players must be at least 2");
            }
            return new PokerGameConfiguration(this);
        }
    }
}
