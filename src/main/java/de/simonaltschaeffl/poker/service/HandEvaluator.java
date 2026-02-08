package de.simonaltschaeffl.poker.service;

import de.simonaltschaeffl.poker.model.Card;
import de.simonaltschaeffl.poker.model.HandResult;
import java.util.List;

public interface HandEvaluator {
    HandResult evaluate(List<Card> holeCards, List<Card> communityCards);
}
