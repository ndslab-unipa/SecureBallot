/*
 * SysInfo.java
 *
 * Created on 2 novembre 2004, 16.36
 */

package rfid.labid.iso15693;

/**
 *
 * @author  Daniele
 */

/** 
* Represents all system information about an iso15693 transponder.
* Not all transponders can respond all information. You shoul check
* the matching "valid" field to ensure that the transponder has really
* sent the value.
*/
public class SysInfo {

	/** Creates a new instance of SysInfo */
	public SysInfo() {
	}
	
	/** 
	* Data Storage Family id.
	*/
	public byte DSFID;
	
	/** 
	* Application falmily id.
	*/
	public byte AFI;
	
	/** 
	* Integrated circuit id.
	*/
	public byte IC_REF;
	
	/** 
	* Number of memory blocks.
	*/
	public int nBlocks;
	
	/** 
	* Number of bytes in each block.
	*/
	public int blockSize;
	
	/** 
	* Serial number of transponder.
	*/
	public byte[] uid;
	
	/** 
	* Tells if transponder can respond AFI
	*/
	public boolean validAFI;
	
	/** 
	* Tells if transponder can respond DSFID
	*/
	public boolean validDSFID;
	
	/** 
	* Tells if transponder can respond its memory size
	*/
	public boolean validMemorySize;
	
	/** 
	* Tells if transponder can respond IC_REF
	*/
	public boolean validIC_REF;
	
}
