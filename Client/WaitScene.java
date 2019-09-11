package Client;

import javafx.event.ActionEvent;
import javafx.scene.text.Text;

public class WaitScene {
    public Text playersConnected;
    private ClientManager manager;

    void init(ClientManager manager) {
        this.manager = manager;
        playersConnected.setText(manager.data_playersConnected);
    }

    public void updatePlayers(String num) {
        playersConnected.setText(num);
    }

    public void Quit_button(ActionEvent actionEvent) {
        manager.wantsToQuit = true;
        manager.getConnection().killConnection();
        manager.resetMenu();
    }
}
