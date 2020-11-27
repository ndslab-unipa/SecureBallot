package model;

public class VotePacket extends Parsable{
	private static final long serialVersionUID = 1L;
	
	private final String encryptedVote;
	private final String encryptedSymmetricKey;
	private final String encryptedIV;
	private final String solvedNonce;
	
	//Parametri per la firma della postazione
	private String salt = null;
	private String HMAC = null;
	
	//Parametro per la firma dell'urna
	private String signature = null;
	
	public VotePacket(String encryptedVote, String encryptedSymmetricKey, String encryptedIV, String solvedNonce) {
		this.encryptedVote = encryptedVote;
		this.encryptedSymmetricKey = encryptedSymmetricKey;
		this.encryptedIV = encryptedIV;
		this.solvedNonce = solvedNonce;
	}
	
	public VotePacket(String encryptedVote, String encryptedSymmetricKey, String encryptedIV, String solvedNonce, String signature) {
		this.encryptedVote = encryptedVote;
		this.encryptedSymmetricKey = encryptedSymmetricKey;
		this.encryptedIV = encryptedIV;
		this.solvedNonce = solvedNonce;
		this.signature = signature;
	}
	
	/**
	 * Firma effettuata dalle postazioni.
	 * @param salt
	 * @param HMAC
	 */
	public void sign(String salt, String HMAC) {
		this.salt = salt;
		this.HMAC = HMAC;
	}
	
	/**
	 * Firma effettuata dall'urna.
	 */
	public void sign(String signature) {
		this.signature = signature;
	}
	
	public String getEncryptedVote() {
		return encryptedVote;
	}
	
	public String getEncryptedKi() {
		return encryptedSymmetricKey;
	}
	
	public String getEncryptedIV() {
		return encryptedIV;
	}
	
	public String getSolvedNonce() {
		return solvedNonce;
	}
	
	public String getHMAC() {
		return HMAC;
	}

	public String getSalt() {
		return salt;
	}
	
	public String getSignature() {
		return signature;
	}
	
	public String[] getUnsignedData() {
		String[] packet = {encryptedVote, encryptedSymmetricKey, encryptedIV, solvedNonce};
		return packet;
	}
}
