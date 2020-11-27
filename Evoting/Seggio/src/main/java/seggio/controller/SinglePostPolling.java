package seggio.controller;

import java.net.InetAddress;

import exceptions.FLRException;
import exceptions.PEException;
import model.DummyPost;
import model.Message;
import model.State.StatePost;
import model.WrittenBallot;
import seggio.model.Station;
import utils.Constants;

public class SinglePostPolling extends Thread {
	private Controller controller;
	private Station station;
	private DummyPost currPost;
	
	private boolean destroyAssociation = false;
	private boolean sendVotes = false;
	
	public SinglePostPolling(Controller controller, Station station, DummyPost post) {
		this.controller = controller;
		this.station = station;
		this.currPost = post;
	}

	@Override
	public void run() {
		String postString = "La postazione @ "+currPost.getIp().getHostAddress();
		
		StatePost lastKnownState = currPost.getState();
		String lastKnownBadge = currPost.getBadge();
		boolean wasUnreachable = currPost.isUnreachable();
		
		Message currPostsInfo = controller.checkForUnreachablePost(currPost);
		StatePost currState = currPostsInfo == null ? null : currPostsInfo.getElement("state");
		String currBadge = currPostsInfo == null ? null : currPostsInfo.getElement("card");
		
		boolean isUnreachable = currState == null;
		currPost.setUnreachable(isUnreachable);
		
		if(!wasUnreachable && !isUnreachable) {
			if(Constants.verbose)
				System.out.println(postString + " è ancora raggiungibile");
			return;
		}
		
		if(wasUnreachable && isUnreachable) {
			if(Constants.verbose)
				System.out.println(postString + " è ancora irraggiungibile. Ultimo stato noto: " + lastKnownState);
			return;
		}
		
		if(!wasUnreachable && isUnreachable) {
			System.out.println(postString + " non è più raggiungibile");
		}
		
		if(wasUnreachable && !isUnreachable) {
			try {
				if(!checkStatesConsistency(currPost.getIp(), lastKnownState, lastKnownBadge, currState, currBadge))
					throw FLRException.FLR_18(currPost.getIp().getHostAddress(), lastKnownState, lastKnownBadge, currState, currBadge, false);

				System.out.println(postString + " non è più irraggiungibile. Nuovo stato consistente con l'ultimo noto.");
				
				int postIdx = station.getPost(lastKnownBadge);
				if(postIdx != -1) {
					if(destroyAssociation) {
						station.destroyAssociation(postIdx);
						System.out.println("\tL'associazione precedentemente creata è stata distrutta, dato il nuovo stato della postazione.");
					}
					
					if(sendVotes) {
						WrittenBallot[] ballots = station.getEncryptedBallots(postIdx);
						if(ballots != null && ballots.length > 0) {
							controller.sendVote(lastKnownBadge, false);
							System.out.println("\tI voti che la postazione aveva inviato al seggio sono stati correttamente inoltrati all'urna.");
						}
					}
				}
				
				station.setPostState(currState, currPost);
				
			} catch (PEException e) {
				controller.printError(e);
				if(e.getCode() == PEException.Code.FLR_18) {
					StatePost newState = e.getGeneric().contains("Necessario Riavvio") ? StatePost.DA_RIAVVIARE : StatePost.DA_RESETTARE;
					
					controller.changePostState(currPost.getIp(), newState);
					System.out.println(postString+" non è più irraggiungibile. Nuovo stato NON consistente con l'ultimo noto.");
				}
			}
		}
		
		controller.updateViewAndSubstation();
	}
	
	private boolean checkStatesConsistency(InetAddress ip, StatePost oldState, String oldBadge, StatePost newState, String newBadge) throws PEException {
		if(oldState.equals(StatePost.DA_RESETTARE)) {
			switch(newState) {
				case DA_RESETTARE:
				case NON_ATTIVA:
				case ATTIVA:
					return true;
					
				case ASSOCIATA:
				case IN_USO:
				case VOTO_PENDING:
				case VOTO_INVIATO:
				case DA_RIAVVIARE:
					throw FLRException.FLR_18(ip.getHostAddress(), oldState, oldBadge, newState, newBadge, true);
					
				case OFFLINE:
					return true;
			}
		}
		
		if(oldState.equals(StatePost.DA_RIAVVIARE)) {
			switch(newState) {
				case DA_RIAVVIARE:
				case NON_ATTIVA:
					return true;

				case ATTIVA:
				case ASSOCIATA:
				case IN_USO:
				case VOTO_PENDING:
				case VOTO_INVIATO:
				case DA_RESETTARE:
					throw FLRException.FLR_18(ip.getHostAddress(), oldState, oldBadge, newState, newBadge, true);
					
				case OFFLINE:
					return true;
			}
		}
		
		if(oldState.equals(StatePost.ATTIVA)) {
			switch(newState) {
				case ATTIVA:
					return oldBadge.equals(newBadge);
			
				case ASSOCIATA:
				case IN_USO:
				case VOTO_PENDING:
				case VOTO_INVIATO:
					throw FLRException.FLR_18(ip.getHostAddress(), oldState, oldBadge, newState, newBadge, false);

				case DA_RESETTARE:
				case DA_RIAVVIARE:
					throw FLRException.FLR_18(ip.getHostAddress(), oldState, oldBadge, newState, newBadge, true);

				case NON_ATTIVA:
				case OFFLINE:
					return true;
			}
		}
		
		if(oldState.equals(StatePost.ASSOCIATA)) {
			switch(newState) {
				case ASSOCIATA:
				case IN_USO:
				case VOTO_PENDING:
					return oldBadge.equals(newBadge);
					
				case ATTIVA:
					destroyAssociation = true;
					return true;
				
					
				case VOTO_INVIATO:
					throw FLRException.FLR_18(ip.getHostAddress(), oldState, oldBadge, newState, newBadge, false);
				
				case DA_RESETTARE:
				case DA_RIAVVIARE:
					throw FLRException.FLR_18(ip.getHostAddress(), oldState, oldBadge, newState, newBadge, true);

				case NON_ATTIVA:
				case OFFLINE:
					return true;
			}
		}
		
		if(oldState.equals(StatePost.IN_USO)) {
			switch(newState) {
				case IN_USO:
				case VOTO_PENDING:
					return oldBadge.equals(newBadge);
					
				case ATTIVA:
					destroyAssociation = true;
					return true;
					
				case ASSOCIATA:
				case VOTO_INVIATO:
					throw FLRException.FLR_18(ip.getHostAddress(), oldState, oldBadge, newState, newBadge, false);
					
				case DA_RESETTARE:
				case DA_RIAVVIARE:
					throw FLRException.FLR_18(ip.getHostAddress(), oldState, oldBadge, newState, newBadge, true);

				case NON_ATTIVA:
				case OFFLINE:
					return true;
			}
		}
		
		if(oldState.equals(StatePost.VOTO_INVIATO)) {
			switch(newState) {
				case VOTO_INVIATO:
					return oldBadge.equals(newBadge);
					
				case NON_ATTIVA:
				case ATTIVA:
					sendVotes = true;
					return true;
					
				case ASSOCIATA:
				case IN_USO:
				case VOTO_PENDING:
					throw FLRException.FLR_18(ip.getHostAddress(), oldState, oldBadge, newState, newBadge, false);
					
				case DA_RESETTARE:
				case DA_RIAVVIARE:
					throw FLRException.FLR_18(ip.getHostAddress(), oldState, oldBadge, newState, newBadge, true);

				case OFFLINE:
					return true;
			}
		}
		
		if(oldState.equals(StatePost.NON_ATTIVA)) {
			switch(newState) {
				case NON_ATTIVA:
				case ATTIVA:
					return true;
					
				case ASSOCIATA:
				case IN_USO:
				case VOTO_PENDING:
				case VOTO_INVIATO:
					throw FLRException.FLR_18(ip.getHostAddress(), oldState, oldBadge, newState, newBadge, false);
					
				case DA_RESETTARE:
				case DA_RIAVVIARE:
					throw FLRException.FLR_18(ip.getHostAddress(), oldState, oldBadge, newState, newBadge, true);
				
				case OFFLINE:
					return true;
			}
		}
		
		return false;
	}
}
