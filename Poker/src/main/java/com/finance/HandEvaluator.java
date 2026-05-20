package com.finance;

import java.util.*;
import java.util.stream.*;

public class HandEvaluator {
    public enum HandRank {
        HIGH_CARD(0, "High Card"),     ONE_PAIR(1, "One Pair"),
        TWO_PAIR(2, "Two Pair"),       THREE_OF_A_KIND(3, "Three of a Kind"),
        STRAIGHT(4, "Straight"),       FLUSH(5, "Flush"),
        FULL_HOUSE(6, "Full House"),   FOUR_OF_A_KIND(7, "Four of a Kind"),
        STRAIGHT_FLUSH(8, "Straight Flush");
        public final int value;
        public final String label;
        HandRank(int v, String l) { this.value = v; this.label = l; }
    }
    public record HandResult(HandRank rank, List<Integer> tiebreakers) implements Comparable<HandResult> {
        @Override
        public int compareTo(HandResult o) {
            int c = Integer.compare(rank.value, o.rank.value);
            if (c != 0) return c;
            for (int i = 0; i < Math.min(tiebreakers.size(), o.tiebreakers.size()); i++) {
                c = Integer.compare(tiebreakers.get(i), o.tiebreakers.get(i));
                if (c != 0) return c;
            }
            return 0;
        }
        @Override
        public String toString() { return rank.label; }
    }
    public static HandResult evaluate(List<Card> hole, List<Card> board) {
        List<Card> all = new ArrayList<>(hole);
        all.addAll(board);
        if (all.size() < 5) return eval5(all);
        HandResult best = null;
        for (List<Card> five : combos(all, 5)) {
            HandResult r = eval5(five);
            if (best == null || r.compareTo(best) > 0) best = r;
        }
        return best;
    }
    public static int score(List<Card> hole, List<Card> board) {
        HandResult r = evaluate(hole, board);
        int[] bases = {0, 15, 30, 45, 55, 65, 75, 88, 100};
        int base = bases[r.rank.value];
        int nudge = r.tiebreakers.isEmpty() ? 0 : r.tiebreakers.getFirst() - 2;
        return Math.min(100, base + nudge / 2);
    }
    private static HandResult eval5(List<Card> cards) {
        List<Integer> vals = cards.stream().map(Card::rankValue)
                .sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        Map<Integer, Long> freq = cards.stream()
                .collect(Collectors.groupingBy(Card::rankValue, Collectors.counting()));
        boolean flush = cards.size() >= 5 && cards.stream().map(Card::suitName).distinct().count() == 1;
        boolean straight = isStraight(vals);
        List<Integer> quads = byFreq(freq, 4), trips = byFreq(freq, 3), pairs = byFreq(freq, 2);
        if (flush && straight)             return new HandResult(HandRank.STRAIGHT_FLUSH, vals);
        if (!quads.isEmpty())              return new HandResult(HandRank.FOUR_OF_A_KIND, join(quads, byFreq(freq,1)));
        if (!trips.isEmpty()&&!pairs.isEmpty()) return new HandResult(HandRank.FULL_HOUSE, join(trips, pairs));
        if (flush)                         return new HandResult(HandRank.FLUSH, vals);
        if (straight)                      return new HandResult(HandRank.STRAIGHT, vals);
        if (!trips.isEmpty())              return new HandResult(HandRank.THREE_OF_A_KIND, join(trips, byFreq(freq,1)));
        if (pairs.size() >= 2)             return new HandResult(HandRank.TWO_PAIR, join(pairs, byFreq(freq,1)));
        if (pairs.size() == 1)             return new HandResult(HandRank.ONE_PAIR, join(pairs, byFreq(freq,1)));
        return new HandResult(HandRank.HIGH_CARD, vals);
    }
    private static boolean isStraight(List<Integer> v) {
        if (v.size() < 5) return false;
        boolean normal = true;
        for (int i = 0; i < 4; i++) if (v.get(i) - v.get(i+1) != 1) { normal = false; break; }
        if (normal) return true;
        return v.equals(Arrays.asList(14,5,4,3,2));
    }
    private static List<Integer> byFreq(Map<Integer,Long> freq, int n) {
        return freq.entrySet().stream()
                .filter(e -> e.getValue() == n)
                .map(Map.Entry::getKey)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }
    private static List<Integer> join(List<Integer> a, List<Integer> b) {
        List<Integer> tb = new ArrayList<>(a);
        b.stream().sorted(Comparator.reverseOrder()).forEach(tb::add);
        return tb;
    }

    private static <T> List<List<T>> combos(List<T> list, int k) {
        List<List<T>> out = new ArrayList<>();
        comboRec(list, k, 0, new ArrayList<>(), out);
        return out;
    }
    private static <T> void comboRec(List<T> list, int k, int start,
                                     List<T> cur, List<List<T>> out) {
        if (cur.size() == k) { out.add(new ArrayList<>(cur)); return; }
        for (int i = start; i < list.size(); i++) {
            cur.add(list.get(i));
            comboRec(list, k, i+1, cur, out);
            cur.removeLast();
        }
    }
}
