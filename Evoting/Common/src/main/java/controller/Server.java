package controller;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import javax.net.ssl.SSLServerSocketFactory;

import exceptions.FLRException;
import exceptions.PEException;
import model.Terminals;
import utils.CfgManager;
import utils.Constants;

/**
 * Classe Server reale, gestisce connessioni sicure o non sicure a seconda del parametro enableSSL.
 * Inoltre permette di creare connessioni in uscita wrappando le socket nella classe Link.
 */
public class Server extends AbstrServer {
	//Porta sulla quale il server si mette in ascolto.
	private ServerSocket welcomeSocket;
	
	/**
	 * Costruttore
	 * @param factory			La factory che produrrà i Service che gestiscono le connessioni in ingresso.
	 * @param port				La porta sulla quale il server si metterà in ascolto.
	 * @param numConnections	Dimensione del thread pool che eseguiranno i Service.
	 * @param name				Nome che verrà dato al thread server (debug).
	 * @throws PEException 
	 */
	public Server(ServiceFactory factory, int port, int numConnections, Terminals.Type type) throws PEException {
		super(numConnections, factory, type);
		
		//Se usiamo SSL dobbiamo indicare posizione e password di truststore e keystore,
		//A meno che non si usino quelli di default della macchina (sconsigliato).
		if(Constants.linkSSL) {
			System.setProperty("javax.net.ssl.keyStore", "ssl/keystore.jks");
			System.setProperty("javax.net.ssl.keyStorePassword", CfgManager.getPassword("ks"));
			
			System.setProperty("javax.net.ssl.trustStore", "ssl/truststore.jks");
			System.setProperty("javax.net.ssl.trustStorePassword", CfgManager.getPassword("ts"));
		}
		
		try {
			//Se usiamo SSL vogliamo una ServerSocket SSL
			if(Constants.linkSSL) {
				//Per creare un server che richieda connessioni sicure (SSL)
				this.welcomeSocket = SSLServerSocketFactory.getDefault().createServerSocket(port);
			}
			else {
				//Per creare un server che NON richieda connessioni sicure
				this.welcomeSocket = new ServerSocket(port);
			}
		}
		catch(BindException e) {
			throw FLRException.FLR_13(port, e);
		}
		catch(SocketException e) {
			throw FLRException.FLR_13(true, e);
		}
		catch(IOException e) {
			throw FLRException.FLR_13(false, e);
		}
	}
	
	/**
	 * Crea un link che è il wrapper di una socket.
	 */
	@Override
	protected Link createLink(InetAddress ip, int port, Integer timeout, String error) throws PEException {
		return new Link(ip, port, timeout, error);
	}

	/**
	 * Funzione richiamata in risposta a una connessione in ingresso.
	 * Viene richiamata la factory per produrre un Service che verrà eseguito-
	 */
	@Override
	protected void exec() {
		try {
			//pool.execute(factory.createService(controller, welcomeSocket.accept()));
			pool.execute(factory.createService(controller, new Link(welcomeSocket.accept()), "Server " + controller.getClass().toString()));
		} catch (IOException e) {
			//se running è false abbiamo semplicemente chiuso il programma
			//altrimenti c'è stato un problema imprevisto.
			if(isRunning()) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Operazioni che ogni Server deve eseguire dopo essere stato spento.
	 * In pratica fermare i thread della view.
	 */
	@Override
	protected void shutDownOps() {
		try {
			if(welcomeSocket != null) {
				welcomeSocket.close();
			}
		} catch (IOException e) {
			// TODO Gestire il messaggio di errore.
			e.printStackTrace();
		}
	}

}
