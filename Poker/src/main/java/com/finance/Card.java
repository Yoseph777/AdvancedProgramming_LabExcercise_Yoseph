package com.finance;

public class Card {

    private final String rank;

    private final String suit;

    public Card(String rank, String suit) {

        this.rank = rank;

        this.suit = suit;
    }

    // =========================
    // GETTERS
    // =========================

    public String getRank() {

        return rank;
    }

    public String getSuit() {

        return suit;
    }

    // =========================
    // DISPLAY
    // =========================

    @Override
    public String toString() {

        return rank + suitSymbol();
    }

    // =========================
    // SUIT SYMBOLS
    // =========================

    private String suitSymbol() {

        switch (suit.toLowerCase()) {

            case "hearts":
                return "♥";

            case "diamonds":
                return "♦";

            case "clubs":
                return "♣";

            case "spades":
                return "♠";
        }

        return suit;
    }
}