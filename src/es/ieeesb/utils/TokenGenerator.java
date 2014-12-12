package es.ieeesb.utils;

import java.security.SecureRandom;

/**
 * @author Gregorio
 * Utility class that generates random tokens for the door.
 */
public class TokenGenerator 
{
	
	/**
	 * Pretty self-explanatory. Uses securerandom because well, we can.
	 * @return a new token. 40 characters, random, alphanumeric. Beautiful.
	 */
	public static String generateToken()
	{
		char[] chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
		StringBuilder sb = new StringBuilder();
		SecureRandom random = new SecureRandom();
		for (int i = 0; i < 39; i++) {
		    char c = chars[random.nextInt(chars.length)];
		    sb.append(c);
		}
		String output = sb.toString();
		return output;
	}

}
