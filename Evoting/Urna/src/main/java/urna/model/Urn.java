package urna.model;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

import exceptions.DEVException;
import exceptions.PEException;
import model.AbstrModel;
import model.EmptyBallot;
import model.Session;
import model.Terminals;
import model.State.StateUrn;
import utils.Logger;

public class Urn extends AbstrModel {
	private final int port;
	private final int numConnection;
	private int procedureCode = -1, sessionCode = -1;
	private byte[] pub1_rp, pr2_rp;
	
	private final HashMap<InetAddress, ArrayList<ArrayList<Integer>>> voteNonces;
	private final HashMap<InetAddress, Integer> postsActivationNonces;
	private final HashMap<InetAddress, Integer> stationsActivationNonces;
	private final HashMap<InetAddress, Integer> substationsActivationNonces;
	
	private EmptyBallot[] procedureBallots;
	
	private ArrayList<Session> sessions = null;
	private StateUrn state = StateUrn.NON_ATTIVA;
	
	private final ArrayList<DummyTerminal> onlineTerminals = new ArrayList<>();
	
	private int eligibleVoters = -1;
	private int hasVoted = -1;
	
	public Urn(int port, int numConnection) {
		this.port = port;
		this.numConnection = numConnection;
		
		voteNonces = new HashMap<>();
		postsActivationNonces = new HashMap<>();
		stationsActivationNonces = new HashMap<>();
		substationsActivationNonces = new HashMap<>();
		logger = new Logger(true);
	}
	
	public void setSessionParameters(int procedureCode, int sessionCode, byte[] pub1, byte[] pr2) {
		this.procedureCode = procedureCode;
		this.sessionCode = sessionCode;
		pub1_rp = pub1;
		pr2_rp = pr2;
	}
	
	public void setVoteNonces(InetAddress ipPost, ArrayList<ArrayList<Integer>> postNonces) {
		synchronized (voteNonces){
			voteNonces.put(ipPost, postNonces);
		}
	}
	
	public ArrayList<ArrayList<Integer>> getVoteNonces(InetAddress ipPost) {
		ArrayList<ArrayList<Integer>> ballotNonces;

		synchronized (voteNonces){
			ballotNonces = voteNonces.get(ipPost);
			voteNonces.remove(ipPost);
		}
		
		return ballotNonces;
	}
	
	public void setActivationNonce(InetAddress ip, Terminals.Type type, int nonce) throws PEException {
		HashMap<InetAddress, Integer> nonces;

		switch(type) {
			case Station:
				nonces = stationsActivationNonces;
				break;
				
			case SubStation:
				nonces = substationsActivationNonces;
				break;
				
			case Post:
				nonces = postsActivationNonces;
				break;
				
			default:
				throw DEVException.DEV_04(type);
		}

		synchronized (nonces){
			nonces.put(ip, nonce);
		}
	}
	
	public Integer getActivationNonce(InetAddress ip, Terminals.Type type) throws PEException {
		Integer nonce;
		HashMap<InetAddress, Integer> nonces;

		switch(type) {
			case Station:
				nonces = stationsActivationNonces;
				break;
				
			case SubStation:
				nonces = substationsActivationNonces;
				break;
				
			case Post:
				nonces = postsActivationNonces;
				break;

			default:
				throw DEVException.DEV_04(type);
		}

		synchronized (nonces){
			nonce = nonces.get(ip);
			nonces.remove(ip);
		}

		return nonce;
	}
	
	public int getPort() { return this.port; }
	public int getNumConnections() { return this.numConnection; }
	public int getSessionCode() { return sessionCode; }
	public int getProcedureCode() { return procedureCode; }
	
	public byte[] getPublicKey1() { return pub1_rp; }
	public byte[] getPrivateKey2() { return pr2_rp;	}
	
	public void setProcedureBallots(EmptyBallot[] procedureBallots) { this.procedureBallots = procedureBallots; }
	public EmptyBallot[] getProcedureBallots() { return procedureBallots; }
	
	public StateUrn getState() { return state; }
	public void setState(StateUrn newState) { state = newState; }
	
	public void setSessions(ArrayList<Session> sessions) { this.sessions = sessions; }
	public ArrayList<Session> getSessions() { return sessions; }

	public Session getSession(int procCode, int sessionCode) {
		if (sessions == null)
			return null;
		
		for (Session s : sessions)
			if (s.getProcedureCode() == procCode && s.getCode() == sessionCode)
				return s;
		
		return null;
	}
	
	public void addOnlineTerminal(InetAddress ip, Terminals.Type type) { onlineTerminals.add(new DummyTerminal(ip, type)); }
	public ArrayList<DummyTerminal> getOnlineTerminals() { return onlineTerminals; }
	public void resetOnlineTerminals() { onlineTerminals.clear(); }
	
	public int getEligibleVoters() { return eligibleVoters; }
	public void setEligibleVoters(int voters) { eligibleVoters = voters; }
	
	public int getHasVoted() { return hasVoted; }
	public void setHasVoted(int hasVoted) { this.hasVoted = hasVoted; }
	public void increaseHasVoted() { hasVoted++; }
}
