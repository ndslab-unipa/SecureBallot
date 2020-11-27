package urna.view;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import model.Terminals;
import urna.controller.Controller;
import utils.Constants;
import view.ViewAbstrController;

public class MainScene extends ViewAbstrController {
	 @FXML private ScrollPane scroll;
	 @FXML private VBox logVBox;
	 @FXML private Menu developerMenu;
	 @FXML private MenuItem closeSessionMenu, logoutMenu, statsMenu;
	 
	 public void initialize() {
		 scroll.vvalueProperty().bind(logVBox.heightProperty());
		 developerMenu.setVisible(Constants.devMode);
	 }
	 
	 public void printLogs() {
		 for (String log : ((Controller) controller).getLogs())
			 logEvent(log);
	 }
	 
	 private void logEvent(String event) {
		 Text txt = new Text(event);
		 txt.setTextAlignment(TextAlignment.JUSTIFY);
		 txt.wrappingWidthProperty().bind(scroll.widthProperty().subtract(20));
		 
		 if (event.contains("[SUCCESS]"))
			 txt.setFill(Color.DARKGREEN);
		 
		 if (event.contains("[ERROR]"))
			 txt.setFill(Color.DARKRED);
		 
		 if (event.contains("[WARNING]"))
			 txt.setFill(Color.CORAL);
		 
		 logVBox.getChildren().add(txt);
	 }
	 
	 @FXML
	 void closeSession(ActionEvent event) {
		 ((Controller) controller).closeSession();
	 }

	 @FXML
	 void logout(ActionEvent event) {
		 ((Controller) controller).logout();
	 }
	 
	 @FXML
	 void showStats(ActionEvent event) {
		 ((Controller) controller).showStats();
	 }
	 
	 @FXML
	 void deactivateStation(ActionEvent event) {
		 deactivateTerminal(Terminals.Type.Station);
	 }
	 
	 @FXML
	 void deactivateSubStation(ActionEvent event) {
		 deactivateTerminal(Terminals.Type.SubStation);
	 }
	 
	 @FXML
	 void deactivatePost(ActionEvent event) {
		 deactivateTerminal(Terminals.Type.Post);
	 }
	 
	 private void deactivateTerminal(Terminals.Type type) {
		 ((Controller) controller).deactivateTerminal(type);
	 }
}
