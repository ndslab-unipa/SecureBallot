package encryption;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import exceptions.ENCException;
import model.VotePacket;

public class Hash {
	
	/**
	 * Calcola il digest della stringa passata per argomento
	 * @param message	Il messaggio di cui effettuare il digest.
	 * @param size		La dimensione desiderata del digest.
	 * @param name		"Nome" dell'oggetto sul quale effettuare la funzione hash (chiave/iv/password). Necessario per l'eventuale lancio dell'eccezione.
	 * @return
	 * @throws ENCException
	 */
	public static byte[] computeHash(String message, int size, String name) throws ENCException {
		
		try {
	    	MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			byte[] digest = sha256.digest(message.getBytes("UTF-8"));
	        return Arrays.copyOf(digest, size);
		}
		catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
			throw ENCException.ENC_3(name, e);
		}
		
	}

	/**
	 * Verifica se il messaggio produce il digest fornito.
	 * @param message			Messaggio di cui si calcolerà il digest.
	 * @param digestReceived	Digest da confrontare con quello del messaggio.
	 * @param name				"Nome" dell'oggetto della verifica (chiave/iv/password). Necessario per l'eventuale lancio dell'eccezione.
	 * @return					Il risultato della verifica (vero/falso).
	 * @throws ENCException
	 */
	public static boolean verifyHash(String message, byte[] digestReceived, String name) throws ENCException {
		int size = digestReceived.length;
		byte[] realDigest = computeHash(message, size, name);
		
		return java.util.Arrays.equals(realDigest, digestReceived);
	}
	
	/**
	 * Calcola il digest del pacchetto di voto
	 * @param packet 	Il pacchetto di voto di cui calcolare il digest.
	 * @param size		Dimensione del digest desiderata.
	 * @return			Il digest del pacchetto di voto.
	 * @throws ENCException 
	 */
	public static byte[] computeHash(VotePacket packet, int size) throws ENCException {
		String[] unsignedData = packet.getUnsignedData();
		
		try {
	    	MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
	    	
	    	for(String data : unsignedData) {
	    		byte[] bytes = data.getBytes("UTF-8");
	    		sha256.update(bytes, 0, bytes.length);
	    	}
	    	
			byte[] digest = sha256.digest();
	        return Arrays.copyOf(digest, size);
		}
		catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
			throw ENCException.ENC_3("un pacchetto di voto", e);
		}
	}
	
	/**
	 * Verifica che il pacchetto di voto produca il digest ricevuto.
	 * @param packet			Il pacchetto di voto per il quale effettuare la verifica.
	 * @param digestReceived	Il digest da confrontare.
	 * @return					Il risultato della verifica
	 * @throws ENCException
	 */
	public static boolean verifyHash(VotePacket packet, byte[] digestReceived) throws ENCException {
		int size = digestReceived.length;
		
		byte[] realDigest = computeHash(packet, size);
		
		return java.util.Arrays.equals(realDigest, digestReceived);
	}
}
