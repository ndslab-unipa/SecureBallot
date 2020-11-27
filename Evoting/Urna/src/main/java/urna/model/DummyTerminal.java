package urna.model;

import java.net.InetAddress;

import model.Terminals;
import utils.Constants;

public class DummyTerminal {
	private InetAddress ip;
	private int port;
	private Terminals.Type type;
	
	public DummyTerminal(InetAddress ip, Terminals.Type type) {
		this.ip = ip;
		this.type = type;
		
		switch(type) {
			case Post:
				port = Constants.portPost;
				break;
				
			case Station:
				port = Constants.portStation;
				break;
				
			case SubStation:
				port = Constants.portSubStation;
				break;
				
			default:
				port = -1;
		}
	}
	
	public InetAddress getIp() { return ip; }
	public void setIp(InetAddress ip) { this.ip = ip; }
	
	public int getPort() { return port; }
	public void setPort(int port) { this.port = port; }
	
	public Terminals.Type getType() { return type; }
	public void setType(Terminals.Type type) { this.type = type; }
}
