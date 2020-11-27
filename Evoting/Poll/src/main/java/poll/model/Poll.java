package poll.model;

import java.util.ArrayList;

import model.AbstrModel;
import model.Procedure;
import model.State.StatePoll;
import poll.view.viewmodel.BallotResult;
//import utils.Logger;

/**
 * Classe Model del modulo Poll. Contiene tutti i dati permanenti, utili allo svolgimento delle operazioni di spoglio (lista di procedure, stato, 
 * dati dell'utente loggato, ecc..), ed espone i vari getter e setter per interagire con questi.
 * <br>
 * Estende {@link AbstrModel}.
 */
public class Poll extends AbstrModel {
	private ArrayList<BallotResult> electoralResults = null;
	private ArrayList<Procedure> procedures = null;
	private StatePoll state = StatePoll.NON_ATTIVO;
	private int procCode = -1;
	
	/**
	 * Costruttore di default del model.
	 */
	public Poll() { 
		//logger = new Logger(true); 
	}
	
	/**
	 * Getter per lo stato dell'applicazione.
	 * @return Stato corrente dell'applicazione, sotto forma di {@link model.State.StatePoll}
	 */
	public StatePoll getState() { 
		return state; 
	}
	
	/**
	 * Setter per lo stato dell'applicazione.
	 * @param newState Nuovo stato dell'applicazione, sotto forma di {@link model.State.StatePoll}
	 */
	public void setState(StatePoll newState) { 
		state = newState;
	}
	
	/**
	 * Getter per l'elenco di procedure.
	 * @return Lista di procedure, sotto forma di ArrayList di {@link model.Procedure}
	 */
	public ArrayList<Procedure> getProcedures() {
		return procedures;
	}
	
	/**
	 * Getter per una specifica procedura all'interno dell'elenco mantenuto nel model.
	 * @param procCode Codice della procedura cercata
	 * @return Procedura cercata sotto forma di {@link model.Procedure} (se esiste), o null
	 */
	public Procedure getProcedure(int procCode) {
		for (Procedure p : procedures)
			if (p.getCode() == procCode)
				return p;
		
		return null;
	}
	
	/**
	 * Setter per l'elenco di procedure.
	 * @param procedures Elenco di procedure, sotto forma di ArrayList di {@link model.Procedure}
	 */
	public void setProcedures(ArrayList<Procedure> procedures) { 
		this.procedures = procedures; 
	}
	
	/**
	 * Getter per il codice della procedura attiva.
	 * @return Codice procedura
	 */
	public int getProcCode() { 
		return procCode; 
	}
	
	/**
	 * Setter per il codice della procedura attiva.
	 * @param procCode Codice della procedura attiva
	 */
	public void setProcCode(int procCode) { 
		this.procCode = procCode; 
	}
	
	/**
	 * Getter per l'elenco di risultati elettorali, sotto forma di ArrayList di {@link poll.view.viewmodel.BallotResult BallotResult}.
	 * @return Elenco di risultati elettorali mantenuto nel model
	 */
	public ArrayList<BallotResult> getElectoralResults() { 
		return electoralResults; 
	}
	
	/**
	 * Setter per l'elenco di risultati elettorali. Prende a parametro la lista di risultati, sotto forma di ArrayList di {@link poll.view.viewmodel.BallotResult BallotResult}
	 * ed aggiorna la lista mantenuta nel model.
	 * @param results Lista di risultati elettorali da memorizzare nel model
	 */
	public void setElectoralResults(ArrayList<BallotResult> results) { 
		electoralResults = results; 
	}
}
