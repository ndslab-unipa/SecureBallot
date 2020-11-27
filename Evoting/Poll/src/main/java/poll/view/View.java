package poll.view;

import java.net.URL;

import javafx.stage.Stage;
import model.State.StatePoll;
import poll.model.Poll;
import view.ViewManager;

/**
 * Classe view del modulo Poll. Gestisce la scena da mostrare all'utente, sulla base dello stato presente all'interno del model.
 * Estende {@link view.ViewManager}.
 */
public class View extends ViewManager {
    private Poll poll;
    private Stage stage;

    /**
     * Costruttore con parametri, inizializza lo stage ed il model, richiesti per il funzionamento della view.
     * @param stage Riferimento allo stage
     * @param poll Model di Poll
     */
    public View(Stage stage, Poll poll) {
        super(stage);
        this.poll = poll;
        this.stage = stage;
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
        StatePoll state = poll.getState();

        switch(state) {
            case NON_ATTIVO :
            	loadScene(getResourceURL("login"), mainController);
            	break;
            	
            case ATTIVO: 
            	ProcedureSelection ps = (ProcedureSelection) loadScene(getResourceURL("procedure-selection"), mainController);
            	ps.getProcedures();
            	break;

            case COUNTING:
                MainScene mainSceneController = (MainScene) loadScene(getResourceURL("main-scene"), mainController);
                mainSceneController.setStage(stage);
                break;
        }
    }
}
