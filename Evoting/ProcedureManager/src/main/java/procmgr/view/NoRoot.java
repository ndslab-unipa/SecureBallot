package procmgr.view;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import procmgr.controller.Controller;
import view.ViewAbstrController;

/**
 * Classe che gestisce la scena <i>noRoot.fxml</i>. Contiene i riferimenti a tutti i nodi JavaFX dichiarati nella scena e gestisce tutte
 * le funzioni richieste. Estende {@link view.ViewAbstrController ViewAbstrController}.
 */
public class NoRoot extends ViewAbstrController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField, repeatPasswordField;
    
    /**
     * Gestisce la creazione dell'utente root, richiamando la funzione {@link Controller#startUserCreation(String, String, String, String)}
     * e passandogli username e password inseriti nei campi di input della scena (Username, Password e Conferma Password).
     * <br/>
     * E' richiamata dal click sul bottone "Crea Utente".
     */
    @FXML
    private void createRoot(){
        String username = usernameField.getText();
        String password = passwordField.getText();
        String repeatPassword = repeatPasswordField.getText();

        ((Controller) controller).startUserCreation("Root", username, password, repeatPassword);
    }

}
