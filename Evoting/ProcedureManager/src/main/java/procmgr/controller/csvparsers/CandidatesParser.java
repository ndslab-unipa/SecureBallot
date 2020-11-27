package procmgr.controller.csvparsers;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import exceptions.FLRException;
import model.Person;
import procmgr.controller.Controller;
import procmgr.model.ProcedurePM;
import utils.FileUtils;

/**
 * Classe che si occupa di effettuare il parsing di file CSV relativi a candidati e liste elettorali. Estende {@link CSVParser}.
 */
public class CandidatesParser extends CSVParser {

	/**
	 * Controlla che il file CSV passato a parametro abbia la sintassi attesa e che contenga informazioni su candidati e liste elettorali.
	 * Se tutto va a buon fine, legge i candidati e le liste elettorali, e le alla procedura passata a parametro.
	 * In caso di problemi, utilizza {@link Controller#writeLogFile(String, ArrayList)} per stampare su un file di log gli errori emersi 
	 * durante il parsing del file. In ogni caso, <b>NON</b> effettua alcun tipo di modifica al DB.
	 * <br/>
	 * Per leggere e verificare il file, richiama {@link #verifyListsAndCandidatesData(String, ArrayList)}.
	 * @param c Riferimento al Controller di ProcedureManager
	 * @param p Riferimento alla procedura da creare, già inizializzata
	 * @param pathToCandidatesFile Percorso al file csv che contiene le informazioni sull'elettorato passivo
	 * @return True se il file è valido e contiene dati validi, false altrimenti
	 */
	public static boolean checkCandidatesAndLists(Controller c, ProcedurePM p, String pathToCandidatesFile) {
		ArrayList<String> errors = new ArrayList<>();
		initControllerAndProcedure(c, p);

		if(!verifyListsAndCandidatesData(pathToCandidatesFile, errors)) {
			newProcedure.resetCandidates(true);
			newProcedure.resetLists(true);

			//Se si sono verificati errori li stampiamo in un file di log
			if(errors.size() != 0)
				controller.writeLogFile("candidati", errors);
			
			return false;
		}

		return true;
	}
	
	/**
	 * Controlla che il file CSV passato a parametro abbia la sintassi attesa e che contenga informazioni su candidati o schede elettorali. 
	 * Instanzia un BufferedReader per leggere il file, quindi lo scorre dalla prima all'ultima riga.
	 * <br/>
	 * Per ogni riga: rimuove commenti e spazi; richiama {@link #verifyCandidateRow(String, ArrayList)} o {@link #verifyListRow(String, ArrayList)}
	 * per verificare che la riga sia valida, a seconda del tipo di riga.
	 * @param pathToFile Percorso al file csv che contiene le informazioni sull'elettorato passivo
	 * @param errors ArrayList in cui inserire eventuali errori
	 * @return True se tutte le verifiche vanno a buon fine, false altrimenti
	 */
	private static boolean verifyListsAndCandidatesData(String pathToFile, ArrayList<String> errors) {
		boolean valid = true;
		newProcedure.resetLists(false);
		newProcedure.resetCandidates(false);

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
			while ((row = csvReader.readLine()) != null) {
				row = FileUtils.removeCommentsAndBlankSpaces(row, fieldSeparator);
				
				if(!row.isEmpty()) {
					//Per ogni riga non vuota distinguiamo se si tratta di un candidato o di una lista tramite il primo carattere
					switch(row.charAt(0)) {
						case 'l':
						case 'L':
							//Verifichiamo che i dati siano interpretabili come lista
							valid &= verifyListRow(row, errors);
							break;
							
						case 'c':
						case 'C':
							//Verifichiamo che i dati siano interpretabili come candidato
							valid &= verifyCandidateRow(row, errors);
							break;
							
						default:
							errors.add("Riga: " + row +
									"\n\tIl primo carattere deve essere 'l' o 'L' per indicare una lista elettorale," +
									"\n\toppure 'c' o 'C' per indicare un candidato. Qualunque altro carattere non è accettato." +
									"\n\tCarattere Trovato: " + row.charAt(0));
							valid = false;
					}
				}
			}
		}
		catch (IOException e) {
			controller.printError(FLRException.FLR_14(pathToFile, true, e));
			valid = false;
		}
		finally {
			FileUtils.closeFileReader(csvReader);
		}

		return valid;
	}

	/**
	 * Effettua la verifica di una singola riga del file sull'elettorato passivo, facente riferimento ad una lista elettorale.
	 * Controlla che il numero di campi trovato sia uguale a quello atteso, quindi verifica che ogni campo sia interpretabile 
	 * secondo il tipo atteso (numerico, stringa, ..).
	 * <br/>
	 * La riga attesa ha il seguente formato:
	 * <p>L/l ; Codice Lista; Nome Lista</p>
	 * Se la riga risulta valida, allora procede a richiamare {@link ProcedurePM#addList(int, String, ArrayList)} per aggiungere la lista
	 * elettorale alla nuova procedura.
	 * @param row Stringa corrispondente alla riga letta
	 * @param errors ArrayList in cui inserire eventuali errori
	 * @return True se la riga è valida e l'aggiunzione alla procedura va a buon fine, false altrimenti
	 */
	private static boolean verifyListRow(String row, ArrayList<String> errors) {
		//Ogni riga ha 3 campi: la "L" / "l", il codice univoco della lista e il nome
		int numFields = 3;

		//Si verifica il numero di campi
		String[] data = row.split(fieldSeparator);
		if(data.length != numFields) {
			errors.add("Riga: " + row + 
					"\n\tNumero di campi non corretto."+
					"\n\tCampi attesi: " + numFields +
					"\n\tCampi trovati: " + data.length);
	    	return false;
		}

		//Il primo campo viene saltato perchè contiene la "L" che identifica la riga come una lista.
		String code = data[1], name = data[2];
		
		//Si verifica la validità dei campi
		boolean validCode = FileUtils.isNumeric(code), validName = FileUtils.isAlpha(name);

		if(!validCode || !validName) {
			errors.add("Riga: " + row +
					"\n\tErrore nei campi inseriti:" +
					(validCode ? "" : "\n\tIl codice inserito per la lista non è numerico") +
					(validName ? "" : "\n\tIl nome inserito per la lista contiene caratteri non permessi"));
			return false;
	    }

		return newProcedure.addList(Integer.parseInt(code), name, errors);
	}

	/**
	 * Effettua la verifica di una singola riga del file sull'elettorato passivo, facente riferimento ad un candidato.
	 * Controlla che il numero di campi trovato sia uguale a quello atteso, quindi verifica che ogni campo sia interpretabile 
	 * secondo il tipo atteso (numerico, stringa, ..).
	 * <br/>
	 * La riga attesa ha il seguente formato:
	 * <p>C/c; Codice Candidato; Nome Candidato; Cognome Candidato; Data Di Nascita/NULL</p>
	 * Se la riga risulta valida, allora procede a richiamare {@link ProcedurePM#addCandidate(Person, ArrayList)} per aggiungere
	 * il candidato alla nuova procedura.
	 * @param row Stringa corrispondente alla riga letta
	 * @param errors ArrayList in cui inserire eventuali errori
	 * @return True se la riga è valida e l'aggiunzione alla procedura va a buon fine, false altrimenti
	 */
	private static boolean verifyCandidateRow(String row, ArrayList<String> errors) {
		//Il numero di campi deve essere esattamente 5
		int numFields = 5;

		//Verifichiamo che il numero di campi sia corretto
		String[] data = row.split(fieldSeparator);
		if(data.length != numFields) {
			errors.add("Riga: " + row + 
					"\n\tNumero di campi non corretto."+
					"\n\tCampi attesi: " + numFields +
					"\n\tCampi trovati: " + data.length);
	    	return false;
		}

		String ID = data[1], firstName = data[2], lastName = data[3], dateOfBirth = data[4];

	    //Si verifica la validità dei singoli campi
	    boolean validID = FileUtils.isID(ID);
	    boolean validFirstName = FileUtils.isAlpha(firstName);
	    boolean validLastName = FileUtils.isAlpha(lastName);
		boolean validDateOfBirth = FileUtils.isDate(dateOfBirth) || FileUtils.isNull(dateOfBirth);

	    //Si segnalano gli errori riscontrati per la riga
	    if(!validID || !validFirstName || !validLastName || !validDateOfBirth) {
	    	errors.add("Riga: " + row +
	    			"\n\tErrore nei campi inseriti:" +
	    			(validID ? "" : "\n\t- La matricola inserita non è alfanumerica") +
	    			(validFirstName ? "" : "\n\t- Il nome contiene caratteri non permessi") +
	    			(validLastName ? "" : "\n\t- Il cognome contiene caratteri non permessi") +
	    			(validDateOfBirth ? "" : "\n\t- Errore nella formattazione della data di nascita"));
			return false;
	    }
	    
	    String birthDate = FileUtils.isDate(dateOfBirth) ? dateOfBirth : null;
		return newProcedure.addCandidate(new Person(firstName, lastName, birthDate, ID), errors);
	}
}
