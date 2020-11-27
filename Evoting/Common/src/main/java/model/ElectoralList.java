package model;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * 
 * @author marco
 * Classe che modella una lista elettorale.
 * Deriva da parsable per la necessità di trasformarla in una stringa per poterla inviare via socket.
 */
public class ElectoralList extends Parsable {
	
	private static final long serialVersionUID = 1L;

	//Codice della lista (utile solo alla creazione procedura)
	private final int code;

	//Nome della lista.
	private final String name;
	
	//Mappa che lega un codice univoco relativo alla persona (matricola, CF, Carta d'identità o quello che si deciderà)
	//ad una coppia persona-boolean, la prima rappresenta il candidato la seconda se è stato selezionato dal votante.
	private final LinkedHashMap< String, Person > list;

	/**
	 * Costruttore, inizializza il nome e crea la mappa vuota.
	 * @param name	Nome della lista.
	 */
	public ElectoralList(String name) {
		this.name = name;
		list = new LinkedHashMap<>();
		code = -1;
	}

	/**
	 * Costruttoreusato solo per la creazione della procedura, inizializza il nome e il codice.
	 * @param name	Nome della lista.
	 * @param code 	Codice della lista.
	 */
	public ElectoralList(String name, int code) {
		this.name = name;
		this.code = code;
		list = new LinkedHashMap<>();
	}

	/**
	 * Restituisce il codice della lista (utile solo alla creazione della procedura).
	 */
	public int getCode(){
		return code;
	}

	/**
	 * Restituisce il nome della lista.
	 * @return	Il nome della lista.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Aggiunge una persona alla lista.
	 * Se il codice univoco relativo alla persona esiste già restituisce null e non effettua operazioni (per evitare sovrascritture).
	 * Altrimenti aggiunge la persona (con preferenza posta a false ovviamente) e restituisce this.
	 * @param person	La persona da aggiungere alla lista.
	 * @return			this o null a seconda se l'operazione ha avuto successo.
	 * 					Probabilmente andrebbe cambiato e lanciare una eccezione invece di restituire null. 
	 */
	public ElectoralList addPerson(Person person) {
		String ID = person.getID();
		if(list.containsKey(ID)) {
			return null;
		}
		
		list.put(ID, person);
		return this;
	}
	
	/**
	 * Funzione d'utility da mettere alla fine di qualunque sequenza di aggiunzioni per controllare che anche 
	 * l'ultima aggiunzione abbia avuto successo e non restituito null.
	 * Se il codice di addPerson dovesse cambiare come specificato questa funzione non servirebbe più.
	 * @return	this.
	 */
	public ElectoralList end() {return this;}

	public ArrayList<Person> getCandidates() {
		ArrayList<Person> candidates = new ArrayList<Person>();
		
		for(Person candidate : list.values()) {
			//Object[] copy = {((Person) pair[0]).clone(), new Boolean((boolean) pair[1])};
			//persons.add(copy);
			candidates.add(candidate);
		}
		
		return candidates;
	}
	public boolean contains(String personID) {
		return list.containsKey(personID);
	}
	
	@Override
	public Object clone() {
		ElectoralList copy = new ElectoralList(name);
		
		for(Person person : list.values()) {
			copy.addPerson(person);
			//copy.setPreference(((Person) pair[0]).getID(), (Boolean) pair[1]);
		}
		
		return copy;
	}
	
}
