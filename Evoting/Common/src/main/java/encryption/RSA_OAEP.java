package encryption;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
//import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import exceptions.ENCException;
import exceptions.PEException;

public class RSA_OAEP {
	
	public final static String SymmetricParametersAlgorithm = "RSA/None/OAEPWithSHA-256AndMGF1Padding";
	public final static String SignatureAlgorithm = "RSA/None/OAEPWithSHA-256AndMGF1Padding";
	
	/**
	 * Funzione che cifra un array di byte (chiave simmetrica/IV monouso o digest per la firma) tramite crittografia asimmetrica.
	 * @param message 	L'oggetto da cifrare (Ki/IV o digest).
	 * @param key		La chiave pubblica o privata da adoperare.
	 * @param flag		True per adoperare l'algoritmo di cifratura per la firma, false per adoperare quello per la cifratura di chiave e IV simmetrici.
	 * @return			La stringa ottenuta cifrando e convertendo in Base64.
	 * @throws PEException
	 */
	public static String encrypt(byte[] message, Key key, boolean flag) throws PEException {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		
		String algorithm = flag ? SignatureAlgorithm : SymmetricParametersAlgorithm;
		
		try {	
			Cipher cipher = Cipher.getInstance(algorithm, "BC");
			//OAEPParameterSpec params = new OAEPParameterSpec("SHA-1", "MGF1", new MGF1ParameterSpec("SHA-256"), PSource.PSpecified.DEFAULT);
			cipher.init(Cipher.ENCRYPT_MODE, key); //, params);
		    return Base64.getEncoder().encodeToString(cipher.doFinal(message));
		} catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			throw ENCException.ENC_5(0, e);
		}
		
	}
	
	/**
	 * Funzione che decifra la stringa cifrata nell'array di byte corrispondente a chiave simmetrica/IV monouso o digest per la firma, tramite crittografia asimmetrica.
	 * @param cipherText 	La stringa cifrata e convertita in base64 da decifrare.
	 * @param key			La chiave pubblica o privata da adoperare.
	 * @param flag			True per adoperare l'algoritmo di cifratura per la firma, false per adoperare quello per la cifratura di chiave e IV simmetrici.
	 * @return				Il messaggio decifrato.
	 * @throws PEException
	 */
	public static byte[] decrypt(String cipherText, Key key, boolean flag) throws PEException {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		
		String algorithm = flag ? SignatureAlgorithm : SymmetricParametersAlgorithm;
		
		try {
			Cipher cipher = Cipher.getInstance(algorithm, "BC");

			//OAEPParameterSpec params = new OAEPParameterSpec("SHA-1", "MGF1", new MGF1ParameterSpec("SHA-256"), PSource.PSpecified.DEFAULT);
			cipher.init(Cipher.DECRYPT_MODE, key); //, params);
			return cipher.doFinal(Base64.getDecoder().decode(cipherText));
		} catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			throw ENCException.ENC_5(1, e);
		}
	}
}
