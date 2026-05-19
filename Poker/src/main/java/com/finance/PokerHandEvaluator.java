package com.finance;

import java.util.*;

public class PokerHandEvaluator {

    /*
     * HAND RANKINGS
     *
     * 10 = Royal Flush
     *  9 = Straight Flush
     *  8 = Four of a Kind
     *  7 = Full House
     *  6 = Flush
     *  5 = Straight
     *  4 = Three of a Kind
     *  3 = Two Pair
     *  2 = One Pair
     *  1 = High Card
     */

    public static int evaluate(List<Card> cards) {

        List<Integer> values = new ArrayList<>();

        Map<String, Integer> rankCount =
                new HashMap<>();

        Map<String, Integer> suitCount =
                new HashMap<>();

        // =========================
        // COUNT RANKS + SUITS
        // =========================

        for (Card c : cards) {

            String rank = c.getRank();

            String suit = c.getSuit();

            rankCount.put(
                    rank,
                    rankCount.getOrDefault(rank, 0) + 1
            );

            suitCount.put(
                    suit,
                    suitCount.getOrDefault(suit, 0) + 1
            );

            values.add(rankValue(rank));
        }

        Collections.sort(values);

        // =========================
        // CHECK FLUSH
        // =========================

        boolean flush = false;

        for (int count : suitCount.values()) {

            if (count >= 5) {

                flush = true;
                break;
            }
        }

        // =========================
        // CHECK STRAIGHT
        // =========================

        boolean straight =
                isStraight(values);

        // =========================
        // CHECK MATCHES
        // =========================

        int pairs = 0;

        boolean three = false;

        boolean four = false;

        for (int count : rankCount.values()) {

            if (count == 4) {

                four = true;
            }

            if (count == 3) {

                three = true;
            }

            if (count == 2) {

                pairs++;
            }
        }

        // =========================
        // ROYAL FLUSH
        // =========================

        if (flush && containsRoyal(values)) {

            return 10;
        }

        // =========================
        // STRAIGHT FLUSH
        // =========================

        if (flush && straight) {

            return 9;
        }

        // =========================
        // FOUR OF A KIND
        // =========================

        if (four) {

            return 8;
        }

        // =========================
        // FULL HOUSE
        // =========================

        if (three && pairs >= 1) {

            return 7;
        }

        // =========================
        // FLUSH
        // =========================

        if (flush) {

            return 6;
        }

        // =========================
        // STRAIGHT
        // =========================

        if (straight) {

            return 5;
        }

        // =========================
        // THREE OF A KIND
        // =========================

        if (three) {

            return 4;
        }

        // =========================
        // TWO PAIR
        // =========================

        if (pairs >= 2) {

            return 3;
        }

        // =========================
        // ONE PAIR
        // =========================

        if (pairs == 1) {

            return 2;
        }

        // =========================
        // HIGH CARD
        // =========================

        return 1;
    }

    // =====================================
    // CONVERT CARD RANK TO INTEGER
    // =====================================

    private static int rankValue(String rank) {

        switch (rank) {

            case "2":
                return 2;

            case "3":
                return 3;

            case "4":
                return 4;

            case "5":
                return 5;

            case "6":
                return 6;

            case "7":
                return 7;

            case "8":
                return 8;

            case "9":
                return 9;

            case "10":
                return 10;

            case "J":
                return 11;

            case "Q":
                return 12;

            case "K":
                return 13;

            case "A":
                return 14;
        }

        return 0;
    }

    // =====================================
    // CHECK STRAIGHT
    // =====================================

    private static boolean isStraight(
            List<Integer> values
    ) {

        Set<Integer> unique =
                new TreeSet<>(values);

        List<Integer> nums =
                new ArrayList<>(unique);

        int streak = 1;

        for (int i = 1; i < nums.size(); i++) {

            if (
                    nums.get(i)
                            ==
                            nums.get(i - 1) + 1
            ) {

                streak++;

                if (streak >= 5) {

                    return true;
                }

            } else {

                streak = 1;
            }
        }

        // Ace-low straight:
        // A 2 3 4 5

        return nums.contains(14)
                &&
                nums.contains(2)
                &&
                nums.contains(3)
                &&
                nums.contains(4)
                &&
                nums.contains(5);
    }

    // =====================================
    // CHECK ROYAL CARDS
    // =====================================

    private static boolean containsRoyal(
            List<Integer> values
    ) {

        return values.contains(10)
                &&
                values.contains(11)
                &&
                values.contains(12)
                &&
                values.contains(13)
                &&
                values.contains(14);
    }
}