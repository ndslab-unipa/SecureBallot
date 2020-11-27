package seggio.aux.view;

import controller.CardReader;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.DummyPost;
import model.Person;
import seggio.aux.controller.Controller;
import seggio.aux.model.SeggioAusiliario;
import seggio.aux.view.viewmodel.VoterCallBack;
import utils.Constants;
import view.ViewAbstrController;
import view.viewmodel.PostViewModel;
import view.viewmodel.VoterViewModel;

public class MainScene extends ViewAbstrController {
    @FXML private StackPane centerPane;
    @FXML private VBox mainVBox, postsVBox, searchVoterVBox;
    @FXML private HBox recapRowOne, recapRowTwo, recapRowThree;
    @FXML private SplitPane postsVoterSplit;
    @FXML private TextField searchLastNameField, searchNameField, documentField, rfidField;
    @FXML private Label noVoterLabel, recapIdLabel, recapLastNameLabel, recapNameLabel, recapDateLabel, recapBallotsLabel, rfidWarningLabel;
    @FXML private Button searchUserBtn, searchGoBackBtn, searchConfirmBtn, resetVoterBtn;
    @FXML private GridPane ballotsGridPane;
    @FXML private ComboBox<String> selectDocumentTypeBox;
    
    @FXML private TableView<PostViewModel> postsTable;
    @FXML private TableColumn<PostViewModel, Integer> nPostCol;
    @FXML private TableColumn<PostViewModel, String> statePostCol;
    @FXML private TableColumn<PostViewModel, ?> voterCol;
    @FXML private TableColumn<PostViewModel, String> voterIdCol, voterLastNameCol, voterNameCol;
    @FXML private TableColumn<PostViewModel, String> badgeCol;

    @FXML private TableView<VoterViewModel> searchVoterTable;
    @FXML private TableColumn<VoterViewModel, String> searchVoterIdCol, searchVoterLastNameCol, searchVoterNameCol, searchVoterDateCol;
    @FXML private TableColumn<VoterViewModel, CheckBox> searchSelectCol;
    
    private SeggioAusiliario substation;
    
    /**
     * Funzione richiamata dall'FXML Loader subito dopo il costruttore default. Utilizzata per settare le proprietà delle tabelle
     * e gli elementi da visualizzare nella ComboBox.
     */
    public void initialize() {
    	postsVBox.maxWidthProperty().bind(postsVoterSplit.widthProperty().multiply(0.65));
    	postsVBox.minWidthProperty().bind(postsVoterSplit.widthProperty().multiply(0.65));
    	
    	//Tabella Postazioni
    	postsTable.setRowFactory(row -> new TableRow<PostViewModel>() {
            @Override
            protected void updateItem(PostViewModel post, boolean empty) {
            	super.updateItem(post, empty);
                
                if (post == null) {
                    setStyle("");
                    return;
                }
                
                if (!post.getState().equals("OFFLINE") && post.isUnreachable() ) {
                	setStyle("-fx-background-color: #fcbc8d;");
                	return;
                }
                
                switch(post.getState()) {
	            	case "NON ATTIVA":
	            		setStyle("-fx-background-color: #eff2aa;");
	            		break;
	            		
	            	case "ATTIVA":
	            		setStyle("-fx-background-color: #b5f2ae;");
	            		break;
	            		
	            	case "ASSOCIATA":
	            	case "IN USO":
	            		setStyle("-fx-background-color: #2eab20;");
	            		break;
	            		
	            	case "VOTO PENDING":
	            		setStyle("-fx-background-color: #a7bcc2;");
	            		break;
	            		
	            	case "VOTO INVIATO":
	            		setStyle("-fx-background-color: #c7e7f0;");
	            		break;
	            		
	            	case "DA RESETTARE":
	            		setStyle("-fx-background-color: #ff9494;");
	            		break;	
	            		
	            	case "DA RIAVVIARE":
	            		setStyle("-fx-background-color: #b57979;");
	            		break;
	            		
	        		default:
	        			setStyle(null);
	            }
            }
        });
    	nPostCol.setCellValueFactory(new PropertyValueFactory<PostViewModel,Integer>("ID"));
    	statePostCol.setCellValueFactory(new PropertyValueFactory<PostViewModel, String>("state"));
    	voterIdCol.setCellValueFactory(new PropertyValueFactory<PostViewModel, String>("voterID"));
        voterLastNameCol.setCellValueFactory(new PropertyValueFactory<PostViewModel, String>("voterLastName"));
        voterNameCol.setCellValueFactory(new PropertyValueFactory<PostViewModel, String>("voterFirstName"));
        badgeCol.setCellValueFactory(new PropertyValueFactory<PostViewModel, String>("badge"));
        
        //Tabella Cerca Votante
        searchVoterTable.setRowFactory(row -> new TableRow<VoterViewModel>() {
            @Override
            protected void updateItem(VoterViewModel item, boolean empty) {
                super.updateItem(item, empty);
                
                if (item == null) {
                    setStyle("");
                    return;
                }
                
                switch(item.getVoteInfo()) {
                	case "cant-vote":
                		setStyle("-fx-background-color: #fcb103;");
                		break;
                		
                	case "no-ballots":
                		setStyle("-fx-background-color: #fc8003;");
                		break;
                	
            		default:
            			setStyle(null);
                }
            }
        });
        searchVoterIdCol.setCellValueFactory(new PropertyValueFactory<>("ID"));
        searchVoterLastNameCol.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        searchVoterNameCol.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        searchVoterDateCol.setCellValueFactory(new PropertyValueFactory<>("birth"));
        searchSelectCol.setCellValueFactory(new VoterCallBack(this));
        
        //Elementi della ComboBox (Tipi di Documento)
        selectDocumentTypeBox.getItems().addAll("Carta d'Identità", "Patente", "Passaporto", "Conoscenza Personale");
        
        //Filtri sulle TextField
        searchNameField.setTextFormatter(new TextFormatter<>(noDigitsFilter));
        searchLastNameField.setTextFormatter(new TextFormatter<>(noDigitsFilter));
        
        //Textfield RFID visibili solo se in devMode
        showElement(rfidField, Constants.devMode && !Constants.auxStatRfid);
        showElement(rfidWarningLabel, false);
    }
    
    /**
     * Funzione richiamata dalla classe {@link seggio.aux.view.View View}, che popola la tabella delle postazioni con le informazioni aggiornate.
     */
    public void updatePostsInfo() {
    	if (substation == null)
    		return;

        DummyPost[] posts = substation.getPosts();

        if(posts == null)
            return;

        ObservableList<PostViewModel> postsData = FXCollections.observableArrayList();

        for(DummyPost p : posts)
            postsData.add(new PostViewModel(p));

        postsTable.setItems(postsData);
    }
    
    /**
     * Funzione che gestisce la selezione di un votante dalla tabella di ricerca degli stessi. Richiamata sia dal click su una delle checkbox che dal click
     * su una qualunque riga. Data la possibilità di selezionare una sola riga, se vi sono altre righe già selezionate le deseleziona.
     * @param voter Istanza della classe {@link view.viewmodel.VoterViewModel VoterViewModel} corrispondente al modello di dati della riga selezionata
     */
    public void selectVoter(VoterViewModel voter) {
        boolean newValue = !voter.isSelected();

        if(newValue)
            for(VoterViewModel v : searchVoterTable.getItems())
                v.setSelected(false);

        voter.setSelected(newValue);

        searchVoterTable.getSelectionModel().clearSelection();
        searchVoterTable.refresh();
    }
    
    /**
     * Funzione che resetta le righe contenenti il riepilogo del votante selezionato.
     * @see seggio.aux.view.MainScene#showDocument(ActionEvent) showDocument(ActionEvent)
     * @see seggio.aux.view.MainScene#showVoterRecap(boolean) showVoterRecap(boolean)
     */
    public void hideNewVoter() {
    	resetVoter(null);
    }
    
    public void toggleWarningLabel(boolean flag) {
    	showElement(rfidWarningLabel, flag);
    }
    
    /**
     * Funzione richiamata al caricamento della scena. Permette alla View di conoscere il Model ({@link seggio.aux.model.SeggioAusiliario SeggioAusiliario}), 
     * e popola la tabella delle postazioni.
     */
    public void initSubStation() {
    	substation = ((Controller) controller).getSubStation();
    	updatePostsInfo();
    }
    
    /**
     * Funzione temporanea (verrà rimossa una volta integrato il vero lettore RFID) che permette di leggere dal TextField contenente l'ID del badge RFID da leggere.
     * Richiama il reader del controller per gestire la lettura del badge.
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    void readRFID(ActionEvent event) {
		CardReader reader = ((Controller) controller).getCardReader();
        reader.write(rfidField.getText());
        reader.endWrite();
        rfidField.clear();
    }
    
    /**
     * Funzione che permette di effettuare la ricerca nel DB dei votanti che corrispondono ai criteri indicati nei TextField corrispondenti.
     * Nasconde la schermata principale per mostrare la tabella dei risultati trovati.
     * Viene richiamata sia dal click sul bottone "Cerca" che dalla pressione del tasto "Invio" durante la scrittura in una delle due TextField.
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    void searchUser(ActionEvent event) {
    	Person[] votersList = ((Controller) controller).retrieveVotersByName(searchNameField.getText(), searchLastNameField.getText());
    	
    	if(votersList == null)
    		return;
    	
    	ObservableList<VoterViewModel> voterData = FXCollections.observableArrayList();

        for(Person voter : votersList)
            voterData.add(new VoterViewModel(voter));

        searchVoterTable.setItems(voterData);
        
        mainVBox.setVisible(false);
        searchVoterVBox.setVisible(true);
    }
    
    /**
     * Funzione che permette di tornare indietro, dalla schermata contenente l'elenco dei votanti trovati alla schermata principale del Seggio.
     * Se è stato selezionato un votante, allora aggiorna le Label di riepilogo delle informazioni sul votante selezionato. Inoltre, gestisce la
     * visibilità degli elementi associati al riepilogo stesso. Chiamata direttamente dal click sul pulsante "Torna Indietro" nella schermata contenente
     * l'elenco dei votanti trovati; chiamata indirettamente alla fine della funzione {@link seggio.aux.view.MainScene#searchConfirm(ActionEvent) searchConfirm}.
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    void searchGoBack(ActionEvent event) {
    	searchVoterTable.getItems().clear();
    	
    	this.mainVBox.setVisible(true);
    	this.searchVoterVBox.setVisible(false);
    }
    
    /**
     * Funzione che permette di confermare la scelta effettuata sul votante da associare. Aggiorna opportunamente il Model e reinizializza la riga relativa
     * agli estremi del documento da fornire. Alla fine, richiama la funzione {@link seggio.aux.view.MainScene#searchGoBack(ActionEvent) searchGoBack}
     * per gestire il ritorno alla schermata precedente. E' richiamata dal click sul pulsante "Conferma Selezione" nella schermata contenente l'elenco dei votanti trovati. 
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    void searchConfirm(ActionEvent event) {
    	VoterViewModel selectedVoterModel = null;
    	
        for(VoterViewModel v : searchVoterTable.getItems())
            if(v.isSelected()) {
                selectedVoterModel = v;
                break;
            }
        
        if(((Controller) controller).setNewVoter(selectedVoterModel)) {
        	showNewVoter();
            searchGoBack(null);
        }
    }
    
    /**
     * Funzione che permette di rimuovere un votante precedentemente selezionato. Aggiorna il Model e richiama la funzione {@link seggio.aux.view.MainScene#hideNewVoter() resetVoter}
     * per gestire l'aggiornamento degli elementi grafici nella scena.
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    void resetVoter(ActionEvent event) {
    	if(event != null)
    		substation.setNewVoter(null);
    	
    	resetDocumentRow();
    	showVoterRecap(false);
    }
    
    /**
     * Funzione che gestisce il click del mouse sulla tabella che mostra i votanti corrispondenti ai criteri cercati. Recupera il modello dei dati
     * corrispondente alla riga selezionata e lo passa alla funzione {@link seggio.aux.view.MainScene#selectVoter(VoterViewModel) selectVoter}.
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    void selectVoterFromTable(MouseEvent event) {
    	VoterViewModel voter = (VoterViewModel) searchVoterTable.getSelectionModel().getSelectedItem();

        if(voter != null)
            selectVoter(voter);
    }

    /**
     * Funzione richiamata dall'interazione con la ComboBox relativa ai tipi di documento selezionabili. Gestisce la visibilità del TextField per inserire
     * l'ID del documento sulla base del tipo di documento selezionato. Aggiorna il model col nuovo tipo di documento selezionato.
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    void showDocument(ActionEvent event) {
    	String selectedDocument = selectDocumentTypeBox.getSelectionModel().getSelectedItem();
    	
    	if (selectedDocument != null) {
    		substation.setDocumentType(selectedDocument);
    		this.documentField.setVisible(!selectedDocument.equals("Conoscenza Personale"));
    	}
    }
    
    /**
     * Funzione che aggiorna l'ID del documento tenuto dal Model con quanto scritto nel TextField corrispondente. Richiamato ad ogni tasto digitato
     * nel TextField.
     * @param event Evento associato alla chiamata a questa funzione. Se il suo codice è uguale a quello del tasto "Invio" allora la funzione ritorna senza
     * effettuare modifiche.
     */
    @FXML
    void updateDocumentID(KeyEvent event) {
    	if (!event.getCode().equals(KeyCode.ENTER))
    		substation.setDocumentID(this.documentField.getText());
    }
    
    /**
     * Funzione che gestisce la visibilità degli elementi relativi al riepilogo dei dati dell'utente selezionato. A seconda del parametro, fa apparire la scritta
     * "(Nessun Votante Selezionato)" e sparire le righe col riepilogo dei dati, o viceversa.
     * @param flag booleano utilizzato per la gestione della visibilità degli elementi. Se true, mostra i dati dell'utente selezionato. Se false, mostra il messaggio
     * di assenza di votanti selezionati.
     */
    private void showVoterRecap(boolean flag) {
    	showElement(noVoterLabel, !flag);
    	
    	showElement(recapRowOne, flag);
    	showElement(recapRowTwo, flag);
    	showElement(recapRowThree, flag);
    }
    
    /**
     * Funzione che resetta la riga contenente gli estremi del documento. Mostra il TextField dopo averne reinizializzato il contenuto e rimuovere l'elemento
     * precedentemente selezionato dalla ComboBox. Aggiorna il Model, reinizializzando il tipo di documento e l'ID correnti.
     */
    private void resetDocumentRow() {
    	selectDocumentTypeBox.getSelectionModel().clearSelection();

    	documentField.setVisible(false);
    	documentField.setText("");
    }
    
    private void showNewVoter() {
    	Person voter = substation.getNewVoter();
    	
    	if (voter != null) {
    		recapIdLabel.setText(voter.getID());
        	recapLastNameLabel.setText(voter.getLastName());
    		recapNameLabel.setText(voter.getFirstName());
    		recapDateLabel.setText(voter.getBirth());
    		recapBallotsLabel.setText(voter.getBallotCodesString());
    		
    		showVoterRecap(true);
    		resetDocumentRow();
    	}
    }
}
