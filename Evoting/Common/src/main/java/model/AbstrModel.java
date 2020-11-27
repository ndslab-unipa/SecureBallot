package model;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import exceptions.FLRException;
import exceptions.PEException;
import utils.Logger;

public class AbstrModel {
	protected InetAddress urnIp;
	protected int urnPort;
	
	protected Logger logger;

	protected String username, password;
	protected boolean rfidReaderReachable = true;
	
	public AbstrModel() {
		logger = new Logger(false);
	}
	
	public ArrayList<String> getLogs() { 
		return logger.getLogs(); 
	}

	public void logInfo(String event) { 
		logger.logInfo(event);
	}
	
	public void logSuccess(String event) { 
		logger.logSuccess(event); 
	}
	
	public void logWarning(String event) { 
		logger.logWarning(event); 
	}
	
	public void logError(String event) { 
		logger.logError(event); 
	}

	/**
	 * Getter per l'username dell'utente loggato.
	 * @return Username dell'utente loggato
	 */
	public String getUsername() { 
		return username; 
	}
	
	/**
	 * Setter per l'username dell'utente loggato.
	 * @param user Username dell'utente loggato
	 */
	public void setUsername(String user) { 
		username = user;
	}
	
	/**
	 * Getter per la password dell'utente loggato.
	 * @return Password dell'utente loggato
	 */
	public String getPassword() { 
		return password;
	}
	
	/**
	 * Setter per la password dell'utente loggato.
	 * @param psw Password dell'utente loggato
	 */
	public void setPassword(String psw) { 
		password = psw; 
	}

	/**
	 * @return the urnIp
	 */
	public InetAddress getUrnIp() {
		return urnIp;
	}

	/**
	 * @param urnIp the urnIp to set
	 */
	public void setUrnIp(InetAddress urnIp) {
		this.urnIp = urnIp;
	}
	
	public void setUrnIp(String ipString) throws PEException {
		try {
			urnIp = InetAddress.getByName(ipString);
		} catch (UnknownHostException e) {
			throw FLRException.FLR_17(ipString);
		}
	}

	/**
	 * @return the urnPort
	 */
	public int getUrnPort() {
		return urnPort;
	}

	/**
	 * @param urnPort the urnPort to set
	 */
	public void setUrnPort(int urnPort) {
		this.urnPort = urnPort;
	}
	
	public boolean isRfidReaderReachable() {
		return rfidReaderReachable;
	}
	
	public void setRfidReaderReachable(boolean reachable) {
		rfidReaderReachable = reachable;
	}
}
