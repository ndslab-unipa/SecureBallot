package rfid;

import java.io.IOException;

import controller.TerminalController;
import rfid.labid.ByteUtils;
import rfid.labid.IO.SerialPortSettings;
import rfid.labid.IO.SerialStream;
import rfid.labid.iso15693.ISO15693Reader;
import sun.misc.Signal;
import utils.Constants;

public class RfidCardReader extends Thread {
	private TerminalController controller;
	
	private volatile int signal_exit = 0;
	private volatile boolean reading = false;
	private volatile boolean alwaysOn = false;
	
	private static int EXITCODE_SIGTERM           = 1;
	//private static int EXITCODE_READ_OK           = 2;
	//private static int EXITCODE_READ_ERROR        = 3;
	//private static int EXITCODE_READ_EXCEPTION    = 4;
	//private static int EXITCODE_ERR_CLOSE_STREAM  = 5;
	//private static int EXITCODE_CONNECTED_READER  = 6;
	
	public RfidCardReader(TerminalController controller) {
		this.controller = controller;
	}
	
	public synchronized void allowReading() {
		reading = true;
		notify();
	}
	
	public synchronized void setAlwaysOn() {
		alwaysOn = true;
		notify();
	}
	
	@Override
	public void run() {
		System.setProperty("gnu.io.rxtx.SerialPorts", "/dev/ttyUSB0:/dev/ttyUSB1:/dev/ttyUSB2");
		
		SerialStream stream = null;
		Signal.handle(new Signal("TERM"), signal -> {
			System.out.println("RFID_READ: " + EXITCODE_SIGTERM);
			signal_exit = 1;
        });
		
		while(signal_exit == 0) {
			try {
				synchronized(this) {
					while(!reading && !alwaysOn) {
						wait();
					}
				}
				
				stream = getSerialStream();
				ISO15693Reader reader = new ISO15693Reader(stream);
				controller.setRfidReachable(true);
				
				byte[][] uid;
				
				do {
					sleep(500);
					uid = reader.inventory();
				} while (uid == null && signal_exit == 0);
				
				if(signal_exit == 1) {
					// Prima di terminare, chiudo eventuale stream aperto
					if(stream != null) {
						try {
							stream.Close();
						} catch(Exception ignored) { }
					}
					
					break;
				}
				
				reading = false;
				
				if(uid.length > 0) {
					String card = ByteUtils.toHexString(ByteUtils.revertedCopy(uid[0]));
					
					if(Constants.verbose)
						System.out.println("Read: " + card);
					
					controller.readCard(card.substring(Math.max(0, card.length() - 6)));
				}
			}
			catch (Exception e) {
				System.err.println(new java.util.Date() + " - RFID Reader not found, trying again in 2 seconds");
				controller.setRfidReachable(false);
				
				try {
					Thread.sleep(2000);
				} catch (InterruptedException ignored) { }
			} 
			finally {
				try {
					stream.Close();
				} catch(Exception ignored) { }
			}
		}
	}
	
	public synchronized void shutDown() {
		reading = true;
		signal_exit = 1;
		notify();
	}
	
	private SerialStream getSerialStream() throws IOException {
		SerialStream stream;
		
		SerialPortSettings sps = new SerialPortSettings();
		sps.setBaudRate(115200);
		sps.setFlowControl(SerialStream.FLOWCONTROL_NONE);
		sps.setParity(SerialStream.PARITY_NONE);
		sps.setPortNumber(1); // <------ PUT HERE THE COM PORT number
		sps.setStopBits(SerialStream.STOPBITS_1);
		
		stream = SerialStream.getInstance(sps);
		
		return stream;
	}
}
