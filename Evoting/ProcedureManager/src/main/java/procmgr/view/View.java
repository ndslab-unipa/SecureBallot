package procmgr.view;

import javafx.stage.Stage;
import model.State;
import procmgr.model.ProcedureManager;
import view.ViewManager;

import java.net.URL;

/**
 * Classe view del modulo ProcedureManager. Gestisce la scena da mostrare all'utente, sulla base dello stato presente all'interno del model.
 * Estende {@link view.ViewManager}.
 */
public class View extends ViewManager {
    private ProcedureManager pm;

    /**
     * Costruttore con parametri, inizializza lo stage ed il model, richiesti per il funzionamento della view.
     * @param stage Riferimento allo stage
     * @param poll Model di Poll
     */
    public View(Stage stage, ProcedureManager pm) {
        super(stage);
        this.pm = pm;
    }
    
    /**
     * Carica una scena da un file .fxml, presente nella cartella src/main/resources/fxml.
     * @param file Nome del file da caricare, senza l'estensione.
     * @return L'URL della risorsa caricata
     */
    private URL getResourceURL(String file) {
        return getClass().getResource("/fxml/" + file + ".fxml");
    }

    /**
     * Gestisce il caricamento delle scene per aggiornare la View, sulla base dello stato conservato nel Model, 
     * e richiama eventuali funzioni necessarie all'inizializzazione ad al funzionamento della nuova scena.
     */
    @Override
    public void updateFromView() {
        State.StatePM state = pm.getState();

        Technic t;
        switch(state) {
            case CONNECTING:
                loadScene(getResourceURL("boot"), mainController);
                break;
                
            case NO_ROOT:
                loadScene(getResourceURL("noRoot"), mainController);
                break;
                
            case LOGIN:
                loadScene(getResourceURL("login"), mainController);
                break;
                
            case SUPERVISOR:
                loadScene(getResourceURL("supervisor"), mainController);
                break;
                
            case TECHNIC:
            case ROOT:
                t = (Technic) loadScene(getResourceURL("technic"), mainController);
                t.setStage(stage);
                t.initTechnicScene(state == State.StatePM.ROOT);
                break;
        }
    }
}
