package seggio.aux.view;

import java.net.URL;

import javafx.stage.Stage;
import model.State;
import seggio.aux.model.SeggioAusiliario;
import utils.Constants;
import view.ViewAbstrController;
import view.ViewManager;

public class View extends ViewManager {
    private SeggioAusiliario substation;
    private String currScene = "";
    private ViewAbstrController currViewController = null;

    public View(Stage stage, SeggioAusiliario substation) {
        super(stage);
        this.substation = substation;
    }
    
    private URL getResourceURL(String file) {
		return getClass().getResource("/fxml/" + file + ".fxml");
    }

	@Override
	public void updateFromView() {
		State.StateSubStation state = substation.getState();

		switch(state){
			case NON_ATTIVO:
				if (!currScene.equals("activation")) {
					currScene = "activation";
					currViewController = loadScene(getResourceURL(currScene), mainController);
				}
				break;

			case IN_ATTESA:
				if (!currScene.equals("waiting")) {
					currScene = "waiting";
					currViewController = loadScene(getResourceURL(currScene), mainController);
				}
				break;
				
			case ATTIVO:
				if (!currScene.equals("main-scene")) {
					currScene = "main-scene";
					currViewController = loadScene(getResourceURL(currScene), mainController);
					((MainScene) currViewController).initSubStation();
				}
				else {
					((MainScene) currViewController).updatePostsInfo();

					if(substation.getNewVoter() == null)
						((MainScene) currViewController).hideNewVoter();
					
					if(Constants.auxStatRfid)
						((MainScene) currViewController).toggleWarningLabel(!substation.isRfidReaderReachable());
				}
				break;

			case OFFLINE:
				break;
		}
	}
}
