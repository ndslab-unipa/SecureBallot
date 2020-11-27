package encryption;

import java.security.SecureRandom;
import java.util.Random;

import utils.Constants;

public class RandStrGenerator {
	private static final String allSymbols = "abcdefghijklmnopqrstuvwxyzABCDEFGJKLMNPQRSTUVWXYZ0123456789";
	
	public static String gen(int minLength, int maxLength) {
		  if (minLength > maxLength)
		      throw new IllegalArgumentException("minLength > maxLength");
		  
		  if (minLength < 1)
		      throw new IllegalArgumentException("minLength < 1");
		  
		  if (maxLength < 1)
		      throw new IllegalArgumentException("maxLength < 1");
		  
		  Random random = new SecureRandom();
		  int length = random.nextInt(maxLength - minLength) + minLength;
		  
		  String res = "";
		  
		  for (int i = 0; i < length; i ++) {
			  res += allSymbols.charAt(random.nextInt(allSymbols.length()));
		  }
		  
		  return res;
	}
	
	public static String genSessionKey() {
		Random random = new SecureRandom();
	  
		String res = "";
		String permChars = Constants.sessionKeyAllowedChars;
	  
		  for (int i = 0; i < Constants.sessionKeyLength; i ++) {
			  res += permChars.charAt(random.nextInt(permChars.length()));
		  }
	
		  return res;
	}
	
}
