package seggio.controller;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

import controller.TerminalController;
import encryption.AES;
import encryption.NonceManager;
import exceptions.CNNException;
import exceptions.DBException;
import exceptions.DEVException;
import exceptions.ENCException;
import exceptions.FLRException;
import exceptions.PEException;
import controller.AbstrServer;
import controller.CardReader;
import controller.UrnPolling;
import controller.Link;
import controller.Server;
import model.DummyPost;
import model.EmptyBallot;
import model.Message;
import model.Person;
import model.State;
import model.State.StatePost;
import model.State.StateStation;
import model.Terminals;
import model.WrittenBallot;
import seggio.model.Station;
import utils.CfgManager;
import utils.Constants;
import utils.FileUtils;
import utils.Protocol;
import view.ViewInterface;
import view.viewmodel.VoterViewModel;

public class Controller extends TerminalController {
	private final Station station;
	private PostsPolling postsThread;
	
	/**
	 * Costruttore adoperato per il testing.
	 * @param server 	Il server fittizio (classe TestServer)
	 * @param view 		La classe che gestisce la GUI fittiziamente.
	 * @param station 	Classe model.
	 */
	public Controller(AbstrServer server, ViewInterface view, Station station) {
		super(server, view, Terminals.Type.Station, false);
		this.station = station;
	}
	
	/**
	 * Costruttore reale.
	 * @param view 		La classe che gestisce la GUI.
	 * @param station 	Classe model.
	 */
	public Controller(ViewInterface view, Station station) throws PEException {
		super(new Server(new Factory(), Constants.portStation, station.getNumConnections(), Terminals.Type.Station), view, Terminals.Type.Station, Constants.statRfid);
		this.station = station;
		
		if(!Constants.devMode)
			station.setUrnIp(CfgManager.getValue("ips.cfg", "urn"));
	}
	
	/* --- Inizializzazione del Seggio --- */
	
	/**
	 * Funzione che tenta di portare a termine la mutua autenticazione con l'urna.
	 * @param sessionKey La chiave di sessione inserita per creare e superare le sfide basate su nonce.
	 */
	public void activate(String sessionKey) {
		//Se il seggio è già stato attivato allora questa funzione non deve essere eseguita
		if(!correctState(StateStation.NON_ATTIVO)) return;
		
		//Si verifica che la chiave di sessione abbia formato corretto (32 caratteri alfanumerici solo maiuscoli)
		if(!FileUtils.isSessionKey(sessionKey)) {
			printError("Chiave di Sessione Errata!", "La chiave di sessione inserita non è una chiave valida.");
			return;
		}
		
		try {
			//Si effettua la prima fase della mutua autenticazione
			String encryptedNonce2 = askForUrnAuthentication(sessionKey);
			
			//Se la prima fase ha restituito un nonce cifrato ...
			if(encryptedNonce2 != null) {
				// ... si procede a risolverlo e ad inviare la soluzione all'urna, che restituisce un messaggio
				Message urnResponse = authenticateToUrn(encryptedNonce2, sessionKey);
				
				//Si verifica che il messaggio sia valido e contenga i campi necessari
				String[] required = {"posts", "subStations", "ballots"};
				Class<?>[] types = {InetAddress[].class, InetAddress[].class, EmptyBallot[].class};
				urnResponse.verifyMessage(Protocol.validAuthentication, required, types, "urna");
				
				//Vengono recuperati tutti i dati necessari inviati dall'urna
				InetAddress[] ipPosts = urnResponse.getElement("posts");
				InetAddress[] ipSubStations = urnResponse.getElement("subStations");
				EmptyBallot[] ballots = urnResponse.getElement("ballots");
				
				station.init(ipPosts, ipSubStations);
				station.setSessionKey(sessionKey); 
				station.setEmptyBallots(ballots);
				
				if(Constants.verbose)
					printSuccess("Attivazione Completata", "Seggio attivato");
				
				updateViewAndSubstation();	
				
				//Infine si notifica alle postazioni l'attivazione del seggio perché queste rispondano con il proprio stato
				askStateToPosts();
				
				urnThread = new UrnPolling(station.getUrnIp(), this, Terminals.Type.Station);
				urnThread.start();
				
				postsThread = new PostsPolling(this, 1000);
				postsThread.start();
				
				if(Constants.statRfid && rfidReader != null) {
					rfidReader.setAlwaysOn();
				}
			}
		}
		catch (PEException e) {
			printError(e);
		}
	}

	/**
	 * Prima fase della mutua autenticazione con l'urna.
	 * Se dovesse avere successo verrà restituito il nonce cifrato inviato dall'urna per il superamento della sfida.
	 * @param sessionKey Chiave di sessione.
	 * @return Il nonce cifrato inviato dall'urna o null.
	 */
	private String askForUrnAuthentication(String sessionKey) throws PEException {
		//Viene generato il nonce
		int nonce1 = NonceManager.genSingleNonce();
		//Il nonce viene cifrato
		String encryptedNonce1 = AES.encryptNonce(nonce1, sessionKey);
		
		//Si avvia la connessione all'urna
		Message response;
		String errMsg = "Attivazione fallita, impossibile contattare l'urna. Controllare che sia l'urna che questo seggio siano connessi alla rete.";
		String ipRecipient = station.getUrnIp().getHostAddress();
		try (Link link = createLink(station.getUrnIp(), station.getUrnPort(), null, errMsg)) {
			if(link.isClosed()) {return null;}
			
			//Indichiamo di volere iniziare la mutua autenticazione
			link.write(Protocol.StationAuthenticationPhase1);
			//Inviamo il nostro nonce cifrato
			link.write(encryptedNonce1);
			
			if(!link.hasNextLine()) {
				throw CNNException.CNN_3("urna", ipRecipient);
			}
			
			//L'urna invia il messaggio di risposta
			response = (Message) Message.fromB64(link.read(), "urna");
		}
		catch (Exception e) {
			throw CNNException.CNN_1("urna", ipRecipient, e);
		}
		
		//Si verifica che il messaggio sia corretto e che abbia i campi richiesti
		String[] required = {"nonce1", "nonce2"};
		Class<?>[] types = {String.class, String.class};
		response.verifyMessage(Protocol.validAuthentication, required, types, "urna");
		
		//Si leggono gli nonce cifrati
		//Il primo è la risposta alla sfida lanciata all'urna
		String encryptedModifiedNonce1 = response.getElement("nonce1");
		//Il secondo è la sfida lanciata dall'urna
		String encryptedNonce2 = response.getElement("nonce2");

		//Si verifica che l'urna abbia superato la sfida
		try {
			if(!NonceManager.verifyChallenge(nonce1, encryptedModifiedNonce1, sessionKey, 1)) {
				printError("Errore di Autenticazione", "L'urna non si è autenticata correttamente.");
				return null;
			}
			
		} catch (PEException e) {
			throw ENCException.ENC_6(e);
		}
		
		//Viene restituito il nonce cifrato da risolvere
		return encryptedNonce2;
	}
	
	/**
	 * Seconda fase della mutua autenticazione con l'urna.
	 * Se dovesse avere successo la postazione sarà correttamente attivata.
	 * @param encryptedNonce2 Nonce cifrato dall'urna come sfida.
	 * @param sessionKey Chiave di sessione.
	 */
	private Message authenticateToUrn(String encryptedNonce2, String sessionKey) throws PEException {
		//Si tenta di risolvere la sfida dell'urna ottenendo un nonce modificato e cifrato nuovamente.
		String encryptedModifiedNonce2;
		try {
			encryptedModifiedNonce2 = NonceManager.solveChallenge(encryptedNonce2, sessionKey, 2);
		} catch (PEException e) {
			throw ENCException.ENC_6(e);
		}
		
		//Si contatta l'urna
		Message response;
		String errMsg = "Attivazione fallita, impossibile contattare l'urna. Controllare che sia l'urna che questo seggio siano connessi alla rete.";

		String ipRecipient = station.getUrnIp().getHostAddress();
		try (Link link = createLink(station.getUrnIp(), station.getUrnPort(), null, errMsg)) {
			//Indichiamo di volere procedere con la mutua autenticazione
			link.write(Protocol.StationAuthenticationPhase2);
			//Inviamo la nostra soluzione alla sfida lanciata dall'urna
			link.write(encryptedModifiedNonce2);
			
			if(!link.hasNextLine()) {
				throw CNNException.CNN_3("urna", ipRecipient);
			}
			
			//L'urna invia la risposta
			response = (Message) Message.fromB64(link.read(), "urna");
		}
		catch (Exception e) {
			throw CNNException.CNN_1("urna", ipRecipient, e);
		}
		
		return response;
	}
	
	/* --- Funzioni richiamate dai controller delle scene JavaFX --- */
	
	/**
	 * Restituisce il model alle classi della view.
	 * @return	Il seggio.
	 */
	public Station getStation(){
		return station;
	}
	
	/**
	 * Funzione adoperata per resettare una postazione riportandola allo stato attivo e interrompendo qualsiasi operazione stesse compiendo.
	 * Il principale strumento di "risoluzione" problemi per lo staff del seggio.
	 * @param post La postazione da riavviare.
	 */
	public void resetPost(int post) {
		//Se il seggio non è attivo la funzione non può essere eseguita
		if(!correctState(StateStation.ATTIVO)) 
			return;
		
		if(!printConfirmation("Conferma Reset: Sei sicuro di voler resettare la postazione scelta (#"+(post+1)+")?", "Verrà riportata allo stato ATTIVA"))
			return;
		
		//Ci connettiamo con la postazione + Inviamo la richiesta di reset e leggiamo la risposta della postazione
		String response;
		String errMsg = "Associazione fallita, impossibile contattare la postazione " + post + ".\n" + "Controllare che sia la postazione " + post + " che questo seggio siano connessi alla rete.";
		String ipRecipient = station.getPostIp(post).getHostAddress();
		try (Link link = createLink(station.getPostIp(post), station.getPostPort(), 1000, errMsg)) {
			if (link.isClosed()) 
				return;
			
			link.write(Protocol.resetPostReq);
			
			if(!link.hasNextLine())
				throw CNNException.CNN_3("postazione " + post, ipRecipient);
			
			response = link.read();
		}
		catch(Exception e) {
			printError(CNNException.CNN_1("postazione " + post, ipRecipient, e));
			return;
		}
		
		//Se il reset è riuscito salviamo lo stato nel seggio e stampiamo a schermo un messaggio informativo
		if(response.equals(Protocol.resetPostGranted)) {
			station.resetPost(post, false);
			updateViewAndSubstation();
			return;
		}
		
		if(response.equals(Protocol.resetPostDenied)) {
			printError("La postazione non può effettuare il reset", "Potrebbe essere necessario il riavvio manuale.");
			return;
		}
		
		printError("Ricevuto Messaggio Non Riconosciuto o Inatteso", response + "\n"
				+ "Impossibile riavviare la postazione.");
		
		askStatePost(post).start();
		
		updateViewAndSubstation();
	}
	
	/**
	 * Funzione che preleva le informazioni relative ad ogni postazione e le invia ai seggi ausiliari.
	 */
	public void updateSubStations() {
		int numPosts = station.getNumPost();
		
		if(numPosts == 0) {
			return;
		}
		
		for(InetAddress ipSubStation : station.getSubStationIps()) {
			try {
				updateSubStations(ipSubStation, station.getPosts());
			} catch (PEException e) {
				e.printStackTrace();
				printError(e);
			}
		}
	}
	
	/**
	 * Funzione che invia all'urna il nome parziale dell'utente 
	 * perchè questa recuperi e restituisca tutti i potenziali match del database.
	 * @param searchFn 	Stringa simile al nome dell'utente.
	 * @param searchLn 	Stringa simile al cognome dell'utente.
	 * @return Un vettore contenente tutti i votanti della procedura con nome e cognome simile.
	 */
	public Person[] retrieveVotersByName(String searchFn, String searchLn) {
		if(!correctState(StateStation.ATTIVO)) {return null;}
		
		if((searchFn == null || searchFn.length() == 0) && (searchLn == null || searchLn.length() == 0)) {
			printError("Impossibile Effettuare la Ricerca", "Assicurati di aver specificato i criteri di ricerca.");
			return null;
		}
		
		//Viene creata la richiesta con nome e cognome simili
		Message request = new Message(Protocol.searchPersonReq);
		request.setElement("firstName", searchFn);
		request.setElement("lastName", searchLn);
		
		//Inviamo la richiesta e aspettiamo la risposta
		Message response;
		String errMsg = "Impossibile contattare l'urna.\n" + "Controllare che sia l'urna che questo seggio siano connessi alla rete.";
		try (Link link = createLink(station.getUrnIp(), station.getUrnPort(), null, errMsg)) {
			if(link.isClosed()) {return null;}
			
			link.write(Protocol.searchPersonReq);		
			link.write(request.toB64());
			
			if(!link.hasNextLine()) {
				throw CNNException.CNN_3("urna", station.getUrnIp().getHostAddress());
			}
			
			response = (Message) Message.fromB64(link.read(), "urna");
		} catch (PEException e) {
			printError(e);
			return null;
		}
		
		//Verifichiamo che la risposta sia corretta
		try {
			String[] required = {"voters", "missingVoters"};
			Class<?>[] types = {Person[].class, Boolean.class};
			response.verifyMessage(Protocol.searchPersonAck, required, types, "urna");
		} catch (PEException e) {
			printError(e);
			return null;
		}
		
		//Recuperiamo e restituiamo i votanti
		Person[] voters = response.getElement("voters");
		//Verifichiamo la presenza di risultati non inviati
		boolean missingVoters = response.getElement("missingVoters");
		
		if(!missingVoters && voters.length > 0)
			return voters;
		
		if (missingVoters)
			printWarning("Impossibile Mostrare i Risultati", "I criteri di ricerca non sono abbastanza stringenti, sono stati trovati troppi risultati.");
			
		if (!missingVoters && voters.length == 0)
			printError("Impossibile Mostrare i Risultati", "Nessun votante corrisponde ai criteri selezionati.");

		return null;
	}
	
	/**
	 * Funzione richiamata dalla view per memorizzare il votante in fase di registrazione.
	 * @param voter Il votante in fase di registrazione.
	 */
	public boolean setNewVoter(VoterViewModel voterModel) {
		if(voterModel == null) {
			printError("Impossibile Procedere", "Non è stato selezionato alcun votante.");
			return false;
		}
		
		Person voter = voterModel.getVoter();
		
		if(!voter.mayVote()) {
			printError("Votante Non Autorizzato", voter.getFirstName() + " " + voter.getLastName() + " potrebbe avere già votato o non avere il diritto di voto per la procedura corrente");
			return false;
		}
		
		if(voter.getBallotCodes() == null || voter.getBallotCodes().length == 0) {
			printError("Votante Non Autorizzato", voter.getFirstName() + " " + voter.getLastName() + " non risulta abilitato ad alcuna scheda della procedura.");
			return false;
		}
		
		station.setNewVoter(voter);
		updateViewAndSubstation();
		return true;
	}
	
	/**
	 * Funzione che permette la registrazione di un nuovo utente nel DB. Controlla i valori inseriti nella view (stampa eventualmente Dialog di errore
	 * se i dati sono incompleti o non corretti), crea il messaggio per l'urna e lo invia alla stessa. Ricevuta la risposta, stampa un dialog di
	 * conferma se l'operazione è andata a buon fine, altrimenti stampa un dialog d'errore.
	 * @param id
	 * @param lastName
	 * @param firstName
	 * @param birthDate
	 * @param ballots
	 * @return
	 */
	public boolean registerNewUser(String id, String lastName, String firstName, String birthDate, int[] ballots) {
		if (id == null || lastName == null || firstName == null || birthDate == null) {
			printError("Impossibile Procedere con la Registrazione", "Assicurati di aver compilato tutti i campi");
			return false;
		}
		
		if (id.isEmpty() || lastName.isEmpty() || firstName.isEmpty() || birthDate.isEmpty() || ballots.length == 0) {
			printError("Impossibile Procedere con la Registrazione", "Assicurati di aver compilato tutti i campi");
			return false;
		}

		if (!FileUtils.isDate(birthDate)) {
			printError("Impossibile Procedere con la Registrazione", "Il formato della data non è corretto. Utilizza il formato gg/mm/aaaa.");
			return false;
		}
		
		Message request = new Message(Protocol.registerNewUserReq);
		request.setElement("ID", id);
		request.setElement("lastName", lastName);
		request.setElement("firstName", firstName);
		request.setElement("birthDate", birthDate);
		request.setElement("ballots", ballots);
		
		if(!printConfirmation("Sei sicuro di voler registrare il nuovo utente?", "ID: "+id+"\nCognome: "+lastName+"\nNome: "+firstName+"\nData di Nascita: "+birthDate+"\nSchede: "+Arrays.toString(ballots)))
			return false;
		
		Message response;
		String errMsg = "Impossibile contattare l'urna.\nControllare che sia l'urna che questo seggio siano connessi alla rete.";
		try (Link link = createLink(station.getUrnIp(), station.getUrnPort(), null, errMsg)) {
			if(link.isClosed()) {return false;}
			
			link.write(Protocol.registerNewUserReq);
			link.write(request.toB64());
			
			if(!link.hasNextLine())
				throw CNNException.CNN_3("urna", station.getUrnIp().getHostAddress());
			
			response = (Message) Message.fromB64(link.read(), "urna");
		} catch (PEException e) {
			printError(e);
			return false;
		}
		
		//Verifichiamo che la risposta sia corretta
		try {
			response.verifyMessage(Protocol.registerNewUserAck, null, null, "urna");
			
			printSuccess("Registrazione Confermata!", "Il nuovo votante è ora abilitato al voto sulle schede selezionate");
			return true;
		} catch (PEException e) {
			printError(e);
			return false;
		}
	}

	public Person updateExistingUser(String id, int[] ballots){
		if(id == null || id.isEmpty() || ballots.length == 0) {
			printError("Impossibile Procedere con l'Aggiornamento", "Assicurati di aver compilato il campo 'Matricola' e di aver scelto le schede");
			return null;
		}
		
		if(!printConfirmation("Abilitare al voto l'utente scelto?", "Matricola: " + id + "\nSchede: "+Arrays.toString(ballots)))
			return null;

		Message request = new Message(Protocol.updateExistingUserReq);
		request.setElement("ID", id);
		request.setElement("ballots", ballots);

		Message response;
		String errMsg = "Impossibile contattare l'urna.\n" + "Controllare che sia l'urna che questo seggio siano connessi alla rete.";
		try (Link link = createLink(station.getUrnIp(), station.getUrnPort(), null, errMsg)) {
			if(link.isClosed()) {return null;}

			link.write(Protocol.updateExistingUserReq);
			link.write(request.toB64());

			if(!link.hasNextLine())
				throw CNNException.CNN_3("urna", station.getUrnIp().getHostAddress());

			response = (Message) Message.fromB64(link.read(), "urna");
		} catch (PEException e) {
			printError(e);
			return null;
		}

		//Verifichiamo che la risposta sia corretta
		try {
			String[] required = {"voter"};
			Class<?>[] types = {Person.class};
			response.verifyMessage(Protocol.updateExistingUserAck, required, types, "urna");
		} catch (PEException e) {
			printError(e);
			return null;
		}

		Person voter = response.getElement("voter");
		printSuccess("Aggiornamento Completato", "Il votante " + voter.getFirstName() + " " + voter.getLastName() + " è ora abilitato al voto sulle schede selezionate");
		
		return voter;
	}
	
	/* --- Gestione del polling per controllare continuamente lo stato delle postazioni online --- */
	
	void checkForUnreachablePosts() {
		int numPost = station.getNumPost();
		
		SinglePostPolling[] postsThreads = new SinglePostPolling[numPost];
		
		for(int post = 0; post < numPost; post++) {
			DummyPost currPost = station.getPosts()[post];
			
			if (currPost.getState().equals(StatePost.OFFLINE))
				continue;
			
			postsThreads[post] = new SinglePostPolling(this, station, currPost);
			postsThreads[post].start();
		}
	}

	Message checkForUnreachablePost(DummyPost post) {
		//Facciamo richiesta alla postazione per l'invio del suo stato
		Message response;
		try (Link link = createLink(post.getIp(), station.getPostPort(), 250, null)) {
			if(link.isClosed()) 
				return null;

			link.write(Protocol.checkUnreachablePost);

			if(!link.hasNextLine())
				return null;

			int idPost = post.getId();
			response = (Message) Message.fromB64(link.read(), "postazione " + idPost);

			String[] required = {"state", "card"};
			Class<?>[] types = {StatePost.class, String.class};

			response.verifyMessage(Protocol.checkUnreachablePost, required, types, "postazione " + idPost);
			
			return response;
		}
		catch (PEException e) {
			return null;
		}
	}
	
	void changePostState(InetAddress ip, State.StatePost newState) {
		// Se è da resettare, ha senso aggiornare la postazione con schermata d'errore?
		Message request = new Message(Protocol.changePostState);
		request.setElement("newState", newState);
		
		try (Link link = createLink(ip, station.getPostPort(), null, null)) {
			if(link.isClosed())
				return; 

			link.write(Protocol.changePostState);
			link.write(request.toB64());
		}
		catch (PEException linkExc) { 
			linkExc.printStackTrace();
		}
	}

	/* --- Funzioni richiamate direttamente da Service --- */
	
	/**
	 * Funzione per verificare che l'ip di chi sta contattando il server corrisponda a quello di una postazione o seggio ausiliario di questo seggio.
	 * @param ip 		L'ip del mittente.
	 * @param isPost 	Flag per indicare se il servizio richiesto vale per le postazioni (true) o per i seggi ausiliari (false)
	 * @return Vero o falso a seconda se il mittente è una postazione o un seggio ausiliario che ha richiesto un servizio valido per la propria tipologia.
	 */
	boolean verifyIp(InetAddress ip, boolean isPost) {
		if(!correctState(StateStation.ATTIVO)) {
			return false;
		}
		
		if(isPost && station.getPost(ip) != -1) {
			return true;
		}
		
		if(!isPost && station.isSubStation(ip)) {
			return true;
		}
		
		String terminal = isPost ? "postazione" : "seggio ausiliario";
		printError("Comunicazione Inattesa", "Tentativo di connessione da parte di " + ip.toString() + "(sconosciuto) come " + terminal + " di questo seggio.");
		return false;
	}
	
	/**
	 * Funzione che memorizza il voto cifrato inviato da una postazione in attesa di inviarlo all'urna.
	 * @param writtenBallots 	Le schede cifrate compilate dal votante.
	 * @param ipPost 			L'ip della postazione presso cui il votante ha votato.
	 * @return 					Vero o falso a seconda dell'esito dell'operazione.
	 */
	boolean storeVoteLocally(WrittenBallot[] writtenBallots, InetAddress ipPost) {
		//Se il seggio non è attivo allora questa funzione non può essere eseguita
		if(!correctState(StateStation.ATTIVO)) {
			return false;
		}
		
		//Si recupera l'indice della postazione
		int post = station.getPost(ipPost);
		if(post == -1) {
			printError("Errore di Ricezione del Voto", "Ricevuta richiesta di invio voti dall'indirizzo:" + ipPost.toString() + ", che non corrisponde ad alcuna postazione");
			return false;
		}
		
		//Si verifica che la postazione sia "in uso" e che sia legata ad una associazione
		String errorMessage = "";
		StatePost currPostState = station.getPostState(post);
		List<StatePost> acceptedStates = List.of(StatePost.ASSOCIATA, StatePost.IN_USO, StatePost.VOTO_PENDING);
		if(!acceptedStates.contains(currPostState)) {
			errorMessage += "La postazione " + post + " risulta " + currPostState + " mentre dovrebbe essere ASSOCIATA, IN USO o VOTO PENDING.\n";
		}
		
		if(station.getPostBadge(post).equals(Protocol.unassignedPost)) {
			errorMessage += "Non è stato associato alcun badge alla postazione " + post + ".\n";
		}
		
		if(station.getPostVoter(post) == null) {
			errorMessage += "Non è stato associato alcun votante alla postazione " + post + ".\n";
		}
		
		//Qualunque errore rilevato viene mostrato e la funzione interrotta
		if(!errorMessage.isEmpty()) {
			printError("Errore di Ricezione del Voto", errorMessage);
			return false;
		}
		
		//Le schede cifrate vengono memorizzate
		station.setEncryptedBallots(writtenBallots, post);
		
		//Si aggiorna la view e i seggi ausiliari rigurado il cambio di stato della postazione
		updateViewAndSubstation();
		return true;
	}
	
	/**
	 * Funzione che riceve e memorizza lo stato di una postazione.
	 * @param stateAsString Lo stato della postazione inviato come stringa.
	 * @param ip			L'ip della postazione.
	 * @return Vero o falso a seconda se lo stato è riconosciuto.
	 */
	void setStatePost(String stateAsString, InetAddress ip) {
		//Se il seggio è già stato attivato allora questa funzione non deve essere eseguita
		if(!correctState(StateStation.ATTIVO)) return;

		//Si recupera l'indice della postazione a partire dall'ip
		int post = station.getPost(ip);
		if(post == -1) {
			printError("Aggiornamento Sconosciuto", "Ricevuto aggiornamento da una postazione con l'IP (sconosciuto):" + ip.toString());
			return;
		}
		
		//Si recupera lo stato inviato dalla postazione
		StatePost state = model.State.getStatePostFromString(stateAsString);
		
		//Se la stringa inviata non corrisponde ad alcuno stato conosciuto
		if(state == null) {
			printError("Aggiornamento Sconosciuto", "La postazione " + post + " ha inviato lo stato non riconosciuto " + stateAsString);
			return;
		}
		
		//Se la postazione comunica di essere offline ...
		if(state == StatePost.OFFLINE) {
			// ... ed era nel mezzo di una votazione lo si comunica allo staff
			List<State.StatePost> votingStates = List.of(StatePost.ASSOCIATA, StatePost.IN_USO, StatePost.VOTO_INVIATO, StatePost.VOTO_PENDING);
			if(votingStates.contains(station.getPostState(post)))
				printError("Aggiornamento Sconosciuto", "La postazione " + post + " è stata spenta durante l'uso.");
			
			//In ogni caso vengono eliminati i dati del votante se ce ne erano
			station.resetPost(post, true);
			updateViewAndSubstation();
			return;
		}
		
		//In qualunque altro caso si procede con la modifica dello stato e l'aggiornamento di view e seggi ausiliari
		station.setPostState(state, post);
		updateViewAndSubstation();
	}
	
	int getPostIdx(String card) {
		return station.getPost(card);
	}
	
	/**
	 * Funzione richiamata in risposta ad una richiesta del seggio ausiliario, dopo che questo legge una card dal lettore RFID.
	 * @param intCard	La la card letta dal seggio ausiliario.
	 * @param voter     L'eventuale votante che si è registrato presso il seggio ausiliario.
	 * @return			La risposta del seggio.
	 */
	synchronized Message readCardForSubStation(String intCard, Person voter) throws PEException {
		//Se seggio non è attivo lo indichiamo al seggio ausiliario
		if(!correctState(StateStation.ATTIVO))
			throw FLRException.FLR_01();
		
		Message response = new Message();
		
		//Dati i controlli sul Service, se voter non è null allora sto creando una nuova associazione
		if (voter != null) {
			//Informiamo il seggio ausiliario della associazione creata
			int associatedPost = createAssociationForSubStation(voter, intCard);
			response.setValue(Protocol.associationAck);
			response.setElement("post", associatedPost);
		}
		else {
			//Se l'invio del voto riesce informiamo il seggio ausiliario
			int post = sendVoteLogic(intCard);
			response.setValue(Protocol.votesReceivedAck);

			try{
				notifyVoteConclusionToPost(post);
			}
			catch(PEException pee){
				response.setElement("details", "Il voto è stato ricevuto dall'urna ma non è stato possibile informare la postazione");
			}
		}
			
		//Restituiamo la risposta perchè venga inviata
		return response;
	}
	
	void deactivateStation() {
		if (station.getState() == State.StateStation.NON_ATTIVO)
			return;
		
		deactivateStationOps();
		
		if(Constants.verbose)
			printWarning("Terminale Disattivato", "L'urna è stata spenta, il seggio è stata riportata allo stato 'NON ATTIVO'");
		
		updateViewAndSubstation();	
	}
	
	/* --- Funzioni di utility --- */
	
	/**
	 * Funzione che permette di usare il seggio come una macchina a stati finiti.
	 * Ogni funzione che richiede di essere in un particolare stato richiama questa per il controllo.
	 * @param expectedState Stato del seggio in cui è necessario trovarsi.
	 * @return Vero o falso a seconda se il server è attivo e lo stato è quello corretto.
	 */
	public boolean correctState(StateStation expectedState) {
		StateStation state = station.getState();
		if(!state.equals(expectedState)) {
			printMessage("controller.correctState:Expected " + expectedState + " but found " + state);
			return false;
		}
		
		if(!server.isRunning()) {
			printMessage("controller.correctState:The server is not running!");
			return false;
		}
		
		return true;
	}

	/**
	 * Funzione che associa un votante, quello in fase di registrazione,
	 * al badge passato ed a una postazione scelta casualmente tra quelle disponibili.
	 * Richiamata quando un badge non associato ad altri votanti è letto dal reader RFID.
	 * @param badge Il valore letto dal reader RFID.
	 * @return Vero o falso a seconda dell'esito dell'operazione.
	 */
	private boolean createAssociation(String badge) {
		//Si recupera il votante selezionato dallo staff con la fase di registrazione
		Person voter = station.getNewVoter();
		
		try {
			//Si effettuano dei controlli (locali) sul votante
			beforeAssociationControls(voter);
		} catch (PEException e) {
			printError(e);
			return false;
		}
		
		//Viene creata una nuova associazione per il votante
		int associatedPost;
		try {
			associatedPost = createAssociationLogic(voter, badge);
		}
		catch (PEException e) {
			printError(e);
			return false;
		}
		
		//Il votante è stato assegnato a una postazione, si può registrare un nuovo votante
		station.setNewVoter(null);
		
		//Si aggiorna la view e si informa lo staff con un messaggio
		String associationMsg = "Postazione: " + associatedPost;
		if(Constants.verbose)
			associationMsg += "\nBadge: " + badge + "\nVotante: " + voter.getLastName() + ", " + voter.getFirstName();
		printSuccess("Associazione Riuscita", associationMsg); 
		updateView();
		
		return true;
	}
	
	/**
	 * Funzione che crea una nuova associazione in vece di un seggio ausiliario.
	 * @param voter Il votante registratosi presso il seggio ausiliario.
	 * @param badge Il badge letto dal reader RFID del seggio ausiliario.
	 * @return La postazione scelta per l'associazione.
	 * @throws PEException
	 */
	private int createAssociationForSubStation(Person voter, String badge) throws PEException {
		//Si effettuano dei controlli (locali) sul votante
		beforeAssociationControls(voter);
		
		//Viene creata una nuova associazione per il votante
		return createAssociationLogic(voter, badge);
	}
	
	/**
	 * Funzione richiamata quando dal reader RFID o da un seggio ausiliario arriva la richiesta di creazione di una associazione.
	 * @param voter Il votante per cui creare l'associazione.
	 * @param badge Il badge usato come riferimento alla associazione.
	 * @return La postazione scelta per la associazione.
	 * @throws PEException
	 */
	private int createAssociationLogic(Person voter, String badge) throws PEException {
		//Riserviamo la postazione in modo che non possa essere assegnata a due votanti contemporaneamente
		if(!station.reservePost()) {
			throw FLRException.FLR_05();
		}

		//Da questo momento in poi non si potranno fare nuove associazioni finchè non avremo terminato con questa.
		//Quindi è necessario che si rilasci il lock che l'associazione o meno.
		try {
			//Recuperiamo la postazione scelta per l'associazione
			int post = station.getAssociatedPost();
			
			//Prepariamo la richiesta
			Message request = new Message();
			request.setValue(Protocol.associationReq);
			request.setElement("ballotCodes", voter.getBallotCodes());
			request.setElement("badge", badge);
			
			//Contattiamo la postazione + Inviamo la richiesta di associazione e attendiamo la risposta
			Message response;
			String errMsg = "Associazione fallita, impossibile contattare la postazione " + post + ".\n" + "Controllare che sia la postazione " + post + " che questo seggio siano connessi alla rete.";
			String ipRecipient = station.getPostIp(post).getHostAddress();
			try (Link link = createLink(station.getPostIp(post), station.getPostPort(), null, errMsg)) {
				
				link.write(Protocol.associationReq);
				link.write(request.toB64());
				
				if(!link.hasNextLine()) {
					String otherInfo = "Associazione della postazione " + post
							+ "\nal badge " + badge 
							+ "\nper il votante " + voter + "\nrespinta, nessun messaggio ricevuto.";
					throw CNNException.CNN_3("postazione " + post, ipRecipient, otherInfo);
				}
				
				 response = (Message) Message.fromB64(link.read(), "postazione " + post);
			}
			
			//Verifichiamo che la postazione abbia risposto positivamente
			response.verifyMessage(Protocol.associationAck, null, null, "postazione " + post);
			
			//Memorizziamo l'associazione nel seggio e aggiorniamo view e seggi ausiliari
			station.setAssociation(voter, badge, post);
			updateViewAndSubstation();

			return post;
		}
		finally {
			//Rilasciamo il lock per permettere di creare altre associazioni
			station.endPostReservation();
		}
	}
	
	/**
	 * Funzione eseguita prima di una associazione per verificare la possibilità del votante.
	 * @param voter Il votante per cui si stanno effettuando i controlli.
	 * @throws PEException
	 */
	private void beforeAssociationControls(Person voter) throws PEException {
		//Si verifica che il seggio sia attivo
		if(!correctState(StateStation.ATTIVO)) {
			throw FLRException.FLR_01();
		}
		
		//Si verifica che il votante possa votare, e che gli siano stati assegnate delle schede di voto
		if(voter == null)
			throw FLRException.FLR_07();

		Person.DocumentType docType = voter.getDocumentType();
		if(docType == null)
			throw FLRException.FLR_15(voter, false);

		String docID = voter.getDocumentID();
		if(docType != Person.DocumentType.CONOSCENZA_PERSONALE && ((docID == null || docID.isEmpty())))
			throw FLRException.FLR_15(voter, true);

		if(!voter.mayVote())
			throw FLRException.FLR_11(voter, -1);
		
		int[] ballotList = voter.getBallotCodes();
		if(ballotList == null || ballotList.length == 0)
			throw DBException.DB_10(voter);
		
		//Si verifica che il votante non sia già stato assegnato a qualche postazione
		isVoterAlreadyVoting(voter);
	}
	
	/**
	 * Funzione che verifica che il votante non sia già stato assegnato ad una delle postazioni.
	 * @param voter2find Il votante per cui effettuare la verifica.
	 * @throws PEException Il messaggio di errore che ha fatto fallire la verifica.
	 */
	private void isVoterAlreadyVoting(Person voter2find) throws PEException {
		int numPost = station.getNumPost();
		if(numPost == 0) {
			throw DEVException.DEV_01();
		}
		
		//Per ogni postazione verifichiamo se un votante con stesso ID sta già votando
		for(int post = 0; post < numPost; post++) {
			Person voter = station.getPostVoter(post);
			
			if(voter != null && voter.hasSameID(voter2find)) {
				throw FLRException.FLR_04(voter.getID(), post+1);
			}
		}
	}
	
	/**
	 * Funzione che invia il voto relativo al badge letto, all'urna, insieme ai dati sul votante.
	 * @param badge Il badge letto dal lettore RFID.
	 * @return Vero o falso a seconda dell'esito dell'operazione.
	 */
	boolean sendVote(String badge, boolean updatePost) {
		int post;

		//Inviamo il voto all'urna
		try {
			post = sendVoteLogic(badge);
		} catch (PEException e) {
			printError(e);
			return false;
		}

		int id = station.getPosts()[post].getId();

		try{
			//Informiamo la postazione del termine della votazione
			if(updatePost)
				notifyVoteConclusionToPost(post);
			
			printSuccess("Voto Inviato con Successo", "Le schede compilate nella postazione " + id + " sono state inviate all'urna.");
		}
		catch(PEException pee){
			pee.printStackTrace();
			printWarning("Voto Inviato con Successo", "Le schede compilate nella postazione " + id + " sono state inviate all'urna, "
					+ "ma non è stato possibile notificarlo alla postazione.");
		}

		//Aggiorniamo la view e i seggi ausiliari e stampiamo a schermo un messaggio informativo
		updateViewAndSubstation();
		
		return true;
	}
	
	/**
	 * Funzione richiamata quando dal reader RFID o da un seggio ausiliario arriva la richiesta di invio voti all'urna.
	 * @param badge Il badge usato come riferimento alla associazione.
	 */
	private int sendVoteLogic(String badge) throws PEException {
		//Se il seggio non è attivo segnaliamo l'errore
		if(!correctState(StateStation.ATTIVO)) {
			throw FLRException.FLR_01();
		}
		
		//Recuperiamo la postazione associata a questo badge
		int post = station.getPost(badge);

		//Recuperiamo il votante associato al badge
		Person voter = station.getPostVoter(post);

		if(station.getPostState(post) != StatePost.VOTO_INVIATO  || voter == null){
			if(voter == null)
				throw DEVException.DEV_05();
			else
				throw DEVException.DEV_05(voter);
		}
		
		//Recuperiamo le schede cifrate del votante
		WrittenBallot[] encryptedBallots = station.getEncryptedBallots(post);
		if(encryptedBallots == null || encryptedBallots.length == 0) {
			throw DEVException.DEV_05(voter);
		}

		//Prepariamo la richiesta per l'urna
		Message request = new Message();
		request.setValue(Protocol.sendVoteToUrn);
		request.setElement("voter", voter);
		request.setElement("encryptedBallots", encryptedBallots);
		request.setElement("ipPost", station.getPostIp(post));
		
		//Contattiamo l'urna + Inviamo la richiesta e aspettiamo la risposta
		Message response;
		String errMsg = "Invio voti fallito, impossibile contattare l'urna.\n" + "Controllare che sia l'urna che questo seggio siano connessi alla rete.";
		String ipRecipient = station.getUrnIp().getHostAddress();
		try (Link link = createLink(station.getUrnIp(), station.getUrnPort(), null, errMsg)) {
			if(link.isClosed()) throw CNNException.CNN_3("urna", ipRecipient);
			
			link.write(Protocol.sendVoteToUrn);
			
			link.write(request.toB64());
			
			if(!link.hasNextLine()) {
				throw CNNException.CNN_3("urna", ipRecipient);
			}
			
			response = (Message) Message.fromB64(link.read(), "urna");
		}
		catch (Exception e) {
			throw CNNException.CNN_1("urna", ipRecipient, e);
		}
		
		//Verifichiamo che la risposta sia valida
		response.verifyMessage(Protocol.votesReceivedAck, null, null, "urna");

		//Eliminiamo l'associazione memorizzata
		station.destroyAssociation(post);

		return post;
	}
	
	/**
	 * Funzione che contatta una postazione al termine di un voto per farla tornare nello stato ATTIVA,
	 * rendendola di nuovo disponibile per ulteriori votazioni.
	 */
	private void notifyVoteConclusionToPost(int post) throws PEException {
		String failure = "Tentativo di notifica postazione fallito.";
		
		//Contattiamo la postazione
		String errMsg = failure + "\nAssicurarsi che sia la postazione " + post + " che questo seggio siano connessi alla rete.";
		String ipRecipient = station.getPostIp(post).getHostAddress();
		try (Link link = createLink(station.getPostIp(post), station.getPostPort(), null, errMsg)) {
			if(link.isClosed()) return;
			
			link.write(Protocol.postEndVoteReq);
			link.hasNextLine(); //Linea necessaria per i test
		}
		catch(Exception e) {
			station.resetPost(post, true);
			throw CNNException.CNN_1("postazione " + station.getPosts()[post].getId(), ipRecipient, e);
		}
		finally{
			updateViewAndSubstation();
		}
	}
	
	/**
	 * Funzione che invia una richiesta a tutte le postazioni affinché queste inviino il loro stato.
	 */
	private void askStateToPosts() {
		//Se il seggio non è attivo allora questa funzione non può essere eseguita
		if(!correctState(StateStation.ATTIVO)) return;
		
		//Si richiede lo stato ad ogni postazione
		int numPost = station.getNumPost();
		Thread[] t = new Thread[numPost];
		
		for(int post = 0; post < numPost; post++) {
			t[post] = askStatePost(post);
			t[post].start();
		}
	}
	
	/**
	 * Funzione che restituisce un thread per contattare e farsi restituire lo stato di una postazione.
	 * Si adoperano i thread in modo da velocizzare l'attivazione del seggio, che se no richiederebbe,
	 * un tempo lineare nel numero di postazioni.
	 * @param post La postazione da contattare.
	 * @return Il thread che contatterà la postazione.
	 */
	private Thread askStatePost(int post) {
		return new Thread(() -> {
			//Facciamo richiesta alla postazione per l'invio del suo stato
			try (Link link = createLink(station.getPostIp(post), station.getPostPort(), 1000, null)) {
				if(link.isClosed()) return;
				
				link.write(Protocol.retrieveStatePost);
			}
			catch (PEException ignored) {
			}
		});
	}
	
	/**
	 * Funzione creata solo perchè a Marco seccava scrivere ogni volta 2 righe anzichè una sola.
	 */
	void updateViewAndSubstation() {
		updateView();
		updateSubStations();
	}
	
	/**
	 * Funzione che inoltra le informazioni sulle postazioni ad un seggio ausiliario.
	 * @param ipSubStation	L'ip del seggio ausiliario a cui inviare le informazioni.
	 * @param posts			Le informazioni da inviare.
	 */
	private void updateSubStations(InetAddress ipSubStation, DummyPost[] posts) throws PEException {
		if(!correctState(StateStation.ATTIVO)) 
			return;
		
		//Creiamo lo snapshot inserendo le postazioni
		Message snapshot = new Message(Protocol.updateSubStation);
		snapshot.setElement("posts", posts);
		
		//Contattiamo il seggio ausiliario + Inviamo lo snapshot
		try (Link link = createLink(ipSubStation, station.getSubStationPort(), null, null)) {
			if(link.isClosed()) return;
			
			link.write(Protocol.updateSubStation);
			link.write(snapshot.toB64());
		}
	}
	
	private void deactivateStationOps() {
		station.setState(StateStation.NON_ATTIVO);
		station.setEmptyBallots(null);
		station.setSessionKey(null);
		
		stopUrnThread();
		stopPostsThread();
	}
	
	private void stopPostsThread() {
		if(postsThread != null) {
			postsThread.shutDown();
			try {
				postsThread.join();
			} catch (InterruptedException ignored) {}
		}
	}
	
	/* --- Funzioni da implementare in quanto TerminalController --- */
	
	@Override
	public synchronized String readCard(String card) {
		if(Constants.devMode) {
			if(card.equals("off")) {
				station.setSimulateOffline(true);
				return null;
			}
			
			if(card.equals("on")) {
				station.setSimulateOffline(false);
				return null;
			}
		}
		
		//Se il seggio non è ancora attivo non facciamo nulla per il reader RFID
		if(!correctState(StateStation.ATTIVO)) {return "noOp";}
		
		//Usato unicamente per non lasciare il thread del reader RFID in attesa dopo lo spegnimento
		if(card.equals(CardReader.exit)) {
			return null;
		}
		
		//Si verifica se il badge era già associato ad una postazione o se sta venendo usato per crearne una nuova
		int post = station.getPost(card);
		if(post == -1) {
			Person.DocumentType docType = station.getDocumentType();
			String documentID = station.getDocumentID() != null ? station.getDocumentID() : "";
			
			if (station.getNewVoter() == null) {
				printError("Impossibile Creare Associazione: Seleziona un votante prima di procedere", "Assicurati di aver selezionato un votante");
				return Protocol.associationNack;
			}
			
	    	if (docType == null) {
	    		printError("Impossibile Creare Associazione: Documento Mancante", "Seleziona una tipologia di documento e riprova");
	    		return Protocol.associationNack;
	    	}
	    	
	    	if (documentID.length() == 0 && !docType.equals(Person.DocumentType.CONOSCENZA_PERSONALE)) {
	    		printError("Impossibile Creare Associazione: Documento Mancante", "Seleziona \"Conoscenza personale\" o aggiungi il numero del documento selezionato");
	    		return Protocol.associationNack;
	    	}
	    	
			//Indichiamo se l'associazione ha successo o meno (Utile per lo unit testing)
			if(createAssociation(card)) {
				return Protocol.associationAck;
			}
			else {
				return Protocol.associationNack;
			}
		}
		else {
			//Indichiamo se l'invio del voto ha successo o meno (Utile per lo unit testing)
			if(sendVote(card, true)) {
				return Protocol.votesReceivedAck;
			}
			else {
				return Protocol.votesReceivedNack;
			}
		}
	}
	
	@Override
	protected void invalidAuthentication() {
		if(station.getState() == StateStation.NON_ATTIVO)
			return;
		
		deactivateStationOps();
		
		printError("Autenticazione Non Valida", "L'urna non riconosce questo seggio come valido, è necessario effettuare nuovamente l'autenticazione.");
		updateViewAndSubstation();
	}
	
	@Override
	protected boolean verifyUrnIp(InetAddress ip) {
		if (ip.equals(station.getUrnIp()))
			return true;
		
		printError("Comunicazione Inattesa", "Tentativo di connessione da parte di " + ip.toString() + "(sconosciuto) come urna.");
		return false;
	}

	@Override
	protected boolean beforeStartOps() { 
		return true; 
	}
	
	@Override
	protected void afterClosureOps() {
		if(station.getState() == StateStation.NON_ATTIVO)
			return;

		stopPostsThread();
		
		signalShutDown(station.getUrnIp(), Constants.portUrn);
		
		DummyPost[] posts = station.getPosts();
		if (posts != null)
			for (DummyPost post : posts)
				signalShutDown(post.getIp(), station.getPostPort());
		
		InetAddress[] substats = station.getSubStationIps();
		if (substats != null)
			for (InetAddress ipSubStation : substats)
				signalShutDown(ipSubStation, station.getSubStationPort());
	}
	
	@Override
	public void setRfidReachable(boolean reachable) {
		if(reachable != station.isRfidReaderReachable()) {
			station.setRfidReaderReachable(reachable);
			updateView();
		}
	}
}