package seggio.aux.controller;

import java.net.InetAddress;
import java.util.ArrayList;

import controller.*;
import encryption.AES;
import encryption.NonceManager;
import exceptions.CNNException;
import exceptions.ENCException;
import exceptions.PEException;
import model.DummyPost;
import model.Message;
import model.Person;
import model.State;
import model.Terminals;
import model.State.StateSubStation;
import seggio.aux.model.SeggioAusiliario;
import utils.CfgManager;
import utils.Constants;
import utils.FileUtils;
import utils.Protocol;
import view.ViewInterface;
import view.viewmodel.VoterViewModel;

public class Controller extends TerminalController {
	private final SeggioAusiliario substation;

	public Controller(AbstrServer server, ViewInterface view, SeggioAusiliario seggio) {
		super(server, view, Terminals.Type.SubStation, false);
		this.substation = seggio;
	}

	public Controller(ViewInterface view, SeggioAusiliario auxStat) throws PEException {
		super(new Server(new Factory(), Constants.portSubStation, auxStat.getNumConnections(), Terminals.Type.SubStation), view, Terminals.Type.SubStation, Constants.auxStatRfid);
		this.substation = auxStat;
		
		if(!Constants.devMode)
			substation.setUrnIp(CfgManager.getValue("ips.cfg", "urn"));
	}
	
	/* --- Inizializzazione del Seggio Ausiliario --- */
	
	public void activate(String sessionKey) {
		if(!correctState(StateSubStation.NON_ATTIVO)) return;
		
		if(!FileUtils.isSessionKey(sessionKey)) {
			printError("Chiave di Sessione Errata!", "La chiave di sessione inserita non è una chiave valida.");
			return;
		}
		
		try {
			String encryptedNonce2 = askForUrnAuthentication(sessionKey);
			
			if(encryptedNonce2 != null) {
				Message bulkIn = authenticateToUrn(encryptedNonce2, sessionKey);
				
				String[] required = {"ipStation"};
				Class<?>[] types = {InetAddress.class};
				bulkIn.verifyMessage(Protocol.validAuthentication, required, types, "urna");
				
				InetAddress ipStation = bulkIn.getElement("ipStation");
				
				substation.setState(StateSubStation.IN_ATTESA);
				substation.setStationIp(ipStation);
				
				if(Constants.verbose)
					printSuccess("Attivazione Completata", "In Attesa del Seggio Principale");
				
				updateView();	

				urnThread = new UrnPolling(substation.getUrnIp(), this, Terminals.Type.SubStation);
				urnThread.start();
				
				if(Constants.auxStatRfid && rfidReader != null) {
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
	 * @throws PEException
	 */
	private String askForUrnAuthentication(String sessionKey) throws PEException {
		//Viene generato il nonce
		int nonce1 = NonceManager.genSingleNonce();
		//Il nonce viene cifrato
		String encryptedNonce1 = AES.encryptNonce(nonce1, sessionKey);
		
		Message response;
		String errMsg = "Attivazione fallita, impossibile contattare l'urna. Controllare che sia l'urna che questa postazione siano connessi alla rete.";
		String ipRecipient = substation.getUrnIp().getHostAddress();
		try (Link link = createLink(substation.getUrnIp(), substation.getUrnPort(), null, errMsg)) {
			if(link.isClosed()) {return null;}
			
			//Indichiamo di volere iniziare la mutua autenticazione
			link.write(Protocol.SubStationAuthenticationPhase1);
			//Inviamo il nostro nonce cifrato
			link.write(encryptedNonce1);
			
			if(!link.hasNextLine()) {
				//throw new PEException("L'urna ha interrotto la comunicazione.", Category.Connection);
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
	 * @throws PEException
	 * @returns response Il messaggio restituito dall'urna in seguito alla risposta alla sfida
	 */
	private Message authenticateToUrn(String encryptedNonce2, String sessionKey) throws PEException {
		
		//Si tenta di risolvere la sfida dell'urna ottenendo un nonce modificato e cifrato nuovamente.
		String encryptedModifiedNonce2 = NonceManager.solveChallenge(encryptedNonce2, sessionKey, 2);
		
		//Si contatta l'urna
		Message response;
		String errMsg = "Attivazione fallita, impossibile contattare l'urna. Controllare che sia l'urna che questa postazione siano connessi alla rete.";
		String ipRecipient = substation.getUrnIp().getHostAddress();
		try (Link link = createLink(substation.getUrnIp(), substation.getUrnPort(), null, errMsg)) {
			if(link.isClosed()) {return null;}
			
			//Indichiamo di volere procedere con la mutua autenticazione
			link.write(Protocol.SubStationAuthenticationPhase2);
			//Inviamo la nostra soluzione alla sfida lanciata dall'urna
			link.write(encryptedModifiedNonce2);
			
			if(!link.hasNextLine()) {
				//throw new PEException("L'urna ha interrotto la comunicazione.", Category.Connection);
				throw CNNException.CNN_3("urna", ipRecipient);
			}
			
			//L'urna invia la risposta
			response = (Message) Message.fromB64(link.read(), "urna");
		}
		catch (Exception e) {
			//throw new PEException("Impossibile comunicare con l'urna.", Category.Connection, e);
			throw CNNException.CNN_1("urna", ipRecipient, e);
		}
		
		return response;
	}
	
	/* --- Funzioni richiamate dai controller delle scene JavaFX --- */
	
	public SeggioAusiliario getSubStation() { 
		return substation; 
	}
	
	/**
	 * Funzione che invia all'urna il nome parziale dell'utente 
	 * perchè questa recuperi e restituisca tutti i potenziali match del database.
	 * @param searchFn 	Stringa simile al nome dell'utente.
	 * @param searchLn 	Stringa simile al cognome dell'utente.
	 * @return Un vettore contenente tutti i votanti della procedura con nome e cognome simile.
	 */
	public Person[] retrieveVotersByName(String searchFn, String searchLn) {
		if(!correctState(StateSubStation.ATTIVO)) {return null;}
		
		if((searchFn == null || searchFn.length() == 0) && (searchLn == null || searchLn.length() == 0)) {
			printError("Impossibile Effettuare la Ricerca", "Assicurati di aver specificato i criteri di ricerca.");
			return null;
		}
		
		//Viene creata la richiesta con nome e cognome simili
		Message request = new Message(Protocol.searchPersonSubStationReq);
		request.setElement("firstName", searchFn);
		request.setElement("lastName", searchLn);
		
		//Inviamo la richiesta e aspettiamo la risposta
		Message response;
		String errMsg = "Impossibile contattare l'urna.\n" + "Controllare che sia l'urna che questo seggio siano connessi alla rete.";
		try (Link link = createLink(substation.getUrnIp(), substation.getUrnPort(), null, errMsg)) {
			if(link.isClosed()) {return null;}
			
			link.write(Protocol.searchPersonSubStationReq);		
			link.write(request.toB64());
			
			if(!link.hasNextLine()) {
				throw CNNException.CNN_3("urna", substation.getUrnIp().getHostAddress());
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
		
		substation.setNewVoter(voter);
		updateView();
		return true;
	}
	
	/* --- Funzioni richiamate direttamente da Service --- */
	
	boolean verifyStationIp(InetAddress ip) {
		InetAddress stationIP = substation.getStationIp();
		
		if(stationIP == null) 
			return false;
		
		if(ip.equals(stationIP))
			return true;
		
		printError("Comunicazione Inattesa", "Tentativo di connessione da parte di " + ip.toString() + "(sconosciuto) come seggio.");
		return false;
	}
	
	protected void updateSubStation(DummyPost[] posts) throws PEException {
		State.StateSubStation currState = substation.getState();
		if(!currState.equals(StateSubStation.IN_ATTESA) && !currState.equals(StateSubStation.ATTIVO)) return;
		
		substation.update(posts);
		substation.setState(StateSubStation.ATTIVO);
		
		updateView();
	}
	
	void resetSubStation() {
		if(substation.getState().equals(State.StateSubStation.NON_ATTIVO)) 
			return;

		substation.setState(State.StateSubStation.IN_ATTESA);
		substation.update(null);
		
		if(Constants.verbose)
			printWarning("Terminale Resettato", "Il seggio è stato spento, il seggio ausiliario è stato riportato allo stato 'IN ATTESA'");
		
		updateView();
	}
	
	void deactivateSubStation() {
		if(substation.getState().equals(State.StateSubStation.NON_ATTIVO)) 
			return;

		resetSubStationOps();
		
		if(Constants.verbose)
			printWarning("Terminale Disattivato", "L'urna è stata spenta, il seggio ausiliario è stato riportato allo stato 'NON ATTIVO'");
		
		updateView();
	}
	
	/* --- Funzioni di utility --- */
	
	public boolean correctState(StateSubStation expectedState) {
		StateSubStation state = substation.getState();
		if(!state.equals(expectedState)) {
			printMessage("controller.correctState\nExpected " + expectedState + " but found " + state);
			return false;
		}
		
		if(!server.isRunning()) {
			printMessage("controller.correctState\nThe server is not running!");
			return false;
		}
		
		return true;
	}
	
	private void resetSubStationOps() {
		substation.setState(State.StateSubStation.NON_ATTIVO);
		substation.update(null);
		stopUrnThread();
	}
	
	/* --- Funzioni da implementare in quanto TerminalController --- */
	
	@Override
	public synchronized String readCard(String card) {
		if(!correctState(StateSubStation.ATTIVO)) {return "noOp";}

		if(card.equals(CardReader.exit)) {return null;}
		
		Person voter = substation.getNewVoter();
		
		Message bulkOut = new Message(Protocol.processCardReq);
		bulkOut.setElement("voter", voter);
		bulkOut.setElement("card", card);
		
		Message response;
		String errMsg = "Impossibile contattare il seggio. Controllare che sia il seggio che questo terminale siano connessi alla rete.";
		String ipRecipient = substation.getStationIp().getHostAddress();
		try (Link link = createLink(substation.getStationIp(), substation.getStationPort(), null, errMsg)) {
			if(link.isClosed()) return null;
			
			link.write(Protocol.processCardReq);
			link.write(bulkOut.toB64());
			
			if(!link.hasNextLine()) {
				throw CNNException.CNN_3("seggio principale", ipRecipient);
			}
			
			response = (Message) Message.fromB64(link.read(), "seggio principale");
		}
		catch(PEException e) {
			printError(e);
			return null;
		}
		catch(Exception e) {
			printError(CNNException.CNN_0(e));
			return null;
		}
		
		//TODO: Aggiustare sto schifo
		
		String outcome = response.getValue();
		
		if(outcome.equals(Protocol.associationAck)) {
			Integer post = response.getElement("post");
			if(post != null) {
				substation.setNewVoter(null);
				
				String associationMsg = "Postazione: " + post;
				if(Constants.verbose)
					associationMsg += "\nBadge: " + card + "\nVotante: " + voter.getLastName() + ", " + voter.getFirstName();
				printSuccess("Associazione Riuscita", associationMsg); 
				
				updateView();
				return outcome;
			}
		}
		
		if(outcome.equals(Protocol.votesReceivedAck)) {
			updateView();

			String details = response.getElement("details");
			if(details == null)
				details = "L'urna ha correttamente ricevuto il pacchetto di voto.";

			printSuccess("Voto Inviato con Successo", details);
			return outcome;
		}
		
		ArrayList<String> errors = response.getErrors();
		if(errors == null || errors.size() == 0) {
			printError("Errore", outcome);
		}
		else {
			String cumulativeError = "";
			for (String error : errors)
				cumulativeError += error + "\n";
			
			printError("Impossibile Creare Associazione", cumulativeError);
		}
		
		return Protocol.error;
	}
	
	@Override
	protected void invalidAuthentication() {
		if(substation.getState() == State.StateSubStation.NON_ATTIVO)
			return;
		
		resetSubStationOps();
		
		printError("Autenticazione Non Valida", "L'urna non riconosce questo seggio ausiliario come valido, è necessario effettuare nuovamente l'autenticazione.");
		updateView();
	}
	
	@Override
	protected boolean verifyUrnIp(InetAddress ip) {
		if (ip.equals(substation.getUrnIp()))
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
		if(substation.getState() == StateSubStation.NON_ATTIVO)
			return;

		signalShutDown(substation.getUrnIp(), Constants.portUrn);
	}
	
	@Override
	public void setRfidReachable(boolean reachable) {
		if(reachable != substation.isRfidReaderReachable()) {
			substation.setRfidReaderReachable(reachable);
			updateView();
		}
	}
}