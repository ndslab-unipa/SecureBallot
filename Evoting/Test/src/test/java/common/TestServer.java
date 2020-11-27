package common;

import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.Scanner;

import controller.AbstrServer;
import controller.Link;
import controller.ServiceFactory;
import model.Terminals;

public class TestServer extends AbstrServer {

	private Object lockTestServerSynch = new Object();
	
	private volatile Scanner inputTest = null;
	private volatile PrintWriter outputTest = null;
	private volatile InetAddress ipOtherTest = null;
	
	private final InetAddress ipThisTest;
	
	public TestServer(int numConnections, ServiceFactory factory, Terminals.Type type, InetAddress ipThisTest) {
		super(numConnections, factory, type);
		
		this.ipThisTest = ipThisTest;
		
		Internet.getInstance().addServer(ipThisTest, this);
	}

	@Override
	protected Link createLink(InetAddress ip, int port, Integer timeout, String error) {
		return Internet.getInstance().connectTo(ipThisTest, ip);
	}

	@Override
	protected void exec() {
		synchronized(lockTestServerSynch) {
			
			try {
				
				while((inputTest == null || outputTest == null || ipOtherTest == null) && isRunning()) {
					lockTestServerSynch.wait();
				}
				
				if(isRunning()) {
					//pool.execute(factory.createService(controller, inputTest, outputTest, ipOtherTest));
					pool.execute(factory.createService(controller, new Link(ipOtherTest, inputTest, outputTest), "Test Server " + controller.getClass().toString()));
					inputTest = null;
					outputTest = null;
					ipOtherTest = null;
					
					lockTestServerSynch.notify();
				}
				
			}
			catch (InterruptedException e) {
				// TODO Gestire il messaggio di errore.
				e.printStackTrace();
				assertTrue(false);
			}
			
		}
	}

	@Override
	protected void shutDownOps() {
		synchronized(lockTestServerSynch) {
			lockTestServerSynch.notifyAll();
		}
	}
		
	
	/**
	 * Funzione che serve a simulare la richiesta di connessione dall'esterno durante i test e dunque SENZA far ricorso a Socket.
	 * I parametri passati fungono da canali di comunicazione tra "Service" e altra parte, e da ip fittizio per l'altra parte.
	 * @param in Stream di input per ricevere i dati dall'altra parte.
	 * @param out Stream di output per inviare dati all'altra parte.
	 * @param ipTest Ip fittizio dell'altra parte.
	 */
	public boolean testConnectToServer(Scanner in, PrintWriter out, InetAddress ipTest) {

		if(!running){
			return false;
		}

		synchronized(lockTestServerSynch) {
			try {
				while(this.inputTest != null || this.outputTest != null || this.ipOtherTest != null) {
					lockTestServerSynch.wait();
				}
				
				this.inputTest = in;
				this.outputTest = out;
				this.ipOtherTest = ipTest;
				
				lockTestServerSynch.notifyAll();
			}
			catch (InterruptedException e) {
				// TODO Gestire il messaggio di errore.
				e.printStackTrace();
				assertTrue(false);
				return false;
			}
		}

		return true;
	}
}
