package procmgr.model;

import model.ElectoralList;
import model.EmptyBallot;
import model.Person;
import model.Procedure;
import model.Session;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

import exceptions.PEException;

/**
 * Classe che modella la procedura in fase di creazione presso il ProcedureManager. Mantiene tutti i dati necessari alla nuova procedura,
 * finchè questa non viene memorizzata nel DB.
 * <br/>
 * Permette, inoltre, di verificare che la procedura sia pronta, ovvero che tutti i dati richiesti siano stati aggiunti e siano validi.
 * <br/>
 * Estende {@link model.Procedure}.
 */
public class ProcedurePM extends Procedure {
	public static final int noListCode = -1;
	
	private final int numBallots;

	//Sessioni
	private HashMap<Integer, Session> sessions;
	private HashMap<Integer, HashMap<Integer, VotingTerminals>> sessionPlaces;
	private HashMap<Integer, Integer> sessionNextFreeTerminalIDs;
	private HashMap<Integer, HashSet<InetAddress>> sessionStations;
	private HashMap<Integer, HashSet<InetAddress>> sessionPosts;
	private HashMap<Integer, HashSet<InetAddress>> sessionSubStations;

	//Candidati
	private HashMap<Integer, String> listsNames;
	private HashMap<String,Person> candidates;

	//Schede
	private EmptyBallot[] procedureBallots;
	private HashMap<Integer, ArrayList<String>>[] ballots2ElectoralLists2Candidates;
	private HashSet<String>[] candidatesPerBallot;
	
	//Votanti
	private HashMap<String, Person> voters;
	
	/**
	 * Costruttore con parametri che inizializza tutti i dati richiesti.
	 * @param code Codice della procedura
	 * @param name Nome della procedura
	 * @param start Data e ora di inizio della procedura
	 * @param end Data e ora di fine della procedura
	 * @param numBallots Numero di schede contenute nella procedura
	 * @param supervisor Nome del supervisore della procedura
	 * @throws PEException Se la data di fine è precedente alla data di inizio della procedura
	 */
	public ProcedurePM(int code, String name, LocalDateTime start, LocalDateTime end, int numBallots, String supervisor) throws PEException {
		super(code, name, start, end, supervisor);
		this.numBallots = numBallots;
	}
	
	/**
	 * Getter per il numero di schede contenute nella procedura.
	 * @return Numero di schede
	 */
	public int getNumBallots() {
		return numBallots;
	}
	
	/**
	 * Verifica che la procedura sia pronta, ovvero che tutti i dati richiesti siano stati inseriti, quindi richiama {@link #completeBallotsCreation(ArrayList)}
	 * per restituire le schede da memorizzare nel DB. Se la procedura non è pronta, restituisce null.
	 * @param errors Lista a cui aggiungere eventuali errori riscontrati durante la verifica dei dati
	 * @return Array di schede elettorali ({@link model.EmptyBallot}), o null
	 */
	public EmptyBallot[] checkProcedureReadyAndGetBallots(ArrayList<String> errors){
		boolean valid = true;

		if(supervisor == null){
			errors.add("Selezionare il supervisore.");
			valid = false;
		}

		if(sessions == null || sessionPlaces == null) {
			errors.add("L'aggiunzione delle sessioni non è ancora terminata con successo.");
			valid = false;
		}

		if(candidates == null || listsNames == null) {
			errors.add("L'aggiunzione dei candidati non è ancora terminata con successo.");
			valid = false;
		}

		if(procedureBallots == null || ballots2ElectoralLists2Candidates == null ) {
			errors.add("L'aggiunzione delle schede non è ancora terminata con successo.");
			valid = false;
		}

		if(voters == null) {
			errors.add("L'aggiunzione dei votanti non è ancora terminata con successo.");
			valid = false;
		}

		if(!valid)
			return null;

		return completeBallotsCreation(errors);
	}

	/************************************
	**** FUNZIONI PER BALLOTS PARSER ****
	*************************************/
	
	/**
	 * Resetta gli oggetti relativi alle schede elettorali, a seconda del flag passato a parametro, settando tutto a null (true) o inizializzando
	 * nuovamente (false).
	 * @param destroy Flag per discriminare l'operazione da compiere sugli attributi
	 */
	@SuppressWarnings("unchecked")
	public void resetProcedureBallots(boolean destroy){
		if(destroy) {
			procedureBallots = null;
			ballots2ElectoralLists2Candidates = null;
			candidatesPerBallot = null;
		}
		else {
			procedureBallots = new EmptyBallot[numBallots];
			ballots2ElectoralLists2Candidates = new HashMap[numBallots];
			candidatesPerBallot = new HashSet[numBallots];
		}
	}
	
	/**
	 * Permette di aggiungere una nuova scheda alla procedura in corso di creazione. E' richiamata per ogni riga valida letta nel file delle schede.
	 * @param ballotCode Codice della scheda
	 * @param title Titolo della scheda
	 * @param description Descrizione della scheda
	 * @param maxPreferences Massimo numero di preferenze esprimibili sulla scheda
	 * @param errors Lista a cui aggiungere eventuali errori nell'aggiunzione della scheda
	 * @return True se l'aggiunzione è andata a buon fine, false altrimenti
	 */
	public boolean addBallot(int ballotCode, String title, String description, int maxPreferences, ArrayList<String> errors) {
		int ballotIndex = ballotCode - 1;

		if(ballotIndex < 0 || ballotIndex >= numBallots){
			errors.add("Inserimento di una scheda fallito, il codice indicato (" + ballotCode + ") non è tra quelli permessi. Deve essere maggiore di 0 e minore o uguale al numero di schede.");
			return false;
		}

		EmptyBallot oldBallot = procedureBallots[ballotIndex];
		if(oldBallot != null) {
			errors.add("Inserimento di una scheda fallito, le seguenti schede presentano lo stesso codice:\n\t- " + ballotCode
			+ title + ", " + description + ", numPref:" + maxPreferences + "\n\t- "
			+ oldBallot.getTitle() + ", "+ oldBallot.getDescription() + ", numPref:" + oldBallot.getMaxPreferences());
			return false;
		}

		procedureBallots[ballotIndex] = new EmptyBallot(title, ballotCode, description, maxPreferences);
		ballots2ElectoralLists2Candidates[ballotIndex] = new HashMap<>();
		candidatesPerBallot[ballotIndex] = new HashSet<>();

		return true;
	}
	
	/**
	 * Permette di aggiungere un candidato ad una scheda elettorale. Si limita a verificare che la scheda esista e che un candidato con lo stesso codice
	 * non sia già presente per la scheda.
	 * <br/>
	 * Il controllo sull'esistenza di un candidato col codice specificato è effettuato in {@link #completeBallotsCreation(ArrayList)}, quando tutti i dati
	 * necessari alla procedura sono già stati inseriti ed è possibile effettuare i controlli di consistenza.
	 * @param ballotCode Codice della scheda
	 * @param candidateID ID del candidato
	 * @param listCode Codice della lista
	 * @param errors Lista a cui aggiungere eventuali errori nell'aggiunzione del candidato
	 * @return True se l'aggiunzione del candidato va a buon fine, false altrimenti
	 */
	public boolean addCandidateToBallot(int ballotCode, String candidateID, Integer listCode, ArrayList<String> errors){
		int ballotIndex = ballotCode - 1;

		HashMap<Integer, ArrayList<String>> ballot = ballots2ElectoralLists2Candidates[ballotIndex];
		if(ballot == null) {
			errors.add("Inserimento del candidato " + candidateID + " in una scheda fallito:"
					+ " non esiste alcuna scheda con codice " + ballotCode);
			return false;
		}

		HashSet<String> candidatesInThisBallot = candidatesPerBallot[ballotIndex];
		if(candidatesInThisBallot.contains(candidateID)) {
			errors.add("Inserimento del candidato " + candidateID + " in una scheda fallito:"
					+ " il candidato " + candidateID + " è stato già inserito nella scheda " + ballotCode);

			return false;
		}
		
		candidatesInThisBallot.add(candidateID);

		//Per indicare i candidati non appartenenti ad alcuna lista
		if(listCode == null)
			listCode = noListCode;

		ArrayList<String> candidatesIDsInList = ballot.computeIfAbsent(listCode, k -> new ArrayList<>());
		candidatesIDsInList.add(candidateID);

		return true;
	}
	
	/**
	 * Permette di aggiungere una opzione di referendum ad una scheda elettorale.
	 * @param ballotCode Codice della scheda
	 * @param option Opzione di referendum
	 * @param errors Lista a cui aggiungere eventuali errori nell'aggiunzione del candidato
	 * @return True se l'aggiunzione del candidato va a buon fine, false altrimenti
	 */
	public boolean addOptionToBallot(int ballotCode, String option, ArrayList<String> errors){
		int ballotIndex = ballotCode - 1;

		EmptyBallot ballot = procedureBallots[ballotIndex];
		if(ballot == null) {
			errors.add("Inserimento dell'opzione " + option +" in una scheda fallito:"
					+ " non esiste alcuna scheda con codice " + ballotCode);
			return false;
		}

		ballot.addOption(option);
		return true;
	}

	/**
	 * Verifica che il numero di schede create sia pari a quello atteso.
	 * @param errors Lista a cui aggiungere eventuali errori nell'aggiunzione del candidato
	 * @return True se la verifica va a buon fine, false altrimenti
	 */
	public boolean allBallotsAdded(ArrayList<String> errors){
		boolean valid = true;

		for(int i = 0; i < numBallots; i++){
			if(procedureBallots[i] == null || ballots2ElectoralLists2Candidates[i] == null || candidatesPerBallot[i] == null) {
				errors.add("Non è stata inserita alcuna scheda con codice " + (i + 1));
				valid = false;
			}
		}

		return valid;
	}
	
	/***************************************
	**** FUNZIONI PER CANDIDATES PARSER ****
	****************************************/
	
	/**
	 * Resetta gli oggetti relativi alle liste elettorali, a seconda del flag passato a parametro, settando tutto a null (true) o inizializzando
	 * nuovamente (false).
	 * @param destroy Flag per discriminare l'operazione da compiere sugli attributi
	 */
	public void resetLists(boolean destroy) {
		listsNames = destroy ? null : new HashMap<>();
	}

	/**
	 * Resetta gli oggetti relativi ai candidati, a seconda del flag passato a parametro, settando tutto a null (true) o inizializzando
	 * nuovamente (false).
	 * @param destroy Flag per discriminare l'operazione da compiere sugli attributi
	 */
	public void resetCandidates(boolean destroy){
		candidates = destroy ? null : new HashMap<>();
	}
	
	/**
	 * Permette di aggiungere un nuovo candidato, verificando che non ne esista già uno con lo stesso codice.
	 * @param newCandidate Oggetto {@link model.Person} contenente i dati del nuovo candidato
	 * @param errors Lista a cui aggiungere eventuali errori nell'aggiunzione del candidato
	 * @return True se l'aggiunzione del candidato va a buon fine, false altrimenti
	 */
	public boolean addCandidate(Person newCandidate, ArrayList<String> errors) {
		String ID = newCandidate.getID();
		Person oldCandidate = candidates.get(ID);

		if(oldCandidate == null) {
			candidates.put(ID, newCandidate);
			return true;
		}

		errors.add("Inserimento di un candidato fallito, i seguenti candidati presentano lo stesso ID:" + "\n\t- "
				+ newCandidate.getLastName() + ", " + newCandidate.getFirstName() + "\n\t- "
				+ oldCandidate.getLastName() + ", " + oldCandidate.getFirstName());

		return false;
	}

	/**
	 * Permette di aggiungere una nuova lista elettorale, verificando che non ne esista già uno con lo stesso codice.
	 * @param code Codice della lista
	 * @param name Nome della lista
	 * @param errors Lista a cui aggiungere eventuali errori nell'aggiunzione del candidato
	 * @return True se l'aggiunzione della lista va a buon fine, false altrimenti
	 */
	public boolean addList(int code, String name, ArrayList<String> errors) {
		String oldName = listsNames.get(code);

		if(oldName == null) {
			listsNames.put(code, name);
			return true;
		}

		errors.add("Inserimento di una lista fallito, le seguenti liste presentano lo stesso codice:" + code + "\n\t"
				+ name + "\n\t"
				+ oldName);

		return false;
	}
	
	/*************************************
	**** FUNZIONI PER SESSIONS PARSER ****
	**************************************/

	/**
	 * Resetta gli oggetti relativi alle sessioni, a seconda del flag passato a parametro, settando tutto a null (true) o inizializzando
	 * nuovamente (false).
	 * @param destroy Flag per discriminare l'operazione da compiere sugli attributi
	 */
	public void resetSessions(boolean destroy) {
		if(destroy) {
			sessions = null;
			sessionPlaces = null;
			sessionNextFreeTerminalIDs = null;
			sessionStations = null;
			sessionPosts = null;
			sessionSubStations = null;
		}
		else {
			sessions = new HashMap<>();
			sessionPlaces = new HashMap<>();
			sessionNextFreeTerminalIDs = new HashMap<>();
			sessionStations = new HashMap<>();
			sessionPosts = new HashMap<>();
			sessionSubStations = new HashMap<>();
		}
	}
	
	/**
	 * Verifica che una sessione, identificata dal suo codice, esista già all'interno della procedura corrente.
	 * @param sessionCode Codice della sessione
	 * @return True se la sessione esiste, false altrimenti
	 */
	public boolean existsSessionCode(int sessionCode) {
		return sessions.containsKey(sessionCode);
	}
	
	/**
	 * Permette di aggiungere una nuova nuova sessione. Verifica che non ne esista già una con lo stesso codice, che le date di inizio e fine
	 * siano contenute all'interno della procedura e che queste non si sovrappongano con altre sessioni già create.
	 * @param code Codice della sessione
	 * @param name session Oggetto {@link model.Session} contenente tutte le informazioni sulla sessione da aggiungere
	 * @param errors Lista a cui aggiungere eventuali errori nell'aggiunzione del candidato
	 * @return True se l'aggiunzione della sessione va a buon fine, false altrimenti
	 */
	public boolean addSession(int code, Session session, ArrayList<String> errors) {
		if(existsSessionCode(code)) {
			errors.add("Il codice " + code + " è già aoperato da un'altra sessione");
			return false;
		}

		//Si verifica che la sessione sia interna alla procedura
		if(!session.inProcedure(getStart(), getEnd())) {
			errors.add("La sessione risulta almeno in parte al di fuori del periodo della procedura");
			return false;
		}

		for(Session s : sessions.values())
			if(session.before(s) == -1) {
				errors.add("La sessione risulta almeno in parte sovrapposta con una o più altre sessioni");
				return false;
			}

		sessions.put(code, session);
		sessionPlaces.put(code, new HashMap<>());
		sessionNextFreeTerminalIDs.put(code, 1);
		sessionStations.put(code, new HashSet<>());
		sessionPosts.put(code, new HashSet<>());
		sessionSubStations.put(code, new HashSet<>());

		return true;
	}
	
	/**
	 * Verifica che, per una data sessione, l'IP del seggio principale non sia già stato inserito. Quindi, aggiunge il nuovo seggio e crea l'oggetto
	 * {@link VotingTerminals} per memorizzare gli IP delle postazioni e dei seggi ausiliari che fanno riferimento al seggio specificato.
	 * @param sessionCode Codice della sessione
	 * @param stationIp Indirizzo IP del seggio principale
	 * @return ID assegnato al seggio principale, o null
	 */
	public Integer createVotingPlaces(int sessionCode, InetAddress stationIp) {
		HashSet<InetAddress> stations = sessionStations.get(sessionCode);
		if(stations.contains(stationIp))
			return null;

		stations.add(stationIp);

		int stationID = getFreeTerminalID(sessionCode);
		sessionPlaces.get(sessionCode).put(stationID, new VotingTerminals(stationID, stationIp));

		return stationID;
	}

	/**
	 * Permette di aggiungere una postazione ai terminali noti per una data sessione, per un dato seggio principale.
	 * @param sessionCode Codice della sessione
	 * @param stationID ID del seggio principale
	 * @param postIp Indirizzo IP della nuova postazione
	 * @return True se l'aggiunzione va a buon fine, false altrimenti
	 */
	public boolean addPostToVotingPlace(int sessionCode, int stationID, InetAddress postIp){
		HashSet<InetAddress> posts = sessionPosts.get(sessionCode);
		if(posts.contains(postIp))
			return false;

		VotingTerminals place = sessionPlaces.get(sessionCode).get(stationID);
		if(place == null)
			return false;

		posts.add(postIp);

		int postID = getFreeTerminalID(sessionCode);
		place.addPost(postID, postIp);
		return true;
	}

	/**
	 * Permette di aggiungere un seggio ausiliario ai terminali noti per una data sessione, per un dato seggio principale.
	 * @param sessionCode Codice della sessione
	 * @param stationID ID del seggio principale
	 * @param postIp Indirizzo IP del nuovo seggio ausiliario
	 * @return True se l'aggiunzione va a buon fine, false altrimenti
	 */
	public boolean addSubStationToVotingPlace(int sessionCode, int stationID, InetAddress subStationIp){
		HashSet<InetAddress> subStations = sessionSubStations.get(sessionCode);
		if(subStations.contains(subStationIp))
			return false;

		VotingTerminals place = sessionPlaces.get(sessionCode).get(stationID);
		if(place == null)
			return false;

		subStations.add(subStationIp);

		int subStationID = getFreeTerminalID(sessionCode);
		place.addSubStation(subStationID, subStationIp);
		return true;
	}
	
	/***********************************
	**** FUNZIONI PER VOTERS PARSER ****
	************************************/
	
	/**
	 * Resetta gli oggetti relativi ai votanti, a seconda del flag passato a parametro, settando tutto a null (true) o inizializzando
	 * nuovamente (false).
	 * @param destroy Flag per discriminare l'operazione da compiere sugli attributi
	 */
	public void resetVoters(boolean destroy) {
		voters = destroy ? null : new HashMap<>();
	}
	
	/**
	 * Permette di aggiungere un votante alla procedura corrente, dopo aver verificato che non ne esista già uno con lo stesso ID.
	 * @param newVoter Oggetto {@link model.Person} contenente tutti i dati del votante
	 * @param errors Lista a cui aggiungere eventuali errori nell'aggiunzione del candidato
	 * @return True se l'aggiunzione va a buon fine, false altrimenti
	 */
	public boolean addVoter(Person newVoter, ArrayList<String> errors) {
		String ID = newVoter.getID();
		Person oldVoter = voters.get(ID);

		if(oldVoter == null) {
			voters.put(ID, newVoter);
			return true;
		}

		errors.add("Inserimento di un votante fallito, i seguenti votanti presentano lo stesso ID:" + "\n\t- "
				+ newVoter.getLastName() + ", " + newVoter.getFirstName() + "\n\t- "
				+ oldVoter.getLastName() + ", " + oldVoter.getFirstName());

		return false;
	}
	
	/**************************
	**** FUNZIONI PER PMDB ****
	***************************/
	
	/**
	 * Riordina le sessioni inserite per la nuova procedura, quindi restituisce una mappa contenente, 
	 * per ciascuna sessione, la lista di {@link VotingTerminals}.
	 * <br/>
	 * Ogni oggetto VotingTerminals contiene un seggio principale e la lista di seggi ausiliari e postazioni ad esso afferenti. Se per una data sessione
	 * sono presenti più seggi principali, allora la lista avrà più di un elemento al suo interno. 
	 * @return Mappa delle sessioni
	 */
	public HashMap<Session, ArrayList<VotingTerminals>> getSessionsData(){
		LinkedHashMap<Session, ArrayList<VotingTerminals>> sessionData = new LinkedHashMap<>();
		ArrayList<Session> sortedSessions = getSortedSession();

		for(Session session : sortedSessions){
		    int sessionCode = session.getCode();
		    ArrayList<VotingTerminals> places = new ArrayList<>();
		    sessionPlaces.get(sessionCode).forEach((stationID, place) -> places.add(place));
		    sessionData.put(session, places);
        }

		return sessionData;
	}
	
	/**
	 * Getter per la lista di candidati da inserire per la nuova procedura.
	 * @return Lista di candidati
	 */
	public ArrayList<Person> getCandidates(){
		ArrayList<Person> candidatesArrayList = new ArrayList<>();
		candidates.forEach((ID, candidate) -> candidatesArrayList.add(candidate));
		return candidatesArrayList;
	}

	/**
	 * Getter per la lista di liste elettorali da inserire per la nuova procedura.
	 * @return Lista di liste elettorali
	 */
	public ArrayList<ElectoralList> getLists(){
		ArrayList<ElectoralList> lists = new ArrayList<>();
		listsNames.forEach((code, name) -> lists.add(new ElectoralList(name, code)));
		return lists;
	}
	
	/**
	 * Getter per l'elenco di votanti da inserire per la nuova procedura.
	 * @return Elenco di votanti, sotto forma di mappa
	 */
	public HashMap<String, Person> getVoters() {
		return voters;
	}
	
	/****************************
	**** FUNZIONI DI UTILITY ****
	*****************************/
	
	/**
	 * Verifica che tutte le schede siano state create e che i candidati e le liste elettorali siano validi. Quindi, organizza i candidati in liste e
	 * le aggiunge alle schede, finalizzandone la creazione. 
	 * @param errors Lista a cui aggiungere eventuali errori nell'aggiunzione del candidato
	 * @return L'array contenente le schede elettorali, o null
	 */
	private EmptyBallot[] completeBallotsCreation(ArrayList<String> errors) {
		for(EmptyBallot ballot : procedureBallots) {
			if(ballot == null) {
				errors.add("L'aggiunzione delle schede non è ancora terminata con successo.");
				return null;
			}
		}

		int ballotsSize = procedureBallots.length;

		for(int i = 0; i < ballotsSize; i++) {
			HashMap<Integer, ArrayList<String>> electoralLists2Candidates = ballots2ElectoralLists2Candidates[i];

			final int ballotIndex = i;
			final int ballotCode = i + 1;
			
			electoralLists2Candidates.forEach( (listCode, candidatesIDs) -> {
				String listName = listCode != -1 ? listsNames.get(listCode) : "candidati senza lista";

				if(listName == null) {
					StringBuilder error = new StringBuilder("Nella scheda " + ballotCode + " è stata richiesta lista non esistente " + listCode + " per i candidati\n\t");
					for(String candidateID : candidatesIDs)
						error.append(candidateID).append("\t");
					errors.add(error.toString());
				}
				else {
					ElectoralList list = new ElectoralList(listName, listCode);

					for(String candidateID : candidatesIDs) {
						Person candidate = candidates.get(candidateID);
						if(candidate == null){
							String error = "Nella scheda " + ballotCode + " è stato richiesto il candidato non esistente " + candidateID;
							errors.add(error);
						}
						else {
							list.addPerson(candidate);
						}
					}

					procedureBallots[ballotIndex].addList(list);
				}

			});
		}

		if(errors.isEmpty())
			return procedureBallots;

		return null;
	}

	/**
	 * Riordina le sessioni inserite per la nuova procedura, quindi restituisce la lista ordinata di sessioni.
	 * @return Lista ordinata di sessioni
	 */
	private ArrayList<Session> getSortedSession(){
        ArrayList<Session> sortedSessions = new ArrayList<>();

        sessions.forEach((code, session) -> {
            boolean added = false;
            int size = sortedSessions.size();
            for(int s = 0; !added && (s < size); s++){
                Session sortedSession = sortedSessions.get(s);

                if(session.before(sortedSession) == 1){
                    sortedSessions.add(s, session);
                    added = true;
                }
            }

            if(!added)
                sortedSessions.add(session);
        });

        return sortedSessions;
    }

	/**
	 * Funzione di utility utilizzata per ottenere il prossimo ID libero per un terminale, per una data sessione. Quest'informazione è memorizzata
	 * in una mappa; ogni volta che questa funzione viene chiamata, la corrispondente entry nella mappa viene incrementata.
	 * @param sessionCode Codice della sessione
	 * @return ID da assegnare al terminale
	 */
	private int getFreeTerminalID(int sessionCode) {
		int freeTerminalID = sessionNextFreeTerminalIDs.get(sessionCode);
		sessionNextFreeTerminalIDs.put(sessionCode, freeTerminalID + 1);
		return freeTerminalID;
	}
}