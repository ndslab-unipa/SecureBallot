package exceptions;

import java.net.InetAddress;

import model.Person;
import model.Terminals;

/**
 * DEV_0: "Errore nel codice"
 * <br>
 * DEV_01: "Ip seggio mancante"
 * <br>
 * DEV_03: "Dati mancanti"
 * <br>
 * DEV_04: "Tipo di terminale non riconosciuto"
 * <br>
 * DEV_05: "Votazione in corso"
 * <br>
 * DEV_06: "Challenge nonce inesistente"
 * <br>
 * DEV_08: "Tipo utente inesistente"
 * <br>
 * DEV_09: "Errore di comunicazione col DB"
 * <br>
 * DEV_10: "Nome del terminale errato"
 * <br>
 */
public class DEVException extends PEException {
	private static final long serialVersionUID = 1L;

	protected DEVException(Code code, String generic, String specific, Exception e) {
		super(code, generic, specific, e);
	}
	
	/**
	 * Generic: "Errore nel codice"
	 * <br>
	 * Specific: "Errore nel codice"
	 * <br>
	 * @param e La causa dell'eccezione.
	 * @return
	 */
	public static DEVException DEV_0(Exception e) {
		return new DEVException(Code.DEV_0, "Errore nel codice", "Errore nel codice", e);
	}
	
	/**
	 * Generic: "Ip seggio mancante"
	 * <br>
	 * Specific: "Impossibile contattare il seggio senza conoscerne l'ip"
	 * @return
	 */
	public static DEVException DEV_01() {
		String specific = "Impossibile contattare il seggio senza conoscerne l'IP";
		return new DEVException(Code.DEV_1, "IP Seggio Mancante", specific, null);
	}
	
	/**
	 * Generic: "Dati mancanti"
	 * <br>
	 * Specific: "Il terminale non può effettuare l'operazione senza " + (chiave di cifratura/chiave di sessione)
	 * @param obj 	(chiave di cifratura/chiave di sessione)
	 * @return
	 */
	public static DEVException DEV_03(String obj) {
		String specific = "Il terminale non può effettuare l'operazione senza " + obj;
		return new DEVException(Code.DEV_3, "Dati mancanti", specific, null);
	}
	
	/**
	 * Generic: "Dati mancanti"
	 * <br>
	 * Specific: "L'operazione non può essere effettuata senza " + (chiave di cifratura/chiave di sessione)  + " del teminale " + type + " con ip:" + ip
	 * @param obj 	(chiave di cifratura/chiave di sessione)
	 * @param type 	Il tipo di terminale (postazione/seggio/seggio ausiliario)
	 * @param ip	L'ip del terminale
	 */
	public static DEVException DEV_03(String obj, Terminals.Type type, InetAddress ip) {
		String specific = "L'operazione non può essere effettuata senza " + obj + " del teminale " + type + " con ip:" + ip.getHostAddress();
		return new DEVException(Code.DEV_3, "Dati mancanti", specific, null);
	}
	
	/**
	 * Generic: "Tipo di terminale non riconosciuto"
	 * <br>
	 * Specific: "Il tipo di terminale richiesto (" + type + ") non fa parte di quelli validi"
	 * <br>
	 */
	public static DEVException DEV_04(Terminals.Type type) {
		String specific = "Il tipo di terminale richiesto (" + type + ") non fa parte di quelli validi";
		return new DEVException(Code.DEV_4, "Tipo di terminale non riconosciuto", specific, null);
	}
	
	/**
	 * Generic: "Votazione in corso"
	 * <br>
	 * Specific: "Impossibile procedere, votazione del votante " + voter + " ancora in corso"
	 */
	public static DEVException DEV_05(Person voter) {
		String specific = "Impossibile procedere, votazione del votante " + 
	voter.getFirstName() + " " + voter.getLastName() + ", ID:" + voter.getID() + 
	" ancora in corso";
		return new DEVException(Code.DEV_5, "Votazione in corso", specific, null);
	}
	
	/**
	 * Generic: "Votazione in corso"
	 * <br>
	 * Specific: "Impossibile procedere, votazione ancora in corso"
	 */
	public static DEVException DEV_05() {
		String specific = "Impossibile procedere, votazione ancora in corso";
		return new DEVException(Code.DEV_5, "Votazione in corso", specific, null);
	}
	
	/**
	 * Generic: "Challenge nonce inesistente"
	 * <br>
	 * Specific: "La challenge nonce " + challenge + " non esiste"
	 * @param challenge 	Challenge richiesta
	 * @param e				Causa dell'eccezione
	 */
	public static DEVException DEV_06(int challenge, Exception e) {
		String specific = "La challenge nonce " + challenge + " non esiste";
		return new DEVException(Code.DEV_6, "Challenge nonce inesistente", specific, e);
	}
	
	/**
	 * Generic: "Card RFID illegibile"
	 * <br>
	 * Specific: "Impossibile leggere la card RFID " + otherInfo 
	 */
	public static DEVException DEV_07(String otherInfo) {
		String specific = "Impossibile leggere la card RFID ";
		if(otherInfo != null) {
			specific += otherInfo;
		}
		return new DEVException(Code.DEV_7, "Card RFID illegibile", specific, null);
	}
	
	/**
	 * Generic: "Tipo utente inesistente"
	 * <br>
	 * Specific: "La tipologia di utente " + type + " non esiste"
	 * <br>
	 * @param type
	 * @return
	 */
	public static DEVException DEV_08(String type) {
		String specific = "La tipologia di utente " + type + " non esiste";
		return new DEVException(Code.DEV_8, "Tipo utente inesistente", specific, null);
	}
	
	/**
	 * Generic: "Errore di comunicazione col DB"
	 * <br>
	 * Specific: "Impossibile comunicare con DB prima di aver stabilito una connessione"
	 */
	public static DEVException DEV_09() {
		return new DEVException(Code.DEV_9, "Errore di comunicazione col DB", "Impossibile comunicare con DB prima di aver stabilito una connessione", null);
	}
	
	/**
	 * Generic: "Nome del terminale errato"
	 * <br>
	 * Specific: "Il nome " + name + " non corrisponde ad alcun tipo di terminale noto"
	 */
	public static DEVException DEV_10(String name) {
		return new DEVException(Code.DEV_10, "Nome del terminale errato", "Il nome " + name + " non corrisponde ad alcun tipo di terminale noto", null);
	}
}
