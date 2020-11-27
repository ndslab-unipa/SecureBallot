package seggio.app;

import java.net.InetAddress;

import exceptions.PEException;
import javafx.application.Application;
import javafx.stage.Stage;
import seggio.controller.Controller;
import seggio.model.Station;
import seggio.view.View;
import utils.Constants;
import view.Dialogs;
import view.JavaFXApp;

public class App extends JavaFXApp {

	public static void main(String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		stageTitle = "Seggio";
		super.start(stage);

		//Creo la classe model
		int numConnections = 5;
		Station station = new Station(InetAddress.getByName(Constants.urnIp), Constants.portUrn, Constants.portSubStation, Constants.portPost, numConnections);

		//Creo la classe view
		View view = new View(stage, station);

		try{
			//Passo la view e il model al controller.
			Controller controller = new Controller(view, station);
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
