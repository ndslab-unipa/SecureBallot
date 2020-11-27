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
 * Classe che si occupa di effettuare il parsing di file CSV relativi ai votanti di una tornata elettorale. Estende {@link CSVParser}.
 */
public class VotersParser extends CSVParser {

	/**
	 * Controlla che il file CSV passato a parametro abbia la sintassi attesa e che contenga informazioni sui votanti.
	 * Se tutto va a buon fine, legge i votanti e li aggiunge alla procedura passata a parametro.
	 * In caso di problemi, utilizza {@link Controller#writeLogFile(String, ArrayList)} per stampare su un file di log gli errori emersi 
	 * durante il parsing del file. In ogni caso, <b>NON</b> effettua alcun tipo di modifica al DB.
	 * <br/>
	 * Per leggere e verificare il file, richiama {@link #verifyVotersData(String, ArrayList)}.
	 * @param c Riferimento al Controller di ProcedureManager
	 * @param p Riferimento alla procedura da creare, già inizializzata
	 * @param pathToVotersFile Percorso al file csv che contiene le informazioni sull'elettorato attivo
	 * @return True se il file è valido e contiene dati validi, false altrimenti
	 */
	public static boolean checkVoters(Controller c, ProcedurePM p, String pathToVotersFile) {
		ArrayList<String> errors = new ArrayList<>();
		initControllerAndProcedure(c, p);

		if(!verifyVotersData(pathToVotersFile, errors)) {
			newProcedure.resetVoters(true);
		
			//Se si sono verificati errori li stampiamo in un file di log
			if(errors.size() != 0)
				controller.writeLogFile("votanti", errors);
			
			return false;
		}

		return true;
	}
	
	/**
	 * Controlla che il file CSV passato a parametro abbia la sintassi attesa e che contenga informazioni sui votanti. 
	 * Instanzia un BufferedReader per leggere il file, quindi lo scorre dalla prima all'ultima riga.
	 * <br/>
	 * Per ogni riga: rimuove commenti e spazi; richiama {@link #verifyVoterRow(String, ArrayList)} per verificare che la 3
	 * riga sia valida.
	 * @param pathToFile Percorso al file csv che contiene le informazioni sull'elettorato passivo
	 * @param errors ArrayList in cui inserire eventuali errori
	 * @return True se tutte le verifiche vanno a buon fine e vi è almeno un votante, false altrimenti
	 */
	private static boolean verifyVotersData(String pathToFile, ArrayList<String> errors) {
		boolean isValid = true;
		boolean noVoters = true;
		newProcedure.resetVoters(false);

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
				//Verifico che ogni riga non vuota sia interpretabile come un votante
				row = FileUtils.removeCommentsAndBlankSpaces(row, fieldSeparator);
				
				if(!row.isEmpty()) {
					noVoters = false;
					isValid = isValid && verifyVoterRow(row, errors);
				}
			}

			//Ovviamente una votazione non può non avere votanti
			if(noVoters) {
				isValid = false;
				String error = "Errore relativo al file \"" + pathToFile + "\""
						+ "\n\tNon è presente alcun votante";
				errors.add(error);
			}
		}
		catch (IOException e) {
			controller.printError(FLRException.FLR_14(pathToFile, true, e));
			isValid = false;
		} finally {
			FileUtils.closeFileReader(csvReader);
		}

		return isValid;
	}

	/**
	 * Effettua la verifica di una singola riga del file sull'elettorato attivo. Controlla che il numero di campi trovato sia 
	 * uguale a quello atteso, quindi verifica che ogni campo sia interpretabile secondo il tipo atteso (numerico, stringa, ..).
	 * <br/>
	 * La riga attesa ha il seguente formato:
	 * <p>Codice Votante; Nome; Cognome; Codice_Scheda_1 [: ... : Codice_Scheda_N]; Data di Nascita/NULL</p>
	 * Se la riga risulta valida, allora procede a richiamare {@link ProcedurePM#addVoter(Person, ArrayList)} per aggiungere 
	 * il votante alla nuova procedura.
	 * @param row Stringa corrispondente alla riga letta
	 * @param errors ArrayList in cui inserire eventuali errori
	 * @return True se la riga è valida e l'aggiunzione alla procedura va a buon fine, false altrimenti
	 */
	private static boolean verifyVoterRow(String row, ArrayList<String> errors) {
		//Il numero di campi deve essere 5
		int fieldNumber = 5;

		//Verifichiamo che il numero di campi sia corretto
		String[] data = row.split(fieldSeparator);
		if(data.length != fieldNumber) {
			errors.add("Riga: " + row + "\n\tNumero di campi trovati non corretto." +
					"\n\tNumero di campi atteso: " + fieldNumber +
					"\n\tNumero di campi trovati: " + data.length);
			return false;
		}
		
		String ID = data[0], firstName = data[1], lastName = data[2], ballots = data[3], dateOfBirth = data[4];
		int numBallots = newProcedure.getNumBallots();

		//Verifichiamo la validità dei singoli campi
		boolean validCode = FileUtils.isID(ID);
		boolean validFirstName = FileUtils.isAlpha(firstName);
		boolean validLastName = FileUtils.isAlpha(lastName);
		boolean validSequenceOfBallots = FileUtils.checkBallotsSequence(ballots, numBallots);
		boolean validDateOfBirth = FileUtils.isDate(dateOfBirth) || FileUtils.isNull(dateOfBirth);

		if(!validCode || !validFirstName || !validLastName || !validSequenceOfBallots || !validDateOfBirth) {
			errors.add("Riga: " + row +
	    			"\n\tErrore nei campi inseriti:" +
	    			(validCode ? "" : "\n\t- La matricola inserita non è alfanumerica") +
	    			(validFirstName ? "" : "\n\t- Il nome contiene caratteri non permessi") +
	    			(validLastName ? "" : "\n\t- Il cognome contiene caratteri non permessi") +
	    			(validSequenceOfBallots ? "" : "\n\t- Le schede di voto inserite sono errate (valori accettati fra 1 e "+numBallots+")") +
	    			(validDateOfBirth ? "" : "\n\t- Errore nella formattazione della data di nascita"));
			return false;
		}
		
		int[] ballotCodes = FileUtils.getBallotsSequence(ballots, numBallots);
		String birthDate = FileUtils.isDate(dateOfBirth) ? dateOfBirth : null;
		return newProcedure.addVoter(new Person(firstName, lastName, ID, ballotCodes, false, birthDate), errors);
	}

}
