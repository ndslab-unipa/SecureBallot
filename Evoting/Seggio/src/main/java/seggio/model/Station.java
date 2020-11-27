package seggio.model;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import model.AbstrModel;
import model.DummyPost;
import model.EmptyBallot;
import model.Person;
import model.State.StatePost;
import model.State.StateStation;
import utils.Protocol;
import model.WrittenBallot;

public class Station extends AbstrModel{
	private final int subStationPort;
	private final int postPort;
	private final int numConnections;
	private int associatedPost = -1;
	
	private StateStation state = StateStation.NON_ATTIVO;
	private DummyPost[] posts;

	private InetAddress[] ipSubStations = null;
	private Person newVoter = null;

	private String sessionKey;
	
	private EmptyBallot[] ballots;
	
	private boolean simulateOffline = false;
	
	/**
	 * Costruttore
	 */
	public Station(InetAddress urnIp, int urnPort, int subStationPort, int postPort, int numConnections) {
		this.urnIp = urnIp;
		this.urnPort = urnPort;
		this.subStationPort = subStationPort;
		this.postPort = postPort;
		this.numConnections = numConnections;
	}
	
	/**
	 * Funzione richiamata all'attivazione del seggio quando l'urna rende noti gli ip di postazioni e seggi ausiliari.
	 * @param ipPosts		Indirizzi ip delle postazioni.
	 * @param ipSubStations	Indirizzi ip dei seggi ausiliari.
	 */
	public void init(InetAddress[] ipPosts, InetAddress[] ipSubStations) {
		if (ipPosts == null && ipSubStations == null) {
			posts = null;
			this.ipSubStations = null;

			return;
		}
		
		int numPosts = ipPosts.length;

		//Creiamo un array di DummyPost per tenere traccia delle diverse postazioni
		posts = new DummyPost[numPosts];
		
		//Alla postazione n associo l'ID n+1
		for (int i = 0; i < numPosts; i ++)
			posts[i] = new DummyPost(i + 1, ipPosts[i]);
		
		//Salviamo gli ip dei seggi ausiliari
		this.ipSubStations = ipSubStations;
		
		//Settiamo lo stato del seggio ad attivo
		state = StateStation.ATTIVO;
	}

	/**
	 * Funzione adoperata per permettere alla view di accedere alle postazioni.
	 */
	public DummyPost[] getPosts(){
		return posts;
	}

	/**
	 * Funzione adoperata per sincronizzare l'associazione delle postazioni.
	 * In pratica adopera un monitor in modo che solo un seggio (principale o ausiliario) per volta possa creare una associazione.
	 * @return Vero o falso a seconda se una postazione è stata riservata, per cui il monitor è ancora in uso, o no, nel qual caso il il monitor è stato rilasciato.
	 */
	public synchronized boolean reservePost() {
		//Attendiamo che chi sta lavorando per creare una associazione termini
		try{
			while(associatedPost != -1) {
				wait();
			}
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//Recuperiamo tutte le postazioni disponibili per l'associazione (tutte quelle attive)
		ArrayList<Integer> possiblePosts = new ArrayList<>();
		for(int post = 0; post < posts.length; post++) {
			StatePost state = posts[post].getState();
			if(state.equals(StatePost.ATTIVA) && !posts[post].isUnreachable()) {
				possiblePosts.add(post);
			}
		}
		
		//Se non ci sono postazioni attive restituiamo false
		//per indicare che al momento non è possibile creare una nuova associazione
		int size = possiblePosts.size();
		if (size == 0) {
			return false;
		}
		
		//Scegliamo una delle postazioni disponibili a caso e
		//settiamola come postazione in procinto di essere associata
		//indicando in questo modo l'impossibilità di creare altre associazioni per il momento
		Random r = new Random(System.currentTimeMillis());
		associatedPost = possiblePosts.get(Math.abs(r.nextInt()) % size);
		
		return true;
	}
	
	/**
	 * Funzione da richiamare dopo aver terminato la creazione di una associazione, sia che questa sia riuscita o no,
	 * per liberare il monitor e permettere la creazione potenziale di altre associazioni.
	 */
	public synchronized void endPostReservation() {
		associatedPost = -1;
		notifyAll();
	}
	
	/**
	 * Memorizza una nuova associazione creata.
	 */
	public void setAssociation(Person voter, String badge, int index) {
		DummyPost post = posts[index];
		
		post.setVoter(voter);
		post.setBadge(badge);
		
		post.setState(StatePost.ASSOCIATA);
	}
	
	/**
	 * Distrugge l'associazione memorizzata relativa alla postazione indicata.
	 * @param index La postazione per cui eliminare l'associazione.
	 */
	public void destroyAssociation(int index) {
		DummyPost post = posts[index];

		post.setVoter(null);
		post.setBadge(Protocol.unassignedPost);
		post.setEncryptedBallots(null);
		
		post.setState(StatePost.ATTIVA);
	}
	
	/**
	 * Funzione che setta i valori memorizzati di una postazione quando si effettua un reset.
	 * @param index		La postazione per cui si è effettuato il reset.
	 * @param shutDown	Se true questo flag indica che la postazione è andata offline, non semplicemente resettata.
	 */
	public void resetPost(int index, boolean shutDown) {
		DummyPost post = posts[index];

		post.setBadge(Protocol.unassignedPost);
		post.setVoter(null);
		post.setEncryptedBallots(null);
		
		post.setState(shutDown ? StatePost.OFFLINE : StatePost.ATTIVA);
	}
	
	/*
	 * GETTER e SETTER vari
	 */

	/**
	 * Funzione che indica se un ip appartiene o meno ad un seggio ausiliario.
	 * @param ip	L'ip da verificare.
	 * @return		Vero o falso a seconda se l'ip appartiene ad un seggio ausiliario.
	 */
	public boolean isSubStation(InetAddress ip) {
		if(ipSubStations == null) {
			return false;
		}
		
		for(InetAddress ipSubStation : ipSubStations) {
			if(ipSubStation.equals(ip)) {
				return true;
			}
		}
		return false;
	}
	
	public int getPost(InetAddress ip){
		for(DummyPost post : posts){
			InetAddress ipPost = post.getIp();
			
			if(ipPost != null && ipPost.equals(ip)){
				return post.getId() - 1;
			}
		}
		
		return -1;
	}
	
	public int getPost(String badge) {
		if(badge.equals(Protocol.unassignedPost))
			return -1;
		
		for(DummyPost post : posts){
			String badgePost = post.getBadge();
			
			if(badgePost.equals(badge)){
				return post.getId() - 1;
			}
		}
		
		return -1;
	}
	
	public int getNumConnections() { return numConnections; }
	public StateStation getState() { return state; }
	public void setState(StateStation state) { this.state = state; }
	public InetAddress[] getSubStationIps() { return ipSubStations; }
	public int getSubStationPort() { return subStationPort; }
	
	public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }
	public String getSessionKey() { return sessionKey; }
	
	public void setEncryptedBallots(WrittenBallot[] encryptedBallots, int index) {
		DummyPost post = posts[index];
		
		post.setEncryptedBallots(encryptedBallots);
		post.setState(StatePost.VOTO_INVIATO);
	}
	
	public WrittenBallot[] getEncryptedBallots(int index) {
		return posts[index].getEncryptedBallots();
	}
	
	public int getNumPost() { return posts == null ? 0 : posts.length; }
	public int getAssociatedPost() { return associatedPost; }
	public int getPostPort() { return postPort; }
	
	public StatePost getPostState(int index) {
		return posts[index].getState() == null ? StatePost.OFFLINE : posts[index].getState();
	}

	public void setPostState(StatePost state, int index) {
		DummyPost post = posts[index];
		setPostState(state, post);
	}
	
	public void setPostState(StatePost state, DummyPost post) {
		post.setState(state);
		
		List<StatePost> votingStates = List.of(StatePost.ASSOCIATA, StatePost.IN_USO, StatePost.VOTO_INVIATO, StatePost.VOTO_PENDING);
		if(!votingStates.contains(state)) {
			post.setBadge(Protocol.unassignedPost);
			post.setEncryptedBallots(null);
			post.setVoter(null);
		}
	}

	public InetAddress getPostIp(int index) { return posts[index].getIp(); }
	public Person getPostVoter(int index) { return posts[index].getVoter(); }
	public String getPostBadge(int index) { return posts[index].getBadge(); }
	
	public void setNewVoter(Person voter) { newVoter = voter; }
	public Person getNewVoter() { return newVoter; }
	
	public void setDocumentType(String type) { this.newVoter.setDocumentType(type); }
	public Person.DocumentType getDocumentType() { return newVoter != null ? newVoter.getDocumentType() : null; }
	
	public void setDocumentID(String id) { this.newVoter.setDocumentID(id); }
	public String getDocumentID() { return newVoter != null ? newVoter.getDocumentID() : null; }
	
	public void setEmptyBallots(EmptyBallot[] ballots) { this.ballots = ballots; }
	public EmptyBallot[] getEmptyBallots() { return ballots; }

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
}
