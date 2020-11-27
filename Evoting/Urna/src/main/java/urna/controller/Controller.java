package urna.controller;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;

import controller.AbstrServer;
import controller.TerminalController;
import encryption.AES;
import encryption.HMAC;
import encryption.NonceManager;
import encryption.VoteEncryption;
import exceptions.DBException;
import exceptions.DEVException;
import exceptions.ENCException;
import exceptions.FLRException;
import exceptions.PEException;
import controller.Server;
import model.EmptyBallot;
import model.Message;
import model.Person;
import model.Session;
import model.Terminals;
import model.VotePacket;
import model.WrittenBallot;
import model.State.StateUrn;
import urna.model.DummyTerminal;
import urna.model.Urn;
import utils.CfgManager;
import utils.Constants;
import utils.Protocol;
import view.ViewInterface;

public class Controller extends TerminalController {
	protected Urn urn = null;
	private UrnDB db = null;
	private final int maxSearchResults = 50;

	/**
	 * Costruttore adoperato per il testing.
	 * @param server 	Il server fittizio (classe TestServer)
	 * @param view 		La classe che gestisce la GUI fittiziamente.
	 * @param urn	 	Classe model.
	 */
	public Controller(AbstrServer server, ViewInterface view, Urn urn, UrnDB db) {
		super(server, view, Terminals.Type.Urn, false);
		this.urn = urn;
		this.db = db;
	}

	/**
	 * Costruttore reale.
	 * @param view 		La classe che gestisce la GUI.
	 * @param urn 		Classe model.
	 */
	public Controller(ViewInterface view, Urn urn, UrnDB db) throws PEException {
		super(new Server(new Factory(), urn.getPort(), urn.getNumConnections(), Terminals.Type.Urn), view, Terminals.Type.Urn, false);
		this.urn = urn;
		this.db = db;
		
		if(!Constants.linkSSL && Constants.dbSSL) {
			System.setProperty("javax.net.ssl.keyStore", "ssl/keystore.jks");
			System.setProperty("javax.net.ssl.keyStorePassword", CfgManager.getPassword("ks"));
			
			System.setProperty("javax.net.ssl.trustStore", "ssl/truststore.jks");
			System.setProperty("javax.net.ssl.trustStorePassword", CfgManager.getPassword("ts"));
		}
	}
	
	/* --- Inizializzazione dell'urna --- */

	private boolean startSession(int procedureCode, int sessionCode) throws PEException {
		byte[] pub1 = db.getRSAKey("PublicKey1", urn.getUsername(), urn.getPassword());
		byte[] pr2 = db.getRSAKey("EncryptedPrivateKey2", urn.getUsername(), urn.getPassword());

		EmptyBallot[] procedureBallots = db.getEmptyBallots(procedureCode);
		urn.setSessionParameters(procedureCode, sessionCode, pub1, pr2);
		urn.setProcedureBallots(procedureBallots);
		urn.setEligibleVoters(db.getNumOfEligibleVoters(procedureCode));
		urn.setHasVoted(db.getNumOfVoted(procedureCode));

		return true;
	}
	
	/* --- Funzioni richiamate dai controller delle scene JavaFX --- */

	public void checkLogin(String user, String psw) {
		if(checkLoginData(urn, db, user, psw)) {
			urn.setState(StateUrn.ATTIVA);
    		updateView();
		}
	}
	
	public void logout() {
		if(confirmLogout(urn)) {
			signalUrnReset();
			urn.setState(StateUrn.NON_ATTIVA);
			
    		updateView();
		}
	}

	public void closeSession() {
		if(printConfirmation("Sei sicuro di voler effettuare chiudere la sessione?", "Verrai riportato alla schermata di scelta della sessione")) {
    		signalUrnReset();
			urn.setState(StateUrn.ATTIVA);
			
			urn.logWarning("Sessione [ID: "+urn.getSessionCode()+"] appartenente alla procedura [ID: "+urn.getProcedureCode()+"] terminata.");
    		updateView();
    	}
	}
	
	public void showStats() {
		ArrayList<DummyTerminal> terminals = urn.getOnlineTerminals();
		int numTerminals = terminals.size(), numPosts = 0, numStats = 0, numAuxStats = 0;

		for (DummyTerminal t : terminals) {
			switch(t.getType()) {
				case Post:
					numPosts++;
					break;
					
				case Station:
					numStats++;
					break;
					
				case SubStation:
					numAuxStats++;
					break;
					
				default:
					//Do nothing
			}
		}

		String stats = "Terminali autenticati: "+numTerminals+", di cui:\n\tSeggi: "+numStats+"\n\tSeggi Ausiliari: "+numAuxStats+"\n\tPostazioni: "+numPosts;
		stats += "\n\nStatistiche Votanti:\n\tAbilitati: "+ urn.getEligibleVoters()+"\n\tHanno Votato: "+ urn.getHasVoted();

		printSuccess("Statistiche Urn", stats);
	}

	public ArrayList<Session> getSessions() {
		try {
			urn.setSessions(db.getSessions(urn.getUsername()));
		} catch (PEException e) {
			printError(e);
		}

		return urn.getSessions();
	}
	
	public void confirmSession(int procCode, int sessionCode) {
		if (procCode == -1 || sessionCode == -1) {
			printError("Seleziona una Sessione", "Assicurati di aver selezionato una sessione prima di confermare");
			return;
		}

		Session s = urn.getSession(procCode, sessionCode);

		if (s == null) {
			printError("Seleziona una Sessione", "Nessuna sessione trovata col codice selezionato. Riprova o contatta un amministratore");
			return;
		}

		if (!s.getValidity() && !Constants.devMode) {
			printError("Sessione Non Valida", "Impossibile procedere con l'attivazione di una sessione non valida.");
			return;
		}

		try {
			if (startSession(procCode, sessionCode)) {
				urn.setState(StateUrn.LOGGING);

				if(Constants.verbose)
					printSuccess("Sessione Avviata Correttamente", "E' ora possibile comunicare con l'urna");
				urn.logSuccess("Sessione [ID: "+sessionCode+"] appartenente alla procedura [ID: "+procCode+"] avviata.");

				updateView();
			}
			else
				printError("Errore", "Impossibile avviare la sessione selezionata. Riprova o contatta un amministratore");

		} catch (PEException e) {
			printError(e);
		}
	}
	
	public void deactivateTerminal(Terminals.Type type) {
		ArrayList<DummyTerminal> onlineTerminals = urn.getOnlineTerminals();
		Iterator<DummyTerminal> it = onlineTerminals.iterator();
		
		while(it.hasNext()) {
			DummyTerminal t = it.next();
			if(t.getType() == type)
				it.remove();
		}
	}

	public ArrayList<String> getLogs() {
		return urn.getLogs();
	}

	/* --- Funzioni richiamate direttamente da Service --- */

	int getProcedureCode() {
		return urn.getProcedureCode();
	}

	int getSessionCode() {
		return urn.getSessionCode();
	}
	
	boolean verifyIp(InetAddress ip, Terminals.Type type, String msg) {
		for (DummyTerminal t : urn.getOnlineTerminals()) {
			if(t.getIp().equals(ip) && t.getType() == type)
				return true;
		}
		
		printError("Comunicazione Inattesa", "Tentativo di connessione da parte di " + ip.getHostAddress() + "(sconosciuto) come "+type+".");
		
		urn.logWarning("Tentativo di connessione all'urna da parte di un terminale sconosciuto [IP: "+ip.getHostAddress()+", Tipo: "+type+"]. Messaggio ricevuto: "+msg+".");
		updateView();
		
		return false;
	}

	// Risposta alla richiesta di autenticazione dell'urna da parte di un terminale (Fase 1 dell'autenticazione mutua fra urna e terminali)
	Message authenticateToTerminal(InetAddress ip, String encryptedNonce, Terminals.Type type) {
		urn.logInfo("Il terminale [IP: "+ip.getHostAddress()+", Tipo: "+type+"] richiede all'urna di autenticarsi.");

		Message bulkOut = new Message();
		try {
			String sessionKey = db.getTerminalSessionKey(urn.getProcedureCode(), urn.getSessionCode(), ip, type);
			String encryptedModifiedNonce1 = NonceManager.solveChallenge(encryptedNonce, sessionKey, 1);

			bulkOut.setElement("nonce1", encryptedModifiedNonce1);

			int nonce2 = NonceManager.genSingleNonce();
			String encryptedNonce2 = AES.encryptNonce(nonce2, sessionKey);

			urn.setActivationNonce(ip, type, nonce2);

			bulkOut.setElement("nonce2", encryptedNonce2);
			bulkOut.setElement("response", Protocol.PostAuthenticationPhase1);
			bulkOut.setValue(Protocol.validAuthentication);

			urn.logInfo("L'urna ha risposto alla sfida lanciata dal terminale [IP: "+ip.getHostAddress()+", Tipo: "+type+"] e ha lanciato una nuova sfida.");

		} catch(PEException e) {
			bulkOut.setElement("response", Protocol.authenticationFailed);
			bulkOut.setElement("error", e.getMessage());
			bulkOut.setValue(Protocol.authenticationFailed);
			bulkOut.addError(e.getMessage());

			urn.logError("Errore durante la comunicazione col terminale [IP: "+ip.getHostAddress()+", Tipo: "+type+"]: "+e.getMessage());
		}

		updateView();
		return bulkOut;
	}

	//Risposta alla richiesta di autenticazione di un terminale presso l'urna (Fase 2 dell'autenticazione mutua fra urna e terminali)
	Message verifyTerminalAuthentication(InetAddress ip, String encryptedNonce, Terminals.Type type) {
		urn.logInfo("Il terminale [IP: "+ip.getHostAddress()+", Tipo: "+type+"] ha autenticato l'urna e ha risposto alla sua sfida per autenticarsi.");

		Message bulkOut = new Message();
		try {
			if(verifyAuthentication(ip, encryptedNonce, type)) {
				switch(type) {
					case Post:
						byte[] pubKey = urn.getPublicKey1();
						EmptyBallot[] ballots = urn.getProcedureBallots();

						bulkOut.setElement("pubKey", pubKey);
						bulkOut.setElement("ballots", ballots);
						//Il break NON ci deve essere

					case SubStation:
						String stringIp = db.getStationIP(urn.getProcedureCode(), urn.getSessionCode(), ip, true);
						InetAddress ipStation;
						try {
							ipStation = InetAddress.getByName(stringIp);
						} catch (UnknownHostException e) {
							throw DBException.DB_04(stringIp);
						}

						bulkOut.setElement("ipStation", ipStation);
						bulkOut.setValue(Protocol.validAuthentication);

						urn.logSuccess("L'autenticazione del terminale [IP: "+ip.getHostAddress()+", Tipo: "+type+"] è andata a buon fine.");
						urn.addOnlineTerminal(ip, type);
						break;

					case Station:
						ArrayList<InetAddress> vectorIpPosts = new ArrayList<>();
						ArrayList<InetAddress> vectorIpSubStations = new ArrayList<>();
						db.getTerminalsIPs(urn.getProcedureCode(), urn.getSessionCode(), ip, vectorIpPosts, vectorIpSubStations);

						InetAddress[] ipPosts = new InetAddress[vectorIpPosts.size()];
						int i = 0;
						for(InetAddress ipPost : vectorIpPosts) {
							ipPosts[i] = ipPost;
							i++;
						}

						InetAddress[] ipSubStations = new InetAddress[vectorIpSubStations.size()];
						i = 0;
						for(InetAddress ipSubStation : vectorIpSubStations) {
							ipSubStations[i] = ipSubStation;
							i++;
						}

						bulkOut.setElement("posts", ipPosts);
						bulkOut.setElement("subStations", ipSubStations);
						bulkOut.setElement("ballots", urn.getProcedureBallots()); //Aggiunto per registrazione nuovi utenti al seggio
						bulkOut.setValue(Protocol.validAuthentication);

						urn.logSuccess("L'autenticazione del terminale [IP: "+ip.getHostAddress()+", Tipo: "+type+"] è andata a buon fine.");
						urn.addOnlineTerminal(ip, type);
						break;

					default:
						bulkOut.setValue(Protocol.authenticationFailed);
						bulkOut.setElement("response", Protocol.authenticationFailed);
						bulkOut.addError("Richiesta attivazione da parte di un terminale di tipo sconosciuto: " + type);
						bulkOut.setElement("error", "Richiesta attivazione da parte di un terminale di tipo sconosciuto: " + type);
						urn.logError("L'autenticazione del terminale [IP: "+ip.getHostAddress()+", Tipo: "+type+"] NON è andata a buon fine: il terminale è sconosciuto.");
				}

			}
			else {
				bulkOut.setValue(Protocol.authenticationFailed);
				bulkOut.addError("Nonce errato.");
				bulkOut.setElement("error", "Nonce errato.");
				urn.logError("L'autenticazione del terminale [IP: "+ip.getHostAddress()+", Tipo: "+type+"] NON è andata a buon fine: Nonce errato.");
			}

		}
		catch (PEException e) {
			bulkOut.setValue(Protocol.authenticationFailed);
			bulkOut.addError(e.getMessage());
			urn.logError("L'autenticazione del terminale [IP: "+ip.getHostAddress()+", Tipo: "+type+"] NON è andata a buon fine: "+e.getMessage());
		}

		updateView();
		return bulkOut;
	}
	
	Message checkTerminalAuthenticated(InetAddress ip, Terminals.Type type) {
		if(Constants.verbose)
			urn.logInfo("Ricevuta richiesta di verifica autenticazione dal terminale [IP: "+ip.getHostAddress()+", Tipo: "+type+"].");
		
		Message response = new Message();
		boolean terminalFound = false;
		for(DummyTerminal t : urn.getOnlineTerminals())
			if(t.getIp().equals(ip) && t.getType() == type) {
				terminalFound = true;
				break;
			}

		response.setValue(terminalFound ? Protocol.authenticatedAck : Protocol.authenticatedNack);
		if(!terminalFound)
			urn.logWarning("Ricevuta richiesta di verifica autenticazione da un terminale sconosciuto [IP: "+ip.getHostAddress()+", Tipo: "+type+"].");
		
		updateView();
		return response;
	}

	//Risposta alla richiesta di ricerca votanti
	Message searchPerson(InetAddress ip, Terminals.Type terminal, String similarFirstName, String similarLastName) {
		urn.logInfo("Il terminale [IP: "+ip.getHostAddress()+", Tipo: "+terminal+"] ha richiesto di cercare votanti.");
		
		Message response = new Message();
		try {
			ArrayList<Person> votersList = db.searchPerson(urn.getProcedureCode(), similarFirstName, similarLastName, maxSearchResults);
			boolean missingVoters = votersList.size() > maxSearchResults;
			
			if(missingVoters)
				votersList = new ArrayList<>();
			
			Person[] voters = new Person[votersList.size()];
			
			for (int i = 0; i < voters.length; i++)
				voters[i] = votersList.get(i);
	
			response.setValue(Protocol.searchPersonAck);
			response.setElement("voters", voters);
			response.setElement("missingVoters", missingVoters);
			
			if (missingVoters)
				urn.logWarning("Restituiti 0 risultati al terminale [IP: "+ip.getHostAddress()+", Tipo: "+terminal+"]: Criteri troppo poco stringenti");
			
			if (!missingVoters && voters.length == 0)
				urn.logWarning("Restituiti 0 risultati al terminale [IP: "+ip.getHostAddress()+", Tipo: "+terminal+"]: Nessun risultato trovato");
	
			if (!missingVoters && voters.length > 0)
				urn.logSuccess("Restituiti "+voters.length+" risultati al terminale [IP: "+ip.getHostAddress()+", Tipo: "+terminal+"].");
			
		} catch(PEException e) {
			e.printStackTrace();
			
			response.setValue(Protocol.searchPersonNack);
			response.addError(e.getMessage());
		}

		updateView();
		return response;
	}

	//Risposta alla richiesta di registrazione di un nuovo utente
	Message registerUser(InetAddress ip, String id, String ln, String fn, String birthDate, int[] ballots) {
		urn.logInfo("Ricevuta richiesta di registrazione di un nuovo utente [ID: "+id+"] dal seggio [IP: "+ip.getHostAddress()+"].");

		//Si crea il messaggio di risposta, inizialmente vuoto
		Message response = new Message();

		try {
			//Si verifica se esiste un utente con l'ID indicato
			Person voter = db.getVoter(urn.getProcedureCode(), id);

			if(voter != null)
				throw DBException.DB_14(voter.getID());
			
			//Se il votante non esiste si aggiunge al db e si abilita e si segnala il successo dell'operazione al seggio
			db.registerNewVoter(urn.getProcedureCode(), ip, id, ln, fn, birthDate, ballots);
			response.setValue(Protocol.registerNewUserAck);
			
			urn.logSuccess("La registrazione dell'utente [ID: "+id+"], richiesta dal seggio [IP: "+ip.getHostAddress()+"], è andata a buon fine.");
		}
		catch(PEException e) {
			response.setValue(Protocol.registerNewUserNack);
			response.addError(e.getSpecific());
			
			urn.logError("La registrazione dell'utente [ID: "+id+"], richiesta dal seggio [IP: "+ip.getHostAddress()+"], NON è andata a buon fine: "+e.getMessage());
		}

		updateView();
		return response;
	}

	//Risposta alla richiesta di registrazione di un nuovo utente
	Message updateExistingUser(InetAddress ip, String id, int[] ballots) {
		urn.logInfo("Ricevuta richiesta di aggiornamento di un utente [ID: "+id+"] dal seggio [IP: "+ip.getHostAddress()+"].");

		//Si crea il messaggio e si indica che è una risposta alla registrazione di un utente esistente
		Message response = new Message();

		try{
			Person voter = db.getVoter(urn.getProcedureCode(), id);
			
			if (voter == null)
				throw DBException.DB_15(id, false);
			
			db.updateExistingVoter(urn.getProcedureCode(), ip, id, ballots);
			response.setValue(Protocol.updateExistingUserAck);
			response.setElement("voter", voter);
			
			urn.logSuccess("L'aggiornamento dell'utente [ID: "+id+"], richiesto dal seggio [IP: "+ip.getHostAddress()+"], è andato a buon fine.");
		} catch (PEException e) {
			response.setValue(Protocol.updateExistingUserNack);
			response.setElement("voter", null);
			response.addError(e.getSpecific());
			
			urn.logError("L'aggiornamento dell'utente [ID: "+id+"], richiesto dal seggio [IP: "+ip.getHostAddress()+"], NON è andato a buon fine: "+e.getMessage());
		}
		
		updateView();
		return response;
	}

	//Risposta alla richiesta di generazione di nonce per l'invio di voti
	String[][] genNonces(InetAddress ipPost, int[] structure) throws PEException {
		urn.logInfo("La postazione [IP: "+ipPost+"] ha richiesto di generare nuovi nonce per l'invio di voti cifrati.");

		String sessionKey = db.getTerminalSessionKey(urn.getProcedureCode(), urn.getSessionCode(), ipPost, Terminals.Type.Post);
		ArrayList<ArrayList<Integer>> voteNonces = NonceManager.genMultipleNonces(structure);
		String[][] encryptedNonces = NonceManager.encryptMultipleNonces(voteNonces, sessionKey);
		urn.setVoteNonces(ipPost, voteNonces);

		urn.logInfo("Gli nonce richiesti dalla postazione [IP: "+ipPost+"] sono stati correttamente generati ed inviati.");

		updateView();
		return encryptedNonces;
	}

	//Risposta all'invio di voti da parte di un seggio
	Message voteReceived(Person voter, WrittenBallot[] encryptedBallots, InetAddress ipStation, InetAddress ipPost) {
		urn.logInfo("Ricevuti voti cifrati dal seggio [IP: "+ipStation+"], provenienti dalla postazione [IP: "+ipPost+"] e relativi al votante [ID: "+voter.getID()+"]");

		String voterID = voter.getID();
		int procedureCode = urn.getProcedureCode(), sessionCode = urn.getSessionCode();

		Message response = new Message();

		if(encryptedBallots == null || ipStation == null || ipPost == null) {
			response.setValue(Protocol.votesReceivedNack);
			response.addError("Non sono stati ricevuti tutti i dati necessari.");

			urn.logError("I voti ricevuti dal seggio [IP: "+ipStation+"], provenienti dalla postazione [IP: "+ipPost+"] e relativi al votante [ID: "+voter.getID()+"] non contengono tutti i dati necessari.");
		}
		else
			synchronized(this) {
				try {
					db.verifyVoteData(procedureCode, sessionCode, voterID, encryptedBallots, ipStation.getHostAddress(), ipPost.getHostAddress());
					String sessionKey = db.getTerminalSessionKey(procedureCode, sessionCode, ipPost, Terminals.Type.Post);

					if(!verifyNonces(encryptedBallots, ipPost, sessionKey)){
						response.setValue(Protocol.votesReceivedNack);
						String error = "La verifica degli nonce delle schede di voto non ha avuto successo";
						response.addError(error);

						urn.logError("I voti ricevuti dal seggio [IP: "+ipStation+"], provenienti dalla postazione [IP: "+ipPost+"] e relativi al votante [ID: "+voter.getID()+"] non sono stati memorizzati: "+ error);
						updateView();
						return response;
					}

					if(verifyBallotsHMAC(encryptedBallots, ipPost, sessionKey)) {
						signBallots(encryptedBallots);
						db.storeVotes(procedureCode, sessionCode, voter, encryptedBallots, ipStation, ipPost);
						response.setValue(Protocol.votesReceivedAck);
						urn.increaseHasVoted();

						urn.logSuccess("I voti ricevuti dal seggio [IP: "+ipStation+"], provenienti dalla postazione [IP: "+ipPost+"] e relativi al votante [ID: "+voter.getID()+"] sono stati correttamente memorizzati.");
					}
					else {
						response.setValue(Protocol.votesReceivedNack);
						response.addError("La firma di una o più schede di voto non è corretta.");

						urn.logError("Uno o voti ricevuti dal seggio [IP: "+ipStation+"], provenienti dalla postazione [IP: "+ipPost+"] e relativi al votante [ID: "+voter.getID()+"] contengono firme errate.");
					}
				}
				catch (PEException e) {
					response.setValue(Protocol.votesReceivedNack);
					response.addError(e.getMessage());

					urn.logError("I voti ricevuti dal seggio [IP: "+ipStation+"], provenienti dalla postazione [IP: "+ipPost+"] e relativi al votante [ID: "+voter.getID()+"] non sono stati memorizzati: "+e.getMessage());
				}
			}

		updateView();
		return response;
	}
	
	void logWarning(String log) {
		urn.logWarning(log);
		updateView();
	}

	//Ricezione dello shutdown di un terminale e conseguente eliminazione dello stesso dalla lista di terminali attivi ed autenticati con l'urna
	synchronized void logShutDown(InetAddress ip, Terminals.Type type) {
		ArrayList<DummyTerminal> terminals = urn.getOnlineTerminals();
		Iterator<DummyTerminal> it = terminals.iterator();

		while (it.hasNext()) {
			DummyTerminal t = it.next();
			if(t.getIp().equals(ip) && t.getType() == type) {
				urn.logWarning("Il terminale [IP: "+ip+", Tipo: "+type+"] è tornato OFFLINE.");
				it.remove();

				updateView();
				return;
			}
		}
	}

	/* --- Funzioni di utility --- */

	//Verifica che l'nonce ricevuto sia quello atteso
	private boolean verifyAuthentication(InetAddress ip, String encryptedNonce, Terminals.Type type) throws PEException {
		Integer storedNonce = urn.getActivationNonce(ip, type);

		if(storedNonce == null) {
			throw FLRException.FLR_02(type, ip);
		}

		String sessionKey = db.getTerminalSessionKey(urn.getProcedureCode(), urn.getSessionCode(), ip, type);

		if(sessionKey == null) {
			throw DEVException.DEV_03("la chiave di sessione", type, ip);
		}

		return NonceManager.verifyChallenge(storedNonce, encryptedNonce, sessionKey, 2);
	}

	//Verifica gli nonce contenuti nei pacchetti di voto
	private boolean verifyNonces(WrittenBallot[] encryptedBallots, InetAddress ipPost, String sessionKey) throws PEException{
		ArrayList<ArrayList<Integer>> voteNonces = urn.getVoteNonces(ipPost);

		if(voteNonces == null) {
			throw FLRException.FLR_02(Terminals.Type.Post, ipPost);
		}

		try {
			if(voteNonces.size() != encryptedBallots.length) {
				throw FLRException.FLR_03(1);
			}

			for(int i = 0; i < voteNonces.size(); i++) {

				ArrayList<Integer> ballotNonces = voteNonces.get(i);
				ArrayList<String> solvedNonces = encryptedBallots[i].getSolvedNonces();
				try {
					if(!NonceManager.verifyMultipleNonces(ballotNonces, solvedNonces, sessionKey)) {
						return false;
					}
				}
				catch(PEException e) {
					throw ENCException.ENC_6(e);
				}
			}
		}
		catch (NullPointerException e) {
			throw FLRException.FLR_03(1);
		}

		return true;
	}

	//Firma i pacchetti di voto con la chiave privata 2 del responsabile
	private void signBallots(WrittenBallot[] encryptedBallots) throws PEException {
		for(WrittenBallot ballot : encryptedBallots) {
			for(VotePacket packet : ballot.getEncryptedVotePackets()) {
				VoteEncryption.signPacket(packet, urn.getPrivateKey2());
			}
		}

	}

	//Verifica l'HMAC appeso ai pacchetti di voto
	private boolean verifyBallotsHMAC(WrittenBallot[] encryptedBallots, InetAddress ipPost, String sessionKey) throws PEException {
		for(WrittenBallot ballot : encryptedBallots) {
			ArrayList<VotePacket> votePackets = ballot.getEncryptedVotePackets();

			for(VotePacket packet : votePackets) {
				if(!HMAC.verify(packet, sessionKey)) {
					return false;
				}
			}
		}
		return true;
	}
	
	private void signalUrnReset() {
		ArrayList<DummyTerminal> onlineTerminals = urn.getOnlineTerminals();
		
		if(onlineTerminals != null)
			for (DummyTerminal t : onlineTerminals)
				signalShutDown(t.getIp(), t.getPort());
		
		urn.resetOnlineTerminals();
	}

	/* --- Funzioni da implementare in quanto TerminalController --- */
	
	@Override
	public String readCard(String card) {
		return null;
	}
	
	@Override
	protected void invalidAuthentication() {
		return;
	}
	
	@Override
	protected boolean verifyUrnIp(InetAddress ip) { 
		return true; 
	}

	@Override
	protected boolean beforeStartOps() {
		urn.logInfo("L'urna è stata avviata.");
		return true;
	}

	@Override
	protected void afterClosureOps(){
		signalUrnReset();
		urn.logInfo("L'urna è stata spenta.");
	}
	
	@Override
	public void setRfidReachable(boolean reachable) {
		//
	}
}
