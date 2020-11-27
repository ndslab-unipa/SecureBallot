package controller;

/**
 * Interfaccia necessaria per implementare il design pattern "Factory".
 * In questo modo il comportamento comune di postazione, seggio, seggio ausiliario e urna,
 * ovvero lo stare in ascolto su di una welcomeSocket e rispondere alle richieste esterne,
 * viene svolto dalla classe base "Controller" di cui le 3 classi sono derivate.
 * Ciò che cambia è come ogni classe debba rispondere alle richieste.
 * Questo viene gestito tramite questa interfaccia.
 * Ognuna delle 4 classi fornirà una Factory differente, che fornisca quindi "Service" differenti.
 */
public interface ServiceFactory {

	public Runnable createService(TerminalController controller, Link link, String name);
	
}
