package procmgr.controller;

import db.ConnectionManager;
import db.DB;
import db.DBMS;
import encryption.RandStrGenerator;
import exceptions.DBException;
import exceptions.FLRException;
import exceptions.PEException;
import model.ElectoralList;
import model.EmptyBallot;
import model.Person;
import model.Session;
import procmgr.model.ProcedurePM;
import procmgr.model.Terminal;
import procmgr.model.VotingTerminals;
import utils.FileUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe che permette al modulo Procedure Manager di interfacciarsi col database. Estende la classe {@link db.DB DB}.
 */
public class PMDB extends DB {
	
	/**
	 * Costruttore con parametri che inizializza tutti i dati richiesti per potersi connettere al DB.
	 * @param host Indirizzo del DB
	 * @param port Porta del DB
	 * @param schema Schema del DB a cui accedere
	 * @throws PEException Se l'applicazione non riesce a recuperare username e password per connettersi al DB dal file <i>psws.cfg</i>
	 */
	public PMDB(String host, String port, String schema) throws PEException {
		dbms = new DBMS(host, port, schema, "Procedure Manager");
	}
	
	/*****************************************
	**** FUNZIONI PER CREAZIONE PROCEDURA ****
	******************************************/
	
	/**
	 * Permette l'inserimento di una nuova procedura all'interno del DB. Avvia la connessione al DB, verifica che il supervisore 
	 * scelto esista e richiama le funzioni preposte all'inserimento dei diversi dati richiesti dalla procedura.
	 * <br/>
	 * In particolare, richiama:
	 * <ul>
	 * <li>{@link #insertProcedureInDB(ProcedurePM, ConnectionManager)} per effettuare l'inserimento della procedura</li>
	 * <li>{@link #insertSessionsInDB(ProcedurePM, ConnectionManager)} per effettuare l'inserimento delle sessioni e dei terminali di voto</li>
	 * <li>{@link #insertListsAndCandidatesInDB(ProcedurePM, ConnectionManager)} per effettuare l'inserimento dell'elettorato passivo</li>
	 * <li>{@link #insertBallotsInDB(ProcedurePM, EmptyBallot[], ConnectionManager)} per effettuare l'inserimento delle schede e dei rispettivi candidati</li>
	 * <li>{@link #insertVotersInDB(ProcedurePM, ConnectionManager)} per effettuare l'inserimento dell'elettorato attivo</li>
	 * </ul>
	 * Se tutto va a buon fine, effettua COMMIT e conferma l'inserimento dei dati nel DB. Viceversa, effettua ROLLBACK e segnala
	 * gli errori.
	 * <br/><br/>
	 * <b>Nota:</b> Tutti i metodi che fanno inserimento sul DB non controllano i dati che inseriscono, dato che questi sono già stati controllati
	 * dal controller e dai parser CSV. Se emergono errori durante l'inserimento, questi vengono semplicemente lanciati come eccezzioni
	 * e catturati nel controller, in modo da poter essere documentati.
	 * @param newProcedure Procedura da inserire nel DB
	 * @param procedureBallots Schede elettorali per la procedura
	 * @throws PEException Se l'applicazione non riesce a connettersi al DB, se riscontra errori nell'esecuzione delle query SQL, 
	 * se riscontra errori nel recupero dei dati desiderati dal DB
	 */
	void uploadProcedure(ProcedurePM newProcedure, EmptyBallot[] procedureBallots) throws PEException {
		ConnectionManager cManager = dbms.getConnectionManager();

		try {
			cManager.startTransaction();

			String supervisor = newProcedure.getSupervisor();
			if(!existSupervisor(supervisor, cManager)) {
				throw DBException.DB_13(supervisor);
			}

			insertProcedureInDB(newProcedure, cManager);
			insertSessionsInDB(newProcedure, cManager);
			insertListsAndCandidatesInDB(newProcedure, cManager);
			insertBallotsInDB(newProcedure, procedureBallots, cManager);
			insertVotersInDB(newProcedure, cManager);

			cManager.commit();
		}
		catch (PEException e){
			cManager.rollback();
			throw e;
		}
		finally {
			cManager.close();
		}
	}
	
	/**
	 * Effettua l'inserimento di una nuova procedura all'interno della tabella `Procedure`.
	 * @param newProcedure Procedura da inserire
	 * @param cManager ConnectionManager per la connessione al DB
	 * @throws PEException Se riscontra errori nell'esecuzione delle query SQL
	 */
	private void insertProcedureInDB(ProcedurePM newProcedure, ConnectionManager cManager) throws PEException {
		String update = "INSERT INTO `Procedure` (Code, Supervisor, Name, Starts, Ends) " + 
						"VALUES (?, ?, ?, ?, ?) ;";

		int code = newProcedure.getCode();
		String supervisor = newProcedure.getSupervisor();
		String name = newProcedure.getName();
		Timestamp start = Timestamp.valueOf(newProcedure.getStart());
		Timestamp end = Timestamp.valueOf(newProcedure.getEnd());

		cManager.executeUpdate(update, code, supervisor, name, start, end);
	}
	
	/**
	 * Ritorna il primo codice utilizzabile come ID per la nuova procedura. Recupera dal DB l'ultimo ID utilizzato (il più grande),
	 * lo incrementa e lo restituisce al Controller.
	 * @return Codice della nuova procedura
	 * @throws PEException Se riscontra errori nell'esecuzione delle query SQL, se riscontra errori nel recupero dei dati desiderati dal DB
	 */
	int findNextFreeProcedureCode() throws PEException {
		String query = 	"SELECT MAX(P.Code) AS NewCode FROM `Procedure` AS P ;";
		int code;
		
		try (ConnectionManager cManager = dbms.getConnectionManager()) {
			ResultSet rs = cManager.executeQuery(query);
			
			code = rs.next() ? rs.getInt("NewCode") + 1 : 1;
			
		} catch (SQLException e) {
			throw DBException.DB_0(e);
		}
		
		return code;
	}
	
	/******************************************
	**** FUNZIONI PER INSERIMENTO SESSIONI ****
	*******************************************/
	
	/**
	 * Recupera dalla procedura la lista delle sessioni e dei corrispondenti terminali per il voto. Per ciascuna sessione, 
	 * invoca {@link #insertSessionInDB(int, int, Session, ArrayList, ConnectionManager)} per effettuare l'inserimento dei dati
	 * nel DB.
	 * @param newProcedure Procedura da inserire nel DB
	 * @param cManager ConnectionManager per la connessione al DB
	 * @throws PEException Se riscontra errori nell'esecuzione delle query SQL
	 */
	private void insertSessionsInDB(ProcedurePM newProcedure, ConnectionManager cManager) throws PEException {
		HashMap<Session, ArrayList<VotingTerminals>> sessionData = newProcedure.getSessionsData();

		int procedureCode = newProcedure.getCode();
		int sessionCode = 0;
		for(Map.Entry<Session, ArrayList<VotingTerminals>> data : sessionData.entrySet()){
			sessionCode++;
			insertSessionInDB(procedureCode, sessionCode, data.getKey(), data.getValue(), cManager);
		}
	}
	
	/**
	 * Effettua l'inserimento di una nuova sessione, per una data procedura, all'interno della tabella `Session`.
	 * @param procedureCode Codice della procedura
	 * @param sessionCode Codice della sessione
	 * @param session Oggetto {@link model.Session Session}, contenente tutti i dati sulla sessione
	 * @param terminalsList Lista di {@link procmgr.model.VotingTerminals VotingTerminals}, contenente tutti i terminali
	 * @param cManager ConnectionManager per la connessione al DB
	 * @throws PEException Se riscontra errori nell'esecuzione delle query SQL
	 */
	private void insertSessionInDB(int procedureCode, int sessionCode, Session session, ArrayList<VotingTerminals> terminalsList, ConnectionManager cManager) throws PEException {
		String updateSession = "INSERT INTO Session (ProcedureCode, Code, StartsAt, EndsAt) VALUES (?, ?, ?, ?) ;";

		cManager.executeUpdate(updateSession, procedureCode, sessionCode, session.getStart(), session.getEnd());

		for(VotingTerminals terminals : terminalsList)
			insertPlaceInDB(procedureCode, sessionCode, terminals, cManager);
	}

	/**
	 * Effettua l'inserimento dei terminali, per una data sessione di una procedura, all'interno della tabella `Terminal`. Inoltre,
	 * crea corrispondenza fra il seggio principale e tutte le sue postazioni e seggi ausiliari, inserendo la relazione all'interno
	 * della tabella `IsStationOf`. Infine, per ogni terminale genera una chiave di sessione randomica e la inserisce nel DB 
	 * all'interno della tabella `SessionKey`.
	 * @param procedureCode Codice della procedura
	 * @param sessionCode Codice della sessione
	 * @param terminals Oggetto {@link procmgr.model.VotingTerminals VotingTerminals}, contenente tutti i terminali
	 * @param cManager ConnectionManager per la connessione al DB
	 * @throws PEException Se riscontra errori nell'esecuzione delle query SQL
	 */
	private void insertPlaceInDB(int procedureCode, int sessionCode, VotingTerminals terminals, ConnectionManager cManager) throws PEException {
		String updateTerminal = "INSERT INTO Terminal (ProcedureCode, SessionCode, ID, IPAddress, Type) VALUES (?, ?, ?, ?, ?) ;";
		String updateIsStationOf = "INSERT INTO IsStationOf (ProcedureCode, SessionCode, Station, Terminal) VALUES (?, ?, ?, ?) ;";
		String updateSymmKey = "INSERT INTO SessionKey (ProcedureCode, SessionCode, TerminalID, SymmetricKey) VALUES(?, ?, ?, ?) ;";

		Terminal station = terminals.getStation();
		int stationID = station.getId();

		cManager.executeUpdate(updateTerminal, procedureCode, sessionCode, stationID, station.getIp().getHostAddress(), "Station");
		cManager.executeUpdate(updateSymmKey, procedureCode, sessionCode, stationID, RandStrGenerator.genSessionKey());

		for(Terminal post : terminals.getPosts()){
			int postID = post.getId();
			cManager.executeUpdate(updateTerminal, procedureCode, sessionCode, postID, post.getIp().getHostAddress(), "Post");
			cManager.executeUpdate(updateIsStationOf, procedureCode, sessionCode, stationID, postID);
			cManager.executeUpdate(updateSymmKey, procedureCode, sessionCode, postID, RandStrGenerator.genSessionKey());
		}

		for(Terminal subStation : terminals.getSubStations()){
			int subStationID = subStation.getId();
			cManager.executeUpdate(updateTerminal, procedureCode, sessionCode, subStationID, subStation.getIp().getHostAddress(), "SubStation");
			cManager.executeUpdate(updateIsStationOf, procedureCode, sessionCode, stationID, subStationID);
			cManager.executeUpdate(updateSymmKey, procedureCode, sessionCode, subStationID, RandStrGenerator.genSessionKey());
		}
	}
	
	/***************************************************
	**** FUNZIONI PER INSERIMENTO LISTE E CANDIDATI ****
	****************************************************/
	
	/**
	 * Recupera dalla procedura la lista dei candidati e delle liste elettorali. Per inserire i dati nel DB, per ciascuna
	 * lista elettorale invoca {@link #insertListInDB(int, int, String, ConnectionManager)}, per ciascun candidato invoca
	 * {@link #insertCandidateInDB(int, String, String, String, String, ConnectionManager)}.
	 * @param newProcedure Procedura da inserire nel DB
	 * @param cManager ConnectionManager per la connessione al DB
	 * @throws PEException Se riscontra errori nell'esecuzione delle query SQL
	 */
	private void insertListsAndCandidatesInDB(ProcedurePM newProcedure, ConnectionManager cManager) throws PEException {
		int procedureCode = newProcedure.getCode();
		ArrayList<ElectoralList> lists = newProcedure.getLists();
		ArrayList<Person> candidates = newProcedure.getCandidates();

		try {
			for(ElectoralList list : lists)
				insertListInDB(procedureCode, list.getCode(), list.getName(), cManager);

			for(Person candidate : candidates)
				insertCandidateInDB(procedureCode, candidate.getID(), candidate.getFirstName(), candidate.getLastName(), candidate.getBirth(), cManager);

		} catch (Exception e) {
			// In caso di errore ci limitiamo a lanciare una eccezione generica,
			// perchè a questo punto abbiamo già effettuato una verifica durante la scelta del file
			throw FLRException.FLR_0(e);
		}
	}
	
	/**
	 * Effettua l'inserimento di una nuova lista elettorale, per una data procedura, all'interno della tabella `ElectoralList`.
	 * @param procedureCode Codice della procedura
	 * @param listCode Codice della lista
	 * @param name Nome della lista
	 * @param cManager ConnectionManager per la connessione al DB
	 * @throws PEException Se riscontra errori nell'esecuzione delle query SQL, se riscontra errori nel recupero dei dati desiderati dal DB
	 */
	private void insertListInDB(int procedureCode, int listCode, String name, ConnectionManager cManager) throws PEException {
		String query = 	"SELECT * " +
						"FROM ElectoralList AS E " +
						"WHERE E.ProcedureCode = ? AND E.Code = ? ;";
		
		ResultSet rs = cManager.executeQuery(query, procedureCode, listCode);
		
		try {
			String update = rs.next() ? 
				"UPDATE ElectoralList SET Name = ? WHERE ProcedureCode = ? AND Code = ? ;" :
				"INSERT INTO ElectoralList(Name, ProcedureCode, Code) VALUES(?, ?, ?) ;";
			
			cManager.executeUpdate(update, name, procedureCode, listCode);
		} catch (SQLException e) {
			throw DBException.DB_0(e);
		}
	}

	/**
	 * Effettua l'inserimento di un nuovo candidato, per una data procedura, all'interno della tabella `Candidate`.
	 * @param procedureCode Codice della procedura
	 * @param ID ID del candidato
	 * @param firstName Nome del candidato
	 * @param lastName Cognome del candidato
	 * @param dateOfBirth Data di nascita del candidato (o null)
	 * @param cManager ConnectionManager per la connessione al DB
	 * @throws PEException Se riscontra errori nell'esecuzione delle query SQL
	 */
	private void insertCandidateInDB(int procedureCode, String ID, String firstName, String lastName, String dateOfBirth, ConnectionManager cManager) throws PEException {
		String update = "INSERT INTO Candidate(FirstName, LastName, DateOfBirth, ProcedureCode, ID) VALUES(?, ?, ?, ?, ?) ;";
		
		try {
			cManager.executeUpdate(update, firstName, lastName, FileUtils.parseDate(dateOfBirth), procedureCode, ID);
		} catch (Exception e) {
			// In caso di errore ci limitiamo a lanciare una eccezione generica,
			// perchè a questo punto abbiamo già effettuato una verifica durante la scelta del file
			throw FLRException.FLR_0(e);
		}
	}
	
	/****************************************
	**** FUNZIONI PER INSERIMENTO SCHEDE ****
	*****************************************/
	
	/**
	 * Recupera dalla procedura la lista delle schede elettorali. Per ciascuna scheda, 
	 * invoca {@link #insertBallotInDB(int, EmptyBallot, ConnectionManager)} per effettuare l'inserimento dei dati
	 * nel DB.
	 * @param newProcedure Procedura da inserire nel DB
	 * @param procedureBallots Lista di EmptyBallot, contenente le informazioni sulle schede
	 * @param cManager ConnectionManager per la connessione al DB
	 * @throws PEException Se riscontra errori nell'esecuzione delle query SQL
	 */
	private void insertBallotsInDB(ProcedurePM newProcedure, EmptyBallot[] procedureBallots, ConnectionManager cManager) throws PEException {
		int procedureCode = newProcedure.getCode();

		for(EmptyBallot ballot : procedureBallots){
			insertBallotInDB(procedureCode, ballot, cManager);
		}
	}
	
	/**
	 * Effettua l'inserimento della scheda, per una data procedura, all'interno della tabella `Ballot`. Inoltre, per ogni candidato
	 * sulla scheda stessa, inserisce la candidatura all'interno della tabella `Running`. Quindi, se il candidato appartiene ad
	 * una lista elettorale, inserisce l'informazione all'interno della tabella `Member`. Infine, se la scheda contiene opzioni
	 * di referendum (anzichè candidati), inserisce i dati all'interno della tabella `ReferendumOption`.
	 * @param procedureCode Codice della procedura
	 * @param ballot Oggetto EmptyBallot, contenente le informazioni sulla scheda
	 * @param cManager ConnectionManager per la connessione al DB
	 * @throws PEException Se riscontra errori nell'esecuzione delle query SQL
	 */
	private void insertBallotInDB(int procedureCode, EmptyBallot ballot, ConnectionManager cManager) throws PEException {
		String insertBallotUpdate = 	"INSERT INTO Ballot (ProcedureCode, Code, Name, Description, MaxPreferences) " +
										"VALUES (?, ?, ?, ?, ?) ;";
		String addCandidateToBallotUpdate = 	"INSERT INTO Running (ProcedureCode, BallotCode, CandidateID) " +
												"VALUES (?, ?, ?) ;";
		String addCandidateToListUpdate = 	"INSERT INTO Member (ProcedureCode, BallotCode, CandidateID, ElectoralListCode) " +
											"VALUES (?, ?, ?, ?) ;";
		String addOptionToBallotUpdate = 	"INSERT INTO ReferendumOption (ProcedureCode, BallotCode, Text) " +
											"VALUES (?, ?, ?) ;";

		int ballotCode = ballot.getCode();
		cManager.executeUpdate(insertBallotUpdate, procedureCode, ballotCode,
				ballot.getTitle(), ballot.getDescription(), ballot.getMaxPreferences());

		for(ElectoralList list : ballot.getLists()){
			int listCode = list.getCode();

			for(Person candidate : list.getCandidates()){
				String ID = candidate.getID();
				cManager.executeUpdate(addCandidateToBallotUpdate, procedureCode, ballotCode, ID);

				//Per distinguere dai candidati non appartenenti ad alcuna lista
				if(listCode != ProcedurePM.noListCode)
					cManager.executeUpdate(addCandidateToListUpdate, procedureCode, ballotCode, ID, listCode);
			}
		}

		for(String option : ballot.getOptions())
			cManager.executeUpdate(addOptionToBallotUpdate, procedureCode, ballotCode, option);
	}
	
	/*****************************************
	**** FUNZIONI PER INSERIMENTO VOTANTI ****
	******************************************/
	
	/**
	 * Recupera dalla procedura la lista dei votanti. Per ciascuno di questi, 
	 * invoca {@link #insertVoterInDB(String, String, String, int, int[], String, ConnectionManager)} per effettuare 
	 * l'inserimento dei dati nel DB.
	 * @param newProcedure Procedura da inserire nel DB
	 * @param cManager ConnectionManager per la connessione al DB
	 * @throws PEException Se riscontra errori nell'esecuzione delle query SQL
	 */
	private void insertVotersInDB(ProcedurePM newProcedure, ConnectionManager cManager) throws PEException {
		HashMap<String, Person> voters = newProcedure.getVoters();
		int procCode = newProcedure.getCode();

		try {
			for(String voterID : voters.keySet()) {
				Person voter = voters.get(voterID);
				insertVoterInDB(voter.getID(), voter.getFirstName(), voter.getLastName(), procCode, voter.getBallotCodes(), voter.getBirth(), cManager);
			}
		} catch (Exception e) {
			// In caso di errore ci limitiamo a lanciare una eccezione generica,
			// perchè a questo punto abbiamo già effettuato una verifica durante la scelta del file
			throw FLRException.FLR_0(e);
		}
	}
	
	/**
	 * Effettua l'inserimento dei votanti, per una data procedura, all'interno della tabella `Voter`. Inoltre, per ogni scheda
	 * su cui il votante è abilitato al voto, inserisce l'informazione all'interno della tabella `VoterBallotsList`. 
	 * @param ID ID del votante
	 * @param firstName Nome del votante
	 * @param lastName Cognome del votante
	 * @param procedureCode Codice della procedura
	 * @param ballotCodes Array contenente i codici delle schede su cui il votante è abilitato al voto
	 * @param dateOfBirth Data di nascita del votante, o null
	 * @param cManager ConnectionManager per la connessione al DB
	 * @throws PEException Se riscontra errori nell'esecuzione delle query SQL
	 */
	private void insertVoterInDB(String ID, String firstName, String lastName, int procedureCode, int[] ballotCodes, String dateOfBirth, ConnectionManager cManager) throws PEException {
		String voterExistenceQuery = 	"SELECT * " + 
										"FROM Voter AS V " +
										"WHERE V.ProcedureCode = ? " +
										"AND V.ID = ? ;";
		
		ResultSet rs = cManager.executeQuery(voterExistenceQuery, procedureCode, ID);
		
		try {
			String voterUpdate = rs.next() ?
					"UPDATE Voter SET FirstName = ?, LastName = ?, DateOfBirth = ? WHERE ProcedureCode = ? AND ID = ? ;" :
					"INSERT INTO Voter(FirstName, LastName, DateOfBirth, ProcedureCode, ID) VALUES(?, ?, ?, ?, ?) ;";
			
			String ballotsListUpdate = "INSERT INTO VoterBallotsList(VoterID, ProcedureCode, BallotCode) VALUES(?, ?, ?) ;";
			
			cManager.executeUpdate(voterUpdate, firstName, lastName, FileUtils.parseDate(dateOfBirth), procedureCode, ID);
		
			for(int ballotCode : ballotCodes) {
				cManager.executeUpdate(ballotsListUpdate, ID, procedureCode, ballotCode);
			}
		} catch(ParseException e) {
			throw FLRException.FLR_0(e);
		} catch(SQLException sqle) {
			throw DBException.DB_0(sqle);
		}
	}
	
	/**************************************************
	**** FUNZIONI PER LA CREAZIONE DI NUOVI UTENTI ****
	***************************************************/
	
	/**
	 * Interroga il DB per determinare se, all'interno della tabella `Staff`, un utente di tipo "Root" esista già o meno.
	 * @return True se l'utente di tipo "Root" esiste, false altrimenti
	 * @throws PEException Se riscontra errori nell'esecuzione delle query SQL, se riscontra errori nel recupero dei dati desiderati dal DB
	 */
	boolean existsRootUser() throws PEException  {
		final String query = 	"SELECT * " + 
								"FROM Staff " + 
								"WHERE Type = 'Root' ;";
		
		try (ConnectionManager cManager = dbms.getConnectionManager()) {
			ResultSet rs = cManager.executeQuery(query);
			return rs.next();
		}
		catch (SQLException e) {
			throw DBException.DB_0(e);
		}
	}
	
	/**
	 * Interroga il DB per determinare se, all'interno della tabella `Staff`, un certo username esista già o meno.
	 * @param username Username da controllare
	 * @return True se l'username esiste, false altrimenti
	 * @throws PEException Se riscontra errori nell'esecuzione delle query SQL, se riscontra errori nel recupero dei dati desiderati dal DB
	 */
	boolean existsUsername(String username) throws PEException {
		final String query = 	"SELECT * " + 
								"FROM Staff " + 
								"WHERE UserName = ? ;";
		
		try (ConnectionManager cManager = dbms.getConnectionManager()) {
			ResultSet rs = cManager.executeQuery(query, username);
			return rs.next();
		}
		catch (SQLException e) {
			throw DBException.DB_0(e);
		}
	}
	
	/**
	 * Effettua l'inserimento nel DB, all'interno della tabella `Staff`, di un nuovo utente.
	 * @param username Username del nuovo utente
	 * @param type Tipo del nuovo utente (Root/Technic)
	 * @param hashedPassword Array di byte contenente l'hash della password
	 * @return True se l'inserimento va a buon fine, false altrimenti
	 * @throws PEException Se riscontra errori nell'esecuzione delle query SQL
	 */
	boolean insertUser(String username, String type, byte[] hashedPassword) throws PEException {
		final String update = 	"INSERT INTO Staff(UserName, Type, HashedPassword) " + 
								"VALUES(?, ?, ?);";
		
		try (ConnectionManager cManager = dbms.getConnectionManager()) {
			int rowAdded = cManager.executeUpdate(update, username, type, hashedPassword);
			
			return rowAdded == 1;
		}
	}
	
	/**
	 * Effettua l'inserimento nel DB, all'interno della tabella `Staff`, di un nuovo supervisore.
	 * @param username Username del nuovo utente
	 * @param type Tipo del nuovo utente (Supervisor)
	 * @param hashedPassword Array di byte contenente l'hash della password
	 * @param publicKey1 Array di byte contenente la chiave pubblica 1 del supervisore
	 * @param encryptedPrivateKey1 Array di byte contenente la chiave privata 1 del supervisore, cifrata con la sua password
	 * @param publicKey2 Array di byte contenente la chiave pubblica 2 del supervisore
	 * @param encryptedPrivateKey2 Array di byte contenente la chiave privata 2 del supervisore, cifrata con la sua password
	 * @return True se l'inserimento va a buon fine, false altrimenti
	 * @throws PEException Se riscontra errori nell'esecuzione delle query SQL
	 */
	boolean insertSupervisor(String username, String type, byte[] hashedPassword, byte[] publicKey1, byte[] encryptedPrivateKey1, byte[] publicKey2, byte[] encryptedPrivateKey2) throws PEException {
		String update = 	"INSERT INTO Staff(UserName, Type, HashedPassword, PublicKey1, EncryptedPrivateKey1, PublicKey2, EncryptedPrivateKey2) " + 
								"VALUES(?, ?, ?, ?, ?, ?, ?);";
		
		try (ConnectionManager cManager = dbms.getConnectionManager()) {
			int rowAdded = cManager.executeUpdate(update, username, type, hashedPassword, 
					publicKey1, encryptedPrivateKey1, publicKey2, encryptedPrivateKey2);
			
			return rowAdded == 1;
		}
	}

	/****************************
	**** FUNZIONI DI UTILITY ****
	*****************************/	
	
	/**
	 * Verifica l'esistenza nel DB, all'interno della tabella `Staff`, di un supervisore con un dato username.
	 * @param username Username del supervisore
	 * @param cManager ConnectionManager per la connessione al DB 
	 * @return True se il supervisore esiste, false altrimenti
	 * @throws PEException Se riscontra errori nell'esecuzione delle query SQL, se riscontra errori nel recupero dei dati desiderati dal DB
	 */
	private boolean existSupervisor(String username, ConnectionManager cManager) throws PEException {
		String query = 	"SELECT * " +
				 		"FROM Staff " + 
				 		"WHERE UserName = ? " + 
				 		"AND Type = 'Supervisor' ;";
		boolean exist;
		
		try {
			ResultSet rs = cManager.executeQuery(query, username);
			exist = rs.next();
		} catch (SQLException e) {
			throw DBException.DB_03(e);
		}
		
		return exist;
	}
	
	/**
	 * Interroga il DB per recuperare, dalla tabella `Staff`, il tipo di utente associato ad un certo username.
	 * @param user Username dell'utente
	 * @return Il tipo dell'utente (Root/Technic/Supervisor)
	 * @throws PEException Se riscontra errori nell'esecuzione delle query SQL, se riscontra errori nel recupero dei dati desiderati dal DB
	 */
	String getUserType(String user) throws PEException {
		String query = "SELECT * FROM Staff WHERE UserName = ? ;";
		
		try (ConnectionManager cm = dbms.getConnectionManager()) {
			ResultSet rs = cm.executeQuery(query, user);
			
			if (rs.next())
				return rs.getString("Type");
		} catch (SQLException e) {
			throw DBException.DB_0(e);
		}
		
		return null;
	}
	
	/**
	 * Interroga il DB per recuperare, dalla tabella `Staff`, tutti i supervisori esistenti.
	 * @return Lista degli username dei supervisori
	 * @throws PEException Se riscontra errori nell'esecuzione delle query SQL, se riscontra errori nel recupero dei dati desiderati dal DB
	 */
	ArrayList<String> getAllSupervisors() throws PEException {
		ArrayList<String> usernames = new ArrayList<>();
		
		final String query = 	"SELECT * " + 
								"FROM Staff " + 
								"WHERE Type = 'Supervisor' ;";
		
		try (ConnectionManager cManager = dbms.getConnectionManager()) {
			ResultSet rs = cManager.executeQuery(query);
			
			while(rs.next()) {
				usernames.add(rs.getString("UserName"));
			}
		}
		catch (SQLException e) {
			throw DBException.DB_03(e);
		}
		
		return usernames;
	}
}
