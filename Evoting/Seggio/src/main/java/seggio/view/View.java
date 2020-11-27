package seggio.view;

import java.net.URL;

import javafx.stage.Stage;
import model.State;
import seggio.model.Station;
import utils.Constants;
import view.ViewAbstrController;
import view.ViewManager;

public class View extends ViewManager {
    private Station station;
    private String currScene = "";
    private ViewAbstrController currViewController = null;

    public View(Stage stage, Station station) {
        super(stage);
        this.station = station;
    }
    
    private URL getResourceURL(String file) {
		return getClass().getResource("/fxml/" + file + ".fxml");
    }
    
	@Override
	public void updateFromView() {
		State.StateStation state = station.getState();

		switch(state){
			case NON_ATTIVO:
				if (!currScene.equals("activation")) {
					currScene = "activation";
					currViewController = loadScene(getResourceURL(currScene), mainController);
				}
				break;

			case ATTIVO:
				if (!currScene.equals("main-scene")) {
					currScene = "main-scene";
					currViewController = loadScene(getResourceURL(currScene), mainController);
					((MainScene) currViewController).initStation();
				}
				else {
					((MainScene) currViewController).updatePostsInfo();

					if (station.getNewVoter() == null)
						((MainScene) currViewController).hideNewVoter();
					
					if(Constants.statRfid)
						((MainScene) currViewController).toggleWarningLabel(!station.isRfidReaderReachable());
				}
				break;

			case OFFLINE:
				break;
		}
	}
}
