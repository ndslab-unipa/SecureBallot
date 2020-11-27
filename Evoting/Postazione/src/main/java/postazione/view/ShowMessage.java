package postazione.view;

import java.io.FileInputStream;

import controller.CardReader;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import postazione.controller.Controller;
import utils.Constants;
import view.ViewAbstrController;

public class ShowMessage extends ViewAbstrController {
    @FXML private TextField txtRFID;
    @FXML private Label lblID, lblState, lblMessage, rfidStatusLabel, reqBadgeLabel, lastBadgeLabel;
    @FXML private SplitPane splitPane;
    @FXML private BorderPane leftLabelPane;
    @FXML private ImageView imgView;
    @FXML private Button resendVotesBtn;
    @FXML private HBox badgeStatusHBox;
    
    public void initialize() {
    	leftLabelPane.maxWidthProperty().bind(splitPane.widthProperty().multiply(0.55));
    	leftLabelPane.minWidthProperty().bind(splitPane.widthProperty().multiply(0.55));
    	
    	//Gestione della textfield per la lettura RFID: visibile solo se devMode = true
        showElement(txtRFID, Constants.devMode && !Constants.postRfid);
        showElement(rfidStatusLabel, false);
        showElement(badgeStatusHBox, false);
    }
    
    public void setInfos(String state, String message, String badge, String image, boolean resendVotes) {
        lblID.setText("Postazione");
        lblState.setText(state);
        lblMessage.setText(message);
        
        showElement(resendVotesBtn, resendVotes);
        showElement(imgView, image != null);
        
        if(state.equals("Associata")) {
        	showElement(rfidStatusLabel, true);
            showElement(badgeStatusHBox, true);
        	reqBadgeLabel.setText(badge);
        }
        
        if(image == null)
        	return;
        
        switch(image) {
        	case "error.png":
        		imgView.setFitHeight(150);
        		imgView.setFitWidth(150);
        		
        	case "rfid.png":
        		showImg(image);
        		break;
        		
    		default:
    			System.err.println("Immagine non riconosciuta.");
        }
    }
    
    public void setRfidReachable(boolean reachable) {
    	rfidStatusLabel.setText("Stato Lettore RFID: " + (reachable ? "Connesso" : "Non Raggiungibile"));
    	rfidStatusLabel.setStyle("-fx-background-color: " + (reachable ? "#a8f582;" : "#eb9a21;"));
    }
    
    public void updateLastWrongBadge(String badge) {
    	if(badge != null) {
    		badgeStatusHBox.setStyle("-fx-background-color: #ffd659;");
    		lastBadgeLabel.setText(badge);
    	}
    }
    
    @FXML
    private void resendVotes(ActionEvent event) {
    	((Controller) controller).resendVotes();
    }
    
    @FXML
    private void readRFID(ActionEvent actionEvent) {
        CardReader reader = ((Controller) controller).getCardReader();
        reader.write(txtRFID.getText());
        reader.endWrite();
        txtRFID.clear();
    }
    
    /**
     * Funzione che carica un'immagine da file system e, se riesce a trovarla, la mostra all'interno dell'ImageView presente nella scena.
     * @param imgName Nome ed estensione del file, da cercare all'interno di src/main/resources/img
     */
    private void showImg(String imgName) {
    	try {
    		FileInputStream inputstream = new FileInputStream("src/main/resources/img/"+imgName);
	    	Image image = new Image(inputstream); 
	    	imgView.setImage(image);
		} 
		catch (Exception e) { 
			e.printStackTrace(); 
		}
    }
}
