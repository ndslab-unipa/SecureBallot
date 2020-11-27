package seggio.aux.model;

import java.net.InetAddress;

import model.AbstrModel;
import model.DummyPost;
import model.Person;
import model.State.StatePost;
import model.State.StateSubStation;
import utils.Protocol;

public class SeggioAusiliario extends AbstrModel {
	private InetAddress stationIp;
	private final int portStation;
	private final int numConnections;
	
	private StateSubStation state = StateSubStation.NON_ATTIVO;

	private DummyPost[] posts;
	
	private Person newVoter;
	
	public SeggioAusiliario(InetAddress urnIp, int urnPort, int stationPort, int numConnections) {
		this.urnIp = urnIp;
		this.urnPort = urnPort;
		this.portStation = stationPort;
		this.numConnections = numConnections;
		
		newVoter = null;
		stationIp = null;
	}
	
	public int getPost(String badge) {
		if(badge.equals(Protocol.unassignedPost))
			return -1;
		
		for(DummyPost post : posts) {
			String badgePost = post.getBadge();
			
			if(badgePost.equals(badge)) {
				return post.getId() - 1;
			}
		}
		
		return -1;
	}
	
	public void update(DummyPost[] posts) {
		this.posts = posts; 
	}
	
	public DummyPost[] getPosts() { return posts; }

	public void setStationIp(InetAddress ipStation) { this.stationIp = ipStation; }
	public InetAddress getStationIp() { return stationIp; }
	public int getStationPort() { return portStation; }
	
	public int getNumConnections() { return numConnections;	}
	public StateSubStation getState() { return state; }
	public void setState(StateSubStation state) { this.state = state; }
	
	public int getNumPost() { return posts == null ? 0 : posts.length; }
	
	public StatePost getPostState(int index) { return posts[index].getState() == null ? StatePost.OFFLINE : posts[index].getState(); }
	public Person getPostVoter(int index) { return posts[index].getVoter(); }
	public String getPostBadge(int index) { return posts[index].getBadge(); }

	public void setNewVoter(Person voter) { newVoter = voter; }
	public Person getNewVoter() { return newVoter; }
	
	public void setDocumentType(String type) { this.newVoter.setDocumentType(type); }
	public Person.DocumentType getDocumentType() { return newVoter != null ? newVoter.getDocumentType() : null; }
	
	public void setDocumentID(String id) { this.newVoter.setDocumentID(id); }
	public String getDocumentID() { return newVoter != null ? newVoter.getDocumentID() : null; }
}
