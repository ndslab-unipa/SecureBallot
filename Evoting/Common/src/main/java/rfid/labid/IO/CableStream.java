/*
 * CableStream.java
 *
 * Created on 18 febbraio 2005, 9.50
 */

package rfid.labid.IO;

import java.io.*;
/**
 *
 * @author  Daniele
 */
public interface CableStream {
	
	public void Close() throws IOException ;
	
	public int Read(byte[] buffer, int offset, int count) throws IOException;
	
	public void Write(byte[] buffer, int offset, int count) throws IOException ;
	
	public int Read(byte[] buffer) throws IOException ;
	
	public void Write(byte[] buffer) throws IOException ;
	
	public void Flush() throws IOException ;
	
	public void SetTimeout(int ReceiveTimeout) ;
	
	public void Purge();

}