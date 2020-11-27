package postazione;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import common.Settings;
import common.TestView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import common.Internet;
import common.RPTemp;
import controller.Link;
import encryption.AES;
import encryption.NonceManager;
import encryption.RandStrGenerator;
import exceptions.PEException;
import model.ElectoralList;
import model.EmptyBallot;
import model.Message;
import model.Person;
import model.VotePacket;
import model.WrittenBallot;
import model.State.StatePost;
import postazione.model.Post;
import utils.Protocol;

import static org.junit.Assert.*;

/**
 * Unit test for simple App.
 */
public class AppTest {	
	private final Internet internet = Internet.getInstance();
	
	private Post p;
	private ControllerP c;
	
	private Link pipe;
	
	private InetAddress ipPost;
	
	private InetAddress trueIpStation;
	private InetAddress falseIpStation;
	
	private InetAddress ipUrn;

	private TestView view = null;

	@Before
	public void setup() throws IOException, PEException {
		trueIpStation  = InetAddress.getByName("127.168.1.51");
		falseIpStation = InetAddress.getByName("127.168.1.115");
		
		ipPost = InetAddress.getByName("127.168.1.22");
		ipUrn = InetAddress.getByName("127.168.1.10");
		
		view = new TestView(Settings.viewBehaviour);
		
        p = new Post(ipUrn, 0, 0, 5);
        p.setStationIp(trueIpStation);
        p.setAccountantPublicKey(RPTemp.getPublic());
        
        c = new ControllerP(view, p, ipPost);
   	}
	
	@After
	public void dismantle() {
		if(pipe != null) {
			pipe.close();
		}
	}
	
	@Test
	public void activationTest() throws Exception {
		
		if(Settings.printTestName) {System.out.println("\nactivationTest");}

		assertTrue(c.correctState(StatePost.NON_ATTIVA));
        
        String sessionKey = RandStrGenerator.genSessionKey();
        
        pipe = internet.userRedirect(ipUrn);
        Link pipe2 = internet.userRedirect(ipUrn);
        
        Thread t = new Thread( () -> c.activate(sessionKey));
        
        EmptyBallot[] ballots = new EmptyBallot[1];
        ballots[0] = (new EmptyBallot("Ballot Title", 1, "Ballot Description", 1));
        
        t.start();
        
        assertEquals(Protocol.PostAuthenticationPhase1, pipe.waitNRead());
        String encryptedNonce1 = pipe.waitNRead();
        
        String encryptedModifiedNonce1 = NonceManager.solveChallenge(encryptedNonce1, sessionKey, 1);
        int nonce2 = NonceManager.genSingleNonce();
        String encryptedNonce2 = AES.encryptNonce(nonce2, sessionKey);
        
        Message bulkOut = new Message(Protocol.validAuthentication);
        bulkOut.setElement("nonce1", encryptedModifiedNonce1);
        bulkOut.setElement("nonce2", encryptedNonce2);
        
        pipe.write(bulkOut.toB64());
        pipe.close();
        
        assertEquals(Protocol.PostAuthenticationPhase2, pipe2.waitNRead());
        String encryptedModifiedNonce2 = pipe2.waitNRead();
        
        assertTrue(NonceManager.verifyChallenge(nonce2, encryptedModifiedNonce2, sessionKey, 2));
        
        bulkOut = new Message(Protocol.validAuthentication);
        bulkOut.setElement("ipStation", InetAddress.getByName("127.168.1.51"));
        bulkOut.setElement("pubKey", RPTemp.getPublic());
        bulkOut.setElement("ballots", ballots);
        
        pipe2.write(bulkOut.toB64());
        pipe2.close();
        
		t.join();

		assertTrue(c.correctState(StatePost.ATTIVA));
	}
	
	@Test
	public void 
    activationFailTest() throws InterruptedException, PEException {
		if(Settings.printTestName) {System.out.println("\nactivationFailTest");}

		assertTrue(c.correctState(StatePost.NON_ATTIVA));
        
		
        String correctSessionKey = RandStrGenerator.genSessionKey(), wrongSessionKey = RandStrGenerator.genSessionKey();
        
        //Errore autenticazione urna: l'urna invia un nonce cifrato con la chiave di sessione sbagliata.
        pipe = internet.userRedirect(ipUrn);
        
        Thread t = new Thread( () -> c.activate(correctSessionKey));
        
        t.start();
        
        assertEquals(Protocol.PostAuthenticationPhase1, pipe.waitNRead());
        String encryptedNonce1 = pipe.waitNRead();
        
        int decryptedNonce1 = AES.decryptNonce(encryptedNonce1, correctSessionKey);
        int modifiedNonce1 = NonceManager.getNonceResponse(decryptedNonce1, 1);
        String encryptedWithWrongPassModifiedNonce1 = AES.encryptNonce(modifiedNonce1, wrongSessionKey);
        
        int nonce2 = NonceManager.genSingleNonce();
        String encryptedNonce2 = AES.encryptNonce(nonce2, correctSessionKey);
        
        Message bulkOut = new Message();
        bulkOut.setValue(Protocol.validAuthentication);
        bulkOut.setElement("nonce1", encryptedWithWrongPassModifiedNonce1);
        bulkOut.setElement("nonce2", encryptedNonce2);
        
        pipe.write(bulkOut.toB64());
        pipe.close();
		
		t.join();

		assertTrue(c.correctState(StatePost.NON_ATTIVA));
		
		//Errore autenticazione postazione: la postazione invia un nonce cifrato con la chiave di sessione sbagliata.
		pipe = internet.userRedirect(ipUrn);
		Link pipe2 = internet.userRedirect(ipUrn);
		
		t = new Thread( () -> c.activate(correctSessionKey));
		
		t.start();
		
		assertEquals(Protocol.PostAuthenticationPhase1, pipe.waitNRead());
        encryptedNonce1 = pipe.waitNRead();
		
		String encryptedModifiedNonce1 = NonceManager.solveChallenge(encryptedNonce1, correctSessionKey, 1);
		String encryptedWithWrongPassModifiedNonce2 = AES.encryptNonce(nonce2, wrongSessionKey);
		
		bulkOut = new Message();
        bulkOut.setElement("nonce1", encryptedModifiedNonce1);
        bulkOut.setElement("nonce2", encryptedWithWrongPassModifiedNonce2);
		
		pipe.write(bulkOut.toB64());
		pipe.close();
		
		pipe2.close();
		
		t.join();
		
		
		
		
		
		
		
		
		
		/*assertEquals(true, c.correctState(StatePost.NON_ATTIVA));
        
		String sessionKey = RandStrGenerator.genSessionKey();
        
        //Messaggio non riconosciuto "abc"
        
        pipe = internet.userRedirect();
        
        Thread t = new Thread( () ->  {
        	c.activate(sessionKey);
        });
        
        t.start();
        
        assertEquals(Protocol.PostActivationRequest, pipe.waitNRead());
        
        pipe.write("abc");
        
        t.join();
        
        pipe.close();
        
        assertEquals(true, c.correctState(StatePost.NON_ATTIVA));
        
        //Richiesta rifiutata (non è presente alcuna risposta)
        
        Message bulk = new Message();

        pipe = internet.userRedirect();
        
        t = new Thread( () ->  {
        	c.activate(sessionKey);
        });
        
        t.start();
        
        assertEquals(Protocol.PostActivationRequest, pipe.waitNRead());
        
        pipe.write(bulk.toXML());
        
        t.join();
        
        pipe.close();
        
        assertEquals(true, c.correctState(StatePost.NON_ATTIVA));
        
        //Richiesta rifiutata (risposta non riconosciuta)
        
        bulk.setElement("response", "abc");

        pipe = internet.userRedirect();
        
        t = new Thread( () ->  {
        	c.activate(sessionKey);
        });
        
        t.start();
        
        assertEquals(Protocol.PostActivationRequest, pipe.waitNRead());
        
        pipe.write(bulk.toXML());
        
        t.join();
        
        pipe.close();
        
        assertEquals(true, c.correctState(StatePost.NON_ATTIVA));
        
        //Richiesta rifiutata esplicitamente
        
        bulk.setElement("response", Protocol.activationDenied);

        pipe = internet.userRedirect();
        
        t = new Thread( () ->  {
        	c.activate(sessionKey);
        });
        
        t.start();
        
        assertEquals(Protocol.PostActivationRequest, pipe.waitNRead());
        
        pipe.write(bulk.toXML());
        
        t.join();
        
        pipe.close();
        
        assertEquals(true, c.correctState(StatePost.NON_ATTIVA));
        
        //Richiesta accettata, dati mancanti.
        
        bulk.setElement("response", Protocol.activationGranted);

        pipe = internet.userRedirect();
        
        t = new Thread( () ->  {
        	c.activate(sessionKey);
        });
        
        t.start();
        
        assertEquals(Protocol.PostActivationRequest, pipe.waitNRead());
        
        pipe.write(bulk.toXML());
        
        t.join();
        
        pipe.close();
        
        assertEquals(true, c.correctState(StatePost.NON_ATTIVA));*/
        
	}
	
	@Test
	public void setAssociationTest() throws Exception {
		
		if(Settings.printTestName) {System.out.println("\nsetAssociationTest");}
		
		StatePost state = StatePost.ATTIVA;
		String badge = "5";
		//int tipoVotante = 1;
		EmptyBallot[] ballots = new EmptyBallot[1];
		ballots[0] = (new EmptyBallot("Scheda Test", 0, null, 0).addList(new ElectoralList("Lista Test").addPerson(new Person("Nome", "Cognome", null, "Codice"))));
		p.setProcedureBallots(ballots);
		p.setState(state);
		//p.setIpSeggio(InetAddress.getByName("127.168.1.51"));

		assertTrue(c.correctState(StatePost.ATTIVA));
		
        c.start();
        
        
        //Comunicazione errata o malevola (ip sbagliato) non comporta modifiche alla postazione.
        //c.testConnectToServer(pipe.getInput0(), pipe.getOutput0(), falseIpStation);
        pipe = internet.connectTo(falseIpStation, ipPost);
        
        int[] ballotCodes = {0};
        
        Message bulkOut = new Message();
        bulkOut.setValue(Protocol.associationReq);
        bulkOut.setElement("badge", badge);
        bulkOut.setElement("ballotCodes", ballotCodes);
        
        pipe.write(Protocol.associationReq);
        pipe.write(bulkOut.toB64());

		assertTrue(c.correctState(StatePost.ATTIVA));
        
        //Comuncazione reale (ip uguale a quello salvato dalla postazione), la postazione diventa associata.
       // c.testConnectToServer(pipe.getInput0(), pipe.getOutput0(), trueIpStation);
		pipe.close();
        pipe = internet.connectTo(trueIpStation, ipPost);
        
        pipe.write(Protocol.associationReq);
        pipe.write(bulkOut.toB64());

        Message bulkIn = (Message) Message.fromB64(pipe.waitNRead(), "postazione test");
        pipe.close();
        
        try {
        	bulkIn.verifyMessage(Protocol.associationAck, null, null, "test");
        }
        catch (Exception e) {
        	e.printStackTrace();
			fail();
        }

		assertTrue(c.correctState(StatePost.ASSOCIATA));
		
		c.shutDown();
	}
	
	@Test
	public void badgeInTest() throws InterruptedException, PEException {
		
		if(Settings.printTestName) {System.out.println("\nbadgeInTest");}
		
		internet.resetUserConnections();
		pipe = internet.userRedirect(trueIpStation);
		
		StatePost state = StatePost.ASSOCIATA;
		String badge = "5";
		int ballotCode = 0;
		int[] ballotsCodes = {ballotCode};
		
		EmptyBallot[] procedureBallots = new EmptyBallot[1];
		procedureBallots[0] = (new EmptyBallot(null, ballotCode, null, 2));
		
		
		p.setProcedureBallots(procedureBallots);
		
		p.setState(state);
		p.setBadgeID(badge);
		p.setBallots(ballotsCodes);

		assertTrue(c.correctState(StatePost.ASSOCIATA));
        
        c.start();
        
        Thread t = new Thread(() -> c.getCardReader().write(badge));
        
        t.start();
		
        assertEquals(Protocol.informStatePost, pipe.waitNRead());
        assertEquals(StatePost.IN_USO.toString(), pipe.waitNRead());
        
        pipe.close();
        
        t.join();

		assertTrue(c.correctState(StatePost.IN_USO));
		
		c.shutDown();
	}
	
	@Test
	public void sendVoteTest() throws PEException, InterruptedException {
		
		if(Settings.printTestName) {System.out.println("\nsendVoteTest");}
		
		StatePost state = StatePost.IN_USO;
		
		EmptyBallot[] ballots = new EmptyBallot[1];
		ballots[0] = (new EmptyBallot("Scheda Test", 0, null, 1).addList(new ElectoralList("Lista Test").addPerson(new Person("Nome", "Cognome", null, "Codice"))));
		
		int[] ballotCodes = new int[]{0};

		ArrayList<Integer> neededNonces = new ArrayList<>();
		neededNonces.add(1);
		
		String sessionKey = RandStrGenerator.genSessionKey();
		
		p.setState(state);
		p.setProcedureBallots(ballots);
		p.setBallots(ballotCodes);
		p.setSessionKey(sessionKey);

		assertTrue(c.correctState(StatePost.IN_USO));
        
        pipe = internet.userRedirect(ipUrn);
        Link pipe2 = internet.userRedirect(trueIpStation);
        
        Thread t = new Thread( () -> c.tryVoteSending());
        
        t.start();
        
        ArrayList<ArrayList<Integer>> plainNonces = NonceManager.genMultipleNonces(neededNonces);
        String[][] encryptedNonces = NonceManager.encryptMultipleNonces(plainNonces, sessionKey);
        
        Message bulk = new Message();
        bulk.setValue(Protocol.nonceAck);
        bulk.setElement("response", Protocol.success);
        bulk.setElement("nonces", encryptedNonces);
        
        assertEquals(Protocol.nonceReq, pipe.waitNRead());
        pipe.write(bulk.toB64());
        
		assertEquals(Protocol.sendVoteToStation, pipe2.waitNRead());
		
		Message message = (Message) VotePacket.fromB64(pipe2.waitNRead(), "postazione test");
		WrittenBallot[] encryptedBallots = message.getElement("encryptedBallots");

		assertEquals(plainNonces.size(), encryptedNonces.length);
		for(int i = 0; i < encryptedBallots.length; i++) {
			NonceManager.verifyMultipleNonces(plainNonces.get(i), encryptedBallots[i].getSolvedNonces(), sessionKey);
		}
		
		pipe2.write(Protocol.votesReceivedAck);
		
		t.join();

		assertTrue(c.correctState(StatePost.VOTO_INVIATO));

	}
	
	@Test
	public void badgeOutTest() {
		
		if(Settings.printTestName) {System.out.println("\nbadgeOutTest");}
		
		StatePost state = StatePost.VOTO_INVIATO;
		
		p.setState(state);
		
		c.start();
		
		//Comunicazione errata o malevola (ip sbagliato) non comporta modifiche alla postazione.
        pipe = internet.connectTo(falseIpStation, ipPost);

		assertTrue(c.correctState(state));
        pipe.write(Protocol.postEndVoteReq);
		assertTrue(c.correctState(state));
        
         pipe.close();
        
        //Comuncazione reale (ip uguale a quello salvato dalla postazione), la postazione diventa attiva.
        pipe = internet.connectTo(trueIpStation, ipPost);
        
        pipe.write(Protocol.postEndVoteReq);
        String test = pipe.waitNRead();
        assertEquals(Protocol.postEndVoteAck, test);

		assertTrue(c.correctState(StatePost.ATTIVA));
	}
	
	@Test
	public void resetPostTest() throws PEException {
		if(Settings.printTestName) {System.out.println("\nresetPostTest");}
		
		StatePost[] states = {StatePost.ATTIVA,
								StatePost.ASSOCIATA,
								StatePost.IN_USO,
								StatePost.VOTO_INVIATO,
								StatePost.NON_ATTIVA,
								StatePost.OFFLINE};

		c.start();
		
		for(StatePost state : states) {
			p.setState(state);
			
			p.setBadgeID("1");
			p.setProcedureBallots(new EmptyBallot[0]);
			p.setBallots(null);
			
			pipe = internet.connectTo(trueIpStation, ipPost);
					
			pipe.write(Protocol.resetPostReq);
			
			if(state.equals(StatePost.NON_ATTIVA) || state.equals(StatePost.OFFLINE)) {
				assertEquals(Protocol.resetPostDenied, pipe.waitNRead());
				
				assertEquals("1", p.getBadge());
				assertFalse(p.areBallotsSet(true));
				assertEquals(state, p.getState());
			}
			else {
				assertEquals(Protocol.resetPostGranted, pipe.waitNRead());
				
				assertEquals(Protocol.unassignedPost, p.getBadge());
				assertFalse(p.areBallotsSet(true));
				assertEquals(StatePost.ATTIVA, p.getState());
			}
			pipe.close();
		}
		
		
	}
	
}
