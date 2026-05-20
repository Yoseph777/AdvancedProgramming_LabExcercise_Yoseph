package com.finance;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.*;

public class App extends Application {

    private static final int STARTING_CHIPS = 1000;
    private static final int SMALL_BLIND    = 10;
    private static final int BIG_BLIND      = 20;
    private static final int AI_DELAY_MS    = 600;
    private static final int NUM_PLAYERS    = 4;
    private final Player[] players = new Player[NUM_PLAYERS];
    private int dealerIdx = 0;
    private Deck deck;
    private final List<Card> community = new ArrayList<>();
    private int pot;
    private int currentBet;
    private int lastRaiserIdx;
    private boolean handInProgress;
    private Label potLabel, statusLabel;
    private TextArea logArea;
    private final VBox[]   panels   = new VBox[NUM_PLAYERS];
    private final Label[]  infos    = new Label[NUM_PLAYERS];
    private final Label[]  badges   = new Label[NUM_PLAYERS];
    private final HBox[]   cardRows = new HBox[NUM_PLAYERS];
    private HBox communityBox;
    private Button startBtn, foldBtn, checkCallBtn, raiseBtn;
    private TextField raiseField;

    @Override
    public void start(Stage stage) {
        for (int i = 0; i < NUM_PLAYERS; i++)
            players[i] = new Player(i == 0 ? "You" : "AI " + i, STARTING_CHIPS);
        buildUI();
        Scene scene = new Scene(buildRoot(), 1150, 820);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/style.css")).toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Texas Hold'em Poker");
        stage.show();
    }
    private void buildUI() {
        potLabel    = styledLabel("pot-label", "$0");
        statusLabel = styledLabel("status-label", "Welcome to Texas Hold'em!");
        statusLabel.setWrapText(true);
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setFocusTraversable(false);
        logArea.getStyleClass().add("log-area");
        logArea.setPrefRowCount(5);
        communityBox = new HBox(16);
        communityBox.setAlignment(Pos.CENTER);
        communityBox.setMinHeight(70);
        String[] names = {"You", "Kena", "Asrat", "Samuel"};
        for (int i = 0; i < NUM_PLAYERS; i++) {
            cardRows[i] = new HBox(8);
            cardRows[i].setAlignment(Pos.CENTER);
            infos[i]  = styledLabel("chip-label", "$" + STARTING_CHIPS);
            badges[i] = styledLabel("status-badge", "");
            panels[i] = makePanel(names[i], infos[i], badges[i], cardRows[i]);
        }
        startBtn     = styledBtn("Deal Hand");
        foldBtn      = styledBtn("Fold");
        checkCallBtn = styledBtn("Check");
        raiseBtn     = styledBtn("Raise");
        raiseField   = new TextField();
        raiseField.setPromptText("Raise to…");
        raiseField.setMaxWidth(110);
        raiseField.getStyleClass().add("bet-field");
        startBtn.setOnAction(_ -> startHand());
        foldBtn.setOnAction(_ -> playerAct(Action.FOLD));
        checkCallBtn.setOnAction(_ -> playerAct(playerToCall() > 0 ? Action.CALL : Action.CHECK));
        raiseBtn.setOnAction(_ -> playerAct(Action.RAISE));
        setControlsEnabled(false);
        startBtn.setDisable(false);
    }
    private VBox makePanel(String name, Label info, Label badge, HBox cards) {
        Label nl = styledLabel("player-name", name);
        cards.setAlignment(Pos.CENTER);
        VBox v = new VBox(6, nl, info, badge, cards);
        v.setAlignment(Pos.CENTER);
        v.getStyleClass().add("player-panel");
        v.setPadding(new Insets(14));
        v.setMinWidth(195);
        v.setMinHeight(165);
        return v;
    }
    private StackPane buildRoot() {
        Label ct = styledLabel("section-title", "COMMUNITY CARDS");
        VBox center = new VBox(10, potLabel, ct, communityBox, statusLabel);
        center.setAlignment(Pos.CENTER);
        center.getStyleClass().add("center-panel");
        center.setPadding(new Insets(18, 24, 18, 24));
        HBox controls = new HBox(10, startBtn, new Separator(), checkCallBtn, raiseField, raiseBtn, new Separator(), foldBtn);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(10, 20, 10, 20));
        controls.getStyleClass().add("controls-bar");
        VBox logPanel = new VBox(4, styledLabel("section-title", "Action Log"), logArea);
        logPanel.getStyleClass().add("log-panel");
        logPanel.setPadding(new Insets(8, 16, 8, 16));
        GridPane grid = new GridPane();
        grid.setHgap(18); grid.setVgap(18);
        grid.setPadding(new Insets(20, 20, 10, 20));
        grid.getColumnConstraints().addAll(pctCol(22), pctCol(56), pctCol(22));
        grid.getRowConstraints().addAll(pctRow(55), pctRow(45));
        grid.add(panels[2],   0, 0);
        grid.add(center,      1, 0);
        grid.add(panels[3],   2, 0);
        grid.add(panels[1],   0, 1);
        grid.add(controls,    1, 1);
        grid.add(panels[0],   2, 1);
        for (int i : new int[]{2, 3, 1, 0}) {
            GridPane.setValignment(panels[i], VPos.CENTER);
            GridPane.setHalignment(panels[i], HPos.CENTER);
            GridPane.setFillHeight(panels[i], true);
        }
        GridPane.setFillWidth(center, true);
        GridPane.setValignment(center, VPos.CENTER);
        GridPane.setValignment(controls, VPos.CENTER);
        GridPane.setHalignment(controls, HPos.CENTER);
        GridPane.setFillWidth(controls, true);
        grid.getStyleClass().add("table-background");
        VBox root = new VBox(0, grid, logPanel);
        VBox.setVgrow(grid, Priority.ALWAYS);
        root.getStyleClass().add("table-background");
        return new StackPane(root);
    }
    private void startHand() {
        List<Integer> alive = activeIndices();
        if (alive.size() < 2) { status("Game over!"); return; }
        do { dealerIdx = (dealerIdx + 1) % NUM_PLAYERS; }
        while (players[dealerIdx].isBusted());
        deck = new Deck();
        community.clear();
        clearAllUI();
        logArea.clear();
        for (Player p : players) p.newHand();
        pot = 0; currentBet = 0;
        handInProgress = true;
        int sbIdx = nextActive(dealerIdx);
        int bbIdx = nextActive(sbIdx);
        postBlind(sbIdx, SMALL_BLIND);
        postBlind(bbIdx, BIG_BLIND);
        currentBet = BIG_BLIND;
        log("--- New Hand (Dealer: " + players[dealerIdx].name + ") ---");
        log(players[sbIdx].name + " posts SB $" + players[sbIdx].streetBet);
        log(players[bbIdx].name + " posts BB $" + players[bbIdx].streetBet);
        for (Player p : players) {
            if (!p.isBusted()) {
                p.hand.add(deck.draw());
                p.hand.add(deck.draw());
            }
        }
        renderCards();
        refreshUI();
        int first = nextActive(bbIdx);
        lastRaiserIdx = bbIdx;
        beginBettingRound(first);
    }
    private void postBlind(int idx, int amount) {
        Player p = players[idx];
        int actual = Math.min(amount, p.chips);
        p.chips -= actual;
        p.streetBet = actual;
        pot += actual;
        if (p.chips == 0) p.allIn = true;
    }
    private void beginBettingRound(int startIdx) {
        for (Player p : players) p.actedThisRound = false;
        if (countCanAct() <= 1 && allBetsSettled()) {
            advanceStreet();
            return;
        }
        int idx = startIdx;
        for (int i = 0; i < NUM_PLAYERS; i++) {
            if (canAct(idx)) break;
            idx = nextActive(idx);
        }
        promptOrAI(idx);
    }
    private void promptOrAI(int idx) {
        if (!handInProgress) return;
        if (!canAct(idx)) {
            advanceToNext(idx);
            return;
        }
        if (idx == 0) {
            promptPlayer();
        } else {
            setBadge(idx, "thinking…", "status-thinking");
            Timeline t = new Timeline(new KeyFrame(Duration.millis(AI_DELAY_MS), _ -> {
                aiAct(idx);
                refreshUI();
                advanceToNext(idx);
            }));
            t.play();
        }
    }
    private void promptPlayer() {
        int toCall = playerToCall();
        checkCallBtn.setText(toCall > 0 ? "Call $" + toCall : "Check");
        setControlsEnabled(true);
        startBtn.setDisable(true);
        status("Your action. To call: $" + toCall);
    }
    private void playerAct(Action action) {
        Player p = players[0];
        if (p.folded || p.allIn || !handInProgress) return;
        setControlsEnabled(false);

        int toCall = currentBet - p.streetBet;
        switch (action) {
            case FOLD -> {
                p.folded = true;
                setBadge(0, "FOLD", "status-fold");
                log("You: FOLD");
            }
            case CHECK -> {
                if (toCall > 0) { status("Can't check — must call or fold."); setControlsEnabled(true); return; }
                setBadge(0, "CHECK", "status-check");
                log("You: CHECK");
            }
            case CALL -> {
                int amt = Math.min(toCall, p.chips);
                p.chips -= amt; p.streetBet += amt; pot += amt;
                if (p.chips == 0) p.allIn = true;
                setBadge(0, "CALL $" + amt, "status-call");
                log("You: CALL $" + amt);
            }
            case RAISE -> {
                String input = raiseField.getText().trim();
                if (input.isEmpty()) { status("Enter a raise amount."); setControlsEnabled(true); return; }
                int total;
                try { total = Integer.parseInt(input); }
                catch (NumberFormatException e) { status("Invalid number."); setControlsEnabled(true); return; }
                int minRaise = currentBet + BIG_BLIND;
                if (total < minRaise) { status("Minimum raise to $" + minRaise); setControlsEnabled(true); return; }
                if (total > p.chips + p.streetBet) { status("Not enough chips."); setControlsEnabled(true); return; }
                int delta = total - p.streetBet;
                p.chips -= delta; p.streetBet = total; pot += delta;
                currentBet = total;
                lastRaiserIdx = 0;
                if (p.chips == 0) p.allIn = true;
                raiseField.clear();
                setBadge(0, "RAISE $" + total, "status-bet");
                log("You: RAISE to $" + total);
            }
        }
        p.actedThisRound = true;
        refreshUI();
        advanceToNext(0);
    }

    private void aiAct(int idx) {
        Player p = players[idx];
        if (p.folded || p.allIn) return;

        int toCall = currentBet - p.streetBet;
        int strength = HandEvaluator.score(p.hand, community);
        Random rng = new Random();
        Action decision;
        int raiseTotal = 0;

        if (toCall <= 0) {
            if (strength >= 60 && rng.nextInt(100) < 60) {
                raiseTotal = Math.min(p.streetBet + strength / 3 + rng.nextInt(30) + BIG_BLIND, p.chips + p.streetBet);
                decision = Action.RAISE;
            } else {
                decision = Action.CHECK;
            }
        } else {
            if (strength >= 65) {
                if (rng.nextInt(100) < 35) {
                    raiseTotal = Math.min(currentBet + strength / 4 + rng.nextInt(40) + BIG_BLIND, p.chips + p.streetBet);
                    decision = Action.RAISE;
                } else {
                    decision = Action.CALL;
                }
            } else if (strength >= 35) {
                decision = rng.nextInt(100) < 18 ? Action.FOLD : Action.CALL;
            } else {
                decision = rng.nextInt(100) < 55 ? Action.FOLD : Action.CALL;
            }
        }

        switch (decision) {
            case FOLD -> {
                p.folded = true;
                setBadge(idx, "FOLD", "status-fold");
                log(p.name + ": FOLD");
            }
            case CHECK -> {
                setBadge(idx, "CHECK", "status-check");
                log(p.name + ": CHECK");
            }
            case CALL -> {
                int amt = Math.min(toCall, p.chips);
                p.chips -= amt; p.streetBet += amt; pot += amt;
                if (p.chips == 0) p.allIn = true;
                setBadge(idx, "CALL $" + amt, "status-call");
                log(p.name + ": CALL $" + amt);
            }
            case RAISE -> {
                int delta = raiseTotal - p.streetBet;
                delta = Math.min(delta, p.chips);
                if (delta <= 0) {
                    int amt = Math.min(toCall, p.chips);
                    p.chips -= amt; p.streetBet += amt; pot += amt;
                    if (p.chips == 0) p.allIn = true;
                    setBadge(idx, "CALL $" + amt, "status-call");
                    log(p.name + ": CALL $" + amt);
                } else {
                    p.chips -= delta; p.streetBet += delta; pot += delta;
                    if (p.streetBet > currentBet) { currentBet = p.streetBet; lastRaiserIdx = idx; }
                    if (p.chips == 0) p.allIn = true;
                    setBadge(idx, "RAISE $" + p.streetBet, "status-bet");
                    log(p.name + ": RAISE to $" + p.streetBet);
                }
            }
        }
        p.actedThisRound = true;
    }

    private void advanceToNext(int fromIdx) {
        if (!handInProgress) return;
        List<Integer> unfolded = unfoldedIndices();
        if (unfolded.size() <= 1) {
            awardPot(unfolded);
            return;
        }
        if (isRoundComplete()) {
            advanceStreet();
            return;
        }
        int next = nextActive(fromIdx);
        for (int i = 0; i < NUM_PLAYERS; i++) {
            if (canAct(next) && (!players[next].actedThisRound || players[next].streetBet < currentBet))
                break;
            next = nextActive(next);
        }
        if (players[next].actedThisRound && players[next].streetBet >= currentBet) {
            advanceStreet();
            return;
        }
        promptOrAI(next);
    }
    private void advanceStreet() {
        for (Player p : players) p.streetBet = 0;
        currentBet = 0;
        clearBadges();
        if (community.isEmpty()) {
            dealCommunity(3);
            log("--- Flop ---");
        } else if (community.size() == 3) {
            dealCommunity(1);
            log("--- Turn ---");
        } else if (community.size() == 4) {
            dealCommunity(1);
            log("--- River ---");
        } else {
            showdown();
            return;
        }
        refreshUI();
        if (countCanAct() <= 1) {
            advanceStreet();
            return;
        }
        int first = nextActive(dealerIdx);
        lastRaiserIdx = -1;
        beginBettingRound(first);
    }
    private void showdown() {
        log("--- Showdown ---");
        for (int i : unfoldedIndices()) revealHand(i);
        Player winner = null;
        HandEvaluator.HandResult bestHand = null;
        for (int i : unfoldedIndices()) {
            HandEvaluator.HandResult r = HandEvaluator.evaluate(players[i].hand, community);
            log(players[i].name + ": " + r.rank().label);
            if (bestHand == null || r.compareTo(bestHand) > 0) {
                bestHand = r;
                winner = players[i];
            }
        }
        if (winner != null) {
            winner.chips += pot;
            String msg = winner.name + " wins with " + bestHand.rank().label + "! +$" + pot;
            log(msg);
            endHand(msg);
        }
    }
    private void awardPot(List<Integer> winnerIndices) {
        if (winnerIndices.isEmpty()) { endHand("No winner?"); return; }
        int wIdx = winnerIndices.getFirst();
        Player w = players[wIdx];
        w.chips += pot;
        String msg;
        if (unfoldedIndices().size() <= 1) {
            msg = w.name + " wins $" + pot + " (all others folded)";
        } else {
            HandEvaluator.HandResult r = HandEvaluator.evaluate(w.hand, community);
            revealHand(wIdx);
            msg = w.name + " wins with " + r.rank().label + "! +$" + pot;
        }
        log(msg);
        endHand(msg);
    }
    private void endHand(String msg) {
        handInProgress = false;
        pot = 0;
        status(msg);
        refreshUI();
        setControlsEnabled(false);
        startBtn.setDisable(false);
    }
    private List<Integer> activeIndices() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < NUM_PLAYERS; i++)
            if (!players[i].isBusted()) list.add(i);
        return list;
    }
    private List<Integer> unfoldedIndices() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < NUM_PLAYERS; i++)
            if (!players[i].folded && !players[i].isBusted()) list.add(i);
        return list;
    }
    private int nextActive(int from) {
        int idx = (from + 1) % NUM_PLAYERS;
        int start = idx;
        while (players[idx].isBusted() || players[idx].folded) {
            idx = (idx + 1) % NUM_PLAYERS;
            if (idx == start) break; // safety
        }
        return idx;
    }
    private boolean canAct(int idx) {
        Player p = players[idx];
        return !p.folded && !p.allIn && !p.isBusted();
    }
    private int countCanAct() {
        int n = 0;
        for (int i = 0; i < NUM_PLAYERS; i++) if (canAct(i)) n++;
        return n;
    }
    private boolean allBetsSettled() {
        for (int i = 0; i < NUM_PLAYERS; i++) {
            Player p = players[i];
            if (!p.folded && !p.allIn && !p.isBusted() && p.streetBet < currentBet)
                return false;
        }
        return true;
    }
    private boolean isRoundComplete() {
        if (!allBetsSettled()) return false;
        for (int i = 0; i < NUM_PLAYERS; i++) {
            Player p = players[i];
            if (canAct(i) && !p.actedThisRound) return false;
        }
        return true;
    }
    private int playerToCall() { return currentBet - players[0].streetBet; }
    private void dealCommunity(int n) {
        for (int i = 0; i < n; i++) {
            Card c = deck.draw();
            community.add(c);
            communityBox.getChildren().add(cardLabel(c.toString()));
        }
    }
    private void renderCards() {
        for (int i = 0; i < NUM_PLAYERS; i++) {
            cardRows[i].getChildren().clear();
            Player p = players[i];
            if (p.isBusted()) continue;
            boolean hidden = (i != 0);
            for (Card c : p.hand)
                cardRows[i].getChildren().add(cardLabel(hidden ? "\uD83C\uDCA0" : c.toString()));
        }
    }
    private void revealHand(int idx) {
        cardRows[idx].getChildren().clear();
        for (Card c : players[idx].hand)
            cardRows[idx].getChildren().add(cardLabel(c.toString()));
    }
    private void refreshUI() {
        for (int i = 0; i < NUM_PLAYERS; i++)
            infos[i].setText(players[i].isBusted() ? "OUT" : "$" + players[i].chips);
        potLabel.setText("$" + pot);
    }
    private void clearAllUI() {
        for (HBox h : cardRows) h.getChildren().clear();
        communityBox.getChildren().clear();
        clearBadges();
    }
    private void clearBadges() {
        for (int i = 0; i < NUM_PLAYERS; i++) {
            badges[i].setText("");
            badges[i].getStyleClass().removeAll(
                    "status-check","status-call","status-bet",
                    "status-fold","status-allin","status-thinking");
        }
    }
    private void setBadge(int idx, String text, String cls) {
        badges[idx].setText(text);
        badges[idx].getStyleClass().removeAll(
                "status-check","status-call","status-bet",
                "status-fold","status-allin","status-thinking");
        if (!cls.isEmpty()) badges[idx].getStyleClass().add(cls);
    }
    private void setControlsEnabled(boolean on) {
        checkCallBtn.setDisable(!on);
        raiseBtn.setDisable(!on);
        foldBtn.setDisable(!on);
        raiseField.setDisable(!on);
    }
    private void status(String msg) { statusLabel.setText(msg); }
    private void log(String msg)    { logArea.appendText(msg + "\n"); }
    private Label cardLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("card");
        return l;
    }
    private Label styledLabel(String css, String text) {
        Label l = new Label(text);
        l.getStyleClass().add(css);
        return l;
    }
    private Button styledBtn(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("game-button");
        return b;
    }
    private ColumnConstraints pctCol(double p) {
        ColumnConstraints c = new ColumnConstraints();
        c.setPercentWidth(p); return c;
    }
    private RowConstraints pctRow(double p) {
        RowConstraints r = new RowConstraints();
        r.setPercentHeight(p); return r;
    }

    private enum Action { FOLD, CHECK, CALL, RAISE }
    private static class Player {
        final String name;
        int chips;
        final List<Card> hand = new ArrayList<>();
        boolean folded;
        boolean allIn;
        int streetBet;
        boolean actedThisRound;
        Player(String name, int chips) {
            this.name = name; this.chips = chips;
        }
        boolean isBusted() { return chips <= 0 && hand.isEmpty(); }
        void newHand() {
            hand.clear();
            folded = chips <= 0;
            allIn = false;
            streetBet = 0;
            actedThisRound = false;
        }
    }
    static void main(String[] args) { launch(args); }
}
