package exceptions;

/**
 * CST_01: "Tipo non supportato"
 * <br>
 * CST_03: "Stringa non interpretabile come Parsable"
 */
public class CSTException extends PEException {
	private static final long serialVersionUID = 1L;
	
	private CSTException(Code code, String generic, String specific, Exception cause) {
		super(code, generic, specific, cause);
	}
	
	/**
	 * Generic: "Errore di Cast"
	 * <br>
	 * Specific: "Errore di Cast"
	 * <br>
	 */
	public static CSTException CST_0(Exception e) {
		return new CSTException(Code.CST_0, "Errore di Cast", "Errore di Cast", e);
	}
	
	/**
	 * Generic: "Tipo non supportato"
	 * <br>
	 * Specific: "Una variabile di tipo " + type + " è stata richiamata in un punto del codice dove il tipo " + type + " non è supportato"
	 * <br>
	 */
	public static CSTException CST_01(Class<?> type) {
		String specific = "Una variabile di tipo " + type + " è stata richiamata in un punto del codice dove il tipo " + type + " non è supportato";
		return new CSTException(Code.CST_1, "Tipo non supportato", specific, null);
	}
	/**
	 * Generic: "Stringa non interpretabile come Parsable"
	 * <br>
	 * Specific: "La stringa " + str + "inviata da " + sender + " non è interpretabile come parsable"
	 * <br>
	 * @param str 		La stringa che non si è riusciti a convertire
	 * @param sender	Il mittente del messaggio
	 * @param e			La causa dell'eccezione
	 */
	public static CSTException CST_02(String str, String sender, Exception e) {
		String specific = "La stringa " + str + "inviata da " + sender + " non è interpretabile come parsable";
		return new CSTException(Code.CST_2, "Stringa non interpretabile come Parsable", specific, e);
	}
}
