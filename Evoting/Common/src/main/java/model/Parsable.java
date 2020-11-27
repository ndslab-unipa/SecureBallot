package model;

import java.io.Serializable;
import java.util.Base64;

import org.apache.commons.lang3.SerializationUtils;

import exceptions.CSTException;
import exceptions.PEException;
/**
 * 
 * @author marco
 * Classe adoperata come base per tutte le classi che modellano oggetti che devono essere trasmessi tramite socket
 * (Liste elettorali, Persone etc).
 */
public class Parsable implements Serializable {
		
	/**
	 * Necessario (a fare che?) per l'interfaccia Serializable.
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Trasforma l'oggetto che la richiama in una stringa Base64 inviabile tramite Scanner.
	 * @return	La stringa convertibile nell'oggetto.
	 */
	public String toB64() {
		String b64 = null;
		
		byte[] bytes = SerializationUtils.serialize(this);
		b64 = new String(Base64.getEncoder().encode(bytes));
		
		return b64;
	}
	
	/**
	 * Parsa la stringa Base64 passata per argomento trasformandola in un Oggetto.
	 * @param b64	La stringa rappresentate l'oggetto.
	 * @return		L'oggetto ottenuto parsando la stringa.
	 * @throws PEException 
	 */
	public static Parsable fromB64(String b64, String sender) throws PEException {
		Parsable obj = null;
		
		try {
			
			byte[] bytes = Base64.getDecoder().decode(b64.getBytes());
			obj = SerializationUtils.deserialize(bytes);
			
		} catch (IllegalArgumentException e) {
			throw CSTException.CST_02(b64, sender, e);
		}
		
		return obj;
	}
}
