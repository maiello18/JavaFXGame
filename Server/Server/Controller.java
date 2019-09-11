package Server;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.scene.paint.Paint;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Controller {
    public SVGPath P1_Connect;
    public SVGPath P1_Turn;
    public Text P1_Cards;
    public Text P1_Name;
    public SVGPath P2_Connect;
    public SVGPath P2_Turn;
    public Text P2_Cards;
    public Text P2_Name;
    public SVGPath P3_Connect;
    public SVGPath P3_Turn;
    public Text P3_Cards;
    public Text P3_Name;
    public SVGPath P4_Connect;
    public SVGPath P4_Turn;
    public Text P4_Cards;
    public Text P4_Name;
    public Rectangle DeckRect;
    public Text DeckNum;
    public Text InfoText;
    public TextField PortInput;
    public ToggleButton ServerToggle;

    public boolean ServerStarted = false;
    private Server theServer = new Server(this);

    public void ServerToggleButton(ActionEvent actionEvent) {
        if (!ServerStarted) {
            startServer();
        } else {
            stopServer();
        }
    }

    void startServer() {
        if (theServer.launchServer(Integer.parseInt(PortInput.getText()))) {
            PortInput.setDisable(true);
            ServerToggle.setSelected(true);
            ServerToggle.setText("Stop");
            ServerStarted = true;
        } else {
            error("The server could not start, please try a different port");
            ServerToggle.setSelected(false);
        }
    }

    void completelyCleanServerGUI(){
        P1_Name.setText("Player 1");
        P2_Name.setText("Player 2");
        P3_Name.setText("Player 3");
        P4_Name.setText("Player 4");

        P1_Turn.setVisible(false);
        P2_Turn.setVisible(false);
        P3_Turn.setVisible(false);
        P4_Turn.setVisible(false);

        P1_Connect.setFill(Color.RED);
        P2_Connect.setFill(Color.RED);
        P3_Connect.setFill(Color.RED);
        P4_Connect.setFill(Color.RED);

        P1_Cards.setText("");
        P2_Cards.setText("");
        P3_Cards.setText("");
        P4_Cards.setText("");

        PortInput.setDisable(false);
        ServerToggle.setSelected(false);
        Platform.runLater(() -> ServerToggle.setText("Start"));
    }

    void stopServer() {
        theServer.turnOffServer();

        PortInput.setDisable(false);
        ServerToggle.setSelected(false);
        ServerToggle.setText("Start");

        ServerStarted = false;
    }

    void serverError(String message) {
        error(message);
        stopServer();
    }

    void error(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("UNO Error");
        alert.setHeaderText("UNO Server Error");
        alert.setContentText(message);
        alert.showAndWait().ifPresent(rs -> {
            if (rs == ButtonType.OK) {
                alert.close();
            }
        });
    }

    void playerConnected(int player, String name){
        if(player == 0){
            P1_Connect.setFill(Color.DODGERBLUE);
            P1_Name.setText(name);
        }
        else if(player == 1){
            P2_Connect.setFill(Color.DODGERBLUE);
            P2_Name.setText(name);
        }
        else if(player == 2){
            P3_Connect.setFill(Color.DODGERBLUE);
            P3_Name.setText(name);
        }
        else if(player == 3){
            P4_Connect.setFill(Color.DODGERBLUE);
            P4_Name.setText(name);
        }
    }

    void playerDisconnected(int player){
        if(player == 0){
            P1_Connect.setFill(Color.RED);
        }
        else if(player == 1){
            P2_Connect.setFill(Color.RED);
        }
        else if(player == 2){
            P3_Connect.setFill(Color.RED);
        }
        else if(player == 3){
            P3_Connect.setFill(Color.RED);
        }
    }

    void showPlayerTurn(int player){
        if(player == 0){
            P1_Turn.setVisible(true);
            P2_Turn.setVisible(false);
            P3_Turn.setVisible(false);
            P4_Turn.setVisible(false);
        }
        else if(player == 1){
            P2_Turn.setVisible(true);
            P1_Turn.setVisible(false);
            P3_Turn.setVisible(false);
            P4_Turn.setVisible(false);
        }
        else if(player == 2){
            P3_Turn.setVisible(true);
            P1_Turn.setVisible(false);
            P2_Turn.setVisible(false);
            P4_Turn.setVisible(false);
        }
        else if(player == 3){
            P4_Turn.setVisible(true);
            P1_Turn.setVisible(false);
            P2_Turn.setVisible(false);
            P3_Turn.setVisible(false);
        }
    }

    void playerCardsOnHand(int player, String Cards){
        if(player == 0){
            P1_Cards.setText(Cards);
        }
        else if(player == 1){
            P2_Cards.setText(Cards);
        }
        else if(player == 2){
            P3_Cards.setText(Cards);
        }
        else if(player == 3){
            P4_Cards.setText(Cards);
        }
    }
}
