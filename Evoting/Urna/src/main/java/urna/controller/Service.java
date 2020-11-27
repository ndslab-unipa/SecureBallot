package urna.controller;

import java.net.InetAddress;
import controller.AbstrService;
import controller.Link;
import controller.TerminalController;
import exceptions.PEException;
import model.Message;
import model.Person;
import model.Terminals;
import model.WrittenBallot;
import utils.Constants;
import utils.Protocol;

public class Service extends AbstrService {
	public Service(TerminalController controller, Link link, String name) {
		super(controller, link, name);
	}
	
	protected void execute() {
		int procedureCode = ((Controller) controller).getProcedureCode();
		int sessionCode = ((Controller) controller).getSessionCode();
		
		if(procedureCode == -1 || sessionCode == -1) {
			return;
		}
		
		String message = link.read();
		
		switch(message) {
			case Protocol.StationAuthenticationPhase1:
				authenticate(Terminals.Type.Station);
				break;
			
			case Protocol.StationAuthenticationPhase2:
				verifyAuthentication(Terminals.Type.Station);
				break;
				
			case Protocol.SubStationAuthenticationPhase1:
				authenticate(Terminals.Type.SubStation);
				break;
			
			case Protocol.SubStationAuthenticationPhase2:
				verifyAuthentication(Terminals.Type.SubStation);
				break;
				
			case Protocol.PostAuthenticationPhase1:
				authenticate(Terminals.Type.Post);
				break;
				
			case Protocol.PostAuthenticationPhase2:
				verifyAuthentication(Terminals.Type.Post);
				break;
				
			case Protocol.checkTerminalAuthentication:
				checkTerminalAuthenticated();
				break;
				
			case Protocol.searchPersonReq:
				if(((Controller) controller).verifyIp(ip, Terminals.Type.Station, message))
					searchPerson(Terminals.Type.Station);
				break;
				
			case Protocol.searchPersonSubStationReq:
				if(((Controller) controller).verifyIp(ip, Terminals.Type.SubStation, message))
					searchPerson(Terminals.Type.SubStation);
				break;
				
			case Protocol.registerNewUserReq:
				if(((Controller) controller).verifyIp(ip, Terminals.Type.Station, message))
					registerUser();
				break;
				
			case Protocol.updateExistingUserReq:
				if(((Controller) controller).verifyIp(ip, Terminals.Type.Station, message))
					updateExistingUser();
				break;
				
			case Protocol.nonceReq:
				if(((Controller) controller).verifyIp(ip, Terminals.Type.Post, message))
					genNonces();
				break;
				
			case Protocol.sendVoteToUrn:
				if(((Controller) controller).verifyIp(ip, Terminals.Type.Station, message))
					receiveVote();
				break;
				
			case Protocol.stationShutDown:
				if(((Controller) controller).verifyIp(ip, Terminals.Type.Station, message))
					logShutDown(Terminals.Type.Station);
				break;
				
			case Protocol.subStationShutDown:
				if(((Controller) controller).verifyIp(ip, Terminals.Type.SubStation, message))
					logShutDown(Terminals.Type.SubStation);
				break;
				
			case Protocol.postShutDown:
				if(((Controller) controller).verifyIp(ip, Terminals.Type.Post, message))
					logShutDown(Terminals.Type.Post);
				break;
				
			//Se il messaggio non è stato riconosciuto si stampa a schermo un errore
			default: 
				String error = "Il terminale [IP: "+ip.getHostAddress()+"] ha inviato il seguente messaggio inatteso: "+message;
				if(Constants.verbose)
					controller.printError("Comunicazione Inattesa", error);
				((Controller) controller).logWarning(error);
		}
	}
	
	private void authenticate(Terminals.Type type) {
		if(!link.hasNextLine()) {
			return;
		}
		
		String encryptedNonce = link.read();
		Message bulkOut = ((Controller) controller).authenticateToTerminal(ip, encryptedNonce, type);
		link.write(bulkOut.toB64());
	}
	
	private void verifyAuthentication(Terminals.Type type) {
		if(!link.hasNextLine()) {
			return;
		}
		
		String encryptedNonce = link.read();
		Message bulkOut = ((Controller) controller).verifyTerminalAuthentication(ip, encryptedNonce, type);
		link.write(bulkOut.toB64());
	}
	
	private void checkTerminalAuthenticated() {
		Message response = new Message();
		try {
			Message request = (Message) Message.fromB64(link.read(), "seggio");
			String[] required = {"terminal"};
			Class<?>[] types = {Terminals.Type.class};
			
			request.verifyMessage(Protocol.checkTerminalAuthentication, required, types, ip.getHostAddress());
			Terminals.Type type = request.getElement("terminal");
			
			response = ((Controller) controller).checkTerminalAuthenticated(ip, type);
		} catch (PEException e) {
			response.setValue(Protocol.authenticatedNack);
			response.addError(e.getMessage());
		}
		
		link.write(response.toB64());
	}
	
	private void searchPerson(Terminals.Type type) {
		if(!link.hasNextLine()) {return;}
		
		String similarFirstName;
		String similarLastName;
		Message response;
		
		try {
			Message request = (Message) Message.fromB64(link.read(), "seggio");
			String[] required = {"firstName", "lastName"};
			Class<?>[] types = {String.class, String.class};
			
			String expectedMsg = type == Terminals.Type.Station ? Protocol.searchPersonReq : Protocol.searchPersonSubStationReq;
			request.verifyMessage(expectedMsg, required, types, ip.getHostAddress());
			similarFirstName = request.getElement("firstName");
			similarLastName = request.getElement("lastName");
			
			response = ((Controller) controller).searchPerson(ip, type, similarFirstName, similarLastName);
			
		} catch (Exception e) {
			e.printStackTrace();
			response = new Message(Protocol.error);
			response.addError(e.getMessage());
		}
		
		link.write(response.toB64());
	}
	
	private void registerUser() {
		if(!link.hasNextLine()) return;
		
		String id, lastName, firstName, birthDate;
		int[] ballots;
		
		Message response;
		try {
			Message request = (Message) Message.fromB64(link.read(), "seggio");
			String[] required = {"ID", "lastName", "firstName", "birthDate", "ballots"};
			Class<?>[] types = {String.class, String.class, String.class, String.class, int[].class};
			
			request.verifyMessage(Protocol.registerNewUserReq, required, types, "seggio");
			id = request.getElement("ID");
			lastName = request.getElement("lastName");
			firstName = request.getElement("firstName");
			birthDate = request.getElement("birthDate");
			ballots = request.getElement("ballots");
			
			response = ((Controller) controller).registerUser(ip, id, lastName, firstName, birthDate, ballots);
			
		} catch (Exception e) {
			e.printStackTrace();
			response = new Message(Protocol.registerNewUserReq);
			response.addError(e.getMessage());
		}
		
		link.write(response.toB64());
	}
	
	private void updateExistingUser(){
		if(!link.hasNextLine()) return;
		
		Message response;
		try{
			Message request = (Message) Message.fromB64(link.read(), "seggio");
			String[] required = {"ID", "ballots"};
			Class<?>[] types = {String.class, int[].class};

			request.verifyMessage(Protocol.updateExistingUserReq, required, types, "seggio");
			String voterID = request.getElement("ID");
			int[] ballots = request.getElement("ballots");
			response = ((Controller) controller).updateExistingUser(ip, voterID, ballots);
		} catch (Exception e) {
			response = new Message(Protocol.updateExistingUserReq);
			response.addError(e.getMessage());

			e.printStackTrace();
		}

		link.write(response.toB64());
	}
	
	private void genNonces() {
		if(!link.hasNextLine()) {return;}
		
		Message bulkOut = new Message();
		try {
			Message bulkIn = (Message) Message.fromB64(link.read(), "postazione");
			String[] required = {"numPreferences"};
			Class<?>[] types = {int[].class};
			
			bulkIn.verifyMessage(Protocol.nonceReq, required, types, "postazione");
			int[] numPreferences =  bulkIn.getElement("numPreferences");
			bulkOut.setValue(Protocol.nonceAck);
			bulkOut.setElement("nonces", ((Controller) controller).genNonces(ip, numPreferences));
		}
		catch (PEException e) {
			bulkOut.setValue(Protocol.nonceNack);
			bulkOut.addError(e.getMessage());
		}
		
		link.write(bulkOut.toB64());
	}
	
	private void receiveVote() {
		if(!link.hasNextLine()) {
			return;
		}
		
		Message response = new Message(Protocol.votesReceivedNack);
		try {
			Message request = (Message) Message.fromB64(link.read(), "seggio");
			String[] required = {"voter", "encryptedBallots", "ipPost"};
			Class<?>[] types = {Person.class, WrittenBallot[].class, InetAddress.class};
			
			request.verifyMessage(Protocol.sendVoteToUrn, required, types, "seggio");
			Person voter = request.getElement("voter");
			WrittenBallot[] encryptedBallots = request.getElement("encryptedBallots");
			InetAddress ipPost = request.getElement("ipPost");
			
			response = ((Controller) controller).voteReceived(voter, encryptedBallots, ip, ipPost);
		}
		catch(PEException e) {
			response = new Message(Protocol.votesReceivedNack);
			response.addError(e.getMessage());
		}
		finally {
			link.write(response.toB64());
		}
	}
	
	private void logShutDown(Terminals.Type type) {
		((Controller) controller).logShutDown(ip, type);
	}
}
