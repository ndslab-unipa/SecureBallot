package model;
/**
 * 
 * @author marco
 * Contiene tutti gli stati possibili dei terminali.
 */
public class State {
	public enum StateStation{
		NON_ATTIVO, 	//Inattivo, in attesa che l'urna confermi la chiave di sessione di voto.
		ATTIVO,			//Attivo, adoperabile dalla commissione.
		OFFLINE			//Il seggio non è connesso alla rete.
	}
	
	public enum StateSubStation{
		NON_ATTIVO, 	//Inattivo, in attesa che l'urna confermi la chiave di sessione di voto.
		IN_ATTESA,		//In attesa che il seggio ausiliario venga attivato.
		ATTIVO,			//Attivo, adoperabile dalla commissione.
		OFFLINE			//Il seggio non è connesso alla rete.
	}
	
	public enum StatePost {
		NON_ATTIVA, 	//Inattiva, in attesa che l'urna confermi la chiave di sessione di voto.
		ATTIVA, 		//Attiva, in attesa che il seggio comunichi una associazione.
		ASSOCIATA, 		//Associata, in attesa che il votante passi il badge.
		IN_USO, 		//In uso, in attesa che il votante termini e confermi.
		VOTO_PENDING,
		VOTO_INVIATO,	//Libera, in attesa che il votante restituisca il badge alla commissione.
		OFFLINE,		//La postazione non è connessa alla rete.
		DA_RESETTARE,
		DA_RIAVVIARE
	}
	
	public enum StatePM {
		CONNECTING,		//Il sistema deve ancora connettersi con il Database
		NO_ROOT,		//Non esiste alcun utente root necessario per la creazione di altri utenti.
		LOGIN,			//Si sta attendendo il login dell'utente
		ROOT,			//Effettuato login come tecnico root
		TECHNIC,		//Effettuato login come tecnico non root
		SUPERVISOR		//Effettuato login come supervisore
	}
	
	public enum StatePoll {
		NON_ATTIVO, 	//In attesa di login
		ATTIVO,			//Login superato, in attesa di selezione della procedura
		COUNTING		//Spoglio in corso
	}
	
	public enum StateUrn {
		NON_ATTIVA,		//In attesa di login
		ATTIVA,			//Login superato, in attesa di selezione della procedura
		LOGGING			//Procedura selezionata ed avviata, mostrando a schermo log dell'attività
	}
	
	public static StatePost getStatePostFromString(String stateAsString) {
		
		for(StatePost state : StatePost.values()) {
			if(state.toString().equals(stateAsString)) {
				return state;
			}
		}
		
		return null;
	}
}
