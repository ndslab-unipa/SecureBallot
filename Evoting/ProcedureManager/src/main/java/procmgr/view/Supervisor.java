package procmgr.view;

import javafx.event.ActionEvent;
import procmgr.controller.Controller;
import view.ViewAbstrController;

/**
 * Classe che gestisce la scena <i>supervisor.fxml</i>. Contiene i riferimenti a tutti i nodi JavaFX dichiarati nella scena e gestisce tutte
 * le funzioni richieste. Estende {@link view.ViewAbstrController ViewAbstrController}.
 */
public class Supervisor extends ViewAbstrController {
	
	/**
     * Funzione richiamata dal click sul menù File &gt; Logout. Invoca la funzione {@link Controller#logout()} per gestire il logout.
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
	public void logout(ActionEvent actionEvent) {
        ((Controller) controller).logout();
    }
}
