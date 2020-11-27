package procmgr.view;

import javafx.fxml.FXML;
import procmgr.controller.Controller;
import utils.Constants;
import view.ViewAbstrController;

/**
 * Classe che gestisce la scena <i>boot.fxml</i>. Contiene i riferimenti a tutti i nodi JavaFX dichiarati nella scena e gestisce tutte
 * le funzioni richieste. Estende {@link view.ViewAbstrController ViewAbstrController}.
 */
public class Boot extends ViewAbstrController {
	
	/**
	 * Invoca il metodo del controller {@link Controller#boot()}, per verificare l'esistenza di un utente root e gestire la transizione alla
	 * scena successiva.
	 * <br/>
	 * E' richiamata dal click sul bottone "Avvia connessione al Database".
	 */
    @FXML
    private void boot(){
    	if(Constants.verbose)
    		System.out.println("booting");
        
    	((Controller) controller).boot();
    }
}
