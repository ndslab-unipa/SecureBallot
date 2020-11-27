package seggio;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import common.Settings;
import common.TestView;
import model.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import common.Internet;
import common.RPTemp;
import controller.CardReader;
import controller.Link;
import encryption.AES;
import encryption.NonceManager;
import encryption.RandStrGenerator;
import encryption.VoteEncryption;
import exceptions.PEException;
import model.State.StatePost;
import model.State.StateStation;
import seggio.model.Station;
import utils.Protocol;

import static org.junit.Assert.*;

/**
 * Unit test for simple App.
 */
public class AppTest {	
	private Internet internet = Internet.getInstance();
	
	private Station s;
	private ControllerS c;
	
	private Link pipe;
	
	private final int numPosts = 3;

	private int nonce = 1515;
	private String sessionKey = "sessionKey";
	
	private InetAddress[] ipPosts;
	private InetAddress ipStation;
	private InetAddress ipUrn;
	
	WrittenBallot[] encryptedBallots;

	private TestView view = null;

	@Before
	public void setup() throws PEException, IOException {
		
		ipPosts = new InetAddress[numPosts];
		for(int i = 0; i < numPosts; i++) {
			ipPosts[i] = InetAddress.getByName("127.0.1." + (33 + i));
		}
		
		ipStation = InetAddress.getByName("127.0.0.5");
		ipUrn = InetAddress.getByName("127.0.0.10");
		
        s = new Station(ipUrn, 0, 0, 0, 5);
        s.init(ipPosts, new InetAddress[0]);
        
        view = new TestView(Settings.viewBehaviour);
		
        c = new ControllerS(view, s, ipStation);
        
        int numBallots = 5;
        encryptedBallots = new WrittenBallot[numBallots];
		for(int i = 0; i < numBallots; i++) {
			int pref = 3;
			
			WrittenBallot wb = new WrittenBallot("Scheda " + i, i, pref);
			
			String[] encryptedNonces = new String[pref];
			for(int p = 0; p < pref; p++) {
				wb.addPreference(Protocol.emptyPreference);
				encryptedNonces[p] = (AES.encryptNonce(nonce, sessionKey));
			}
			
			wb.encryptBallot(RPTemp.getPublic(), encryptedNonces, sessionKey);
			
			encryptedBallots[i] = wb;
		}
                
   	}
	
	@After
	public void dismantle() {
		pipe.close();
	}
	
	@Test
	public void activationTest() throws InterruptedException, PEException {
		
		if(Settings.printTestName) {System.out.println("\nactivationTest");}
		
		s = new Station(ipUrn, 0, 0, 0, 5);
		
		c = new ControllerS(view, s, ipStation);

		assertTrue(c.correctState(StateStation.NON_ATTIVO));
        
        String sessionKey = RandStrGenerator.genSessionKey();
        
        pipe = internet.userRedirect(ipUrn);
        Link pipe2 = internet.userRedirect(ipUrn);
        
        Thread t = new Thread( () -> c.activate(sessionKey));
        
        t.start();
        
        assertEquals(Protocol.StationAuthenticationPhase1, pipe.waitNRead());
        String encryptedNonce1 = pipe.waitNRead();
        
        String encryptedModifiedNonce1 = NonceManager.solveChallenge(encryptedNonce1, sessionKey, 1);
        int nonce2 = NonceManager.genSingleNonce();
        String encryptedNonce2 = AES.encryptNonce(nonce2, sessionKey);
        
        Message bulkOut = new Message();
        bulkOut.setValue(Protocol.validAuthentication);
        bulkOut.setElement("response", Protocol.validAuthentication);
        bulkOut.setElement("nonce1", encryptedModifiedNonce1);
        bulkOut.setElement("nonce2", encryptedNonce2);
        
        pipe.write(bulkOut.toB64());
        
        pipe.close();
        
        assertEquals(Protocol.StationAuthenticationPhase2, pipe2.waitNRead());
        String encryptedModifiedNonce2 = pipe2.waitNRead();
        
        assertTrue(NonceManager.verifyChallenge(nonce2, encryptedModifiedNonce2, sessionKey, 2));
        
        bulkOut = new Message();
        bulkOut.setValue(Protocol.validAuthentication);
        bulkOut.setElement("posts", ipPosts);
        bulkOut.setElement("subStations", new InetAddress[0]);
		bulkOut.setElement("ballots", new EmptyBallot[0]);
        pipe2.write(bulkOut.toB64());
        pipe2.close();
		
		t.join();

		assertTrue(c.correctState(StateStation.ATTIVO));
	}
	
	@Test
	public void createAssociationTest() throws Exception {
		
		if(Settings.printTestName) {System.out.println("\ncreateAssociationTest");}
		
		String voterName = "Cesare";
		
		int post = 1;
		String card = "123";
		
		
		c.start();
		pipe = internet.connectTo(ipPosts[post], ipStation);
		
		StatePost statePost = StatePost.ATTIVA;
		pipe.write(Protocol.informStatePost);
		pipe.write(statePost.toString());
		
		pipe.hasNextLine();
		//assertEquals(statePost.toString(), pipe.waitNRead());

		assertTrue(c.correctState(StateStation.ATTIVO));
		assertEquals(statePost, s.getPostState(post));
		
		pipe.close();
		pipe = internet.userRedirect(ipPosts[post]);
		
		int[] ballotCodes = {0};
		
        Thread t = new Thread( () ->  {
        	Person voter = new Person(voterName, voterName, voterName, ballotCodes, true);
        	voter.setDocumentType("Conoscenza Personale");
        	s.setNewVoter(voter);
        	//c.createAssociation();
        	c.getCardReader().write(card);
        });
        
        t.start();
		
        assertEquals(Protocol.associationReq, pipe.waitNRead());
        
        Message bulkIn = (Message) Message.fromB64(pipe.waitNRead(), "seggio test");
        
        String badge = bulkIn.getElement("badge");
        int[] ballotCodesReceived = bulkIn.getElement("ballotCodes");
        
        assertEquals(ballotCodes.length, ballotCodesReceived.length);
        for(int i = 0; i < ballotCodes.length; i++) {
        	assertEquals(ballotCodes[i], ballotCodesReceived[i]);
        }
        
        Message bulkOut = new Message(Protocol.associationAck);
        //bulkOut.setElement("response", Protocol.associationAck);
        
		pipe.write(bulkOut.toB64());
		
		t.join();
		
		assertEquals(s.getPostBadge(post), badge);

		assertTrue(c.correctState(StateStation.ATTIVO));
	}
	
	@Test
	public void setVoteTest() {
		
		if(Settings.printTestName) {System.out.println("\nsetVoteTest");}
		
		String voterName = "Cesare";
		
		for(int post = 0; post < numPosts; post++) {
			s.setPostState(StatePost.ATTIVA, post);
		}
		
		Person voter = new Person(voterName, voterName, voterName, new int[0], true);
		
		assertTrue(s.reservePost());
		
		int post = s.getAssociatedPost();
		String badge = "15";
		
		s.setAssociation(voter, badge, post);
		
		s.setPostState(StatePost.IN_USO, post);

		assertTrue(c.correctState(StateStation.ATTIVO));
        
        c.start();
        pipe = internet.connectTo(ipPosts[post], ipStation);
        
        pipe.write(Protocol.sendVoteToStation);
        
        Message bulk = new Message(Protocol.sendVoteToStation);
        bulk.setElement("encryptedBallots", encryptedBallots);
        
        pipe.write(bulk.toB64());
        
        assertEquals(Protocol.votesReceivedAck, pipe.waitNRead());

		assertTrue(c.correctState(StateStation.ATTIVO));
        
        c.shutDown();
	}
	
	@Test
	public void sendVoteAndNotifyPostazioneTest() throws Exception {
		
		if(Settings.printTestName) {System.out.println("\nsendVoteAndNotifyPostazioneTest");}
		
		Person voter = new Person("Gaio", "Giulio", "Cesare", new int[0], true);
		
		for(int post = 0; post < numPosts; post++) {
			s.setPostState(StatePost.ATTIVA, post);
		}
		
		assertTrue(s.reservePost());
		
		int post = s.getAssociatedPost();
		String badge = "12";
		
		s.setAssociation(voter, badge, post);
		
		s.setEncryptedBallots(encryptedBallots, post);

		assertTrue(c.correctState(StateStation.ATTIVO));
		
        pipe = internet.userRedirect(ipUrn);
        Link pipe2 = internet.userRedirect(ipPosts[post]);
        Link pipe3 = internet.userRedirect(ipPosts[post]);
        
        Thread t = new Thread( () ->  {
        	CardReader cardReader = c.getCardReader();
        	assertEquals(Protocol.votesReceivedAck, cardReader.write(badge));
        	cardReader.endWrite();
        	
        });
        
        c.start();
        
        t.start();

		assertEquals(Protocol.sendVoteToUrn, pipe.waitNRead());
		
		String message = pipe.waitNRead();
		Message bulk = (Message) Message.fromB64(message, "seggio test");
		
		try {
			String[] required = {"voter", "encryptedBallots"};
			Class<?>[] types = {Person.class, WrittenBallot[].class};
			bulk.verifyMessage(Protocol.sendVoteToUrn, required, types, "urna");
		}
		catch (PEException e) {
			fail();
		}
		
		Person voterReceived = bulk.getElement("voter");
		WrittenBallot[] encrBallots = bulk.getElement("encryptedBallots");
		
		assertEquals(voter.getFirstName(), voterReceived.getFirstName());
		assertEquals(voter.getLastName(), voterReceived.getLastName());
		assertEquals(voter.getID(), voterReceived.getID());

		assertEquals(encryptedBallots.length, encrBallots.length);
		
		for(int i = 0; i < encryptedBallots.length; i++) {
			/*ArrayList<String[]> preferences = encryptedBallots.get(i).getEncryptedPreferences();
			ArrayList<String[]> prefs = encrBallots.get(i).getEncryptedPreferences();*/
			ArrayList<VotePacket> packets = encryptedBallots[i].getEncryptedVotePackets();
			ArrayList<VotePacket> pcks = encrBallots[i].getEncryptedVotePackets();
			
			assertEquals(packets.size(), pcks.size());
			
			for(int j = 0; j < packets.size(); j++) {
				assertEquals(packets.get(j).getEncryptedVote(), pcks.get(j).getEncryptedVote());
				assertEquals(packets.get(j).getEncryptedKi(), pcks.get(j).getEncryptedKi());
				assertEquals(packets.get(j).getEncryptedIV(), pcks.get(j).getEncryptedIV());
			}
		}
		
		for(WrittenBallot b : encryptedBallots) {
			for(VotePacket encryptedVote : b.getEncryptedVotePackets()) {
				assertEquals(Protocol.emptyPreference, VoteEncryption.decrypt(encryptedVote, RPTemp.getPrivate()));
			}
		}
		
		//pipe.write(Protocol.voteReceived);
		pipe.write(new Message(Protocol.votesReceivedAck).toB64());
		
		assertEquals(Protocol.postEndVoteReq, pipe2.waitNRead());
		
		pipe3.write(Protocol.postEndVoteAck);
		
		pipe2.close();
		
		pipe3.close();
		
		t.join();
		
		assertTrue(c.correctState(StateStation.ATTIVO));
		
		c.shutDown();
	}
	
	@Test
	public void resetPostTest() throws InterruptedException {
		
		if(Settings.printTestName) {System.out.println("\nresetPostTest");}
		
		int post = 0;
		s.setPostState(StatePost.VOTO_INVIATO, post);
		
		pipe = internet.userRedirect(ipPosts[post]);
		
		Thread t = new Thread(()-> c.resetPost(post));
		
		t.start();
		
		assertEquals(Protocol.resetPostReq, pipe.waitNRead());
		
		pipe.write(Protocol.resetPostGranted);
		
		t.join();
		
		assertEquals(StatePost.ATTIVA, s.getPostState(post));
	}
	
	@Test
	public void findVotersTest() throws Exception {
		
		if(Settings.printTestName) {System.out.println("\nfindVotersTest");}
		
		internet.resetUserConnections();
		pipe = internet.userRedirect(ipUrn);
		
		String firstName = "Pippo";
		String lastName = "Civati";
		
		int numVoters = 3;
		Person[] voters0 = new Person[numVoters];
		for(int i = 0; i < 3; i++) {
			voters0[i] = (new Person(firstName, lastName, Integer.toString(i), new int[0], (i%2 == 0)));
		}
		
		Thread t = new Thread(() -> {
			
			Person[] voters1 = c.retrieveVotersByName(firstName, lastName);
			
			assertEquals(voters0.length, voters1.length);
			
			for(int i = 0; i < voters0.length; i++) {
				Person voter0 = voters0[i];
				Person voter1 = voters1[i];
				assertEquals(voter0.getFirstName(), voter1.getFirstName());
				assertEquals(voter0.getLastName(), voter1.getLastName());
				assertEquals(voter0.getID(), voter1.getID());
				assertEquals(voter0.mayVote(), voter1.mayVote());
			}
			
		});
		
		t.start();
		
		assertEquals(Protocol.searchPersonReq, pipe.waitNRead());
		
		Message bulkIn = (Message) Message.fromB64(pipe.waitNRead(), "seggio test");
		
		try {
			String[] required = {"firstName", "lastName",};
			Class<?>[] types = {String.class, String.class};
			bulkIn.verifyMessage(Protocol.searchPersonReq, required, types, "urna");
		}
		catch(PEException e) {
			e.printStackTrace();
			fail();
		}
		
		assertEquals(firstName, bulkIn.getElement("firstName"));
		assertEquals(lastName, bulkIn.getElement("lastName"));
		
		Message bulk1 = new Message(Protocol.searchPersonReq);
		bulk1.setElement("voters", voters0);
		bulk1.setElement("missingVoters", false);
		
		pipe.write(bulk1.toB64());
		pipe.write(Protocol.searchPersonReq);
		
		t.join();
		
	}
	
}













