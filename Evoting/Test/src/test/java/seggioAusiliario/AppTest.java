package seggioAusiliario;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import common.Settings;
import common.TestView;
import model.DummyPost;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import common.Internet;
import controller.Link;
import encryption.AES;
import encryption.NonceManager;
import encryption.RandStrGenerator;
import exceptions.PEException;
import model.Message;
import model.Person;
import model.State.StatePost;
import model.State.StateSubStation;
import seggio.aux.model.SeggioAusiliario;
import utils.Constants;
import utils.Protocol;

/**
 * Unit test for simple App.
 */
public class AppTest 
{	
	
	private Internet internet = Internet.getInstance();
	
	private SeggioAusiliario s;
	private ControllerSubS c;
	
	private Link pipe;

	InetAddress ipPost;
	InetAddress ipSubStation;
	InetAddress ipStation;
	InetAddress ipUrn;

	private TestView view = null;

	@Before
	public void setup() throws IOException {

		ipPost = InetAddress.getByName("127.0.0.8");
		ipSubStation = InetAddress.getByName("127.0.20.111");
		ipStation = InetAddress.getByName("127.0.0.15");
		ipUrn = InetAddress.getByName("127.0.0.30");
		
		s = new SeggioAusiliario(ipUrn, Constants.portUrn, Constants.portStation, 5);
		
		view = new TestView(Settings.viewBehaviour);
		
		c = new ControllerSubS(view, s, ipSubStation);
		c.start();
   	}
	
	@After
	public void dismantle() {
		pipe.close();
		
		c.shutDown();
	}
	
	
	@Test
	public void sendVoteTest() throws Exception {
		
		if(Settings.printTestName) {System.out.println("\nsendVoteTest");}
		
		s.setState(StateSubStation.ATTIVO);
		s.setStationIp(ipStation);

		Person voter = new Person("firstName", "lastName", "ID", new int[0], true);
		voter.setDocumentType("Conoscenza Personale");
		s.setNewVoter(voter);
		DummyPost[] posts = {new DummyPost(0, ipPost)};
		s.update(posts);

		String card = "1";
		
		pipe = internet.userRedirect(ipStation);
		
		Thread t = new Thread(() -> assertEquals(Protocol.votesReceivedAck, c.getCardReader().write(card)));
		
		t.start();
		
		assertEquals(Protocol.processCardReq, pipe.waitNRead());
		
		Message bulkIn = (Message) Message.fromB64(pipe.waitNRead(), "seggio ausiliario test");
		
		assertEquals(card, bulkIn.getElement("card"));
		
		Message bulkOut = new Message(Protocol.votesReceivedAck);
		
		pipe.write(bulkOut.toB64());
		
		t.join();
		
	}
	
	@Test
	public void activationTest() throws InterruptedException, PEException, UnknownHostException {
		
		if(Settings.printTestName) {System.out.println("\nactivationTest");}
		
		assertEquals(StateSubStation.NON_ATTIVO, s.getState());
		
		String sessionKey = RandStrGenerator.genSessionKey();
				
		pipe = internet.userRedirect(ipUrn);
        Link pipe2 = internet.userRedirect(ipUrn);
        
        Thread t = new Thread( () -> c.activate(sessionKey));
        
        t.start();
        
        assertEquals(Protocol.SubStationAuthenticationPhase1, pipe.waitNRead());
        String encryptedNonce1 = pipe.waitNRead();
        
        String encryptedModifiedNonce1 = NonceManager.solveChallenge(encryptedNonce1, sessionKey, 1);
        int nonce2 = NonceManager.genSingleNonce();
        String encryptedNonce2 = AES.encryptNonce(nonce2, sessionKey);
        
        Message bulkOut = new Message();
        bulkOut.setValue(Protocol.validAuthentication);
        bulkOut.setElement("nonce1", encryptedModifiedNonce1);
        bulkOut.setElement("nonce2", encryptedNonce2);
        
        pipe.write(bulkOut.toB64());
        pipe.close();
        
        assertEquals(Protocol.SubStationAuthenticationPhase2, pipe2.waitNRead());
        String encryptedModifiedNonce2 = pipe2.waitNRead();
        
        assertTrue(NonceManager.verifyChallenge(nonce2, encryptedModifiedNonce2, sessionKey, 2));
        
        bulkOut = new Message(Protocol.validAuthentication);
        bulkOut.setElement("ipStation", InetAddress.getByName("127.168.1.51"));
        
        pipe2.write(bulkOut.toB64());
        pipe2.close();
        
		t.join();

		assertTrue(c.correctState(StateSubStation.IN_ATTESA));
	}

	@Test
	public void stationCalledTest() throws UnknownHostException {
		
		if(Settings.printTestName) {System.out.println("\nstationCalledTest");}
		
		s.setStationIp(ipStation);
		s.setState(StateSubStation.IN_ATTESA);
		
		pipe = internet.connectTo(ipStation, ipSubStation);

		int index = 15;
		DummyPost[] posts = new DummyPost[2];
		for(DummyPost post : posts){
			post = new DummyPost(index, InetAddress.getByName("127.0.0." + index));
			index ++;
			post.setVoter(null);
			post.setBadge(Protocol.unassignedPost);
			post.setState(StatePost.ATTIVA);
		}


		Message snapshot = new Message(Protocol.updateSubStation);
		snapshot.setElement("posts", posts);
		
		pipe.write(Protocol.updateSubStation);
		
		pipe.write(snapshot.toB64());
		
		pipe.hasNextLine();
		
		pipe.close();
		
		assertEquals(StateSubStation.ATTIVO, s.getState());
		
	}
	
}