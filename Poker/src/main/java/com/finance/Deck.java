package com.finance;

import java.util.*;

public class Deck {
    private final List<Card> cards = new ArrayList<>();
    public Deck() {
        for (int s = 0; s < 4; s++)
            for (int r = 0; r < 13; r++)
                cards.add(new Card(r, s));
        Collections.shuffle(cards);
    }
    public Card draw() { return cards.removeFirst(); }
}
