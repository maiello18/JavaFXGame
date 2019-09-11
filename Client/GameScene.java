package Client;

import javafx.application.Platform;
import javafx.event.ActionEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class GameScene {
	private ClientManager manager;

    public ArrayList<String> playerHand;
    public int getNumCardsInHand() { return playerHand.size(); }

    private ArrayList<ImageView> imageViewList;

	public String playerName;

	@FXML
	private ImageView card_1, card_2, card_3, card_4, card_5,
            card_6, card_7, card_8, card_9, card_10;
    @FXML
	private VBox Card_1, Card_2, Card_3, Card_4, Card_5,
			Card_6, Card_7, Card_8, Card_9, Card_10;
	@FXML
	private ImageView top_of_deck;
	@FXML
    private Button quit_btn, uno_btn, draw_card_btn;
    @FXML
    private Text player_inst;

	public boolean isTurn;

	public void init(ClientManager manager, String playerName) {
		this.manager = manager;
		this.playerName = playerName;
		isTurn = false;

		quit_btn.setDisable(false);

		imageViewList = new ArrayList<>();
		imageViewList.add(card_1);
        imageViewList.add(card_2);
        imageViewList.add(card_3);
        imageViewList.add(card_4);
        imageViewList.add(card_5);
        imageViewList.add(card_6);
        imageViewList.add(card_7);
        imageViewList.add(card_8);
        imageViewList.add(card_9);
        imageViewList.add(card_10);
	}

	public void enablePlay() {
        isTurn = true;
        uno_btn.setDisable(false);
        draw_card_btn.setDisable(false);
        player_inst.setText(playerName + ", make a move!");
	}

	public void disablePlay() {
        isTurn = false;
        draw_card_btn.setDisable(true);
        player_inst.setText(playerName + ", please wait your turn!");
	}

	public void setHand(ArrayList<String> hand) {
        playerHand = hand;
        updatePlayerHandView();
	}

	public void addCard(String card) {
        if (getNumCardsInHand() < 10 ) {
            playerHand.add(card);
            updatePlayerHandView();
        }
    }
    
    public void updatePlayerHandView() {
        int size = getNumCardsInHand();

        //System.out.println(playerHand + " SIZE: " + size);

        // Make current cards visible
        for (int i = 0; i < size; i++) {
		    String cardName = getCardPNGFileName(playerHand.get(i));
            Image image = null;

            try {
                File imgPath = new File("C:\\Users\\mikea\\Downloads\\helloworldtest\\src\\Client\\cards\\" + cardName);
                image = new Image(new FileInputStream(imgPath));

                imageViewList.get(i).setVisible(true);
                imageViewList.get(i).setDisable(false);
                imageViewList.get(i).setImage(image);
            }
            catch (Exception e) { System.out.println("Failed to set player card image!!!"); e.printStackTrace(); }
        }

        // Make empty cards hidden
        for (int i = size; i < 10; i++) {
            imageViewList.get(i).setVisible(false);
            imageViewList.get(i).setDisable(true);
        }
    }

	public void declareWinner(String player) {
        player_inst.setText(player + ", has won the game! Game over.");
        uno_btn.setDisable(true);
        draw_card_btn.setDisable(true);
    }
    
    public void setTopOfDeck(String player, String card) {
        if (!card.equals("Draw")) {
            String cardName = getCardPNGFileName(card);
            Image image = null;

            try {
                File imgPath = new File("C:\\Users\\mikea\\Downloads\\helloworldtest\\src\\Client\\cards\\" + cardName);
                image = new Image(new FileInputStream(imgPath));

                top_of_deck.setImage(image);
            }
            catch(Exception e) { System.out.println("Failed to set top of deck card image!!!"); e.printStackTrace(); }
        }
    }   
    
    public void playerMaker(int cardPos) {
        isTurn = false;
        if (imageViewList.get(cardPos).isVisible()) {
            manager.gameMakeMove(playerHand.get(cardPos), playerName);
            playerHand.remove(cardPos);
            updatePlayerHandView();
        }
    }

	// button on action functions
	@FXML
	public void Uno_button(ActionEvent event) {
		manager.declareUno(playerHand.size());
    }
    @FXML
    public void Draw_button(ActionEvent event) {
        if (getNumCardsInHand() <= 10 && isTurn) {
            isTurn = false;
            manager.makeDraw();
        }
    }
	@FXML
	public void Quit_button(ActionEvent event) {
		manager.getConnection().killConnection();
		manager.resetMenu();
    }
    
    // card on mouse clicked event
	@FXML
	public void Card_1(MouseEvent event) {
		if (isTurn) playerMaker(0);
	}
	@FXML
	public void Card_2(MouseEvent event) {
		if (isTurn) playerMaker(1);
	}
	@FXML
	public void Card_3(MouseEvent event) {
        if (isTurn) playerMaker(2);
	}
	@FXML
	public void Card_4(MouseEvent event) {
        if (isTurn) playerMaker(3);
	}
	@FXML
	public void Card_5(MouseEvent event) {
        if (isTurn) playerMaker(4);
	}
	@FXML
	public void Card_6(MouseEvent event) {
        if (isTurn) playerMaker(5);
	}
	@FXML
	public void Card_7(MouseEvent event) {
        if (isTurn) playerMaker(6);
	}
	@FXML
	public void Card_8(MouseEvent event) {
        if (isTurn) playerMaker(7);
	}
	@FXML
	public void Card_9(MouseEvent event) {
        if (isTurn) playerMaker(8);
	}
	@FXML
	public void Card_10(MouseEvent event) {
        if (isTurn) playerMaker(9);
	}

    public String getCardPNGFileName(String s){

        //first check wild
        if(s.equals("W")){
            return "wild_color_changer.png";
        }

        Character colorChar = s.charAt(0);
        Character numberChar = s.charAt(1);
        Character checkForDT_R = s.charAt(1);

        //Reverse
        if(checkForDT_R.equals('R')){
            if(colorChar.equals('R')){
                return "red_reverse.png";
            }
            else if(colorChar.equals('G')) {
                return "green_reverse.png";
            }
            else if(colorChar.equals('B')) {
                return "blue_reverse.png";
            }
            else if(colorChar.equals('Y')){
                return "yellow_reverse.png";
            }
        }
        //Draw two
        else if(checkForDT_R.equals('D')){
            if(colorChar.equals('R')){
                return "red_picker.png";
            }
            else if(colorChar.equals('G')) {
                return "green_picker.png";
            }
            else if(colorChar.equals('B')) {
                return "blue_picker.png";
            }
            else if(colorChar.equals('Y')){
                return "yellow_picker.png";
            }
        }

        //normal color cards
        if(colorChar.equals('R')){
            return "red_" + numberChar + ".png";
        }
        else if(colorChar.equals('G')) {
            return "green_" + numberChar + ".png";
        }
        else if(colorChar.equals('B')) {
            return "blue_" + numberChar + ".png";
        }
        else if(colorChar.equals('Y')){
            return "yellow_" + numberChar + ".png";
        }

        return "NoMatchingCard";
    }
}
