package procmgr.model;

import model.AbstrModel;
import model.State.StatePM;

/**
 * Classe Model del modulo ProcedureManager. Contiene tutti i dati permanenti, utili allo svolgimento delle operazioni del modulo, 
 * ed espone i vari getter e setter per interagire con questi.
 * <br>
 * Estende {@link AbstrModel}.
 */
public class ProcedureManager extends AbstrModel {
	private StatePM state;
	
	
	/**
	 * Costruttore di default del model, inizializza lo stato a CONNECTING.
	 */
	public ProcedureManager() {
		state = StatePM.CONNECTING;
	}
	
	/**
	 * Setter per lo stato dell'applicazione.
	 * @param newState Nuovo stato dell'applicazione, sotto forma di {@link model.State.StatePM}
	 */
	public void setState(StatePM state) {
		this.state = state;
	}
	
	/**
	 * Getter per lo stato dell'applicazione.
	 * @return Stato corrente dell'applicazione, sotto forma di {@link model.State.StatePM}
	 */
	public StatePM getState() {
		return state;
	}
}
