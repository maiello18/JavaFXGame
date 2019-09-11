package Server;

import javafx.application.Platform;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Server {
    private final int PLAYER_ONE = 0;
    private final int PLAYER_TWO = 1;
    private final int PLAYER_THREE = 2;
    private final int PLAYER_FOUR = 3;
    private static boolean ThereIsAnActiveGame = false;

    private ArrayList<PlayerThread> playerThreads; //stores all 4 "client" threads
    private Deck deck; //holds all the game cards
    private DiscardCardPile discardCardPile;
    private ServerSocket myServerSocket;
    private Controller controller; //used to access gui elements on another thread

    /******************************************************************************
     * Overloaded Server constructor
     ******************************************************************************/
    public Server(Controller controller) {
        playerThreads = new ArrayList<>();
        this.controller = controller;
    }

    /******************************************************************************
     * This function....
     *  1. Sends string messages to all the online clients to update their GUIs
     ******************************************************************************/
    public void updateClientsGUI(String message) {
        for(PlayerThread player : playerThreads){
            if(player.playerInformation.getPlayerIsOnline()){
                player.sendMessage(message);
            }
        }
    }

    /******************************************************************************
     * This function....
     *  1. Creates and initializes the server
     *  2. Returns true if the server was launched properly, false otherwise
     ******************************************************************************/
    public boolean launchServer(int port) {
        // Initialize all the variables for the server
        playerThreads = new ArrayList<>();
        deck = new Deck();
        discardCardPile = new DiscardCardPile();

        // listens for incoming/connecting clients
        ServerSocketThread serverSocketThread = new ServerSocketThread();

        // Make sure the port number is valid
        try { myServerSocket = new ServerSocket(port); }
        catch (Exception serverException) { return false; }

        // Start the thread for making new connections
        serverSocketThread.start();

        // Everything was set up correctly
        return true;
    }

    /******************************************************************************
     * This function....
     *  1. Turns the server off
     ******************************************************************************/
    public void turnOffServer() {
        Platform.runLater(() -> {
            // Close all connections
            for (PlayerThread pThread : playerThreads) {
                pThread.destroyPlayerThread();
            }});


        playerThreads.clear(); //clear of all current players
        discardCardPile.clearDiscardedCards();
        deck.clearDeck();

        try { myServerSocket.close();}
        catch (Exception failedTurnOff) { }
    }

    /******************************************************************************
     * This Thread Class....
     *  1. Handles all incoming connections and sets them up with their
     *     own communication threads
     ******************************************************************************/
    class ServerSocketThread extends Thread {
        public void run() {
            try {
                while(true) {
                    if(playerThreads.size() == 4){
                        return;
                    }

                    //create the player thread, add it to the thread list, and start the thread
                    PlayerThread newPlayer = new PlayerThread(myServerSocket.accept(), playerThreads.size());

                    if(newPlayer.inStream != null){
                        playerThreads.add(newPlayer);
                        newPlayer.start();
                    }
                    else{
                        newPlayer = null;
                    }

                    //show the cards on hand in server gui
                    controller.playerCardsOnHand(newPlayer.playerInformation.getID(),newPlayer.turnHandIntoString());

                    //necessary for the gui to update....if it wasn't there, it would not update (player name would not)
                    newPlayer.sleep(100);

                    //update the server gui with a client connection
                    controller.playerConnected(playerThreads.size()-1, newPlayer.playerInformation.getName());

                    //alert clients that there is a new player....also tell them the number of players
                    for(PlayerThread p : playerThreads){
                        p.sendMessage("NewPlayer;");
                        p.sendMessage("NumberOfPlayers;" + playerThreads.size() + ";");
                    }

                    //check if we have 4 people and a game can start
                    if(playerThreads.size() == 4){
                        ThereIsAnActiveGame = true;

                        //if so alert the players...client will change scenes at this point...
                        for(PlayerThread p : playerThreads){
                            p.sendMessage("GameReady;");
                        }

                        Thread.sleep(100);

                        //alert them of their decks
                        for(PlayerThread p : playerThreads){
                            //dealing of the initial 7 cards takes place in the PlayerThread() constructor
                            // now we just have to alert the client what cards they have
                            //deck class has a function below that takes a array list of cards and returns the string to be
                            //sent to the client
                            p.sendMessage(deck.turnDealtCardsIntoMessage(p.playerInformation.getCardsInHand()));
                        }

                        //we know we have 4 players, so we have to tell player 1 it is their turn
                        //and players 2,3,4 it is not their turn
                        showAndTellWhoHasNextTurn(PLAYER_ONE);
                    }
                }
            }
            catch (Exception e) {
                try {myServerSocket.close();}
                catch (Exception ex) {ex.printStackTrace();}
                //alert the playerThreads to go back to starting screens
                for(PlayerThread p : playerThreads){
                    p.sendMessage("ServerDied;");
                }
                myServerSocket = null;
            }
        }
    }

    /******************************************************************************
     * This Thread Class....
     *  1. Handles communications with the client
     ******************************************************************************/
    class PlayerThread extends Thread {
        //thread related variables
        Socket communicationSocket;
        ObjectOutputStream outStream;
        ObjectInputStream inStream;

        //player specific variable...holds all game data about the player
        PlayerInformation playerInformation;

        //overload constructor to create the player thread
        PlayerThread(Socket socket, int id){
            System.out.println("created thread");
            communicationSocket = socket;

            try{
                outStream = new ObjectOutputStream(communicationSocket.getOutputStream());
                inStream = new ObjectInputStream(communicationSocket.getInputStream());
            }
            catch (Exception e){
                controller.serverError(e.getMessage());
                outStream = null;
                inStream = null;
                e.printStackTrace();
            }

            playerInformation = new PlayerInformation();

            //initialize the player information
            playerInformation.setPlayerIsOnline(true);
            playerInformation.setHasMoved(false);
            playerInformation.setCardsInHand(deck.distributeCards(7)); //function deals as many cards as you tell it to
            playerInformation.setID(id);
            playerInformation.setMove("");
            //playerInformation.setName("Default");
        }

        //stops the thread's communication and nulls any of its data
        public void destroyPlayerThread(){
            try{
                outStream.close();
                inStream.close();
                communicationSocket.close();
            }
            catch (Exception e) {e.printStackTrace();}

            //the thread needs to be removed from the thread list
            playerThreads.remove(this);

            outStream = null;
            inStream = null;
            communicationSocket = null;
        }

        //sends a message through the thread's out stream
        public void sendMessage(String message){
            try{ outStream.writeObject(message); }
            catch (Exception failToSendMessage){}
        }

        public void run(){
            try{
                while (true){
                    Serializable data = (Serializable) inStream.readObject();
                    System.out.println("Data sent by client: " + data.toString());
                    String flag = interpretData(data.toString());
                    if(ThereIsAnActiveGame){
                        //at this point, the player has sent a message to the server and the server has acted accordingly
                        //now we need to check if a special card was played and figure out who is going next
                        int playerWhoHasNext;
                        if(flag.equals("ReverseCardWasPlayed")){
                            //if its player 1, next move has to go to player 4
                            //if its any other player, next move has to go current player minus 1
                            if(this == playerThreads.get(PLAYER_ONE)){
                                playerWhoHasNext = PLAYER_FOUR;
                            }
                            else{
                                playerWhoHasNext = playerInformation.getID() - 1;
                            }
                        }
                        else{
                            if(playerThreads.size() == 4 && this == playerThreads.get(PLAYER_FOUR)){
                                playerWhoHasNext = PLAYER_ONE;
                            }
                            else{
                                playerWhoHasNext = playerInformation.getID() + 1;
                            }
                            if(flag.equals("DrawTwoWasPlayed")){
                                //draw the cards for the next player
                                Card cardOne = deck.drawOnePenaltyCard(); //returns one card
                                Card cardTwo = deck.drawOnePenaltyCard(); //returns one card

                                //add the cards to the players hand
                                playerThreads.get(playerWhoHasNext).playerInformation.getCardsInHand().add(cardOne);
                                playerThreads.get(playerWhoHasNext).playerInformation.getCardsInHand().add(cardTwo);

                                //deal the above cards to the next player
                                playerThreads.get(playerWhoHasNext).sendMessage("SingleCard;" + cardOne.getCardName() + ";");
                                playerThreads.get(playerWhoHasNext).sendMessage("SingleCard;" + cardTwo.getCardName() + ";");
                            }
                        }

                        //update the visible cards on hand for each player
                        updateAllHandsOnServerGUI();

                        //now alert everyone if its their turn or not
                        showAndTellWhoHasNextTurn(playerWhoHasNext);
                    }
                }
            }
            catch (Exception e){
                //first make sure of no nulls
                if(this.outStream == null){
                    playerThreads.remove(this);

                    //if we are here, then a client thread suddenly quit
                    // so we need to tell all the threads, clean up the server gui, and restart the game
                    //destroyPlayerThread();

                    //alert threads
                    alertThreads();
                }
                else{
                    destroyPlayerThread();
                }


                //update the server gui...completely clean house
                controller.completelyCleanServerGUI();

                //turn off the server and update game information
                turnOffServer();
            }
        }

        /******************************************************************************
         * This function....
         *  1. Takes in a string input
         *  2. Then does actions according to said input (ex: gameplay, deletion of
         *     a thread, etc.)
         ******************************************************************************/
        public String interpretData(String input){
            String returnString = "";
            ArrayList<String> data = messageToArrayList(input);
            String command = data.get(0);

            if(command.equals("PLAY")){
                //"PLAY; String name; String card;"
                String playerName = data.get(1);
                String playerCardName = data.get(2);
                Card playerCard = null;
                for(Card c : deck.getDeck()){
                    if(c.getCardName().equals(playerCardName)){
                        playerCard = c;
                        break;
                    }
                }

                //making sure it is a valid move
                if(discardCardPile.getDiscardedCardPile().isEmpty() || cardIsLegal(discardCardPile.getTopCard(),playerCard)){
                    discardCardPile.addDiscardedCard(playerCard); //add it to the discard pile
                    playerInformation.getCardsInHand().remove(playerCard);//remove it from the clients hand
                    controller.playerCardsOnHand(playerInformation.getID(),this.turnHandIntoString());//update what cards on visible on screen

                    //alert the players what was played by who
                    for(PlayerThread p : playerThreads){
                        p.sendMessage("PLAYER;" + playerInformation.getName() + ";" + playerCardName + ";");
                    }

                    //check if our return string needs to be updated. This is important because it dictates who's turn
                    if(playerCard != null && playerCard.isReverse()){
                        returnString = "ReverseCardWasPlayed";
                    }
                    else if(playerCard != null && playerCard.isDrawTwo()){
                        returnString = "DrawTwoWasPlayed";
                    }
                }
                //else it is not a valid move...notify clients that this player had to draw
                else{
                    playerInformation.getCardsInHand().remove(playerCard);//remove it from the clients hand
                    drawWasMade();
                }
            }
            else if(command.equals("Name")){
                String playerName = data.get(1);
                playerInformation.setName(playerName);
            }
            else if(command.equals("DRAW")){
                drawWasMade();
            }
            else if(command.equals("UNO")){
                //"UNO; String name; int cards;"
                String UNOWinnerName = data.get(1);
                int UNOWinnerNumberOfCards = Integer.parseInt(data.get(2));

                //if == to 0 then we actually have a winner
                if(UNOWinnerNumberOfCards == 0){
                    //alert all the players
                    for(PlayerThread p : playerThreads){
                        p.sendMessage("UNO;" + UNOWinnerName + ";"); //client takes care of going back the start screen
                    }

                    controller.InfoText.setText(UNOWinnerName + " HAS WON!");

                    ThereIsAnActiveGame = false;
                }
                //else the person incorrectly hit UNO so we should penalize them by drawing one card
                else{
                    Card penaltyCard = deck.drawOnePenaltyCard(); //returns one card
                    //add to the players hand
                    playerInformation.getCardsInHand().add(penaltyCard);

                    //show the cards on hand in server gui
                    controller.playerCardsOnHand(playerInformation.getID(),this.turnHandIntoString());

                    this.sendMessage("SingleCard;" + penaltyCard.getCardName() + ";"); //only alert the person who clicked UNO
                }

            }
            else if(command.equals("KILL YOURSELF")){
                this.destroyPlayerThread();
            }

            Platform.runLater(() -> updateAllHandsOnServerGUI());

            return returnString;
        }

        //this function was made to avoid having duplicate code
        public void drawWasMade(){
            Card penaltyCard = deck.drawOnePenaltyCard(); //returns one card
            playerInformation.getCardsInHand().add(penaltyCard); // add it to the players hand
            //show the cards on hand in server gui
            controller.playerCardsOnHand(playerInformation.getID(),this.turnHandIntoString());

            this.sendMessage("SingleCard;" + penaltyCard.getCardName() + ";"); //only alert the person who clicked

            //tell players the player had/chose to draw
            for(PlayerThread p : playerThreads){
                if(p != this){
                    p.sendMessage("PLAYER;" + playerInformation.getName() + ";" + "Draw;");
                }
            }
        }

        public String turnHandIntoString(){
            String s = "";

            for(Card c : playerInformation.getCardsInHand()){
                s += c.getCardName() + "; ";
            }

            return s;
        }

    }

    public void updateAllHandsOnServerGUI(){
        for(PlayerThread p : playerThreads){
            //show the cards on hand in server gui
            controller.playerCardsOnHand(p.playerInformation.getID(),p.turnHandIntoString());
        }
    }

    public void alertThreads() {
        //the thread needs to be killed
        //playerThread.destroyPlayerThread();

        Platform.runLater(() ->
        {
            //alert all the clients/players that this client quit
            for(PlayerThread p : playerThreads){
                p.sendMessage("PlayerDisconnected;");}

        });

    }

    //this function was made to avoid having duplicate code
    public void showAndTellWhoHasNextTurn(int playerWhoHasNext){
        //now alert everyone if its their turn or not
        for(PlayerThread p : playerThreads){
            if(p == playerThreads.get(playerWhoHasNext)){
                p.sendMessage("YourTurn;");
                controller.showPlayerTurn(playerWhoHasNext);
            }
            else{
                p.sendMessage("NotYourTurn;");
            }
        }
    }

    /******************************************************************************
     * This Function ....
     *  1. Takes two cards as parameters and returns true/false depending on whether
     *     or not they are matching cards (same color, same number, or wild)
     ******************************************************************************/
    public boolean cardIsLegal(Card topOfDiscardPileCard, Card playerCard){
        return     playerCard.getCardName().equals("W")
                || playerCard.getCardName().equals(topOfDiscardPileCard.getCardName())
                || playerCard.getColor().equals(topOfDiscardPileCard.getColor())
                || topOfDiscardPileCard.getCardName().equals("W")
                || playerCard.getCardNumber() == topOfDiscardPileCard.getCardNumber();
    }

    /******************************************************************************
     * This Function ....
     *  1. Takes an incoming string message and "splits" the string into an array
     *     list of strings using the given regex
     ******************************************************************************/
    public ArrayList<String> messageToArrayList(String message) {
        return new ArrayList<String>(Arrays.asList(message.split(";")));
    }

    /******************************************************************************
     * This Class ....
     *  1. Holds all data necessary for a player client
     *      The player's id, name, move, online status, has moved status, and cards
     *      in their hand
     ******************************************************************************/
    public class PlayerInformation{
        private int ID;
        private String Name;
        private String Move;
        private boolean playerIsOnline;
        private boolean hasMoved;
        private ArrayList<Card> cardsInHand;

        //************************GETTERS************************
        public int getID() {
            return ID;
        }
        public String getName() {
            return Name;
        }
        public String getMove() {
            return Move;
        }
        public boolean getPlayerIsOnline() {
            return playerIsOnline;
        }
        public boolean getHasMoved() {
            return hasMoved;
        }
        public ArrayList<Card> getCardsInHand() {
            return cardsInHand;
        }
        //**********************END GETTERS**********************

        //************************SETTERS************************
        public void setID(int ID) {
            this.ID = ID;
        }
        public void setName(String name) {
            Name = name;
        }
        public void setMove(String move) {
            Move = move;
        }
        public void setPlayerIsOnline(boolean playerIsOnline) {
            this.playerIsOnline = playerIsOnline;
        }
        public void setHasMoved(boolean hasMoved) {
            this.hasMoved = hasMoved;
        }
        public void setCardsInHand(ArrayList<Card> cardsInHand) {
            this.cardsInHand = cardsInHand;
        }
        //**********************END SETTERS**********************
    }

    /******************************************************************************
     * This Class ....
     *  1. Holds all data necessary for a deck
     *      The list of cards
     *  2. The functions needed to distribute said cards
     *      Distribute/deal, init, mark, shuffle
     ******************************************************************************/
    public class Deck{
        private ArrayList<Card> deck;

        public ArrayList<Card> getDeck() { return deck; }
        public void setDeck(ArrayList<Card> deck) { this.deck = deck; }

        Deck(){
            deck = new ArrayList<>();
            initializeDeck();
            shuffleDeck();
        }

        public String turnDealtCardsIntoMessage(ArrayList<Card> hand){
            String message = ("DealingCards;");

            for(Card c : hand){
                message += c.getCardName() + ";";
            }

            return message;
        }

        public ArrayList<Card> distributeCards(int dealThisMany){
            ArrayList<Card> cards = new ArrayList<>();

            for(Card c : this.deck){
                if(dealThisMany == 0){
                    break;
                }
                else if(!c.cardPlayed){
                    c.setCardPlayed(true);
                    cards.add(c);
                    dealThisMany--;
                }
            }

            return cards;
        }

        public void clearDeck(){
            this.deck.clear();
        }

        public void shuffleDeck(){
            Collections.shuffle(this.deck);
        }

        public void initializeDeck(){

            this.deck.add(new Card("R0",false,true, false, false,
                    false,"Red",0));
            this.deck.add(new Card("G0",false,true, false, false,
                    false,"Green",0));
            this.deck.add(new Card("B0",false,true, false, false,
                    false,"Blue",0));
            this.deck.add(new Card("Y0",false,true, false, false,
                    false,"Yellow",0));

            //create the numbered cards
            for(int i = 1; i < 10; i++){
                for(int j = 0; j < 2; j++){
                    this.deck.add(new Card("R"+i,false,true, false, false,
                            false,"Red",i));
                }
            }
            //create the numbered cards
            for(int i = 1; i < 10; i++){
                for(int j = 0; j < 2; j++){
                    this.deck.add(new Card("G"+i,false,true, false, false,
                            false,"Green",i));
                }
            }
            //create the numbered cards
            for(int i = 1; i < 10; i++){
                for(int j = 0; j < 2; j++){
                    this.deck.add(new Card("B"+i,false,true, false, false,
                            false,"Blue",i));
                }
            }
            //create the numbered cards
            for(int i = 1; i < 10; i++){
                for(int j = 0; j < 2; j++){
                    this.deck.add(new Card("Y"+i,false,true, false, false,
                            false,"Yellow",i));
                }
            }


            //create the draw two cards
            for(int i = 0; i < 2; i++){
                this.deck.add(new Card("RDT",false,false, true, false,
                        false,"Red",i));
            }
            //create the draw two cards
            for(int i = 0; i < 2; i++){
                this.deck.add(new Card("BDT",false,false, true, false,
                        false,"Blue",i));
            }
            //create the draw two cards
            for(int i = 0; i < 2; i++){
                this.deck.add(new Card("GDT",false,false, true, false,
                        false,"Green",i));
            }
            //create the draw two cards
            for(int i = 0; i < 2; i++){
                this.deck.add(new Card("YDT",false,false, true, false,
                        false,"Yellow",i));
            }


            //create the reverse cards
            for(int i = 0; i < 2; i++){
                this.deck.add(new Card("RR",false,false, false, true,
                        false,"Red",i));
            }
            //create the reverse cards
            for(int i = 0; i < 2; i++){
                this.deck.add(new Card("GR",false,false, false, true,
                        false,"Green",i));
            }
            //create the reverse cards
            for(int i = 0; i < 2; i++){
                this.deck.add(new Card("BR",false,false, false, true,
                        false,"Blue",i));
            }
            //create the reverse cards
            for(int i = 0; i < 2; i++){
                this.deck.add(new Card("YR",false,false, false, true,
                        false,"Yellow",i));
            }

            //wild cards
            for(int i = 0; i < 4; i++){
                this.deck.add(new Card("W",false,false, false, false,
                        false,"Wild",i));
            }
        }

        public Card drawOnePenaltyCard(){
            Card c = null;
            for(Card card : this.deck){
                if(!card.cardPlayed){
                    card.setCardPlayed(true);
                    c = card;
                    break;
                }
            }

            return c;
        }
    }

    /******************************************************************************
     * This Class....
     *  1. Holds all data necessary for a discard pile
     *      The list of cards
     *  2. The functions needed to add said cards
     *      add, init
     ******************************************************************************/
    public class DiscardCardPile{
        private ArrayList<Card> discardedCardPile;

        public ArrayList<Card> getDiscardedCardPile() { return discardedCardPile; }
        public void setDiscardedCardPile(ArrayList<Card> discardedCardPile) { this.discardedCardPile = discardedCardPile; }

        DiscardCardPile(){
            this.discardedCardPile = new ArrayList<>();
        }

        public Card getTopCard(){
            return this.discardedCardPile.get(this.discardedCardPile.size() - 1);
        }

        public void addDiscardedCard(Card c){
            this.discardedCardPile.add(c);
        }

        public void clearDiscardedCards(){
            this.discardedCardPile.clear();
        }
    }

    /******************************************************************************
     * This Class....
     *  1. Holds all data necessary for a card
     *      The card name, its card type
     ******************************************************************************/
    public class Card{
        private String cardName;
        private boolean cardPlayed;
        private boolean isNormalCard;
        private boolean isDrawTwo;
        private boolean isReverse;
        private boolean isWild;
        private String cardColor;
        private int cardNumber;

        Card(String cardName, boolean cardPlayed, boolean isNormalCard, boolean isDrawTwo,
             boolean isReverse, boolean isWild, String color, int cardNumber){
            this.cardName = cardName;
            this.cardPlayed = cardPlayed;
            this.isNormalCard = isNormalCard;
            this.isDrawTwo = isDrawTwo;
            this.isReverse = isReverse;
            this.isWild = isWild;
            this.cardColor = color;
            this.cardNumber = cardNumber;
        }

        //************************GETTERS************************
        public String getColor(){return this.cardColor;}
        public boolean isNormalCard() {
            return isNormalCard;
        }
        public String getCardName() { return cardName; }
        public boolean isCardPlayed() { return cardPlayed; }
        public boolean isWild() { return isWild; }
        public boolean isReverse() { return isReverse; }
        public boolean isDrawTwo() { return isDrawTwo; }
        public int getCardNumber() {return  cardNumber;}
        //**********************END GETTERS**********************

        //************************SETTERS************************
        public void setColor(String c){this.cardColor = c;}
        public void setNormalCard(boolean normalCard) { isNormalCard = normalCard; }
        public void setCardName(String cardName) { this.cardName = cardName; }
        public void setCardPlayed(boolean cardPlayed) { this.cardPlayed = cardPlayed; }
        public void setDrawTwo(boolean drawTwo) { isDrawTwo = drawTwo; }
        public void setReverse(boolean reverse) { isReverse = reverse; }
        public void setWild(boolean wild) { isWild = wild; }
        //**********************END SETTERS**********************
    }
}
