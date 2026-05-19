package com.finance;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.*;

public class App extends Application {

    private Deck deck;

    private final List<Card> playerHand = new ArrayList<>();
    private final List<Card> ai1Hand = new ArrayList<>();
    private final List<Card> ai2Hand = new ArrayList<>();
    private final List<Card> community = new ArrayList<>();

    private int playerChips = 1000;
    private int ai1Chips = 1000;
    private int ai2Chips = 1000;

    private int pot = 0;
    private int gameState = 0;

    private Label potLabel;
    private Label playerInfo;
    private Label ai1Info;
    private Label ai2Info;
    private Label statusLabel;

    private HBox playerBox;
    private HBox ai1Box;
    private HBox ai2Box;
    private HBox communityBox;

    private Button startBtn;
    private Button foldBtn;
    private Button checkBtn;
    private Button betBtn;

    private TextField betField;

    @Override
    public void start(Stage stage) {

        potLabel = new Label("Pot: $0");
        playerInfo = new Label("You: $1000");
        ai1Info = new Label("AI 1: $1000");
        ai2Info = new Label("AI 2: $1000");
        statusLabel = new Label("Welcome to Poker");

        playerBox = new HBox(10);
        ai1Box = new HBox(10);
        ai2Box = new HBox(10);
        communityBox = new HBox(10);

        playerBox.setAlignment(Pos.CENTER);
        ai1Box.setAlignment(Pos.CENTER);
        ai2Box.setAlignment(Pos.CENTER);
        communityBox.setAlignment(Pos.CENTER);

        startBtn = new Button("Start Hand");
        foldBtn = new Button("Fold");
        checkBtn = new Button("Check");
        betBtn = new Button("Bet");

        betField = new TextField();
        betField.setPromptText("Bet Amount");
        betField.setMaxWidth(120);

        startBtn.getStyleClass().add("game-button");
        foldBtn.getStyleClass().add("game-button");
        checkBtn.getStyleClass().add("game-button");
        betBtn.getStyleClass().add("game-button");

        VBox root = new VBox(20,
                title("Texas Hold'em Poker"),
                ai1Info,
                ai1Box,
                ai2Info,
                ai2Box,
                title("Community Cards"),
                communityBox,
                playerInfo,
                playerBox,
                potLabel,
                statusLabel,
                controls()
        );

        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("table-background");

        startBtn.setOnAction(e -> startHand());
        checkBtn.setOnAction(e -> playerCheck());
        foldBtn.setOnAction(e -> playerFold());
        betBtn.setOnAction(e -> playerBet());

        setControlsDisabled(true);

        Scene scene = new Scene(root, 1000, 750);

        scene.getStylesheets().add(
                getClass().getResource("/style.css").toExternalForm()
        );

        stage.setScene(scene);
        stage.setTitle("Poker Game");
        stage.show();
    }

    private Label title(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("title-label");
        return label;
    }

    private HBox controls() {

        HBox box = new HBox(15,
                startBtn,
                checkBtn,
                betField,
                betBtn,
                foldBtn
        );

        box.setAlignment(Pos.CENTER);

        return box;
    }

    private void startHand() {

        deck = new Deck();

        playerHand.clear();
        ai1Hand.clear();
        ai2Hand.clear();
        community.clear();

        playerBox.getChildren().clear();
        ai1Box.getChildren().clear();
        ai2Box.getChildren().clear();
        communityBox.getChildren().clear();

        pot = 0;
        gameState = 1;

        deal(playerHand, playerBox, false);
        deal(playerHand, playerBox, false);

        deal(ai1Hand, ai1Box, true);
        deal(ai1Hand, ai1Box, true);

        deal(ai2Hand, ai2Box, true);
        deal(ai2Hand, ai2Box, true);

        statusLabel.setText("Place your bet.");

        updateLabels();

        setControlsDisabled(false);

        startBtn.setDisable(true);
    }

    private void playerCheck() {
        aiTurns(0);
    }

    private void playerFold() {

        ai1Chips += pot / 2;
        ai2Chips += pot / 2;

        endRound("You folded. AI players split the pot.");
    }

    private void playerBet() {

        String input = betField.getText().trim();

        if (input.isEmpty()) {
            statusLabel.setText("Enter a bet amount.");
            return;
        }

        int amount;

        try {
            amount = Integer.parseInt(input);
        } catch (Exception e) {
            statusLabel.setText("Invalid number.");
            return;
        }

        if (amount <= 0) {
            statusLabel.setText("Bet must be positive.");
            return;
        }

        if (amount > playerChips) {
            statusLabel.setText("Not enough chips.");
            return;
        }

        playerChips -= amount;
        pot += amount;

        aiTurns(amount);

        betField.clear();
    }

    private void aiTurns(int playerBet) {

        Random rand = new Random();

        int ai1Bet = Math.min(playerBet + rand.nextInt(50), ai1Chips);
        int ai2Bet = Math.min(playerBet + rand.nextInt(50), ai2Chips);

        ai1Chips -= ai1Bet;
        ai2Chips -= ai2Bet;

        pot += ai1Bet + ai2Bet;

        statusLabel.setText(
                "AI 1 bets $" + ai1Bet +
                        " | AI 2 bets $" + ai2Bet
        );

        progressGame();
    }

    private void progressGame() {

        if (gameState == 1) {

            for (int i = 0; i < 3; i++) {
                addCommunityCard();
            }

            gameState = 2;

        } else if (gameState == 2) {

            addCommunityCard();
            gameState = 3;

        } else if (gameState == 3) {

            addCommunityCard();
            gameState = 4;

        } else {

            showdown();
        }

        updateLabels();
    }

    private void showdown() {

        revealAI(ai1Hand, ai1Box);
        revealAI(ai2Hand, ai2Box);

        Random rand = new Random();

        int winner = rand.nextInt(3);

        if (winner == 0) {

            playerChips += pot;
            endRound("You win the pot! +$" + pot);

        } else if (winner == 1) {

            ai1Chips += pot;
            endRound("AI 1 wins the pot.");

        } else {

            ai2Chips += pot;
            endRound("AI 2 wins the pot.");
        }
    }

    private void endRound(String msg) {

        pot = 0;

        statusLabel.setText(msg);

        updateLabels();

        setControlsDisabled(true);

        startBtn.setDisable(false);
    }

    private void addCommunityCard() {

        Card c = deck.draw();

        community.add(c);

        communityBox.getChildren().add(cardLabel(c.toString()));
    }

    private void revealAI(List<Card> hand, HBox box) {

        box.getChildren().clear();

        for (Card c : hand) {
            box.getChildren().add(cardLabel(c.toString()));
        }
    }

    private void deal(List<Card> hand, HBox box, boolean hidden) {

        Card c = deck.draw();

        hand.add(c);

        if (hidden) {
            box.getChildren().add(cardLabel("🂠"));
        } else {
            box.getChildren().add(cardLabel(c.toString()));
        }
    }

    private Label cardLabel(String text) {

        Label card = new Label(text);

        card.getStyleClass().add("card");

        return card;
    }

    private void updateLabels() {

        playerInfo.setText("You: $" + playerChips);
        ai1Info.setText("AI 1: $" + ai1Chips);
        ai2Info.setText("AI 2: $" + ai2Chips);
        potLabel.setText("Pot: $" + pot);
    }

    private void setControlsDisabled(boolean disabled) {

        checkBtn.setDisable(disabled);
        foldBtn.setDisable(disabled);
        betBtn.setDisable(disabled);
        betField.setDisable(disabled);
    }

    public static void main(String[] args) {
        launch(args);
    }
}