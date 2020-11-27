package model;

import java.util.ArrayList;
import java.util.TreeSet;

import encryption.VoteEncryption;
import exceptions.FLRException;
import exceptions.PEException;
import utils.Protocol;

public class WrittenBallot extends Parsable {

	private static final long serialVersionUID = 1L;
	
	//Codice univoco della scheda;
	private final int code;
	
	//Titolo della scheda.
	private final String title;
	
	//Numero di preferenze esprimibili.
	private final int maxPref;
	
	//Insieme di codici univoci della scheda (ID di candidati o opzione per intero) selezionati dal votante.
	private TreeSet<String> preferencesSet;
	
	//Pacchetti di voto cifrati
	private ArrayList<VotePacket> encryptedVotePackets;
	
	/**
	 * Costruttore.
	 * @param title Titolo della scheda.
	 * @param code Codice univoco della scheda.
	 * @param maxPref Numero di preferenze esprimibili.
	 */
	public WrittenBallot(String title, int code, int maxPref) {
		this.title = title;
		this.code = code;
		this.maxPref = maxPref;
		this.preferencesSet = new TreeSet<String>();
		this.encryptedVotePackets = new ArrayList<VotePacket>(maxPref);
	}
	
	/**
	 * Costruttore di (quasi) copia.
	 */
	@SuppressWarnings("unchecked")
	public WrittenBallot(WrittenBallot wb) {
		this.title = wb.title;
		this.code = wb.code;
		this.maxPref = wb.maxPref;
		this.preferencesSet = (TreeSet<String>) wb.preferencesSet.clone();
		this.encryptedVotePackets = new ArrayList<VotePacket>(maxPref);
	}
	
	/**
	 * Restituisce il titolo della scheda.
	 * @return Il titolo della scheda.
	 */
	public String getTitle() {
		return title;
	}
	
	public int getCode() {
		return code;
	}
	
	/**
	 * Indica se il candidato passato è stata selezionato dal votante.
	 * @param candidateID Identificativo del candidato scelto.
	 * @return Vero o falso a seconda se il candidato è stata scelto dal votante.
	 */
	public boolean chosenCandidate(String candidateID) {
		return preferencesSet.contains(candidateID);
	}

	/**
	 * Indica se l'opzione passata è stata selezionata dal votante.
	 * @param option Identificativo dell' opzione scelta.
	 * @return Vero o falso a seconda se l' opzione è stata scelta dal votante.
	 */
	public boolean chosenOption(String option) {
		String markedOption = Protocol.isOption + option;
		return preferencesSet.contains(markedOption);
	}

	/**
	 * Aggiunge una preferenza per il candidato (o opzione) identificata dal parametro.
	 * @param personID L'identificativo del candidato (o opzione).
	 * @return Vero o falso a seconda se l'operazione è andata a buon fine.
	 */
	public boolean addPreference(String personID) {
		if(preferencesSet.size() >= maxPref) {
			return false;
		}
		
		return preferencesSet.add(personID);
	}
	
	/**
	 * Rimuove la preferenza per il candidato (o opzione) identificata dal parametro.
	 * @param personID L'identificativo del candidato (o opzione).
	 * @return Vero o falso a seconda se l'operazione è andata a buon fine.
	 */
	public boolean removePreference(String personID) {
		return preferencesSet.remove(personID);
	}
	
	/**
	 * Cifra tutte le preferenze in modo che solo il responsabile di procedimento possa decifrarle
	 * (effettuata dalla postazione prima di inviare le schede al seggio).
	 * @param Kpu_rp Chiave pubblica del responsabile di procedimento.
	 * @param encryptedNonces Nonce cifrati inviati dall'urna come sfida per l'autenticazione.
	 * @param sessionKey Chiave di sessione inserita all'attivazione.
	 * @throws PEException
	 */
	public void encryptBallot(byte[] Kpu_rp, String[] encryptedNonces, String sessionKey) throws PEException {
		encryptedVotePackets.clear();
		
		if(encryptedNonces.length != maxPref) {
			throw FLRException.FLR_03(0);
		}
		
		int index = 0;
		for(String preference : preferencesSet) {
			VotePacket packet = VoteEncryption.encrypt(preference, Kpu_rp, encryptedNonces[index], sessionKey);
			index++;
			
			encryptedVotePackets.add(packet);
		}
		
		if (index == 0) {
			VotePacket packet = VoteEncryption.encrypt(Protocol.emptyBallot, Kpu_rp, encryptedNonces[index], sessionKey);
			
			index++;
			encryptedVotePackets.add(packet);
		}
		
		for(int i = maxPref - index; i > 0; i--) {
			VotePacket emptyPacket = VoteEncryption.encrypt(Protocol.emptyPreference, Kpu_rp, encryptedNonces[index], sessionKey);
			index++;
			
			encryptedVotePackets.add(emptyPacket);
		}
		
		preferencesSet.clear();
	}
	
	public ArrayList<String> getSolvedNonces(){
		ArrayList<String> solvedNonces = new ArrayList<String>();
		
		for(VotePacket packet : encryptedVotePackets){
			solvedNonces.add(packet.getSolvedNonce());
		}
		
		return solvedNonces;
	}
	
	public ArrayList<VotePacket> getEncryptedVotePackets() {
		return encryptedVotePackets;
	}
	
	public int getMaxPreferences() {
		return maxPref;
	}
	
	public int temp() {
		return preferencesSet.size();
	}
}
