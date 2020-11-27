/*
 * RFIDTagDetectedListener.java
 *
 * Created on 27 maggio 2005, 16.43
 */

package rfid.labid;

/**
 * This interface declares a method to be run when transponder is in the field and
 * you have previously started a {@link labid.LabIdReader#getNextTagEvent(int, boolean)}
 */
public interface RFIDTagDetectedListener extends java.util.EventListener {
	
	/**
	 *The method to be executed when a transponder is detected by the reader.
	 */
	void TagDetected(int protocol) throws java.io.IOException;
}
