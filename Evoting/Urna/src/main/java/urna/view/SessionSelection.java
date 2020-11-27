package urna.view;

import java.util.ArrayList;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import model.Session;
import urna.controller.Controller;
import urna.view.viewmodel.SessionCallBack;
import urna.view.viewmodel.SessionViewModel;
import view.ViewAbstrController;

public class SessionSelection extends ViewAbstrController {
    @FXML private MenuItem logoutBtn;
    @FXML private TableView<SessionViewModel> sessionTable;
    @FXML private TableColumn<SessionViewModel, Integer> procCodCol;
    @FXML private TableColumn<SessionViewModel, String> procNameCol;
    @FXML private TableColumn<SessionViewModel, Integer> sessCodeCol;
    @FXML private TableColumn<SessionViewModel, String> startDateCol;
    @FXML private TableColumn<SessionViewModel, String> endDateCol;
    @FXML private TableColumn<SessionViewModel, String> validityCol;
    @FXML private TableColumn<SessionViewModel, CheckBox> selectCol;
    @FXML private Button confirmSessionBtn;
    
    public void initialize() {
    	sessionTable.setRowFactory(row -> new TableRow<SessionViewModel>() {
            @Override
            protected void updateItem(SessionViewModel s, boolean empty) {
                super.updateItem(s, empty);
                
                if (s == null) {
                    setStyle("");
                    return;
                }
                
                if (s.getValidity())
                	setStyle("-fx-background-color: #b5f2ae;");
                else
                	setStyle("-fx-background-color: #f59898;");
            }
        });
    	procCodCol.setCellValueFactory(new PropertyValueFactory<>("ProcedureCode"));
    	procNameCol.setCellValueFactory(new PropertyValueFactory<>("ProcedureName"));
    	sessCodeCol.setCellValueFactory(new PropertyValueFactory<>("Code"));
    	startDateCol.setCellValueFactory(new PropertyValueFactory<>("Start"));
    	endDateCol.setCellValueFactory(new PropertyValueFactory<>("End"));
    	validityCol.setCellValueFactory(new PropertyValueFactory<>("Validity"));
    	selectCol.setCellValueFactory(new SessionCallBack(this));
    }
    
    public void getSessions() {
    	ArrayList<Session> sessions = ((Controller) controller).getSessions();
    	
    	if (sessions == null)
    		return;
    	
    	ObservableList<SessionViewModel> sessionsData = FXCollections.observableArrayList();
    	for (Session s : sessions)
    		sessionsData.add(new SessionViewModel(s));
    	
    	sessionTable.setItems(sessionsData);
    }
    
    public void chooseSession(SessionViewModel session) {
    	boolean newValue = !session.isSelected();

        if(newValue)
            for(SessionViewModel s : sessionTable.getItems())
                s.setSelected(false);

        session.setSelected(newValue);

        sessionTable.getSelectionModel().clearSelection();
        sessionTable.refresh();
        
    	confirmSessionBtn.setDisable(!newValue);
    }
    
    @FXML
    void selectSessionFromTable(MouseEvent event) {
    	SessionViewModel session = sessionTable.getSelectionModel().getSelectedItem();

        if(session != null)
            chooseSession(session);
    }
    
    @FXML
    void logout(ActionEvent event) {
    	((Controller) controller).logout();
    }
    
    @FXML
    void confirmSession(ActionEvent event) {
    	int procCode = -1, sessionCode = -1;
    	
    	for (SessionViewModel s : sessionTable.getItems())
    		if (s.isSelected()) {
    			procCode = s.getProcedureCode();
    			sessionCode = s.getCode();
    			break;
    		}
    	
    	((Controller) controller).confirmSession(procCode, sessionCode);
    }
}
