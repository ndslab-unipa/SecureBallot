package poll.view.viewmodel;

/**
 * Classe utilizzata per modellare i dati di un'opzione ed i voti da questa ricevuti e visualizzarli all'interno di una tabella JavaFX.
 */
public class OptionEntry {
	private String option;
	private Integer votes;
	
	/**
	 * Costruttore con parametri che inizializza tutti i dati dell'opzione.
	 * @param option Nome dell'opzione
	 * @param votes Numero di voti ricevuti dall'opzione
	 */
	public OptionEntry(String option, Integer votes) {
		this.option = option;
		this.votes = votes;
	}

	/**
	 * Getter per il nome dell'opzione.
	 * @return Nome dell'opzione
	 */
	public String getOption() {
		return option;
	}
	
	/**
	 * Getter per il numero di voti ricevuti dall'opzione.
	 * @return Numero di voti ricevuti dall'opzione
	 */
	public Integer getVotes() {
		return votes;
	}
}
