package urna;

import java.net.InetAddress;
import java.security.KeyPair;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import common.Settings;
import common.TestView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import common.Internet;
import common.RPTemp;
import controller.Link;
import db.ConnectionManager;
import db.DBMS;
import encryption.AES;
import encryption.Hash;
import encryption.KeyPairManager;
import encryption.NonceManager;
import encryption.RandStrGenerator;
import exceptions.DBException;
import exceptions.FLRException;
import exceptions.PEException;
import model.ElectoralList;
import model.EmptyBallot;
import model.Message;
import model.Person;
import model.Terminals;
import model.WrittenBallot;
import urna.controller.UrnDB;
import urna.model.Urn;
import utils.CfgManager;
import utils.Protocol;

import static org.junit.Assert.*;

/**
 * Unit test for simple App.
 */
public class AppTest {	
	Internet internet = Internet.getInstance();
	
	private Urn u;
	private ControllerU c;
	private TestDB db;
	
	private InetAddress[] ipPosts;
	private InetAddress[] ipSubStations;
	private InetAddress ipStation;
	private InetAddress ipUrn;
	
	private Link pipe;
	
	private final String host = "localhost";
	private final String port = "3306";
	private final String schema = "evotingDBTest";
	private DBMS manager;
	
	private final Person voterTest = new Person("voter", "test", "VT00", null, false);

	private TestView view = null;

	@Before
	public void setup() throws Exception {
		manager = new DBMS(host, port, schema, "Test");
		
		view = new TestView(Settings.viewBehaviour);
		
		ipPosts = new InetAddress[1];
		ipPosts[0] = InetAddress.getByName("127.0.0.1");
		
		ipSubStations = new InetAddress[0];
		
		ipStation = InetAddress.getByName("127.0.0.5");
		
		ipUrn = InetAddress.getByName("127.0.0.22");
		
		
		ArrayList<String> stationSessionKeyes = new ArrayList<>();
		stationSessionKeyes.add(RandStrGenerator.genSessionKey());
		
		ArrayList<String> postsSessionKeyes = new ArrayList<>();
		postsSessionKeyes.add(RandStrGenerator.genSessionKey());
		
		db = new TestDB(ipStation, ipPosts, ipSubStations);
		db.setSessionKeyes(stationSessionKeyes, null, postsSessionKeyes);
		
		u = new Urn(0, 5);
		c = new ControllerU(view, u, db, ipUrn);
        
		populateDB(true);
		
        c.start();
   	}
	
	@After
	public void dismantle() {
		if(pipe != null) {
			pipe.close();
		}
		c.shutDown();
	}
	
	@Test
	public void activationPostazioneTest() throws Exception {
		
		if(Settings.printTestName) {System.out.println("\nactivationPostazioneTest");}
		
		pipe = internet.connectTo(ipPosts[0], ipUrn);
		
		String sessionKey = db.getTerminalSessionKey(0, 0, ipPosts[0], Terminals.Type.Post);
		int nonce1 = NonceManager.genSingleNonce();
		String encryptedNonce1 = AES.encryptNonce(nonce1, sessionKey);
		int modifiedNonce1 = NonceManager.getNonceResponse(nonce1, 1);
		
        pipe.write(Protocol.PostAuthenticationPhase1);
        pipe.write(encryptedNonce1);
        
        Message bulk = (Message) Message.fromB64(pipe.waitNRead(), "urna test");
        pipe.close();
        
        String error = bulk.getElement("error");
        if(error != null) {
        	if(Settings.printTestName)
        		System.out.println(error);
			fail();
        }

        assertEquals(Protocol.validAuthentication, bulk.getValue());
        
        assertEquals(modifiedNonce1, AES.decryptNonce(bulk.getElement("nonce1"), sessionKey));
        String encryptedNonce2 = bulk.getElement("nonce2");
        
        String encryptedModifiedNonce2 = NonceManager.solveChallenge(encryptedNonce2, sessionKey, 2);

        pipe = internet.connectTo(ipPosts[0], ipUrn);
        
        pipe.write(Protocol.PostAuthenticationPhase2);
        pipe.write(encryptedModifiedNonce2);
        
        bulk = (Message) Message.fromB64(pipe.waitNRead(), "urna test");
        pipe.close();

        try {
        	String[] required = {"ipStation"};
        	Class<?>[] types = {InetAddress.class};
        	bulk.verifyMessage(Protocol.validAuthentication, required, types, "test");
        }
        catch(PEException e) {
			fail();
        }
        
        assertEquals(ipStation, bulk.getElement("ipStation"));

		assertNotNull(bulk.getElement("pubKey"));
		EmptyBallot[] ballots = bulk.getElement("ballots");
		assertNotNull(ballots);
		assertTrue(ballots.length != 0);
		
	}
	
	@Test
	public void activationSeggioTest() throws Exception {
		
		if(Settings.printTestName) {System.out.println("\nactivationSeggioTest");}
		
		pipe = internet.connectTo(ipStation, ipUrn);
		
		String sessionKey = db.getTerminalSessionKey(0, 0, ipStation, Terminals.Type.Station);
		int nonce1 = NonceManager.genSingleNonce();
		NonceManager.getNonceResponse(nonce1, 1);
		String encryptedNonce1 = AES.encryptNonce(nonce1, sessionKey);
		
        pipe.write(Protocol.StationAuthenticationPhase1);
		pipe.write(encryptedNonce1);
		
		Message bulkIn = (Message) Message.fromB64(pipe.waitNRead(), "urna test");
        pipe.close();
        
		try {
			String [] required = {"nonce1", "nonce2"};
			Class<?>[] types = {String.class, String.class};
			bulkIn.verifyMessage(Protocol.validAuthentication, required, types, "urna");
		}
		catch (PEException e) {
			fail();
		}
		
		String encryptedModifiedNonce1 = bulkIn.getElement("nonce1");
		String encryptedNonce2 = bulkIn.getElement("nonce2");
		
		assertTrue(NonceManager.verifyChallenge(nonce1, encryptedModifiedNonce1, sessionKey, 1));
		String encryptedModifiedNonce2 = NonceManager.solveChallenge(encryptedNonce2, sessionKey, 2);
		
		pipe = internet.connectTo(ipStation, ipUrn);
		
		pipe.write(Protocol.StationAuthenticationPhase2);
		pipe.write(encryptedModifiedNonce2);
		
		bulkIn = (Message) Message.fromB64(pipe.waitNRead(), "urna test");
		
		try {
			String [] required = {"posts", "subStations"};
			Class<?>[] types = {InetAddress[].class, InetAddress[].class};
			bulkIn.verifyMessage(Protocol.validAuthentication, required, types, "urna");
		}
		catch (PEException e) {
			fail();
		}
        
        InetAddress[] ipPostsReceived = bulkIn.getElement("posts");
        InetAddress[] ipSubStationsReceived = bulkIn.getElement("subStations");
        
        assertEquals(ipPosts.length, ipPostsReceived.length);
        for(int i = 0; i < ipPosts.length; i++) {
        	assertEquals(ipPosts[i], ipPostsReceived[i]);
        }
        
        assertEquals(ipSubStations.length, ipSubStationsReceived.length);
        for(int i = 0; i < ipSubStations.length; i++) {
        	assertEquals(ipSubStations[i], ipSubStationsReceived[i]);
        }
		
	}
	
	@Test
	public void receiveVoteTest() throws PEException {
		if(Settings.printTestName) {System.out.println("\nreceiveVoteTest");}
		
		u.addOnlineTerminal(ipPosts[0], Terminals.Type.Post);
		u.addOnlineTerminal(ipStation, Terminals.Type.Station);

		int[] numPreferences = {1, 2, 3};

		Message bulkOut = new Message(Protocol.nonceReq);
		bulkOut.setElement("numPreferences", numPreferences);
		
		pipe = internet.connectTo(ipPosts[0], ipUrn);
		pipe.write(Protocol.nonceReq);
		pipe.write(bulkOut.toB64());
		
		Message bulkIn = (Message) Message.fromB64(pipe.waitNRead(), "urna test");
		String[][] nonces = bulkIn.getElement("nonces");
		pipe.close();
		
		try {
			String[] required = {"nonces"};
			Class<?>[] types = {String[][].class};
			bulkIn.verifyMessage(Protocol.nonceAck, required, types, ipPosts[0].toString());
		}
		catch (PEException e) {
			e.printStackTrace();
			fail();
		}

		WrittenBallot[] encryptedBallots = new WrittenBallot[nonces.length];
		int i = 0;
		for(String[] ballotNonces : nonces) {
			int numPref = nonces[i].length;
			WrittenBallot wb = new WrittenBallot("Scheda test", i, numPref);
			
			for(int p = 0; p < numPref; p++) {
				wb.addPreference(Protocol.emptyPreference);
			}
			
			wb.encryptBallot(RPTemp.getPublic(), ballotNonces, db.getTerminalSessionKey(0, 0, ipPosts[0], Terminals.Type.Post));
			
			encryptedBallots[i] = wb;
			
			i++;
		}
		
		pipe = internet.connectTo(ipStation, ipUrn);
		
		Person voter = new Person("Test", "Test", "Test", new int[0], true);
		
        pipe.write(Protocol.sendVoteToUrn);
        
        bulkOut = new Message(Protocol.sendVoteToUrn);
        bulkOut.setElement("voter", voter);
        bulkOut.setElement("encryptedBallots", encryptedBallots);
        bulkOut.setElement("ipPost", ipPosts[0]);
        
        pipe.write(bulkOut.toB64());
        
        bulkIn = (Message) Message.fromB64(pipe.waitNRead(), "urna test");
        try {
        	bulkIn.verifyMessage(Protocol.votesReceivedAck, null, null, ipPosts[0].toString());
		}
		catch (PEException e) {
			fail();
		}
	}

	@Test
	public void realDBTest() throws Exception {
		if(Settings.printTestName) {System.out.println("\nrealDBTest");}
		
		if(Settings.testDB) {

			//Ricopiati qui perché dovrebbe pensare il server a settare questi parametri,
			//ma nei test non si adoperano Server ma TestServer
			System.setProperty("javax.net.ssl.keyStore", "ssl/keystore.jks");
			System.setProperty("javax.net.ssl.keyStorePassword", CfgManager.getPassword("ks"));

			System.setProperty("javax.net.ssl.trustStore", "ssl/truststore.jks");
			System.setProperty("javax.net.ssl.trustStorePassword", CfgManager.getPassword("ts"));


			emptyRealDB();			
			populateRealDB();
			
			UrnDB uDB = new UrnDB(host, port, schema, "Test");
			
			WrittenBallot wb0 = new WrittenBallot("Votable Ballot", 0, 1);
			WrittenBallot wb1 = new WrittenBallot("Non Votable Ballot", 1, 1);
			wb0.addPreference("Yes");
			wb1.addPreference("No");			
			
			WrittenBallot[] wbs0 = {wb0};
			
			InetAddress ipStation = InetAddress.getByName("127.0.0.1");
			InetAddress ipPost = InetAddress.getByName("127.0.0.2");
			
			int procedureCode = 0;
			int sessionCode = 0;
			
			String fakeID = "fakeID";
			
			String fail = null;
			
			try {
				uDB.verifyVoteData(procedureCode, sessionCode, fakeID, wbs0, ipStation.getHostAddress(), ipPost.getHostAddress());
				fail();
			}
			catch (PEException e) {
				fail = e.getMessage();
			}
			
			assertEquals("Non esiste alcuna sessione 0 relativa alla procedura 0", fail);
			
			insertSessionInRealDB(procedureCode, sessionCode);
			
			fakeID = "fakeID";
			
			try {
				uDB.verifyVoteData(procedureCode, sessionCode, fakeID, wbs0, ipStation.getHostAddress(), ipPost.getHostAddress());
			}
			catch (PEException e) {
				fail = e.getMessage();
			}
			
			assertEquals("Non risulta alcun votante con ID:" + fakeID, fail);
			
			try {
				uDB.verifyVoteData(procedureCode, sessionCode, voterTest.getID(), wbs0, ipStation.getHostAddress(), ipPost.getHostAddress());
				
			}
			catch (PEException e) {
				fail = e.getMessage();
			}
			
			String noSuchStation = DBException.DB_08(0, ipStation.getHostAddress()).getMessage();
			assertEquals(noSuchStation, fail);
			
			insertTerminalsInRealDB(procedureCode, sessionCode,0, ipStation, ipPost);
			
			InetAddress ipSecondStation = InetAddress.getByName("127.0.0.3");
			InetAddress ipSecondPost = InetAddress.getByName("127.0.0.4");
			assertNotEquals(ipPost, ipSecondPost);
			assertNotEquals(ipStation, ipSecondStation);
			
			try {
				uDB.verifyVoteData(procedureCode, sessionCode, voterTest.getID(), wbs0, ipStation.getHostAddress(), ipSecondPost.getHostAddress());
				
			}
			catch (PEException e) {
				fail = e.getMessage();
			}
			
			String noSuchPost = DBException.DB_08(2, ipSecondPost.getHostAddress()).getMessage();
			assertEquals(noSuchPost, fail);
			
			insertTerminalsInRealDB(procedureCode, sessionCode, 2, ipSecondStation, ipSecondPost);
			
			try {
				uDB.verifyVoteData(procedureCode, sessionCode, voterTest.getID(), wbs0, ipStation.getHostAddress(), ipSecondPost.getHostAddress());
			}
			catch (PEException e) {
				fail = e.getMessage();
			}
			
			String postDoNotBelongToStation = DBException.DB_09(ipSecondPost.getHostAddress(), ipStation.getHostAddress()).getMessage();
			assertEquals(postDoNotBelongToStation, fail);
			
			WrittenBallot[] wbs1 = {wb1};
			
			fail = FLRException.FLR_11(voterTest, 2).getMessage();
			
			String res = null;
			try {
				uDB.verifyVoteData(procedureCode, sessionCode, voterTest.getID(), wbs1, ipStation.getHostAddress(), ipPost.getHostAddress());
			}
			catch (PEException e) {
				res = e.getMessage();
			}

			assertEquals(fail, res);
			
			try {
				uDB.verifyVoteData(procedureCode, sessionCode, voterTest.getID(), wbs0, ipStation.getHostAddress(), ipPost.getHostAddress());
			}
			catch (PEException e) {
				fail();
			}
			
			try {
				voterTest.setDocumentType("Conoscenza Personale");
				uDB.storeVotes(procedureCode, sessionCode, voterTest, wbs0, ipStation, ipPost);
			}
			catch (PEException e) {
				fail();
			}
			
			fail = FLRException.FLR_11(voterTest, 0).getMessage();
			
			try {
				uDB.verifyVoteData(procedureCode, sessionCode, voterTest.getID(), wbs0, ipStation.getHostAddress(), ipPost.getHostAddress());
			}
			catch (PEException e) {
				res = e.getMessage();
			}
			
			assertEquals(fail, res);
			
			emptyRealDB();
		}
	}
	
	private void populateRealDB() throws Exception {

		try(ConnectionManager cManager = manager.getConnectionManager()) {
			String voterID = voterTest.getID();
			String supervisor = "supervisor";
			String password = "12345";
			
			int procedureCode = 0;

			insertSupervisorInRealDB(cManager, supervisor, password);
			insertProcedureInRealDB(cManager, procedureCode, supervisor);

			insertVoterInRealDB(cManager, procedureCode);
			insertBallotsInRealDB(cManager, voterID, procedureCode, supervisor);
			
		} catch (SQLException e) {
			e.printStackTrace();
			fail();
		}
	}
	
	private void insertVoterInRealDB(ConnectionManager cManager, int procedureCode) throws SQLException, PEException {
		
		String query = "INSERT INTO evotingDBTest.Voter(FirstName, LastName, ProcedureCode, ID) VALUES(?, ?, ?, ?) ; ";
		
		cManager.executeUpdate(query, voterTest.getFirstName(), voterTest.getLastName(), procedureCode, voterTest.getID());
		
		query = "SELECT * " +
				"FROM evotingDBTest.Voter AS V ;";
		
		ResultSet rs = cManager.executeQuery(query);
		
		boolean empty = true;
		while(rs.next()) {
			empty = false;
			assertEquals(rs.getString("V.FirstName"), voterTest.getFirstName());
			assertEquals(rs.getString("V.LastName"), voterTest.getLastName());
			assertEquals(rs.getString("V.ID"), voterTest.getID());
			
		}
		assertFalse(empty);
	}
	
	private void insertSessionInRealDB(int procedureCode, int sessionCode) throws PEException {
		
		ConnectionManager cManager = manager.getConnectionManager();
		
		String update = "INSERT INTO evotingDBTest.Session(ProcedureCode, Code, StartsAt, EndsAt) " + 
				"VALUES(?, ?, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 1 HOUR)) ;";

		cManager.executeUpdate(update, procedureCode, sessionCode);
		cManager.close();
		
	}
	
	private void insertSupervisorInRealDB(ConnectionManager cManager, String username, String password) throws Exception {
		
		byte[] hashedPassword = Hash.computeHash(password, 16, "password");
		
		byte[] publicKey, pr;
		KeyPair pair = KeyPairManager.genKeyPair();
		publicKey = pair.getPublic().getEncoded();
		pr = pair.getPrivate().getEncoded();
		
		byte[] encryptedPrivateKey = AES.encryptPrivateKey(pr, password);
		
		String update = "INSERT INTO Staff(UserName, Type, HashedPassword, PublicKey1, EncryptedPrivateKey1) VALUES(?, ?, ?, ?, ?)";
		
		cManager.executeUpdate(update, username, "Supervisor", hashedPassword, publicKey, encryptedPrivateKey);
		
	}

	private void insertProcedureInRealDB(ConnectionManager cManager, int procedureCode, String supervisor) throws PEException {



		String query = "INSERT INTO evotingDBTest.Procedure(Code, Supervisor, Name, Starts, Ends) VALUES(?, ?, 'Test Procedure', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 2 HOUR)) ; ";
		cManager.executeUpdate(query, procedureCode, supervisor);
	}

	private void insertBallotsInRealDB(ConnectionManager cManager, String ID, int procedureCode, String supervisor) throws SQLException, PEException {		
		/*String query = "INSERT INTO evotingDBTest.Procedure(Code, Supervisor, Name, Starts, Ends) VALUES(?, ?, 'Test Procedure', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 2 HOUR)) ; ";
		
		cManager.executeUpdate(query, procedureCode, supervisor);*/

		String query = "INSERT INTO evotingDBTest.Ballot(ProcedureCode, Code, Name, Description, MaxPreferences) VALUES(?, ?, ?, ?, ?) ; ";
		
		String[] name = {"Votable Ballot", "Non Votable Ballot"};
		String[] description = {"A test ballot that can be voted by the single test user.", "A test ballot that cannot be voted by the single test user."} ;
		int preferences = 1;
		
		for(int i = 0; i < 2; i++) {
			cManager.executeUpdate(query, procedureCode, i, name[i], description[i], preferences);
		}
		
		query = "INSERT INTO evotingDBTest.VoterBallotsList(ProcedureCode, BallotCode, VoterID) VALUES(?, ?, ?) ;" ;

		cManager.executeUpdate(query, procedureCode, 0, ID);
		
		query = "INSERT INTO evotingDBTest.ReferendumOption(ProcedureCode, BallotCode, Text) VALUES(?, ?, ?) ;" ;
		String[] texts = {"Yes", "No"};
		
		for(int i = 0; i < 2; i++) {
			
			for(String text : texts) {
				cManager.executeUpdate(query, procedureCode, i, text);
			}
			
		}
		
		query = "select * from evotingDBTest.Ballot as B;";
		ResultSet rs = cManager.executeQuery(query);
		
		int i = 0;
		while(rs.next()) {
			assertEquals(rs.getString("B.Name"), name[i]);
			assertEquals(rs.getString("B.Description"), description[i]);
			assertEquals(rs.getInt("B.MaxPreferences"), preferences);
			assertEquals(rs.getInt("B.Code"), i);
			
			i++;
		}
	}
	
	private void insertTerminalsInRealDB(int procedureCode, int sessionCode, int id, InetAddress ipStation, InetAddress ipPost) throws PEException{
		ConnectionManager cManager = manager.getConnectionManager();
		
		String query = "INSERT INTO evotingDBTest.Terminal(ProcedureCode, SessionCode, ID, IPAddress, Type) VALUES(?, ?, ?, ?, 'Station'), (?, ?, ?, ?, 'Post')";
		
		cManager.executeUpdate(query, procedureCode, sessionCode, id, ipStation.getHostAddress(), procedureCode, sessionCode, id+1, ipPost.getHostAddress());
		
		query = "INSERT INTO evotingDBTest.IsStationOf(ProcedureCode, SessionCode, Station, Terminal) VALUES(?, ?, ?, ?)";
		
		cManager.executeUpdate(query, procedureCode, sessionCode, id, id+1);
		cManager.close();
	}
	
	private void emptyRealDB() throws PEException {

		try (ConnectionManager cManager = manager.getConnectionManager()) {
			//Relazioni
			
			String query = "DELETE FROM evotingDBTest.IsStationOf ;";
			cManager.executeUpdate(query);
			
			query = "DELETE FROM evotingDBTest.VoterBallotsList ;";
			cManager.executeUpdate(query);
			
			query = "DELETE FROM evotingDBTest.HasVoted ;";
			cManager.executeUpdate(query);
			
			query = "DELETE FROM evotingDBTest.ReferendumOption ;";
			cManager.executeUpdate(query);

			query = "DELETE FROM evotingDBTest.Member ;";
			cManager.executeUpdate(query);

			query = "DELETE FROM evotingDBTest.ElectoralList ;";
			cManager.executeUpdate(query);

			query = "DELETE FROM evotingDBTest.Running ;";
			cManager.executeUpdate(query);

			query = "DELETE FROM evotingDBTest.IsStationOf ;";
			cManager.executeUpdate(query);

			query = "DELETE FROM evotingDBTest.SessionKey ;";
			cManager.executeUpdate(query);
			
			//Entità
			
			query = "DELETE FROM evotingDBTest.Voter ;";
			cManager.executeUpdate(query);
			
			query = "DELETE FROM evotingDBTest.Vote ;";
			cManager.executeUpdate(query);

			query = "DELETE FROM evotingDBTest.Candidate ;";
			cManager.executeUpdate(query);

			query = "DELETE FROM evotingDBTest.Ballot ;";
			cManager.executeUpdate(query);
			
			query = "DELETE FROM evotingDBTest.SessionKey ;";
			cManager.executeUpdate(query);

			query = "DELETE FROM evotingDBTest.Terminal";
			cManager.executeUpdate(query);
			
			query = "DELETE FROM evotingDBTest.Session ;";
			cManager.executeUpdate(query);
			
			query = "DELETE FROM evotingDBTest.Procedure ;";
			cManager.executeUpdate(query);
			
			query = "DELETE FROM evotingDBTest.Staff ;";
			cManager.executeUpdate(query);
		}
	}
	
	private void populateDB(boolean well) {
		Person p00 = new Person("P", "00", null, "p00");
		Person p01 = new Person("P", "01", null, "p01");
		Person p02 = new Person("P", "02", null, "p02");
		
		Person p10 = new Person("P", "10", null, "p10");
		Person p11 = new Person("P", "11", null, "p11");
		Person p12 = new Person("P", "12", null, "p12");
		
		Person p20 = new Person("P", "20", null, "p20");
		Person p21 = new Person("P", "21", null, "p21");
		Person p22 = new Person("P", "22", null, "p22");
		
		ArrayList<EmptyBallot> schede = new ArrayList<>();
		schede.add(new EmptyBallot("Scheda 0", 0, null, 1)
				.addList(new ElectoralList("Lista 0").addPerson(p00).end())
				.addList(new ElectoralList("Lista 1").addPerson(p01).end())
				.addList(new ElectoralList("Lista 2").addPerson(p02).end()));
		
		schede.add(new EmptyBallot("Scheda 1", 0, null, 1)
				.addList(new ElectoralList("Lista 0").addPerson(p10).end())
				.addList(new ElectoralList("Lista 1").addPerson(p11).end())
				.addList(new ElectoralList("Lista 2").addPerson(p12).end())
				);
		
		schede.add(new EmptyBallot("Scheda 2", 0, null, 1)
				.addList(new ElectoralList("Lista 0").addPerson(p20).end())
				.addList(new ElectoralList("Lista 1").addPerson(p21).end())
				.addList(new ElectoralList("Lista 2").addPerson(p22).end()));
		
		if(!well) {
			schede.add(new EmptyBallot("Scheda erronea", 0, null, 1)
					.addList(new ElectoralList("Lista 0").addPerson(p22).addPerson(p22))
					);
		}
		
		ArrayList<Person> voters = new ArrayList<>();
		
		db.setBallots(schede, voters);
		
		u.setProcedureBallots(db.getEmptyBallots(0));
	}
	
}