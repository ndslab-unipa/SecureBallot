package poll.view.viewmodel;

/**
 * Classe utilizzata per modellare i dati di un candidato ed i voti da questo ricevuti e visualizzarli all'interno di una tabella JavaFX.
 */
public class CandidateEntry {
	private String code, name;
	private Integer votes;
	
	/**
	 * Costruttore con parametri che inizializza tutti i dati del candidato.
	 * @param code Identificativo del candidato
	 * @param name Nome completo del candidato
	 * @param votes Voti ricevuti dal candidato
	 */
	public CandidateEntry(String code, String name, Integer votes) {
		this.code = code;
		this.name = name;
		this.votes = votes;
	}
	
	/**
	 * Getter per l'identificativo del candidato.
	 * @return Identificativo del candidato
	 */
	public String getCode() {
		return code;
	}
	
	/**
	 * Getter per il nome completo del candidato.
	 * @return Nome completo del candidato
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Getter per il numero di voti ricevuti dal candidato.
	 * @return Numero di voti ricevuti dal candidato
	 */
	public Integer getVotes() {
		return votes;
	}
}
