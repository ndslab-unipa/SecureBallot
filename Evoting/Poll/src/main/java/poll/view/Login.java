package poll.view;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import poll.controller.Controller;
import view.ViewAbstrController;

/**
 * Classe che gestisce la scena <i>login.fxml</i>. Contiene i riferimenti a tutti i nodi JavaFX dichiarati nella scena e gestisce tutte
 * le funzioni richieste. Estende {@link view.ViewAbstrController ViewAbstrController}.
 */
public class Login extends ViewAbstrController {
    @FXML private Label welcomeLabel, userLabel, pswLabel;
    @FXML private TextField userField;
    @FXML private PasswordField pswField;
    @FXML private Button loginBtn;
    
    /**
     * Controlla il codice relativo al KeyEvent con cui è chiamato. Se questo corrisponde al codice del tasto "Invio", allora
     * richiama la funzione {@link #checkLogin(ActionEvent) checkLogin()}.
     * <br>
     * E' richiamata ad ogni tasto premuto mentre il focus è su una delle due aree di input della scena (Username e Password). 
     * @param event Evento associato alla chiamata a questa funzione.
     */
    @FXML
    private void checkKey(KeyEvent event) {
    	if (event.getCode().equals(KeyCode.ENTER))
    		checkLogin(null);
    }
    
    /**
     * Prende i dati inseriti all'interno delle aree di input e li invia al controller per effettuarne la verifica 
     * (tramite la funzione {@link Controller#checkLogin(String, String)}).
     * <br>
     * E' richiamata premendo il bottone "Login" o premendo il tasto "Invio" in una delle due aree di input della scena (Username e Password).
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    private void checkLogin(ActionEvent event) {
    	String user = userField.getText(), psw = pswField.getText();
    	((Controller) controller).checkLogin(user, psw);
    }
}
