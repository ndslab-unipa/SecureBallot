package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe utilizzata per effettuare il logging delle operazioni compiute dai vari terminali. Al momento, è utilizzata in modo estensivo solo dall'urna, che stampa
 * i log sulla GUI e, contemporaneamente, su file. Tutti gli altri terminali contengono già il Logger (è contenuto in AbstrModel), ma al momento non ne è previsto 
 * l'utilizzo.
 */
public class Logger {
	private final boolean logOnFile;
	
	private ArrayList<String> logs;
	private int lastLog;
	
	private final String logDir = System.getProperty("user.dir") + "/logs/";
	private final DateTimeFormatter logDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	/**
	 * Costruttore con parametro che specifica se il logger deve stampare su file o no. In caso affermativo, verifica che la directory per i log esista (ed in caso
	 * provvede a crearla).
	 * @param logOnFile True per stampare su file, false altrimenti
	 */
	public Logger(boolean logOnFile) {
		this.logs = new ArrayList<>();
		this.lastLog = 0;
		this.logOnFile = logOnFile;
		
		if(logOnFile) {
			File dir = new File(logDir);
			if (!dir.exists()) 
				dir.mkdir();
		}
	}
	
	/**
	 * Restituisce tutti i log che non sono già stati ritornati. Ogni volta che viene chiamata tiene traccia dell'ultimo log inviato, in modo tale da non restituire
	 * più volte gli stessi log.
	 * @return Lista di log non ancora restituiti
	 */
	public ArrayList<String> getLogs() {
		List<String> newLogs = logs.subList(lastLog, logs.size());
		lastLog = logs.size();
		return new ArrayList<String>(newLogs);
	}
	
	/**
	 * Permette di loggare un evento, anteponendogli il label "[INFO]".
	 * @param event Evento da loggare
	 */
	public void logInfo(String event) { 
		logEvent("INFO", event); 
	}
	
	/**
	 * Permette di loggare un evento, anteponendogli il label "[SUCCESS]".
	 * @param event Evento da loggare
	 */
	public void logSuccess(String event) { 
		logEvent("SUCCESS", event); 
	}
	
	/**
	 * Permette di loggare un evento, anteponendogli il label "[WARNING]".
	 * @param event Evento da loggare
	 */
	public void logWarning(String event) { 
		logEvent("WARNING", event); 
	}
	
	/**
	 * Permette di loggare un evento, anteponendogli il label "[ERROR]".
	 * @param event Evento da loggare
	 */
	public void logError(String event) { 
		logEvent("ERROR", event); 
	}
	
	/**
	 * Aggiunge un timestamp ed il label scelto al log, quindi aggiunge il log completo alla lista mantenuta nella classe e, a seconda 
	 * dell'attributo {@link #logOnFile}, stampa anche il log su file.
	 * @param type Tipo di evento
	 * @param event Evento da loggare
	 */
	private void logEvent(String type, String event) {
		String fullLog = new java.util.Date()+" - ["+type+"] " + event;
		logs.add(fullLog);
		
		if(logOnFile)
			writeLogToFile(fullLog);
	}
	
	/**
	 * Scrive il log passato a parametro su un file. Il nome del file dipende dal giorno, secondo il formato "yyyy-mm-dd". 
	 * @param log Log da scrivere su file
	 */
	private void writeLogToFile(String log) {
		String file = logDir + logDateFormatter.format(LocalDateTime.now()) + ".log";
		
		try (PrintWriter pw = new PrintWriter(new FileOutputStream(file, true))) {
			pw.println(log);
		} 
		catch (FileNotFoundException e) { 
			e.printStackTrace(); 
		}
	}
}
