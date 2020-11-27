package poll.view;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Map;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import poll.controller.Controller;
import poll.view.viewmodel.BallotResult;
import poll.view.viewmodel.CandidateEntry;
import poll.view.viewmodel.OptionEntry;
import view.ViewAbstrController;

/**
 * Classe che gestisce la scena <i>main-scene.fxml</i>. Contiene i riferimenti a tutti i nodi JavaFX dichiarati 
 * nella scena e gestisce tutte le funzioni richieste. Estende {@link view.ViewAbstrController ViewAbstrController}.
 */
public class MainScene extends ViewAbstrController {
    @FXML private MenuBar menuBar;
    @FXML private Menu fileMenu, exportMenu;
    @FXML private MenuItem menuLogout, csvExportMenu, pdfExportMenu;
    @FXML private SplitPane resPane;
    @FXML private VBox ballotInfoPane;
    @FXML private Label titleLabel, descLabel, loadingLabel, nullPrefs, emptyBallots;
    @FXML private BorderPane ballotResultsPane;
    @FXML private VBox loadingPane;
    @FXML private Button prevBtn, startBtn, nextBtn;
    @FXML private ImageView imageView;
    
    @FXML private TableView<CandidateEntry> candidatesTbl;
    @FXML private TableColumn<CandidateEntry, String> codColumn, nameColumn, votesColumn;
    
    @FXML private TableView<OptionEntry> optionsTbl;
    @FXML private TableColumn<OptionEntry, String> optionColumn, optVotesColumn;
    
    private Stage stage = null;
    private ArrayList<BallotResult> results = null;
    private int currBallotIdx = -1, nBallots = -1;
    
    /**
     * Setter per lo stage mantenuto all'interno della classe. A differenza degli altri controller delle scene, ha bisogno dello stage
     * per permettere di mostrare i FileChooser, necessari per esportare i risultati dello spoglio.
     * @param stage Riferimento allo stage JavaFX
     */
    public void setStage(Stage stage) { 
    	this.stage = stage; 
    }
    
    /**
     * Funzione richiamata automaticamente da FXMLLoader al caricamento della scena, subito dopo il costruttore di default.
     * <br>
     * E' utilizzata per inizializzare le tabelle che permettono di visualizzare i risultati dello spoglio: mappa gli attributi di 
     * {@link CandidateEntry} ed {@link OptionEntry} sulle corrispondenti colonne delle tabella e rende non riordinabile ogni colonna. 
     */
    @FXML
    public void initialize() {
        codColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        votesColumn.setCellValueFactory(new PropertyValueFactory<>("votes"));
    	codColumn.setReorderable(false);
        nameColumn.setReorderable(false);
        votesColumn.setReorderable(false);
        
        optionColumn.setCellValueFactory(new PropertyValueFactory<>("option"));
        optVotesColumn.setCellValueFactory(new PropertyValueFactory<>("votes"));
        optionColumn.setReorderable(false);
        optVotesColumn.setReorderable(false);
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
     * Funzione richiamata dal click sul menù Stampa Risultati &gt; CSV. Invoca la funzione {@link #exportResults(String)}, passandogli l'estensione
     * ".csv" per esportare i risultati elettorali in formato CSV.
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    private void exportCSV(ActionEvent event) {
    	exportResults(".csv");
    }

    /**
     * Funzione richiamata dal click sul menù Stampa Risultati &gt; PDF. Invoca la funzione {@link #exportResults(String)}, passandogli l'estensione
     * ".pdf" per esportare i risultati elettorali in formato PDF.
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    private void exportPDF(ActionEvent event) {
    	exportResults(".pdf");
    }
    
    /**
     * Gestisce lo spoglio dei risultati e la loro visualizzazione. 
     * <br>
     * Inizialmente, disabilita tutti i menù ed il bottone "Inizia Spoglio" e visualizza una gif di caricamento mentre viene eseguito
     * lo spoglio stesso. Quindi, avvia un Task su un Thread separato per eseguire lo spoglio (tramite {@link Controller#countVotesAndGetResults()}.
     * <br>
     * Finito lo spoglio, gestisce la visibilità degli elementi per mostrare le tabelle coi risultati, se ce ne sono, o una schermata di errore.
     * <br>
     * E' richiamata dal click sul bottone "Inizia Spoglio".
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    private void startCounting(ActionEvent event) {
    	showImg("loading.gif"); 
    	
    	fileMenu.setDisable(true);
    	loadingPane.setVisible(true);
    	startBtn.setDisable(true);
    	
    	Task<Void> countVotesTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
            	results = ((Controller) controller).countVotesAndGetResults();
            	nBallots = results.size();
            	return null;
            }
        };
        
        countVotesTask.setOnFailed(ev -> {
        	showErrorMsg();
        });
        
        countVotesTask.setOnSucceeded(ev -> {
        	if (results == null || results.size() == 0) {
        		showErrorMsg();
        		return;
        	}

        	handleSceneElements();
        	
        	currBallotIdx = 0;
    		showNewBallot();
        });
        
        new Thread(countVotesTask).start();
    }
    
    /**
     * Carica un'immagine da file system e, se riesce a trovarla, la mostra all'interno dell'ImageView presente nella scena.
     * @param imgName Nome ed estensione del file, da cercare all'interno di src/main/resources/img
     */
    private void showImg(String imgName) {
    	try {
    		FileInputStream inputstream = new FileInputStream("src/main/resources/img/"+imgName);
	    	Image image = new Image(inputstream); 
	    	imageView.setImage(image);
		} 
		catch (Exception e) { 
			e.printStackTrace(); 
		}
    }
    
    /**
     * Mostra un messaggio d'errore nel caso in cui lo spoglio non sia andato a buon fine.
     */
    private void showErrorMsg() {
    	startBtn.setVisible(false);
    	fileMenu.setDisable(false);
    	
    	showImg("error.png");
		
		loadingLabel.setText("Errore! Per favore, effettua il logout e riprova.");
    }
    
    /**
     * Gestisce i vari elementi della scena in modo da permettere di passare dalla visualizzazione dell'immagine di
     * caricamento alla visualizzazione dei risultati elettorali.
     */
    private void handleSceneElements() {
    	startBtn.setVisible(false);
    	fileMenu.setDisable(false);

    	loadingPane.setVisible(false);
    	
    	ballotInfoPane.setVisible(true);
    	ballotResultsPane.setVisible(true);
    	
    	prevBtn.setVisible(true);
    	nextBtn.setVisible(true);
    	
    	exportMenu.setDisable(false);
    }

    /**
     * Permette di mostrare la scheda precedente rispetto a quella corrente. Decrementa l'indice della scheda
     * corrente e richiama {@link #showNewBallot()}.
     * <br>
     * E' richiamata dal click sul bottone "&lt;-".
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    private void showPrevResults(ActionEvent event) {
    	currBallotIdx--;
    	showNewBallot();
    }
    
    /**
     * Permette di mostrare la scheda successiva rispetto a quella corrente. Incrementa l'indice della scheda
     * corrente e richiama {@link #showNewBallot()}.
     * <br>
     * E' richiamata dal click sul bottone "-&gt;".
     * @param event Evento associato alla chiamata a questa funzione, non utilizzato.
     */
    @FXML
    private void showNextResults(ActionEvent event) {
    	currBallotIdx++;
    	showNewBallot();
    }
    
    /**
     * Permette di visualizzare una nuova scheda di risultati elettorali, sulla base dell'indice della scheda corrente.
     * <br>
     * Se l'indice è fuori dai limiti consentiti non fa nulla. Altrimenti, aggiorna la scena per mostrare i dati della nuova scheda selezionata
     * (titolo, descrizione, numero di preferenze non espresse, numero di schede bianche, tabelle dei risultati).
     * <br>
     * Quindi, gestisce i bottoni per passare alla scheda successiva e precedente: se la scheda attuale è la prima, disabilita il bottone per andare
     * alla precedente; se la scheda attuale è l'ultima, disabilita il bottone per passare alla successiva.
     */
    private void showNewBallot() {
    	if (currBallotIdx < 0 || currBallotIdx > nBallots -1)
    		return;
    	
    	BallotResult newBallot = results.get(currBallotIdx);
    	
    	titleLabel.setText(newBallot.getTitle());
    	descLabel.setText(newBallot.getDescription());
    	nullPrefs.setText(String.valueOf(newBallot.getNullPreferences()));
    	emptyBallots.setText(String.valueOf(newBallot.getEmptyBallots()));
    	
    	ObservableList<CandidateEntry> candidatesData = candidatesTbl.getItems();
    	candidatesData.clear();
    	
    	Map<String, CandidateEntry> candidatesResults = newBallot.getCandidatesResults();
    	showElement(candidatesTbl, candidatesResults.size() > 0);

		candidatesResults.forEach((String candidateId, CandidateEntry entry) -> {
			candidatesData.add(entry);
		});
    	
    	ObservableList<OptionEntry> optionsData = optionsTbl.getItems();
    	optionsData.clear();
    	
    	Map<String, OptionEntry> optionsResults = newBallot.getOptionsResults();
    	showElement(optionsTbl, optionsResults.size() > 0);

		optionsResults.forEach((String candidateId, OptionEntry entry) -> {
			optionsData.add(entry);
		});
    	
    	nextBtn.setDisable(currBallotIdx >= nBallots - 1);
    	prevBtn.setDisable(currBallotIdx <= 0);
    }
    
    /**
     * Mostra il FileChooser per scegliere percorso e nome del file in cui devono essere esportati i risultati dello spoglio.
     * Scelto il percorso, richiama {@link Controller#exportResults(File, String)} passandogli il file creato dal FileChooser e l'estensione
     * desiderata (.pdf o .csv).
     * @param ext Estensione del file
     */
    private void exportResults(String ext) {
    	if (stage == null)
    		return;
    	
    	FileChooser fileChooser = new FileChooser();
    	 
        //Set extension filter for text files
        FileChooser.ExtensionFilter extFilter = ext.equals(".csv") ?
        		new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv") :
        		new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf");
        
        fileChooser.getExtensionFilters().add(extFilter);

        //Show save file dialog
        File file = fileChooser.showSaveDialog(stage);

        //Handle file saving
        ((Controller) controller).exportResults(file, ext);
    }
}
