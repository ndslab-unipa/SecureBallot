package procmgr.view;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import procmgr.controller.Controller;
import utils.Constants;
import view.ViewAbstrController;

import java.io.File;

public class Technic extends ViewAbstrController {
    @FXML private TabPane tabPane;
    @FXML private Label accessLabel;
	private Stage stage;

    /********************************************
     * Campi relativi alle funzionalità di root.*
     ********************************************/

    @FXML private Tab rootTab;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField, repeatPasswordField;
    @FXML private ChoiceBox<String> userTypeChoiceBox;

    /************************************************
     * Campi relativi alle funzionalità dei tecnici.*
     ************************************************/

    //Le 2 schermate di creazione procedura
    @FXML private VBox initialParametersVBox, fileLoaderVBox;

    //I textfield relativi ai parametri iniziali della procedura
    @FXML private TextField procedureNameTxt, startDayTxt, startTimeTxt, endDayTxt, endTimeTxt, ballotsNumTxt;

    /*Campi relativi alla seconda schermata della creazione procedura*/

    //I textfield relativi ai nomi dei file csv per la creazione della procedura
    @FXML private TextField sessionsFileTxtField, candidatesFileTxtField, ballotsFileTxtField, votersFileTxtField;

    //I bottoni che attivano il filechooser per il caricamento dei file csv necessari alla creazione della procedura
    @FXML private Button sessionsFileChooser, candidatesFileChooser, ballotsFileChooser, votersFileChooser;

    //Choicebox per la selezione del supervisore responsabile della procedura.
    @FXML private ChoiceBox<String> supervisorChoiceBox;

    void setStage(Stage stage){
        this.stage = stage;
    }

    /**
     * Funzione richiamata automaticamente da FXMLLoader al caricamento della scena, subito dopo il costruttore di default.
     * <br>
     * E' utilizzata per inizializzare la lista dei supervisor, il tipo di dati atteso dai vari campi di input (e loro valori default) e per
     * gestire la visibilità di alcuni nodi JavaFX, al fine di visualizzare correttamente la scena iniziale.
     */
    @FXML
    private void initialize() {
        userTypeChoiceBox.getItems().addAll("Tecnico", "Supervisore");
        
        startDayTxt.setTextFormatter(new TextFormatter<>(dateFilter));
        startTimeTxt.setTextFormatter(new TextFormatter<>(timeFilter));
        endDayTxt.setTextFormatter(new TextFormatter<>(dateFilter));
        endTimeTxt.setTextFormatter(new TextFormatter<>(timeFilter));
        ballotsNumTxt.setTextFormatter(new TextFormatter<>(onlyDigitsFilter));
        
        if(Constants.devMode) {
	        procedureNameTxt.setText("test");
	        startDayTxt.setText("01/01/2020");
	        startTimeTxt.setText("00:00:00");
	        endDayTxt.setText("31/12/2020");
	        endTimeTxt.setText("23:59:59");
	        ballotsNumTxt.setText("4");
        }
        
        showInitialParameterVBox();
    }

    /**
     * Funzione richiamata dal click sul menù File &gt; Logout. Invoca la funzione {@link Controller#logout()} per gestire il logout.
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    public void logout(ActionEvent actionEvent) {
        ((Controller) controller).logout();
    }
    
    /**
     * Gestisce la visibilità della scheda relativa alla creazione di nuovi utenti, a cui può accedere soltanto un utente di tipo root.
     * @param rootLogged True se l'utente loggato è root, false altrimenti.
     */
    public void initTechnicScene(boolean rootLogged) {
    	if(!rootLogged) {
	    	accessLabel.setText("Accesso come tecnico");
			tabPane.getTabs().remove(rootTab);
    	}

        supervisorChoiceBox.getItems().addAll(((Controller) controller).getAllSupervisors());
    }

    /*******************************************************
     * Metodi relativi alle funzionalità esclusive di root.*
     *******************************************************/
    
    /**
     * Inizia la creazione di un nuovo utente, richiamando il metodo {@link Controller#startUserCreation(String, String, String, String)} e passandogli
     * tutti i valori inseriti all'interno dei campi di input corrispondenti.
     * <br/>
     * Se la creazione va a buon fine, resetta i valori dei campi di input. Inoltre, se l'utente creato è un supervisore lo aggiunge alla lista
     * dei supervisori a cui è possibile assegnare una nuova procedura.
     * <br/>
     * E' richiamata dal click sul bottone "Conferma Creazione Utente".
     * @param actionEvent Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    public void createUser(ActionEvent actionEvent) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String repeatPassword = repeatPasswordField.getText();
        String userType = userTypeChoiceBox.getSelectionModel().getSelectedItem();
        
        if(((Controller) controller).startUserCreation(userType, username, password, repeatPassword)) {
            usernameField.setText("");
            passwordField.setText("");
            repeatPasswordField.setText("");
            userTypeChoiceBox.getSelectionModel().clearSelection();
            
            if(userType.equals("Supervisore"))
            	supervisorChoiceBox.getItems().add(username);
        }
    }
        
    /**********************************************************
    * Metodi relativi alle funzionalità dei tecnici e di root.*
    **********************************************************/
    
    /**
     * Gestisce la visibilità dei nodi JavaFX in modo da rendere visibile la prima parte del form di creazione di una nuova procedura.
     * <br/>
     * Richiama {@link #setProcedureCreationForm(boolean)} passandogli true a parametro.
     */
    @FXML
    private void showInitialParameterVBox(){
        setProcedureCreationForm(true);
    }
    
    /**
     * Gestisce la visibilità dei nodi JavaFX in modo da rendere visibile una delle due parti del form di creazione di una nuova
     * procedura. A seconda del parametro, mostra la prima parte e nasconde la seconda, o viceversa.
     * @param showFirstScreen True se si vuole mostrare la prima schermata, false per la seconda
     */
    private void setProcedureCreationForm(boolean showFirstScreen){
        showElement(initialParametersVBox, showFirstScreen);
        showElement(fileLoaderVBox, !showFirstScreen);
    }
    
    /**
     * Permette di inizializzare una nuova procedura. Invoca {@link Controller#createNewProcedure(String, String, String, String, String)}
     * e gli passa i valori inseriti nei diversi campi di input della prima parte del form.
     * <br/>
     * Se l'inizializzazione va a buon fine, richiama {@link #setProcedureCreationForm(boolean)} passandogli false a parametro per
     * permettere all'utente di visualizzare la seconda parte del form di creazione di una procedura.
     * <br/>
     * E' richiamata dal click sul bottone "Carica File CSV".
     */
    @FXML
    private void initProcedure() {
        String name = procedureNameTxt.getText();
        String starts = startDayTxt.getText() + " " + startTimeTxt.getText();
        String ends = endDayTxt.getText() + " " + endTimeTxt.getText();
        String numBallots = ballotsNumTxt.getText();
    	String supervisor = supervisorChoiceBox.getSelectionModel().getSelectedItem();

        if(((Controller) controller).createNewProcedure(name, starts, ends, numBallots, supervisor))
            setProcedureCreationForm(false);
    }

    /**
     * Apre un file chooser per permettere il caricamento del file CSV relativo a sessioni e postazioni di voto. Scelto un file,
     * richiama {@link Controller#checkSessionsAndVotingPlaces(String)} per effettuare la validazione dello stesso.
     * <br/>
     * Se tutto va a buon fine, accetta il file scelto e compila la TextField corrispondente.
     * <br/>
     * E' richiamata dal click sul bottone "Scegli File", in corrispondenza della riga "File Sessioni".
     * @param actionEvent Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    private void chooseSessions(ActionEvent actionEvent) {
        File sessionsCsv = chooseFile();
        
        if (sessionsCsv == null)
        	return;

        if(((Controller) controller).checkSessionsAndVotingPlaces(sessionsCsv.getAbsolutePath()))
            sessionsFileTxtField.setText(sessionsCsv.getName());
        else
        	sessionsFileTxtField.setText(null);
    }

    /**
     * Apre un file chooser per permettere il caricamento del file CSV relativo a candidati e liste elettorali. Scelto un file,
     * richiama {@link Controller#checkCandidatesAndLists(String)} per effettuare la validazione dello stesso.
     * <br/>
     * Se tutto va a buon fine, accetta il file scelto e compila la TextField corrispondente.
     * <br/>
     * E' richiamata dal click sul bottone "Scegli File", in corrispondenza della riga "File Candidati".
     * @param actionEvent Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    private void chooseCandidates(ActionEvent actionEvent) {
        File candidatesCsv = chooseFile();
        
        if (candidatesCsv == null)
        	return;

        if(((Controller) controller).checkCandidatesAndLists(candidatesCsv.getAbsolutePath()))
            candidatesFileTxtField.setText(candidatesCsv.getName());
        else
        	candidatesFileTxtField.setText(null);
    }

    /**
     * Apre un file chooser per permettere il caricamento del file CSV relativo alle schede elettorali. Scelto un file,
     * richiama {@link Controller#checkBallots(String)} per effettuare la validazione dello stesso.
     * <br/>
     * Se tutto va a buon fine, accetta il file scelto e compila la TextField corrispondente.
     * <br/>
     * E' richiamata dal click sul bottone "Scegli File", in corrispondenza della riga "File Schede".
     * @param actionEvent Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    private void chooseBallots(ActionEvent actionEvent) {
        File ballotsCsv = chooseFile();
        
        if (ballotsCsv == null)
        	return;

        if(((Controller) controller).checkBallots(ballotsCsv.getAbsolutePath()))
            ballotsFileTxtField.setText(ballotsCsv.getName());
        else
        	ballotsFileTxtField.setText(null);
    }

    /**
     * Apre un file chooser per permettere il caricamento del file CSV relativo ai votanti. Scelto un file,
     * richiama {@link Controller#checkVoters(String)} per effettuare la validazione dello stesso.
     * <br/>
     * Se tutto va a buon fine, accetta il file scelto e compila la TextField corrispondente.
     * <br/>
     * E' richiamata dal click sul bottone "Scegli File", in corrispondenza della riga "File Votanti".
     * @param actionEvent Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    private void chooseVoters(ActionEvent actionEvent) {
        File votersCsv = chooseFile();
        
        if (votersCsv == null)
        	return;

        if(((Controller) controller).checkVoters(votersCsv.getAbsolutePath()))
            votersFileTxtField.setText(votersCsv.getName());
        else
        	votersFileTxtField.setText(null);
    }
    
    /**
     * Permette la generazione di template validi per ogni file richiesto dalla creazione di una nuova procedura, invocando
     * {@link Controller#createValidSampleFiles()}.
     * <br/>
     * E' richiamata dal click sul bottone "Genera Template CSV".
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    private void genCSVPrototypes(ActionEvent event) {
    	((Controller) controller).createValidSampleFiles();
    }

    /**
     * Permette di completare la creazione di una nuova procedura e il caricamento della stessa sul DB, invocando 
     * {@link Controller#uploadProcedure(String, String, String, String)} e passandogli i nomi dei file caricati.
     * <br/>
     * Se la creazione va a buon fine, resetta tutti i campi di input e mostra nuovamente all'utente la prima parte del
     * form di creazione di una nuova procedura.
     * <br/>
     * E' richiamata dal click sul bottone "Conferma Creazione Procedura".
     */
    @FXML
    private void createProcedure() {
    	String sessionsFile = sessionsFileTxtField.getText();
    	String candidatesFile = candidatesFileTxtField.getText();
    	String ballotsFile = ballotsFileTxtField.getText();
    	String votersFile = votersFileTxtField.getText();
    	
        if(((Controller)controller).uploadProcedure(sessionsFile, candidatesFile, ballotsFile, votersFile)){
            resetProcedureInfos();
            showInitialParameterVBox();
            setProcedureCreationForm(true);
        }

        resetCsvFiles();
    }
    
    /**
     * Mostra a schermo un file chooser, permette all'utente di selezionare un file CSV dal file system.
     * @return Il percorso del file scelto
     */
    private File chooseFile() {
        if(stage == null)
            return null;

        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("File CSV (*.csv)", "*.csv");
        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showOpenDialog(stage);
        
        return file;
    }
    
    /**
     * Resetta la prima parte del form di creazione di una procedura, svuotando le caselle di testo e rimuovendo il supervisore
     * selezionato dal menu a tendina.
     */
    private void resetProcedureInfos() {
        procedureNameTxt.setText("");
        startDayTxt.setText("");
        startTimeTxt.setText("");
        endDayTxt.setText("");
        endTimeTxt.setText("");
        ballotsNumTxt.setText("");
        
        supervisorChoiceBox.getSelectionModel().clearSelection();
    }

    /**
     * Resetta la seconda parte del form di creazione di una procedura, svuotando le caselle di testo corrispondenti ai file CSV
     * caricati.
     */
    private void resetCsvFiles() {
        sessionsFileTxtField.setText("");
        candidatesFileTxtField.setText("");
        ballotsFileTxtField.setText("");
        votersFileTxtField.setText("");
    }
}
