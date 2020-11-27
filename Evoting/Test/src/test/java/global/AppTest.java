package global;

import common.Internet;
import common.RPTemp;
import common.Settings;
import common.TestView;
import controller.CardReader;
import controller.Link;
import encryption.RandStrGenerator;
import encryption.VoteEncryption;
import exceptions.PEException;
import model.*;
import model.State.StatePost;
import model.State.StateStation;
import model.State.StateSubStation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import postazione.ControllerP;
import postazione.model.Post;
import seggio.ControllerS;
import seggio.TestViewStation;
import seggio.aux.model.SeggioAusiliario;
import seggio.model.Station;
import seggioAusiliario.ControllerSubS;
import urna.ControllerU;
import urna.TestDB;
import urna.model.Urn;
import utils.Protocol;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Random;

import static org.junit.Assert.*;

public class AppTest {
	
	private final String voterName = "Bernardo";
	private EmptyBallot[] procedureBallots;
	private final int numPosts = 3;
	
	private ArrayList<String> stationsSessionKeys;
	private ArrayList<String> subStationsSessionKeys;
	private ArrayList<String> postsSessionKeys;
	
	private TestDB db;
	private ControllerU cu;
	
	private Station s;
	private ControllerS cs;
	
	private SeggioAusiliario sub;
	private ControllerSubS csub;
	
	private final Post[] P = new Post[numPosts];
	private final ControllerP[] Cp = new ControllerP[numPosts];
	
	private final InetAddress[] ipPosts = new InetAddress[numPosts];
	
	private InetAddress ipStation;

	private TestViewStation viewStation = null;
	
	/**
	 * Richiamata prima di ogni test, fa partire ogni controller in gioco, simulando quindi l'avvio dei programmi sui terminali.
	 * Inoltre assegna un indirizzo ip fittizio ad ogni terminale.
	 */
	@Before
	public void setup() throws IOException, PEException {

		TestView viewPosts = new TestView(Settings.viewBehaviour, Terminals.Type.Post);
		TestView viewSubStation = new TestView(Settings.viewBehaviour, Terminals.Type.SubStation);
		TestView viewUrn = new TestView(Settings.viewBehaviour, Terminals.Type.Urn);
		viewStation = new TestViewStation(Settings.viewBehaviour);

		stationsSessionKeys = new ArrayList<>();
    	subStationsSessionKeys = new ArrayList<>();
    	postsSessionKeys = new ArrayList<>();
        
        ipStation = InetAddress.getByName("127.0.0.5");
        stationsSessionKeys.add(RandStrGenerator.genSessionKey());

		InetAddress ipSubStation = InetAddress.getByName("127.0.0.1");
        subStationsSessionKeys.add(RandStrGenerator.genSessionKey());

		InetAddress ipUrn = InetAddress.getByName("127.0.0.10");
        
        for(int post = 0; post < numPosts; post++) {
			String ip = "127.0.0." + (post + 18);
			ipPosts[post] = InetAddress.getByName(ip);
			
			P[post] = new Post(ipUrn, 0, 0, 5);
			Cp[post] = new ControllerP(viewPosts, P[post], ipPosts[post]);
        	Cp[post].start();
        	
        	postsSessionKeys.add(RandStrGenerator.genSessionKey());
		}
		
		s = new Station(ipUrn, 0, 0, 0, 5);
        cs = new ControllerS(viewStation, s, ipStation);
		viewStation.setController(cs);

        sub = new SeggioAusiliario(ipUrn, 0, 0, 5);
        csub = new ControllerSubS(viewSubStation, sub, ipSubStation);
		
		InetAddress[] ipSubStations = {ipSubStation};
		
		db = new TestDB(ipStation, ipPosts, ipSubStations);
		db.setSessionKeyes(stationsSessionKeys, subStationsSessionKeys, postsSessionKeys);
		
		populateDB(true);
		procedureBallots = db.getEmptyBallots(0);

		Urn u = new Urn(0, 10);
		u.setProcedureBallots(procedureBallots);
		cu = new ControllerU(viewUrn, u, db, ipUrn);
		
        cs.start();
        csub.start();
        
        cu.start();
   	}
	
	/**
	 * Interrompe l'esecuzione di ogni terminale dopo ogni test.
	 */
	@After
	public void dismantle() {
		
		for(int i = 0; i < numPosts; i++) {
			Cp[i].shutDown();
		}
		
		cs.shutDown();
		cu.shutDown();
		csub.shutDown();
	}

	/*+++++
	 *TEST+
	 *++++*/

	@Test
	public void terminalsActivationTest() throws InterruptedException {
		if(Settings.printTestName)
			System.out.println("\nterminalsActivationTest");

		assertTrue(csub.correctState(StateSubStation.NON_ATTIVO));

		Thread activateSubstation_t = activateSubStation();
		activateSubstation_t.start();
		activateSubstation_t.join();

		assertTrue(csub.correctState(StateSubStation.IN_ATTESA));

		for(int postazione = 0; postazione < numPosts; postazione++) {
			assertTrue(Cp[postazione].correctState(StatePost.NON_ATTIVA));
		}

		assertTrue(cs.correctState(StateStation.NON_ATTIVO));

		Thread[] activatePosts_t = new Thread[numPosts];
		for(int postazione = 0; postazione < numPosts; postazione++) {
			activatePosts_t[postazione] = activatePost(postazione);
		}

		Thread activateStation_t = activateStation();
		Thread activateSubStation_t = activateSubStation();

		for(int postazione = 0; postazione < numPosts; postazione++) {
			activatePosts_t[postazione].start();
		}
		activateStation_t.start();
		activateSubStation_t.start();

		for(int postazione = 0; postazione < numPosts; postazione++) {
			activatePosts_t[postazione].join();
		}
		activateStation_t.join();
		activateSubStation_t.join();

		cs.updateSubStations();

		for(int postazione = 0; postazione < numPosts; postazione++) {
			assertTrue(Cp[postazione].correctState(StatePost.ATTIVA));
			assertNull(s.getPostVoter(postazione));
			assertEquals(Protocol.unassignedPost, s.getPostBadge(postazione));
			assertEquals(Protocol.unassignedPost, P[postazione].getBadge());
			assertNull(s.getEncryptedBallots(postazione));
			assertEquals(postsSessionKeys.get(postazione), P[postazione].getSessionKey());
		}
		
		assertTrue(cs.correctState(StateStation.ATTIVO));
		assertTrue(csub.correctState(StateSubStation.ATTIVO));
	}

	@Test
	public void associationTest() {
		if(Settings.printTestName)
			System.out.println("\nassociationTest");
		
		activateStationSubStationAndPosts();

		assertTrue(cs.correctState(StateStation.ATTIVO));
		for(int p = 0; p < numPosts; p++) {
			assertTrue(Cp[p].correctState(StatePost.ATTIVA));
			assertEquals(StatePost.ATTIVA, s.getPostState(p));
		}
		
		int[] ballotCodes = {0, 1, 2};

		String card = "144";
		int post = viewStation.createAssociation(card, new Person(voterName, "-1", voterName + "-1", ballotCodes, true));

		assertTrue(Cp[post].correctState(StatePost.ASSOCIATA));
		assertEquals(card, P[post].getBadge());
	}

	@Test
	public void associatePostFromSubStationTest() {
		if(Settings.printTestName)
			System.out.println("associatePostFromSubStationTest");

		activateStationSubStationAndPosts();

		sub.setStationIp(ipStation);

		int[] ballotCodes = {0};

		String card = "123";
		Person voter = new Person("Charlie", "Brown", "ID", ballotCodes, true);

		sub.setNewVoter(voter);
		sub.setDocumentType("Conoscenza Personale");

		CardReader subStatCardReader = csub.getCardReader();
		assertEquals(Protocol.associationAck, subStatCardReader.write(card));
		subStatCardReader.endWrite();
	}
	
	@Test
	public void badgeInTest() {
		if(Settings.printTestName)
			System.out.println("\nbadgeInTest");
		
		activateStationSubStationAndPosts();
		
		StatePost state = StatePost.ASSOCIATA;
		
		int post = 1;
		String badge = "0";
		
		P[post].setState(state);
		P[post].setBadgeID(badge);
		P[post].setStationIp(ipStation);


		assertTrue(Cp[post].correctState(StatePost.ASSOCIATA));
	}
	
	@Test
	public void sendVoteToStationTest() throws PEException {
		if(Settings.printTestName)
			System.out.println("\nsendVoteToSeggioTest");
		
		activateStationSubStationAndPosts();

		int[] ballotCodes = {0};
		String card = "15";
		Person voter = new Person("Nome", "Cognome", "Matricola", ballotCodes, true);

		int post = new Random().nextInt(numPosts);
		P[post].setBallots(ballotCodes);
		P[post].setState(StatePost.IN_USO);

		s.setAssociation(voter, card, post);
		s.setPostState(StatePost.IN_USO, post);

		sendVoteToStation(post);

		assertTrue(Cp[post].correctState(StatePost.VOTO_INVIATO));
		assertTrue(cs.correctState(StateStation.ATTIVO));
	}
	
	@Test
	public void sendVoteToUrnAndNotifyPostTest() throws PEException {
		if(Settings.printTestName)
			System.out.println("\nsendVoteToUrnAndNotifyPostTest");
		
		activateStationSubStationAndPosts();

		int[] ballotCodes = {0};
		String card = "22";
		Person voter = new Person(voterName, voterName, voterName, ballotCodes, true);
		
		int post = new Random().nextInt(numPosts);

		P[post].setBallots(ballotCodes);
		P[post].setBadgeID(card);
		P[post].setState(StatePost.IN_USO);
		
		s.setAssociation(voter, card, post);
		s.setPostState(StatePost.IN_USO, post);
		
		Cp[post].tryVoteSending();

		assertTrue(Cp[post].correctState(StatePost.VOTO_INVIATO));
		assertTrue(cs.correctState(StateStation.ATTIVO));

		viewStation.sendVote(card);

		assertTrue(cs.correctState(StateStation.ATTIVO));
		assertTrue(Cp[post].correctState(StatePost.ATTIVA));
		assertEquals(1, db.countVotes());
	}
	
	@Test
	public void sendVoteFromSubStationTest() throws PEException {
		if(Settings.printTestName)
			System.out.println("\nsendVoteFromSubStationTest");
		
		activateStationSubStationAndPosts();
		
		sub.setStationIp(ipStation);
		cs.updateSubStations();
		
		int[] ballotCodes = {0};
		
		Person voter = new Person("Nome", "Cognome", "ID", ballotCodes, true);
		voter.setDocumentType("Conoscenza Personale");

		assertTrue(s.reservePost());
		
		int post = s.getAssociatedPost();
		String card = "22";
		
		StatePost requiredStatePost = StatePost.IN_USO;
		
		P[post].setBallots(ballotCodes);
		P[post].setBadgeID(card);
		P[post].setState(requiredStatePost);
		
		s.setAssociation(voter, card, post);
		s.setPostState(requiredStatePost, post);
		
		Cp[post].tryVoteSending();

		CardReader subStatCardReader = csub.getCardReader();
		assertEquals(Protocol.votesReceivedAck, subStatCardReader.write(card));
		subStatCardReader.endWrite();
	}
	
	@Test
	public void completeCircleTest() throws Exception {
		if(Settings.printTestName)
			System.out.println("\ncompleteCircle");
		
		activateStationSubStationAndPosts();
		
		int[] ballotCodes = {0, 1, 2};
		
		String card = "125";

		int post = viewStation.createAssociation(card, new Person(voterName, "-1", voterName + "-1", ballotCodes, true));

		assertTrue(Cp[post].correctState(StatePost.ASSOCIATA));
		assertEquals(voterName, s.getPostVoter(post).getFirstName());
		assertEquals(card, s.getPostBadge(post));
		assertEquals(card, P[post].getBadge());
		
		badgeIn(card, post);
		assertTrue(Cp[post].correctState(StatePost.IN_USO));
		
		P[post].setBallots(ballotCodes);
		
		sendVoteToStation(post);

		assertTrue(Cp[post].correctState(StatePost.VOTO_INVIATO));
		
		assertEquals(procedureBallots.length, s.getEncryptedBallots(post).length);
		for(WrittenBallot writtenBallot : s.getEncryptedBallots(post)) {
			ArrayList<VotePacket> encryptedPackets = writtenBallot.getEncryptedVotePackets();
			boolean firstPacket = true;
			for(VotePacket packet : encryptedPackets) {
				String pref = VoteEncryption.decrypt(packet, RPTemp.getPrivate());
				if(firstPacket){
					assertEquals(Protocol.emptyBallot, pref);
					firstPacket = false;
				}
				else{
					assertEquals(Protocol.emptyPreference, pref);
				}
			}
		}

		viewStation.sendVote(card);

		verifyStateAfterVote();
		
		assertEquals(1, db.countVotes());
	}
	
	@Test
	public void sequentialVoteTest() {
		if(Settings.printTestName)
			System.out.println("\nsequentialVoteTest");
		
		int numVoters = 30;
				
		activateStationSubStationAndPosts();
		
		int[] ballotCodes = {0};
		
		//Thread t;
		
		for(int i = 0; i < numVoters; i++) {
			/*t = singleVote(new Person(voterName, Integer.toString(i), voterName + i, ballotCodes, true), "" + i);
			t.start();
			t.join();*/
			singleVote(new Person(voterName, Integer.toString(i), voterName + i, ballotCodes, true), "" + i);
		}

		
		verifyStateAfterVote();
		assertEquals(numVoters, db.countVotes());
	}
	
	
	@Test
	public void stressTest() throws InterruptedException {
		if(Settings.printTestName)
			System.out.println("\nstressTest");
		
		int numVoters = 30;
		
		activateStationSubStationAndPosts();
		
		int[] ballotCodes = {0};
		
		Thread[] voters = new Thread[numVoters];
		
		for(int i = 0; i < numVoters; i++) {
			int finalI = i;
			voters[i] = new Thread(() -> singleVote(new Person(voterName, Integer.toString(finalI), voterName + finalI, ballotCodes, true), "" + finalI));
		}
		
		for(int i = 0; i < numVoters; i++) {voters[i].start();}
		
		for(int i = 0; i < numVoters; i++) {voters[i].join();}
		
		verifyStateAfterVote();
		
		assertEquals(numVoters, db.countVotes());
	}
	
	
	@Test
	public void retrieveVotersTest() {
		if(Settings.printTestName)
			System.out.println("\nretrieveVotersTest");
		
		activateStationSubStationAndPosts();

		String similarFirstName = "Nome";
		String similarLastName = "Cognome";
		
		int[] ballotCodes = {0};
		
		ArrayList<Person> voters0 = new ArrayList<>();
		
		for(int i = 0; i < 3; i++) {
			voters0.add(new Person(similarFirstName, similarLastName, Integer.toString(i), ballotCodes, i%2==0));
		}
		
		db.setVoters(voters0);
		
		Person[] voters1 = cs.retrieveVotersByName(similarFirstName, similarLastName);
		Person[] voters2 = csub.retrieveVotersByName(similarFirstName, similarLastName);
		
		assertEquals(voters0.size(), voters1.length);
		
		for(int i = 0; i < voters0.size(); i++) {
			Person voter0 = voters0.get(i);
			Person voter1 = voters1[i];
			Person voter2 = voters2[i];
			
			assertEquals(voter0.getFirstName(), voter1.getFirstName());
			assertEquals(voter0.getLastName(), voter1.getLastName());
			assertEquals(voter0.getID(), voter1.getID());
			assertEquals(voter0.mayVote(), voter1.mayVote());
			
			assertEquals(voter0.getFirstName(), voter2.getFirstName());
			assertEquals(voter0.getLastName(), voter2.getLastName());
			assertEquals(voter0.getID(), voter2.getID());
			assertEquals(voter0.mayVote(), voter2.mayVote());
		}
	}

	/*+++++++++
	 * UTILITY+
	 +++++++++*/

	/**
	 * Funzione che attiva i terminali e aggiorna "forzatamente" seggio e seggio ausiliario sullo stato delle postazioni,
	 * senza aspettare che a farlo siano le postazioni (per il seggio) e il seggio (per il seggio ausiliario).
	 * Necessaria per altrimenti
	 */
	private void activateStationSubStationAndPosts() {

		cs.activate(stationsSessionKeys.get(0));
		assertEquals(StateStation.ATTIVO, s.getState());

		DummyPost[] dummyPosts = new DummyPost[Cp.length];

		for(int i = 0; i < numPosts; i++) {

			//Per intercettare l'aggiornamento che la postazione invia al seggio
			Link link = Internet.getInstance().userRedirect(ipPosts[i], ipStation);

			Cp[i].activate(postsSessionKeys.get(i));

			//Dato che dava problemi in certi test e che comunque aggiorno manualmente
			link.waitNRead();
			link.close();

			assertEquals(StatePost.ATTIVA, P[i].getState());
			s.setPostState(StatePost.ATTIVA, i);

			dummyPosts[i] = new DummyPost(i, ipPosts[i]);
			dummyPosts[i].setState(StatePost.ATTIVA);
		}

		csub.activate(subStationsSessionKeys.get(0));
		assertEquals(StateSubStation.IN_ATTESA, sub.getState());

		try {
			csub.updateSubStation(dummyPosts);
		} catch (PEException e) {
			e.printStackTrace();
			fail();
		}
		assertEquals(StateSubStation.ATTIVO, sub.getState());

	}

	/**
	 * Richiamata dai test si effettua l'intero processo di votazione.
	 * Verifica che lo stato dei terminali sia quello atteso.
	 */
	private void verifyStateAfterVote() {
		for(int postazione = 0; postazione < numPosts; postazione++) {
			assertTrue(Cp[postazione].correctState(StatePost.ATTIVA));
			assertNull(s.getPostVoter(postazione));
			assertEquals(Protocol.unassignedPost, s.getPostBadge(postazione));
			assertEquals(Protocol.unassignedPost, P[postazione].getBadge());
			assertNull(s.getEncryptedBallots(postazione));
		}
		assertTrue(cs.correctState(StateStation.ATTIVO));
		
		assertEquals(StateSubStation.ATTIVO, sub.getState());
		
		assertTrue(csub.correctState(StateSubStation.ATTIVO));
	}

	/**
	 * Funzione richiamata in {@link AppTest#activateStationSubStationAndPosts()} per testare l'attivazione concorrente dei terminali.
	 * @param post 	La postazione da attivare.
	 * @return		Il thread che si occuperà dell'attivazione della postazione.
	 */
	private Thread activatePost(int post) {
		
		assertTrue(post >= 0 && post < numPosts);

		return new Thread(() -> {
	    	Cp[post].activate(postsSessionKeys.get(post));
	    	assertEquals(StatePost.ATTIVA, P[post].getState());
	    });
	}

	/**
	 * Funzione richiamata in {@link AppTest#activateStationSubStationAndPosts()} per testare l'attivazione concorrente dei terminali.
	 * @return	Il thread che si occuperà dell'attivazione del seggio ausiliario.
	 */
	private Thread activateSubStation() {

		return new Thread( () ->  {
	    	csub.activate(subStationsSessionKeys.get(0));
	    	assertEquals(StateSubStation.IN_ATTESA, sub.getState());
	    });
	}

	/**
	 * Funzione richiamata in {@link AppTest#activateStationSubStationAndPosts()} per testare l'attivazione concorrente dei terminali.
	 * @return	Il thread che si occuperà dell'attivazione del seggio principale.
	 */
	private Thread activateStation() {

		return new Thread( () ->  {
	    	cs.activate(stationsSessionKeys.get(0));
	    	assertEquals(StateStation.ATTIVO, s.getState());
	    });
	}

	/**
	 * Funzione che si occupa di eseguire le operazioni di alto livello per procedere al "badge in" di una postazione.
	 * @param badge	L'id della card attesa dalla postazione.
	 * @param post	La postazione presso cui si vuole procedere con il voto.
	 */
	private void badgeIn(String badge, int post) { 
		CardReader postCardReader = Cp[post].getCardReader();
		postCardReader.write(badge);
		postCardReader.endWrite();
	}

	/**
	 * Funzione che si occupa di far inviare ad una postazione il voto verso il seggio.
	 * @param post	La postazione che deve invire il voto.
	 */
	private void sendVoteToStation(int post) {
		Cp[post].tryVoteSending();
	}

	/**
	 * Funzione nella quale si effettua una votazione completa,
	 * utile per testare votazioni sequenziali in {@link AppTest#sequentialVoteTest()}
	 * e concorrenti in {@link AppTest#stressTest}.
	 * @param voter	Il votante fittizio della votazione.
	 * @param card	La card associata alla votazione.
	 */
	private void singleVote(Person voter, String card) {
		int post = viewStation.createAssociation(card, voter);

		assertEquals(StatePost.ASSOCIATA, P[post].getState());

		badgeIn(card, post);

		assertEquals(StatePost.IN_USO, P[post].getState());

		sendVoteToStation(post);

		assertEquals(StatePost.VOTO_INVIATO, P[post].getState());

		viewStation.sendVote(card);
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
		
		ArrayList<EmptyBallot> schede = new ArrayList<EmptyBallot>();
		schede.add(new EmptyBallot("Scheda 0", 0, "Scheda fittizia usata per lo unit testing", 3)
				.addList(new ElectoralList("Lista 0").addPerson(p00).end())
				.addList(new ElectoralList("Lista 1").addPerson(p01).end())
				.addList(new ElectoralList("Lista 2").addPerson(p02).end()));
		
		schede.add(new EmptyBallot("Scheda 1", 1, "Scheda fittizia usata per lo unit testing", 4)
				.addList(new ElectoralList("Lista 0").addPerson(p10).end())
				.addList(new ElectoralList("Lista 1").addPerson(p11).end())
				.addList(new ElectoralList("Lista 2").addPerson(p12).end())
				);
		
		schede.add(new EmptyBallot("Scheda 2", 2, "Scheda fittizia usata per lo unit testing", 5)
				.addList(new ElectoralList("Lista 0").addPerson(p20).end())
				.addList(new ElectoralList("Lista 1").addPerson(p21).end())
				.addList(new ElectoralList("Lista 2").addPerson(p22).end()));
		
		if(!well) {
			schede.add(new EmptyBallot("Scheda erronea", 3, "Scheda fittizia usata per lo unit testing", 8)
					.addList(new ElectoralList("Lista 0").addPerson(p22).addPerson(p22))
					);
		}
		
		db.setBallots(schede, null);
	}
	
}

