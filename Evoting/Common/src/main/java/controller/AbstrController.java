package controller;


import db.DB;
import exceptions.PEException;
import model.AbstrModel;
import utils.Constants;
import view.ViewInterface;

/**
 * Controller astratto, adoperato semplicemente per fornire alle classi derivate 
 * dei metodi comuni di comunicazione con la view (PrintWriter).
 */
public class AbstrController {
	private ViewInterface view;
	
	public AbstrController(ViewInterface view) {
		this.view = view;
	}
	
	/**
	 * Funzione che verifica i dati inseriti nella schermata di login. Se almeno uno dei due campi non è stato compilato, allora stampa
	 * automaticamente un dialog di errore. Altrimenti, verifica i dati inseriti nel DB 
	 * (tramite la funzione {@link DB#checkLoginData(String, String)}). 
	 * <br>
	 * In caso di verifica positiva, memorizza i dati dell'utente loggato nel model e restituisce True. Se si verifica un qualunque errore,
	 * stampa un dialog d'errore e restituisce False.
	 * @param model Model del terminale che intende effettuare login
	 * @param db Interfaccia col DB del terminale che intende effettuare login
	 * @param user Username dell'utente
	 * @param psw Password dell'utente
	 * @return True se il login è verificato, false altrimenti
	 */
	public boolean checkLoginData(AbstrModel model, DB db, String user, String psw) {
		if (user == null || psw == null || user.length() == 0 || psw.length() == 0) {
    		printError("Errore di Login", "Inserisci sia username che password");
    		return false;
    	}
		
		model.logInfo("Un nuovo utente [Username: "+user+"] sta provando ad effettuare login.");

		try {
			if (!db.checkLoginData(user, psw)) {
	    		printError("Errore di Login", "Le informazioni inserite non sono corrette");
	    		model.logError("L'utente [Username: "+user+"] ha inserito credenziali di login non valide.");
	    		return false;
	    	}
			
			model.setUsername(user);
			model.setPassword(psw);
			
			model.logSuccess("L'utente [Username: "+user+"] ha completato le operazioni di login.");
			
			if(Constants.verbose)
				printSuccess("Login Completato Correttamente", "Puoi ora accedere alle funzionalità del terminale.");
			
			return true;
			
		} catch (PEException e) {
			printError(e);
			return false;
		}
	}
	
	public boolean confirmLogout(AbstrModel model) {
		if(printConfirmation("Sei sicuro di voler effettuare il logout?", "Verrai riportato alla schermata di login")) {
			model.setUsername(null);
			model.setPassword(null);
			
			model.logWarning("L'utente [Username: "+model.getUsername()+"] ha effettuato il logout.");
			return true;
    	}
		
		return false;
	}
	
	/*Funzioni per comunicare con la view*/
	/**
	 * Funzione adoperata mostrare un messaggio di controllo non pensato per il pubblico ma per il programmatore.
	 * Elimina il testo presente nel debugPane (se si usa una qualche Window) se presente.
	 * @param message Il messaggio da mostrare.
	 */
	public void printMessage(String message) {
		if(view != null) {
			view.printMessage(message);
		}
	}
	
	/**
	 * Funzione adoperata per mandare un generico messaggio alla view.
	 * Stampa in coda al debugPane (se si usa una qualche Window).
	 * @param message Il messaggio da mostrare.
	 */
	public void println(String message) {
		if(view != null) {
			view.println(message);
		}
	}
	
	/**
	 * Funzione adoperata per mandare un messaggio d'errore diretto all'utente.
	 * Se adoperata una qualche Window (WindowPostazione, WindowSeggio),
	 * farà apparire una finestra popup col messaggio di errore.
	 * @param message	Il messaggio d'errore da mostrare.
	 */
	public void printError(String message, String content) {
		if(view != null) {
			view.printError(message, content);
		}
	}
	
	public void printError(PEException e) {
		if(view != null) {
			view.printError(e);
		}
	}
	
	/**
	 * Funzione adoperata per mandare un messaggio diretto all'utente
	 * per indicare che una operazione ha avuto successo.
	 * Se adoperata una qualche Window (WindowPostazione, WindowSeggio),
	 * farà apparire una finestra popup col messaggio.
	 * @param message	Il messaggio da mostrare.
	 */
	public void printSuccess(String message, String content) {
		if(view != null) {
			/*message = message.replace("\n", Protocol.carriage);
			view.println(Protocol.success + message);*/
			view.printSuccess(message, content);
		}
	}
	
	public void printWarning(String msg, String content) {
		if (view != null)
			view.printWarning(msg, content);
	}
	
	public boolean printConfirmation(String msg, String content) {
		if (view != null)
			return view.printConfirmation(msg, content);
		
		return false;
	}
	
	/**
	 * Funzione adoperata per aggiornare la view, mandando un messaggio al ViewListener.
	 */
	public void updateView() {
		if(view != null) {
			//view.println(Protocol.updateView);
			view.update();
		}
	}
	/**
	 * Funzione adoperata per far terminare il thread di ascolto della view.
	 */
	void closeView() {
		if(view != null) {
			//view.close();
		}
	}
	
	public void shutDown() {
		// qualcosa da implementare qui?
	}
}	