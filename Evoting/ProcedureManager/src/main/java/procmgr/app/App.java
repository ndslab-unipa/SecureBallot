package procmgr.app;

import exceptions.PEException;
import javafx.application.Application;
import javafx.stage.Stage;
import procmgr.controller.Controller;
import procmgr.controller.PMDB;
import procmgr.model.ProcedureManager;
import procmgr.view.View;
import view.Dialogs;
import view.JavaFXApp;

/**
 * Classe main del modulo ProcedureManager. Inizializza Model, View, Controller e permette di visualizzare la prima scena (boot). 
 * Estende {@link JavaFXApp}, di cui richiama il metodo {@link view.JavaFXApp#start(Stage) start(Stage)} per definire le proprietà comuni 
 * della view (dimensioni, richiesta di password per la chiusura ecc...).
 */
public class App extends JavaFXApp {

	/**
	 * Metodo main, che lancia l'applicazione e richiama il metodo {@link #start(Stage)} per inizializzare Model, View e Controller.
	 * @param args
	 */
	public static void main(String[] args){
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
		stageTitle = "Gestore Procedure";
		super.start(stage);

		//Creo la classe model
		ProcedureManager pm = new ProcedureManager();

		//Creo la classe view
		View view = new View(stage, pm);

		try {
			PMDB db = new PMDB("localhost", "3306", "evotingDB");
			
			// Passo la view e il model al controller.
			Controller controller = new Controller(view, pm, db);
			this.controller = controller;
	
			//Passo il controller alla view
			view.setControllerAndShowStage(controller);
	
			view.update();
		} catch(PEException pee) {
			Dialogs.printException(pee);
		}

	}
	
}

