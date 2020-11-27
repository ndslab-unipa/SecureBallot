package seggio;

import common.TestView;
import controller.CardReader;
import model.DummyPost;
import model.Person;
import model.State;
import model.Terminals;
import seggio.model.Station;
import utils.Protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestViewStation extends TestView {

    private ControllerS controller;

    public TestViewStation(Behaviour behaviour) {
        super(behaviour, Terminals.Type.Station);
    }

    public void setController(ControllerS controller) {
        this.controller = controller;
    }

    public synchronized int createAssociation(String card, Person voter){

        int post = -1;

        try{
            Station s = controller.getStation();

            voter.setDocumentType("Conoscenza Personale");

            while(!avaiablePost() || !readyToRegister())
                wait();

            s.setNewVoter(voter);

            CardReader stationCardReader = controller.getCardReader();
            assertEquals(Protocol.associationAck, stationCardReader.write(card));
            stationCardReader.endWrite();

            for(int p = 0; p < s.getNumPost(); p++) {
                Person registeredVoter = s.getPostVoter(p);

                if(registeredVoter != null && registeredVoter.getID().equals(voter.getID())) {
                    post = p;
                }

            }

            notifyAll();
        }
        catch (InterruptedException e){
            e.printStackTrace();
            assertTrue(false);
        }

        return post;
    }

    public synchronized void sendVote(String badge) {
        CardReader cardReader = controller.getCardReader();
        assertEquals(Protocol.votesReceivedAck, cardReader.write(badge));
        cardReader.endWrite();

        notifyAll();
    }

    private boolean avaiablePost(){
        DummyPost[] posts = controller.getStation().getPosts();
        for(DummyPost post : posts){
            if(post.getState() == State.StatePost.ATTIVA)
                return true;
        }

        return false;
    }

    private boolean readyToRegister(){
        return controller.getStation().getNewVoter() == null;
    }
}
