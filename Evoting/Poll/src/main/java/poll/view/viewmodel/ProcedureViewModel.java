package poll.view.viewmodel;

import java.time.LocalDateTime;

import model.Procedure;

/**
 * Classe che modella la struttura dati richiesta dalla tabella delle procedure, mostrata durante la scena ProcedureSelection.
 * Estende {@link model.Procedure Procedure}.
 */
public class ProcedureViewModel extends Procedure {
	private boolean selected = false;
	
	/**
	 * Costruttore con parametri che inizializza tutti i dati richiesti da una Procedure.
	 * @param procCode Codice procedura
	 * @param procName Nome procedura
	 * @param procSupervisor Supervisore della procedura
	 * @param start Data di inizio procedura
	 * @param end Data di fine procedura
	 * @param validity Flag vero/falso a seconda se la procedura è terminata o no
	 */
	public ProcedureViewModel(int procCode, String procName, String procSupervisor, LocalDateTime start, LocalDateTime end, boolean validity) {
		super(procCode, procName, procSupervisor, start, end, validity);
	}
	
	/**
	 * Costruttore con parametri che prende una Procedure e la usa per inizializzare tutti i dati richiesti.
	 * @param p
	 */
	public ProcedureViewModel(Procedure p) {
		super(p.getCode(), p.getName(), p.getSupervisor(), p.getStart(), p.getEnd(), p.getTerminated());
	}
	
	/**
	 * Getter per il booleano selected.
	 * @return True se la procedura è selezionata nella tabella, false altrimenti
	 */
	public boolean isSelected() { 
		return selected; 
	}
	
	/**
	 * Setter per il booleano selected.
	 * @param newValue Nuovo valore per selected.
	 */
	public void setSelected(boolean newValue) { 
		selected = newValue;
	}
}
