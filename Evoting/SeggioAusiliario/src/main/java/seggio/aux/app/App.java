package seggio.aux.app;

import java.net.InetAddress;
import exceptions.PEException;
import javafx.application.Application;
import javafx.stage.Stage;
import seggio.aux.controller.Controller;
import seggio.aux.model.SeggioAusiliario;
import seggio.aux.view.View;
import utils.Constants;
import view.Dialogs;
import view.JavaFXApp;

public class App extends JavaFXApp {
	
	public static void main(String[] args) {
		Application.launch(args);
	}
	
    public void start(Stage stage) throws Exception {
    	stageTitle = "SeggioAusiliario";
    	super.start(stage);

		//Creo la classe model
		int numConnections = 5;
		SeggioAusiliario auxStat = new SeggioAusiliario(InetAddress.getByName(Constants.urnIp), Constants.portUrn, Constants.portStation, numConnections);

		//Creo la classe view
		View view = new View(stage, auxStat);

		try{
			//Passo la view e il model al controller.
			Controller controller = new Controller(view, auxStat);
			this.controller = controller;

			//Passo il controller alla view
			view.setControllerAndShowStage(controller);

			//Il controller viene avviato.
			controller.start();

			view.update();
		}
		catch(PEException pee) {
			//controller.shutDown();
			Dialogs.printException(pee);
		}
    }
}
