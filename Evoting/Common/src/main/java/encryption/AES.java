package encryption;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import exceptions.ENCException;
import exceptions.PEException;

import java.util.Base64;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
 
public class AES {
	
	private static final String CBCMode = "AES/CBC/PKCS5Padding";
	private static final String ECBMode = "AES/ECB/PKCS5Padding";

	public static byte[] genKey(String random) throws PEException {
		return Hash.computeHash(random, 32, "chiave");
	}
	
	public static byte[] genIV(String random) throws PEException {
		return Hash.computeHash(random, 16, "IV");
	}
	
    public static String encryptVote(String plainVote, byte[] ki, byte[] iv) throws PEException {
    	SecretKeySpec secretKey = new SecretKeySpec(ki, "AES");
    	IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
    	try {
    		Cipher cipher = Cipher.getInstance(CBCMode);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(plainVote.getBytes("UTF-8")));
    	} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException e) {
    		throw ENCException.ENC_1("il voto", e);
		}
    }
 
    public static String decryptVote(String encryptedVote, byte[] ki, byte[] iv) throws PEException {
    	SecretKeySpec secretKey = new SecretKeySpec(ki, "AES");
    	IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
    	try {
            Cipher cipher = Cipher.getInstance(CBCMode);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedVote)), "UTF-8");
    	} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException e) {
    		throw ENCException.ENC_2("il voto", e);
		}
    	
    }

    public static byte[] encryptPrivateKey(byte[] key, String secret) throws PEException {
    	SecretKeySpec secretKey = new SecretKeySpec(genKey(secret), "AES");

    	try {
    		Cipher cipher = Cipher.getInstance(ECBMode);
        	cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        	return cipher.doFinal(key);
    	} 
    	catch (IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
    		throw ENCException.ENC_1("la chiave privata", e);
    	}
    }
    
    public static byte[] decryptPrivateKey(byte[] encrKey, String secret) throws PEException {
    	SecretKeySpec secretKey = new SecretKeySpec(genKey(secret), "AES");
    	
    	try {
    		Cipher cipher = Cipher.getInstance(ECBMode);
        	cipher.init(Cipher.DECRYPT_MODE, secretKey);
        	return cipher.doFinal(encrKey);
    	}
    	catch (IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
    		throw ENCException.ENC_2("la chiave privata", e);
    	}
    }
    
    public static String encryptNonce(int nonce, String secret) throws PEException {
    	SecretKeySpec secretKey = new SecretKeySpec(genKey(secret), "AES");
    	
    	String nonceString = Integer.toString(nonce);
    	
    	try {
    		byte[] nonceBA = nonceString.getBytes("UTF-8");
        	
        	Cipher cipher = Cipher.getInstance(ECBMode);
        	cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        	
        	return Base64.getEncoder().encodeToString(cipher.doFinal(nonceBA));
    	}
    	catch (UnsupportedEncodingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
    		throw ENCException.ENC_1("un nonce", e);
    	}
    	
    }

    public static int decryptNonce(String encryptedNonce, String secret) throws PEException  {
    	SecretKeySpec secretKey = new SecretKeySpec(genKey(secret), "AES");
    	
    	try {
    		Cipher cipher = Cipher.getInstance(ECBMode);
        	cipher.init(Cipher.DECRYPT_MODE, secretKey);
        	byte[] nonceBA = cipher.doFinal(Base64.getDecoder().decode(encryptedNonce));
        	String nonceString= new String(nonceBA, "UTF-8");
        	return Integer.parseInt(nonceString);
    	}
    	catch(NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | UnsupportedEncodingException e) {
    		throw ENCException.ENC_2("un nonce", e);
    	}
    	catch (InvalidKeyException | BadPaddingException e) {
    		throw ENCException.ENC_6(e);
    	}
    	
    }
}