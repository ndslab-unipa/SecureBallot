package controller;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Classe che simula il comportamento dei lettori RFID, ricevendo una Stringa tramite Stream.
 */

public class CardReader extends Thread {
	private TerminalController controller;
	private volatile boolean running = true;
	private volatile boolean writing = false;
	private volatile boolean reading = false;
	
	private String lastOp = "noOp";
	
	private Scanner in;
	private PrintWriter out;
	
	public static String exit = "RFIDServerStop";
	
	/**
	 * Crea il card reader e gli assegna il controller con il quale comunicare.
	 * @param controller il controller a cui appartiene il Card Reader.
	 */
	public CardReader(TerminalController controller) {
		super("Card Reader");
		this.controller = controller;
		
		PipedOutputStream outStrm;
        PipedInputStream inStrm;
		try {
			outStrm = new PipedOutputStream();
			inStrm = new PipedInputStream(outStrm);
			
	        out = new PrintWriter(outStrm, true);
	        in = new Scanner(inStrm);
		} catch (IOException e) {
			e.printStackTrace();
		}
        
	}
	
	public synchronized String write(String card) {
		
			if(writing) {
				//notifyAll();
				return "occupied";
			}
		
			writing = true;
			
			/*try {
				while(writing) {
					wait();
				}
			} catch (InterruptedException e) {e.printStackTrace();}*/
			
			out.println(card);
			
			reading = true;
			notifyAll();
			
			try {
				while(reading) {
					wait();
				}
			} catch (InterruptedException e) {e.printStackTrace();}
			
			String op = lastOp;
			lastOp = "noOp";
			
			//writing = false;
			
			//notifyAll();
			
			return op;
	}
	
	public synchronized void endWrite() {
		writing = false;
	}
	
	@Override
	public void run() {
		while(running) {
			
			try {
				
				synchronized(this) {
					
					while(!reading) {
						wait();
					}
					
					String card = in.nextLine();
				    if(!card.equals(exit)){
				    	lastOp = controller.readCard(card);
				    }
					
					reading = false;
					
					notifyAll();
					
				}
				
			}
			catch(Exception e) {
				e.printStackTrace();
			}			
		}
	}
	
	public void shutDown() {
		
		synchronized(this) {
			running = false;
			reading = true;
			out.println(exit);
			notifyAll();
		}
		
	}
	
}
