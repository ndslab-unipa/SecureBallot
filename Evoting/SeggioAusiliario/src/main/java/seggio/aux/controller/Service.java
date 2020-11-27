package seggio.aux.controller;

import controller.AbstrService;
import controller.Link;
import controller.TerminalController;
import exceptions.PEException;
import model.DummyPost;
import model.Message;
import utils.Constants;
import utils.Protocol;

public class Service extends AbstrService {
	public Service(TerminalController controller, Link link, String name) {
		super(controller, link, name);
	}
	
	protected void execute() {
		String message = link.read();
		
		switch (message) {
			case Protocol.updateSubStation:
				if (((Controller) controller).verifyStationIp(ip))
					updateSubStation();
				break;
			
			case Protocol.stationShutDown:
				if (((Controller) controller).verifyStationIp(ip))
					resetSubStation();
				break;
				
			case Protocol.urnShutDown:
				if (((Controller) controller).verifyUrnIp(ip))
					deactivateSubStation();
				break;
				
			//Se il messaggio non è stato riconosciuto si stampa a schermo un errore
			default: 
				String error = "Il terminale [IP: "+ip.getHostAddress()+"] ha inviato il seguente messaggio inatteso: "+message;
				if(Constants.verbose)
					controller.printError("Comunicazione Inattesa", error);
		}
	}
	
	private void updateSubStation() {
		if(!link.hasNextLine()) {
			controller.printError("Errore di Comunicazione", "Il seggio non ha terminato l'invio dei dati richiesti.");
			return;
		}
		
		Message request;
		DummyPost[] posts;
		try {
			request = (Message) Message.fromB64(link.read(), "seggio principale");
			String[] required = {"posts"};
			Class<?>[] types = {DummyPost[].class};
			
			request.verifyMessage(Protocol.updateSubStation, required, types, "seggio");
			posts = request.getElement("posts");
			
			((Controller) controller).updateSubStation(posts);
			
		} catch(PEException e) {
			controller.printError(e);
		}
	}
	
	private void resetSubStation() {
		((Controller) controller).resetSubStation();
	}
	
	private void deactivateSubStation() {
		((Controller) controller).deactivateSubStation();
	}
}
