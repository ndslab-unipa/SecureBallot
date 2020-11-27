package rfid.labid;

import rfid.labid.IO.*;
import java.io.*;

/**  is an abstract class which represents a RF reader
 * connected through serial or USB port.
 */
public abstract class LocalRFReader {
	/**  Size of serial number of an iso15693 transponder.
	 * You should never change it.
	 */
	protected int uidSize = 8;
	
	/**
	 * Number of bytes in each block of an iso15693 transponder memory.
	 */
	protected int blockSize = 4;
	
	/**
	 * Gets how many bytes are supposed to be in each data block of transponders.
	 */
	public int getBlockSize() {
		return this.blockSize;
	}
	
	/**
	 * Sets how many bytes are supposed to be in each data block of transponders.
	 */
	public void setBlockSize(int bytes) {
		this.blockSize = bytes;
	}
	
	/**  Sets serial communication timeout
	 */
	public int getTimeout() {
		return this.timeoutMillis;
	}
	
	public void	setTimeout(int value) {
		this.timeoutMillis = value;
	}
	
	/**  Sets the number of attempts for every serial communication
	 */
	public void setRetry(int value) {
		nTentativi = value;
	}
	
	public int getRetry() {
		return this.nTentativi;
	}
	
	/**  indica se � possibile eseguire una nuova operazione, segnala cio� la disponibilit�
	 * della porta seriale
	 */
	protected  boolean available = true;
	
	/** The communication stream
	 */
	protected  CableStream sp;
	
	/**  il numero di tentativi di comunicazione con la porta seriale che vengono
	 * effettuati prima di considerare l'operazione fallita
	 */
	protected  int nTentativi = 5;
	/**  il tempo (in ms) che deve trascorrere prima di considerare fallita una
	 * comunicazione con la porta seriale
	 */
	protected  int timeoutMillis = 1000;
	
	/**  Opens a serial connection with the RFID reader with its
	 * default settings.
	 */
	
	public abstract void openSerialPort(String portName) throws RFReaderException;
	
	/**
	 * Closes the current serial connection.
	 */
	public  void closeSerialPort() throws IOException {
		sp.Close();
	}
	
	/**
	 * Writes a sequence of bytes to the opened serial port and returns
	 * the reader response. Use it with care and only if you know the
	 * exact communication protocol of the connected device.
	 *
	 * @param cmd Bytes to send to serial port.
	 * @return Bytes read from serial port.
	 *
	 */
	public byte[] writeToSerialPort(byte[] cmd) throws RFReaderException {
		while (available == false) {
			try {
				wait();
			}
			catch (Exception e) {
			}
		}
		available = false;
		
		int tentativi;
		int nLetti = 0;
		int daLeggere = 0;
		int rimasti;
		byte[] letti = new byte[256];
		byte[] reply;
		
		try {
			sp.Write(cmd,0,cmd.length);
			
			//sp.enableReceiveThreshold(1); // attendo almeno 1 byte
			nLetti = 0;
			tentativi = nTentativi;
			while ((nLetti == 0) && (tentativi-- > 0)) {
				nLetti = sp.Read(letti, 0, letti.length);
			}
			
			if (tentativi < 0)
				throw new RFReaderException("No answer from reader");
			else {
				
				daLeggere = letti[0] & 0xFF - nLetti;
				rimasti = daLeggere;
				
				while ((rimasti > 0) && (tentativi-- > 0)) {
					//sp.enableReceiveThreshold(rimasti);
					nLetti += sp.Read(letti, nLetti, rimasti);
					rimasti = daLeggere - nLetti;
				}
				
				if (tentativi < 0)
					throw new RFReaderException("No answer from reader");
				else {
					reply = letti;
				}
			}
		}
		catch (Exception e) {
			throw new RFReaderException("No answer from reader: " + e.getMessage());
		}
		
		available = true;
		notifyAll();
		return reply;
	}
	
	/**
	 * Opens a serial port with the specified parameters. Be sure to indicate
	 * right settings for your current device.
	 *
	 * @param portName Name of the serial port (ex. "COM1")
	 * @param BaudRate Communication baudrate.
	 * @param flowControl Communication flow control.
	 * @param parity Communication parity.
	 *
	 */
	public void openSerialPort(String portName, int BaudRate, int flowControl, int parity, int stopBits) throws RFReaderException {
		try {
			sp = new SerialStream(portName);
			((SerialStream)sp).SetPortSettings( BaudRate, flowControl, parity, 8, stopBits);
			
			sp.SetTimeout(timeoutMillis);
		}
		catch (Exception e) {
			throw new RFReaderException("Unable to open " + portName);
		}
	}
	
	/**
	 * Opens a serial port with the specified settings object.
	 *
	 * @param sps The object which represents serial port settings.
	 *
	 */
	public void openSerialPort(SerialPortSettings sps) throws RFReaderException{
		try {
			sp = SerialStream.getInstance(sps);
		}
		catch (Exception e) {
			throw new RFReaderException("Unable to open COM" + sps.getPortNumber());
		}
	}
	
	/**
	 * Sets an already open communication stream as the current stream of the reader.
	 */
	public void open(CableStream stream) throws RFReaderException {
		sp = stream;
	}
	
	/**
	 * Closes the current communication stream.
	 */
	public void close() throws IOException {
		sp.Close();
	}
}
