package model;
import java.util.Arrays;
import java.util.Map;

import model.Parsable;
/**
 * 
 * @author marco
 * Classe adoperata per rappresentare una persona (votante o candidato).
 */
public class Person extends Parsable {
	private static final long serialVersionUID = 1L;
	
	public enum DocumentType {
		CARTA_IDENTITA,
		PATENTE,
		PASSAPORTO,
		CONOSCENZA_PERSONALE
	}
	
	//Nome
	private final String firstName;
	//Cognome
	private final String lastName;
	//Codice identificativo univoco della persona, da decidere (matricola, CF, carta d'identità etc...).
	private final String ID;
	//Lista di schede cui il votante può accedere
	private final int[] ballotCodes;
	//Indica se esistono per il votante schede che ancora non ha compilato.
	private final boolean mayVote;
	//Numero di voti ricevuti dal candidato.
	private final Integer votesReceived;
	//Data di nascita
	private String birthDate;
	//Dati sul documento
	private String documentID;
	private DocumentType documentType;
	
	/**
	 * Costruttore per il candidato durante la fase di votazione, setta tutti valori, i quali non potranno essere cambiati.
	 * @param firstName		Nome.
	 * @param lastName		Cognome.
	 * @param ID			Codice identificativo.
	 */
	public Person(String firstName, String lastName, String birthDate, String ID) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.birthDate = birthDate;
		this.ID = ID;
		this.ballotCodes = null;
		this.mayVote = false;
		this.votesReceived = null;
	}
	
	/**
	 * Costruttore per il candidato durante la fase di scrutinio, setta tutti valori, i quali non potranno essere cambiati.
	 * @param firstName		Nome.
	 * @param lastName		Cognome.
	 * @param ID			Codice identificativo.
	 */
	public Person(String firstName, String lastName, String birthDate, String ID, int votesReceived) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.birthDate = birthDate;
		this.ID = ID;
		this.ballotCodes = null;
		this.mayVote = false;
		this.votesReceived = votesReceived;
	}

	/**
	 * Costruttore per il votante, setta tutti valori, i quali non potranno essere cambiati.
	 * @param firstName		Nome.
	 * @param lastName		Cognome.
	 * @param ID			Codice identificativo.
	 * @param ballotCodes	Lista di interi relativi alle schede che il votante può votare.
	 * @param mayVote
	 */
	public Person(String firstName, String lastName, String ID, int[] ballotCodes, boolean mayVote) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.ID = ID;
		this.ballotCodes = ballotCodes;
		this.mayVote = mayVote;
		this.votesReceived = null;
	}
	
	public Person(String firstName, String lastName, String ID, int[] ballotCodes, boolean mayVote, String birth) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.ID = ID;
		this.ballotCodes = ballotCodes;
		this.mayVote = mayVote;
		this.votesReceived = null;
		this.birthDate = birth;
	}

	/**
	 * Restituisce il nome.
	 * @return	Nome.
	 */
	public String getFirstName() {
		return firstName;
	}
	
	/**
	 * Restituisce il cognome.
	 * @return	Cognome.
	 */
	public String getLastName() {
		return lastName;
	}
	
	/**
	 * Restituisce il codice identificativo.
	 * @return	Codice identificativo.
	 */
	public String getID() {
		return ID;
	}
	
	/**
	 * Restituisce gli indici delle schede che l'utente è abilitato a votare.
	 * @return I codici delle schede da votare.
	 */
	public int[] getBallotCodes() {
		return ballotCodes;
	}
	
	public String getBallotCodesString() {
		return Arrays.toString(ballotCodes);
	}
	
	/**
	 * Restituisce se la persona ha diritto a votare almeno per una scheda della sessione attuale.
	 * Sempre falso per i candidati, falso per un votante se ha già votato nella sessione.
	 * @return Il diritto o meno della persona a votare.
	 */
	public boolean mayVote() {
		return mayVote;
	}
	
	/**
	 * Restituisce i voti ricevuti dal candidato.
	 * @return I voti ricevuti dal candidato.
	 */
	public Integer getVotesReceived() {
		return votesReceived;
	}
	
	/**
	 * Indica se due persone hanno stesso ID.
	 * @param other L'altra persona.
	 * @return Se le due persone hanno stesso ID.
	 */
	public boolean hasSameID(Person other) {
		return ID.equals(other.ID);
	}
	
	public String getBirth() { return this.birthDate; }
	
	public void setDocumentID(String id) { this.documentID = id; }
	public String getDocumentID() { return documentID; }
	
	public void setDocumentType(String type) {
		Map<String, DocumentType> typeStringToDocumentType = Map.of(
				"Carta d'Identità", DocumentType.CARTA_IDENTITA,
				"Patente", DocumentType.PATENTE,
				"Passaporto", DocumentType.PASSAPORTO,
				"Conoscenza Personale", DocumentType.CONOSCENZA_PERSONALE
		);
		
		this.documentType = typeStringToDocumentType.get(type); 
	}
	
	public DocumentType getDocumentType() { 
		return documentType; 
	}
}
