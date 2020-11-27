package exceptions;

import java.net.InetAddress;
import java.util.List;

import model.Person;
import model.State;
import model.Terminals;
import utils.Protocol;

/**
 * FLR_0: "Operazione fallita"
 * <br>
 * FLR_01: "Seggio non attivo"
 * <br>
 * FLR_02: "Nonce non concordati"
 * <br>
 * FLR_03: "Numero di nonce inaspettato"
 * <br>
 * FLR_04: "L'utente sta già votando"
 * <br>
 * FLR_05: "Impossibile prenotare postazione"
 * <br>
 * FLR_06: "Errore durante il login"
 * <br>
 * FLR_07: "Votante non selezionato"
 * <br>
 * FLR_08: "Ricevuti messaggi d’errore"
 * <br>
 * FLR_09: "Discordanza tra messaggio atteso e ricevuto"
 * <br>
 * FLR_10: "Dati mancanti o del tipo errato"
 * <br>
 * FLR_11: "Operazione di voto fallita"
 * <br>
 * FLR_12: "Errore durante la lettura del file di configurazione"
 * <br>
 * FLR_13: "Errore durante l'avvio del programma"
 * <br>
 * FLR_14: "Errore di lettura file"
 * <br>
 * FLR_15: "Documento non specificato"
 * <br>
 * FLR_16: "Impossibile recuperare le chiavi RSA"
 * <br>
 * FLR_17: "Errore nell'IP dell'urna"
 * <br>
 * FLR_18: "Stato Inconsistente"
 * <br>
 * FLR_19: "Errore nelle date della procedura"
 */
public class FLRException extends PEException {
	private static final long serialVersionUID = 1L;

	protected FLRException(Code code, String generic, String specific, Exception e) {
		super(code, generic, specific, e);
	}

	/**
	 * Generic: "Operazione fallita"
	 * <br>
	 * Specific: "Operazione fallita"
	 */
	public static FLRException FLR_0(Exception e) {
		return new FLRException(Code.FLR_0, "Operazione fallita", "Operazione fallita", e);
	}
	
	/**
	 * Generic: "Seggio non attivo"
	 * <br>
	 * Specific: "Impossibile procedere con l'operazione, il seggio non è attivo";
	 * <br>
	 */
	public static FLRException FLR_01() {
		return new FLRException(Code.FLR_1, "Seggio non attivo", "Impossibile procedere con l'operazione, il seggio non è attivo", null);
	}
	
	/**
	 * Generic: "Nonce non concordati"
	 * <br>
	 * Specific: "Non è stato concordato alcun nonce con il terminale " + type + " con ip:" + ipSender 
	 * <br>
	 */
	public static FLRException FLR_02(Terminals.Type type, InetAddress ipSender) {
		String specific = "Non è stato concordato alcun nonce con il terminale " + type + " con ip:" + ipSender.getHostAddress();
		return new FLRException(Code.FLR_2, "Nonce non concordati", specific, null);
	}
	
	/**
	 * Generic: "Numero di nonce non corretto"
	 * <br>
	 * Specific: "Numero di nonce diverso da quello atteso " + (impossibile creare il pacchetto di voto/)
	 * @param flag 
	 * <br>0: impossibile creare il pacchetto di voto;
	 * <br>1: "impossibile accettare schede cifrate"
	 */
	public static FLRException FLR_03(int flag) {
		String specific = "Numero di nonce diverso da quello atteso";
		
		switch(flag) {
		case 0:
			specific += "\nimpossibile creare il pacchetto di voto";
			break;
		case 1:
			specific += "\nimpossibile accettare schede cifrate";
			break;
		}
		
		return new FLRException(Code.FLR_3, "Numero di nonce non corretto", specific, null);
	}
	
	/**
	 * Generic: "L'utente sta già votando"
	 * <br>
	 * Specific: "L'utente " + ID + " sta già votando nella postazione " + post
	 * <br>
	 */
	public static FLRException FLR_04(String ID, int post ) {
		String specific = "L'utente " + ID + " sta già votando nella postazione " + post;
		return new FLRException(Code.FLR_4, "L'utente sta già votando", specific, null);
	}
	
	/**
	 * Generic: "Impossibile prenotare postazione"
	 * <br>
	 * Specific:"Impossibile creare una associazione con le postazioni attualmente disponibili." +
	 * <br>
				"Attendere che si liberi una postazione e riprovare."
	 * <br>
	 */
	public static FLRException FLR_05() {
		String specific = "Impossibile creare una associazione con le postazioni attualmente disponibili." +
				"\nAttendere che si liberi una postazione e riprovare.";
		return new FLRException(Code.FLR_5, "Impossibile prenotare postazione", specific, null);
	}
	
	/**
	 * Generic: "Errore durante il login"
	 * <br>
	 * Specific: "Impossibile effettuare il login: username o password errati"
	 * <br>
	 */
	public static FLRException FLR_06() {
		String specific = "Impossibile effettuare il login: username o password errati";
		return new FLRException(Code.FLR_6, "Errore durante il login", specific, null);
	}

	/**
	 * Generic: "Votante non selezionato"
	 * <br>
	 * Specific: "Impossibile procedere: non è stato selezionato alcun votante"
	 */
	public static FLRException FLR_07() {
		return new FLRException(Code.FLR_7, "Votante non selezionato", "Impossibile procedere: non è stato selezionato alcun votante", null);
	}
	
	/**
	 * Generic: "Ricevuti messaggi d’errore"
	 * <br>
	 * Specific: "Ricevuti i seguenti messaggi di errore da " + sender + ":" + errList
	 * @param sender 	Indirizzo ip del mittente
	 * @param errList	I messaggi di errore ricevuti
	 */
	public static FLRException FLR_08(String sender, List<String> errList) {
		StringBuilder specific = new StringBuilder("Ricevuti i seguenti messaggi di errore da " + sender + ":");
		
		for(String err : errList) {
			specific.append("\n- ").append(err);
		}
		return new FLRException(Code.FLR_8, "Ricevuti messaggi d’errore", specific.toString(), null);
	}
	
	/**
	 * Generic: "Discordanza tra messaggio atteso e ricevuto"
	 * <br>
	 * Specific: "Il messaggio ricevuto da " + sender + " non corrisponde a quello atteso\n\tatteso: " + expected + "\n\tricevuto: " + received
	 * @param sender 	Mittente
	 * @param expected	Il messaggio atteso
	 * @param received 	Il messaggio ricevuto
	 */
	public static FLRException FLR_09(String sender, String expected, String received) {
		String specific = "Il messaggio ricevuto da " + sender + " non corrisponde a quello atteso\n\tatteso: " + expected + "\n\tricevuto: " + received;
		return new FLRException(Code.FLR_9, "Discordanza tra messaggio atteso e ricevuto", specific, null);
	}
	
	/**
	 * Generic: "Dati mancanti o del tipo errato"
	 * <br>
	 * Specific: "Nel messaggio ricevuto da " + sender + " mancano dati e/o sono presenti dati del tipo errato
	 * <br>
	 * mancanti:"
	 * <br> 
	 * + missing + 
	 * <br>
	 * "tipo errato:"
	 * <br>
	 * + wrongType
	 * @param sender 		Mittente
	 * @param missingElements		I dati mancanti
	 * @param wrongTypeElements 	I dati di tipo errato
	 */
	public static FLRException FLR_10(String sender, List<String> missingElements, List<String> wrongTypeElements) {
		String specific = "Dati mancanti o del tipo errato";
		
		StringBuilder typeErr = new StringBuilder("Nel messaggio inviato da " + sender + " i seguenti elementi sono del tipo sbagliato:");
		for (String wrongType : wrongTypeElements)
			typeErr.append("\n\t").append(wrongType);
		
		StringBuilder missErr = new StringBuilder("Nel messaggio inviato da " + sender + " mancano i seguenti dati necessari:");
		for (String miss : missingElements)
			missErr.append("\n\t").append(miss);
				
		if (!missingElements.isEmpty() && !wrongTypeElements.isEmpty())
			specific = missErr + "\nInoltre,\n" + typeErr;
		else {
			if (!missingElements.isEmpty())
				specific = missErr.toString();
			
			if (!wrongTypeElements.isEmpty())
				specific = typeErr.toString();
		}
		
		return new FLRException(Code.FLR_10, "Dati mancanti o del tipo errato", specific, null);
	}
	
	/**
	 * Generic: "Operazione di voto fallita"
	 * <br>
	 * Specific: "L'utente " + voter + motivo del fallimento operazione
	 * <br>
	 * @param voter Il votante per il quale non si è potuto registrare il voto
	 * @param flag	Intero che specifica la causa
	 * <br>		0: " ha gia votato";
	 * <br>		1: " non è abilitato al voto per la procedura attuale";
	 * <br>		2: " non può registrare il voto per via di una incongruenza tra schede presentate e quelle per cui era abilitato";
	 * <br>		default: " ha gia votato o non è abilitato al voto per la procedura attuale";
	 */
	public static FLRException FLR_11(Person voter, int flag) {
		String specific = "L'utente " + voter.getFirstName() + " " + voter.getLastName() + ", ID:" + voter.getID();
		
		switch(flag) {
		case 0: 
			specific += " ha gia votato";
			break;
		case 1:
			specific += " non è abilitato al voto per la procedura attuale";
			break;
		case 2:
			specific += " non può registrare il voto per via di una incongruenza tra schede presentate e quelle per cui era abilitato";
			break;
		default:
			specific += " ha gia votato o non è abilitato al voto per la procedura attuale";
		}
		
		return new FLRException(Code.FLR_11, "Operazione di voto fallita", specific, null);
	}
	
	/**
	 * Generic: "Errore durante la lettura del file di configurazione"
	 * <br>
	 * Specific: [Causa]
	 * <br>
	 * @param flag 0: file inesistente; 1: credenziali DB assenti; 2: credenziali SSL assenti; Altro: parametro inesistente.
	 */
	public static FLRException FLR_12(int flag, Exception cause) {
		String specific;
		
		switch(flag) {
			case 0:
				specific = "Impossibile leggere il file";
				break;
			case 1:
				specific = "Non sono state trovate le credenziali per l'accesso al DB";
				break;
			case 2:
				specific = "Non sono state trovate le credenziali dei keystores SSL";
				break;
			default:
				specific = "Non esiste alcun parametro corrispondente alla richiesta";
		}
		
		return new FLRException(Code.FLR_12, "Errore durante la lettura del file di configurazione", specific, cause);
	}
	
	/**
	 * Generic: "Errore durante l'avvio del programma"
	 * <br>
	 * Specific: "Impossibile accedere al keystore (file inesistente o password errata)" / "Impossibile creare la server socket"
	 * <br>
	 * @param flag True: "Impossibile accedere al keystore (file inesistente o password errata)"; False: "Impossibile creare la server socket".
	 */
	public static FLRException FLR_13(boolean flag, Exception cause){
		String specific = flag ? "Impossibile accedere al keystore (file inesistente o password errata)." : "Impossibile creare la server socket.";
		return new FLRException(Code.FLR_13, "Errore durante l'avvio del programma", specific, cause);
	}

	/**
	 * Generic: "Errore durante l'avvio del programma"
	 * <br>
	 * Specific: "Un altro programma sta adoperando la porta " + port
	 * <br>
	 * @param port La porta su cui non è stato possibile mettersi in ascolto.
	 */
	public static FLRException FLR_13(int port, Exception cause){
		return new FLRException(Code.FLR_13, "Errore durante l'avvio del programma", "Un altro programma sta adoperando la porta " + port + ".", cause);
	}

	/**
	 * Generic: "Errore di lettura file"
	 * <br>
	 * Specific: "Impossibile trovare il file"/"Errore durante la lettura del file"
	 * <br>
	 * @param path		Percorso del file.
	 * @param during 	Indica se il problema si verifica prima (false) o durante (true) la lettura del file.
	 * @param cause		Causa dell'eccezione.
	 */
	public static FLRException FLR_14(String path, boolean during, Exception cause){
		String specific = (during ? "Errore durante la lettura del file" : "Impossibile trovare il file") + ": " + path;
		return new FLRException(Code.FLR_14, "Errore di lettura file", specific, cause);
	}

	/**
	 * Generic: "Documento non specificato"
	 * <br>
	 * Specific: "Non è stato indicato il tipo di documento"/"Non è stato indicato il codice del documento"
	 * <br>
	 * @param voter Il votante per cui si è verificato l'errore.
	 * @param typeOrCode Flag che indica se si tratta del tipo di documento (false) o del codice del documento (true).
	 */
	public static FLRException FLR_15(Person voter, boolean typeOrCode){
		String voterInfo = voter.getFirstName() + " " + voter.getLastName() + ", " + voter.getID();
		String specific = "Non è stato indicato " + (typeOrCode ? "il codice del documento" : "il tipo di documento") + " per il votante " + voterInfo;
		return new FLRException(Code.FLR_15, "Documento non specificato", specific, null);
	}
	
	/**
	 * Generic: "Impossibile recuperare le chiavi RSA"
	 * <br>
	 * Specific: "Le seguenti chiavi RSA, appartenenti a "+owner+" non sono state recuperate correttamente dal DB:" + pubKey + prvKey
	 * <br>
	 * @param owner Il nome del responsabile di cui non si sono recuperate le chiavi RSA
	 * @param pubKey Flag per indicare l'impossibilità di recupero della chiave pubblica
	 * @param prvKey Flag per indicare l'impossibilità di recupero della chiave privata
	 */
	public static FLRException FLR_16(String owner, boolean pubKey, boolean prvKey) {
		String generic = "Impossibile recuperare le chiavi RSA";
		String specific = "Le seguenti chiavi RSA, appartenenti a "+owner+", non sono state recuperate correttamente dal DB:";
		specific += pubKey ? "\n- Chiave Pubblica" : "";
		specific += prvKey ? "\n- Chiave Privata" : "";
		return new FLRException(Code.FLR_16, generic, specific, null);
	}
	
	/**
	 * Generic: "Errore nell'IP dell'urna"
	 * <br>
	 * Specific: "La stringa letta non è interpretabile come IP: "+ip
	 * 
	 */
	public static FLRException FLR_17(String ip) {
		return new FLRException(Code.FLR_17, "Errore nell'IP dell'urna", "La stringa letta non è interpretabile come IP: "+ip, null);
	}
	
	/**
	 * Generic: "Stato Inconsistente: Necessario Riavvio" OPPURE "Stato Inconsistente: Necessario Reset/Riavvio"
	 * <br>
	 * Specific: "La postazione [IP: "+terminalIp+"] è tornata raggiungibile in uno stato inconsistente con l'ultimo noto."
	 * + "Variazione di Stato: " + oldState + " -> " + newState;
	 * + "Variazione di Badge: " + oldBadgeStr + " -> " + newBadgeStr;
	 * @param terminalIp IP del terminale
	 * @param oldState Ultimo stato noto del terminale
	 * @param oldBadge Ultimo badge associato noto
	 * @param newState Stato corrente del terminale
	 * @param newBadge Badge corrente associato alla postazione
	 * @param needReboot Booleano per indicare se la postazione ha bisogno di riavvio o se basta un reset
	 */
	public static FLRException FLR_18(String terminalIp, State.StatePost oldState, String oldBadge, State.StatePost newState, String newBadge, boolean needReboot) {
		String oldBadgeStr = oldBadge.equals(Protocol.unassignedPost) ? "(N/A)" : oldBadge;
		String newBadgeStr = newBadge.equals(Protocol.unassignedPost) ? "(N/A)" : newBadge;
		
		String generic = "Stato Inconsistente: ";
		generic +=  needReboot ? "Necessario Riavvio" : "Necessario Reset/Riavvio";
		
		String specific = "La postazione [IP: "+terminalIp+"] è tornata raggiungibile in uno stato inconsistente con l'ultimo noto.";
		specific += "\nVariazione di Stato: " + oldState + " -> " + newState;
		specific += "\nVariazione di Badge: " + oldBadgeStr + " -> " + newBadgeStr;
		
		return new FLRException(Code.FLR_18, generic, specific, null);
	}
	
	/**
	 * Generic: "Errore nelle date della procedura"
	 * <br>
	 * Specific: "La data di fine procedura è precedente alla data di inizio"
	 */
	public static FLRException FLR_19() {
		return new FLRException(Code.FLR_19, "Errore nelle date della procedura", "La data di fine procedura è precedente alla data di inizio", null);
	}
}
