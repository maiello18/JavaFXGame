package Client;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

public class ServerConnection {
    private int port;
    private String ip;
    private String playerName;

    public Socket clientSocket;
    public ObjectOutputStream outStream;
    public ObjectInputStream inStream;
    serverCommunicationThread serverThread;

    ClientManager manager;

    private Consumer<Serializable> callback;

    public ServerConnection(ClientManager clientManager, Consumer<Serializable> callback) {
        manager = clientManager;
        manager.wantsToQuit = false;
        this.callback = callback;
    }

    // ######################################################################
    // Parses a CSV (Comma separated values) string to an array list
    // ######################################################################
    public static ArrayList<String> csvToArrayList(String csvString){
        return new ArrayList<String>(Arrays.asList(csvString.split(";")));
    }
    // ######################################################################

    /* Connects to UNO game server.
       We should expect this response upon connecting: "NumberOfPlayers;NUM;"
     */
    public boolean connectToServer(String ip, int port, String playerName) {
        this.port = port;
        this.ip = ip;
        this.playerName = playerName;
        serverThread = new serverCommunicationThread();

        try
        {
            clientSocket = new Socket(ip, port);;
            outStream = new ObjectOutputStream(clientSocket.getOutputStream());;
            inStream = new ObjectInputStream(clientSocket.getInputStream());

            clientSocket.setTcpNoDelay(true);

            serverThread.start();

            sendMessage("Name;" + playerName + ";");

            return true;
        }
        catch (Exception e) {
            System.out.println("Failed to connect");
            //e.printStackTrace();
            return false;
        }
    }

    public void killConnection() {
        try {
            sendMessage("KILL YOURSELF;");
            outStream.close();
            inStream.close();
            clientSocket.close();
        }
        catch (Exception e) {
            //socket closed but we return to menu to re connect so it is okay
            //e.printStackTrace();
        }
    }

    /* Sends a play to the server,
       ex: "PLAY;Bobby;B4" - PLAY MADE, player name: Bobby, card played: blue 4 */
    public void gamePlayCard(String cardName, String playerName) {
        System.out.println("PLAY;" + playerName + ";" + cardName + ";");
        sendMessage("PLAY;" + playerName + ";" + cardName + ";");
    }

    /* Sends a delcare UNO for the client to the server,
       ex: "UNO;Bobby;0;" - UNO WINNER, Player name: Bobby, Bobby has 0 cards - because he has 0 he is infact the winner.
           "UNO;Bobby;5;" - UNO WINNER, Player name: Bobby, Bobby has 5 cards - because he has 2 he is NOT the winner, 
                              so give him 1 extra card for trying to cheat. */
    public void declareUno(String playerName, int cardsInHand) {
        sendMessage("UNO;" + playerName + ";" + cardsInHand + ";");
    }

    public void makeDraw() {
        sendMessage("DRAW;");
	}

    /* Helper function to send message to server */
    private void sendMessage(String msg){
        try {
            outStream.writeObject(msg);
        }
        catch (Exception e)   {
            System.out.println("Failed to send message.");
        }
    }

    class serverCommunicationThread extends Thread {
        @Override
        public void run()
        {
            try {
                this.sleep(500);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            // Continually listen to what the server has to tell us
            try {
                // Continually listen to the server for commands
                while (true) {
                    Serializable data = (Serializable) inStream.readObject();
                    System.out.println("Data sent from server: " + data.toString());
                    interpretData(data.toString());
                }
            }
            // If the connection has been broken we need to go back to the
            // screen that allows us to connect to a new server.
            catch (Exception e) {
                // System.out.println("Failed in server communication thread");
                // e.printStackTrace();
                killConnection();
                serverThread = null;
                callback.accept("ServerClosed;");
            }
        }
    }

    private void interpretData(String data) {
        // Convert the string to an array list
        ArrayList<String> coms = csvToArrayList(data);

        if (coms.get(0).equals("GameReady")) { callback.accept("SwitchToGame;"); }
        else if (coms.get(0).equals("YourTurn")) { callback.accept("PlayersTurn;"); }
        else if (coms.get(0).equals("NotYourTurn")) { callback.accept("NotPlayersTurn;"); }
        else if (coms.get(0).equals("DealingCards")) {
            String hand = coms.get(1) + ";" + coms.get(2) + ";" + coms.get(3) + ";" + coms.get(4) + ";" + 
                          coms.get(5) + ";" + coms.get(6) + ";" + coms.get(7) + ";";
            System.out.println(hand);
            callback.accept("NewHand;" + hand + ";");
        }
        else if (coms.get(0).equals("SingleCard")) { callback.accept("AddCard;" + coms.get(1) + ";"); }
        else if (coms.get(0).equals("UNO")) { callback.accept("UNO;" + coms.get(1) + ";"); }
        else if (coms.get(0).equals("NumberOfPlayers")) { callback.accept("UpdateNumPlayers;"+ coms.get(1) + ";");}
        else if (coms.get(0).equals("PlayerDisconnected")) { callback.accept("PlayerDisconnect;"); }
        else if (coms.get(0).equals("PLAYER")) { callback.accept("Play;" + coms.get(1) + ";" + coms.get(2) + ";"); }
    }
}
