package postazione.app;

import java.net.InetAddress;

import exceptions.PEException;
import javafx.application.Application;
import javafx.stage.Stage;
import postazione.controller.Controller;
import postazione.model.Post;
import postazione.view.View;
import utils.Constants;
import view.Dialogs;
import view.JavaFXApp;

/**
 * Classe "main" della postazione.
 * Inizializza il model postazione, fa partire il controller e la view.
 */
public class App extends JavaFXApp {

	/**
	 * Metodo main, che lancia l'applicazione e richiama il metodo {@link #start(Stage)} per inizializzare Model, View e Controller.
	 */
	public static void main(String[] args){
		Application.launch(args);
	}

	/**
	 * Metodo chiamato da Application.launch(). Inizializza Model, View e Controller. Se non riesce, ad esempio perchè
	 * non trova i keystores Java, allora mostra un dialog di errore ed interrompe l'applicazione.
	 */
	@Override
	public void start(Stage stage) throws Exception {
		stageTitle = "Postazione";
		super.start(stage);

		//Creo la classe model
		int numConnections = 5;
		Post post =  new Post(InetAddress.getByName(Constants.urnIp), Constants.portUrn, Constants.portStation, numConnections);

		//Creo la classe view
		View view = new View(stage, post);

		try{
			//Passo la view e il model al controller.
			Controller controller = new Controller(view, post);
			this.controller = controller;

			//Passo il controller alla view
			view.setControllerAndShowStage(controller);

			//Il controller viene avviato.
			controller.start();

			//Aggiorno la view
			view.update();
		}
		catch (PEException pee){
			//controller.shutDown(); -> rompe tutto per qualche motivo
			Dialogs.printException(pee);
		}

	}
}
