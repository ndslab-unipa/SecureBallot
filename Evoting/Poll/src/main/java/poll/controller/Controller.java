package poll.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import controller.AbstrController;
import exceptions.PEException;
import model.Procedure;
import model.State.StatePoll;
import poll.model.Poll;
import poll.view.viewmodel.BallotResult;
import poll.view.viewmodel.CandidateEntry;
import poll.view.viewmodel.OptionEntry;
import utils.CfgManager;
import utils.Constants;
import view.ViewInterface;

/**
 * Classe controller del modulo Poll. Contiene tutte le funzioni necessarie ad inizializzare il modulo e ad interagire con view e model.
 */
public class Controller extends AbstrController {
	private final Poll poll;
	private final PollDB db;
	
	/**
	 * Costruttore che inizializza tutti i dati richiesti dal controller: view, model ed interfaccia col DB. 
	 * Inoltre, se è richiesta connessione SSL al DB, setta le proprietà di sistema necessarie al reperimento di TrustStore e KeyStore
	 * (<i>javax.net.ssl.keyStore</i>, <i>javax.net.ssl.keyStorePassword</i> ecc..).
	 * @param view View di Poll
	 * @param pm Model di Poll
	 * @param db Oggetto per l'interazione col DB
	 * @throws PEException Se non trova i keystores o se non riesce a recuperare le password di questi dal file <i>psws.cfg</i>
	 */
	public Controller(ViewInterface view, Poll pm, PollDB db) throws PEException { 
		super(view);
		this.poll = pm;
		this.db = db;
		
		if (Constants.dbSSL) {
			System.setProperty("javax.net.ssl.keyStore", "ssl/keystore.jks");
			System.setProperty("javax.net.ssl.keyStorePassword", CfgManager.getPassword("ks"));
				
			System.setProperty("javax.net.ssl.trustStore", "ssl/truststore.jks");
			System.setProperty("javax.net.ssl.trustStorePassword", CfgManager.getPassword("ts"));
		}
	}
	
	/* -------------------------------- */
	/*                                  */
	/* --- Inizializzazione di Poll --- */
	/*                                  */
	/* -------------------------------- */
	
	/**
	 * Inizializza il Poll col codice della procedura scelta. Verifica che il codice della procedura sia valido ed aggiorna
	 * di conseguenza model e view: porta lo stato del model a COUNTING ed aggiorna la view per permettere il caricamento della nuova scena.
	 * Altrimenti, se la verifica non va a buon fine, mostra un dialog di errore.
	 * @param procCode Codice della procedura selezionata
	 */
	public void confirmProcedure(int procCode) {
		if (procCode == -1) {
			printError("Seleziona una Procedura", "Assicurati di aver selezionato una procedura prima di confermare.");
			return;
		}
		
		Procedure proc = poll.getProcedure(procCode);
		
		if (proc == null) {
			printError("Seleziona una Procedura", "Nessuna procedura trovata col codice selezionato. Riprova o contatta un amministratore");
			return;
		}
		
		if (!proc.getTerminated() && !Constants.devMode) {
			printError("Procedura Non Terminata", "Impossibile procedere con lo spoglio di una procedura non terminata.");
			return;
		}
		
		poll.setProcCode(procCode);
		poll.setState(StatePoll.COUNTING);
		updateView();
	}
	
	/* ------------------------------------------------------------- */
	/*                                                               */
	/* --- Funzioni richiamate dai controller delle scene JavaFX --- */
	/*                                                               */
	/* ------------------------------------------------------------- */
	
	/**
	 * Verifica i dati inseriti nella schermata di login richiamando {@link AbstrController#checkLoginData(model.AbstrModel, db.DB, String, String)}.
	 * Se il login è verificato, aggiorna lo stato del model e la view, per permettere il caricamento della nuova scena.
	 * @param user Username dell'utente
	 * @param psw Password dell'utente
	 */
	public void checkLogin(String user, String psw) {
		if(checkLoginData(poll, db, user, psw)) {
			poll.setState(StatePoll.ATTIVO);
    		updateView();
		}
	}
	
	/**
	 * Chiede la conferma per eseguire il logout richiamando {@link AbstrController#confirmLogout(model.AbstrModel)} (a cui passa
	 * il proprio model). Se confermato, il model viene riportato allo stato NON ATTIVO e viene aggiorna di conseguenza la view.
	 */
	public void logout() {
    	if(confirmLogout(poll)) {
    		poll.setState(StatePoll.NON_ATTIVO);
    		updateView();
    	}
	}
	
	/**
	 * Recupera dal DB l'elenco di procedure associate all'utente loggato 
	 * (tramite la funzione {@link PollDB#getProcedures(String)}, a cui passa l'username dell'utente).
	 * <br>
	 * Le procedure sono memorizzate come oggetti di tipo {@link model.Procedure Procedure}.
	 * @return Lista delle procedure recuperate dal DB
	 */
	public ArrayList<Procedure> getProcedures() {
		try {
			poll.setProcedures(db.getProcedures(poll.getUsername()));
		} catch (PEException e) {
			printError(e);
		}
		
		return poll.getProcedures();
	}
	
	/**
	 * Avvia lo spoglio dei voti (tramite {@link PollDB#countVotes(int, String, String)}, a cui passa il codice della procedura selezionata
	 * ed i dati dell'utente loggato) 
	 * e restituisce i risultati elettorali. Inoltre, se almeno un pacchetto di voto ha causato errore, mostra un dialog di avvertimento.
	 * @return Lista dei risultati elettorali, sotto forma di ArrayList di {@link poll.view.viewmodel.BallotResult BallotResult}
	 */
	public ArrayList<BallotResult> countVotesAndGetResults() {
		try {
			int errors = db.countVotes(poll.getProcCode(), poll.getUsername(), poll.getPassword());
			
			if (errors > 0)
				printWarning("Problemi durante lo spoglio dei voti", errors+" voti non sono stati conteggiati a causa di errori di cifratura/decifratura");
			
			poll.setElectoralResults(db.getResults(poll.getProcCode()));
		} 
		catch (PEException e) {
			printError(e);
		}
		
		return poll.getElectoralResults();
	}
	
	/**
	 * Permette di esportare i risultati elettorali in un file CSV o PDF.
	 * @param file File creato da un FileChooser di JavaFX
	 * @param extension Estensione del file (.csv o .pdf)
	 */
	public void exportResults(File file, String extension) {
		if (file == null)
			return;
		
		if (!file.getAbsolutePath().endsWith(extension))
			file = new File(file.getAbsolutePath()+extension);
		
		switch(extension) {
			case ".csv":
				exportResultsToCSV(file);
				break;
				
			case ".pdf":
				exportResultsToPDF(file);
				break;
		}
	}
	
	/* --------------------------- */
	/*                             */
	/* --- Funzioni di utility --- */
	/*                             */
	/* --------------------------- */
	
	/**
	 * Esporta i risultati dello spoglio in un file CSV.
	 * @param file File creato da un FileChooser di JavaFX
	 */
	private void exportResultsToCSV(File file) {
		try (PrintWriter pw = new PrintWriter(new FileOutputStream(file, false))) {
			pw.println("ballot_id;candidate_code;candidate_name;candidate_votes;");
			
			for (BallotResult ballot : poll.getElectoralResults()) {
				String ballotId = ballot.getId();
				
				Map<String, CandidateEntry> candidatesResults = ballot.getCandidatesResults();
                for (String candidateId : candidatesResults.keySet()) {
                	CandidateEntry entry = candidatesResults.get(candidateId);
                	
                	pw.println(ballotId+";"+entry.getCode()+";"+entry.getName()+";"+entry.getVotes()+";");
                }
                
                Map<String, OptionEntry> optionsResults = ballot.getOptionsResults();
                for (String candidateId : optionsResults.keySet()) {
                	OptionEntry entry = optionsResults.get(candidateId);
                	
                	pw.println(ballotId+";"+"---"+";"+entry.getOption()+";"+entry.getVotes()+";");
                }
                
                pw.println(ballotId+";"+"NULL"+";"+"Preferenze Nulle"+";"+ballot.getNullPreferences());
                pw.println(ballotId+";"+"EMPTY"+";"+"Schede Bianche"+";"+ballot.getEmptyBallots());
			}
			printSuccess("Esportazione completa", "Risultati correttamente esportati in CSV nel file: "+file.getAbsolutePath());
		} catch (IOException e) {
			printError("Esportazione fallita", "Impossibile esportare i risultati in CSV");
			e.printStackTrace();
		}
	}
	
	/**
	 * Esporta i risultati dello spoglio in un file PDF.
	 * @param file File creato da un FileChooser di JavaFX
	 */
	private void exportResultsToPDF(File file) {
		try (PDDocument doc = new PDDocument()) {
			for (BallotResult ballot : poll.getElectoralResults()) {
				PDPage currPage = new PDPage();
	            doc.addPage(currPage);
	            
	            try (PDPageContentStream currPageContents = new PDPageContentStream(doc, currPage)) {
	            	PDRectangle mediaBox = currPage.getMediaBox();
	            	PDImageXObject header = PDImageXObject.createFromFile("src/main/resources/img/logo-unipa.gif", doc);
	            	
	            	//Resize dell'immagine
	            	float imgW = header.getWidth() * 0.5f;
	            	float imgH = header.getHeight() * 0.5f;
	            	
	            	//X e Y partono dall'angolo in basso a sinistra dell'immagine!
	                float startX = (mediaBox.getWidth() - imgW) / 2;
	                float startY = mediaBox.getHeight() - imgH;
	                
	                currPageContents.drawImage(header, startX, startY, imgW, imgH);
	                
	                currPageContents.beginText();
	                currPageContents.setLeading(14.5f);
	                currPageContents.newLineAtOffset(25, startY - 10);
	                
	                printPDFBallotInfo(currPageContents, "ID Scheda", ballot.getId());
	                printPDFBallotInfo(currPageContents, "Titolo", ballot.getTitle());
	                printPDFBallotInfo(currPageContents, "Descrizione", ballot.getDescription());

	                currPageContents.setFont(PDType1Font.TIMES_ROMAN, 12);
	                currPageContents.newLine();
	                
	                Map<String, CandidateEntry> candidatesResults = ballot.getCandidatesResults();
	                for (String candidateId : candidatesResults.keySet()) {
	                	CandidateEntry entry = candidatesResults.get(candidateId);
	                	printPDFVotes(currPageContents, entry.getCode(), entry.getName(), entry.getVotes());
	                }
	                
	                Map<String, OptionEntry> optionResults = ballot.getOptionsResults();
	                if (candidatesResults.size() > 0 && optionResults.size() > 0)
	                	currPageContents.newLine();
	                
	                for (String candidateId : optionResults.keySet()) {
	                	OptionEntry entry = optionResults.get(candidateId);
	                	printPDFVotes(currPageContents, null, entry.getOption(), entry.getVotes());
	                }
	                
	                currPageContents.newLine();
	                printPDFVotes(currPageContents, "(NULL)", "Preferenze Nulle", ballot.getNullPreferences());
	                printPDFVotes(currPageContents, "(EMPTY)", "Schede Bianche", ballot.getEmptyBallots());
	                
	                currPageContents.endText();
	            }
			}
            doc.save(file.getAbsolutePath());
            printSuccess("Esportazione completa", "Risultati correttamente esportati in PDF nel file: "+file.getAbsolutePath());
        } catch (IOException e) {
        	printError("Esportazione fallita", "Impossibile esportare i risultati in PDF");
			e.printStackTrace();
		}
	}
	
	/**
	 * Stampa testo all'interno di un file PDF. Stampa il nome del campo in grassetto, dimensione 13, mentre
	 * il contenuto del campo in corsivo, dimensione 13. Utilizzata per stampare Codice, Titolo e Descrizione di ogni scheda.
	 * @param contents Riferimento ai contenuti da stampare sul file PDF
	 * @param fieldName Nome del campo
	 * @param fieldContent Contenuto del campo
	 * @throws IOException Se occorre un'eccezione IOException
	 */
	private void printPDFBallotInfo(PDPageContentStream contents, String fieldName, String fieldContent) throws IOException {
		contents.setFont(PDType1Font.TIMES_BOLD, 13);
        contents.showText(fieldName+": ");
        contents.setFont(PDType1Font.TIMES_ROMAN, 13);
        contents.showText(fieldContent);
        contents.newLine();
	}
	
	/**
	 * Stampa testo all'interno di un file PDF. Stampa i dati passati a parametro nel seguente formato:
	 * <ul>
	 * <li><b>Caso Candidato:</b> [Codice] - [Nome]: [Voti]</li>
	 * <li><b>Caso Opzione:</b> [Nome]: [Voti]</li>
	 * </ul> 
	 * @param contents Riferimento ai contenuti da stampare sul file PDF
	 * @param code Codice del candidato (o stringa vuota per opzione)
	 * @param name Nome del candidato/opzione
	 * @param votes Numero di voti ricevuti
	 * @throws IOException Se occorre un'eccezione IOException
	 */
	private void printPDFVotes(PDPageContentStream contents, String code, String name, int votes) throws IOException {
		String line = code != null ? code + " - " : "";
		line += name + ": " + votes;
    	line += votes == 1 ? " voto" : " voti";
    	contents.showText(line);
    	contents.newLine();
	}
}