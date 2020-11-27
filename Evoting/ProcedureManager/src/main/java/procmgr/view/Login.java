package procmgr.view;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import procmgr.controller.Controller;
import view.ViewAbstrController;

/**
 * Classe che gestisce la scena <i>login.fxml</i>. Contiene i riferimenti a tutti i nodi JavaFX dichiarati nella scena e gestisce tutte
 * le funzioni richieste. Estende {@link view.ViewAbstrController ViewAbstrController}.
 */
public class Login extends ViewAbstrController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    /**
     * Controlla il codice relativo al KeyEvent con cui è chiamato. Se questo corrisponde al codice del tasto "Invio", allora
     * richiama la funzione {@link #checkLogin(ActionEvent) checkLogin()}.
     * <br>
     * E' richiamata ad ogni tasto premuto mentre il focus è su una delle due aree di input della scena (Username e Password). 
     * @param event Evento associato alla chiamata a questa funzione.
     */
    @FXML
    void checkKey(KeyEvent event) {
    	if(event.getCode().equals(KeyCode.ENTER))
    		login(null);
    }
    
    /**
     * Prende i dati inseriti all'interno delle aree di input e li invia al controller per effettuarne la verifica 
     * (tramite la funzione {@link Controller#login(String, String)}).
     * <br>
     * E' richiamata premendo il bottone "Login" o premendo il tasto "Invio" in una delle due aree di input della scena (Username e Password).
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    void login(ActionEvent actionEvent) {
        ((Controller) controller).login(usernameField.getText(), passwordField.getText());
    }
}
