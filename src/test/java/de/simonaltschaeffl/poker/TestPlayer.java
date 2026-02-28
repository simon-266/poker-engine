package de.simonaltschaeffl.poker;

import de.simonaltschaeffl.poker.model.Player;

public class TestPlayer extends Player {
    public TestPlayer(String id, String name, int chips) {
        super(id, name, chips);
    }

    @Override
    public void onLeave() {
    }

    @Override
    public void onHandEnded(java.util.Map<String, Integer> payouts) {
    }
}
