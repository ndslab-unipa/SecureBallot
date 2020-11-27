package urna.view;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import urna.controller.Controller;
import view.ViewAbstrController;

public class Login extends ViewAbstrController {
    @FXML private TextField userField;
    @FXML private PasswordField pswField;
    @FXML private Button loginBtn;

    @FXML
    private void checkKey(KeyEvent event) {
    	if (event.getCode().equals(KeyCode.ENTER))
    		checkLogin(null);
    }
    
    @FXML
    private void checkLogin(ActionEvent event) {
    	String user = userField.getText(), psw = pswField.getText();
    	((Controller) controller).checkLogin(user, psw);
    }

}
