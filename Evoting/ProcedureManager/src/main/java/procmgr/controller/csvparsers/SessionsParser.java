package procmgr.controller.csvparsers;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;

import exceptions.FLRException;
import model.Session;
import procmgr.controller.Controller;
import procmgr.model.ProcedurePM;
import utils.FileUtils;

/**
 * Classe che si occupa di effettuare il parsing di file CSV relativi alle sessioni e alle postazioni di voto. Estende {@link CSVParser}.
 */
public class SessionsParser extends CSVParser {
	
	/**
	 * Controlla che il file CSV passato a parametro abbia la sintassi attesa e che contenga informazioni sulle sessioni di voto e sui
	 * rispettivi terminali. Se tutto va a buon fine, legge sessioni di voto e terminali, e li aggiunge alla procedura passata a parametro.
	 * In caso di problemi, utilizza {@link Controller#writeLogFile(String, ArrayList)} per stampare su un file di log gli errori emersi 
	 * durante il parsing del file. In ogni caso, <b>NON</b> effettua alcun tipo di modifica al DB.
	 * <br/>
	 * Per leggere e verificare il file, richiama {@link #verifySessionsAndVotingTerminalsData(String, ArrayList)}.
	 * @param c Riferimento al Controller di ProcedureManager
	 * @param p Riferimento alla procedura da creare, già inizializzata
	 * @param pathToSessionsFile Percorso al file csv che contiene le informazioni sulle sessioni
	 * @return True se il file è valido e contiene dati validi, false altrimenti
	 */
	public static boolean checkSessionsAndVotingPlaces(Controller c, ProcedurePM p, String pathToSessionsFile) {
		ArrayList<String> errors = new ArrayList<>();
		initControllerAndProcedure(c,p);

		//Se si sono verificati errori li stampiamo in un file di log
		if(!verifySessionsAndVotingTerminalsData(pathToSessionsFile, errors)) {
			newProcedure.resetSessions(true);

			if(errors.size() != 0)
				controller.writeLogFile("sessioni", errors);
			
			return false;
		}
		
		return true;
	}
	
	/**
	 * Controlla che il file CSV passato a parametro abbia la sintassi attesa e che contenga informazioni sulle sessioni ed i terminali. 
	 * Instanzia un BufferedReader per leggere il file, quindi lo scorre dalla prima all'ultima riga.
	 * <br/>
	 * Per ogni riga: rimuove commenti e spazi; richiama {@link #verifySessionsAndVotingTerminalsData(String, ArrayList)}
	 * per verificare che la riga sia valida.
	 * @param pathToFile Percorso al file csv che contiene le informazioni sulle sessioni
	 * @param errors ArrayList in cui inserire eventuali errori
	 * @return True se tutte le verifiche vanno a buon fine, false altrimenti
	 */
	private static boolean verifySessionsAndVotingTerminalsData(String pathToFile, ArrayList<String> errors) {
		boolean isValid = true;
		newProcedure.resetSessions(false);

		//Verifichiamo l'esistenza del file
		BufferedReader csvReader;
		try {
			csvReader = FileUtils.getFileReader(pathToFile);
		} catch (FLRException e) {
			controller.printError(e);
			return false;
		}

		String row;
		try {
			//ArrayList<Session> sessions = new ArrayList<>();
			while ((row = csvReader.readLine()) != null) {
				//Per ogni riga del file eliminiamo commenti, e compattiamo spazi e tabulazioni in un singolo spazio.
				row = FileUtils.removeCommentsAndBlankSpaces(row, fieldSeparator);

				//Se la riga non resta vuota (ad esempio se era un commento dall'inizio riga)
				//verifichiamo che la singola sessione sia valida
				if(!row.isEmpty())
					isValid = isValid && verifySessionOrVotingTerminalsRow(row, errors);
			}
		}
		catch (IOException e) {
			controller.printError(FLRException.FLR_14(pathToFile, true, e));
			isValid = false;
		}
		finally {
			FileUtils.closeFileReader(csvReader);
		}

		return isValid;
	}

	/**
	 * Effettua la verifica di una singola riga del file sulle sessioni. Controlla che il numero di campi trovato sia 
	 * uguale a quello atteso, quindi richiama {@link #verifySessionRow(String, String, String)} o {@link #verifyVotingTerminalsRow(String, String, String, String)}
	 * a seconda del tipo di riga che deve controllare.
	 * @param row Stringa corrispondente alla riga letta
	 * @param errors ArrayList in cui inserire eventuali errori
	 * @return True se la riga è valida e l'aggiunzione alla procedura va a buon fine, false altrimenti
	 */
	private static boolean verifySessionOrVotingTerminalsRow(String row, ArrayList<String> errors) {
		//Ogni riga relativa a una sessione ha 3 campi: codice (temporaneo), inizio e fine
		int numFieldsSession = 3;

		//Ogni riga relativa a un "voting place" ha 4 campi: codice (temporaneo), ip seggio, ip postazioni e ip seggi ausiliari
		int numFieldsPlace = 4;

		//Si verifica il numero di campi
		String[] data = row.split(fieldSeparator);
		if(data.length != numFieldsSession && data.length != numFieldsPlace) {
			errors.add("Riga: "+row+"\n\tNumero di campi trovati non corretto." +
					"\n\tNumero di campi atteso: " + numFieldsSession + " o " + numFieldsPlace +
					"\n\tNumero di campi trovati: " + data.length);
			return false;
		}

		ArrayList<String> rowErrors = new ArrayList<>();
		String code = data[0];

		if(data.length == numFieldsSession) {
			String starts = data[1], ends = data[2];
			rowErrors = verifySessionRow(code, starts, ends);
		}
		else {
			String stationIp = data[1], postsIps = data[2], subStationsIps = data[3];
			rowErrors = verifyVotingTerminalsRow(code, stationIp, postsIps, subStationsIps);
		}
		
		if(rowErrors != null && rowErrors.size() > 0) {
			String rowError = "Riga: " + row;
			for(String error : rowErrors)
				rowError += "\n\t "+error;
			errors.add(rowError);
			
			return false;
		}

		return true;
	}

	/**
	 * Effettua la verifica di una singola riga del file sulle sessioni. Controlla che la riga contenga informazioni su una
	 * sessione di votazioni, quindi verifica che ogni campo sia interpretabile secondo il tipo atteso (numerico, stringa, ..).
	 * <br/>
	 * La riga attesa ha il seguente formato:
	 * <p>Codice Sessione; Timestamp Inizio; Timestamp Fine</p>
	 * Se la riga risulta valida, allora procede a richiamare {@link ProcedurePM#addSession(int, Session)} per aggiungere 
	 * la sessione alla nuova procedura.
	 * @param code Stringa contenente il codice della sessione
	 * @param starts Stringa contenente il timestamp di inizio della sessione
	 * @param ends Stringa contenente il timestamp di fine della sessione
	 * @return Lista contenente gli errori trovati sulla riga, o null
	 */
	private static ArrayList<String> verifySessionRow(String code, String starts, String ends) {
		ArrayList<String> errors = new ArrayList<>();
		
		int intCode = 0;
		try {
			intCode = Integer.parseInt(code);
		}
		catch(NumberFormatException e) {
			errors.add("Il codice della sessione non è un numero intero");
		}

		//Si prova ad interpretare le stringhe inizio e fine come timestamp
		LocalDateTime dateStarts = FileUtils.dateStringToLocalDateTime(starts);
		LocalDateTime dateEnds = FileUtils.dateStringToLocalDateTime(ends);
		
		if(dateStarts == null || dateEnds == null) {
			errors.add("Le date non sono formattate correttamente. Formattazione Richiesta: gg/MM/aaaa hh:mm:ss");
		}

		if(!errors.isEmpty())
			return errors;

		//Viene creata la nuova sessione
		Session session = new Session(intCode, dateStarts, dateEnds);

		//Si verifica che l'inizio sia precedente alla fine
		if(!session.isChronological()) {
			errors.add("La data di inizio è uguale o successiva a quella di termine");
			return errors;
		}
		
		if(!newProcedure.addSession(intCode, session, errors)) {
			return errors;
		}
		
		return null;
	}

	/**
	 * Effettua la verifica di una singola riga del file sulle sessioni. Controlla che la riga contenga informazioni sui terminali di
	 * voto per una sessione, quindi verifica che ogni campo sia interpretabile secondo il tipo atteso (numerico, stringa, ..).
	 * <br/>
	 * La riga attesa ha il seguente formato:
	 * <p>Codice Sessione; IP Seggio; IP_Postazione_1 [: ... : IP_Postazione_N]; IP_Seggio_Ausiliario_1 [: ... : IP_Seggio_Ausiliario_N]</p>
	 * Se la riga risulta valida, allora procede a richiamare {@link ProcedurePM#createVotingPlaces(int, InetAddress)},
	 * {@link ProcedurePM#addPostToVotingPlace(int, int, InetAddress)} e {@link ProcedurePM#addSubStationToVotingPlace(int, int, InetAddress)}
	 * per inserire, rispettivamente, gli IP di seggio, postazioni e seggi ausiliari (per una data sessione) alla nuova procedura.
	 * @param sessionCode Stringa contenente il codice della sessione
	 * @param stationIpSTR Stringa contenente l'IP del seggio
	 * @param postsIpsSTR Stringa contenente gli IP delle postazioni, separati da ":"
	 * @param subStationsIpsSTR Stringa contenente gli IP dei seggi ausiliari, separati da ":"
	 * @return Lista contenente gli errori trovati sulla riga, o null
	 */
	private static ArrayList<String> verifyVotingTerminalsRow(String sessionCode, String stationIpSTR, String postsIpsSTR, String subStationsIpsSTR) {
		ArrayList<String> errors = new ArrayList<>();
		ArrayList<InetAddress> postsIps;
		ArrayList<InetAddress> subStationsIps;
		
		int intCode = 0;
		try{
			intCode = Integer.parseInt(sessionCode);
		}
		catch(NumberFormatException e){
			errors.add("Il codice della sessione non è un numero intero");
		}
		
		InetAddress stationIp = null;
		try {
			stationIp = InetAddress.getByName(stationIpSTR);
		} catch (UnknownHostException e) {
			errors.add("L'IP del seggio non è valido.");
		}

		postsIps = splitAndCheckIPs(postsIpsSTR);
		if(postsIps == null){
			errors.add("L'IP di una o più postazioni non è valido.");
		}

		subStationsIps = splitAndCheckIPs(subStationsIpsSTR);
		if(subStationsIps == null){
			errors.add("L'IP di uno o più seggi ausiliari non è valido.");
		}

		if(!errors.isEmpty())
			return errors;

		if(!newProcedure.existsSessionCode(intCode)){
			errors.add("Sessione " + sessionCode + " mancante.");
			return errors;
		}

		Integer stationID = newProcedure.createVotingPlaces(intCode, stationIp);

		if(stationID == null) {
			errors.add("Sessione " + sessionCode + " mancante o IP seggio " + stationIpSTR + " già presente per la sessione.");
			return errors;
		}

		for(InetAddress postIp : postsIps){
			if(!newProcedure.addPostToVotingPlace(intCode, stationID, postIp))
				errors.add("L'IP della postazione \"" + postIp.toString() + "\" è già adoperato da un'altra postazione per la stessa sessione.");
		}

		for(InetAddress subStationIp : subStationsIps){
			if(!newProcedure.addSubStationToVotingPlace(intCode, stationID, subStationIp))
				errors.add("L'IP del seggio ausiliario \"" + subStationIp.toString() + "\" è già adoperato da un altro seggio ausiliario per la stessa sessione.");
		}
		
		return errors.isEmpty() ? null : errors;
	}
	
	/**
	 * Funzione di utility che separa una stringa contenente diversi indirizzi IP, divisi fra loro da ":". Per ogni IP prova a creare
	 * un oggetto di tipo InetAddress, per verificare che l'IP sia valido, quindi restituisce la lista di oggetti InetAddress creati.
	 * <br/>
	 * Nel caso in cui un IP non fosse valido, l'oggetto InetAddress non potrebbe essere creato e la funzione ritornerebbe null.
	 * @param str Stringa contenente diversi indirizzi IP, separati da ":"
	 * @return Lista di oggetti InetAddress corrispondenti ai diversi indirizzi IP se tutto è andato a buon fine, o null
	 */
	private static ArrayList<InetAddress> splitAndCheckIPs(String str){
		String[] ipsStr = FileUtils.removeCommentsAndBlankSpaces(str, ":").split(":");

		ArrayList<InetAddress> ips = new ArrayList<>();
		try {
			for(String ipStr : ipsStr)
				ips.add(InetAddress.getByName(ipStr));
		}
		catch(UnknownHostException e){
			return null;
		}

		return ips;
	}
}
