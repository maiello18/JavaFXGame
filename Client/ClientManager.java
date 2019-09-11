package Client;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.application.Platform;

import java.util.ArrayList;


// Orchestrates each GUI Window and the ServerConnection
class ClientManager {
    private Stage primaryStage;
    private ServerConnection connection;
    private GameScene GameSceneController;
    private ErrorScreen ErrorScreenController;
    private WaitScene WaitSceneController;

    // Defaults
    public boolean wantsToQuit = false;
    public String data_host = "127.0.0.1";
    public String data_port = "5555";
    public String data_user = "Mark Hallenbeck";
    public String data_errorMsg = "There has been an error";
    public String data_playersConnected = "0";

    ServerConnection getConnection(){
        return connection;
    }


    // Constructor (only runs once for the life of the application)
    ClientManager(Stage app_main) throws Exception {
        // Set Primary Stage
        this.primaryStage = app_main;

        // Begin with the Welcome Screen
        resetMenu();
    }

    // Tries to connect to the server, showing the error box or next scene depending if it connects
    public void ConnectToServer() {
        // Create new connection
        connection = new ServerConnection(this, data -> {
            Platform.runLater(() -> {
                ArrayList<String> words = connection.csvToArrayList(data.toString());
                if (words.get(0).equals("SwitchToGame")) {
                    this.goToGameScene();
                }
                else if (words.get(0).equals("UpdateNumPlayers")) {
                    this.data_playersConnected = words.get(1);
                    WaitSceneController.updatePlayers(words.get(1));
                }
                else if (words.get(0).equals("PlayersTurn")) {
                    GameSceneController.enablePlay();
                }
                else if (words.get(0).equals("NotPlayersTurn")) {
                    GameSceneController.disablePlay();
                }
                else if (words.get(0).equals("NewHand")) {
                    words.remove(0);
                    GameSceneController.setHand(words);
                }
                else if (words.get(0).equals("AddCard")) {
                    GameSceneController.addCard(words.get(1));
                }
                else if (words.get(0).equals("UNO")) {
                    System.out.println(words.get(1));
                    GameSceneController.declareWinner(words.get(1));
                }
                else if (words.get(0).equals("PlayerDisconnect")) {
                    connection.killConnection();
                    Error("A player disconnected.");
                }
                else if (words.get(0).equals("ServerClosed")) { Error("Server Closed."); }
                else if (words.get(0).equals("Play")) {
                    GameSceneController.setTopOfDeck(words.get(1), words.get(2));
                }
            });
        });

         if (connection.connectToServer(data_host, Integer.parseInt(data_port), data_user)) {
            goToWaitScene();
        } else {
            // Cannot connect, show an error
            Error("Cannot Connect to Server");
        }
    }

    // Initializes a new Menu
    public void resetMenu() {
        //System.out.println("Resetting menu");
        try {
            WelcomeScreenSwitch();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void goToGameScene() {
        try {
            GameSceneSwitch();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void goToWaitScene() {
        try {
            if (data_playersConnected != "4")
                WaitSceneSwitch();
            else
                GameSceneSwitch();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Shows an Error message in an a GUI Error Box, will kill connection automatically if need be
    public void Error(String message) {
        data_errorMsg = message;
        try {
            ErrorScreenSwitch();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Sends move that client made to the server
    public void gameMakeMove(String card, String player) {
        connection.gamePlayCard(card, player);
    }

    public void makeDraw() {
        connection.makeDraw();
	}

    public void declareUno(int cardsInHand) {
        connection.declareUno(data_user, cardsInHand);
	}

    // Quit out of the entire program
    public void Quit() {
        primaryStage.close();
    }


    /*
     * Below loads the scenes and helps pass the ClientManager object
     */
    private void WelcomeScreenSwitch() throws Exception {
        FXMLLoader WelcomeScreen = new FXMLLoader(getClass().getResource("WelcomeScene.fxml"));
        Parent root = (Parent) WelcomeScreen.load();
        WelcomeScreen controller = WelcomeScreen.getController();
        controller.init(this);
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.setTitle("Welcome to UNO");
        primaryStage.show();
    }


    private void ErrorScreenSwitch() throws Exception {
        FXMLLoader ErrorScreen = new FXMLLoader(getClass().getResource("ErrorScreen.fxml"));
        Parent root = (Parent) ErrorScreen.load();
        ErrorScreen controller = ErrorScreen.getController();
        controller.init(this);
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.setTitle("Something went wrong :|");
        primaryStage.show();
    }


    private void GameSceneSwitch() throws Exception {
        FXMLLoader GameScene = new FXMLLoader(getClass().getResource("GameScene.fxml"));
        Parent root = (Parent) GameScene.load();
        GameSceneController = GameScene.getController();
        GameSceneController.init(this, data_user);
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.setTitle("UNO Match");
        primaryStage.show();
    }


    private void WaitSceneSwitch() throws Exception {
        FXMLLoader WaitScene = new FXMLLoader(getClass().getResource("WaitingScene.fxml"));
        Parent root = (Parent) WaitScene.load();
        WaitSceneController = WaitScene.getController();
        WaitSceneController.init(this);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.setTitle("Wait!");
        primaryStage.show();
    }
}
