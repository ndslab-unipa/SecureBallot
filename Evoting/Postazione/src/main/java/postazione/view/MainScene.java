package postazione.view;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.ElectoralList;
import model.EmptyBallot;
import model.Person;
import model.WrittenBallot;
import postazione.controller.Controller;
import postazione.model.Post;
import postazione.view.viewmodel.*;
import utils.Constants;
import view.ViewAbstrController;

import java.util.ArrayList;

public class MainScene extends ViewAbstrController {
    @FXML private TableView<CandidateViewModel> tblCandidates;
    @FXML private TableView<OptionViewModel> tblOptions;

    @FXML private VBox VBoxRecap;
    @FXML private TableView<BallotRecapViewModel> tblBallotsRecap;
    @FXML private TableView<VoteRecapViewModel> tblVotesRecap;

    @FXML private TableColumn<CandidateViewModel, String> listCol, firstNameCol, lastNameCol, IDCol, dateCol;
    @FXML private TableColumn<CandidateViewModel, CheckBox> selCandidateCol;

    @FXML private TableColumn<OptionViewModel, String> strOptionCol;
    @FXML private TableColumn<OptionViewModel, CheckBox> selOptionCol;

    @FXML private TableColumn<VoteRecapViewModel, String> recapVoteBallotCol, recapListCol, recapPrefCol;
    @FXML private TableColumn<BallotRecapViewModel, String> recapBallotCol, recapBallotPrefCol;

    @FXML private HBox prefHBox;
    @FXML private Button btnStart, btnPrevious, btnNext, btnSend;
    @FXML private Label lblTitle, lblDescription, currPrefLabel, maxPrefLabel;
    
    private int numBallots, currentBallot = 0;
    private EmptyBallot[] emptyBallots;
    private ArrayList<WrittenBallot> writtenBallots;

    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
     */
    @FXML
    private void initialize() {
    	//Tabella candidati
        listCol.setCellValueFactory(
                new PropertyValueFactory<>("electoralList"));
        firstNameCol.setCellValueFactory(
                new PropertyValueFactory<>("firstName"));
        lastNameCol.setCellValueFactory(
                new PropertyValueFactory<>("lastName"));
        IDCol.setCellValueFactory(
                new PropertyValueFactory<>("ID"));
        dateCol.setCellValueFactory(
                new PropertyValueFactory<>("dateOfBirth"));

        selCandidateCol.setCellValueFactory(new CandidateCallBack(this));
        
        //Tabella opzioni
        strOptionCol.setCellValueFactory(
                new PropertyValueFactory<>("option"));
        
        selOptionCol.setCellValueFactory(new OptionCallBack(this));

        //Tabella recap voti
        recapVoteBallotCol.setCellValueFactory(
                new PropertyValueFactory<>("ballot"));
        recapListCol.setCellValueFactory(
                new PropertyValueFactory<>("list"));
        recapPrefCol.setCellValueFactory(
                new PropertyValueFactory<>("preference"));

        //Tabella recap scheda
        recapBallotCol.setCellValueFactory(
                new PropertyValueFactory<>("ballot"));
        recapBallotPrefCol.setCellValueFactory(
                new PropertyValueFactory<>("prefString"));

        //Elimino tutti gli elementi in attesa del click dell'utente
        showElement(tblCandidates, false);
        showElement(tblOptions, false);
        showElement(VBoxRecap, false);
    }

    /**
     * Funzione richiamata per popolare le tabelle quando l'utente indica di essere pronto premendo un bottone.
     */
    @FXML
    private void start() {
        Post post = ((Controller) controller).getPost();
        
        if(Constants.devMode && post.getSimulateOffline()) {
        	post.setSimulateOffline(false);
        	return;
        }

        emptyBallots = post.getEmptyBallots();
        writtenBallots = post.getWrittenBallots();
        numBallots = emptyBallots.length;

        lblTitle.setVisible(true);
        lblDescription.setVisible(true);

        showElement(prefHBox, true);
        showElement(btnPrevious, true);
        showElement(btnNext, true);
        showElement(btnSend, true);
        showElement(btnStart, false);

        showBallot();
    }

    /**
     * Mostra la scheda precedente.
     */
    @FXML
    private void previousBallot(){
        currentBallot--;
        showBallot();
    }

    /**
     * Mostra la scheda successiva o quella di recap se si è arrivati alla fine.
     */
    @FXML
    private void nextBallot(){
        currentBallot++;
        if(currentBallot < numBallots)
            showBallot();
        else
            showRecap();
    }

    /**
     * Invia il voto
     */
    @FXML
    private void sendVote(){
    	((Controller) controller).tryVoteSending();
    }

    /**
     * Seleziona un candidato leggendo il click da qualunque punto della tabella meno che i maledetti checkbox.
     * Morte ai checkbox maledetti.
     */
    @FXML
    private void selectCandidateFromTableView() {
    	CandidateViewModel candidate = tblCandidates.getSelectionModel().getSelectedItem();
    	
    	if(candidate == null) {
        	tblCandidates.getSelectionModel().clearSelection();
        	return;
    	}

    	selectCandidate(candidate);
    }

    /**
     * Seleziona una opzione leggendo il click da qualunque punto della tabella meno che dai checkbox.
     */
    @FXML
    private void selectOptionFromTableView() {
        OptionViewModel option = tblOptions.getSelectionModel().getSelectedItem();
        
        if (option == null) {
        	tblOptions.getSelectionModel().clearSelection();
        	return;
        }
        
        selectOption(option);
    }

    /**
     * Seleziona un candidato indipendentemente dall'origine della richiesta.
     * Richiamata sia dal click su tabella che su checkbox.
     * @param candidate Il candidato da selezionare.
     */
    public void selectCandidate(CandidateViewModel candidate) {
        boolean newValue = !candidate.isSelected();
        int currPrefs = Integer.parseInt(currPrefLabel.getText());
        
        if(((Controller) controller).selectCandidate(currentBallot, candidate.getElectoralList(), candidate.getID(), newValue)) {
            currPrefLabel.setText(newValue ? String.valueOf(currPrefs+1) : String.valueOf(currPrefs-1));
        	candidate.setSelected(newValue);
        } 
        else
            candidate.setSelected(!newValue);
        
        tblCandidates.getSelectionModel().clearSelection();
        tblCandidates.refresh();
    }
    
    /**
     * Seleziona una opzione indipendentemente dall'origine della richiesta.
     * Richiamata sia dal click su tabella che su checkbox.
     * @param option L'opzione da selezionare.
     */
    public void selectOption(OptionViewModel option) {
        boolean newValue = !option.isSelected();
        int currPrefs = Integer.parseInt(currPrefLabel.getText());
        
        if(((Controller) controller).selectOption(currentBallot, option.getOption(), newValue)) {
        	currPrefLabel.setText(newValue ? String.valueOf(currPrefs+1) : String.valueOf(currPrefs-1));
            option.setSelected(newValue);
        }
        else
        	option.setSelected(!newValue);
        
        tblOptions.getSelectionModel().clearSelection();
        tblOptions.refresh();
    }
    
    /**
     * Recupera e mostra a schermo le informazioni sulla scheda corrente.
     */
    private void showBallot() {
    	int rowLimit = 15;
        boolean rowLimitReached = false;
        int minRowHeight = 24, maxRowHeight = 50, minFontSize = 14, maxFontSize = 20;
        
        //Recupera la scheda e setta a 0 il numero di preferenze espresse
        EmptyBallot eb = emptyBallots[currentBallot];
        WrittenBallot wb = writtenBallots.get(currentBallot);
        int prefChosen = 0;

        //Scrive titolo e descrizione della scheda su schermo
        lblTitle.setText(eb.getTitle());
        lblDescription.setText(eb.getDescription());

        //Incapsula le informazioni sui candidati in un array di Observable
        ObservableList<CandidateViewModel> candidatesData = FXCollections.observableArrayList();
        
        ArrayList<ElectoralList> electoralLists = eb.getLists();
        int numCandidates = 0;
        for(ElectoralList list : electoralLists)
        	numCandidates += list.getCandidates().size();
        
        //Incapsula le informazioni sui candidati in un array di Observable
        ObservableList<OptionViewModel> optionsData = FXCollections.observableArrayList();
        
        ArrayList<String> options = eb.getOptions();
        int numOpts = options.size();
        
        if(numCandidates > 0 && numOpts == 0) {
        	rowLimitReached = numCandidates >= rowLimit;
        	
            for(ElectoralList list : electoralLists)
        		for(Person candidate : list.getCandidates()) {
        		    //Aggiorna il numero di preferenze espresse per questa scheda
        			boolean chosen = wb.chosenCandidate(candidate.getID());
        			if(chosen) 
        				prefChosen++;
        			
        			//riga reale
        			candidatesData.add(new CandidateViewModel(currentBallot, list.getName(), candidate, chosen));
        			
        			if(!rowLimitReached) {
    	    			//riga di padding
    	    			candidatesData.add(null);
        			}
        		}
            
        	if(!rowLimitReached) {
	        	//Rimuoviamo l'ultima riga di padding aggiunta perché inutile
	            candidatesData.remove(candidatesData.size() - 1);
	
	            //Aggiungiamo il listener alla tabella dei candidati per la gestione dinamica delle altezze delle righe
	            setRowFactoryListener(tblCandidates, numCandidates, minRowHeight, maxRowHeight, 80, minFontSize, maxFontSize);
        	}

    		tblCandidates.setItems(candidatesData);
        }
        else if(numCandidates == 0 && numOpts > 0) {
        	rowLimitReached = numOpts >= rowLimit;
        	
        	for(String option : eb.getOptions()) {
                //Aggiorna il numero di preferenze espresse per questa scheda
                boolean chosen = wb.chosenOption(option);
    			if(chosen) 
    				prefChosen++;
    			
    			//riga reale
    			optionsData.add(new OptionViewModel(option, chosen));
                
    			if(!rowLimitReached) {
    				//riga di padding
    				optionsData.add(null);
    			}
    		}

            //Se la scheda non presenta candidati la tabella candidati non viene mostrata affatto
        	if(!rowLimitReached) {
	        	//Rimuoviamo l'ultima riga di padding aggiunta perché inutile
	            optionsData.remove(optionsData.size() - 1);
	
	            //Aggiungiamo il listener alla tabella delle opzioni per la gestione dinamica delle altezze delle righe
	            setRowFactoryListener(tblOptions, numOpts, minRowHeight, maxRowHeight, 40, minFontSize, maxFontSize);
        	}

    		tblOptions.setItems(optionsData);
        }
        
        //Se la scheda non presenta opzioni la tabella opzioni non viene mostrata affatto
        showElement(tblOptions, !optionsData.isEmpty());
        
        //Se la scheda non presenta candidati la tabella candidati non viene mostrata affatto
        showElement(tblCandidates, !candidatesData.isEmpty());

    	//Mostriamo a schermo il numero di preferenze espresse e di quelle massime
    	currPrefLabel.setText(String.valueOf(prefChosen));
        maxPrefLabel.setText(String.valueOf(wb.getMaxPreferences()));

        //Gestiamo bottoni e label a seconda della scheda corrente
        enableAndDisableElements();
    }

    /**
     * Funzione che assegna un listener ad una tabella perché adatti alla sua dimensione l'altezza delle sue righe,
     * in base ai parametri passati per argomento ad ogni resize.
     * @param table         La tabella per cui si vuole gestire il comportamento dopo un resize.
     * @param numRows       Il numero di righe valide (solo quelle relative a dati reali, non quelle usate per il padding).
     * @param minRowHeight  L'altezza minima che devono avere le righe valide (per le righe per il padding è 0).
     * @param maxRowHeight  L'altezza massima che devono avere le righe valide (per le righe per il padding è pari a quella effettiva delle righe valide).
     * @param margin        Un margine che viene tenuto per gestire la parte di tabella in cui sono presenti i nomi delle colonne.
     * @param minFontSize   La dimemsione minima del font (il font cresce linearmente con l'altezza delle righe valide)
     * @param maxFontSize   La dimemsione massima del font (il font cresce linearmente con l'altezza delle righe valide)
     * @param <ViewModel>   Rispettivamente {@link postazione.view.viewmodel.CandidateViewModel CandidateViewModel} e {@link postazione.view.viewmodel.OptionViewModel OptionViewModel}.
     */
    private <ViewModel> void setRowFactoryListener(TableView<ViewModel> table, int numRows, int minRowHeight, int maxRowHeight, int margin, int minFontSize, int maxFontSize){

        /*
         * Listener simile al change listener.
         * Risponde ai cambi di dimensione dell'observable passato per argomento (l'altezza della tabella)
         * ricalcolando la giusta altezza di righe contenente dati, righe di "padding" e dimensione font
         */
        final InvalidationListener resizeListener = new InvalidationListener() {
            @Override
            public void invalidated(final Observable observable) {

                //Si recupera la dimensione effettiva della tabella
                double height = table.getHeight();

                //Una parte della dimensione, passata per argomento, viene eliminata dal calcolo allo scopo di
                // lavorare solamente alla parte di tabella contenente dati, escludendo lo spazio riservato ai nomi delle colonne
                double h = height - margin;

                //Lo scopo di questo listener è calcolare questi due parametri e assegnarne uno come altezza della riga,
                // a seconda se si tratta di una riga di padding o di una riga valida (contenente dati
                int rowHeight, paddingHeight;

                //Verifichiamo se l'altezza della tabella è sufficiente ad avere righe reali e di padding con altezza uguale tra di loro e
                // maggiore dell'altezza minima passata per argomento
                int temp = (int) Math.floor(h / (2 * numRows - 1));
                if(temp >= minRowHeight){
                    //Se siamo quì vuol dire che abbiamo abbastanza spazio per cui settiamo entrambe le altezze
                    // al più piccolo valore tra quella calcolata e quella massima permessa
                    rowHeight = Math.min(temp, maxRowHeight);
                    paddingHeight = rowHeight;
                }
                else{
                    //Se siamo quì abbiamo poco spazio, settiamo l'altezza delle righe valide al minimo possibile e
                    // l'altezza delle righe di padding al più grande valore positivo che ci permetta di non sforare
                    // al di fuori della tabella, o a 0 altrimenti.
                    rowHeight = minRowHeight;
                    paddingHeight = (int) Math.max(Math.floor((h - numRows * rowHeight) / (numRows - 1)), 1);
                }

                //Adesso non resta che sovrascrivere la rowFactory della tabella con una avente i parametri appena calcolati
                table.setRowFactory(row -> new TableRow<>() {
                    @Override
                    protected void updateItem(ViewModel viewModel, boolean empty) {
                        super.updateItem(viewModel, empty);

                        //Se la riga è di padding l'oggetto è null e ne settiamo l'altezza a paddingHeight
                        if (viewModel == null) {
                            setStyle("-fx-cell-size: " + paddingHeight +"px;");
                            return;
                        }

                        //La dimensione del font è lineare rispetto alla altezza di una riga
                        int fontSize = minFontSize + (maxFontSize - minFontSize) * (rowHeight - minRowHeight) / (maxRowHeight - minRowHeight);
                        //Se la riga contiene dati allora settiamo altezza e dimensione font a rowHeight e fontSize
                        setStyle("-fx-cell-size: " + rowHeight +"px; -fx-font-size:" + fontSize + "px;");
                    }
                });

                //Per evitare che le modifiche effettuate cambino nuovamente la dimensione della tabella
                // settiamo l'altezza desiderata a quella di partenza ed effettuiamo immediatamente un refresh
                table.setPrefHeight(height);
                table.refresh();
            }
        };
        table.widthProperty().addListener(resizeListener);
        table.heightProperty().addListener(resizeListener);
    }

    /**
     * Mostra la schermata di recap visualizzata dal votante prima di dare conferma e di inviare definitivamente i voti.
     */
    private void showRecap(){
        lblTitle.setText("Riepilogo Votazioni");
        lblDescription.setText("Dopo aver verificato le votazioni effettuate, premi il bottone \"Invia Voti\".");

        //Incapsula le informazioni in array di Observable
        ObservableList<BallotRecapViewModel> ballotRecapData = FXCollections.observableArrayList();
        ObservableList<VoteRecapViewModel> voteRecapData = FXCollections.observableArrayList();

        //Si scorrono le schede e si recuperano le info dei soli candidati/opzioni scelte/i dal votante
        int i = 0;
        for(WrittenBallot wb : writtenBallots){
            EmptyBallot eb = emptyBallots[i];
            i++;

            int maxPrefs = wb.getMaxPreferences();
            int numPrefs = 0;
            for(ElectoralList list : eb.getLists()){
                for(Person candidate : list.getCandidates()){
                    if(wb.chosenCandidate(candidate.getID())){
                        String preference = candidate.getFirstName() + " " + candidate.getLastName() + " (" + candidate.getID() + ")";
                        voteRecapData.add(new VoteRecapViewModel(eb.getTitle(), list.getName(), preference));
                        numPrefs++;
                    }
                }
            }

            for(String option : eb.getOptions()){
                if(wb.chosenOption(option)){
                    voteRecapData.add(new VoteRecapViewModel(eb.getTitle(), "---", "\"" + option + "\""));
                    numPrefs++;
                }
            }

            ballotRecapData.add(new BallotRecapViewModel(eb.getTitle(), numPrefs, maxPrefs));
        }

        //Per ogni scheda deve essere mostrate numero di preferenze espresse e esprimibili
        tblBallotsRecap.setItems(ballotRecapData);
        //Ogni candidato e ogni opzione selezionata vengono mostrati come righe della tabella riepilogativa
        tblVotesRecap.setItems(voteRecapData);

        showElement(tblCandidates, false);
        showElement(tblOptions, false);

        //Gestiamo bottoni e label
        enableAndDisableElements();
    }

    /**
     * Funzione che mostra o nasconde bottoni e label a seconda di quale è la scheda corrente.
     */
    private void enableAndDisableElements(){
        btnPrevious.setDisable(currentBallot <= 0);
        btnNext.setDisable(currentBallot >= numBallots);
        btnSend.setVisible(currentBallot >= numBallots);

        prefHBox.setVisible(currentBallot < numBallots);
        showElement(VBoxRecap, currentBallot >= numBallots);
    }
}