package controller;

import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import exceptions.PEException;
import model.Terminals;

/**
 * Server astratto, classe base per le operazioni comuni a Server e TestServer.
 */
public abstract class AbstrServer extends Thread {
	protected TerminalController controller;
	protected ExecutorService pool;
	protected ServiceFactory factory;
	
	protected volatile boolean running;
	
	/**
	 * Costruttore del server astratto (classi derivate: Server e TestServer)
	 * @param numConnections	La dimensione del pool di thread del server (e quindi il numero massimo di connessioni simultanee).
	 * @param factory			La factory che produce i service (necessaria a realizzare il paradigma Factory) che variano a seconda del tipo di terminale.
	 * @param name				Il nome dato al tipo di terminale (utile in fase di testing per tenere d'occhio i thread).
	 */
	public AbstrServer(int numConnections, ServiceFactory factory, Terminals.Type type) {
		super(type.toString());
		this.pool = Executors.newFixedThreadPool(numConnections);
		this.factory = factory;
		this.running = true;
	}
	
	/**
	 * Setta il controller di questo server.
	 * Chiamata dal costruttore classe AbstrServer.
	 * Purtroppo così si hanno dipendenze cicliche.
	 * @param controller
	 */
	public void setController(TerminalController controller) {
		this.controller = controller;
	}
	
	/**
	 * Restituisce l'eventuale errore di avviamento del server.
	 * Chiamato dal controller.
	 * @return	L'errore di avviamento del server.
	 */
	/*public PEException getStartUpError() {
		return startUpException;
	}*/
	
	/**
	 * Indica se il thread server ha terminato.
	 * @return	Vero o falso a seconda se il server ha teminato
	 */
	public boolean isRunning() {
		return running;
	}
	
	/**
	 * Funzione principale del server.
	 * Cicla finchè running è posto a true e richiama la exec la quale varia a seconda se il server è reale o di test.
	 */
	@Override
	public void run() {
		while(running) {
			exec();
		}
	}
	
	/**
	 * Funzione che restituisce un collegamento con un terminale destinatario relativo all'ip inserito.
	 * Per la classe Server il Link non sarà altro che un wrapper della classe Socket.
	 * Per la classe TestServer il Link simulerà tale comportamento tramite una sorta di pipe.
	 * @param ip		L'indirizzo ip del terminale destinatario.
	 * @param port		La porta del terminale destinatario.
	 * @param timeout	Un timeout oltre il quale la connessione verrà terminata in caso di assenza di risposta.
	 * @param error		L'errore che deve essere mostrato all'utente se la connessione non dovesse riuscire.
	 * @return
	 * @throws PEException 
	 */
	protected abstract Link createLink(InetAddress ip, int port, Integer timeout, String error) throws PEException;
	
	/**
	 * Funzione generica che varia a seconda se si tratta di un Server o di un TestServer.
	 */
	protected abstract void exec();
	
	/**
	 * Funzione che termina il thread server.
	 */
	protected void shutDown() {
		running = false;
		pool.shutdown(); // Disable new tasks from being submitted
		
		try {
			if (!pool.awaitTermination(600, TimeUnit.MILLISECONDS)) {
				pool.shutdownNow();
				// Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!pool.awaitTermination(600, TimeUnit.MILLISECONDS))
					System.err.println("Pool did not terminate");
		    	}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			pool.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
	  }
		
		shutDownOps();
	}
	
	/**
	 * Funzione per le operazioni necessarie durante la terminazione del thread server,
	 * ma specifiche per il tipo di server (Server o TestServer).
	 */
	protected abstract void shutDownOps();
}
