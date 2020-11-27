package rfid.labid.iso15693;

import rfid.labid.IO.*;
import rfid.labid.*;
import java.io.*;

/**
 * This class provides a software interface for ISO15693 commands
 * using LAB ID multi-standard tag reader. If you want to have a technical background of used technologies
 * and standards, refer to ISO15693. If you need to use ISO14443 commands,
 * use class <see cref="iso14443.ISO14443Reader"/>.
 */
public class ISO15693Reader extends LabIdReader implements RFIDReaderISO15693 {
	
	/**
	 * After responding to an inventory command, every tag is put in Quiet state, in order
	 * not to get duplicate values in following inventories.
	 */
	public static final int	ISO_OPTION_StayQuiet = 0;
	
	/**
	 * Inventory command retrieves only transponders with Afi equal to
	 * the current value store in the <see cref="ISO15693Reader.AFI"/>AFI property.
	 */
	public static final int	ISO_OPTION_AfiEnabled = 1;
	
	/**
	 * Specifies if the anticollision algorithm should use only 1 time slot (faster).
	 * If a collision is detected, the reader switches automatically to 16-timeslots
	 * mode. <br/> If your application uses always more than one transponder you
	 * should set this option to false in order to improve speed.
	 */
	public static final int	ISO_OPTION_TimeSlot1 = 2;
	
	/**
	 * Reserved for future use.
	 */
	public static final int	ISO_OPTION_Unused = 3;
	
	/**
	 * This option is automatically set by commands which use selected addressing.
	 * Don't modify it.
	 */
	public static final int	ISO_OPTION_Selected = 4;
	
	/**
	 * This option is automatically set by commands which use selected addressing.
	 * Don't modify it.
	 */
	public static final int	ISO_OPTION_Addressed = 5;
	
	/**
	 * Enable this option if you want to receive the security status of each block
	 * read using <see cref="ISO15693Reader.read"/>
	 */
	public static final int	ISO_OPTION_SecurityStatus = 6;
	
	/**
	 * Enable this option if you want to use options defined in your software.
	 * If this option is cleared, options stored in internal registers of the
	 * reader will be used. This does not apply to options <i>Addressed</i> and
	 * <i>Selected</i> <br/>
	 * This is the only option enabled by default.
	 */
	public static final int	ISO_OPTION_UseOptions = 7;
	
	/**
	 * The AFI code (Application Family Identifier) to be used in Inventories
	 * Default is 0x00 (all transponders will respond).
	 */
	public byte Afi = 0x00;
	
	/**
	 * Sets the current block size in bytes. Default is 4.
	 * Change this property only if you are sure that you are using
	 * transponders with different block size.
	 */
	public int getBlockSize() {
		return blockSize;
	}
	
	public void setBlockSize(int value) {
		this.blockSize = value;
	}
	
	/**
	 * Sono le opzioni (bit di mode) dei comandi iso. Hanno il seguente formato
	 * 0 - stay quiet
	 * 1 - AFI enable
	 * 2 - use only 1 time slot
	 * 3 - unused
	 * 4 - selected mode
	 * 5 - addressed mode
	 * 6 - security status
	 * 7 - 0: register mode / 1:normal mode
	 */
	private byte isoOptions = (byte)0x84; //normal mode
	
	/**
	 * Sets one of the current options for iso15693 commands. <br/>
	 * The following example shows how to set the proper option for retrieving
	 * also the security status of each block during <see cref="read"/> blocks operations.
	 * <code>
	 * LabIdReader reader = new LabIdReader();
	 * reader.setIso15693Option (Iso15693Option.SecurityStatus, true);
	 * </code>
	 *
	 * @param op The options you want to set
	 * @param val The value you want to give to the selected option
	 * @throws RFReaderException If you try to modify values of
	 * Addressed or Selected options, an Exception is raised. They are managed
	 */
	public void setIso15693Option(int op, boolean val) throws RFReaderException {
		if (op == ISO_OPTION_Addressed || op == ISO_OPTION_Selected)
			throw new RFReaderException("Attempt to modify read-only ISO15693 option");
		this.isoOptions = ByteUtils.setBit(isoOptions, op, val);
	}
	
	/**
	 * Gets the current value of an Iso15693 option. <br/>
	 * The following example shows how to get the current value of the AfiEnabled
	 * option.
	 * <code>
	 * LabIdReader reader = new LabIdReader();
	 * boolean afiEn = reader.getIso15693Option (Iso15693Option.AfiEnabled);
	 * </code>
	 *
	 * @param op The option you want to check.
	 *
	 */
	public boolean getIso15693Option(int op) {
		return ByteUtils.getBit(this.isoOptions, op);
	}
	
	/**
	 * Instantiates a new ISO15693Reader object
	 */
	public ISO15693Reader() {
		super();
	}
	
	/**
	 * Instantiates a new LabIdReader object connected through a SerialStream object
	 */
	public ISO15693Reader(CableStream stream) {
		super(stream);
	}
	
	/**
	 * Retrieves serial numbers of all iso15693 tags in the RF field.
	 * If the AFI property is set to a value different from 0 and the
	 * iso15693 afiEnabled option is set, only tags
	 * with the proper AFI will respond to Inventory commands
	 *
	 * @return An array of serial numbers (n*8) or <c>null</c> if no
	 *
	 */
	public  byte[][] inventory() throws RFReaderException {
		return inventory(this.Afi);
	}
	
	/**
	 * Retrieves serial numbers of all iso15693 tags in the RF field.
	 * The afi parameter is used only if the iso15693 afiEnabled option
	 * is set. In this case, only transponders with matching AFI will
	 * send their serial number.
	 *
	 * @param afi The AFI code of transponders that you want to send
	 * their serial number
	 * @return An array of serial numbers (n*8) or <c>null</c> if no
	 *
	 */
	public byte[][] inventory(byte afi) throws RFReaderException {
		//Wait();
		
		byte[][] result;
		int numeroTag = 0;
		
		byte[] cmd;
		
		//se sono in normal mode (no registri) con afi enabled devo inviare anche il byte di afi
		if (ByteUtils.getBit(this.isoOptions, ISO_OPTION_UseOptions)
		&& ByteUtils.getBit(this.isoOptions, ISO_OPTION_AfiEnabled) ) {
			cmd = new byte[4];
			cmd[3] = afi;
		}
		else
			cmd = new byte[3];
		
		cmd[0] = (byte)0xB0; //control byte: iso command
		cmd[1] = (byte)0x01; //command code: inventory
		cmd[2] = this.isoOptions;
		
		try {
			//System.out.println("cmd = " + ByteUtils.toHexString(ByteUtils.revertedCopy(cmd), ' '));
			send(cmd);
			receive();
		}
		catch (IOException e) {
			//Notify();
			throw new RFReaderException(errMsg("Serial communication problem"));
		}
		
		numeroTag = recv_buf[5] & 0xFF;
		
		if (recv_buf[4] == (byte) 0x00) {
			result = new byte[numeroTag][];
			for (int i = 0; i < numeroTag; i++) {
				result[i] = new byte[uidSize];
			}
			
			int indiceLetti = 8;
			for (int i = 0; i < numeroTag; i++) {
				ByteUtils.copy(recv_buf, indiceLetti, result[i], 0, uidSize);
				indiceLetti += (uidSize + 2); //8 (uid) + 2 (separatore)
			}
		}
		else {
			//Notify();
			//throw new RFReaderException(errMsg("No transponders found"));
			return null;
		}
		
		//Notify();
		return result;
	}
	
	/**
	 * Reads multiple data blocks from an addressed transponder. Take
	 * care in not trying to read beyond the memory size of the transponder.
	 *
	 * @param uid Serial number of the addressed transponder
	 * @param Start 0-based index of the first data block to read.
	 * @param Number Number of data blocks to read.
	 * @return Content of requested data block. If iso15693 SecurityStatus
	 * option is set, each data block is preceded by a byte which specifies
	 * the current security status (see <see cref="getBlockSecurityStatus"/>
	 * of the block.
	 * @throws RFReaderException If unable to read blocks. With message.
	 */
	public  byte[] read(byte[] uid, int Start, int Number) throws RFReaderException {
		//Wait();
		
		byte[] result;
		
		byte[] cmd = new byte[13];
		cmd[0] = (byte) 0xB0;
		cmd[1] = (byte) 0x23;
		cmd[2] = ByteUtils.setBit(this.isoOptions,ISO_OPTION_Addressed,true); //mode addressed, con security status
		//cmd[2] = ByteUtils.setBit(cmd[2],ISO_OPTION_SecurityStatus,true);
		ByteUtils.copy(uid, 0, cmd, 3); //copio l'uid nel comando[5 - 12] da mandare al reader
		cmd[11] = (byte) Start;
		cmd[12] = (byte) Number;
		
		sendReceive(cmd, "Could not read");
		
		int nBlocks = 0xFF & recv_buf[5];
		
		@SuppressWarnings("unused")
		int blocksize = recv_buf[6] & 0xFF;
		
		if (ByteUtils.getBit(this.isoOptions, ISO_OPTION_SecurityStatus)) //voglio anche il byte di lock
			blocksize++;
		
		if (nBlocks == 0) {
			//Notify();
			throw new RFReaderException(errMsg("Could not read"));
		}
		
		int dataBytesRead = recv_buf[0] + ((recv_buf[1] & 0x0F) << 8) - 9; //9 = 7(header) + 2(crc)
		result = new byte[dataBytesRead];
		ByteUtils.copy(recv_buf, 7, result, 0, dataBytesRead);
		
		//Notify();
		return result;
	}
	
	/**
	 * Reads multiple data blocks from a selected or non addressed transponder. Take
	 * care in not trying to read beyond the memory size of the transponder.
	 *
	 * @param Start 0-based index of the first data block to read.
	 * @param Number Number of data blocks to read.
	 * @param selected Specifies selected or non addressed mode
	 * @return Content of requested data block. If iso15693 SecurityStatus
	 * option is set, each data block is preceded by a byte which specifies
	 * the current security status (see <see cref="getBlockSecurityStatus"/>)
	 * of the block.
	 * @throws RFReaderException If unable to read data blocks. With message.
	 */
	public  byte[] read(int Start, int Number, boolean selected) throws RFReaderException {
		//Wait();
		
		byte[] result;
		
		//byte[] cmd = {(byte) 0x11, (byte) 0xFF,(byte) 0xB0,(byte) 0x23,(byte)0x09,(byte)0x00, (byte)0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,(byte)0x00,(byte) 0x00,(byte)0x00};
		byte[] cmd = new byte[5];
		
		cmd[0] = (byte) 0xB0;
		cmd[1] = (byte) 0x23;
		cmd[2] = this.isoOptions; //mode
		if (selected) cmd[2] = ByteUtils.setBit(cmd[2],ISO_OPTION_Selected,true);
		cmd[3] = (byte) Start;
		cmd[4] = (byte) Number;
		
		sendReceive(cmd, "Could not read");
		
		int nBlocks = 0xFF & recv_buf[5];
		
		@SuppressWarnings("unused")
		int blocksize = recv_buf[6] & 0xFF;
		
		if (ByteUtils.getBit(this.isoOptions, ISO_OPTION_SecurityStatus)) //voglio anche il byte di lock
			blocksize++;
		
		if (nBlocks == 0) {
			//Notify();
			throw new RFReaderException(errMsg("Could not read"));
		}
		
		int dataBytesRead = recv_buf[0] + ((recv_buf[1] & 0x0F) << 8) - 9; //9 = 7(header) + 2(crc)
		result = new byte[dataBytesRead];
		ByteUtils.copy(recv_buf, 7, result, 0, dataBytesRead);
		
		//Notify();
		return result;
	}
	
	/**
	 * Writes multiple data blocks of an addressed transponder.
	 *
	 * @param uid Serial number of the addressed transponder
	 * @param data Data to write into data blocks. This array must
	 * have a length which is multiple of the current block size and must
	 * be consistent with Start and Number parameters. If data is shorter it will
	 * be filled with zeros, if it is longer it will be truncated.
	 * @param Start 0-based index of the first block to write.
	 * @param Number Number of blocks to write.
	 * @throws RFReaderException If unable to write data blocks. With message.
	 * In the Detail field of RFReaderException you will find the index of the
	 * first block which was not written. All following blocks were not written.
	 */
	public  void  write(byte[] uid, byte[] data, int Start, int Number) throws RFReaderException {
		//Wait();
		
		//int result = - 1;
		int expectedSize = Number * blockSize;
		
		byte[] cmd = new byte[uidSize + 6 + expectedSize];
		cmd[0] = (byte) 0xB0; //control byte: iso command
		cmd[1] = (byte) 0x24; //command code: write
		cmd[2] = ByteUtils.setBit(this.isoOptions,ISO_OPTION_Addressed,true); //mode: addressed
		
		ByteUtils.copy(uid, 0, cmd, 3);
		cmd[11] = (byte) Start;
		cmd[12] = (byte) Number;
		cmd[13] = (byte) blockSize;
		
		if (data.length < expectedSize)
			ByteUtils.copy(data, 0, cmd, 14, data.length);
		else
			ByteUtils.copy(data, 0, cmd, 14, expectedSize);
		
		sendReceive(cmd, "Could not write", recv_buf[5]);
		
		//Notify();
	}
	
	/**
	 * Writes multiple data blocks of a non addressed or selected transponder.
	 *
	 * @param data Data to write into data blocks. This array must
	 * have a length which is multiple of the current block size and must
	 * be consistent with Start and Number parameters. If data is shorter it will
	 * be filled with zeros, if it is longer it will be truncated.
	 * @param Start 0-based index of the first block to write.
	 * @param Number Number of blocks to write.
	 * @param selected Specifies selected or non addressed mode
	 * @throws RFReaderException If unable to write data blocks. With message.
	 * In the Detail field of RFReaderException you will find the index of the
	 * first block which was not written. All following blocks were not written.
	 */
	public  void write(byte[] data, int Start, int Number, boolean selected) throws RFReaderException {
		//Wait();
		
		available = false;
		int expectedSize = Number * blockSize;
		
		byte[] cmd = new byte[6 + expectedSize];
		cmd[0] = (byte) 0xB0; // control byte: iso command
		cmd[1] = (byte) 0x24; // command code: write
		cmd[2] = this.isoOptions; //non addressed
		if (selected) cmd[2] = ByteUtils.setBit(cmd[2],ISO_OPTION_Selected,true);
		cmd[3] = (byte) Start;
		cmd[4] = (byte) Number;
		cmd[5] = (byte) blockSize; //block size
		
		if (data.length < expectedSize)
			ByteUtils.copy(data, 0, cmd, 6, data.length);
		else
			ByteUtils.copy(data, 0, cmd, 6, expectedSize);
		
		sendReceive(cmd, "Could not write", recv_buf[5]);
		
		//Notify();
	}
	
	/**
	 * Prevents from further writings of a data block of an addressed
	 * transponder by setting its security status.
	 *
	 * @param uid Serial number of the addressed transponder.
	 * @param blockAddr Index of block to lock.
	 * @throws RFReaderException If unable to lock blocks. With message.
	 */
	public  void lockBlock(byte[] uid, int blockAddr) throws RFReaderException {
		lockMultipleBlocks(uid, blockAddr, 1);
	}
	
	
	/**
	 * Prevents from further writings of a data block of an addressed
	 * transponder by setting their security status.
	 *
	 * @param uid Serial number of the addressed transponder.
	 * @param start Index of the first block to lock.
	 * @param nBlocks Number of blocks to lock.
	 * @throws RFReaderException If unable to lock blocks. With message.
	 * In the Detail field of RFReaderException you will find the index of the
	 * first block which was not locked. All following blocks were not locked.
	 */
	public void lockMultipleBlocks(byte[] uid, int start, int nBlocks) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[13];
		cmd[0] = (byte) 0xB0; //control byte: iso command
		cmd[1] = (byte) 0x22; //command code: lock  block
		cmd[2] = ByteUtils.setBit(this.isoOptions, ISO_OPTION_Addressed, true); //mode: addressed
		ByteUtils.copy(uid,0,cmd,3);
		cmd[11] = (byte) start;
		cmd[12] = (byte) nBlocks;
		
		sendReceive(cmd, "Could not lock block", recv_buf[5]);
		
		//Notify();
		return;
	}
	
	/**
	 * Prevents from further writings of a data block of a non addressed
	 * transponder by setting its security status.
	 *
	 * @param blockAddr 0-based index of the block to lock.
	 * @param selected Specifies selected or non addressed mode.
	 * @throws RFReaderException If unable to lock blocks. With message.
	 */
	public  void lockBlock(int blockAddr, boolean selected) throws RFReaderException {
		lockMultipleBlocks(blockAddr, 1, selected);
	}
	
	/**
	 * Prevents from further writings of a data block of a non addressed
	 * transponder by setting their security status.
	 *
	 * @param blockAddr Index of the first block to lock.
	 * @param nBlocks Number of blocks to lock.
	 * @param selected Specifies selected or non addressed mode
	 * @throws RFReaderException If unable to lock blocks. With message.
	 * In the Detail field of RFReaderException you will find the index of the
	 * first block which was not locked. All following blocks were not locked.
	 */
	public void lockMultipleBlocks(int blockAddr, int nBlocks, boolean selected) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[5];
		cmd[0] = (byte) 0xB0; //control byte: iso command
		cmd[1] = (byte) 0x22; //command code: lock block
		cmd[2] = this.isoOptions;
		if (selected)
			cmd[2] = ByteUtils.setBit(this.isoOptions, ISO_OPTION_Selected, true); //mode: selected
		cmd[3] = (byte) blockAddr;
		cmd[4] = (byte) nBlocks;
		
		sendReceive(cmd, "Could not lock block",recv_buf[5]);
		
		//Notify();
		return;
	}
	
	
	/**
	 * Writes the AFI code (Application Family Identifier) to an addressed transponder
	 *
	 * @param uid Serial number of the addressed transponder
	 * @param afi AFI code to write
	 * @throws RFReaderException If unable to write AFI code. With message.
	 */
	public  void writeAFI(byte[] uid, byte afi) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[12];
		cmd[0] = (byte) 0xB0; //control byte: iso command
		cmd[1] = 0x27; //command code: write afi
		cmd[2] = ByteUtils.setBit(this.isoOptions, ISO_OPTION_Addressed, true); //addressed mode
		ByteUtils.copy(uid,0,cmd,3);
		cmd[11] = afi;
		
		sendReceive(cmd, "Could not write AFI");
		
		//Notify();
		return;
	}
	
	/**
	 * Writes the AFI code (Application Family Identifier) to a non
	 * addressed or selected transponder.
	 *
	 * @param afi AFI code to write
	 * @param selected Specifies selected or non addressed mode
	 * @throws RFReaderException If unable to write AFI code. With message.
	 */
	public void writeAFI(byte afi, boolean selected) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[4];
		cmd[0] = (byte) 0xB0; //control byte: iso command
		cmd[1] = 0x27; //command code: write afi
		cmd[2] = this.isoOptions; //non addressed mode
		if (selected) cmd[2] = ByteUtils.setBit(cmd[2],ISO_OPTION_Selected,true);
		cmd[3] = afi;
		
		sendReceive(cmd, "Could not write AFI");
		
		//Notify();
		return;
	}
	
	/**
	 * Prevents from further writings of the AFI code of an addressed transponder.
	 *
	 * @param uid Serial number of the addressed transponder
	 * @throws RFReaderException If unable to lock AFI. With message.
	 */
	public void lockAFI(byte[] uid) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[11];
		cmd[0] = (byte) 0xB0; //control byte: iso command
		cmd[1] = 0x28; //command code: lock afi
		cmd[2] = ByteUtils.setBit(this.isoOptions, ISO_OPTION_Addressed, true); //addressed mode
		ByteUtils.copy(uid,0,cmd,3);
		
		sendReceive(cmd, "Could not lock AFI");
		
		//Notify();
		return;
	}
	
	/**
	 * Prevents from further writings of the AFI code of non addressed
	 * or selected transponder.
	 *
	 * @param selected Specifies selected or non addressed mode.
	 * @throws RFReaderException If unable to lock AFI. With message.
	 */
	public void lockAFI(boolean selected) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[3];
		cmd[0] = (byte) 0xB0; //control byte: iso command
		cmd[1] = 0x28; //command code: lock afi
		cmd[2] = this.isoOptions ; //non addressed mode
		if (selected) cmd[2] = ByteUtils.setBit(cmd[2],ISO_OPTION_Selected,true);
		
		sendReceive(cmd, "Could not lock AFI");
		
		//Notify();
		return;
	}
	
	/**
	 * Writes the DSFID code (Data Storage Family ID) to an addressed transponder
	 *
	 * @param uid Serial number of the addressed transponder
	 * @param dsfid DSFID code to write
	 * @throws RFReaderException If unable to write DSFID code. With message.
	 */
	public  void writeDSFID(byte[] uid, byte dsfid) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[12];
		cmd[0] = (byte) 0xB0; //control byte: iso command
		cmd[1] = (byte) 0x29; //command code: write dsfid
		cmd[2] = ByteUtils.setBit(this.isoOptions, ISO_OPTION_Addressed, true); //addressed mode
		ByteUtils.copy(uid,0,cmd,3);
		cmd[11] = dsfid;
		
		sendReceive(cmd, "Could not write DSFID");
		
		//Notify();
		return;
	}
	
	/**
	 * Writes the DSFID code (Data Storage Family ID) to a non
	 * addressed or selected transponder.
	 *
	 * @param dsfid DSFID code to write
	 * @param selected Specifies selected or non addressed mode
	 * @throws RFReaderException If unable to write DSFID code. With message.
	 */
	public void writeDSFID(byte dsfid, boolean selected) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[4];
		cmd[0] = (byte) 0xB0; //control byte: iso command
		cmd[1] = (byte) 0x29; //command code: write dsfid
		cmd[2] = this.isoOptions ; //non addressed mode
		if (selected) cmd[2] = ByteUtils.setBit(cmd[2],ISO_OPTION_Selected,true);
		cmd[3] = dsfid;
		
		sendReceive(cmd, "Could not write DSFID");
		
		//Notify();
		return;
	}
	
	/**
	 * Prevents from further writings of the DSFID code of an addressed transponder.
	 *
	 * @param uid Serial number of the addressed transponder
	 * @throws RFReaderException If unable to lock DSFID. With message.
	 */
	public void lockDSFID(byte[] uid) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[11];
		cmd[0] = (byte) 0xB0; // control byte: iso command
		cmd[1] = 0x2A; // command code: lock dsfid
		cmd[2] = ByteUtils.setBit(this.isoOptions, ISO_OPTION_Addressed, true); // addressed mode
		ByteUtils.copy(uid,0,cmd,3);
		
		sendReceive(cmd, "Could not lock DSFID");
		
		//Notify();
		return;
	}
	
	/**
	 * Prevents from further writings of the DSFID code of non addressed
	 * or selected transponder.
	 *
	 * @param selected Specifies selected or non addressed mode.
	 * @throws RFReaderException If unable to lock DSFID. With message.
	 */
	public void lockDSFID(boolean selected) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[3];
		cmd[0] = (byte) 0xB0; // control byte: iso command
		cmd[1] = 0x2A; // command code: lock dsfid
		cmd[2] = this.isoOptions; // non addressed mode
		if (selected) cmd[2] = ByteUtils.setBit(cmd[2],ISO_OPTION_Selected,true);
		
		sendReceive(cmd, "Could not lock DSFID");
		
		//Notify();
		return;
	}
	
	/**
	 * Reads the security status of data blocks of an addressed transponder. <br/>
	 *
	 * @param uid Serial number of the addressed transponder
	 * @param start 0-based index of the first block to consider
	 * @param nBlocks Number of blocks to consider
	 * @return An array with the security status of the considered data blocks.
	 * Every byte has the following meaning: <br/>
	 * 0x00 : Unlocked <br/>
	 * 0x01 : User locked <br/>
	 * 0x02 : Factory locked
	 *
	 * @throws RFReaderException  If unable to get security status. With message
	 */
	public  byte[] getBlockSecurityStatus(byte[] uid, int start, int nBlocks) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[13];
		cmd[0] = (byte) 0xB0; // control byte: iso command
		cmd[1] = 0x2C; // command code: get block security status
		cmd[2] = ByteUtils.setBit(this.isoOptions, ISO_OPTION_Addressed, true); // addressed mode
		ByteUtils.copy(uid,0,cmd,3);
		cmd[11] = (byte) start;
		cmd[12] = (byte) nBlocks;
		
		sendReceive(cmd, "Could not read security status");
		
		int nblocchi = recv_buf[5] & 0xFF;
		byte[] result = new byte[nblocchi];
		ByteUtils.copy(recv_buf, 6, result, 0, nblocchi);
		
		//Notify();
		return result;
	}
	
	/**
	 * Reads the security status of data blocks of an addressed transponder. <br/>
	 *
	 * @param start 0-based index of the first block to consider
	 * @param nBlocks Number of blocks to consider
	 * @param selected Specifies selected or non addressed mode
	 * @return An array with the security status of the considered data blocks.
	 * Every byte has the following meaning: <br/>
	 * 0x00 : Unlocked <br/>
	 * 0x01 : User locked <br/>
	 * 0x02 : Factory locked
	 *
	 * @throws RFReaderException  If unable to get security status. With message
	 */
	public  byte[] getBlockSecurityStatus(int start, int nBlocks, boolean selected) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[5];
		cmd[0] = (byte) 0xB0; // control byte: iso command
		cmd[1] = (byte) 0x2C; // command code: get block security status
		cmd[2] = this.isoOptions; // addressed mode
		if (selected) cmd[2] = ByteUtils.setBit(cmd[2],ISO_OPTION_Selected,true);
		cmd[3] = (byte) start;
		cmd[4] = (byte) nBlocks;
		
		sendReceive(cmd, "Could not read security status");
		
		int nblocchi = recv_buf[5] & 0xFF;
		byte[] result = new byte[nblocchi];
		ByteUtils.copy(recv_buf, 6, result, 0, nblocchi);
		
		//Notify();
		return result;
	}
	
	/**
	 * Sets the addressed transponder in Quiet state. This means that it will
	 * not respond to further Inventory commands until it is put in to Ready
	 * state (see <see cref="resetToReady"/>) or it is powered down.
	 *
	 * @param uid Serial nuumber of the addressed transponder
	 * @throws RFReaderException If unable to perform operation. With message.
	 */
	public  void stayQuiet(byte[] uid) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[11];
		cmd[0] = (byte) 0xB0; // control byte: iso command
		cmd[1] = 0x02; // command code
		cmd[2] = ByteUtils.setBit(this.isoOptions, ISO_OPTION_Addressed, true);
		ByteUtils.copy(uid,0,cmd,3);
		
		sendReceive(cmd, "Could not set Stay Quiet");
		
		//Notify();
		return;
	}
	
	/**
	 * Sets the addressed transponder in Selected state. Only transponders
	 * in this state can respond to commands addressed to "selected" tags
	 *
	 * @param uid Serial number of the addressed transponder
	 * @throws RFreaderException If unable to perform operation. With message.
	 */
	public  void select(byte[] uid) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[11];
		cmd[0] = (byte) 0xB0; //control byte: iso command
		cmd[1] = 0x25; //command code
		cmd[2] = ByteUtils.setBit(this.isoOptions, ISO_OPTION_Addressed, true); //mode
		ByteUtils.copy(uid,0,cmd,3);
		
		sendReceive(cmd, "Could not set Stay Quiet");
		
		//Notify();
		return;
	}
	
	/**
	 * Sets the addressed transponder in Ready state. It means that now the tag
	 * will respond to Inventory commands.
	 *
	 * @param uid Serial number of the addressed transponder
	 * @throws RFreaderException If unable to perform operation. With message.
	 */
	public  void resetToReady(byte[] uid) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[11];
		cmd[0] = (byte) 0xB0; //control byte: iso command
		cmd[1] = 0x26; //command code
		cmd[2] = ByteUtils.setBit(this.isoOptions, ISO_OPTION_Addressed, true); //mode
		ByteUtils.copy(uid,0,cmd,3);
		
		sendReceive(cmd, "Could not Reset to Ready");
		
		//Notify();
		return;
	}
	
	/**
	 * Sets transponders in RF field to Ready state. It means that now tags
	 * will respond to Inventory commands.
	 *
	 * @param selected Specifies selected or non addressed mode
	 * @throws RFreaderException If unable to perform operation. With message.
	 */
	public  void resetToReady(boolean selected) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[3];
		cmd[0] = (byte) 0xB0; //control byte: iso command
		cmd[1] = 0x26; //command code
		cmd[2] = this.isoOptions;
		if (selected)
			cmd[2] = ByteUtils.setBit(this.isoOptions, ISO_OPTION_Selected, true); //mode
		
		sendReceive(cmd, "Could not Reset to Ready");
		
		//Notify();
		return;
	}
	
	/**
	 * Gets some information about the addressed transponder.
	 *
	 * @param uid Serial number of the addressed transponder.
	 * @return The SysInfo struct which contains all retrieved information.
	 * @throws RFReaderException If could not read information throws an
	 *
	 */
	public SysInfo getSystemInformation(byte[] uid) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[11];
		cmd[0] = (byte) 0xB0;
		cmd[1] = 0x2B; //command code
		cmd[2] = ByteUtils.setBit(this.isoOptions, ISO_OPTION_Addressed, true); //mode
		ByteUtils.copy(uid,0,cmd,3,uidSize);
		
		sendReceive(cmd, "Could not read information");
		
		byte flag = recv_buf[5];
		SysInfo result = new SysInfo();
		
		result.uid = new byte[uidSize];
		ByteUtils.copy(recv_buf, 6, result.uid, 0, uidSize);
		
		int cnt = 14;
		if (ByteUtils.getBit(flag, 0))  //DSFID presente
		{
			result.DSFID = recv_buf[cnt++];
			result.validDSFID = true;
		}
		
		if (ByteUtils.getBit(flag, 1)) //AFI presente
		{
			result.AFI = recv_buf[cnt++];
			result.validAFI = true;
		}
		
		if (ByteUtils.getBit(flag, 2)) //mem size presente
		{
			result.nBlocks = recv_buf[cnt++] + 1 ;
			result.blockSize = (byte)((recv_buf[cnt++] & 0x1F) + 1);
			result.validMemorySize = true;
		}
		
		if (ByteUtils.getBit(flag, 3)) //IC REF presente
		{
			result.IC_REF = recv_buf[cnt++];
			result.validIC_REF = true;
		}
		
		//Notify();
		return result;
	}
	
	/**
	 * Gets some information about a selected or non-addressed transponder.
	 *
	 * @param selected Specifies selected or non addressed mode.
	 * @return The SysInfo struct which contains all retrieved information.
	 * @throws RFReaderException If could not read information throws an
	 */
	public SysInfo getSystemInformation(boolean selected) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[3];
		cmd[0] = (byte) 0xB0;
		cmd[1] = 0x2B; //command code
		cmd[2] = this.isoOptions;
		if (selected)
			cmd[2] = ByteUtils.setBit(this.isoOptions, ISO_OPTION_Selected, true); //mode
		
		sendReceive(cmd, "Could not read information");
		
		SysInfo result = new SysInfo() ;
		
		byte flag = recv_buf[5];
		
		result.uid = new byte[uidSize];
		ByteUtils.copy(recv_buf, 6, result.uid, 0, uidSize);
		
		int cnt = 14;
		if (ByteUtils.getBit(flag, 0))  //DSFID presente
		{
			result.DSFID = recv_buf[cnt++];
			result.validDSFID = true;
		}
		
		if (ByteUtils.getBit(flag, 1)) //AFI presente
		{
			result.AFI = recv_buf[cnt++];
			result.validAFI = true;
		}
		
		if (ByteUtils.getBit(flag, 2)) //mem size presente
		{
			result.nBlocks = recv_buf[cnt++] + 1 ;
			result.blockSize = (byte)((recv_buf[cnt++] & 0x1F) + 1);
			result.validMemorySize = true;
		}
		
		if (ByteUtils.getBit(flag, 3)) //IC REF presente
		{
			result.IC_REF = recv_buf[cnt++];
			result.validIC_REF = true;
		}
		
		//Notify();
		return result;
	}
	
	/** This command sets the EAS bit to 1 in all tags in RF field. <br/>
	 * <i>This command works only on Philips transponders.</i>
	 * @param selected Specifies selected or non addressed mode.
	 * @throws RFReaderException If unable to perform operation. With message.
	 */
	public  void EAS_set(boolean selected) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[4];
		cmd[0] = (byte) 0xB1; //control byte: iso command custom
		cmd[1] = this.isoOptions; //iso flag
		if (selected)
			cmd[1] = ByteUtils.setBit(this.isoOptions, ISO_OPTION_Selected, true);
		cmd[2] = (byte)0xA2; //command code
		cmd[3] = 0x04; //philips manufacturer code
		
		sendReceive(cmd, "Could not set EAS");
		
		//Notify();
		return;
	}
	
	/** This command sets the EAS bit to 1 in all tags in RF field. <br/>
	 * <i>This command works only on Philips transponders.</i>
	 * @param uid Serial number of the addressed transponder.
	 * @throws RFReaderException If unable to perform operation. With message.
	 */
	public  void EAS_set(byte[] uid) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[12];
		cmd[0] = (byte) 0xB1; //control byte: iso command custom
		cmd[1] = ByteUtils.setBit(this.isoOptions, ISO_OPTION_Addressed, true); //iso flag
		cmd[2] = (byte)0xA2; //command code
		cmd[3] = 0x04; //philips manufacturer code
		ByteUtils.copy(uid, 0, cmd, 4, uidSize);
		
		sendReceive(cmd, "Could not set EAS");
		
		//Notify();
		return;
	}
	
	/** This command sets the EAS bit to 0 in all tags in RF field. <br/>
	 * <i>This command works only on Philips transponders.</i>
	 * @param selected Specifies selected or non addressed mode.
	 * @throws RFReaderException If unable to perform operation. With message.
	 */
	public void EAS_reset(boolean selected) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[4];
		cmd[0] = (byte) 0xB1; //control byte: iso command custom
		cmd[1] = this.isoOptions; //iso flag
		if (selected)
			cmd[1] = ByteUtils.setBit(this.isoOptions, ISO_OPTION_Selected, true);
		cmd[2] = (byte)0xA3; //command code
		cmd[3] = 0x04; //philips manufacturer code
		
		sendReceive(cmd, "Could not reset EAS");
		
		//Notify();
		return;
	}

	/** This command sets the EAS bit to 0 in all tags in RF field. <br/>
	 * <i>This command works only on Philips transponders.</i>
	 * @param uid Serial number of the addressed transponder.
	 * @throws RFReaderException If unable to perform operation. With message.
	 */
	public void EAS_reset(byte[] uid) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[12];
		cmd[0] = (byte) 0xB1; //control byte: iso command custom
		cmd[1] = ByteUtils.setBit(this.isoOptions, ISO_OPTION_Addressed, true); //iso flag
		cmd[2] = (byte)0xA3; //command code
		cmd[3] = 0x04; //philips manufacturer code
		ByteUtils.copy(uid, 0, cmd, 4, uidSize);
		
		sendReceive(cmd, "Could not reset EAS");
		
		//Notify();
		return;
	}
	
	/** This command locks the current EAS state in all tags in RF field. <br/>
	 * <i>This command works only on Philips transponders.</i>
	 * @param selected Specifies selected or non addressed mode.
	 * @throws RFReaderException If unable to perform operation. With message.
	 */
	public  void EAS_lock(boolean selected) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[4];
		cmd[0] = (byte) 0xB1; //control byte: iso command custom
		cmd[1] = this.isoOptions; //iso flag
		if (selected)
			cmd[1] = ByteUtils.setBit(this.isoOptions, ISO_OPTION_Selected, true);
		cmd[2] = (byte)0xA4; //command code
		cmd[3] = 0x04; //philips manufacturer code
		
		sendReceive(cmd, "Could not lock EAS");
		
		//Notify();
		return;
	}

	/** This command locks the current EAS state in all tags in RF field. <br/>
	 * <i>This command works only on Philips transponders.</i>
	 * @param uid Serial number of the addressed transponder.
	 * @throws RFReaderException If unable to perform operation. With message.
	 */
	public void EAS_lock(byte[] uid) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[12];
		cmd[0] = (byte) 0xB1; //control byte: iso command custom
		cmd[1] = ByteUtils.setBit(this.isoOptions, ISO_OPTION_Addressed, true);
		cmd[2] = (byte)0xA4; //command code
		cmd[3] = 0x04; //philips manufacturer code
		ByteUtils.copy(uid, 0, cmd, 4, uidSize);
		
		sendReceive(cmd, "Could not lock EAS");
		
		//Notify();
		return;
	}
	
	/**
	 * Checks if there are transponders with EAS bit set to 1 in the RF field.
	 * @return If the EAS bit is set to 1 in a transponder in the RF field, the
	 * 32 bytes long EAS response is returned. Otherwise returns <i>null</i>.
	 * @param selected Specifies selected or non addressed mode.
	 * @throws RFReaderException If unable to perform operation.With message.
	 */
	public byte[] EAS_alarm(boolean selected) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[4];
		cmd[0] = (byte) 0xB1; //control byte: iso command custom
		cmd[1] = this.isoOptions; //iso flag
		if (selected)
			cmd[1] = ByteUtils.setBit(this.isoOptions, ISO_OPTION_Selected, true);
		cmd[2] = (byte)0xA5; //command code
		cmd[3] = 0x04; //philips manufacturer code
		
		try {
			send(cmd);
			receive();
		}
		catch (IOException e) {
			//Notify();
			throw new RFReaderException(errMsg("Serial communication problem"));
		}
		
		if (recv_buf[4] != 0x00) {
			//Notify();
			return null;
		}
		
		byte[] result = new byte[32];
		ByteUtils.copy(recv_buf, 6, result, 0, 32);
		
		//Notify();
		return result;
	}
	
	/**
	 * Checks if there are transponders with EAS bit set to 1 in the RF field.
	 * @return If the EAS bit is set to 1 in a transponder in the RF field, the
	 * 32 bytes long EAS response is returned. Otherwise returns <i>null</i>.
	 * @param uid Serial number of the addressed transponder.
	 * @throws RFReaderException If unable to perform operation.With message.
	 */
	public byte[] EAS_alarm(byte[] uid) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[12];
		cmd[0] = (byte) 0xB1; //control byte: iso command custom
		cmd[1] = ByteUtils.setBit(this.isoOptions, ISO_OPTION_Addressed, true);
		cmd[2] = (byte)0xA5; //command code
		cmd[3] = 0x04; //philips manufacturer code
		ByteUtils.copy(uid, 0, cmd, 4, uidSize);
		
		try {
			send(cmd);
			receive();
		}
		catch (IOException e) {
			//Notify();
			throw new RFReaderException(errMsg("Serial communication problem"));
		}
		
		if (recv_buf[4] != 0x00) {
			//Notify();
			return null;
		}
		
		byte[] result = new byte[32];
		ByteUtils.copy(recv_buf, 6, result, 0, 32);
		
		//Notify();
		return result;
	}
}