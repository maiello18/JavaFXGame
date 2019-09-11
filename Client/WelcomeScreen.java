package Client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

public class WelcomeScreen {
    //public HBox Connecting_Box;
    public Button connect_btn;
    public Button quit_btn;

    private ClientManager manager;

    @FXML
    public TextField userName_input, address_input, port_input;
    @FXML
    public Text message;

    public void init(ClientManager manager) {
        this.manager = manager;
        manager.wantsToQuit = false;

        // Set default/already used values
        userName_input.setText(manager.data_user);
        address_input.setText(manager.data_host);
        port_input.setText(manager.data_port);

        connect_btn.setDisable(false);
        quit_btn.setDisable(true);
    }

    @FXML
    public void Connect_button(ActionEvent actionEvent) {
        // Update GUI for Connecting
        connect_btn.setDisable(true);
        quit_btn.setDisable(false);

        // Update manager on Values
        manager.data_host = address_input.getText();
        manager.data_port = port_input.getText();
        manager.data_user = userName_input.getText();

        System.out.println(manager.data_host + " " + manager.data_port + " " + manager.data_user);

        // Tell the manager to Connect to the Server
        manager.ConnectToServer();
    }

    @FXML
    public void Quit_button(ActionEvent actionEvent) {
        manager.Quit();
    }
}
