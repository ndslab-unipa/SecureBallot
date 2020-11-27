package procmgr.controller.csvparsers;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import exceptions.FLRException;
import procmgr.controller.Controller;
import procmgr.model.ProcedurePM;
import utils.FileUtils;

/**
 * Classe che si occupa di effettuare il parsing di file CSV relativi alle schede elettorali. Estende {@link CSVParser}.
 */
public class BallotsParser extends CSVParser {
	
	/**
	 * Controlla che il file CSV passato a parametro abbia la sintassi attesa e che contenga informazioni su schede elettorali.
	 * Se tutto va a buon fine, legge le schede elettorali e le aggiunge alla procedura passata a parametro. In caso di problemi, 
	 * utilizza {@link Controller#writeLogFile(String, ArrayList)} per stampare su un file di log gli errori emersi durante 
	 * il parsing del file. In ogni caso, <b>NON</b> effettua alcun tipo di modifica al DB.
	 * <br/>
	 * Per leggere e verificare il file, richiama {@link #verifyBallotsData(String, ArrayList)}.
	 * @param c Riferimento al Controller di ProcedureManager
	 * @param p Riferimento alla procedura da creare, già inizializzata
	 * @param pathToBallotsFile Percorso al file csv che contiene le informazioni sulle schede.
	 * @return True se il file è valido e contiene dati validi, false altrimenti
	 */
	public static boolean checkBallots(Controller c, ProcedurePM p, String pathToBallotsFile) {
		ArrayList<String> errors = new ArrayList<>();
		initControllerAndProcedure(c, p);

		if(!verifyBallotsData(pathToBallotsFile, errors)) {
			newProcedure.resetProcedureBallots(true);
			
			//Se si sono verificati errori li stampiamo in un file di log
			if(errors.size() > 0)
				controller.writeLogFile("schede", errors);
			
			return false;
		}
		
		return true;
	}
	
	/**
	 * Controlla che il file CSV passato a parametro abbia la sintassi attesa e che contenga informazioni su schede elettorali. Instanzia
	 * un BufferedReader per leggere il file, quindi lo scorre dalla prima all'ultima riga.
	 * <br/>
	 * Per ogni riga: rimuove commenti e spazi; richiama {@link #verifyBallotRow(String, ArrayList)} per verificare che la riga
	 * sia valida.
	 * <br/>
	 * Lette tutte le righe, verifica inoltre che il numero di schede letto sia uguale a quello inserito dall'utente nel form di creazione
	 * della procedura.
	 * @param pathToFile Percorso al file csv che contiene le informazioni sulle schede.
	 * @param errors ArrayList in cui inserire eventuali errori
	 * @return True se tutte le verifiche vanno a buon fine, false altrimenti
	 */
	private static boolean verifyBallotsData(String pathToFile, ArrayList<String> errors) {
		boolean valid = true;
		newProcedure.resetProcedureBallots(false);

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
					ArrayList<String> ballotErrors = new ArrayList<>();

					//Parso ogni riga e verifico che possa essere interpretata come una scheda valida
					if(!verifyBallotRow(row, ballotErrors)) {
						
						//Eventuali errori sui campi vengono aggiunti agli errori "globali"
						if(!ballotErrors.isEmpty()) {
							String rowErrors = "";
							for (String error : ballotErrors)
								rowErrors += "\n\t" + error;
							
							errors.add("Riga: " + row + rowErrors);
						}
						
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

		valid &= newProcedure.allBallotsAdded(errors);
		return valid;
	}
	
	/**
	 * Effettua la verifica di una singola riga del file contenente le schede elettorali. Controlla che il numero di campi trovato
	 * sia uguale a quello atteso, quindi verifica che ogni campo sia interpretabile secondo il tipo atteso (numerico, stringa, ..).
	 * <br/>
	 * La riga attesa ha il seguente formato:
	 * <p><b>Scheda con candidati:</b> C/c ; Codice Scheda; Titolo; Descrizione; Numero massimo preferenze; Codice_Candidato_1 [: Lista_Candidato1] ; [...] ; Codice_Candidato_N [: Lista_Candidato_N]</p>
	 * <p><b>Scheda con opzioni:</b> O/o ; Codice Scheda; Titolo; Descrizione; Numero massimo preferenze; Opzione_1 ; [...] ; Opzione_N</p>
	 * Se la riga risulta valida, allora procede a richiamare {@link ProcedurePM#addBallot(int, String, String, int, ArrayList)} per aggiungere una scheda
	 * alla procedura, e {@link ProcedurePM#addOptionToBallot(int, String, ArrayList)} o {@link ProcedurePM#addCandidateToBallot(int, String, Integer, ArrayList)}
	 * per aggiungere, rispettivamente, opzioni di referendum o candidati alla scheda.
	 * @param row Stringa corrispondente alla riga letta
	 * @param errors ArrayList in cui inserire eventuali errori
	 * @return True se la riga è valida e le aggiunzioni alla procedura vanno a buon fine, false altrimenti
	 */
	private static boolean verifyBallotRow(String row, ArrayList<String> errors){
		//Il numero di campi è variabile a seconda del numero di opzioni/candidati, ma devono essere almeno 6
		int minFields = 6;
		
		//Verifichiamo che il numero di campi sia corretto
		String[] data = row.split(fieldSeparator);
		if(data.length < minFields) {
			errors.add("Numero di campi trovati non corretto."+
					"\n\tNumero di campi atteso: Almeno " + minFields +
					"\n\tNumero di campi trovati: " + data.length);
			return false;
		}

		String identifier = data[0];
		String code = data[1];
		String title = data[2];
		String description = data[3];
		String preferences = data[4];
		boolean valid = true, isCandidateRow = true;

		//Si verificano i campi "fissi"
		switch(identifier) {
			case "c":
			case "C":
				isCandidateRow = true;
				break;
				
			case "o":
			case "O":
				isCandidateRow = false;
				break;
				
			default:
				errors.add("Il primo carattere deve essere 'c' o 'C' per indicare un candidato," +
						"\n\toppure 'o' o 'O' per indicare una opzione di referendum. Qualunque altro carattere non è accettato." +
						"\n\tCarattere Trovato: " + identifier);
				valid = false;
		}

		if(!FileUtils.isNumeric(code)) {
			errors.add("Il codice inserito non è numerico");
			valid = false;
		}

		if(!FileUtils.isGenericText(title)) {
			errors.add("Il titolo inserito contiene caratteri non permessi");
			valid = false;
		}

		if(!FileUtils.isGenericText(description)) {
			errors.add("La descrizione inserita contiene caratteri non permessi");
			valid = false;
		}

		if(!FileUtils.isNumeric(preferences)) {
			errors.add("Il numero di preferenze non è un numero intero");
			valid = false;
		}
		
		if(!valid) {
			return false;
		}

		int ballotCode = Integer.parseInt(code);
		int numPreferences = Integer.parseInt(preferences);
		if(!newProcedure.addBallot(ballotCode, title, description, numPreferences, errors)) {
			return false;
		}

		//Si scorrono i campi opzioni/candidati
		for (int f = minFields - 1; f < data.length; f++) {
			String field = data[f];
			
			if(!isCandidateRow) { //Scheda opzioni: Opzione
				valid &= newProcedure.addOptionToBallot(ballotCode, field, errors);
			}
			else { //Scheda candidati: CodiceCandidato [: ListaCandidato]
				String[] IDAndList = FileUtils.removeCommentsAndBlankSpaces(field, ":").split(":");
				String errorMsg = "La riga " + field + " non presenta il formato atteso (ID_Candidato:Codice_Lista)";

				int nFields = IDAndList.length;
				if(nFields > 2) {
					errors.add(errorMsg + " - Rilevati troppi campi separati da :");
					valid = false;
				}
				else {
					String candidateID = IDAndList[0];
					String listCode = nFields == 2 ? IDAndList[1] : null;

					if(listCode != null && !FileUtils.isNumeric(listCode)) {
						errors.add(errorMsg + " - Il codice della lista non è numerico");
						valid = false;
					}

					if(!FileUtils.isID(candidateID)) {
						errors.add(errorMsg + " - " + candidateID + " non è un ID valido per un candidato");
						valid = false;
					}

					if(valid)
						valid = newProcedure.addCandidateToBallot(ballotCode, candidateID, listCode != null ? Integer.parseInt(listCode) : null, errors);
				}
			}
		}

		return valid;
	}
}
