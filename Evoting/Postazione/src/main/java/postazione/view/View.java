package postazione.view;

import java.net.URL;

import javafx.stage.Stage;
import model.State.StatePost;
import postazione.model.Post;
import utils.Constants;
import view.ViewAbstrController;
import view.ViewManager;

public class View extends ViewManager {
    private Post post;
    private String currScene = "";
    private ViewAbstrController currViewController = null;

    public View(Stage stage, Post post) {
        super(stage);
        this.post = post;
    }
    
    private URL getResourceURL(String file) {
        return getClass().getResource("/fxml/" + file + ".fxml");
    }

    @Override
    public void updateFromView() {
        StatePost state = post.getState();

        switch(state) {
            case NON_ATTIVA :
            	if(!currScene.equals("activation")) {
            		currScene = "activation";
            		loadScene(getResourceURL("activation"), mainController);
            	}
            	break;
            	
            case ATTIVA:
            	if(!currScene.equals("show-msg-attiva")) {
            		currScene = "show-msg-attiva";
            		currViewController = loadScene(getResourceURL("show-message"), mainController);
                	((ShowMessage) currViewController).setInfos("Attiva", "Postazione Pronta", null, null, false);
            	}
            	break;

            case ASSOCIATA:
            	if(!currScene.equals("show-msg-associata")) {
            		currScene = "show-msg-associata";
            		currViewController = loadScene(getResourceURL("show-message"), mainController);
            		((ShowMessage) currViewController).setInfos("Associata", "Avvicina la card al lettore", post.getBadge(), "rfid.png", false);
            	}
            	else if (Constants.postRfid) {
        			((ShowMessage) currViewController).setRfidReachable(post.isRfidReaderReachable());
        			((ShowMessage) currViewController).updateLastWrongBadge(post.getLastWrongBadge());
            	}
            	break;

            case IN_USO:
            	if(!currScene.equals("main-scene")) {
            		currScene = "main-scene";
                    currViewController = loadScene(getResourceURL("main-scene"), mainController);
            	}
                break;
                
            case VOTO_PENDING:
            	if(!currScene.equals("show-msg-pending")) {
            		currScene = "show-msg-pending";
            		currViewController = loadScene(getResourceURL("show-message"), mainController);
            		((ShowMessage) currViewController).setInfos("Errore Invio Voti", "Non è stato possibile inviare i voti al seggio. Riprova.", null, null, true);
            	}
            	break;

            case VOTO_INVIATO:
            	if(!currScene.equals("show-msg-inviato")) {
            		currScene = "show-msg-inviato";
            		currViewController = loadScene(getResourceURL("show-message"), mainController);
            		((ShowMessage) currViewController).setInfos("Voto inviato", "Restituire la card allo staff", null, null, false);
            	}
                break;
                
            case DA_RIAVVIARE:
            	if(!currScene.equals("show-msg-riavvio")) {
            		currScene = "show-msg-riavvio";
            		currViewController = loadScene(getResourceURL("show-message"), mainController);
                	((ShowMessage) currViewController).setInfos("Da riavviare", "Necessario riavvio della postazione. Contattare lo staff.", null, "error.png", false);
            	}
            	break;
                
            case DA_RESETTARE:
            	if(!currScene.equals("show-msg-reset")) {
            		currScene = "show-msg-reset";
            		currViewController = loadScene(getResourceURL("show-message"), mainController);
                	((ShowMessage) currViewController).setInfos("Da resettare", "Necessario reset o riavvio della postazione. Contattare lo staff.", null, "error.png", false);
            	}
            	break;
            	
            case OFFLINE:
				break;
        }
    }
}
