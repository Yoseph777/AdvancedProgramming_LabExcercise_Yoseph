package com.finance;

public class Card {
    private static final String[] RANKS = {"2","3","4","5","6","7","8","9","10","J","Q","K","A"};
    private static final String[] SUIT_SYMBOLS = {"\u2665","\u2666","\u2663","\u2660"};
    private static final String[] SUIT_NAMES = {"hearts","diamonds","clubs","spades"};
    private final int rankIdx;
    private final int suitIdx;
    public Card(int rankIdx, int suitIdx) {
        this.rankIdx = rankIdx;
        this.suitIdx = suitIdx;
    }
    public int rankValue() { return rankIdx + 2; }
    public String suitName() { return SUIT_NAMES[suitIdx]; }
    public boolean sameSuit(Card o) { return this.suitIdx == o.suitIdx; }
    @Override
    public String toString() { return RANKS[rankIdx] + SUIT_SYMBOLS[suitIdx]; }
}
