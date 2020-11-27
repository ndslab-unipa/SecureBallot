package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import exceptions.FLRException;
import exceptions.PEException;

/**
 * Classe utilizzata da tutti i moduli per leggere credenziali salvate all'interno di file <i>.cfg</i> nella cartella <i>src/main/resources/cfg/</i>.
 * <br/>
 * Nota che tutti i file in questa directory presentano la seguente formattazione: <i>key:value</i>.
 */
public class CfgManager {
	
	/**
	 * Legge un file <i>.cfg</i> all'interno della cartella <i>src/main/resources/cfg</i> del modulo da cui è chiamata, quindi restituisce il valore
	 * associato alla chiave passata a parametro, se esiste, altrimenti lancia un'eccezione FLRException.
	 * @param fileName Nome del file a cui si vuole accedere
	 * @param key Chiave associata al valore che si vuole recuperare
	 * @return Il valore corrispondente alla chiave
	 * @throws PEException Se il file non è <i>.cfg</i>, se il file non esiste, se la chiave non esiste 
	 */
	public static String getValue(String fileName, String key) throws PEException {
		if(!fileName.endsWith(".cfg"))
			throw FLRException.FLR_12(0, null);
		
		try (BufferedReader br = new BufferedReader(new FileReader("src/main/resources/cfg/"+fileName))) {
			String line, currKey, currValue;
			
			while ((line = br.readLine()) != null) {
				currKey = line.split(":")[0];
				currValue = line.split(":")[1];
				
				if (currKey.equals(key))
					return currValue;
			}
		} catch (IOException e) {
			throw FLRException.FLR_12(0, e);
		}
		
		if(!fileName.equals("psws.cfg"))
			throw FLRException.FLR_12(-1, null);
		
		switch(key) {
			case "dbu":
			case "dbp":
				throw FLRException.FLR_12(1, null);
				
			case "ks":
			case "ts":
				throw FLRException.FLR_12(2, null);
				
			default:
				throw FLRException.FLR_12(-1, null);
		}
	}
	
	/**
	 * Shortcut per leggere dal file <i>psws.cfg</i>, presente in ogni modulo. Si limita a richiamare {@link #getValue(String, String)}, specificando il file. 
	 * @param key Chiave associata al valore che si vuole recuperare
	 * @return Il valore corrispondente alla chiave
	 * @throws PEException Se il file non esiste, se la chiave non esiste 
	 */
	public static String getPassword(String key) throws PEException {
		return getValue("psws.cfg", key);
	}
}
