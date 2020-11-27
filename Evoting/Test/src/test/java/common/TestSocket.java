package common;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * 
 * @author marco
 * Classe usata unicamente a scopo di test per simulare la comunicazione tramite Socket in un singolo computer.
 * Per permettere una comunicazione bidirezionale crea 2 Scanner e 2 PrintWriter e li connette (Scanner con PrintWriter).
 * Per far comunicare 2 parti deve fornire ad entrambe uno Scanner e un PrintWriter tramite le funzioni get(Other)Input e get(Other)Output.
 * Si può comunicare direttamente da codice con una parte fornendole Scanner e PrintWriter SOLO tramite getInput e getOutput,
 * e scrivendo con la funzione write e leggendo con la read.
 */
public class TestSocket{
	private Scanner in0;
	private PrintWriter out0;
	
	private Scanner in1;
	private PrintWriter out1;
	
	public TestSocket() throws IOException {
		
		PipedOutputStream outStrm0 = new PipedOutputStream();
        PipedInputStream inStrm0 = new PipedInputStream(outStrm0);
        
        PipedOutputStream outStrm1 = new PipedOutputStream();
        PipedInputStream inStrm1 = new PipedInputStream(outStrm1);
    	
        out0 = new PrintWriter(outStrm0, true);
        in0 = new Scanner(inStrm0);
        
        out1 = new PrintWriter(outStrm1, true);
        in1 = new Scanner(inStrm1);
        
	}
	/*
	public void write(String message) {
		out0.println(message);
	}
	
	public void write(int value) {
		out0.println(value);
	}
	
	public String read() {
		String message = "Wrong Message";
		
		if(in1 != null && in1.hasNextLine()) {
			message = in1.nextLine();
		}
		else {
			assertTrue(false);
		}
		return message;
	}
	*/
	public PrintWriter getOutput0() {
		return out1;
	}
	
	public Scanner getInput0() {
		return in0;
	}
	
	public PrintWriter getOutput1() {
		return out0;
	}
	
	public Scanner getInput1() {
		return in1;
	}
	/*
	public void close() {
		out0.close();
		in0.close();
		
		out1.close();
		in1.close();
	}
	*/
}