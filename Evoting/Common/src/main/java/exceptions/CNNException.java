package exceptions;

/**
 * CNN_0: "Errore di connessione"
 * <br>
 * CNN_1: "Destinazione irragiungibile"
 * <br>
 * CNN_2: "Destinazione non attiva"
 * <br>
 * CNN_3: "Comunicazione interrotta"
 */
public class CNNException extends PEException {
	private static final long serialVersionUID = 1L;
	
	private CNNException(Code code, String generic, String specific, Exception e) {
		super(code, generic, specific, e);
	}
	
	/**
	 * Generic: "Errore di connessione"
	 * <br>
	 * Specific: "Errore di connessione"
	 */
	public static CNNException CNN_0(Exception e) {
		return new CNNException(Code.CNN_0, "Errore di connessione", "Errore di connessione", e);
	}
	
	/**
	 * Generic: "Destinazione irraggiungibile"
	 * <br>
	 * Specific: "Non è stato possibile contattare il terminale " + type +
	 * <br>
	 * "all'indirizzo " + ipRecipient
	 * @param type 			Stringa relativa al terminale (postazione num/ seggio/ seggio ausiliario/ urna)
	 * @param ipRecipient 	Indirizzo ip del terminale
	 * @param e				Causa dell'eccezione
	 * @return
	 */
	public static CNNException CNN_1(String type, String ipRecipient, Exception e) {
		String specific = "Non è stato possibile contattare il terminale " + type;
		specific += ipRecipient != null ? " all'indirizzo " + ipRecipient : "";
		return new CNNException(Code.CNN_1, "Destinazione irraggiungibile", specific, e);
	}
	
	/**
	 * Generic: "Destinazione irraggiungibile"
	 * <br>
	 * Specific: "Non è stato possibile contattare il terminale " + type +
	 * <br>
	 * "all'indirizzo " + ipRecipient + 
	 * <br> 
	 * otherInfo
	 * @param type 					Stringa relativa al terminale (postazione num/ seggio/ seggio ausiliario/ urna)
	 * @param ipRecipient 			Indirizzo ip del terminale
	 * @param otherInfo 			Altre informazioni opzionali che si vuole aggiungere
	 * @param e						Causa dell'eccezione
	 * @return
	 */
	public static CNNException CNN_1(String type, String ipRecipient, String otherInfo, Exception e) {
		String specific = "Non è stato possibile contattare il terminale " + type +
				"\nall'indirizzo " + ipRecipient + "\n" + otherInfo;
		return new CNNException(Code.CNN_1, "Destinazione irraggiungibile", specific, e);
	}
	
	/**
	 * Generic: "Destinazione non attiva"
	 * <br>
	 * Specific: "Il terminale " + type + " non è attivo"
	 */
	public static CNNException CNN_2(String type) {
		String specific = "Il terminale " + type + " non è attivo";
		return new CNNException(Code.CNN_2, "Destinazione non attiva", specific, null);
	}
	
	/**
	 * Generic: "Comunicazione interrotta"
	 * <br>
	 * Specific: "Il terminale " + type + " con ip " + ipRecipient + " ha interrotto la comunicazione"
	 * @param type 				Stringa relativa al terminale (postazione num/ seggio/ seggio ausiliario/ urna)
	 * @param ipRecipient 		Indirizzo ip del terminale
	 */
	public static CNNException CNN_3(String type, String ipRecipient) {
		String specific = "Il terminale " + type + " con ip " + ipRecipient + " ha interrotto la comunicazione";
		return new CNNException(Code.CNN_3, "Comunicazione interrotta", specific, null);
	}
	
	/**
	 * Generic: "Comunicazione interrotta"
	 * <br>
	 * Specific: "Il terminale " + type + " con ip " + ipRecipient + " ha interrotto la comunicazione" +
	 * <br> otherInfo
	 * @param type 					Stringa relativa al terminale (postazione num/ seggio/ seggio ausiliario/ urna)
	 * @param ipRecipient 			Indirizzo ip del terminale
	 * @param otherInfo 			Altre informazioni opzionali che si vuole aggiungere
	 */
	public static CNNException CNN_3(String type, String ipRecipient, String otherInfo) {
		String specific = "Il terminale " + type + " con ip " + ipRecipient + " ha interrotto la comunicazione\n" + otherInfo;
		return new CNNException(Code.CNN_3, "Comunicazione interrotta", specific, null);
	}
}
