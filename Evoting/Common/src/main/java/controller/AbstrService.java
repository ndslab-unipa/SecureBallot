package controller;

import java.net.InetAddress;

/**
 * Service astratto, classe base da cui derivano tutti i differenti tipi di Service (uno per tipologia di terminale).
 */
public abstract class AbstrService extends Thread{
	protected TerminalController controller;
	protected Link link;
	protected InetAddress ip;

	public AbstrService(TerminalController controller, Link link, String name){
		super(name);
		this.controller = controller;
		this.link = link;
		this.ip = link.getIp();
	}

	@Override
	public void run() {
		if(link.hasNextLine()){
			execute();
		}

		link.close();
	}
	
	/**
	 * Funzione in cui il service gestisce la richiesta da parte del mittente.
	 */
	protected abstract void execute();
}
