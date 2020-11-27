package poll.app;

import exceptions.PEException;
import javafx.application.Application;
import javafx.stage.Stage;
import poll.controller.Controller;
import poll.controller.PollDB;
import poll.model.Poll;
import poll.view.View;
import view.Dialogs;
import view.JavaFXApp;

/**
 * Classe main del modulo Poll. Inizializza Model, View, Controller e permette di visualizzare la prima scena (login). Estende {@link JavaFXApp},
 * di cui richiama il metodo {@link view.JavaFXApp#start(Stage) start(Stage)} per definire le proprietà comuni della view (dimensioni, richiesta di
 * password per la chiusura ecc...).
 *
 */
public class App extends JavaFXApp {
	
	/**
	 * Metodo main, che lancia l'applicazione e richiama il metodo {@link #start(Stage)} per inizializzare Model, View e Controller.
	 * @param args
	 */
	public static void main(String[] args) {
	    Application.launch(args);
	}

	/**
	 * Metodo chiamato da Application.launch(). Inizializza Model, View, Controller ed interfaccia col DB. Se non riesce, ad esempio perchè
	 * non trova il file con le credenziali del DB, o perchè non trova i keystores Java, allora mostra un dialog di errore ed interrompe
	 * l'applicazione.
	 * @param stage Oggetto Stage per la visualizzazione di scene JavaFX
	 */
    @Override
    public void start(Stage stage) throws Exception {
    	stageTitle = "Poll";
    	super.start(stage);

    	//Creo la classe model
		Poll poll = new Poll();
		
		//Creo la classe view
		View view = new View(stage, poll);
		
		try {
			//Creo l'interfaccia col DB
	    	PollDB db = new PollDB("localhost", "3306", "evotingDB");
	    	
	    	//Passo la view, il model ed il db al controller
	    	Controller pc = new Controller(view, poll, db);
	    	this.controller = pc;
	    	
	    	//Passo il controller alla view
	    	view.setControllerAndShowStage(pc);
	    	
	    	//Aggiorno la view
	    	view.update();
    	} catch (PEException e) {
    		Dialogs.printException(e);
    	}
    }
}