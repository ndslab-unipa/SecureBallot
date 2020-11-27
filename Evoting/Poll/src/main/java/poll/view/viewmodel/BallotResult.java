package poll.view.viewmodel;

import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Classe utilizzata per modellare i risultati di una scheda elettorale. Contiene tutti i dati per identificare la scheda (codice, titolo
 * e descrizione) e l'elenco di candidati/opzioni con relative informazioni e numero di voti ricevuti. Inoltre, tiene traccia anche del numero
 * di schede bianche e di preferenze non espresse. 
 */
public class BallotResult {
	private String id = "";
	private String title = "";
	private String description = "";
	private Map<String, CandidateEntry> candidatesVotes = new LinkedHashMap<>();
	private Map<String, OptionEntry> optionsVotes = new LinkedHashMap<>();
	private int nullPreferences = 0, emptyBallots = 0;
	
	/**
	 * Costruttore con parametri che inizializza i dati identificativi della scheda elettorale.
	 * @param id Codice della scheda
	 * @param title Titolo della scheda
	 * @param description Descrizione della scheda
	 */
	public BallotResult(String id, String title, String description) {
		this.id = id;
		this.title = title;
		this.description = description;
	}
	
	/**
	 * Getter per il codice della scheda elettorale.
	 * @return Codice della scheda
	 */
	public String getId() { 
		return id; 
	}
	
	/**
	 * Getter per il titolo della scheda elettorale.
	 * @return Titolo della scheda
	 */
	public String getTitle() { 
		return title; 
	}
	
	/**
	 * Getter per la descrizione della scheda elettorale.
	 * @return Descrizione della scheda
	 */
	public String getDescription() { 
		return description; 
	}
	
	/**
	 * Getter per i risultati dei candidati presenti nella scheda elettorale.
	 * @return Risultati dei candidati per la scheda, sotto forma di Map&lt;String,{@link poll.view.viewmodel.CandidateEntry CandidateEntry}&gt;
	 */
	public Map<String, CandidateEntry> getCandidatesResults() { 
		return candidatesVotes; 
	}
	
	/**
	 * Getter per i risultati delle opzioni presenti nella scheda elettorale.
	 * @return Risultati delle opzioni per la scheda, sotto forma di Map&lt;String,{@link poll.view.viewmodel.OptionEntry OptionEntry}&gt;
	 */
	public Map<String, OptionEntry> getOptionsResults() { 
		return optionsVotes; 
	}
	
	/**
	 * Getter per il numero di preferenze non espresse sulla scheda.
	 * @return Numero di preferenze non espresse
	 */
	public int getNullPreferences() {
		return nullPreferences;
	}

	/**
	 * Setter per il numero di preferenze non espresse sulla scheda.
	 * @param nullPreferences Numero di preferenze non espresse
	 */
	public void setNullPreferences(int nullPreferences) {
		this.nullPreferences = nullPreferences;
	}

	/**
	 * Getter per il numero di schede bianche.
	 * @return Numero di schede bianche
	 */
	public int getEmptyBallots() {
		return emptyBallots;
	}

	/**
	 * Setter per il numero di schede bianche
	 * @param emptyBallots Numero di schede bianche
	 */
	public void setEmptyBallots(int emptyBallots) {
		this.emptyBallots = emptyBallots;
	}
	
	/**
	 * Inserisce un nuovo candidato, ed i relativi voti ricevuti, all'interno dei risultati della scheda elettorale.
	 * I dati del candidato vengono utilizzati per costruire un oggetto di tipo {@link poll.view.viewmodel.CandidateEntry CandidateEntry}.
	 * Cognome e nome vengono combinati in una sola stringa: [Cognome], [Nome].
	 * @param id Identificativo del candidato
	 * @param firstName Nome del candidato
	 * @param lastName Cognome del candidato
	 * @param votesReceived Numero di voti ricevuti
	 */
	public void insertCandidateVotes(String id, String firstName, String lastName, int votesReceived) {
		candidatesVotes.put(id, new CandidateEntry(id, lastName+", "+firstName, votesReceived));
	}
	
	/**
	 * Inserisce una nuova opzione, ed i relativi voti ricevuti, all'interno dei risultati della scheda elettorale.
	 * I dati dell'opzione vengono utilizzati per costruire un oggetto di tipo {@link poll.view.viewmodel.OptionEntry OptionEntry}.
	 * @param option Nome dell'opzione
	 * @param votesReceived Numero di voti ricevuti
	 */
	public void insertOptionVotes(String option, int votesReceived) {
		optionsVotes.put(option, new OptionEntry(option, votesReceived));
	}
}
