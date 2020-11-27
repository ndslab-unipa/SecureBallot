package postazione.controller;

import java.net.InetAddress;
import java.util.List;
import java.util.Random;

import controller.TerminalController;
import encryption.AES;
import encryption.NonceManager;
import exceptions.CNNException;
import exceptions.DEVException;
import exceptions.ENCException;
import exceptions.FLRException;
import exceptions.PEException;
import controller.AbstrServer;
import controller.CardReader;
import controller.UrnPolling;
import controller.Link;
import controller.Server;
import model.EmptyBallot;
import model.Message;
import model.State;
import model.Terminals;
import model.State.StatePost;
import model.WrittenBallot;
import postazione.model.Post;
import utils.CfgManager;
import utils.Constants;
import utils.FileUtils;
import utils.Protocol;
import view.ViewInterface;

public class Controller extends TerminalController {
	private final Post post;
	
	/**
	 * Costruttore adoperato per il testing.
	 * @param server 	Il server fittizio (classe TestServer)
	 * @param view 		Lo stream di output che fa da view fittizia.
	 * @param post 		Classe model.
	 */
	public Controller(AbstrServer server, ViewInterface view, Post post) {
		super(server, view, Terminals.Type.Post, false);
		this.post = post;
	}
	
	/**
	 * Costruttore reale.
	 * @param view Lo stream di output da cui legge la view.
	 * @param post Classe model.
	 */
	public Controller(ViewInterface view, Post post) throws PEException {
		super(new Server(new Factory(), Constants.portPost, post.getNumConnections(), Terminals.Type.Post), view, Terminals.Type.Post, Constants.postRfid);
		this.server.setController(this);
		this.post = post;
		
		if(!Constants.devMode)
			this.post.setUrnIp(CfgManager.getValue("ips.cfg", "urn"));
	}
	
	/* --- Inizializzazione della Postazione --- */
	
	/**
	 * Funzione che tenta di portare a termine la mutua autenticazione con l'urna.
	 * @param sessionKey La chiave di sessione inserita per creare e superare le sfide basate su nonce.
	 */
	public void activate(String sessionKey) {
		//Se la postazione è già stata attivata allora questa funzione non deve essere eseguita
		if(!correctState(StatePost.NON_ATTIVA)) {return;}
		
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
				
				//Si verifica che il messaggio sia valido e contenga i campi necessari (autenticazione con l'urna avvenuta)
				String[] required = {"ipStation", "pubKey", "ballots"};
				Class<?>[] types = {InetAddress.class, byte[].class, EmptyBallot[].class};
				urnResponse.verifyMessage(Protocol.validAuthentication, required, types, "urna");
				
				//Vengono recuperati tutti i dati necessari inviati dall'urna
				InetAddress stationIp = urnResponse.getElement("ipStation");
				byte[] accountantPublicKey = urnResponse.getElement("pubKey");
				EmptyBallot[] ballots = urnResponse.getElement("ballots");
				
				//La postazione diventa attiva e i dati recuperati vengono memorizzati
				post.setState(StatePost.ATTIVA);
				post.setStationIp(stationIp);
				post.setSessionKey(sessionKey);
				post.setAccountantPublicKey(accountantPublicKey);
				post.setProcedureBallots(ballots);
				
				if(Constants.verbose)
					printSuccess("Attivazione Completata", "La postazione è stata attivata!");
				
				//Vengono informati view e utente
				updateView();
				
				//Se tutto ha avuto successo si notifica la propria attivazione al seggio
				if(post.getStationIp() != null) {
					notifyStateToStation(null);
				}

				urnThread = new UrnPolling(post.getUrnIp(), this, Terminals.Type.Post);
				urnThread.start();
			}
		}
		catch (PEException e) {
			//e.printStackTrace();
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
		
		Message response;
		String errMsg = "Attivazione fallita, impossibile contattare l'urna. Controllare che sia l'urna che questa postazione siano connessi alla rete.";
		String ipRecipient = post.getUrnIp().getHostAddress();
		try (Link link = createLink(post.getUrnIp(), post.getUrnPort(), null, errMsg)) {
			if(link.isClosed()) {return null;}
			
			//Indichiamo di volere iniziare la mutua autenticazione
			link.write(Protocol.PostAuthenticationPhase1);
			//Inviamo il nostro nonce cifrato
			link.write(encryptedNonce1);
			
			if(!link.hasNextLine()) {
				throw CNNException.CNN_3("urna", ipRecipient);
			}
			
			//L'urna invia il messaggio di risposta
			response = (Message) Message.fromB64(link.read(), "urna");
		}
		catch (Exception e) {
			//throw new PEException("Impossibile comunicare con l'urna.", Category.Connection, e);
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
	 * @return response Il messaggio restituito dall'urna in seguito alla risposta alla sfida
	 */
	private Message authenticateToUrn(String encryptedNonce2, String sessionKey) throws PEException {
		//Si tenta di risolvere la sfida dell'urna ottenendo un nonce modificato e cifrato nuovamente.
		String encryptedModifiedNonce2 = NonceManager.solveChallenge(encryptedNonce2, sessionKey, 2);
		
		//Si contatta l'urna
		Message response;
		String errMsg = "Attivazione fallita, impossibile contattare l'urna. Controllare che sia l'urna che questa postazione siano connessi alla rete.";
		String ipRecipient = post.getUrnIp().getHostAddress();
		try (Link link = createLink(post.getUrnIp(), post.getUrnPort(), null, errMsg)) {
			//Indichiamo di volere procedere con la mutua autenticazione
			link.write(Protocol.PostAuthenticationPhase2);
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
	 * @return	La postazione.
	 */
	public Post getPost(){
		return post;
	}
	
	/**
	 * Funzione che cifra e invia i pacchetti di voto al seggio.
	 * @return 
	 */
	public void tryVoteSending() {
		//Se la postazione non è in uso non effettuiamo operazioni
		if(!correctState(StatePost.IN_USO))
			return;
				
		if (!printConfirmation("Sei sicuro di procedere con l'invio dei voti?", "Non potrai più modificare le scelte effettuate"))
			return;
		
		try {
			askForNonces();
			sendVotes();
			
			return;
		} 
		catch (PEException e) {
			printError(e);
		}

		post.setState(StatePost.VOTO_PENDING);
		updateView();
	}
	
	public void resendVotes() {
		//Se la postazione non è in vote_pending non effettuiamo operazioni
		if(!correctState(StatePost.VOTO_PENDING))
			return;
		
		try {
			askForNonces();
			sendVotes();
			
			printSuccess("Voti Inviati con Successo", "Il seggio ha ricevuto i voti. Tornare al seggio per concludere il voto.");
		} 
		catch (PEException e) {
			if(e.getCode() != PEException.Code.FLR_0) {
				printError(e);
				return;
			}
			
			printError("Impossibile Inviare i Voti", "Tornare al seggio per ricominciare la procedura di voto.");
				
			post.setState(StatePost.DA_RESETTARE);
			try {
				notifyStateToStation(null);
			} catch (PEException ignored) {}

			updateView();
		}
	}
	
	/**
	 * Funzione richiamata dalla view per aggiungere la preferenza ad un candidato.
	 * @param ballotNum Il numero della scheda.
	 * @param listName Il nome della lista del candidato.
	 * @param candidateID L'ID del candidato.
	 * @param add Vero o falso a seconda se si vuole selezionare o deselezionare il candidato.
	 * @return Vero o falso a seconda se l'operazione ha avuto successo.
	 */
	public boolean selectCandidate(int ballotNum, String listName, String candidateID, boolean add) {
		//Si verifica se il candidato sta realmente presentandosi per la lista nella scheda i-esima
		if(!post.validCandidate(ballotNum, listName, candidateID)) {
			printError("Errore di Voto", "Il candidato selezionato ("+candidateID+") non è valido per la scheda corrente ("+ballotNum+")");
			return false;
		}
		
		//Si tenta di aggiungere la preferenza ...
		if(post.setPreference(ballotNum, candidateID, add)) {
			return true;
		}
		//controllando di non aver superato il numero minimo (?) ...
		else if(!add){
			printError("Errore di Voto: Impossibile rimuovere altre preferenze", "Numero minimo già raggiunto. Aggiungere una preferenza e riprovare.");
			return false;
		}
		// o massimo di preferenze
		else {
			printError("Errore di voto: Impossibile aggiungere altre preferenze", "Numero massimo già raggiunto. Rimuovere una preferenza e riprovare.");
			return false;
		}
	}
	
	/**
	 * Funzione richiamata dalla view per aggiungere la preferenza ad una opzione.
	 * @param ballotNum Il numero della scheda.
	 * @param option	La stringa relativa all'opzione da aggiungere/rimuovere.
	 * @param add		True per l'aggiunzione, false per la rimozione.
	 * @return			L'esito dell'operazione.
	 */
	public boolean selectOption(int ballotNum, String option, boolean add) {
		//Si verifica se l'opzione è realmente esistente per la scheda i-esima
		if(!post.validOption(ballotNum, option)) {
			printError("Errore di Voto", "Non è stato trovato alcuna opzione \"" + option + "\" per la scheda " + ballotNum);
			return false;
		}
		
		//Per distinguere le preferenze dai candidati si aggiunge un prefisso
		String markedOption = Protocol.isOption + option;
		
		//Si tenta di aggiungere la preferenza ...
		if(post.setPreference(ballotNum, markedOption, add)) {
			return true;
		}
		//controllando di non aver superato il numero minimo (?) ...
		else if(!add){
			printError("Errore di Voto", "Non sono state selezionate sufficienti preferenze per questa scheda");
			return false;
		}
		// o massimo di preferenze
		else{
			printError("Errore di Voto", "Sono state selezionate troppe preferenze per questa scheda");
			return false;
		}
	}
	
	/* --- Funzioni richiamate direttamente da Service --- */
	
	/**
	 * Funzione per verificare che l'ip di chi sta contattando il server corrisponda a quello del seggio.
	 * @param ip L'ip del mittente.
	 * @return Vero o falso a seconda se il mittente è davvero il seggio.
	 */
	boolean verifyStationIp(InetAddress ip) {
		InetAddress stationIP = post.getStationIp();
		
		if(stationIP == null)
			return true; //-> così la postazione può rispondere a richieste sul proprio stato dai seggi, anche prima di essere attivata
		
		if(stationIP.equals(ip))
			return true;
		
		printError("Comunicazione Inattesa", "Tentativo di connessione da parte di " + ip.toString() + "(sconosciuto) come seggio.");
		return false;
	}
	
	/**
	 * Funzione richiamata quando il server riceve una richiesta di associazione da parte del seggio.
	 * @param ballotCodes 	La lista dei codici delle schede di questa procedura valide per il votante.
	 * @param badge 		Il badge di questa associazione.
	 * @return 				Il messaggio di risposta per il seggio.
	 */
	Message setAssociation(int[] ballotCodes, String badge) throws PEException {
		//Se la postazione non è attiva informiamo il seggio che non possiamo associarla
		if(!correctState(StatePost.ATTIVA))
			throw CNNException.CNN_2("postazione");
		
		Message response = new Message();
		
		//Verifichiamo che non sia ancora assegnato un badge alla postazione
		String currentBadge = post.getBadge();
		if(!currentBadge.equals(Protocol.unassignedPost))
			response.addError("La postazione è già associata al badge:" + currentBadge + ".");
		
		//Verifichiamo che non siano rimaste schede memorizzate
		if(post.areBallotsSet(false))
			response.addError("La postazione risulta essere già occupata.");
		
		//Se qualche verifica non è stata superata informiamo il seggio
		if(!response.getErrors().isEmpty()) {
			response.setValue(Protocol.associationNack);
			return response;
		}
		
		//Settiamo lo stato ad associata e memorizziamo i dati inviati dal seggio
		post.setState(StatePost.ASSOCIATA);
		post.setBallots(ballotCodes);
		post.setBadgeID(badge);
		post.setLastWrongBadge(null);
		
		//Indichiamo nella risposta al seggio che l'associazione ha avuto successo
		response.setValue(Protocol.associationAck);
		
		//Aggiorniamo la view e stampiamo un messaggio informativo
		if(Constants.verbose)
			printSuccess("Postazione Associata", "In attesa del Badge " + badge);
		
		if(Constants.postRfid && rfidReader != null)
			rfidReader.allowReading();
		
		updateView();
		return response;
	}
	
	/**
	 * Funzione richiamata quando il seggio legge il badge associato a questa postazione.
	 * Significa che la votazione è anda a buon fine e possiamo di nuovo rendere disponibile (attiva) la postazione.
	 * @throws PEException Nel caso in cui il voto non venga correttamente registrato per qualsiasi motivo.
	 */
	void badgeOut() throws PEException {
		//Se la postazione non è nello stato "voto inviato" non procediamo
		if(!correctState(StatePost.VOTO_INVIATO))
			throw DEVException.DEV_05();
		
		//Settiamo la postazione ad attiva, e resettiamo schede e badge.
		post.setState(StatePost.ATTIVA);
		post.resetInfo();
		
		//Aggiorniamo la view
		updateView();
	}
	
	/**
	 * Funzione che notifica il seggio di un cambiamento di stato.
	 * Viene richiamata sia da funzioni server che client, dato che i cambiamenti di stato
	 * possono essere causati sia dalla view che dal seggio.
	 * @param ipStation L'ip del mittente, utile nel caso la postazione non sia stata ancora attivata e quindi non conosca l'ip del proprio seggio.
	 */
	void notifyStateToStation(InetAddress ipStation) throws PEException {
		//Questo metodo viene chiamato con null da badgeIn(), con l'IP di chi ha avviato la connessione da Service
		if(ipStation == null) {
			ipStation = post.getStationIp();
			
			if(ipStation == null)
				throw DEVException.DEV_01();
		}
		
		//Crea la connessione al seggio per informarlo dello stato della postazione
		try (Link link = createLink(ipStation, post.getStationPort(), null, null)){
			if(link.isClosed()) 
				return;
			
			//Indichiamo al seggio l'intenzione di inviare lo stato
			link.write(Protocol.informStatePost);
			//Inviamo lo stato
			link.write(post.getState());
		}
	}
	
	/**
	 * Funzione il cui scopo è gestire quasi qualunque tipo di errore di sincronizzazione con il seggio.
	 * Se si rendesse necessario il seggio può ordinare alla postazione di resettarsi nello stato attiva,
	 * eventualmente ricominciando da zero le operazioni di voto del votante.
	 * Non è permesso il reset se la postazione dovesse risultare non ancora attivata
	 * visto che le operazioni di mutua autenticazione non devono essere saltate,
	 * o offline (attualmente non viene mai settato questo stato e forse verrà proprio eliminato).
	 * @throws PEException Se l'operazione non dovesse avere successo.
	 */
	void resetPost() throws PEException {
		//Se la postazione è non attiva o offline torniamo senza effettuare modifiche
		List<State.StatePost> unresettableStates = List.of(StatePost.NON_ATTIVA, StatePost.OFFLINE, StatePost.DA_RIAVVIARE);
		if(unresettableStates.contains(post.getState()))
			throw CNNException.CNN_2("postazione");
		
		//Riportiamo la postazione allo stato attiva
		post.setState(StatePost.ATTIVA);
		post.resetInfo();
		
		//Aggiorniamo la view
		if(Constants.verbose)
			printWarning("Terminale Resettato", "La postazione è stata resettata, tornare al seggio per ricominciare le operazioni di voto");
		
		updateView();
	}

	void setNewState(InetAddress stationIp, StatePost newState) throws PEException {
		post.setState(newState);
		updateView();
		
		notifyStateToStation(stationIp);
	}
	
	void deactivatePost() throws PEException {
		if(post.getState().equals(StatePost.NON_ATTIVA))
			return;

		deactivatePostOps();
		
		if(Constants.verbose)
			printWarning("Terminale Disattivato", "L'urna è stata spenta, la postazione è stata riportata allo stato 'NON ATTIVA'");
		
		updateView();
	}
	
	/* --- Funzioni di utility --- */

	/**
	 * Funzione che permette di usare la postazione come una macchina a stati finiti.
	 * Ogni funzione che richiede di essere in un particolare stato richiama questa per il controllo.
	 * @param expectedState Stato della postazione in cui è necessario trovarsi.
	 * @return Vero o falso a seconda se il server è attivo e lo stato è quello corretto.
	 */
	public boolean correctState(StatePost expectedState) {
		StatePost state = post.getState();
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
	 * Funzione richiamata quando un badge è letto dal reader RFID.
	 * Verifica se il badge è quello corretto e se sono presenti le schede necessarie
	 * e in caso positivo notifica al seggio il cambio di stato in "in uso".
	 * @param badge Il badge letto dal reader RFID.
	 */
	private void badgeIn(String badge) {
		//Effettua le verifiche necessarie ed eventualmente procede al cambio di stato
		if(readBadge(badge)) {
			try {
				//Se tutto va bene notifichiamo il seggio
				notifyStateToStation(null);
			} catch (PEException e) {
				printError(e);
			}
		}
	}
	
	/**
	 * Funzione che aggiorna lo stato da associata a "in uso" se il badge presentato è quello corretto.
	 * @param newBadge Il badge presentato.
	 * @return Vero o falso a seconda se l'operazione ha avuto successo.
	 */
	private boolean readBadge(String newBadge) {
		//Se la postazione non è associata non effettuiamo alcuna operazione
		if(!correctState(StatePost.ASSOCIATA)) 
			return false;
		
		//Verifichiamo che il badge presentato corrisponda a quello atteso
		String correctBadge = post.getBadge();
		
		//Il valore -1 sta per badge non assegnato
		if(correctBadge.equals(Protocol.unassignedPost)) {
			printError("Errore di Associazione", "A questa postazione non è stato assegnato alcun badge");
			return false;
		}
		
		//Se i badge hanno valore diverso mostriamo un errore a schermo e non procediamo
		if(!newBadge.equals(correctBadge)) {
			String lastWrongBadge = post.getLastWrongBadge();
			if(lastWrongBadge == null || !newBadge.equals(post.getLastWrongBadge())) {
				post.setLastWrongBadge(newBadge);
				updateView();
			}
			
			if(Constants.postRfid && rfidReader != null)
				rfidReader.allowReading();
			
			return false;
		}
		
		//Se non sono stati inizializzati i vettori di schede vuote e schede compilate non procediamo
		if(!post.areBallotsSet(true)) {
			printError("Errore di Associazione", "Nessuna scheda di voto è stata associata a questa postazione");
			return false;
		}
		
		//Settiamo lo stato a "in uso"
		post.setState(StatePost.IN_USO);
		
		//Aggiorniamo la view
		updateView();
		
		return true;
	}
	
	/**
	 * Funzione che contatta l'urna per ottenere gli nonce necessari per inviare i voti.
	 * @return Gli nonce (cifrati) "sfida" dell'urna o null se qualcosa andasse storto.
	 */
	private void askForNonces() throws PEException {
		//Se la postazione non è "in uso" non effettuiamo alcuna operazione
		if(!correctState(StatePost.IN_USO) && !correctState(StatePost.VOTO_PENDING))
			throw DEVException.DEV_05();
		
		if(post.getVotesNonces() != null)
			return;
		
		String failure = "Invio voto fallito.";
		String solving = "Assicurarsi che sia questa postazione che l'urna siano connessi.";
		
		//Si recupera il vettore delle preferenze esprimibili delle schede
		int[] maxPreferences = post.getBallotsMaxPreferences();
		if(maxPreferences == null)
			throw DEVException.DEV_05();
		
		//Si prepara la richiesta all'urna inserendo la struttura delle preferenze
		Message request = new Message(Protocol.nonceReq);
		request.setElement("numPreferences", maxPreferences);
		
		//Ci connettiamo all'urna
		//Si invia la richiesta e si aspetta la risposta
		Message response;
		String recipientIp = post.getUrnIp().getHostAddress();
		try (Link link = createLink(post.getUrnIp(), post.getUrnPort(), null, failure+"\n"+solving)){
			if(link.isClosed())
				throw CNNException.CNN_3("urna", recipientIp);
			
			link.write(Protocol.nonceReq);
			link.write(request.toB64());
			
			if(!link.hasNextLine())
				throw CNNException.CNN_3("urna", recipientIp);
			
			response = (Message) Message.fromB64(link.read(), "urna");
		}
		catch (Exception e) {
			throw CNNException.CNN_1("urna", recipientIp, null);
		}
		
		//Si verifica che il messaggio di risposta sia corretto e abbia i campi richiesti
		String [] required = {"nonces"};
		Class<?>[] types = {String[][].class};
		response.verifyMessage(Protocol.nonceAck, required, types, "urna");
		
		//Gli nonce vengono prelevati dal messaggio e restituiti al chiamante
		String[][] encryptedNonces = response.getElement("nonces");
		post.setVotesNonces(encryptedNonces);
	}
	
	/**
	 * Funzione richiamata per cifrare i pacchetti di voto e inviarli al seggio.
	 * @param encryptedNonces Nonce cifrati inviati dall'urna come sfida per l'autenticazione.
	 */
	private void sendVotes() throws PEException {
		//Recuperiamo chiave pubblica, chiave di sessione e (una copia delle) schede compilate
		byte[] pubKey = post.getAccountantPublicKey();
		String sessionKey = post.getSessionKey();
		WrittenBallot[] encryptedBallots = post.getWrittenBallotsCopy();
		String[][] encryptedNonces = post.getVotesNonces();
		
		if(pubKey == null) 
			throw DEVException.DEV_03("la chiave pubblica");
		
		if(sessionKey == null)
			throw DEVException.DEV_03("la chiave di sessione");
		
		if(encryptedBallots == null)
			throw DEVException.DEV_05();
		
		if(encryptedNonces == null)
			throw DEVException.DEV_05();
		
		String failure = "Invio voto fallito";
		String solving = "Assicurarsi che sia questa postazione che il seggio siano connessi.";
		
		//Cifriamo le schede compilate
		try {
			int i = 0;
			for(WrittenBallot ballot : encryptedBallots) {
				ballot.encryptBallot(pubKey, encryptedNonces[i], sessionKey);
				i++;
			}
		}
		catch(Exception e) {		
			throw ENCException.ENC_1("i voti", e);
		}
		
		//Creiamo la richiesta con le schede cifrate
		Message request = new Message(Protocol.sendVoteToStation);
		request.setElement("encryptedBallots", encryptedBallots);
		
		//Ci connettiamo al seggio
		//Inviamo la richiesta e salviamo la risposta del seggio
		String received;
		String errMsg = failure + "\nImpossibile contattare il seggio all'ip: " + post.getStationIp() + ".\n" + solving;
		InetAddress stationIp = post.getStationIp();
		
		if(stationIp == null)
			throw DEVException.DEV_01();
		
		String recipientIp = stationIp.getHostAddress();
		try (Link link = createLink(stationIp, post.getStationPort(), null, errMsg)) {			
			if(link.isClosed())
				throw CNNException.CNN_3("seggio", recipientIp);
			
			link.write(Protocol.sendVoteToStation);
			link.write(request.toB64());
			
			if(!link.hasNextLine())
				throw CNNException.CNN_3("seggio", recipientIp);
			
			//TODO: diventare message
			received = link.read();
		}
		catch(Exception e) {
			throw CNNException.CNN_1("seggio", null, e);
		}
		
		//Se la risposta non è voto ricevuto, qualcosa è andato storto
		if(!received.equals(Protocol.votesReceivedAck)) {
			throw FLRException.FLR_0(null);
		}
		
		//Settiamo lo stato a voto inviato e eliminiamo le schede in memoria
		post.setState(StatePost.VOTO_INVIATO);
		post.setBallots(null);
		
		//Aggiorniamo la view
		updateView();
	}
	
	private void deactivatePostOps() {
		post.setState(StatePost.NON_ATTIVA);

		try {
			post.resetInfo();
		} catch (PEException ignored) { }
		
		stopUrnThread();
	}
	
	/* --- Funzioni da implementare in quanto TerminalController --- */
	
	@Override
	public String readCard(String card) {
		if(Constants.devMode) {
			if(card.equals("off")) {
				post.setSimulateOffline(true);
				return null;
			}
			
			if(card.equals("on")) {
				post.setSimulateOffline(false);
				return null;
			}
			
			if(card.equals("info")) {
				printWarning("Info Stato", String.valueOf(post.getState()));
				return null;
			}
			
			Random r = new Random(System.currentTimeMillis());
			
			if(card.equals("att")) {
				post.setState(State.StatePost.ATTIVA);
				
				try {
					post.resetInfo();
				} catch (PEException ignored) { }
				
				updateView();
				return null;
			}
			
			if(card.equals("ass")) {
				post.setState(State.StatePost.ASSOCIATA);
				if(r.nextInt() % 2 == 0)
					post.setBadgeID(String.valueOf(Math.abs(r.nextInt())));
				
				updateView();
				return null;
			}
			
			if(card.equals("uso")) {
				post.setState(State.StatePost.IN_USO);
				if(r.nextInt() % 2 == 0)
					post.setBadgeID(String.valueOf(Math.abs(r.nextInt())));
				
				updateView();
				return null;
			}
			
			if(card.equals("inv")) {
				post.setState(State.StatePost.VOTO_INVIATO);
				if(r.nextInt() % 2 == 0)
					post.setBadgeID(String.valueOf(Math.abs(r.nextInt())));
				
				updateView();
				return null;
			}
			
			if(card.equals("nonatt")) {
				post.setState(State.StatePost.NON_ATTIVA);
				try {
					post.resetInfo();
				} catch (PEException ignored) { }
				
				updateView();
				return null;
			}
		}
		
		try {
			if(!card.equals(CardReader.exit)) {
				this.badgeIn(card);
			}
		}
		catch(Exception e) {}
		
		return null;
	}
	
	@Override
	protected void invalidAuthentication() {
		if(post.getState() == StatePost.NON_ATTIVA)
			return;
		
		deactivatePostOps();

		try {
			notifyStateToStation(null);
		} catch (PEException e) {}
		
		printError("Autenticazione Non Valida", "L'urna non riconosce questa postazione come valida, è necessario effettuare nuovamente l'autenticazione.");
		updateView();
	}
	
	/**
	 * Verifica che l'ip dal quale è arrivato un messaggio il cui mittente dovrebbe essere l'urna appartenga effettivamente ad essa.
	 * @param ip 	L'ip da verificare.
	 * @return		True o false a seconda se l'ip appartiene all'urna.
	 */
	protected boolean verifyUrnIp(InetAddress ip) {
		if (ip.equals(post.getUrnIp()))
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
		if(post.getState() == StatePost.NON_ATTIVA)
			return;

		signalShutDown(post.getUrnIp(), Constants.portUrn);
		
		post.setState(StatePost.OFFLINE);
		try {
			notifyStateToStation(null);
		} catch (PEException e) {}
		
	}
	
	@Override
	public void setRfidReachable(boolean reachable) {
		if(reachable != post.isRfidReaderReachable()) {
			post.setRfidReaderReachable(reachable);
			updateView();
		}
	}
}
