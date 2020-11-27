package urna.view;

import java.net.URL;

import javafx.stage.Stage;
import model.State.StateUrn;
import urna.model.Urn;
import view.ViewAbstrController;
import view.ViewManager;

public class View extends ViewManager {
    private Urn urn;
    private String currScene = "";
    private ViewAbstrController currViewController = null;

    public View(Stage stage, Urn urn) {
        super(stage);
        this.urn = urn;
    }
    
    private URL getResourceURL(String file) {
		return getClass().getResource("/fxml/" + file + ".fxml");
    }
    
    @Override
    public void updateFromView() {
        StateUrn state = urn.getState();

        switch(state) {
            case NON_ATTIVA:
            	if (!currScene.equals("login")) {
            		currScene = "login";
            		currViewController = loadScene(getResourceURL(currScene), mainController);
            	}
            	break;
            	
            case ATTIVA: 
            	if (!currScene.equals("session-selection")) {
            		currScene = "session-selection";
            		currViewController = loadScene(getResourceURL(currScene), mainController);
            		((SessionSelection) currViewController).getSessions();
            	}
            	break;

            case LOGGING:
            	if (!currScene.equals("main-scene")) {
            		currScene = "main-scene";
            		currViewController = loadScene(getResourceURL(currScene), mainController);
            	}
            	
        		((MainScene) currViewController).printLogs();
                break;
        }
    }
}
