package controller;

import java.net.InetAddress;

import model.Terminals;

public class UrnPolling extends Thread {
	private final int pollingRate = 5000;
	private final InetAddress urnIp;
	private TerminalController controller;
	private Terminals.Type terminalType;
	private volatile boolean running = true;
	
	public UrnPolling(InetAddress urnIp, TerminalController controller, Terminals.Type type) {
		this.urnIp = urnIp;
		this.controller = controller;
		this.terminalType = type;
	}
	
	@Override
	public void run() {
		while(running) {
			try {
                Thread.sleep(pollingRate);
            } catch (InterruptedException e) {
                return;
            }
			
			if(!controller.checkAuthentication(urnIp, terminalType)) {
				controller.invalidAuthentication();
			}
		}
	}
	
	public void shutDown(){
        try {
        	running = false;
        	this.interrupt();
        }
        catch (Exception ignored) { };
    }
}
