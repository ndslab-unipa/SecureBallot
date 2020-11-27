package seggio.view;

import java.util.ArrayList;
import java.util.List;

import controller.CardReader;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
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
import model.EmptyBallot;
import model.Person;
import seggio.controller.Controller;
import seggio.model.Station;
import seggio.view.viewmodel.VoterCallBack;
import utils.Constants;
import view.ViewAbstrController;
import view.viewmodel.PostViewModel;
import view.viewmodel.VoterViewModel;

public class MainScene extends ViewAbstrController {
    @FXML private StackPane centerPane;
    @FXML private VBox mainVBox, postsVBox, registerVoterVBox, searchVoterVBox;
    @FXML private HBox recapRowOne, recapRowTwo, recapRowThree;
    @FXML private SplitPane postsVoterSplit;
    @FXML private TextField searchLastNameField, searchNameField, documentField, registerIdField, registerLastNameField, registerNameField, registerDateField, rfidField;
    @FXML private Label noVoterLabel, recapIdLabel, recapLastNameLabel, recapNameLabel, recapDateLabel, recapBallotsLabel, registerTitleLabel, registerLastNameLabel, registerNameLabel, registerDateLabel, rfidWarningLabel;
    @FXML private Button auxStatsBtn, registerUserBtn, updateUserBtn, searchUserBtn, searchGoBackBtn, searchConfirmBtn, registerGoBackBtn, registerConfirmBtn, updateConfirmBtn, resetVoterBtn;
    @FXML private GridPane ballotsGridPane;
    @FXML private ComboBox<String> selectDocumentTypeBox;
    
    @FXML private TableView<PostViewModel> postsTable;
    @FXML private TableColumn<PostViewModel, Integer> nPostCol;
    @FXML private TableColumn<PostViewModel, String> statePostCol;
    @FXML private TableColumn<PostViewModel, ?> voterCol;
    @FXML private TableColumn<PostViewModel, String> voterIdCol, voterLastNameCol, voterNameCol;
    @FXML private TableColumn<PostViewModel, String> badgeCol;
    @FXML private TableColumn<PostViewModel, PostViewModel> resetPostCol;

    @FXML private TableView<VoterViewModel> searchVoterTable;
    @FXML private TableColumn<VoterViewModel, String> searchVoterIdCol, searchVoterLastNameCol, searchVoterNameCol, searchVoterDateCol;
    @FXML private TableColumn<VoterViewModel, CheckBox> searchSelectCol;
    
    private Station station;
    
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
        
        resetPostCol.setCellValueFactory(
            param -> new ReadOnlyObjectWrapper<>(param.getValue())
        );
        
        resetPostCol.setCellFactory(param -> new TableCell<PostViewModel, PostViewModel>() {
            private final Button deleteButton = new Button("Reset");

            @Override
            protected void updateItem(PostViewModel pvm, boolean empty) {
                super.updateItem(pvm, empty);
                
                if(pvm == null) {
                	setGraphic(null);
                	return;
                }
                
                if(List.of("OFFLINE", "DA RIAVVIARE").contains(pvm.getState())) {
                	setGraphic(null);
                	return;
                }
                
                if(!Constants.devMode && !pvm.getState().equals("DA RESETTARE")) {
                	setGraphic(null);
                	return;
                }
                
                setGraphic(deleteButton);
                deleteButton.setOnAction(event -> {
                	((Controller) controller).resetPost(pvm.getID()-1);
                });
            }
        });
        
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
        registerNameField.setTextFormatter(new TextFormatter<>(noDigitsFilter));
        registerLastNameField.setTextFormatter(new TextFormatter<>(noDigitsFilter));
        registerDateField.setTextFormatter(new TextFormatter<>(dateFilter));
        
        //Textfield RFID visibili solo se in devMode
        showElement(rfidField, Constants.devMode && !Constants.statRfid);
        showElement(rfidWarningLabel, false);
    }
    
    /**
     * Funzione richiamata dalla classe {@link seggio.view.View View}, che popola la tabella delle postazioni con le informazioni aggiornate.
     */
    public void updatePostsInfo() {
    	if (station == null)
    		return;

        DummyPost[] posts = station.getPosts();

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
     * @see seggio.view.MainScene#showDocument(ActionEvent) showDocument(ActionEvent)
     * @see seggio.view.MainScene#showVoterRecap(boolean) showVoterRecap(boolean)
     */
    public void hideNewVoter() {
    	resetVoter(null);
    }
    
    public void toggleWarningLabel(boolean flag) {
    	showElement(rfidWarningLabel, flag);
    }
    
    /**
     * Funzione richiamata al caricamento della scena. Permette alla View di conoscere il Model ({@link seggio.model.Station Station}), 
     * e popola la tabella delle postazioni.
     */
    public void initStation() {
    	station = ((Controller) controller).getStation();
    	updatePostsInfo();
    	
    	EmptyBallot[] ballots = station.getEmptyBallots();
    	for (int i=0; i<ballots.length; i++) {
    		CheckBox cb = new CheckBox(ballots[i].getTitle());
    		ballotsGridPane.add(cb, i%3, i / 3);
    	}
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
    
    @FXML
    void activateAuxStats(ActionEvent event) {
    	((Controller) controller).updateSubStations();
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
    	
    	if (votersList == null)
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
     * l'elenco dei votanti trovati; chiamata indirettamente alla fine della funzione {@link seggio.view.MainScene#searchConfirm(ActionEvent) searchConfirm}.
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    void searchGoBack(ActionEvent event) {
    	searchVoterTable.getItems().clear();
    	
    	mainVBox.setVisible(true);
    	searchVoterVBox.setVisible(false);
    }
    
    /**
     * Funzione che permette di confermare la scelta effettuata sul votante da associare. Aggiorna opportunamente il Model e reinizializza la riga relativa
     * agli estremi del documento da fornire. Alla fine, richiama la funzione {@link seggio.view.MainScene#searchGoBack(ActionEvent) searchGoBack}
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
     * Funzione che permette di rimuovere un votante precedentemente selezionato. Aggiorna il Model e richiama la funzione {@link seggio.view.MainScene#hideNewVoter() resetVoter}
     * per gestire l'aggiornamento degli elementi grafici nella scena.
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    void resetVoter(ActionEvent event) {
    	if(event != null)
    		station.setNewVoter(null);
    	
    	resetDocumentRow();
    	showVoterRecap(false);
    }
    
    /**
     * Funzione che gestisce il cambio di schermata, per visualizzare la schermata di registrazione di un nuovo utente. Pre-Compila le TextField
     * corrispondenti a nome e cognome coi corrispondenti valori presenti nelle TextField di ricerca e gestisce la visibilità dei contenuti. E' richiamata
     * cliccando sul bottone "Registra Nuovo" nella schermata principale.
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    void registerUser(ActionEvent event) {
    	showRegisterPanel();
    	
    	registerTitleLabel.setText("Registra Nuovo Votante");
    	showRegisterFullData(true);
    }
    
    @FXML
    void updateUser(ActionEvent event) {
    	showRegisterPanel();
    	
    	registerTitleLabel.setText("Aggiorna Votante Esistente");
    	showRegisterFullData(false);
    }
    
    /**
     * Funzione che permette di tornare alla schermata principale, dalla schermata di registrazione di un nuovo utente. E' richiamata cliccando sul
     * bottone "Torna Indietro" nella schermata di registrazione.
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    void registerGoBack(ActionEvent event) {
    	if (event == null)
    		resetVoter(null);
    	
    	this.mainVBox.setVisible(true);
    	this.registerVoterVBox.setVisible(false);
    }

    @FXML
    void updateConfirm(ActionEvent event){
        String userId = registerIdField.getText();
        int[] selectedBallots = getSelectedBallotsCodes();

        Person voter = ((Controller) controller).updateExistingUser(userId, selectedBallots);
        if (voter != null) {
            searchLastNameField.setText(voter.getLastName());
            searchNameField.setText(voter.getFirstName());

            registerGoBack(null);
        }
    }

    /**
     * Funzione che permette di effettuare la registrazione di un nuovo utente, richiamata cliccando sul bottone "Conferma Registrazione". Recupera
     * i dati inseriti nelle diverse TextField, e gli indici delle schede selezionate tramite CheckBox, e passa i dati al Controller, che si occuperà di
     * controllare i valori ed, eventualmente, inviare gli stessi all'Urna. Se la registrazione va a buon fine, riporta alla schermata principale,
     * compilando i campi di ricerca con Nome e Cognome dell'utente appena registrato.
     * @see {@link seggio.controller.Controller#registerNewUser(String, String, String, String, int[]) Controller#registerNewUser()}
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    void registerConfirm(ActionEvent event) {
    	String userId = this.registerIdField.getText();
    	String userLn = this.registerLastNameField.getText();
    	String userN = this.registerNameField.getText();
    	String userBirth = this.registerDateField.getText();
    	int[] selectedBallots = getSelectedBallotsCodes();
    	
    	if (((Controller) controller).registerNewUser(userId, userLn, userN, userBirth, selectedBallots)) {
    		searchLastNameField.setText(this.registerLastNameField.getText());
        	searchNameField.setText(this.registerNameField.getText());
        	
        	registerGoBack(null);
    	}
    }

    /**
     * Funzione che gestisce il click del mouse sulla tabella che mostra i votanti corrispondenti ai criteri cercati. Recupera il modello dei dati
     * corrispondente alla riga selezionata e lo passa alla funzione {@link seggio.view.MainScene#selectVoter(VoterViewModel) selectVoter}.
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    void selectVoterFromTable(MouseEvent event) {
    	VoterViewModel voter = searchVoterTable.getSelectionModel().getSelectedItem();

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
    		station.setDocumentType(selectedDocument);
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
    		station.setDocumentID(this.documentField.getText());
    }
    
    /**
     * Funzione di utility che rimuove la selezione da tutte le checkbox corrispondenti alle schede nella schermata di registrazione utente. Richiamata
     * ogni volta che si accede alla schermata stessa.
     */
    private void resetBallotsCheckBoxes() {
    	for (Node n : ballotsGridPane.getChildren())
    		((CheckBox) n).setSelected(false);
    }
    
    /**
     * Funzione utilizzata per ottenere il nodo del GridPane utilizzando i suoi indici di riga e di colonna.
     * @param children Lista dei nodi all'interno del GridPane
     * @param x Indice di riga
     * @param y Indice di colonna
     * @return Il nodo alle coordinate (x,y), o null
     */
    private static Node getChildAt(ObservableList<Node> children, int x, int y) {
    	for (Node n : children)
    		if (GridPane.getRowIndex(n) == x && GridPane.getColumnIndex(n) == y)
    			return n;
    	
    	return null;
    }
    
    private int[] getSelectedBallotsCodes() {
    	ObservableList<Node> gridPaneChildren = ballotsGridPane.getChildren();
    	EmptyBallot[] emptyBallots = station.getEmptyBallots();
    	
    	ArrayList<Integer> selectedList = new ArrayList<>();
    	for (int x = 0; x < ballotsGridPane.getRowCount(); x++) 
    		for (int y = 0; y < ballotsGridPane.getColumnCount(); y++) {
    			Node n = getChildAt(gridPaneChildren, x, y);
    			
    			if (n != null) {
    				if (((CheckBox) n).isSelected())
    					selectedList.add(3 * x + y);
    			}
    		}

    	int[] selected = new int[selectedList.size()];
    	
    	for (int i=0; i<selectedList.size(); i++) 
    		selected[i] = emptyBallots[selectedList.get(i)].getCode();
    	
    	return selected;
    }
    
    private void showRegisterPanel() {
    	registerIdField.setText(null);
    	registerLastNameField.setText(searchLastNameField.getText());
    	registerNameField.setText(searchNameField.getText());
    	registerDateField.setText(null);
    	resetBallotsCheckBoxes();
    	
    	mainVBox.setVisible(false);
    	registerVoterVBox.setVisible(true);
    }
    
    private void showRegisterFullData(boolean flag) {
    	showElement(registerLastNameLabel, flag);
    	showElement(registerLastNameField, flag);
    	
    	showElement(registerNameLabel, flag);
    	showElement(registerNameField, flag);
    	
    	showElement(registerDateLabel, flag);
    	showElement(registerDateField, flag);
    	
    	showElement(registerConfirmBtn, flag);
    	showElement(updateConfirmBtn, !flag);
    }
    
    private void showNewVoter() {
    	Person voter = station.getNewVoter();
    	
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
    
    /**
     * Funzione che resetta la riga contenente gli estremi del documento. Mostra il TextField dopo averne reinizializzato il contenuto e rimuovere l'elemento
     * precedentemente selezionato dalla ComboBox. Aggiorna il Model, reinizializzando il tipo di documento e l'ID correnti.
     */
    private void resetDocumentRow() {
        selectDocumentTypeBox.getSelectionModel().clearSelection();

    	documentField.setVisible(false);
    	documentField.setText("");
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
}