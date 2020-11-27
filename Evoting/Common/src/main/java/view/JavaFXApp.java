package view;

import java.util.List;

import controller.AbstrController;
import controller.TerminalController;
import javafx.application.Application;
import javafx.stage.Stage;
import utils.Constants;

public class JavaFXApp extends Application {
	private List<String> terminals = List.of("Postazione", "Seggio", "Urna", "SeggioAusiliario");
	protected AbstrController controller = null;
	protected String stageTitle = "";
	
	@Override
	public void start(Stage stage) throws Exception {
		stage.setOnCloseRequest(event -> {
			if(!Constants.devMode) {
				String psw = Dialogs.printTextConfirmation("Conferma Chiusura", "Password Richiesta", "Inserisci la password");
	    		
				if (psw.equals("<<abort>>")) {
	    			event.consume();
	    			return;
				}
				
	    		if (!psw.equals(Constants.exitCode)) {
	    			Dialogs.printError("Password Errata", "La password inserita non è corretta", "Per favore, riprova.");
	    			event.consume();
	    			return;
	    		}
			}
			
			if (terminals.contains(stageTitle))
				((TerminalController) controller).shutDown();
    	});
		
		stage.setTitle(stageTitle);
		
        stage.setMinWidth(720);
        stage.setWidth(840);
        
        stage.setMinHeight(510);
        stage.setHeight(590);
        
        stage.setResizable(true);
	}
}
