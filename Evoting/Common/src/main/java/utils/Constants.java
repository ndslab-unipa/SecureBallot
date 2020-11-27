package utils;

/**
 * Classe che contiene le costanti utilizzate dai vari moduli. Utilizzata per fonire dei valori comuni 
 * <br/>
 * <b>Nota:</b> per effettuare test reali e/o vere votazioni, assicurarsi di settare a false la costante <i>devMode</i>.
 */
public class Constants {
	
	/**
	 * Flag che permette di distinguere la modalità developer dalla modalità user. 
	 * In modalità developer il sistema espone più funzionalità e si comporta in maniera leggermente diversa.
	 * <br/>
	 * Ad esempio, i terminali in modalità developer espongono una barra di testo che simula un lettore RFID.
	 */
	public static boolean devMode = true;
	
	/**
	 * Flag che permette di settare la modalità <i>verbose</i> del programma. In modalità verbose, aumenta l'output testuale verso l'utente e in console.
	 */
	public static boolean verbose = false;
	
	/**
	 * IP dell'urna, utilizzato per test in locale. Non è utilizzato dai vari terminali se {@link #devMode} è posta a false.
	 */
	public final static String urnIp = "127.0.0.1";
	
	/**
	 * Porta utilizzata per avviare comunicazioni verso l'urna.
	 */
	public final static int portUrn = 4343;
	
	/**
	 * Porta utilizzata per avviare comunicazioni verso un seggio principale.
	 */
	public final static int portStation = 4344;
	
	/**
	 * Porta utilizzata per avviare comunicazioni verso un seggio ausiliario.
	 */
	public final static int portSubStation = 4346;
	
	/**
	 * Porta utilizzata per avviare comunicazioni verso una postazione.
	 */
	public final static int portPost = 4345;
	
	/**
	 * Stringa contenente tutti i caratteri validi per una chiave di sessione.
	 */
	public final static String sessionKeyAllowedChars = "ABCDEFGJKLMNPQRSTUVWXYZ0123456789";
	
	/**
	 * Lunghezza stabilita per le chiavi di sessione.
	 */
	public final static int sessionKeyLength = 32;
	
	/**
	 * Espressione regolare identificante qualunque chiave di sessione.
	 */
	public final static String sessionKeyPatternMatcher = "^[A-Z0-9]{"+sessionKeyLength+"}$";
	
	/**
	 * Stringa utilizzata come password per confermare la chiusura di un modulo.
	 */
	public final static String exitCode = "adminexit";
	
	// ----------------------------------------------------------------------------------------------------------------------------------
	// Queste ultime costanti hanno il proprio valore in OR con !devMode per assicurarsi che fuori dalla developer mode siano sempre true.
	// ----------------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Flag che permette di settare la comunicazione SSL fra i terminali ed il database. 
	 * <br/>
	 * <b>Nota:</b> Solo Urna, Poll e Procedure Manager comunicano direttamente col DB.
	 */
	public static boolean dbSSL = true || !devMode;
	
	/**
	 * Flag che permette di settare la comunicazione SSL fra i terminali. 
	 */
	public static boolean linkSSL = true || !devMode;
	
	/**
	 * Flag che permette alle postazioni di settare la lettura di schede RFID da un lettore fisico. 
	 */
	public static boolean postRfid = false || !devMode;
	
	/**
	 * Flag che permette ai seggi principali di settare la lettura di schede RFID da un lettore fisico. 
	 */
	public static boolean statRfid = false || !devMode;
	
	/**
	 * Flag che permette ai seggi ausiliari di settare la lettura di schede RFID da un lettore fisico. 
	 */
	public static boolean auxStatRfid = false || !devMode;
}
