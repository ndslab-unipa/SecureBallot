package postazione.model;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

import exceptions.DBException;
import exceptions.PEException;
import model.AbstrModel;
import model.ElectoralList;
import model.EmptyBallot;
import model.WrittenBallot;
import model.State.StatePost;
import utils.Constants;
import utils.Protocol;

public class Post extends AbstrModel {
	private InetAddress stationIp;
	private final int stationPort;
	
	private final int numConnections;
	
	private byte[] accountantPubKey;
	private String sessionKey;
	
	private String badgeID = Protocol.unassignedPost;
	private String lastWrongBadge = null;
	
	private HashMap<Integer, EmptyBallot> procedureBallots = null;
	
	private String[][] votesNonces = null;
	private EmptyBallot[] emptyBallots = null;
	private ArrayList<WrittenBallot> writtenBallots = null;
	private StatePost state = StatePost.NON_ATTIVA;
	
	// Booleano usato per testare il thread del seggio che fa polling sulle postazioni
	private boolean simulateOffline = false;
	
	public Post(InetAddress urnIp, int urnPort, int stationPort, int numConnections) {
		this.urnIp = urnIp;
		this.urnPort = urnPort;
		this.stationPort = stationPort;
		this.numConnections = numConnections;
	}
	
	public void resetInfo() throws PEException {
		setBallots(null);
		setVotesNonces(null);
		setBadgeID(Protocol.unassignedPost);
	}
	
	public int getStationPort() {
		return stationPort;
	}
	
	public void setStationIp(InetAddress ipStation) {
		this.stationIp = ipStation;
	}
	
	public InetAddress getStationIp() {
		if(Constants.devMode && simulateOffline)
			return null;
		
		return stationIp;
	}
	
	public String getBadge() {
		return this.badgeID;
	}
	
	public void setBadgeID(String badgeID) {
		this.badgeID = badgeID;
	}
	
	public StatePost getState() {
		return state;
	}
	
	public void setState(StatePost state) {
		this.state = state;
	}

	public int getNumConnections() {
		return numConnections;
	}

	/**
	 * Funzione richiamata quando un nuovo votante è assegnato alla postazione.
	 * I codici delle schede vengono adoperati per selezionare tra le schede della procedura di voto, quelle da mostrare al votante.
	 * @param codes Lista di codici delle schede da mostrare.
	 */
	public void setBallots(int[] codes) throws PEException {
		if(codes == null) {
			emptyBallots = null;
			writtenBallots = null;
			return;
		}
		
		emptyBallots = getEmptyBallots(codes);
		
		writtenBallots = new ArrayList<>();
		for(EmptyBallot ballot : emptyBallots) {
			writtenBallots.add(new WrittenBallot(ballot.getTitle(), ballot.getCode(), ballot.getMaxPreferences()));
		}
	}
	
	/**
	 * Funzione che verifica se una data combinazione numero scheda/nome lista/ID candidato esiste per la procedura attuale.
	 * @param ballotNum 	Posizione della scheda (relativamete a quelle mostrate al votante) da controllare.
	 * @param listName 		Nome della lista con la quale il candidato si sta portando per la scheda scelta.
	 * @param candidateID	ID del candidato.
	 * @return				Se la combinazione scheda/lista/candidato esiste realmente o meno.
	 */
	public boolean validCandidate(int ballotNum, String listName, String candidateID) {
		if(ballotNum >= emptyBallots.length) {
			return false;
		}
		
		ArrayList<ElectoralList> lists = emptyBallots[ballotNum].getLists();
		
		for(ElectoralList list : lists) {
			if(list.getName().equals(listName)) {
				return list.contains(candidateID);
			}
		}
		
		return false;
	}
	
	/**
	 * Verifica se una data opzione è effettivamente presente nella scheda selezionata.
	 * @param ballotNum			Posizione della scheda (relativamete a quelle mostrate al votante) da controllare.
	 * @param selectedOption	Opzione per la quale si vuole verificare la presenza.
	 * @return					Se l'opzione è realmente presente nella scheda o meno.
	 */
	public boolean validOption(int ballotNum, String selectedOption) {
		if(ballotNum >= emptyBallots.length) {
			return false;
		}
		
		ArrayList<String> options = emptyBallots[ballotNum].getOptions();
		
		for(String option : options) {
			if(selectedOption.equals(option)) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Aggiunge o rimuove una preferenza (candidato o opzione) alla scheda selezionata
	 * @param ballotNum		Posizione della scheda (relativamete a quelle mostrate al votante) a cui aggiungere o rimuovere la preferenza.
	 * @param preference	Preferenza (candidato o opzione) da aggiungere o rimuovere.
	 * @param add			Flag che indica se l'operazione è una aggiunzione (true) o una rimozione (false).
	 * @return				L'esito dell'operazione.
	 */
	public boolean setPreference(int ballotNum, String preference, boolean add) {
		WrittenBallot ballot = writtenBallots.get(ballotNum);
		
		return add ? ballot.addPreference(preference) : ballot.removePreference(preference);
	}
	
	public void setProcedureBallots(EmptyBallot[] ballots){
		procedureBallots = new HashMap<Integer, EmptyBallot>();
		
		for(EmptyBallot ballot : ballots) {
			procedureBallots.put(ballot.getCode(), ballot);
		}
	}
	
	/**
	 * Funzione che recupera dall'insieme di tutte le schede di procedura, solo quelle il cui codice appartiene alla lista passata per argomento.
	 * @param codes I codici delle schede richieste.
	 * @return		Un array contenente le schede richieste.
	 */
	private EmptyBallot[] getEmptyBallots(int[] codes) throws PEException{
		int len = codes.length;
		
		if(len == 0) {
			throw DBException.DB_10();
		}
		
		EmptyBallot[] ballots = new EmptyBallot[len];
		
		for(int i = 0; i < len; i++) {
			int code = codes[i];
			
			EmptyBallot ballot = procedureBallots.get(code);
			
			if(ballot == null) {
				throw DBException.DB_11(code);
			}
			
			ballots[i] = ballot;
		}
		
		return ballots;
	}
	
	public EmptyBallot[] getEmptyBallots(){
		return emptyBallots;
	}
	
	public ArrayList<WrittenBallot> getWrittenBallots(){
		return writtenBallots;
	}
	
	public WrittenBallot[] getWrittenBallotsCopy() {
		if(writtenBallots == null) {
			return null;
		}
		
		WrittenBallot[] wb = new WrittenBallot[writtenBallots.size()];
		
		int i = 0;
		for(WrittenBallot ballot : writtenBallots) {
			wb[i] = new WrittenBallot(ballot);
			i++;
		}
		
		return wb;
	}
	
	public boolean areBallotsSet(boolean strict) {
		if(strict) {
			return emptyBallots != null && writtenBallots != null;
		}
		
		return emptyBallots != null || writtenBallots != null;
	}
	
	public int[] getBallotsMaxPreferences(){
		if(writtenBallots == null) {
			return null;
		}
		
		int[] maxPreferences = new int[writtenBallots.size()];
		for (int i=0; i < writtenBallots.size(); i++)
			maxPreferences[i] = writtenBallots.get(i).getMaxPreferences();
		
		return maxPreferences;
	}
	
	public void setAccountantPublicKey(byte[] pubKey) {
		accountantPubKey = pubKey;
	}
	
	public byte[] getAccountantPublicKey() {
		return accountantPubKey;
	}
	
	public void setSessionKey(String sessionKey) {
		this.sessionKey = sessionKey;
	}
	
	public String getSessionKey() {
		return sessionKey;
	}

	/**
	 * @return the simulateOffline
	 */
	public boolean getSimulateOffline() {
		return simulateOffline;
	}

	/**
	 * @param simulateOffline the simulateOffline to set
	 */
	public void setSimulateOffline(boolean simulateOffline) {
		this.simulateOffline = simulateOffline;
	}

	/**
	 * @return the votesNonces
	 */
	public String[][] getVotesNonces() {
		return votesNonces;
	}

	/**
	 * @param votesNonces the votesNonces to set
	 */
	public void setVotesNonces(String[][] votesNonces) {
		this.votesNonces = votesNonces;
	}

	/**
	 * @return the lastWrongBadge
	 */
	public String getLastWrongBadge() {
		return lastWrongBadge;
	}

	/**
	 * @param lastWrongBadge the lastWrongBadge to set
	 */
	public void setLastWrongBadge(String lastWrongBadge) {
		this.lastWrongBadge = lastWrongBadge;
	}
}
