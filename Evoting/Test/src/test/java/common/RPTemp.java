package common;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

import encryption.KeyPairManager;
import exceptions.PEException;

public class RPTemp {
	
	private static Key Kpu = null;
	private static Key Kpr = null;
	
	private synchronized static void init() throws PEException {
		if(Kpr == null || Kpu == null) {
			SecureRandom random = new SecureRandom();
		    KeyPairGenerator generator = null;
		    
		    KeyPair pair = KeyPairManager.genKeyPair();
		    
			Kpr = pair.getPrivate();
			Kpu = pair.getPublic();
		    
			/*try {
				Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
				//System.out.println("Security providers:" + Security.getProviders().length);
				generator = KeyPairGenerator.getInstance("RSA", "BC");
			} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		    generator.initialize(1024, random);
			
		    KeyPair pair = generator.generateKeyPair();
		    
			Kpu = pair.getPublic();
			Kpr = pair.getPrivate();*/
		}
	}
	
	//public Key getPrivate() {
	public static byte[] getPrivate() throws PEException {
		init();
		//return Base64.getEncoder().encodeToString(Kpr.getEncoded());
		return Kpr.getEncoded();
	}

	//public Key getPublic() {
	public static byte[] getPublic() throws PEException {
		init();
		//return Base64.getEncoder().encodeToString(Kpu.getEncoded());
		return Kpu.getEncoded();
	}
}
