/*
 * ArrayUtils.java
 * author: Daniele Zanoli
 * Created on 21 aprile 2004, 15.06
 */

package rfid.labid;

/** ByteUtils provides static methods for common manipulations
* and conversions on byte and byte arrays
*/
public class ByteUtils {
	
	private ByteUtils() {
		
	}
	
	/** Copies bytes from src to dest
	*/
	public static void  copy(byte[] src, byte[] dest) {
		copy(src, 0, dest, 0, src.length);
	}
	
	/** Copies all bytes from src (starting from srcOffset) to dest (starting from
	* destOffset)
	*/
	public static void  copy(byte[] src, int srcOffset, byte[] dest, int destOffset) {
		copy(src, srcOffset, dest, destOffset, src.length - srcOffset);
	}
	
	/** Copies length bytes from src to dest starting from srcOffset and destOffset
	*/
	public static void  copy(byte[] src, int srcOffset, byte[] dest, int destOffset, int length) {
		java.lang.System.arraycopy(src, srcOffset, dest, destOffset, length);
	}
	
	/** Returns true if a and b have the same content
	*/
	public static boolean areEqual(byte[] a, byte[] b) {
		int len = a.length;
		if (len != b.length)
			return false;
		while (len > 0) {
			if (a[--len] != b[len]) {
				return false;
			}
		}
		return true;
	}
	
	/** 
	* Returns true if a and b have the same len bytes at position offA and offB
	*/
	public static boolean areEqual(byte[] a, int offA, byte[] b, int offB, int len) {
		if ((len + offA) > a.length)
			return false;
		if ((len + offB) > b.length)
			return false;
		while (len > 0) {
			--len;
			if (a[len + offA] != b[len + offB]) {
				return false;
			}
		}
		return true;
	}
	
	/** Returns a new byte array instance made of the concatenation of
	* the content of a followed by the content of b
	*/
	public static byte[] concat(byte[] a, byte[] b) {
		int lenA = a.length;
		int lenB = b.length;
		byte[] result = new byte[lenA + lenB];
		copy(a, result);
		copy(b, 0, result, lenA, lenB);
		return result;
	}
	
	/** Converts a byte into two bytes containing the ascii representation
	* of an hexadecimal value (example: (byte)0xE0 -> {(byte)'E', (byte)'0'} )
	*/
	public static byte[] toHex(byte b) {
		byte[] hex = new byte[]{(byte)'0', (byte)'1', (byte)'2', (byte)'3', (byte)'4',
		(byte)'5', (byte)'6', (byte)'7', (byte)'8', (byte)'9', (byte)'A', (byte)'B',
		(byte)'C', (byte)'D', (byte)'E', (byte)'F'};
		byte[] result = new byte[]{hex[(b >> 4) & 0x0F], hex[b & 0x0F]};
		return result;
	}
	
	/** Converts a byte array into its hexadecimal ascii representation
	*/
	public static byte[] toHex(byte[] b) {
		byte[] hex = new byte[]{(byte)'0', (byte)'1', (byte)'2', (byte)'3', (byte)'4',
		(byte)'5', (byte)'6', (byte)'7', (byte)'8', (byte)'9', (byte)'A', (byte)'B',
		(byte)'C', (byte)'D', (byte)'E', (byte)'F'};
		int len = b.length, j = len * 2;
		byte[] result = new byte[j];
		while (len > 0) {
			result[--j] = hex[(b[--len] & 0x0F)];
			result[--j] = hex[(b[len] >> 4) & 0x0F];
		}
		//		for (int i=0;i<len;i++) {
		//			result[j++] = hex[(b[i]>>4) & 0x0F];
		//			result[j++] = hex[(b[i] & 0x0F)];
		//		}
		return result;
	}
	
	/** Converts a byte array into its hexadecimal ascii representation, with
	* a separator between bytes (es: "e0-04")
	*/
	public static byte[] toHex(byte[] b, byte separator) {
		byte[] hex = new byte[]{(byte)'0', (byte)'1', (byte)'2', (byte)'3', (byte)'4',
		(byte)'5', (byte)'6', (byte)'7', (byte)'8', (byte)'9', (byte)'A', (byte)'B',
		(byte)'C', (byte)'D', (byte)'E', (byte)'F'};
		int len = b.length, j = len * 3 - 1;
		byte[] result = new byte[j];
		while (len > 0) {
			result[--j] = hex[(b[--len] & 0x0F)];
			result[--j] = hex[(b[len] >> 4) & 0x0F];
			if (len > 0)
				result[--j] = separator;
		}
		//		for (int i=0;i<len;i++) {
		//			result[j++] = hex[(b[i]>>4) & 0x0F];
		//			result[j++] = hex[(b[i] & 0x0F)];
		//			if (i<len-1) result[j++] = separator;
		//		}
		return result;
	}
	
	/** Converts a byte array into its hexadecimal string representation
	*/
	public static String toHexString(byte[] b) {
		return new String(toHex(b));
	}
	
	/** Converts a byte array into its hexadecimal string representation
	* with a separator.
	*/
	public static String toHexString(byte[] b, char separator) {
		return new String(toHex(b, (byte) separator));
	}
	
	/** Converts a byte array into its hexadecimal string representation
	* with a separator.
	*/
	public static String toHexString(byte[] b, byte separator) {
		return new String(toHex(b, separator));
	}
	
	/** 
	* Converts a byte into its hexadecimal string representation.
	* 
	* @param b Byte to convert.
	*/
	public static String toHexString(byte b) {
		return new String(toHex(b));
	}
	
	
	/** 
	* Reverts the content of a byte array. Changes affect the original data.
	*/
	public static void revert(byte[] data) {
		int len = data.length;
		int begin = 0;
		int end = len - 1;
		byte temp;
		int mid = len >> 1; // mid = len /2
		while (begin < mid) {
			temp = data[begin];
			data[begin] = data[end];
			data[end] = temp;
			begin++;
			end--;
		}
	}
	
	/** 
	* Reverts the order of bytes in an array.
	* 
	* @param data Bytes to revert.
	*/
	public static byte[] revertedCopy(byte[] data) {
		byte[] buf = new byte[data.length];
		copy(data, 0, buf, 0, data.length);
		revert(buf);
		return buf;
	}
	
	/** 
	* Parses a string representation of a byte array and returns the corresponding
	* byte array
	* 
	* @param str Hex string (ex. "0Acf41")
	* @return Byte array with parsed values (ex. {0x0a, 0xcf, 0x41} )
	*/
	public static byte[] parseHexString(String str) throws Exception {
		int strlen = str.length();
		if (strlen == 0) throw new Exception("Empty string");
		int len = strlen >> 1; // strlen/2
		String s;
		if ((strlen & 0x01) > 0) { //se la stringa ha lungh. dispari aggiungo uno 0 all'inizio
			s = "0" + str;
			len++;
		}
		else s = str;
		
		byte[] result = new byte[len];
		
		for (int i=0;i<len;i++) {
			result[i] = (byte) ( (intValue(s.charAt(i*2)) << 4) + intValue(s.charAt(i*2+1)) );
		}
		
		return result;
	}
	
	/** 
	* Parses a string representation of a byte array with a separation char and returns the corresponding byte array
	* 
	* @param str Hex string (ex. "0A-cf-41", "0A cf 41")
	* @return Byte array with parsed values (ex. {0x0a, 0xcf, 0x41} )
	*/
	public static byte[] parseHexStringWithSepChar(String str) throws Exception {
		if (str.length() == 0) throw new Exception("Empty string");
		int len = str.length() / 3 + 1;
		
		byte[] result = new byte[len];
		
		for (int i=0;i<len;i++) {
			result[i] = (byte) ( (intValue(str.charAt(i*3)) << 4)
			+ intValue(str.charAt(i*3+1)) );
		}
		
		return result;
	}
	
	private static int intValue(char c) {
		if (c >= '0' && c <= '9') return c-'0';
		if (c >= 'A' && c <= 'F') return 10 + (c-'A');
		if (c >= 'a' && c <= 'f') return 10 + (c-'a');
		throw new NumberFormatException("Not hexadecimal: "+c);
	}
	
	/** 
	* Returns the value of a single bit at a certain position in a byte
	*/
	public static boolean getBit(byte data, int index) {
		return ((data & (1 << index)) != 0);
	}
	
	
	/** 
	* Sets a single bit in a given byte. <br/>
	* Example:
	* <code>
	* byte a = 0x00;
	* byte b = ByteUtils.setBit(a, 1, true);
	* </code>
	* Now b=0x02 and a=0x00.
	* 
	* @param data Given byte. Its value is not changed by this method
	* @param index Index of the bit you want to set. It must be [0-7]
	* @param val New value of the bit: 0-false, 1-true
	*/
	public static byte setBit(byte data, int index, boolean val) {
		if (val) {
			return (byte)(data | (1 << index));
		}
		else {
			return (byte)(data & ~(1 << index));
		}
	}
	
	/** 
	* Composes a byte from 8 booleanean values.
	* 
	* @param b0 The less significant bit.
	* @param b1 
	* @param b2 
	* @param b3 
	* @param b4 
	* @param b5 
	* @param b6 
	* @param b7 The most significant bit.
	*/
	public static byte composeByte(boolean b0, boolean b1, boolean b2, boolean b3, boolean b4, boolean b5, boolean b6, boolean b7) {
		byte b = 0;
		if (b0) b |= 1 ;
		if (b1) b |= (1 << 1);
		if (b2) b |= (1 << 2);
		if (b3) b |= (1 << 3);
		if (b4) b |= (1 << 4);
		if (b5) b |= (1 << 5);
		if (b6) b |= (1 << 6);
		if (b7) b |= (1 << 7);
		return b;
	}

	public static void convertLSBFirst(int value, byte[] target, int offset) {
		target[offset++] = (byte)(value & 0xFF);
		target[offset++] = (byte)((value >> 8) & 0xFF);
		target[offset++] = (byte)((value >> 16) & 0xFF);
		target[offset++] = (byte)((value >> 24) & 0xFF);
	}

	public static void convertMSBFirst(int value, byte[] target, int offset) {
		target[offset++] = (byte)((value >> 24) & 0xFF);
		target[offset++] = (byte)((value >> 16) & 0xFF);
		target[offset++] = (byte)((value >> 8) & 0xFF);
		target[offset++] = (byte)(value & 0xFF);
	}

	public static int toIntLSBfirst(byte[] target, int offset, int count) {
		if (count > 4)
			throw new IllegalArgumentException("count parameter must be less than 5");

		int result = 0;

		for (int i = 0; i < count; i++)	{
				result += target[offset + i] << (8 * i);
		}

		return result;
	}

	public static int toIntMSBfirst(byte[] target, int offset, int count) {
		if (count > 4)
			throw new IllegalArgumentException("count parameter must be less than 5");

		int result = 0;

		for (int i = 0; i < count; i++) {
			result += target[offset + i] << (8 * (count - 1 - i));
		}

		return result;
	}

	public static long toLongLSBfirst(byte[] target, int offset, int count) {
		if (count > 8)
			throw new IllegalArgumentException("count parameter must be less than 5");

		long result = 0;

		for (int i = 0; i < count; i++) {
			result += (long)target[offset + i] << (8 * i);
		}

		return result;
	}

	public static long toLongMSBfirst(byte[] target, int offset, int count) {
		if (count > 8)
			throw new IllegalArgumentException("count parameter must be less than 5");

		long result = 0;

		for (int i = 0; i < count; i++) {
			result += (long)target[offset + i] << (8 * (count - 1 - i));
		}

		return result;
	}

	public static void convertLSBFirst(long value, byte[] target, int offset) {
		target[offset++] = (byte)(value & 0xFF);
		target[offset++] = (byte)((value >> 8) & 0xFF);
		target[offset++] = (byte)((value >> 16) & 0xFF);
		target[offset++] = (byte)((value >> 24) & 0xFF);
		target[offset++] = (byte)((value >> 32) & 0xFF);
		target[offset++] = (byte)((value >> 40) & 0xFF);
		target[offset++] = (byte)((value >> 48) & 0xFF);
		target[offset++] = (byte)((value >> 56) & 0xFF);
	}

	public static void convertMSBFirst(long value, byte[] target, int offset) {
		target[offset++] = (byte)((value >> 56) & 0xFF);
		target[offset++] = (byte)((value >> 48) & 0xFF);
		target[offset++] = (byte)((value >> 40) & 0xFF);
		target[offset++] = (byte)((value >> 32) & 0xFF);
		target[offset++] = (byte)((value >> 24) & 0xFF);
		target[offset++] = (byte)((value >> 16) & 0xFF);
		target[offset++] = (byte)((value >> 8) & 0xFF);
		target[offset++] = (byte)(value & 0xFF);
	}

	public static String toASCII(byte[] text) {
		return new String(text);
	}

	public static byte[] toBytes(String text) {
		return text.getBytes();
	}
}