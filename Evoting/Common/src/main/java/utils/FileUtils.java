package utils;

import exceptions.FLRException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Classe contenente alcune funzioni di utility utilizzate da più moduli.
 */
public class FileUtils {
	
	/**
	 * Stringa contenente tutti i caratteri permessi da file generici. Questi includono:
	 * <ul>
	 * <li>Tutte le lettere, maiuscole (A-Z) e minuscole (a-z)</li>
	 * <li>Tutti i numeri (0-9)</li>
	 * <li>La maggior parte delle lettere accentate (àèéìòùÈÉ)</li>
	 * <li>I principali caratteri di punteggiatura (?:;.,'")</li>
	 * <li>I principali caratteri speciali (%&/\()=)</li>
	 * </ul>
	 */
	private static String permittedGenericTextCharacter = "[a-zA-Z'àèéìòùÈÉ0-9!\'\"%&/\\()=?:;.,\\s]+";
	
	/**
	 * Restituisce un reader {@link BufferedReader} per il file specificato a parametro.
	 * @param pathToFile File da aprire
	 * @return BufferedReader sul file
	 * @throws FLRException Se emergono errori durante l'apertura del BufferedReader
	 */
	public static BufferedReader getFileReader(String pathToFile) throws FLRException {
		File csvFile = new File(pathToFile);
		
		BufferedReader csvReader;
		if (csvFile.isFile()) {
			try {
				csvReader = new BufferedReader(new FileReader(csvFile));
			} catch (FileNotFoundException e) {
				throw FLRException.FLR_14(pathToFile, false, e);
			}
		}
		else{
			throw FLRException.FLR_14(pathToFile, false, null);
		}
		return csvReader;
	}
	
	/**
	 * Chiude il reader {@link BufferedReader} passato a parametro.
	 * @param reader Reader aperto su un qualche file
	 */
	public static void closeFileReader(BufferedReader reader) {
		if(reader == null) {
			return;
		}
		
		try {
			reader.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Verifica che la stringa passata a parametro contenga solo numeri (una o più cifre).
	 * @param str Stringa da verificare
	 * @return True se la stringa contiene solo numeri, false altrimenti
	 */
	public static boolean isNumeric(String str) {
		if(str == null) {
			return false;
		}
		
		Pattern pattern = Pattern.compile("\\d+");
		return pattern.matcher(str).matches();
	}
	
	/**
	 * Verifica che la stringa passata a parametro contenga solo lettere (eventualmente accentate).
	 * @param str Stringa da verificare
	 * @return True se la stringa contiene solo lettere, false altrimenti
	 */
	public static boolean isAlpha(String str) {
		if(str == null) {
			return false;
		}
		
		Pattern pattern = Pattern.compile("[a-zA-Z'àèìòù\\s]+");
		return pattern.matcher(str).matches();
	}
	
	/**
	 * Verifica che la stringa passata a parametro contenga solo caratteri permessi, specificati in {@link #permittedGenericTextCharacter}.
	 * @param str Stringa da verificare
	 * @return True se la stringa contiene solo caratteri permessi, false altrimenti
	 */
	public static boolean isGenericText(String str) {
		if(str == null) {
			return false;
		}
		
		Pattern pattern = Pattern.compile(permittedGenericTextCharacter);
		return pattern.matcher(str).matches();
	}
	
	/**
	 * Verifica che la stringa passata a parametro corrisponda ad un identificativo univoco (matricola, codice fiscale).
	 * @param str Stringa da verificare
	 * @return True se la stringa contiene solo numeri e lettere, false altrimenti
	 */
	public static boolean isID(String str) {
		if(str == null) {
			return false;
		}
		
		Pattern pattern = Pattern.compile("[a-zA-Z0-9]+");
		return pattern.matcher(str).matches();
	}
	
	/**
	 * Verifica che la stringa passata a parametro corrisponda ad una data, nel formato gg/mm/aaaa.
	 * @param str Stringa da verificare
	 * @return True se la stringa corrisponde ad una data, false altrimenti
	 */
	public static boolean isDate(String str){
		if(str == null){
			return false;
		}

		String day = "([0]?[1-9]|[12][0-9]|3[01])";
		String month = "([0]?[1-9]|1[012])";
		String year = "(19[0-9]{2}|20[0-9]{2})";
		String regex = day + "/" + month + "/" + year;
		Pattern pattern = Pattern.compile(regex);

		return pattern.matcher(str).matches();
	}

	/**
	 * Verifica che la stringa passata a parametro corrisponda ad un orario, nel formato hh:mm:ss.
	 * @param str Stringa da verificare
	 * @return True se la stringa corrisponde ad un orario, false altrimenti
	 */
	public static boolean isTime(String str){
		if(str == null){
			return false;
		}

		String hours = "([01]?[0-9]|2[0-3])";
		String sexagesimal = "([0-5][0-9])";
		String regex = hours + ":" + sexagesimal + ":" + sexagesimal;
		Pattern pattern = Pattern.compile(regex);

		return pattern.matcher(str).matches();
	}

	public static boolean isNull(String str) {
		if(str.replace(" ", "").toUpperCase().equals("NULL")) {
			return true;
		}
		
		return false;
	}
	
	public static boolean isSessionKey(String sessionKey) {
		if(sessionKey == null) {
			return false;
		}
		
		return Pattern.compile(Constants.sessionKeyPatternMatcher).matcher(sessionKey).matches();
	}
	
	public static String removeCommentsAndBlankSpaces(String line, String fieldSeparator) {
		String newLine = line.trim()
				.replaceAll("\t+", " ")
				.replaceAll("\\s+", " ")
				.replace(" " + fieldSeparator, fieldSeparator)
				.replace(fieldSeparator + " ", fieldSeparator);
		
		int firstOccurrence = newLine.indexOf("#");
		return firstOccurrence == -1 ? newLine : newLine.substring(0, firstOccurrence);
	}
	
	public static boolean checkBallotsSequence(String row, int numBallots) {
		if(row == null) {
			return false;
		}
		
		String[] strCodes = row.split(",");
		for(int i = 0; i<strCodes.length; i++) {
			try {
				int code = Integer.parseInt(strCodes[i]);
				if(code < 1 || code > numBallots)
					return false;
			}
			catch(Exception e) {
				return false;
			}
		}
		
		return true;
	}
	
	public static int[] getBallotsSequence(String row, int numBallots) {
		if(row == null) {
			return null;
		}
		
		String[] strCodes = row.split(",");
		int[] codes = new int[strCodes.length];
		
		for(int i = 0; i<strCodes.length; i++) {
			try {
				int code = Integer.parseInt(strCodes[i]);
				if(code < 1 || code > numBallots)
					return null;
				
				codes[i] = code;
			}
			catch(Exception e) {
				return null;
			}
		}
		
		return codes;
	}
	
	public static LocalDateTime dateStringToLocalDateTime(String stringDate) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy[ [HH][:mm][:ss]]");
		
		try{
			LocalDateTime dateTime = LocalDateTime.parse(stringDate, formatter);
			return dateTime;
		}
		catch(DateTimeParseException e) {
			e.printStackTrace();
			return null;
		}
	}

	@SuppressWarnings("exports")
	public static java.sql.Date parseDate(String stringDate) throws ParseException{
		if(isNull(stringDate)) {
			return null;
		}

		java.util.Date utilDate = new SimpleDateFormat("dd/MM/yyyy").parse(stringDate);
		return new java.sql.Date(utilDate.getTime());
	}
}
