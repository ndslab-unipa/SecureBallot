package urna;

import common.RPTemp;
import exceptions.DEVException;
import exceptions.PEException;
import model.*;
import urna.controller.UrnDB;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.fail;

public class TestDB extends UrnDB {

	private final InetAddress ipStation;
	private final ArrayList<InetAddress> ipSubStations;
	private final ArrayList<InetAddress> ipPosts;

	private ArrayList<String> stationsSessionKeyes;
	private ArrayList<String> subStationsSessionKeyes;
	private ArrayList<String> postsSessionKeyes;

	private ArrayList<EmptyBallot> ballots;
	private ArrayList<Person> voters;

	private int numVotes = 0;

	public TestDB(InetAddress ipStation, InetAddress[] Ips, InetAddress[] subStationsIps) throws PEException {
		super(null,null,null,null);

		this.ipStation = ipStation;
		this.ipPosts = new ArrayList<>();
		this.ipSubStations = new ArrayList<>();

		this.ipPosts.addAll(Arrays.asList(Ips));
		this.ipSubStations.addAll(Arrays.asList(subStationsIps));
	}

	private int findTerminalFromIp(Terminals.Type type, InetAddress ip) {

		switch(type) {
			case Station:
				if(ipStation.equals(ip)) {
					return 0;
				}
				break;
				
			case SubStation:
				int i = 0;
				for(InetAddress ipSubStation : ipSubStations) {
					if(ipSubStation.equals(ip)) {
						return i;
					}
					i++;
				}
				break;
				
			case Post:
				i = 0;
				for(InetAddress ipPost : ipPosts) {
					if(ipPost.equals(ip)) {
						return i;
					}
					i++;
				}
				break;
				
			default:
				fail();
		}
		return -1;
	}

	public void setBallots(ArrayList<EmptyBallot> ballots, ArrayList<Person> voters) {
		this.ballots = ballots;
		this.voters = voters;
	}

	@Override
	public void getTerminalsIPs(int procedureCode, int sessionCode, InetAddress ipStation, ArrayList<InetAddress> ipPosts, ArrayList<InetAddress> ipSubStations) throws PEException {
		//public void getTerminalsIPs(InetAddress ipStation, ArrayList<InetAddress> ipPosts, ArrayList<InetAddress> ipSubStations) {

		for(InetAddress ip : this.ipPosts) {
			ipPosts.add(ip);
		}

		for(InetAddress ip : this.ipSubStations) {
			ipSubStations.add(ip);
		}
	}

	@Override
	public String getStationIP(int procedureCode, int sessionCode, InetAddress ipTerminal, boolean isPost) {
		return ipStation.getHostName();
	}

	@Override
	public ArrayList<Person> searchPerson(int procedureCode, String similarFirstName, String similarLastName, int maxResults){
		//Necessario effettuare la copia perchè il controller consuna i risultati ricevuti dal DB.
		return new ArrayList<>(voters);
	}

	@Override
	public void storeVotes(int procedureCode, int sessionCode, Person voter, WrittenBallot[] ballots, InetAddress ipStation, InetAddress ipPost) {
		numVotes++;
	}

	public void setVoters(ArrayList<Person> voters) {
		this.voters = voters;
	}

	public int countVotes() {
		return numVotes;
	}

	@Override
	public EmptyBallot[] getEmptyBallots(int procedureCode) {
		return ballots.toArray(new EmptyBallot[0]);
	}

	/*@Override
	public Vector<String[]> getProcedures() {
		return null;
	}*/

	public void setSessionKeyes(ArrayList<String> stationsSessionKeyes, ArrayList<String> subStationsSessionKeyes, ArrayList<String> postsSessionKeyes) {
		this.stationsSessionKeyes = stationsSessionKeyes;
		this.subStationsSessionKeyes = subStationsSessionKeyes;
		this.postsSessionKeyes = postsSessionKeyes;

	}

	@Override
	public String getTerminalSessionKey(int procedureCode, int sessionCode, InetAddress ip, Terminals.Type type) {
		int index = findTerminalFromIp(type, ip);

		if(type == Terminals.Type.Post) {
			return postsSessionKeyes.get(index);
		}

		if(type == Terminals.Type.Station) {
			return stationsSessionKeyes.get(index);
		}

		if(type == Terminals.Type.SubStation) {
			return subStationsSessionKeyes.get(index);
		}
		
		return null;
	}

	@Override
	public void verifyVoteData(int procedureCode, int sessionCode, String voterID, WrittenBallot[] ballots,
			String ipStation, String ipPost) {}

	@Override
	public void registerNewVoter(int procCode, InetAddress ip, String id, String ln, String fn, String birthDate, int[] ballots) throws PEException {

	}

	//Metodi da verificare

	@Override
	public boolean checkLoginData(String user, String psw) throws PEException {
		return true;
	}

	@Override
	public byte[] getRSAKey(String key, String user, String psw) throws PEException {
		switch(key){
			case "PublicKey1":
				return RPTemp.getPublic();
			case "EncryptedPrivateKey2":
				return RPTemp.getPrivate();
			default:
				throw DEVException.DEV_0(null);
		}
	}

	@Override
	public ArrayList<Session> getSessions(String username) throws PEException {
		return null;
	}

	@Override
	public int getNumOfEligibleVoters(int procedureCode) {
		return 0;
	}

	@Override
	public int getNumOfVoted(int procedureCode) {
		return 0;
	}
}
