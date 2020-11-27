package db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import encryption.AES;
import encryption.Hash;
import exceptions.DBException;
import exceptions.PEException;

//TODO: Trovare un nome decente
public class DB {
	protected DBMS dbms;
	
	/**
	 * Funzione che controlla che i dati ricevuti (username e password) corrispondano ad un qualche utente sul DB. Della password viene calcolato il digest
	 * tramite {@link encryption.Hash#computeHash(String, int, String) computeHash}. 
	 * @param user Username da verificare
	 * @param psw Password da verificare
	 * @return True o False a seconda dell'esito della verifica
	 * @throws PEException
	 */
	public boolean checkLoginData(String user, String psw) throws PEException {
		byte[] typedHashedPsw = Hash.computeHash(psw, 16, "password");
		byte[] dbHashedPsw = null;
		
		String query =  "SELECT HashedPassword AS HP " + 
    					"FROM Staff " +
    					"WHERE UserName = ? ;";
        
        try (ConnectionManager cManager = dbms.getConnectionManager()) {
			ResultSet rs = cManager.executeQuery(query, user);
			
			if(rs.next()) {
				dbHashedPsw = rs.getBytes("HP");
			}
			else
				return false;
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
        
		return Arrays.equals(dbHashedPsw, typedHashedPsw);
	}
	
	/**
	 * Funzione che recupera una chiave RSA dal DB, dati l'username e la password dell'utente corrispondente.
	 * @param key Nome della colonna corrispondente alla chiave desiderata (Uno fra PublicKey1, PublicKey2, EncryptedPrivateKey1, EncryptedPrivateKey2)
	 * @param user Username dell'utente
	 * @param psw Password dell'utente
	 * @return Chiave RSA, sotto forma di array di byte
	 * @throws PEException
	 */
	 public byte[] getRSAKey(String key, String user, String psw) throws PEException {
		byte[] rsaKey = null;
		
		List<String> keys = List.of("PublicKey1", "PublicKey2", "EncryptedPrivateKey1", "EncryptedPrivateKey2");
		if (!keys.contains(key))
			return null;
		
		String query = "SELECT "+key+" AS RSAKEY "
					+  "FROM Staff "
				    +  "WHERE UserName = ? AND HashedPassword = ? ;";
		
		try (ConnectionManager cm = dbms.getConnectionManager()) {
			byte[] hashedPsw = Hash.computeHash(psw, 16, "password");
			ResultSet rs = cm.executeQuery(query, user, hashedPsw);
			
			if (rs.next())
				rsaKey = rs.getBytes("RSAKEY");
			
		} catch (SQLException e) {
			throw DBException.DB_0(e);
		}
		
		if (key.contains("Private") && rsaKey != null)
			rsaKey = AES.decryptPrivateKey(rsaKey, psw);
		
		return rsaKey;
	}
}
