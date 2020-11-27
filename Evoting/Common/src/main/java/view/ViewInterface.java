package view;

import exceptions.PEException;

public interface ViewInterface {

    /**
     * Funzione per aggiornare la view, richiamabile da qualsiasi parte del codice.
     */
    public void update();

    /**
     * Funzione utilizzata per aggiornare la view, sulla base dello stato conservato nel model. E' richiamabile soltanto 
     * dal thread della view (ovvero il thread principale).
     */
    public void updateFromView();

    /**
     * Funzione per la stampa sul terminale di un messaggio.
     * @param message
     */
    public void println(String message);

    /**
     * Stampa un messaggio che non deve arrivare all'utente finale,
     * ma utile per il debug o per seguire l'andamento dell'app da terminale.
     * @param message Il messaggio.
     */
    public void printMessage(String message);

    /**
     * Stampa un messaggio informativo con informazioni su di una operazione.
     * @param message Il messaggio generico.
     * @param content Il messaggio specifico.
     */
    public void printSuccess(String message, String content);

    /**
     * Stampa un messaggio di errore per una operazione fallita.
     * @param message Il messaggio generico.
     * @param content Il messaggio specifico.
     */
    public void printError(String message, String content);

    /**
     * Stampa un messaggio di errore partendo da una PEException (Print Error Exception).
     * @param pee
     */
    public void printError(PEException pee);

    /**
     * Stampa un messaggio di warning relativo di una operazione.
     * @param message Il messaggio generico.
     * @param content Il messaggio specifico.
     */
    public void printWarning(String message, String content);
    
    public boolean printConfirmation(String message, String content);
}
