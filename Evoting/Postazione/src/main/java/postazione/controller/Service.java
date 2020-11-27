package postazione.controller;

import controller.AbstrService;
import controller.Link;
import controller.TerminalController;
import exceptions.PEException;
import model.Message;
import model.State.StatePost;
import postazione.model.Post;
import utils.Constants;
import utils.Protocol;

public class Service extends AbstrService {
	private Post post;
	
	public Service(TerminalController controller, Link link, String name) {
		super(controller, link, name);
		post = ((Controller) controller).getPost();
	}

	@Override
	protected void execute() {
		//Il primo messaggio indica cosa è richiesto alla postazione
		String message = link.read();
		
		// Codice aggiunto per testare il thread di seggio che fa polling sulle postazioni
		if(Constants.devMode && post.getSimulateOffline())
			return;
		// -------------------------------------------------------------------------------

		switch(message) {
			//Se il messaggio è una richiesta di associazione viene richiamata associationRequest
			case Protocol.associationReq:
				if (((Controller) controller).verifyStationIp(ip))
					associationRequest();
				break;
				
			//Se il messaggio indica la fine del voto viene richiamata endVote
			case Protocol.postEndVoteReq:
				if (((Controller) controller).verifyStationIp(ip))
					endVote();
				break;
				
			//Se il messaggio è una richiesta dello stato della postazione viene richiamata sendState
			case Protocol.retrieveStatePost:
				if (((Controller) controller).verifyStationIp(ip))
					sendState();
				break;
				
			//Se il messaggio richiede il reset della postazione viene richiamata resetPost
			case Protocol.resetPostReq:
				if (((Controller) controller).verifyStationIp(ip))
					resetPost(true);
				break;
				
			case Protocol.stationShutDown:
				if (((Controller) controller).verifyStationIp(ip))
					resetPost(false);
				break;
				
			case Protocol.urnShutDown:
				if (((Controller) controller).verifyUrnIp(ip))
					deactivatePost();
				break;

			case Protocol.changePostState:
				if (((Controller) controller).verifyStationIp(ip))
					changeState();
				break;
				
			case Protocol.checkUnreachablePost:
				if (((Controller) controller).verifyStationIp(ip))
					notifyReachable();
				break;

			//Se il messaggio non è stato riconosciuto si stampa a schermo un errore
			default: 
				String error = "Il terminale [IP: "+ip.getHostAddress()+"] ha inviato il seguente messaggio inatteso: "+message;
				if(Constants.verbose)
					controller.printError("Comunicazione Inattesa", error);
		}
	}
	
	/**
	 * Funzione che gestisce la richiesta di associazione da parte del seggio.
	 */
	private void associationRequest() {
		if(!link.hasNextLine()) {
			controller.printError("Errore di Comunicazione", "Il seggio non ha terminato l'invio dei dati richiesti.");
			return;
		}

		Message response = new Message();
		int[] ballotCodes;
		String badge;
		try {
			//Verifichiamo che il seggio abbia inviato tutti i dati necessari
			Message request = (Message) Message.fromB64(link.read(), "seggio");
			String[] required = {"ballotCodes", "badge"};
			Class<?>[] types = {int[].class, String.class};
			
			request.verifyMessage(Protocol.associationReq, required, types, "seggio");
			ballotCodes = request.getElement("ballotCodes");
			badge = request.getElement("badge");
			
			response = ((Controller) controller).setAssociation(ballotCodes, badge);
			
		} catch(PEException e) {
			//Se la richiesta non è completa informiamo il seggio
			controller.printError(e);
			
			response.setValue(Protocol.associationNack);
			response.addError(e.getMessage());
		}
	
		//Si invia il messaggio al seggio
		link.write(response.toB64());
	}
	
	/**
	 * Funzione che libera la postazione ricevuto il messaggio di termine del voto dal seggio.
	 */
	private void endVote() {
		try {
			((Controller) controller).badgeOut();
			link.write(Protocol.postEndVoteAck);
		}
		catch (PEException e) {
			controller.printError(e);
			link.write(Protocol.postEndVoteNack);
		}
	}
	
	/**
	 * Risponde inviando il proprio stato al seggio.
	 */
	private void sendState() {
		try {
			((Controller) controller).notifyStateToStation(ip);
		} catch (PEException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Funzione che resetta lo stato della postazione se il seggio lo richiede.
	 */
	private void resetPost(boolean produceResponse) {
		try {
			((Controller) controller).resetPost();
			
			if (produceResponse)
				link.write(Protocol.resetPostGranted);
			
		} catch (PEException e) {
			controller.printError(e);
			
			if (produceResponse)
				link.write(Protocol.resetPostDenied);
		}
	}

	private void deactivatePost() {
		try {
			((Controller) controller).deactivatePost();

		} catch(PEException e) {
			controller.printError(e);
		}
	}
	
	private void changeState() {
		if(!link.hasNextLine()) {
			controller.printError("Errore di Comunicazione", "Il seggio non ha terminato l'invio dei dati necessari al cambio stato.");
			return;
		}
		
		StatePost newState;
		try {
			//Verifichiamo che il seggio abbia inviato tutti i dati necessari
			Message request = (Message) Message.fromB64(link.read(), "seggio");
			String[] required = {"newState"};
			Class<?>[] types = {StatePost.class};
			
			request.verifyMessage(Protocol.changePostState, required, types, "seggio");
			newState = request.getElement("newState");
			
			((Controller) controller).setNewState(ip, newState);
		} catch(PEException e) {
			//Se la richiesta non è completa informiamo il seggio
			controller.printError(e);
		}
	}

	private void notifyReachable() {
		Message response = new Message(Protocol.checkUnreachablePost);
		response.setElement("state", ((Controller) controller).getPost().getState());
		response.setElement("card", ((Controller) controller).getPost().getBadge());
		link.write(response.toB64());
	}
}
