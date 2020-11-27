/*
 * RFReaderException.java
 *
 * Created on 28 aprile 2004, 14.51
 */

package rfid.labid;

/** 
* Represents an error while trying to perform a command with a LAB ID RFID reader.
* It is usually provided with a message in english, stored in the Message property.
*/
public class RFReaderException extends java.io.IOException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int detail = 0;
	
	/**  Creates a new instance of <code>RFReaderException</code> without detail message.
	*/
	public RFReaderException() {
	}
	
	/**  Constructs an instance of <code>RFReaderException</code> with the specified detail message.
	* 
	* @param msg The detail message.
	*/
	public RFReaderException(String msg) {
		super(msg);
	}
	
	/**  Constructs an instance of <code>RFReaderException</code> with the specified detail message
	* and detail value.
	* 
	* @param msg The detail message.
	* @param detail The detail value.
	*/
	public RFReaderException(String msg, int detail) {
		super(msg + " " + detail);
		this.detail = detail;
	}
	
	/**
	 * Getter for property detail.
	 * @return Value of property detail.
	 */
	public int getDetail() {
		return detail;
	}
}