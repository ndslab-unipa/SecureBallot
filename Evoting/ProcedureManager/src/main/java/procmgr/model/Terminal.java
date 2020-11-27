package procmgr.model;

import java.net.InetAddress;

/**
 * Classe che modella un singolo terminale di voto, contiene il suo ID ed il suo IP.
 */
public class Terminal {
    private int id;
    private InetAddress ip;

    /**
     * Costruttore con parametri che inizializza sia l'ID che l'IP del terminale.
     * @param id ID del terminale
     * @param ip Indirizzo IP del terminale
     */
    public Terminal(int id, InetAddress ip){
        this.id = id;
        this.ip = ip;
    }

    /**
	 * Getter per l'ID del terminale.
	 * @return ID del terminale
	 */
    public int getId(){
        return id;
    }

    /**
     * Getter per l'indirizzo IP del terminale.
     * @return Indirizzo IP del terminale
     */
    public InetAddress getIp(){
        return ip;
    }
}
