package exceptions;

import java.net.InetAddress;

import model.Person;

/**
 * DB_0: "Errore del DB"
 * <br>
 * DB_01: "Errore di connessione al DB"
 * <br>
 * DB_02: "Errore di disconnessione dal DB"
 * <br>
 * DB_03: "Errore del DB non gestito"
 * <br>
 * DB_04: "Presente ip non valido nel DB"
 * <br>
 * DB_05: "Terminale senza seggio"
 * <br>
 * DB_06: "Sessione inesistente"
 * <br>
 * DB_07: "Votante non esistente"
 * <br>
 * DB_08: "Terminale non esistente"
 * <br>
 * DB_09: "Combinazione Seggio-Postazione non valida"
 * <br>
 * DB_10: "Schede non esistenti"
 * <br>
 * DB_11: "Richiesta scheda non esistente"
 * <br>
 * DB_12: "Dati procedura incompleti"
 * <br>
 * DB_13: "Il supervisore non esiste"
 * <br>
 * DB_14: "Operazione di registrazione votante fallita"
 * <br>
 * DB_15: "Operazione di aggiornamento votante fallita"
 * <br>
 */
public class DBException extends PEException {
	private static final long serialVersionUID = 1L;

	protected DBException(Code code, String generic, String specific, Exception e) {
		super(code, generic, specific, e);
	}

	/**
	 * Generic: "Errore nella comunicazione col DB"
	 * <br>
	 * Specific: [Messaggio associato all'eccezione]
	 */
	public static DBException DB_0(Exception e) {
		return new DBException(Code.DB_0, "Errore nella comunicazione col DB", e.getMessage(), e);
	}

	/**
	 * Generic: "Errore di connessione al DB"
	 * <br>
	 * Specific: [Messaggio associato all'eccezione]
	 */
	public static DBException DB_01(Exception e) {
		return new DBException(Code.DB_1, "Errore di connessione al DB", e.getMessage(), e);
	}

	/**
	 * Generic: "Errore di disconnessione dal DB"
	 * <br>
	 * Specific: "Sono sorti problemi durante la disconnesione dal DB"
	 */
	public static DBException DB_02(Exception e) {
		return new DBException(Code.DB_2, "Errore di disconnessione dal DB", "Sono sorti problemi durante la disconnesione dal DB", e);
	}

	/**
	 * Generic: "Errore del DB non gestito"
	 * <br>
	 * Specific: "Errore durante la comunicazione con DB"
	 * @param e La causa dell'eccezione
	 */
	public static DBException DB_03(Exception e) {
		return new DBException(Code.DB_3, "Errore del DB non gestito", "Errore durante la comunicazione con DB", e);
	}
	
	/**
	 * Generic: "Presente ip non valido nel DB"
	 * <br>
	 * Specific: "L'ip:" ip + " recuperato dal database risulta non essere un indirizzo valido"
	 */
	public static DBException DB_04(String ip) {
		String specific = "L'ip:" + ip + " recuperato dal database risulta non essere un indirizzo valido";
		return new DBException(Code.DB_4, "Presente ip non valido nel DB", specific, null);
	}
	
	/**
	 * Generic: "Terminale senza seggio"
	 * <br>
	 * Specific: "Non è stato trovato alcun seggio relativo al terminale " + (postazione/seggio ausiliario) + " con indirizzo ip:"
	 * <br>
	 * @param flag 	Il tipo di terminale true: postazione false: seggio ausiliario.
	 * @param ip	L'indirizzo ip del terminale.
	 */
	public static DBException DB_05(boolean flag, InetAddress ip) {
		String specific = "Non è stato trovato alcun seggio relativo al terminale " + 
				(flag ? "postazione" : "seggio ausiliario") +
				" con indirizzo ip:" + ip.getHostAddress();
		
		return new DBException(Code.DB_5, "Terminale senza seggio", specific, null);
	}
	
	/**
	 * Generic: "Sessione inesistente"
	 * <br>
	 * Specific: "Non esiste alcuna sessione " + sessionCode + " relativa alla procedura " + procedureCode
	 * <br>
	 */
	public static DBException DB_06(int sessionCode, int procedureCode) {
		String specific = "Non esiste alcuna sessione " + sessionCode + " relativa alla procedura " + procedureCode;
		return new DBException(Code.DB_6, "Sessione inesistente", specific, null);
	}
	
	/**
	 * Generic: "Votante non esistente"
	 * <br>
	 * Specific: "Non risulta alcun votante con ID:" + ID
	 */
	public static DBException DB_07(String ID) {
		String specific = "Non risulta alcun votante con ID:" + ID;
		return new DBException(Code.DB_7, "Votante non esistente", specific, null);
	}
	
	/**
	 * Generic: "Terminale non esistente"
	 * <br>
	 * Specific: "Non risulta alcun" + (seggio principale /seggio ausiliario/postazione) + " con ip:" + ip
	 * <br>
	 * @param flag 	0: seggio p. 1: seggio a. 2: post.
	 * @param ip 	L'indirizzo ip del terminale.
	 */
	public static DBException DB_08(int flag, String ip) {
		String specific = "Non risulta alcun";
		
		switch(flag) {
		case 0:
			specific += " seggio principale";
			break;
		case 1:
			specific += " seggio ausiliario";
			break;
		case 2:
			specific += "a postazione";
			break;
		default:
			specific += " terminale";
			break;
		}
		
		specific += " con ip:" + ip;
		return new DBException(Code.DB_8, "Terminale non esistente", specific, null);
	}
	
	/**
	 * Generic: "Combinazione Seggio-Postazione non valida"
	 * <br>
	 * Specific: "La postazione con ip:" + ipPost + " non è tra quelle assegnate al seggio con ip:" + ipStation
	 * <br>
	 */
	public static DBException DB_09(String ipPost, String ipStation) {
		String specific = "La postazione con ip:" + ipPost + " non è tra quelle assegnate al seggio con ip:" + ipStation;
		return new DBException(Code.DB_9, "Combinazione Seggio-Postazione non valida", specific, null);
	}
	
	/**
	 * Generic: "Schede non esistenti"
	 * <br>
	 * Specific: "Non sono state trovate schede per il votante"
	 */
	public static DBException DB_10() {
		return new DBException(Code.DB_10, "Schede non esistenti", "Non sono state trovate schede per il votante", null);
	}
	
	/**
	 * Generic: "Schede non esistenti"
	 * <br>
	 * Specific: "Non sono state trovate schede per il votante " + voter
	 * @param voter Il votante per il quale non risultano schede.
	 */
	public static DBException DB_10(Person voter) {
		String specific = "Non sono state trovate schede per il votante " + voter.getFirstName() + " " + voter.getLastName() + ", ID:" + voter.getID();
		return new DBException(Code.DB_10, "Schede non esistenti", specific, null);
	}
	
	/**
	 * Generic: "Richiesta scheda non esistente"
	 * <br>
	 * Specific: "La scheda " + numBallot + " è stata richiesta ma non risulta esistere per la procedura corrente"
	 */
	public static DBException DB_11(int numBallot) {
		String specific = "La scheda " + numBallot + " è stata richiesta ma non risulta esistere per la procedura corrente";
		return new DBException(Code.DB_11, "Richiesta scheda non esistente", specific, null);
	}
	
	/**
	 * Generic: "Dati procedura incompleti"
	 * <br>
	 * Specific: "Impossibile trovare " + (la chiave pubblica della procedura / una chiave di sessione)
	 * <br>
	 * @param flag Vero: "la chiave pubblica della procedura", falso: "una chiave di sessione"
	 */
	public static DBException DB_12(boolean flag) {
		String specific = "Impossibile trovare " + (flag ? "la chiave pubblica della procedura" : "una chiave di sessione");
		return new DBException(Code.DB_12, "Dati procedura incompleti", specific, null);
	}
	
	/**
	 * Generic: "Il supervisore non esiste"
	 * <br>
	 * Specific: "Impossibile effettuare l'operazione,
	 * <br>
	 * il supervisore " + supervisor + "non esiste"
	 * @param supervisor Il supervisor causa dell'eccezione.
	 */
	public static DBException DB_13(String supervisor) {
		String specific = "Impossibile effettuare l'operazione,\nil supervisore " + supervisor + "non esiste";
		return new DBException(Code.DB_13, "Il supervisore non esiste", specific, null);
	}

	/**
	 * Generic: "Operazione di registrazione votante fallita"
	 * <br>
	 * Specific: "Impossibile registrare il nuovo utente: l'ID " + id + " esiste già"
	 * @param id L'id del votante che ha causato l'eccezione.
	 */
	public static DBException DB_14(String id) {
		String specific = "Impossibile registrare il nuovo utente: l'ID "+ id + " esiste già";
		return new DBException(Code.DB_14, "Operazione di registrazione votante fallita", specific, null);
	}

	/**
	 * Generic: "Operazione di aggiornamento votante fallita"
	 * <br>
	 * Specific: "Impossibile abilitare l'utente al voto,
	 * <br>
	 * l'id "+ id + " non esiste"
	 * @param id	L'id del votante che ha causato l'eccezione.
	 * @param flag 	False se il problema è che il votante non esiste,
	 *              true se il problema è che il votante non può essere modificato perchè ha già votato per questa procedura.
	 */
	public static DBException DB_15(String id, boolean flag) {
		String specific = "Impossibile abilitare l'utente al voto: ";

		if(flag)
			specific += "l'utente risulta aver già votato";
		else
			specific += "l'ID "+ id + " non esiste";

		return new DBException(Code.DB_15, "Operazione di aggiornamento votante fallita", specific, null);
	}
}
