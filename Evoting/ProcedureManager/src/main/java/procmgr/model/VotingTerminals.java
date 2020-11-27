package procmgr.model;

import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Classe che modella l'insieme di terminali di voto specificati per una singola sessione. Contiene l'oggetto {@link Terminal}
 * relativo al seggio principale, e le liste di oggetti Terminal relative a postazioni e seggi ausiliari di quel seggio.
 */
public class VotingTerminals {
    private final Terminal station;
    private final ArrayList<Terminal> postsIps;
    private final ArrayList<Terminal> subStationsIps;

    /**
     * Costruttore con parametri che inizializza l'oggetto {@link Terminal} relativo al seggio principale e le liste (vuote) degli
     * altri terminali.
     * @param stationID ID del seggio principale
     * @param stationIp Indirizzo IP del seggio principale
     */
    public VotingTerminals(int stationID, InetAddress stationIp) {
        station = new Terminal(stationID, stationIp);
        postsIps = new ArrayList<>();
        subStationsIps = new ArrayList<>();
    }
    
    /**
     * Getter per il seggio principale.
     * @return Oggetto {@link Terminal} contenente ID ed indirizzo IP del seggio principale
     */
    public Terminal getStation(){
        return station;
    }
    
    /**
     * Permette di aggiungere una nuova postazione, dati ID ed IP, alla lista mantenuta nell'oggetto.
     * @param id ID della nuova postazione
     * @param ip Indirizzo IP della nuova postazione
     */
    public void addPost(int id, InetAddress ip) {
        postsIps.add(new Terminal(id, ip));
    }

    /**
     * Getter per la lista di postazioni.
     * @return Lista delle postazioni
     */
    public ArrayList<Terminal> getPosts() {
        return postsIps;
    }

    /**
     * Permette di aggiungere un nuovo seggio ausiliario, dati ID ed IP, alla lista mantenuta nell'oggetto.
     * @param id ID del nuovo seggio ausiliario
     * @param ip Indirizzo IP del nuovo seggio ausiliario
     */
    public void addSubStation(int id, InetAddress ip){
        subStationsIps.add(new Terminal(id, ip));
    }
    
    /**
     * Getter per la lista di seggi ausiliari.
     * @return Lista dei seggi ausiliari
     */
    public ArrayList<Terminal> getSubStations() {
        return subStationsIps;
    }
}
