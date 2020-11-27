package rfid.labid.IO;

import gnu.io.*;

import java.io.*;
import java.util.Enumeration;

/// <summary>
/// Represents a communication stream through RS232.
/// </summary>
public class SerialStream implements CableStream {
	
	private InputStream in;
	private OutputStream out;
	private CommPortIdentifier portId ;
	private SerialPort sp;
	
	public static final int PARITY_NONE = SerialPort.PARITY_NONE;
	public static final int PARITY_EVEN = SerialPort.PARITY_EVEN;
	public static final int PARITY_ODD  = SerialPort.PARITY_ODD;
	
	public static final int FLOWCONTROL_NONE  = SerialPort.FLOWCONTROL_NONE;
	public static final int FLOWCONTROL_HARDWARE = SerialPort.FLOWCONTROL_RTSCTS_IN;
	public static final int FLOWCONTROL_SOFTWARE = SerialPort.FLOWCONTROL_XONXOFF_IN;
	
	public static final int STOPBITS_1 = SerialPort.STOPBITS_1;
	public static final int STOPBITS_1_5 = SerialPort.STOPBITS_1_5;
	public static final int STOPBITS_2 = SerialPort.STOPBITS_2;
	
	public static final int DATABITS_8 = SerialPort.DATABITS_8;
	public static final int DATABITS_7 = SerialPort.DATABITS_7;
	
	public SerialStream(String port) throws IOException {
		Open(port, 115200);
	}
	
	public void Open(String port, int baudrate) throws IOException {
		try {
			portId = CommPortIdentifier.getPortIdentifier(port.trim());
			sp = (SerialPort)portId.open("LabId", 20000);
			
			out = sp.getOutputStream() ;
			in  = sp.getInputStream() ;
			
			sp.setSerialPortParams(baudrate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			sp.setInputBufferSize(1024);
			sp.enableReceiveTimeout(250);
		}
		catch(Exception e) {
			throw new IOException();
		}
	}
	
	public  void Close() throws IOException {
		in.close();
		out.close();
		sp.close();
	}
	
	public  int Read(byte[] buffer, int offset, int count) throws IOException{
		return in.read(buffer, offset, count);
	}
	
	public  void Write(byte[] buffer, int offset, int count) throws IOException {
		out.write(buffer, offset, count);
		out.flush();
	}
	
	@Override
	public int Read(byte[] buffer) throws IOException {
		return in.read(buffer);
	}
	
	@Override
	public void Write(byte[] buffer) throws IOException {
		out.write(buffer);
		out.flush();
	}
	
	@Override
	public  void Flush() throws IOException {
		out.flush();
	}
	
	public void Purge() {
		
		int oldTimeout = sp.getReceiveTimeout();
		
		SetTimeout(10);
		
		// purge the data stream
		try {
			while (	in.read() > -1);
		} catch (Exception ex) { }
		
		SetTimeout(oldTimeout);
	}
	
	public void SetTimeout(int ReceiveTimeout) {
		try {
			sp.enableReceiveTimeout(ReceiveTimeout);
		}
		catch (Exception e) {}
	}
	
	public void SetPortSettings(int baudrate, int flowcontrol, int parity, int databits, int stopbits) throws Exception {
		sp.setSerialPortParams(baudrate, databits, stopbits, parity);
		sp.setFlowControlMode(flowcontrol);
	}
	
	/// Gets a new instance of SerialStream. You don't need to open it.
	/// <param name="sps">Settings of serial port.</param>
	public static SerialStream getInstance(SerialPortSettings sps) throws IOException{
		SerialStream sp;

		// try to open USB emulated serial port
		try {
			String connectionString = null;
			Enumeration<?> thePorts = CommPortIdentifier.getPortIdentifiers();
			while (thePorts.hasMoreElements() && connectionString == null){
			   CommPortIdentifier com = (CommPortIdentifier) thePorts.nextElement();
			   switch (com.getPortType()){
			      case CommPortIdentifier.PORT_SERIAL:
			      	if(com.getName().contains("USB")){
			      		connectionString = com.getName();
			      		System.out.println(new java.util.Date() + "- RFID Reader found, connected to " + com.getName());
			      	}
			   }
			}
			
			sp = new SerialStream(connectionString);
			return sp;

		} catch (Exception ex) {
			sp = null;
		}

		// try to open hardware serial port
		try {
			sp = new SerialStream("/dev/ttyS" + sps.getPortNumber());
			return sp;
		} catch(Exception e) {
			throw new IOException("Unable to open Serial Port " + sps.getPortNumber());
		}
	}
}
