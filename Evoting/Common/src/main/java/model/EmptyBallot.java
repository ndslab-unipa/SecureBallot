package model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
/**
 * 
 * @author marco
 * Classe che modella la Scheda elettorale da presentare ai votanti.
 */
public class EmptyBallot extends Parsable {

	private static final long serialVersionUID = 1L;
	
	//Titolo della scheda (posizione per cui si sta votando).
	private final String title;
	
	//Codice univoco della scheda.
	private final int code;
	
	//Descrizione della scheda.
	private final String text;
	
	//Preferenze massime esprimibili.
	private final int maxPref;
	
	//Insieme di tutte le liste elettorali presenti nella scheda.
	private final LinkedHashMap<String, ElectoralList> lists;
	
	//Lista di opzioni che non riguardano candidati (necessari per votazioni come referendum).
	private final ArrayList<String> options;
	
	/**
	 * Costruttore, setta titolo e preferenze esprimibili.
	 * @param title		Titolo della scheda.
	 * @param maxPref	Numero di preferenze esprimibili.
	 */
	public EmptyBallot(String title, int code, String text, int maxPref) {
		this.title = title;
		this.code = code;
		this.text = text;
		this.lists = new LinkedHashMap<>();
		this.options = new ArrayList<>();
		this.maxPref = maxPref;
	}
	
	/**
	 * Restituisce il titolo della scheda.
	 * @return	Titolo della scheda.
	 */
	public String getTitle() {
		return title;
	}
	
	public int getCode() {
		return code;
	}
	
	public String getDescription() {
		return text;
	}
	
	/**
	 * Aggiunge una lista alla scheda.
	 * Se il titolo della lista è già presente restituisce null e non effettua operazioni (per evitare sovrascritture).
	 * Altrimenti aggiunge la lista e restituisce this.
	 * @param list		La lista da aggiungere.
	 * @return			this o null.
	 */
	public EmptyBallot addList(ElectoralList list) {
		if(list == null) {
			return null;
		}
		
		if(lists.containsKey(list.getName())) {
			return null;
		}
		
		lists.put(list.getName(), list);
		return this;
	}

	public EmptyBallot end() {
		return this;
	}
	
	/**
	 * Restituisce le liste di questa scheda come array.
	 * @return	Le liste contenute nella scheda.
	 */
	public ArrayList<ElectoralList> getLists() {
		ArrayList<ElectoralList> listArray = new ArrayList<>();
			
		lists.forEach((name, list) -> listArray.add(list));
		
		return listArray;
	}

	/**
	 * Aggiunge un'opzione a quelle disponibili.
	 * @param option Opzione da aggiungere.
	 */
	public void addOption(String option) {
		options.add(option);
	}
	
	/**
	 * Restituisce una copia di tutte le opzioni disponibili per questa scheda.
	 * @return Un copia di options.
	 */
	public ArrayList<String> getOptions(){
		return new ArrayList<>(options);
	}
	
	/**
	 * Restituisce il numero di preferenze esprimibili.
	 * @return	Il numero preferenze massimo.
	 */
	public int getMaxPreferences() {
		return maxPref;
	}
	
}
