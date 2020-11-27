package utils;

/**
 * Classe che contiene le stringhe associate ai diversi messaggi del protocollo che i terminali utilizzano
 * per la comunicazione. Ogni stringa costituisce l'intestazione di un messaggio.
 */
public class Protocol {
	
	/* ********************************** */
	/* --- Messaggi di autenticazione --- */
	/* ********************************** */
	
	/**
	 * Messaggio corrispondente alla prima fase di autenticazione di un seggio. In questa prima fase,
	 * il seggio contatta l'urna all'indirizzo IP noto e le invia un nonce, chiedendo quindi
	 * all'urna di autenticarsi.
	 */
	public static final String StationAuthenticationPhase1 = "<<activateStation>>";
	
	/**
	 * Messaggio corrispondente alla seconda fase di autenticazione di un seggio. Durante questa seconda fase,
	 * l'urna, dopo essersi autenticata col seggio, richiede al seggio di autenticarsi a sua volta,
	 * inviando un nonce.
	 */
	public static final String StationAuthenticationPhase2 = "<<authenticateStation>>";
	
	/**
	 * Messaggio corrispondente alla prima fase di autenticazione di un seggio ausiliario. In questa prima fase,
	 * il seggio ausiliario contatta l'urna all'indirizzo IP noto e le invia un nonce, chiedendo quindi
	 * all'urna di autenticarsi.
	 */
	public static final String SubStationAuthenticationPhase1 = "<<activateSubStation>>";
	
	/**
	 * Messaggio corrispondente alla seconda fase di autenticazione di un seggio ausiliario. Durante questa seconda fase,
	 * l'urna, dopo essersi autenticata col seggio ausiliario, richiede al seggio ausiliario di autenticarsi a sua volta, 
	 * inviando un nonce.
	 */
	public static final String SubStationAuthenticationPhase2 = "<<authenticateSubStation>>";
	
	/**
	 * Messaggio corrispondente alla prima fase di autenticazione di una postazione. In questa prima fase,
	 * la postazione contatta l'urna all'indirizzo IP noto e le invia un nonce, chiedendo quindi
	 * all'urna di autenticarsi.
	 */
	public static final String PostAuthenticationPhase1 = "<<activatePost>>";
	
	/**
	 * Messaggio corrispondente alla seconda fase di autenticazione di una postazione. Durante questa seconda fase,
	 * l'urna, dopo essersi autenticata con la postazione, richiede alla stessa di autenticarsi a sua volta, 
	 * inviando un nonce.
	 */
	public static final String PostAuthenticationPhase2 = "<<authenticatePost>>";
	
	/**
	 * Messaggio di risposta inviato dall'urna a qualunque terminale in caso di autenticazione valida, consente di passare
	 * dalla prima fase di autenticazione (urna con terminale) alla seconda (terminale con urna), o di
	 * completare la fase di autenticazione mutua.
	 */
	public static final String validAuthentication = "<<activationGranted>>";
	
	/**
	 * Messaggio di risposta inviato dall'urna a qualunque terminale in caso di autenticazione fallita. Blocca il processo di
	 * mutua autenticazione ed interrompe l'attivazione del terminale.
	 */
	public static final String authenticationFailed = "<<activationDenied>>";
	
	/**
	 * Messaggio inviato da qualunque terminale all'urna per verificare che l'urna riconosca ancora il terminale come
	 * autenticato. Ogni tot secondi, i terminali contattano l'urna per effettuare questa verifica. Questo è un controllo di sicurezza
	 * aggiuntivo che permette di resettare i terminali in caso di crash dell'urna.
	 */
	public static final String checkTerminalAuthentication = "<<checkTerminalAuth>>";
	
	/**
	 * Messaggio di risposta a {@link #checkTerminalAuthentication}, inviato dall'urna ad un terminale che ha chiesto
	 * di verificare la propria autenticazione, e la cui verifica è andata a buon fine.
	 */
	public static final String authenticatedAck = "<<terminalAuthAck>>";
	
	/**
	 * Messaggio di risposta {@link #checkTerminalAuthentication}, inviato dall'urna ad un terminale che ha chiesto
	 * di verificare la propria autenticazione, e la cui verifica <b>non</b> è andata a buon fine.
	 */
	public static final String authenticatedNack = "<<terminalAuthNack>>";
	
	/* ****************************************** */
	/* --- Messaggi per segnalare spegnimento --- */
	/* ****************************************** */

	/**
	 * Messaggio utilizzato dall'urna per segnalare la sua disattivazione. E' utilizzato quando il software viene terminato, l'utente effettua il logout o
	 * la sessione corrente viene chiusa. Il messaggio è mandato a tutti i terminali autenticati; i terminali che ricevono questo messaggio vengono 
	 * disattivati a loro volta.
	 */
	public static final String urnShutDown = "<<urnShutDown>>";
	
	/**
	 * Messaggio utilizzato dal seggio per segnalare la sua disattivazione. E' utilizzato quando il software viene terminato. Il messaggio è mandato
	 * sia all'urna che a tutte le postazioni associate al seggio. L'urna aggiorna l'elenco dei terminali attivi; ogni postazione viene riportate allo stato ATTIVA.
	 */
	public static final String stationShutDown = "<<stationShutDown>>";
	
	/**
	 * Messaggio utilizzato dal seggio ausiliario per segnalare la sua disattivazione. E' utilizzato quando il software viene terminato. Il messaggio
	 * è mandato solo all'urna, che lo utilizza per aggiornare l'elenco dei terminali attivi ed autenticati.
	 */
	public static final String subStationShutDown = "<<subStationShutDown>>";
	
	/**
	 * Messaggio utilizzato dalla postazione per segnalare la sua disattivazione. E' utilizzato quando il software viene terminato. Il messaggio è mandato
	 * sia all'urna che al seggio a cui la postazione appartiene. L'urna lo utilizza per aggiornare l'elenco dei terminali attivi; il seggio, invece, aggiorna
	 * l'elenco delle postazioni utilizzabili per creare associazioni.
	 */
	public static final String postShutDown = "<<postShutDown>>";
	
	/* ************************************************* */
	/* --- Messaggi di recupero/modifica dello stato --- */
	/* ************************************************* */
	
	/**
	 * Messaggio inviato dal seggio, al momento della sua attivazione, a tutte le postazioni ad esso associate (di cui
	 * viene informato dall'urna). Il messaggio costituisce una richiesta di conoscere lo stato di ogni postazione.
	 */
	public static final String retrieveStatePost = "<<requestPostState>>";
	
	/**
	 * Messaggio di risposta a {@link #retrieveStatePost}, inviato dalle postazioni al seggio. In seguito alla richiesta
	 * del seggio, ogni postazione comunica il suo stato corrente attraverso questo messaggio.
	 */
	public static final String informStatePost = "<<responsePostState>>";

	/**
	 * Messaggio utilizzato dal thread del seggio dedicato al polling delle postazioni. Ogni tot secondi, il seggio invia
	 * questo messaggio alle sue postazioni per conoscerne lo stato. Questo è un controllo di sicurezza aggiuntivo, che consente
	 * di mantenere consistenti le informazioni in caso di crash o disconnessione delle postazioni.
	 */
	public static final String checkUnreachablePost = "<<checkReachability>>";
	
	/**
	 * Messaggio inviato dal seggio ad una delle postazioni per richiederne il reset.
	 */
	public static final String resetPostReq = "<<resetPostReq>>";
	
	/**
	 * Messaggio di risposta a {@link #resetPostReq}, inviato da una postazione al suo seggio. Informa il seggio
	 * di aver effettuato il reset, esser tornata allo stato "ATTIVA" ed aver rimosso l'eventuale associazione
	 * presente.
	 */
	public static final String resetPostGranted = "<<resetPostGrant>>";
	
	/**
	 * Messaggio di risposta a {@link #resetPostReq}, inviato da una postazione al suo seggio. Informa il seggio
	 * di <b>non</b> aver potuto completare il reset. Generalmente, questo avviene quando la postazione è in uno
	 * stato non resettabile, come NON ATTIVA o DA RIAVVIARE.
	 */
	public static final String resetPostDenied = "<<resetPostDenied>>";
	
	/* ******************************************************************************************** */
	/* --- Messaggi per la gestione delle associazione delle postazioni (creazione/distruzione) --- */
	/* ******************************************************************************************** */
	
	/**
	 * Messaggio utilizzato dal seggio per comunicare alle postazioni la richiesta di nuova associazione.
	 * L'associazione permette di "occupare" una postazione e consentire di effettuare la procedura di voto. 
	 */
	public static final String associationReq = "<<associationReq>>";
	
	/**
	 * Messaggio di risposta a {@link #associationReq}, inviato da una postazione al suo seggio. Comunica al
	 * seggio che l'associazione è andata a buon fine.
	 */
	public static final String associationAck = "<<associationAck>>";
	
	/**
	 * Messaggio di risposta a {@link #associationReq}, inviato da una postazione al suo seggio. Comunica al
	 * seggio che l'associazione <b>non</b> è andata a buon fine.
	 */
	public static final String associationNack = "<<associationNack>>";
	
	/**
	 * Messaggio inviato dal seggio ad una sua postazione, nel momento in cui il badge associato a quella postazione
	 * viene riportato al seggio e passato sul lettore, per confermare il termine delle operazioni di voto.
	 */
	public static final String postEndVoteReq = "<<postEndVoteReq>>";
	
	/**
	 * Messaggio di risposta a {@link #postEndVoteReq}, inviato da una postazione al suo seggio, per confermare
	 * la distruzione dell'associazione della postazione ed il suo conseguente ritorno allo stato ATTIVA.
	 */
	public static final String postEndVoteAck = "<<postEndVoteAck>>";
	
	/**
	 * Messaggio di risposta a {@link #postEndVoteReq}, inviato da una postazione al suo seggio, per informare il seggio
	 * di eventuali errori avvenuti durante la distruzione dell'associazione.
	 */
	public static final String postEndVoteNack = "<<postEndVoteNack>>";
	
	/* ************************************************** */
	/* --- Messaggi per l'invio dei pacchetti di voto --- */
	/* ************************************************** */
	
	/**
	 * Messaggio utilizzato dalla postazione per richiedere all'urna, alla fine della fase di votazione, di generare un certo
	 * numero di nonce, a seconda dei voti esprimibili dal votante associato alla postazione. Gli nonce vengono risolti dalla postazione,
	 * cifrati ed appesi al pacchetto di voto finale.
	 */
	public static final String nonceReq = "<<nonceReq>>";
	
	/**
	 * Messaggio di risposta a {@link #nonceReq}, inviato dall'urna alla postazione per confermare l'invio degli nonce.
	 */
	public static final String nonceAck = "<<nonceAck>>";
	
	/**
	 * Messaggio di risposta {@link #nonceReq}, inviato dall'urna alla postazione per segnalare un qualche errore avvenuto durante la generazione
	 * degli nonce. Ricevuto questo messaggio, la postazione non può procedere con l'invio dei voti, quindi si mette in attesa (stato VOTO PENDING).
	 */
	public static final String nonceNack = "<<nonceNack>>";
	
	/**
	 * Messaggio utilizzato dalla postazione per inviare il pacchetto di voto al seggio. Finita la votazione e ricevuti gli nonce dall'urna,
	 * la postazione cifra il pacchetto di voto ed invia il tutto al seggio. In attesa di essere liberata, si mette quindi nello stato VOTO INVIATO.
	 */
	public static final String sendVoteToStation = "<<sendVoteToStation>>";
	
	/**
	 * Messaggio utilizzato dal seggio per inviare il pacchetto di voto, precedentemente ricevuto da una postazione, all'urna. Il seggio, ricevuti i voti,
	 * e letta la card RFID corrispondente, inoltra i voti all'urna e libera definitivamente la postazione.
	 */
	public static final String sendVoteToUrn = "<<sendVoteToUrn>>";
	
	/**
	 * Messaggio utilizzato dal seggio e dall'urna per confermare la ricezione dei voti, rispettivamente da una postazione e da un seggio.
	 */
	public static final String votesReceivedAck = "<<votesReceivedAck>>";
	
	/**
	 * Messaggio utilizzato dal seggio e dall'urna per segnalare un errore nella ricezione dei voti, rispettivamente da una postazione e da un seggio.
	 */
	public static final String votesReceivedNack = "<<votesReceivedNack>>";
	
	/* ****************************************************** */
	/* --- Messaggi per funzionalità esclusive del seggio --- */
	/* ****************************************************** */
	
	/**
	 * Messaggio utilizzato dal seggio per richiedere all'urna la registrazione di un nuovo utente all'interno della procedura corrente.
	 */
	public static final String registerNewUserReq = "<<registerNewUser>>";
	
	/**
	 * Messaggio di risposta a {@link #registerNewUserReq}, inviato dall'urna al seggio per confermare l'avvenuto inserimento di un nuovo utente
	 * all'interno del database.
	 */
	public static final String registerNewUserAck = "<<registerNewUserAck>>";
	
	/**
	 * Messaggio di risposta a {@link #registerNewUserReq}, inviato dall'urna al seggio per segnalare errori avvenuti durante l'inserimento di un nuovo 
	 * utente all'interno del database.
	 */
	public static final String registerNewUserNack = "<<registerNewUserNack>>";
	
	/**
	 * Messaggio utilizzato dal seggio per richiedere all'urna di effettuare l'aggiornamento dei dati di un votante già registrato all'interno del DB. 
	 */
	public static final String updateExistingUserReq = "<<updateExistingUserReq>>";
	
	/**
	 * Messaggio di risposta a {@link #updateExistingUserReq}, inviato dall'urna al seggio per confermare l'avvenuto aggiornamento dei dati di
	 * un votante.
	 */
	public static final String updateExistingUserAck= "<<updateExistingUserAck>>";
	
	/**
	 * Messaggio di risposta a {@link #updateExistingUserReq}, inviato dall'urna al seggio per segnalare errori avvenuti durante l'aggiornamento dei
	 * dati di un votante.
	 */
	public static final String updateExistingUserNack = "<<updateExistingUserNack>>";
	
	/**
	 * Messaggio utilizzato dal seggio per comunicare ad una postazione il suo nuovo stato. E' utilizzato solo nel caso in cui il thread del seggio che
	 * fa polling sulle postazioni nota cambiamenti di stato non coerenti. Quindi, server a comunicare alla postazione di mettersi nello stato DA RESETTARE
	 * o DA RIAVVIARE.
	 */
	public static final String changePostState = "<<changePostState>>";
	
	/**
	 * Messaggio utilizzato dal seggio per comunicare ad un seggio ausiliario il nuovo stato delle postazioni, in modo da aggiornarlo e mantenere
	 * sincronizzate le informazioni.
	 */
	public static final String updateSubStation = "<<updateSubStation>>";
	
	/* ******************************************************************************* */
	/* --- Messaggi per funzionalità esclusive dei seggi (principali ed ausiliari) --- */
	/* ******************************************************************************* */
	
	/**
	 * Messaggio utilizzato dal seggio per inviare una richiesta di ricerca votanti all'urna.
	 */
	public static final String searchPersonReq = "<<searchPersonReq>>";
	
	/**
	 * Messaggio utilizzato dal seggio ausiliario per inviare una richiesta di ricerca votanti all'urna.
	 */
	public static final String searchPersonSubStationReq = "<<searchPersonSubStationReq>>";
	
	/**
	 * Messaggio di risposta a {@link #searchPersonReq} o {@link #searchPersonSubStationReq}, inviato dall'urna ad un seggio principale o ausiliario, per
	 * confermare l'avvenuta ricerca di votanti nel database ed il conseguente invio di questi.
	 */
	public static final String searchPersonAck = "<<searchPersonAck>>";
	
	/**
	 * Messaggio di risposta a {@link #searchPersonReq} o {@link #searchPersonSubStationReq}, inviato dall'urna ad un seggio principale o ausiliario, per
	 * segnalare errori avvenuti durante la ricerca di votanti nel database.
	 */
	public static final String searchPersonNack = "<<searchPersonNack>>";
	
	/**
	 * Messaggio utilizzato dal seggio ausiliario per richiedere al seggio principale la lettura di una nuova card RFID. Il seggio ausiliario, quindi, 
	 * demanda al seggio principale la creazione/distruzione di associazioni.
	 */
	public static final String processCardReq = "<<processCardReq>>";
	
	/**
	 * Messaggio di risposta a {@link #processCardReq}, inviato dal seggio al seggio ausiliario in caso di errore nella lettura della card inviata dal seggio
	 * ausiliario. 
	 * <br>
	 * A differenza di altri scambi di messaggi, non esiste un <i>processCardAck</i>: in caso non emergano errori, il seggio invia al seggio ausiliario 
	 * {@link #associationAck} (per informare dell'avvenuta creazione di un'associazione) o {@link #votesReceivedAck} (per informare della corretta ricezione dei voti).
	 */
	public static final String processCardNack = "<<processCardNack>>";
	
	/* ************************* */
	/* --- Messaggi generici --- */
	/* ************************* */
	
	/**
	 * Messaggio generico utilizzato per segnalare il successo di un'operazione. E' utilizzato soltanto nei
	 * test.
	 */
	public static final String success = "<<successfulOperation>>";
	
	/**
	 * Messaggio generico utilizzato per segnalare un errore. 
	 */
	public static final String error = "<<error>>";
	
	/* *************************** */
	/* --- Stringhe di utility --- */
	/* *************************** */
	
	/**
	 * Stringa indicante una preferenza non espressa all'interno di una votazione. Ad ogni scheda elettorale è associato un numero massimo di preferenze esprimibili;
	 * in caso queste non vengano utilizzate completamente, questa stringa viene utilizzata fino a completare il numero di preferenze.
	 */
	public static final String emptyPreference = "<<emptyPreference>>";
	
	/**
	 * Stringa indicante una scheda bianca, su cui non è stata espressa alcuna preferenza. Le preferenze associate alla scheda vengono riempite prima con questa stringa,
	 * poi con {@link #emptyPreference} fino al raggiungimento del numero di preferenze esprimibili.
	 */
	public static final String emptyBallot = "<<emptyBallot>>";
	
	/**
	 * Stringa utilizzata per distinguere i voti su opzioni di referendum dai voti su candidati per schede elettorali.
	 */
	public static final String isOption = "<<option>>";

	/**
	 * Stringa utilizzata per identificare le postazioni libere, in attesa di associazione.
	 */
	public static final String unassignedPost = "<<unassignedPost>>";
}
