package com.finance;
import java.util.*;

public class Deck {
    private List<Card> cards = new ArrayList<>();
    public Deck() {
        String[] suits = {"♥", "♦", "♣", "♠"};
        String[] ranks = {"2","3","4","5","6","7","8","9","10","J","Q","K","A"};
        for (String s : suits) for (String r : ranks) cards.add(new Card(r, s));
        Collections.shuffle(cards);
    }
    public Card draw() { return cards.remove(0); }
}