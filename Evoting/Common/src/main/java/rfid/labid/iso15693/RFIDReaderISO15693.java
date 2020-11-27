package rfid.labid.iso15693;

import rfid.labid.*;

/**
 * This interface provides the base for implementations of software interfaces
 * to iso15693 tag readers by LAB ID. You should have concrete implementations,
 * according to the product you have purchased.
 * They allow your software to dialog with RFID readers connected through
 * serial or USB ports. <br/>
 */
public interface RFIDReaderISO15693 {
	
	/**
	 * Retrieves serial numbers of all iso15693 tags in the RF field.
	 *
	 * @return An array of serial numbers (n*8)
	 * @throws RFReaderException If no transponders are found
	 */
	byte[][] inventory() throws RFReaderException;
	
	/** Turns on or off RF power.
	 * @param mode 1: on, 0: off.
	 * @throws labid.RFReaderException If unable to perform operation. With message.
	 */
	void  rfOnOff(int mode) throws RFReaderException;
	
	/**
	 * Reads multiple data blocks from an addressed transponder. Take
	 * care in not trying to read beyond the memory size of the transponder.
	 *
	 * @param uid Serial number of the addressed transponder
	 * @param Start 0-based index of the first data block to read.
	 * @param Number Number of data blocks to read.
	 * @return Content of requested data block.
	 * @throws RFReaderException If unable to read blocks. With message.
	 */
	byte[] read(byte[] uid, int Start, int Number) throws RFReaderException;
	
	/**
	 * Reads multiple data blocks from a selected or non addressed transponder. Take
	 * care in not trying to read beyond the memory size of the transponder.
	 *
	 * @param Start 0-based index of the first data block to read.
	 * @param Number Number of data blocks to read.
	 * @param selected Specifies selected or non addressed mode
	 * @return Content of requested data block.
	 * @throws RFReaderException If unable to read data blocks. With message.
	 */
	byte[] read(int Start, int Number, boolean selected) throws RFReaderException;
	
	/**
	 * Writes multiple data blocks of an addressed transponder.
	 *
	 * @param uid Serial number of the addressed transponder
	 * @param data Data to write into data blocks. This array must
	 * have a length which is multiple of the current block size and must
	 * be consistent with following parameters
	 * @param Start 0-based index of the first block to write.
	 * @param Number Number of blocks to write.
	 * @throws RFReaderException If unable to write data blocks. With message.
	 */
	void  write(byte[] uid, byte[] data, int Start, int Number) throws RFReaderException;
	
	/**
	 * Writes multiple data blocks of a non addressed or selected transponder.
	 *
	 * @param data Data to write into data blocks. This array must
	 * have a length which is multiple of the current block size and must
	 * be consistent with following parameters
	 * @param Start 0-based index of the first block to write.
	 * @param Number Number of blocks to write.
	 * @param selected Specifies selected or non addressed mode
	 * @throws RFReaderException If unable to write data blocks. With message.
	 */
	void  write(byte[] data, int Start, int Number, boolean selected) throws RFReaderException;
	
	/**
	 * Prevents from further writings of a data block of an addressed
	 * transponder from  by setting their security status.
	 *
	 * @param uid Serial number of the addressed transponder.
	 * @param blockAddr 0-based index of the block to lock.
	 * @throws RFReaderException If unable to lock blocks. With message.
	 */
	void lockBlock(byte[] uid, int blockAddr) throws RFReaderException;
	
	/**
	 * Prevents from further writings of a data block of a non addressed
	 * transponder from  by setting their security status.
	 *
	 * @param blockAddr 0-based index of the block to lock.
	 * @param selected Specifies selected or non addressed mode.
	 * @throws RFReaderException If unable to lock blocks. With message.
	 */
	void lockBlock(int blockAddr, boolean selected) throws RFReaderException;
	
	/**
	 * Sets the addressed transponder in Selected state. Only transponders
	 * in this state can respond to commands addressed to "selected" tags
	 *
	 * @param uid Serial number of the addressed transponder
	 * @throws RFreaderException If unable to perform operation. With message.
	 */
	void select(byte[] uid) throws RFReaderException;
	
	/**
	 * Sets the addressed transponder in Quiet state. This means that it will
	 * not respond to further Inventory commands until it is put in to Ready
	 * state (see <see cref="resetToReady"/>) or it is powered down.
	 *
	 * @param uid Serial nuumber of the addressed transponder
	 * @throws RFReaderException If unable to perform operation. With message.
	 */
	void stayQuiet(byte[] uid) throws RFReaderException;
	
	/**
	 * Sets the addressed transponder in Ready state. It means that now tha tag
	 * will respond to Inventory commands.
	 *
	 * @param uid Serial number of the addressed transponder
	 * @throws RFreaderException If unable to perform operation. With message.
	 */
	void resetToReady(byte[] uid) throws RFReaderException;
	
	/**
	 * Sets the addressed transponder in Ready state. It means that now tha tag
	 * will respond to Inventory commands.
	 *
	 * @param selected Specifies selected or non addressed mode.
	 * @throws RFreaderException If unable to perform operation. With message.
	 */
	void resetToReady(boolean selected) throws RFReaderException;
	
	/**
	 * Writes the DSFID code (Data Storage Family ID) to an addressed transponder
	 *
	 * @param uid Serial number of the addressed transponder
	 * @param dsfid DSFID code to write
	 * @throws RFReaderException If unable to write DSFID code. With message.
	 */
	void writeDSFID(byte[] uid, byte dsfid) throws RFReaderException;
	
	/**
	 * Writes the AFI code (Application Family Identifier) to an addressed transponder
	 *
	 * @param uid Serial number of the addressed transponder
	 * @param afi AFI code to write
	 * @throws RFReaderException If unable to write AFI code. With message.
	 */
	void writeAFI(byte[] uid, byte afi) throws RFReaderException;
	
	/**
	 * Reads the security status of data blocks of an addressed transponder. <br/>
	 *
	 * @param uid Serial number of the addressed transponder
	 * @param start 0-based index of the first block to consider
	 * @param nBlocks Number of blocks to consider
	 * @return An array with the security status of the considered data blocks
	 * Every byte has the following meaning: <br/>
	 * 0x00 : Unlocked <br/>
	 * 0x01 : User locked <br/>
	 * 0x02 : Factory locked
	 *
	 * @throws RFReaderException  If unable to get security status. With message
	 */
	byte[] getBlockSecurityStatus(byte[] uid, int start, int nBlocks) throws RFReaderException;
	
	/**
	 * Reads the security status of data blocks of an addressed transponder. <br/>
	 *
	 * @param start 0-based index of the first block to consider
	 * @param nBlocks Number of blocks to consider
	 * @param selected Specifies selected or non addressed mode
	 * @return An array with the security status of the considered data blocks
	 * Every byte has the following meaning: <br/>
	 * 0x00 : Unlocked <br/>
	 * 0x01 : User locked <br/>
	 * 0x02 : Factory locked
	 *
	 * @throws RFReaderException  If unable to get security status. With message
	 */
	byte[] getBlockSecurityStatus(int start, int nBlocks, boolean selected) throws RFReaderException;
	
	/**
	 * Gets some information about the addressed transponder.
	 *
	 * @param uid Serial number of the addressed transponder.
	 * @return The SysInfo struct which contains all retrieved information.
	 * @throws RFReaderException If could not read information throws an
	 * exception with a message.
	 */
	SysInfo getSystemInformation(byte[] uid) throws RFReaderException;
	
	/**
	 * Sets the current number of bytes in every data block for write commands
	 * @param nBytes Number of bytes per block. For several ISO 15693 tags it is usually 4.
	 */
	void setBlockSize(int nBytes);
	
	/**
	 * Gets the current number of bytes in every data block for write commands
	 */
	int getBlockSize();
	
}
