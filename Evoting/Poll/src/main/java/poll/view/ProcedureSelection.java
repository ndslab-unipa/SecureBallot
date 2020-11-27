package poll.view;

import java.util.ArrayList;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import model.Procedure;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import poll.controller.Controller;
import poll.view.viewmodel.ProcedureCallBack;
import poll.view.viewmodel.ProcedureViewModel;
import view.ViewAbstrController;

/**
 * Classe che gestisce la scena <i>procedure-selection.fxml</i>. Contiene i riferimenti a tutti i nodi JavaFX dichiarati 
 * nella scena e gestisce tutte le funzioni richieste. Estende {@link view.ViewAbstrController ViewAbstrController}.
 */
public class ProcedureSelection extends ViewAbstrController {
    @FXML private MenuItem logoutMenu;
    @FXML private TableView<ProcedureViewModel> proceduresTable;
    @FXML private TableColumn<ProcedureViewModel, Integer> procCodCol;
    @FXML private TableColumn<ProcedureViewModel, String> procNameCol;
    @FXML private TableColumn<ProcedureViewModel, String> startDateCol;
    @FXML private TableColumn<ProcedureViewModel, String> endDateCol;
    @FXML private TableColumn<ProcedureViewModel, String> terminatedCol;
    @FXML private TableColumn<ProcedureViewModel, CheckBox> selectCol;
    @FXML private Button confirmProcedureBtn;
    
    /**
     * Funzione richiamata automaticamente da FXMLLoader al caricamento della scena, subito dopo il costruttore di default.
     * <br>
     * E' utilizzata per inizializzare la tabella delle procedure: mappa gli attributi di {@link ProcedureViewModel}
     * sulle corrispondenti colonne della tabella e configura la tabella in modo che mostri in verde le righe relative a procedure 
     * terminate, su cui è possibile effettuare lo spoglio, ed in rosso quelle ancora attive, su cui lo spoglio non può essere effettuato.
     */
    public void initialize() {
    	proceduresTable.setRowFactory(row -> new TableRow<ProcedureViewModel>() {
            @Override
            protected void updateItem(ProcedureViewModel p, boolean empty) {
                super.updateItem(p, empty);
                
                if (p == null) {
                    setStyle("");
                    return;
                }
                
                if (p.getTerminated())
                	setStyle("-fx-background-color: #b5f2ae;");
                else
                	setStyle("-fx-background-color: #f59898;");
            }
        });
    	procCodCol.setCellValueFactory(new PropertyValueFactory<>("Code"));
    	procNameCol.setCellValueFactory(new PropertyValueFactory<>("Name"));
    	startDateCol.setCellValueFactory(new PropertyValueFactory<>("Start"));
    	endDateCol.setCellValueFactory(new PropertyValueFactory<>("End"));
    	terminatedCol.setCellValueFactory(new PropertyValueFactory<>("Terminated"));
    	selectCol.setCellValueFactory(new ProcedureCallBack(this));
    }
    
    
    /**
     * Permette alla scena di ricevere l'elenco di procedure dal controller (tramite {@link Controller#getProcedures()}) e di mostrarle
     * nella tabella. Se vi è almeno una procedura, rende visibile la tabella delle procedure e disabilita il bottone per recuperare le procedure.
     * <br>
     * E' richiamata subito dopo il caricamento della scena. 
     */
    public void getProcedures() {
    	ArrayList<Procedure> procedures = ((Controller) controller).getProcedures();
    	
    	if (procedures == null)
    		return;
    	
    	ObservableList<ProcedureViewModel> proceduresData = FXCollections.observableArrayList();
    	for (Procedure p : procedures)
    		proceduresData.add(new ProcedureViewModel(p));
    	
    	proceduresTable.setItems(proceduresData);
    }
    
    /**
     * Permette di selezionare/deselezionare una procedura, a seconda della riga da cui è richiamata. Se la riga era selezionata, procede a deselezionarla,
     * altrimenti la seleziona e deselezione tutte le altre righe.
     * <br>
     * Ogni volta in cui è richiamata, inoltre, gestisce il bottone di conferma procedura, in modo che sia abilitato solo se vi è una
     * procedura selezionata.
     * <br>
     * E' richiamata cliccando in un qualunque punto della riga la cui struttura dati è passata a parametro.
     * @param rowModel Riferimento alla struttura dati corrispondente alla riga selezionata
     */
    public void chooseProcedure(ProcedureViewModel rowModel) {
    	boolean newValue = !rowModel.isSelected();

        if(newValue)
            for(ProcedureViewModel p : proceduresTable.getItems())
                p.setSelected(false);

        rowModel.setSelected(newValue);
        proceduresTable.getSelectionModel().clearSelection();
        proceduresTable.refresh();
        
        confirmProcedureBtn.setDisable(!newValue);
    }
    
    /**
     * Gestisce i click sulla tabella delle procedure. Se il click avviene su una riga (quindi se una riga è stata selezionata)
     * richiama {@link #chooseProcedure(ProcedureViewModel)}.
     * <br>
     * E' richiamata ogni volta che avviene un click del mouse su una qualunque parte della tabella delle procedure.
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato. 
     */
    @FXML
    private void selectProcedureFromTable(MouseEvent event) {
    	ProcedureViewModel proc = proceduresTable.getSelectionModel().getSelectedItem();

        if(proc != null)
            chooseProcedure(proc);
    }

    /**
     * Funzione richiamata dal click sul menù File &gt; Logout. Invoca la funzione {@link Controller#logout()} per gestire il logout.
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    private void logout(ActionEvent event) {
    	((Controller) controller).logout();
    }
    
    /**
     * Gestisce la conferma della scelta di una procedura invocando la funzione {@link Controller#confirmProcedure(int)} col codice
     * della procedura selezionata (o -1).
     * <br>
     * E' richiamata dal click sul bottone "Conferma Procedura"
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    private void confirmProcedure(ActionEvent event) {
    	int procCode = -1;
    	
    	for(ProcedureViewModel p : proceduresTable.getItems())
    		if (p.isSelected()) {
    			procCode = p.getCode();
    			break;
    		}
    	
		((Controller) controller).confirmProcedure(procCode);
    }
}