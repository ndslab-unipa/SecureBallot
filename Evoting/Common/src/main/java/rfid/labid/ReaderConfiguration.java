
package rfid.labid;

/**
 * Represents all configurations stored in internal registers of the reader.
 */
public class ReaderConfiguration {
	
	/** Creates a new instance of ReaderConfiguration */
	public ReaderConfiguration() {
	}
	
	/**
	 * Specifies if communication between reader and tag should use a
	 * high data rate. Default: true;
	 */
	public boolean HighDataRate ;
	
	/**
	 * Specifies if communication between reader and tag should use a dual
	 * subcarrier. Default: false.
	 */
	public boolean DualSubcarrier ;
	
	/**
	 * Specifies if communication between reader and tag should use a 1/256
	 * encoding (slower) or 1/4 (faster). Default: false;
	 */
	public boolean VCDDataRate256 ;
	
	/**
	 * Specifies if reader should first try single time slot inventories
	 * (faster if there is only 1 tag in the RF field, slower otherwise).
	 * Default: false;
	 */
	public boolean TimeSlot1 ;
	
	/**
	 * Specifies if reader should use AFI code in inventories.
	 * Default: false.
	 */
	public boolean AfiEnabled ;
	
	/**
	 * Specifies if reader should retrieve also security status during read
	 * block operations. Defaul: false.
	 */
	public boolean SecurityStatus ;
	
	/**
	 * Specifies the default protocol: <br/>
	 * 0xA0 - ISO14443a <br/>
	 * 0xB0 - ISO15693  <br/>
	 * <b>Don't specify different values.</b>
	 * Default: 0xA0.
	 */
	public byte DefaultProtocol ;
	
	/**
	 * Tells the reader to use only the Texas Instruments convention for
	 * write operations. Set
	 * this to true only if you are sure that you don't use tags from
	 * other manufacturers. Default: false.
	 */
	public boolean TI ;
	
	/**
	 * Specifies the serial port baudrate. It can assume the following values: <br/>
	 * 9   - baudrate = 9600<br/>
	 * 19  - baudrate = 19200<br/>
	 * 38  - baudrate = 38400<br/>
	 * 57  - baudrate = 57600<br/>
	 * 115 - baudrate = 115200<br/>
	 * Any different value enables the default factory level (115200).
	 */
	public byte Baudrate;
	
	/**
	 * Tells the reader to produce a short acoustic signal when an operation
	 * is successful
	 */
	public boolean BeepOnSuccess ;
	
	/**
	 * Tells the reader to produce a short acoustic signal when an operation
	 * is not successful. The pitch of the sound is different from the "success"
	 * sound.
	 */
	public boolean BeepOnFailure ;
	
	/**
	 * Switches RF on before every command, then switches it off. <b>This feature
	 * makes useless ISO15693 Select command and all ISO14443 commands except for
	 * ActivateIdleA.</b><br/>
	 * Default: false;
	 */
	public boolean AutoRfOff;
	
	/**
	 * If true, the reader handles all ISO15693 serial numbers from Most Significant
	 * Byte to Less Significant Byte (MSB-LSB).<br/>
	 * Default: false;
	 */
	public boolean MSB_first_ISO15693_UID;
	
	/**
	 * If true, the reader handles all ISO15693 data blocks from Most Significant
	 * Byte to Less Significant Byte (MSB-LSB).<br/>
	 * Default: false;
	 */
	public boolean MSB_first_ISO15693_DataBlocks;
	
	/**
	 * Enables automatic search of transponders. If Scan mode is enabled, the reader
	 * will send asynchronously through the communication channel all selected data
	 * (UID, data blocks) as soon as it reads them. Avoid using Scan mode and other
	 * commands on transponders at the same time, in order to exclude confusion
	 * between reply data.
	 */
	public boolean Scan_Enabled;
	
	/**
	 * If true and if the reader is in Scan mode it will send UID of ISO15693 transponders.
	 */
	public boolean Scan_ReadUid;
	
	/**
	 * If true and if the reader is in Scan mode it will send data blocks of ISO15693
	 * transponders according to <see cref="Scan_FirstBlock"/> and
	 * <see cref="Scan_NBlocks"/> parameters.
	 */
	public boolean Scan_ReadDataBlocks;
	
	/**
	 * If true and if the reader is in Scan mode it will execute read operations as
	 * fast as possible, otherwise only 3-5 times per second.
	 */
	public boolean Scan_Fast;
	
	/**
	 * If true, puts any detected ISO15693 transponder in quiet mode in order to
	 * avoid duplicate detections.
	 */
	public boolean Scan_SingleRead;
	
	/**
	 * If true, sends data in hex ASCII encoding instead of plain bytes.
	 */
	public boolean Scan_AsciiOutput;
	
	/**
	 * If true, ignores the last detected ISO15693 transponder.
	 */
	public boolean Scan_IgnoreLast;
	
	/**
	 * If true, writes 'LIOK' block 27 of any detected ISO15693 transponder
	 */
	public boolean Scan_WriteOk;
	
	/**
	 * Specifies the first data block to be read from ISO15693 transponders.
	 */
	public int Scan_FirstBlock;
	
	/**
	 * Specifies how many data block to be read from ISO15693 transponders.
	 */
	public int Scan_NBlocks;
	
	/**
	 * If true, Scan mode can read also UIDs of ISO14443A transponders.
	 */
	public boolean Scan_ISO14443A;
}
