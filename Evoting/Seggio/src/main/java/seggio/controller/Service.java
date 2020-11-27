package seggio.controller;

import controller.AbstrService;
import controller.Link;
import controller.TerminalController;
import exceptions.PEException;
import model.Message;
import model.Person;
import model.WrittenBallot;
import seggio.model.Station;
import utils.Constants;
import utils.Protocol;

public class Service extends AbstrService {
	private Station station;
	
	public Service(TerminalController controller, Link link, String name) {
		super(controller, link, name);
		station = ((Controller) controller).getStation();
	}
	
	@Override
	protected void execute() {
		//Il primo messaggio indica cosa è richiesto al seggio
		String message = link.read();
		
		if(Constants.devMode && station.getSimulateOffline())
			return;
		
		switch(message) {
			//Se una postazione richiede l'invio dei voti viene richiamata receiveVote
			case Protocol.sendVoteToStation:
				if(((Controller) controller).verifyIp(ip, true))
					receiveVote();
				break;
				
			//Se una postazione invia il proprio stato viene richiamata updateStatePost
			case Protocol.informStatePost:
				if(((Controller) controller).verifyIp(ip, true))
					updateStatePost();
				break;
				
			//Se un seggio ausiliario legge una card RFID viene richiamata readCardForSubStation
			case Protocol.processCardReq:
				if(((Controller) controller).verifyIp(ip, false))
					readCardForSubStation();
				break;
			
			//Se l'urna segnala il proprio spegnimento, allora viene richiamato deactivateStation() per resettare il seggio
			case Protocol.urnShutDown:
				if (((Controller) controller).verifyUrnIp(ip))
					deactivateStation();
				break;
				
			//Se il messaggio non è stato riconosciuto si stampa a schermo un errore
			default: 
				String error = "Il terminale [IP: "+ip.getHostAddress()+"] ha inviato il seguente messaggio inatteso: "+message;
				if(Constants.verbose)
					controller.printError("Comunicazione Inattesa", error);
		}
	}
	
	/**
	 * Funzione che gestisce la ricezione dei voti dalla postazione.
	 */
	private void receiveVote() {
		//Se la postazione termina la connessione informiamo lo staff con un messaggio di errore
		if(!link.hasNextLine()) {
			controller.printError("Errore di Comunicazione", "La postazione "+ ip.getHostAddress() + " non ha inviato i pacchetti di voto.");
			return;
		}
		
		//Vengono recuperati i voti cifrati
		Message request;
		WrittenBallot[] encryptedBallots;
		try {
			request = (Message) Message.fromB64(link.read(), "postazione " + ip.getHostAddress());
			String[] required = {"encryptedBallots"};
			Class<?>[] types = {WrittenBallot[].class};
			
			request.verifyMessage(Protocol.sendVoteToStation, required, types, "postazione " + ip.getHostAddress());
			encryptedBallots = request.getElement("encryptedBallots");
			
			//I voti vengono memorizzati nel seggio in attesa dell'invio all'urna
			if(((Controller) controller).storeVoteLocally(encryptedBallots, ip))
				link.write(Protocol.votesReceivedAck);
			else
				link.write(Protocol.votesReceivedNack);
			
		} catch (PEException e) {
			link.write(Protocol.votesReceivedNack);
			controller.printError(e);
		}
	}
	
	/**
	 * Funzione che aggiorna lo stato della postazione memorizzato.
	 */
	private void updateStatePost() {
		if(!link.hasNextLine()) {
			controller.printError("Errore di Comunicazione", "La postazione " + ip.getHostAddress() + " non ha inviato il proprio stato.");
			return;
		}
		
		String state = link.read();
		((Controller) controller).setStatePost(state, ip);
	}
	
	/**
	 * Funzione che gestisce la richiesta di un seggio ausiliario quando quest'ultimo legge una card RFID.
	 */
	private void readCardForSubStation() {
		if(!link.hasNextLine()) {
			controller.printError("Errore di Comunicazione", "Il seggio ausiliario "+ip.getHostAddress()+" non ha inviato il numero della card.");
			return;
		}
		
		Message request, response = new Message();
		String card;
		Person voter = null;
		try {
			request = (Message) Message.fromB64(link.read(), "seggio ausiliario");
			
			String[] required = {"card"};
			Class<?>[] types = {String.class};
			request.verifyMessage(Protocol.processCardReq, required, types, "seggio ausiliario");
			card = request.getElement("card");
			
			//Si recupera la postazione associata alla card, se esiste
			int postIdx = ((Controller) controller).getPostIdx(card);
			
			//Si verifica se il badge era già associato ad una postazione o se sta venendo usato per crearne una nuova
			if (postIdx == -1) {
				required = new String[]{"card", "voter"};
				types = new Class<?>[]{String.class, Person.class};
				
				request.verifyMessage(Protocol.processCardReq, required, types, "seggio ausiliario");
				voter = request.getElement("voter");
			}
			
			response = ((Controller) controller).readCardForSubStation(card, voter);
			
		} catch (PEException e) {
			response.setValue(Protocol.processCardNack);
			response.addError(e.getMessage());
		}
		
		link.write(response.toB64());
	}
	
	private void deactivateStation() {
		((Controller) controller).deactivateStation();
	}
}
