package postazione.view;

import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import postazione.controller.Controller;
import view.ViewAbstrController;

public class Activation extends ViewAbstrController {
    @FXML private TextField sessKeyField;
    @FXML private Button sessKeyBtn;
    @FXML private Label welcomeLabel;

    @FXML
    private void initialize() {
    	sessKeyField.setTextFormatter(new TextFormatter<>((change) -> {
    		String editedText = change.getText().toUpperCase().replaceAll("[^a-zA-Z0-9]", "");
    	    change.setText(editedText);
    	    return change;
    	}));
    }

    @FXML
    private void checkKey(KeyEvent event) {
        if (event.getCode().equals(KeyCode.ENTER))
            checkSessionKey(null);
    }

    @FXML
    private void checkSessionKey(ActionEvent event) {
        String sessKey = sessKeyField.getText();
        ((Controller) controller).activate(sessKey);
    }
}
