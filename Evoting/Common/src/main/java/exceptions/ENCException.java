package exceptions;

/**
 * ENC_0: "Errore di cifratura"
 * <br>
 * ENC_1: "Errore di cifratura simmetrica"
 * <br>
 * ENC_2: "Errore di decifratura simmetrica"
 * <br>
 * ENC_3: "Errore di hashing"
 * <br>
 * ENC_4: "Errore HMAC"
 * <br>
 * ENC_5: "Errore RSA"
 * <br>
 * ENC_6: "Chiave di sessione errata"
 * <br>
 * ENC_7: "Errore lettura chiavi asimmetriche"
 * <br>
 * ENC_8: "Errore nella verifica della firma"
 * <br>
 */
public class ENCException extends PEException {
	
	private static final long serialVersionUID = 1L;
	
	private ENCException(Code code, String generic, String specific, Exception e) {
		super(code, generic, specific, e);
	}
	
	/**
	 * Generic: "Errore di cifratura"
	 * <br>
	 * Specific: "Errore di cifratura"
	 */
	public static ENCException ENC_0(Exception e) {
		return new ENCException(Code.ENC_0, "Errore di cifratura", "Errore di cifratura", e);
	}
	
	/**
	 * Generic: "Errore di cifratura simmetrica"
	 * <br>
	 * Specific: "Impossibile cifrare " + (voto/chiave privata/nonce)
	 */
	public static ENCException ENC_1(String arg1, Exception e) {
		String specific = "Impossibile cifrare " + arg1;
		return new ENCException(Code.ENC_1, "Errore di cifratura simmetrica", specific, e);
	}
	
	/**
	 * Generic: "Errore di decifratura simmetrica"
	 * <br>
	 * Specific: "Impossibile decifrare " + (voto/chiave privata/nonce)
	 */
	public static ENCException ENC_2(String arg1, Exception e) {
		String specific = "Impossibile decifrare " + arg1;
		return new ENCException(Code.ENC_2, "Errore di decifratura simmetrica", specific, e);
	}
	
	/**
	 * Generic: "Errore di hashing"
	 * <br>
	 * Specific: "Impossibile effettuare l'hash di " + (chiave/digest/iv)
	 */
	public static ENCException ENC_3(String arg1, Exception e) {
		String specific = "Impossibile effettuare l'hash di " + arg1;
		return new ENCException(Code.ENC_3, "Errore di hashing", specific, e);
	}
	
	/**
	 * Generic: "Errore HMAC"
	 * <br>
	 * Specific: "Impossibile (generare la chiave/effettuare il digest) HMAC"
	 * @param flag 	True: "generare la chiave"; false: "calcolare il digest"
	 * @param e		Causa dell'eccezione
	 */
	public static ENCException ENC_4(boolean flag, Exception e) {
		String specific = "Impossibile " + (flag ? "generare la chiave" : "calcolare il digest") + " HMAC";
		return new ENCException(Code.ENC_4, "Errore HMAC", specific, e);
	}
	
	/**
	 * Generic: "Errore RSA"
	 * <br>
	 * Specific: "Errore RSA relativo a " + (cifratura/decifratura/chiavi)
	 * @param flag 	0: cifratura; 1: decifratura; 2: chiavi
	 * @param e 	Causa dell'eccezione
	 */
	public static ENCException ENC_5(int flag, Exception e) {
		String specific = "Errore RSA relativo a ";
		
		switch(flag) {
		case 0:
			specific += "cifratura";
			break;
		case 1:
			specific += "decifratura";
			break;
		case 2:
			specific += "chiavi";
			break;
		default:
			specific = "Errore RSA";
		}
		
		return new ENCException(Code.ENC_5, "Errore RSA", specific, e);
	}
	
	/**
	 * Generic: "Chiave di sessione errata"
	 * <br>
	 * Specific: "La chiave di sessione immessa potrebbe essere errata"
	 */
	public static ENCException ENC_6(Exception e) {
		String specific = "La chiave di sessione immessa potrebbe essere errata";
		return new ENCException(Code.ENC_6, "Chiave di sessione errata", specific, e);
	}
	
	/**
	 * Generic: "Errore lettura chiavi asimmetriche"
	 * <br>
	 * Specific: "Impossibile leggere la chiave " + (pubblica/privata)
	 */
	public static ENCException ENC_7(String arg1, Exception e) {
		String specific = "Impossibile leggere la chiave " + arg1;
		return new ENCException(Code.ENC_7, "Errore lettura chiavi asimmetriche", specific, e);
	}
	
	/**
	 * Generic: "Errore nella verifica della firma"
	 * <br>
	 * Specific: "Impossibile verificare la firma apposta sul pacchetto di voto" / [Messaggio dell'eccezione specifica]
	 */
	public static ENCException ENC_8(Exception e) {
		String specific = "Impossibile verificare la firma apposta sul pacchetto di voto";
		return new ENCException(Code.ENC_8, "Errore nella verifica della firma", e != null ? e.getMessage() : specific, e);
	}
	
}
