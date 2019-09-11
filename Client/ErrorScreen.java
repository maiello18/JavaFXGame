package Client;

import javafx.event.ActionEvent;
import javafx.scene.text.Text;

public class ErrorScreen {
    public Text message;
    private ClientManager manager;


    void init(ClientManager manager) {
        this.manager = manager;

        message.setText(manager.data_errorMsg);
    }

    public void Okay_Button(ActionEvent actionEvent) {
        manager.resetMenu();
    }
}
