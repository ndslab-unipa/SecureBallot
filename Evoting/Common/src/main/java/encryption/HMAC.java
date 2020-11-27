package encryption;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import exceptions.ENCException;
import exceptions.PEException;
import model.VotePacket;

public class HMAC {

	private static final String keyGenAlgorithm = "PBKDF2WITHHMACSHA256";
	private static final String HMACAlgorithm = "HMACSHA256";
	private static final int iterationCount = 2000;//valore minimo consigliato : 1000.
	private static final int saltLength = 8;
	private static final int keyLength = 128;
	
	
	private static SecretKey keyGen(String password, byte[] salt) throws PEException {
		
		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance(keyGenAlgorithm);
			KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterationCount, keyLength);
			SecretKey key = factory.generateSecret(spec);
			
			return key;
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw ENCException.ENC_4(true, e);
		}
	}
	
	private static String computeHMAC(String[] message, String password, byte[] salt) throws PEException {
		
		SecretKey key = keyGen(password, salt);
		
		try {
			Mac mac = Mac.getInstance(HMACAlgorithm);
			mac.init(key);
			
			for(String block : message) {
				byte[] bytes = block.getBytes();
				mac.update(bytes, 0, bytes.length);
			}
			
			return Base64.getEncoder().encodeToString(mac.doFinal());
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw ENCException.ENC_4(false, e);
		}
	}
	
	public static void sign(VotePacket packet, String password) throws PEException {
		
		byte[] salt = new byte[saltLength];
		
		Random random = new SecureRandom();
		random.nextBytes(salt);
		
		String digest = computeHMAC(packet.getUnsignedData(), password, salt);
		
		packet.sign(Base64.getEncoder().encodeToString(salt), digest);
	}
	
	public static boolean verify(VotePacket packet, String password) throws PEException {
		String salt = packet.getSalt();
		String HMAC = packet.getHMAC();
		
		String ComputedHMAC = computeHMAC(packet.getUnsignedData(), password, Base64.getDecoder().decode(salt));
		return HMAC.equals(ComputedHMAC);		
	}
}
