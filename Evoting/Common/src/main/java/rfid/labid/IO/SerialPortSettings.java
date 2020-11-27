package rfid.labid.IO;

/**
 * This class contains all settings for opening a serial port. <u>All parameters are set using
 *constants defined as static in {@link labid.IO.SerialStream} class</u>.
 */
public class SerialPortSettings {
	private int portName = 1;
	private int parity = SerialStream.PARITY_NONE;
	private int stopBits = SerialStream.STOPBITS_1;
	private int baudRate = 115200;
	private int flowControl = SerialStream.FLOWCONTROL_NONE;
	private int timeout = 100;
	
	/**
	 * Indicates the COM port number.
	 */
	public int getPortNumber() {
		return this.portName;
	}
	public void	setPortNumber(int value) {
		this.portName = value;
	}
	
	/**
	 * Parity of serial frames.
	 */
	public int getParity() {
		return this.parity;
	}
	
	public void setParity(int value) {
		this.parity = value;
	}
	
	/**
	 * Getter for property baudRate.
	 * @return Value of property baudRate.
	 */
	public int getBaudRate() {
		return baudRate;
	}
	
	/**
	 * Setter for property baudRate.
	 * @param baudRate New value of property baudRate.
	 */
	public void setBaudRate(int baudRate) {
		this.baudRate = baudRate;
	}
	
	/**
	 * Getter for property flowControl.
	 * @return Value of property flowControl.
	 */
	public int getFlowControl() {
		return flowControl;
	}
	
	/**
	 * Setter for property flowControl.
	 * @param flowControl New value of property flowControl.
	 */
	public void setFlowControl(int flowControl) {
		this.flowControl = flowControl;
	}
	
	/**
	 * Getter for property timeout.
	 * @return Value of property timeout.
	 */
	public int getTimeout() {
		return timeout;
	}
	
	/**
	 * Setter for property timeout.
	 * @param timeout New value of property timeout.
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	/**
	 * This constructor does not initialize any value.
	 */
	public SerialPortSettings() {
		//
		// TODO: aggiungere qui la logica del costruttore
		//
	}
	
	public int getStopBits() {
		return this.stopBits;
	}
	
	public void setStopBits(int stopBits) {
		this.stopBits = stopBits;
	}
}