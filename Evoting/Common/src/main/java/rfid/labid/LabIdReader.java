package rfid.labid;
import rfid.labid.IO.*;
import java.io.*;

/**
 * is a base class which provides methods to control and configure
 * LAB ID multi standard RFID reader. According to your needs, you can
 * instantiate a {@link labid.iso15693.ISO15693Reader} or {@link labid.iso14443.ISO14443Reader}
 * object, which provide methods for ISO commands.
 */
public class LabIdReader extends LocalRFReader implements Runnable {
	protected byte[] recv_buf;
	
	
	/**
	 * No. of EEPROM config registers used on reader.
	 */
	protected final int nConfigurationRegisters = 9;
	
	protected static final byte ERR_CRC            = 0x02;
	protected static final byte OK                 = 0x00;
	protected static final byte ERR_NO_TRANSPONDER = 0x01;
	protected static final byte ERR_ISO            = (byte)0x95;
	protected static final byte ERR_COLLISION      = 0x0B;
	protected static final byte ERR_FORMAT         = 0x04;
	protected static final byte ERR_FRAMING        = 0x05;
	protected static final byte ERR_WRITE          = 0x03;
	protected static final byte ERR_KEY            = (byte)0xF7;
	
	public static final int RF_ISOProtocol_None      = 0x00;
	public static final int RF_ISOProtocol_Any       = 0x0F;
	public static final int RF_ISOProtocol_ISO15693  = 0x01;
	public static final int RF_ISOProtocol_ISO14443A = 0x02;
	public static final int RF_ISOProtocol_ISO14443B = 0x04;
	public static final int RF_ISOProtocol_EPC       = 0x08;
	
	protected boolean notificationThreadRunning = false;
	protected RFIDTagDetectedListener eventListener;
	
	/**
	 * Instantiates a new LabIdReader object.
	 */
	public LabIdReader() {
		recv_buf = new byte[512];
	}
	
	/**
	 * Instantiates a new LabIdReader object connected through a SerialStream object
	 *
	 * @param stream Communication stream.
	 */
	public LabIdReader(CableStream stream) {
		recv_buf = new byte[512];
		this.sp = stream;
	}

	public CableStream getStream()
	{
		return sp;
	}
	
	/**
	 * Opens a serial port with default parameters (baudrate = 115200,
	 * no parity, 1 stop bit)
	 *
	 * @param portName Name of the serial port (ex. "COM1")
	 * @throws RFReaderException  If unable to open the port
	 */
	public  void openSerialPort(String portName) throws RFReaderException {
		// revisione daniele
		openSerialPort(portName, 115200, SerialStream.FLOWCONTROL_NONE, SerialStream.PARITY_NONE, SerialStream.STOPBITS_1);
	}
	
	public void close() throws IOException {
		this.notificationThreadRunning = false;
		super.close();
	}
	
	protected int crc16(byte[] data, int len) {
		//copiato da doc Texas - revisione daniele
		int CRC_POLYNOM = 0x8408;
		int CRC_PRESET = 0xFFFF;
		int crc = CRC_PRESET;
		
		int[] intdata = new int[len];
		for (int i = 0; i < len; i++)
			intdata[i] = (int)data[i] & 0xFF;
		
		for (int i = 0; i < len; i++) {
			crc ^= intdata[i];
			for (int j = 0; j < 8; j++) {
				if ((crc & 0x01) == 1)
					crc = (crc >> 1) ^ CRC_POLYNOM;
				else
					crc = (crc >> 1);
			}
		}
		return crc;
	}
	
	protected void  receive() throws IOException {
		int nLetti = 0;
		int tentativi = this.nTentativi;
		int daLeggere;
		
		while ((nLetti == 0) && (tentativi-- > 0)) {
			nLetti = sp.Read(recv_buf, 0, 2); //recv_buf.length);
		}
		if (tentativi < 0)
			throw new IOException();
		
		daLeggere = (recv_buf[0] & 0xFF) + ((recv_buf[1] & 0x01) << 8);
		int rimasti = daLeggere - nLetti;
		
		tentativi = this.nTentativi;
		while ((rimasti > 0) && (tentativi-- > 0)) {
			//sp.enableReceiveThreshold(rimasti);
			nLetti += sp.Read(recv_buf, nLetti, rimasti);
			rimasti = daLeggere - nLetti;
		}
		
		if (tentativi < 0)
			throw new IOException();
		
		//check del crc
		int crc = crc16(recv_buf, nLetti);
		if (crc != 0)
			throw new IOException("CRC error");
	}
	
	protected void send(byte[] data) throws IOException {
		int len = data.length ;
		int totalLen = len + 4; //4 � il numero di byte aggiunti come header e footer
		byte[] dataToSend = new byte[totalLen];
		
		dataToSend[0] = (byte)totalLen; //lungh. del messaggio
		dataToSend[1] = (byte)0x00; //address (non usato in connessione con pc)
		
		ByteUtils.copy(data, 0, dataToSend, 2, len); //copio il contenuto da inviare
		
		int crc = crc16(dataToSend, totalLen - 2); //aggiungo in coda il crc16
		dataToSend[totalLen - 2] = (byte)(crc & 0xFF);
		dataToSend[totalLen - 1] = (byte)((crc >> 8) & 0xFF);
		
		try {
			sp.Write(dataToSend);
		} catch (IOException e) {
			throw e;
		}
	}
	
	protected void sendReceive(byte[] toSend, String errMessage) throws RFReaderException {
		try {
			send(toSend);
			receive();
		} catch (Exception e) {
			//Notify();
			throw new RFReaderException(errMsg("Serial communication problem"));
		}
		
		int result = recv_buf[4] & 0xFF;
		
		if (result != 0) {
			//Notify();
			throw new RFReaderException(errMsg(errMessage));
		}
	}
	
	protected void sendReceive(byte[] toSend, String errMessage, int detail) throws RFReaderException {
		try {
			send(toSend);
			receive();
		} catch (Exception e) {
			//Notify();
			throw new RFReaderException(errMsg("Serial communication problem"), detail);
		}
		
		int result = recv_buf[4] & 0xFF;
		
		if (result != 0) {
			//Notify();
			throw new RFReaderException(errMsg(errMessage));
		}
	}
	
	
	/**
	 * Turns off the RF field so that all transponders are
	 * powered off, then turns the RF field on on again.
	 * All transponders are reset to Ready state.
	 *
	 * @throws RFReaderException If unable to perform operation. With message.
	 */
	public void rfReset() throws RFReaderException {
		//Wait();
		
		//int result;
		
		byte[] cmd = new byte[2];
		cmd[0] = 0x00;			//control byte: reader command
		cmd[1] = (byte) 0x69;   //command code: RF reset
		
		sendReceive(cmd, "Could not reset RF");
	}
	
	
	/**
	 * Sets internal registers of the RFID reader to factory settings.
	 *
	 * @throws RFReaderException If unable to perform operation.
	 * With message.
	 */
	public void setDefaultConfiguration() throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[2];
		cmd[0] = (byte) 0x00;
		cmd[1] = (byte) 0x83;
		
		sendReceive(cmd, "Could not set Default Configuration");
		
		//Notify();
	}
	
	/**
	 * Switches on or off the RF field. All ISO commands when RF is off will fail.
	 * Inventory commands will automatically switch on RF.
	 *
	 * @param mode If 0 sets RF to OFF, else to ON.
	 * @throws RFReaderException If operation fails. With message.
	 */
	public void rfOnOff(int mode) throws RFReaderException {
		//Wait();
		
		//int result;
		
		byte[] cmd = new byte[3];
		cmd[0] = 0x00;
		cmd[1] = 0x6A; //control byte: RF on/off command
		cmd[2] = (byte)(mode & 0xFF); //1 = on; 0 = off
		
		sendReceive(cmd, "Could not set RF state");
	}
	
	/**
	 * Writes a configuration register on the reader.
	 *
	 * @param ram If 0 writes to EEPROM, else only to RAM.
	 * @param cfgAddr Index of config register.
	 * @param data The new value of the register.
	 */
	protected void setReaderConfiguration(byte ram, int cfgAddr, byte data ) throws RFReaderException {
		if (cfgAddr >= nConfigurationRegisters)
			throw new RFReaderException("Config address out of range");
		
		//Wait();
		
		byte[] cmd = new byte[5];
		cmd[0] = 0x00;
		cmd[1] = (byte)0x81;
		cmd[2] = ram;
		cmd[3] = (byte)(cfgAddr & 0xFF);
		cmd[4] = data;
		
		sendReceive(cmd, "Could not set reader configuration",cfgAddr);
		
		//Notify();
	}
	
	/**
	 * Writes a new configuration to the reader. <br/>
	 * NB: if you specify a new baudrate it will be effective only after a reboot
	 * of the reader.
	 *
	 * @param Settings A ReaderConfiguration struct which represents the new
	 * settings of the reader.
	 * @param saveEEPROM Specifies if saving new configuration to
	 * the EEPROM of the reader, in order to keep new settings after powering
	 * down.
	 * @throws RFReaderException If unable to perform operation.
	 * With message.
	 */
	public void setReaderConfiguration(ReaderConfiguration Settings, boolean saveEEPROM) throws RFReaderException {
		byte[] reg = new byte[nConfigurationRegisters];
		
		byte ram;
		if (saveEEPROM) ram = 0;
		else ram = 1;
		
		reg[0] = 0;
		reg[0] = ByteUtils.setBit(reg[0], 0, Settings.DualSubcarrier);
		reg[0] = ByteUtils.setBit(reg[0], 1, Settings.HighDataRate);
		reg[0] = ByteUtils.setBit(reg[0], 2, Settings.VCDDataRate256);
		reg[0] = ByteUtils.setBit(reg[0], 7, Settings.AutoRfOff);
		
		reg[1] = 0;
		reg[1] = ByteUtils.setBit(reg[1], 4, Settings.AfiEnabled);
		reg[1] = ByteUtils.setBit(reg[1], 5, Settings.TimeSlot1);
		reg[1] = ByteUtils.setBit(reg[1], 6, Settings.MSB_first_ISO15693_DataBlocks);
		reg[1] = ByteUtils.setBit(reg[1], 7, Settings.MSB_first_ISO15693_UID);
		
		reg[2] = 0;
		reg[2] = ByteUtils.setBit(reg[2], 4, Settings.SecurityStatus);
		reg[2] = ByteUtils.setBit(reg[2], 0, Settings.BeepOnSuccess);
		reg[2] = ByteUtils.setBit(reg[2], 1, Settings.BeepOnFailure);
		
		reg[3] = Settings.DefaultProtocol;
		
		reg[4] = 0;
		reg[4] = ByteUtils.setBit(reg[4], 6, Settings.TI);
		
		reg[5] = Settings.Baudrate;
		
		reg[6] = ByteUtils.setBit(reg[6], 0, Settings.Scan_Enabled);
			reg[6] = ByteUtils.setBit(reg[6], 1, Settings.Scan_ReadUid);
			reg[6] = ByteUtils.setBit(reg[6], 2, Settings.Scan_ReadDataBlocks);
			reg[6] = ByteUtils.setBit(reg[6], 3, !Settings.Scan_Fast);
			reg[6] = ByteUtils.setBit(reg[6], 4, !Settings.Scan_SingleRead);
			reg[6] = ByteUtils.setBit(reg[6], 5, Settings.Scan_AsciiOutput);
			reg[6] = ByteUtils.setBit(reg[6], 6, Settings.Scan_IgnoreLast);
			reg[6] = ByteUtils.setBit(reg[6], 7, Settings.Scan_WriteOk);

			reg[7] = (byte)Settings.Scan_FirstBlock;
			reg[8] = (byte)Settings.Scan_NBlocks;
		
		for (int i=0; i < nConfigurationRegisters; i++) {
			this.setReaderConfiguration(ram, i, reg[i]);
		}
	}
	
	/**
	 * Reads the current configuration from the reader and stores it in the
	 * Settings field of the LabIdReader object.
	 *
	 * @return A ReaderConfiguration struct which contains the current
	 * @throws RFReaderException If unable to perform operation.
	 * With message.
	 */
	public ReaderConfiguration getReaderConfiguration() throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[2];
		cmd[0] = 0x00; //control byte: reader command
		cmd[1] = (byte)0x80; //command code: read configs
		
		sendReceive(cmd, "Could not get reader configurations");
		
		ReaderConfiguration Settings = new ReaderConfiguration();
		
		Settings.DualSubcarrier = ByteUtils.getBit(recv_buf[5], 0);
		Settings.HighDataRate = ByteUtils.getBit(recv_buf[5], 1);
		Settings.VCDDataRate256 = ByteUtils.getBit(recv_buf[5], 2);
		Settings.AutoRfOff = ByteUtils.getBit(recv_buf[5], 7);
		
		Settings.AfiEnabled = ByteUtils.getBit(recv_buf[6], 4);
		Settings.TimeSlot1 = ByteUtils.getBit(recv_buf[6], 5);
		Settings.MSB_first_ISO15693_UID = ByteUtils.getBit(recv_buf[6], 7);
		Settings.MSB_first_ISO15693_DataBlocks = ByteUtils.getBit(recv_buf[6], 6);
		
		Settings.SecurityStatus = ByteUtils.getBit(recv_buf[7], 6);
		Settings.BeepOnSuccess = ByteUtils.getBit(recv_buf[7], 0);
		Settings.BeepOnFailure = ByteUtils.getBit(recv_buf[7], 1);
		
		Settings.DefaultProtocol = recv_buf[8];
		
		Settings.TI = ByteUtils.getBit(recv_buf[9], 6);
		
		Settings.Baudrate = recv_buf[10];
		
		Settings.Scan_Enabled = ByteUtils.getBit(recv_buf[11], 0);
			Settings.Scan_ReadUid = ByteUtils.getBit(recv_buf[11], 1);
			Settings.Scan_ReadDataBlocks = ByteUtils.getBit(recv_buf[11], 2);
			Settings.Scan_Fast = !ByteUtils.getBit(recv_buf[11], 3);
			Settings.Scan_SingleRead = !ByteUtils.getBit(recv_buf[11], 4);
			Settings.Scan_AsciiOutput = ByteUtils.getBit(recv_buf[11], 5);
			Settings.Scan_IgnoreLast = ByteUtils.getBit(recv_buf[11], 6);
			Settings.Scan_WriteOk = ByteUtils.getBit(recv_buf[11], 7);
			Settings.Scan_FirstBlock = recv_buf[12] & 0xFF;
			Settings.Scan_NBlocks = recv_buf[13] & 0xFF;
			Settings.Scan_ISO14443A = ByteUtils.getBit(recv_buf[9], 2);
		
		//Notify();
		return Settings;
	}
	
	/**
		* Reads raw bytes from communication channel.
		* @param buffer Output buffer where read data will be stored
		* @return Number of read bytes (0 if none).
		*/
		public int scan(byte[] buffer) throws java.io.IOException
		{
			return this.sp.Read(buffer, 0, buffer.length);
		}

		/**
		* Reads raw bytes from communication channel.
		* @param buffer Output buffer where read data will be stored
		* @param offset Offset in the buffer.
		* @param count Maximum number of bytes to be read.
		* @return Number of read bytes (0 if none).
		 */
		public int scan(byte[] buffer, int offset, int count) throws java.io.IOException
		{
			return this.sp.Read(buffer, offset, (count > buffer.length ? buffer.length : count));
		}
	
	/**
	 * Gets the current firmware version.
	 *
	 * @return 5 bytes with the software version with this coding: <br/>
	 * 0 - Version number<br/>
	 * 1 - Subversion number<br/>
	 * 2 - Year<br/>
	 * 3 - Month<br/>
	 * 4 - Day<br/>
	 *
	 * @throws RFReaderException If unable to perform operation.
	 * With message.
	 */
	public byte[] getSoftwareVersion() throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[2];
		cmd[0] = 0x00;
		cmd[1] = (byte)0x65;
		
		sendReceive(cmd, "Could not get software version");
		
		byte[] result = new byte[5];
		ByteUtils.copy(recv_buf, 5, result, 0, 5);
		
		//Notify();
		return result;
	}
	
	/**
	 * Gets the 4 bytes long unique identifier of the reader.
	 *
	 * @return 4 bytes with the UID of the reader.
	 * @throws RFReaderException If unable to perform operation.
	 * With message.
	 */
	public byte[] getReaderUID() throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[2];
		cmd[0] = 0x00;
		cmd[1] = (byte)0x01;
		
		sendReceive(cmd, "Could not get reader UID");
		
		byte[] result = new byte[4];
		ByteUtils.copy(recv_buf, 5, result, 0, 4);
		
		//Notify();
		return result;
	}
	
	/**
	 * The reader beeps.
	 *
	 * @throws RFReaderException If unable to perform operation.
	 * With message.
	 */
	public void beep() throws RFReaderException {
		beep((byte)0x18,(byte) 0x1C);
	}
	
	/**
	 * The reader beeps.
	 *
	 * @param dSec Beep duration. Approx 1 = 1/10 second.
	 * @param freq Pitch of the played sound.
	 * @throws RFReaderException If unable to perform operation.
	 * With message.
	 */
	public void beep(byte dSec, byte freq) throws RFReaderException {
		//Wait();
		
		byte[] cmd = new byte[4];
		cmd[0] = 0x00;
		cmd[1] = (byte)0xBE;
		cmd[2] = dSec;
		cmd[3] = freq;
		
		sendReceive(cmd, "Could not beep");
		
		//Notify();
		return ;
	}
	
	/**
	 * Reads the current status of input pins. <br/>
	 * This command is not available in all hardware versions.
	 * @return Bitwise value of input pins.
	 * @throws RFReaderException If unable to perform operation.
	 * With message.
	 */
	public byte getInputPinsStatus() throws RFReaderException {
		byte[] cmd = new byte[2];
		
		cmd[0] = 0x00;
		cmd[1] = 0x53;
		
		sendReceive(cmd, "Could not get Input Pins status");
		
		return recv_buf[5];
	}
	
	/**
	 * Sets to logical 1 output pins according to bits in mask parameter. If pins were
	 * 0xF4 and you execute setOutputPinsStatus(0x03) the new pin configuration
	 * will be 0xF7 (0xF7 = 0xF4 OR 0x03). <br/>
	 * This command is not available in all hardware versions.
	 * @param mask Bitwise mask of bits to be set to 1.
	 * @throws RFReaderException If unable to perform operation.
	 * With message.
	 */
	public void setOutputPinsStatus(byte mask) throws RFReaderException {
		byte[] cmd = new byte[4];
		
		cmd[0] = 0x00;
		cmd[1] = 0x51;
		cmd[2] = mask;
		cmd[3] = 0; //time: forever
		
		sendReceive(cmd, "Could not set Output Pins status");
	}
	
	/**
	 * Sets to logical 0 output pins according to bits in mask parameter. If pins were
	 * 0xF4 and you execute setOutputPinsStatus(0x30) the new pin configuration
	 * will be 0xC4 (0xC4 = 0xF4 AND not(0x30)). <br/>
	 * This command is not available in all hardware versions.
	 * @param mask Bitwise mask of bits to be set to 0.
	 * @throws RFReaderException If unable to perform operation.
	 * With message.
	 */
	public void clearOutputPinsStatus(byte mask) throws RFReaderException {
		byte[] cmd = new byte[4];
		
		cmd[0] = 0x00;
		cmd[1] = 0x50;
		cmd[2] = mask;
		cmd[3] = 0; //time: forever
		
		sendReceive(cmd, "Could not clear Output Pins status");
	}
	
	/**
	 * @param defaultMsg
	 * @return new message
	 */
	protected String errMsg(String defaultMsg) {
		String msg;
		switch (recv_buf[4]) //esamina il byte di status
		{
			case ERR_COLLISION:
				msg = "Collision error. " + defaultMsg;
				break;
			case ERR_CRC:
				msg = "CRC error. " + defaultMsg;
				break;
			case ERR_FORMAT:
				msg = "Format error. " + defaultMsg;
				break;
			case ERR_FRAMING:
				msg = "Framing error. " + defaultMsg;
				break;
			case ERR_ISO:
				msg = "ISO error. " + defaultMsg;
				break;
			default:
				msg = defaultMsg;
				break;
		}
		return msg;
	}
	
	/**
	 * Not used.
	 */
	protected synchronized void Wait() {
		while (!available) {
			try {
				this.wait();
			} catch (Exception e) {
			}
		}
		available = false;
	}
	
	/**
	 * Not used.
	 */
	protected synchronized void Notify() {
		available = true;
		notifyAll();
	}
	
	/**
	 *Allows your application to terminate the "new tag" event notification thread.
	 */
	public void stopTagEventNotification() throws IOException {
		byte[] cmd = new byte[3];
		
		cmd[0] = 0x00; //reader command
		cmd[1] = (byte)0xE0; //tag event
		cmd[2] = 0x00; //stop tag events
		
		send(cmd);
		
		if (this.notificationThreadRunning) {
			// aspetto che il thread di notifica sia terminato
			this.notificationThreadRunning = false;
			this.available = false;
			this.Wait();
		}
	}
	
	/**
	 * Begins the search for a new tag in the RF field. As soon as a new tag is
	 * detected, the {@link labid.RFIDTagDetectedListener#TagDetected(int)} event is fired. <br/>
	 * A new tag detection occurs when, after a failed detection (no tags found), a transponder
	 * enters the RF field. This means that if you put a tag in field you get an event
	 * notification. If then you <i>add</i> another tag without removing the previous one,
	 * you will not get a new notification, because there must be a short time where no
	 * tags are in RF field.<br/>
	 * Note that while your application is waiting for a "new tag" event notification,
	 * you cannot send other commands to the reader, because its response would be
	 * "stolen" by the background event notification thread. If you need to send
	 * commands to the reader, stop the receiver thread using {@link #stopTagEventNotification()}.
	 * Your application should subscribe the event and declare an appropriate event
	 * handler. <br/>
	 * For example: <br/>
	 * <pre>
	 * public class MyClass implements RFIDTagDetectedListener
	 * {
	 * private LabIdReader reader ;
	 *
	 * // ... instantiate reader and open serial port ...
	 *
	 * private void SubscribeTagEventHandler()
	 * {
	 *	//this line subscribes the TagEventReceiver method to the event.
	 *	//You can set only one subscription.
	 *	this.reader.addTagEventListener(this);
	 * }
	 *
	 * private void LookForTags()
	 * {
	 *	this.reader.getNextTagEvent(LabIdReader.RF_ISOProtocol_Any, false);
	 * }
	 *
	 * void TagDetected(int protocol);
	 * {
	 *	switch (protocol)
	 * 	{
	 * 		case (LabIdReader.RF_ISOProtocol_ISO15693)
	 * 			System.out.writeln("A ISO15693 tag entered the RF field");
	 * 			break;
	 * 		case (LabIdReader.RF_ISOProtocol_ISO14443A)
	 * 			System.out.writeln("A ISO14443 Type A tag entered the RF field");
	 * 			break;
	 * 	}
	 *
	 * 	//this starts the search for another tag
	 * 	if (lookingForAnotherTag)
	 * 		this.reader.getNextTagEvent(LabIdReader.RF_ISOProtocol_Any, false);
	 * 	}
	 * }
	 * }
	 * </pre>
	 * <b>When you close the application, remember to stop the notification, if it is
	 * running, using {@link #stopTagEventNotification()}.</b>
	 * <i>Note: event notification feature is available only on readers with firmware
	 * version 2.3 or later.</i>
	 * @param isoStandard The ISO standard of the transponder you
	 * want to detect.
	 * @param beep The reader beeps when a new tag is detected
	 * @throws IOException If unable to send the command to the reader.
	 */
	public void getNextTagEvent(int isoStandard, boolean beep) throws IOException {
		byte protocol = (byte)isoStandard;
		
		if (beep)
			protocol = (byte)(protocol | 0x40);
		
		byte[] cmd = new byte[3];
		
		cmd[0] = 0x00; //reader command
		cmd[1] = (byte)0xE0; //tag event
		cmd[2] = (byte)isoStandard;
		
		send(cmd);
		
		if (!this.notificationThreadRunning) {
			Thread evCatcher = new Thread(this);
			evCatcher.start();
		}
	}
	
	/**
	 * Subscribes a RFIDTagDetectedListener as listener. Only one listener is allowed.
	 */
	public void addTagEventListener(RFIDTagDetectedListener listener) {
		this.eventListener = listener;
	}
	
	/**
	 * The tag detection background thread. <b>Do not start this thread using this method. Use the
	 * {@link #getNextTagEvent(int, boolean)} method, instead.</b>
	 */
	public void run() {
		this.notificationThreadRunning = true;
		while (notificationThreadRunning) {
			try {
				receive();
				
				if ((recv_buf[2] == 0x00) &&
						((recv_buf[3] & 0xFF) == 0xE0) &&
						(recv_buf[4] == 0x00) ) {
					int protocol ;
					switch (recv_buf[5] & 0xFF) {
						case 0xA0:
							protocol = RF_ISOProtocol_ISO14443A;
							break;
						case 0xB0:
							protocol = RF_ISOProtocol_ISO15693;
							break;
						case 0xC0:
							protocol = RF_ISOProtocol_ISO14443B;
							break;
						case 0xD0:
							protocol = RF_ISOProtocol_EPC;
							break;
						default:
							protocol = RF_ISOProtocol_ISO15693;
							break;
					}
					
					this.notificationThreadRunning = false;
					this.eventListener.TagDetected(protocol);
					return; //esco e termina il thread
				}
			} catch (Exception e) {
			}
		}
		
		this.notificationThreadRunning = false;
		
		// se � stato fermato il thread con uno stopTagEventNotification,
		// devo notificare che questo thread � terminato
		if (!this.available)
			this.Notify();
		
	}
	
}

