package controller;

import java.net.InetAddress;

import exceptions.CNNException;
import exceptions.PEException;
import model.Message;
import model.Terminals;
import rfid.RfidCardReader;
import utils.Constants;
import utils.Protocol;
import view.ViewInterface;

/**
 * Controller da cui derivano i controller dei terminali (Postazione, Seggio, Seggio Ausiliario, Urna).
 * Oltre alla view possiede un server per le comunicazioni con gli altri terminali e un card reader 
 * (qust'ultimo in effetti è inutile per l'urna che lo ignora).
 */
public abstract class TerminalController extends AbstrController {
	//Server adoperato (Server reale o TestServer per lo unitTesting)
	protected AbstrServer server;
	
	protected UrnPolling urnThread;
	
	//Lettore RFID
	protected CardReader cardReader;
	protected RfidCardReader rfidReader;
	
	private final String shutdownMsg;
	
	/**
	 * Costruttore
	 * @param server 	Il server che si vuole adoperare (reale o di test).
	 * @param view		Stream di output che permette la comunicazione con la view.
	 */
	public TerminalController(AbstrServer server, ViewInterface view, Terminals.Type type, boolean physicalReader) {
		super(view);
		this.server = server;
		this.server.setController(this);
		
		switch(type) {
			case Station:
				this.shutdownMsg = Protocol.stationShutDown;
				break;
				
			case SubStation:
				this.shutdownMsg = Protocol.subStationShutDown;
				break;
				
			case Post:
				this.shutdownMsg = Protocol.postShutDown;
				break;
				
			case Urn:
				this.shutdownMsg = Protocol.urnShutDown;
				return;
				
			default:
				this.shutdownMsg = null;
 		}
		
		if(!physicalReader) {
			cardReader = new CardReader(this);
			cardReader.start();
		}
		else {
			rfidReader = new RfidCardReader(this);
			rfidReader.start();
		}
	}
	
	public boolean checkAuthentication(InetAddress urnIp, Terminals.Type type) {
		Message request = new Message(Protocol.checkTerminalAuthentication);
		request.setElement("terminal", type);
		
		Message response = null;
		try (Link link = createLink(urnIp, Constants.portUrn, 250, null)) {			
			if(link.isClosed())
				throw CNNException.CNN_3("urna", urnIp.getHostAddress());
			
			link.write(Protocol.checkTerminalAuthentication);
			link.write(request.toB64());
			
			if(!link.hasNextLine())
				throw CNNException.CNN_3("urna", urnIp.getHostAddress());

			response = (Message) Message.fromB64(link.read(), "urna");
		}
		catch(Exception e) {
			System.err.println("Impossibile comunicare con l'urna");
		}
		
		//Non dovrebbe succedere mai, ma se l'urna non ritorna risposta e non viene lanciata eccezione,
		// assumiamo temporaneamente che il terminale sia ancora autenticato
		if(response == null)
			return true;
		
		return response.getValue().equals(Protocol.authenticatedAck);
	}
	
	/**
	 * Restituisce il card reader del controller.
	 * @return il card reader del controller.
	 */
	public CardReader getCardReader() {return cardReader;}
	
	/* + + + + + + + + + + + + + + 
	 * Funzioni relative al server
	 * + + + + + + + + + + + + + + */
	
	/**
	 * Funzionze che collega al terminale individuato da ip e porta.
	 * Nel caso il server sia quello reale la classe Link fa da wrapper per una socket.
	 * Nel caso il server sia quello di test la classe Link fa da pipe tra i 2 thread (server e client).
	 * 
	 * @param ip		Indirizzo ip del terminale destinatario.
	 * @param port		Porta del terminale destinatario
	 * @param timeout	Timeout oltre il quale la connessione deve essere terminata in mancanza di risposta.
	 * @return			Un oggetto Link, che fa da wrapper per la socket.
	 */
	protected Link createLink(InetAddress ip, int port, Integer timeout, String error) throws PEException {
		return server.createLink(ip, port, timeout, error);
	}
	
	/**
	 * Funzione che avvia server e card reader del controller.
	 */
	public void start() {
		server.start();
		//cardReader.start();
		
		//Se qualcosa va storto durante l'avvio il controller termina direttamente.
		if(!beforeStartOps()) {
			shutDown();
		}
	}
	
	/**
	 * Funzione che mette in attesa fino a che server e card reader non abbiano terminato.
	 * Da chiamare solo dopo la shutdown.
	 */
	/*
	public void join() {
		try {
			server.join();
			cardReader.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	*/
	
	protected void stopUrnThread() {
		if(urnThread != null) {
			urnThread.shutDown();
			try {
				urnThread.join();
			} catch(InterruptedException ignored) { }
		}
	}
	
	/**
	 * Funzione che termina view (se presente), server e card reader.
	 */
	@Override
	public void shutDown() {
		closeView();
		stopUrnThread();
		
		//System.out.println("UrnThreadStopped");
		
		//Funzioni da eseguire al termine del programma, ma prima di spegnere il server.
		//Variano da terminale a terminale.
		afterClosureOps();
		
		//cardReader.shutDown();
		//server.shutDown();
		stopThreads();
	}
	
	protected void stopThreads() {
		if(cardReader != null) {
			cardReader.shutDown();
			try {
				cardReader.join();
			} catch(InterruptedException ignored) { }
		}
		
		//System.out.println("CardReaderStopped");
		
		if(rfidReader != null) {
			rfidReader.shutDown();
			try {
				rfidReader.join();
			} catch(InterruptedException ignored) { }
		}
		
		//System.out.println("RfidCardReaderStopped");
		
		if(server != null) {
			server.shutDown();
			try {
				server.join();
			} catch(InterruptedException ignored) { }
		}
		
		//System.out.println("ServerStopped");
	}
	
	protected void signalShutDown(InetAddress ip, int port) {
		if (shutdownMsg != null)
			try (Link link = createLink(ip, port, 100, null)) {
				if(link.isClosed()) 
					return;
				
				link.write(shutdownMsg);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
	
	protected abstract void invalidAuthentication();
	
	/**
	 * Funzione richiamata dai service per verificare se l'IP del terminale che ha aperto la connessione corrisponda a quello noto dell'urna.
	 * @param ip Ip da verificare
	 * @return True se la verifica passa, False altrimenti.
	 */
	protected abstract boolean verifyUrnIp(InetAddress ip);
	
	/**
	 * Funzione richiamata dal card reader quando viene letta una card
	 * in modo che il controller gestisca questo ulteriore input.
	 * @param card	La stringa inviata dal card reader.
	 * @return		Una stringa relativa al risultato dell'operazione in risposta alla lettura della card.
	 */
	public abstract String readCard(String card);
	
	/**
	 * Qui vanno inserite le operazioni che il terminale specifico dovrebbe compiere all'avvio.
	 * @return true o false a seconda se l'avvio ha avuto esito positivo.
	 */
	protected abstract boolean beforeStartOps();
	
	/**
	 * Qui vanno inserite le operazioni da eseguire al termine dell'applicazione.
	 */
	protected abstract void afterClosureOps();
	
	public abstract void setRfidReachable(boolean reachable);
	
}
