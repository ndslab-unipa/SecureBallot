package controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

import javax.net.ssl.SSLSocketFactory;

import exceptions.PEException;
import exceptions.CNNException;
import utils.Constants;

public class Link implements AutoCloseable {
	private Socket socket;
	private InetAddress ip;
	private Scanner in;
	private PrintWriter out;

	private boolean closed;

	//Costruttore per il server reale
	public Link(Socket socket){
		closed = false;

		this.socket = socket;

		try {
			setParametersFromSocket();
		} catch (IOException e) {
			e.printStackTrace();
			close();
		}
	}

	//Costruttore per il client reale
	public Link(InetAddress ip, int port, Integer timeout, String error) throws PEException {
		closed = false;
		
		try {
			socket = createSocket(ip, port, timeout, Constants.linkSSL);
			setParametersFromSocket();
		}
		catch(IOException e) {
			close();
			
			if(error != null)
				throw CNNException.CNN_1( "terminale", ip.getHostAddress(), e);
		}
	}

	private void setParametersFromSocket() throws IOException {
		ip = socket.getInetAddress();
		out = new PrintWriter(socket.getOutputStream(), true);
		in = new Scanner(socket.getInputStream());
	}

	//Costruttore per il testing
	public Link(InetAddress ip, Scanner in, PrintWriter out) {
		socket = null;
		this.ip = ip;
		if(in != null && out != null) {
			this.in = in;
			this.out = out;
			closed = false;
		}
		else {
			closed = true;
		}
	}

	public InetAddress getIp(){
		return ip;
	}

	public void write(Object message) {
		out.println(message);
	}

	public boolean hasNextLine() {
		return in.hasNextLine();
	}
	
	public String read() {
		return in.nextLine();
	}
	
	public String waitNRead() {
		String message = null;
		
		if(in != null && in.hasNextLine()) {
			message = in.nextLine();
		}

		return message;
	}
	
	public boolean isClosed() {
		return closed;
	}
	
	/**
	 * Funzione che termina la connessione con l'altra parte.
	 * Chiude i canali di comunicazione e nel caso reale anche la Socket adoperata.
	 */
	//public void endConnection(){
	@Override
	public void close() {
		closed = true;
		
		if(socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if(out != null) {
			out.close();
		}
		
		if(in != null) {
			in.close();
		}
		
	}
	
	/**
	 * Funzione che restituisce una socket dato ip e porta,
	 * ed eventualmente un timeout (se messo a null verrà usato un valore di default).
	 * Usata principalmente per passare in fretta da connessioni sicure ad non sicure (per motivi di test o altro).
	 * 
	 * @param ip			L'indirizzo ip a cui connettersi.
	 * @param port			La porta del destinatario con cui comunicare.
	 * @param timeout		Eventuale timeout.
	 * @return
	 * @throws IOException 
	 */
	static Socket createSocket(InetAddress ip, int port, Integer timeout, boolean enableSSL) throws IOException {
		
		//try {
		Socket socket;
		
		if(enableSSL) {
			socket = SSLSocketFactory.getDefault().createSocket();//<--Per effettuare connessioni sicure come client.
		}
		else {
			socket = new Socket();//<--Per effettuare connessioni NON sicure come client.
		}
		
		if(timeout == null) {
			//TODO:5 secondi mi sembra un tempo sufficiente per stabilire una connessione.
			timeout = 5000;
		}
		
		socket.connect(new InetSocketAddress(ip, port), timeout);
		
		return socket;
		/*}
		catch (IOException e) {
			e.printStackTrace();
			throw new PEException("Impossibile connettersi a " + ip + ":" + port);
		}*/
		
	}
}
