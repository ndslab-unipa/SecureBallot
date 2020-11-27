package model;

import java.net.InetAddress;

import utils.Protocol;

/**
 * Classe che rappresenta le postazioni viste dal punto di vista del seggio.
 */
public class DummyPost extends Parsable {
	private static final long serialVersionUID = 1L;
	
    private final int id;
    private final InetAddress ip;

    private State.StatePost state;
    private boolean unreachable = false;

    private Person voter;
    private String badge;

    private WrittenBallot[] encryptedBallots;

    public DummyPost(int id, InetAddress ip){ 
    	this.id = id;
    	this.ip = ip;
    	
    	state = State.StatePost.OFFLINE; 
    	voter = null; 
    	badge = Protocol.unassignedPost;
    	
    	encryptedBallots = null; 
    }

    public int getId() { return id; }
    public InetAddress getIp(){ return ip; }

    public State.StatePost getState(){ return state; }
    public void setState(State.StatePost state){
        this.state = state;
        this.unreachable = state == State.StatePost.OFFLINE;
    }

    public boolean isUnreachable(){
        return unreachable;
    }

    public void setUnreachable(boolean unreachable){
        this.unreachable = unreachable;
    }

    public Person getVoter() { return voter; }
    public void setVoter(Person voter) { this.voter = voter; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    public WrittenBallot[] getEncryptedBallots(){ return encryptedBallots; }
    public void setEncryptedBallots(WrittenBallot[] encryptedBallots){ this.encryptedBallots = encryptedBallots; }
}