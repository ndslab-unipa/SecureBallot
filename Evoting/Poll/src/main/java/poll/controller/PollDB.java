package poll.controller;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import db.ConnectionManager;
import db.DBMS;
import db.DB;
import encryption.VoteEncryption;
import exceptions.DBException;
import exceptions.ENCException;
import exceptions.FLRException;
import exceptions.PEException;
import model.Procedure;
import model.VotePacket;
import poll.view.viewmodel.BallotResult;
import utils.Protocol;

/**
 * Classe che permette al modulo Poll di interfacciarsi col database. Estende la classe {@link db.DB DB}.
 */
public class PollDB extends DB {
	private Map<Integer, Integer[]> nullVotesMap;
	
	/**
	 * Costruttore con parametri che inizializza tutti i dati richiesti per potersi connettere al DB.
	 * @param host Indirizzo del DB
	 * @param port Porta del DB
	 * @param schema Schema del DB a cui accedere
	 * @throws PEException Se l'applicazione non riesce a recuperare username e password per connettersi al DB dal file <i>psws.cfg</i>
	 */
	public PollDB(String host, String port, String schema) throws PEException {
		dbms = new DBMS(host, port, schema, "Poll");
	}
	
	/**
	 * Recupera dal DB tutte le procedure associate all'utente loggato, codifica ciascuna di queste in un oggetto di tipo 
	 * {@link model.Procedure Procedure} e li restituisce tutti sotto forma di lista.
	 * @param username Username dell'utente loggato
	 * @return Lista di procedure associate all'utente loggato
	 * @throws PEException Se l'applicazione non riesce a connettersi al DB, se riscontra errori nell'esecuzione delle query SQL, 
	 * se riscontra errori nel recupero dei dati desiderati dal DB
	 */
	public ArrayList<Procedure> getProcedures(String username) throws PEException {
		ArrayList<Procedure> proceduresList = new ArrayList<>();
		
		String query = 	"SELECT P.Code, P.Name, P.Supervisor,  P.Starts, P.Ends, (P.Starts <= NOW() AND P.Ends <= NOW()) AS `Terminated` " + 
    					"FROM `Procedure` P " +
    					"WHERE Supervisor = ? ;";
        
        try (ConnectionManager cManager = dbms.getConnectionManager()) {
			ResultSet rs = cManager.executeQuery(query, username);
			
			while (rs.next()) {
				LocalDateTime start = rs.getTimestamp("Starts").toLocalDateTime();
				LocalDateTime end = rs.getTimestamp("Ends").toLocalDateTime();
				proceduresList.add(new Procedure(rs.getInt("Code"), rs.getString("Name"), rs.getString("Supervisor"), start, end, rs.getBoolean("Terminated")));
			}
			
		} catch (SQLException e) {
			throw DBException.DB_0(e);
		}
        
		return proceduresList;
	}
	
	/**
	 * Esegue lo spoglio dei voti memorizzati sul DB e ritorna il numero di pacchetti che hanno causato errore nello spoglio.
	 * Opera come segue:
	 * <ol>
	 * <li>Setta a 0 i voti ricevuti da tutti i candidati e da tutte le opzioni presenti nella la procedura selezionata. </li>
	 * <li>Recupera dal DB tutti i pacchetti di voto e li memorizza all'interno di una mappa, indicizzata sul codice della scheda
	 * a cui appartiene il voto. Ogni pacchetto di voto è memorizzato come oggetto di tipo {@link model.VotePacket VotePacket}.</li>
	 * <li>Recupera dal DB la chiave pubblica 2 del responsabile, da utilizzare per verificare la firma apposta su ogni pacchetto.</li>
	 * <li>Recupera dal DB la chiave privata 1 del responsabile, da utilizzare per decifrare i pacchetti di voto.</li>
	 * <li>Per ogni pacchetto di voto:
	 * <ol>
	 * <li>Verifica la firma apposta sul pacchetto. Scarta il pacchetto se la verifica non passa.</li>
	 * <li>Decifra il voto, o scarta il pacchetto se non riesce.</li>
	 * <li>Se il voto è una preferenza nulla o una scheda vuota, incrementa i relativi contatori per la scheda a cui appartiene il voto.</li>
	 * <li>Altrimenti, incrementa di 1 i voti ricevuti dal relativo candidato/opzione nel DB.</li>
	 * </ol>
	 * </li>
	 * <li>Restituisce il numero di pacchetti che hanno causato errore nella verifica della firma o nella decifratura.</li>
	 * </ol>
	 * @param procCode Codice della procedura selezionata
	 * @param user Username dell'utente loggato
	 * @param psw Password dell'utente loggato
	 * @return Numero di pacchetti che hanno causato errore nella verifica della firma o nella decifratura della preferenza.
	 * @throws PEException Se l'applicazione non riesce a connettersi al DB, se riscontra errori nell'esecuzione delle query SQL, 
	 * se riscontra errori nel recupero dei dati desiderati dal DB, se non esistono chiavi RSA per l'utente loggato
	 */
	public int countVotes(int procCode, String user, String psw) throws PEException {
		resetVotesReceived(procCode);
		Map<Integer, ArrayList<VotePacket>> votesMap = getVotesMap(procCode);
		return processVotes(procCode, votesMap, user, psw);
	}
	
	/**
	 * Permette di ritornare al Controller la lista di risultati elettorali. Per ogni scheda crea un oggetto di tipo 
	 * {@link BallotResult}, quindi recupera l'elenco di candidati/opzioni e voti ricevuti per inserirli all'interno
	 * dell'oggetto stesso. Inoltre, per ogni scheda inserisce il numero di preferenze non espresse e di schede bianche.
	 * @param procCode Codice della procedura selezionata
	 * @return Lista di risultati elettorali
	 * @throws PEException Se l'applicazione non riesce a connettersi al DB, se riscontra errori nell'esecuzione delle query SQL, 
	 * se riscontra errori nel recupero dei dati desiderati dal DB
	 */
	public ArrayList<BallotResult> getResults(int procCode) throws PEException {
		ArrayList<BallotResult> results = new ArrayList<>();
		String query = "SELECT * FROM Ballot WHERE ProcedureCode = ? ;";
		
		//Inizialmente, si recuperano dal DB tutte le schede presenti per la procedura corrente
		try (ConnectionManager cm = dbms.getConnectionManager()) {
			ResultSet rs = cm.executeQuery(query, procCode);
			
			while (rs.next()) {
				//Per ciascuna scheda, si inizializza l'oggetto BallotResult con i dati identificativi della stessa
				results.add(new BallotResult(rs.getString("Code"), rs.getString("Name"), rs.getString("Description")));
			}
		} catch (SQLException e) {
			throw DBException.DB_0(e);
		}
		
		for (BallotResult ballot : results) {
			//Per ogni scheda, si recuperano dal DB tutti i candidati

			query = "SELECT * " +
					"FROM Candidate AS C " +
					"JOIN Running AS R ON C.ProcedureCode = R.ProcedureCode AND C.ID = R.CandidateID " +
					"WHERE R.ProcedureCode = ? AND R.BallotCode = ?;";
			
			try (ConnectionManager cm = dbms.getConnectionManager()) {
				ResultSet rs = cm.executeQuery(query, procCode, ballot.getId());
				
				while (rs.next()) {
					//Per ogni candidato, si inseriscono i suoi dati e i voti da questo ricevuti all'interno della corrispondente BallotResult
					ballot.insertCandidateVotes(rs.getString("C.ID"), rs.getString("C.FirstName"), rs.getString("C.LastName"), rs.getInt("R.VotesReceived"));
				}
			} catch (SQLException e) {
				throw DBException.DB_0(e);
			}

			//Per ogni scheda, si recuperano dal DB tutte le opzioni
			query = "SELECT * FROM ReferendumOption O WHERE O.ProcedureCode = ? AND O.BallotCode = ? ;";
			
			try (ConnectionManager cm = dbms.getConnectionManager()) {
				ResultSet rs = cm.executeQuery(query, procCode, ballot.getId());
				
				while (rs.next()) {
					//Per ogni opzione, si inseriscono i suoi dati e i voti da questa ricevuti all'interno della corrispondente BallotResult
					ballot.insertOptionVotes(rs.getString("O.Text"), rs.getInt("O.VotesReceived"));
				}
			} catch (SQLException e) {
				throw DBException.DB_0(e);
			}
			
			//Per ogni scheda, si inseriscono all'interno della corrispondente BallotResult il numero di preferenze nulle e di schede bianche
			int ballotCode = Integer.valueOf(ballot.getId());
			ballot.setEmptyBallots(nullVotesMap.get(ballotCode)[0]);
			ballot.setNullPreferences(nullVotesMap.get(ballotCode)[1]);
		}
		
		return results;
	}
	
	/**
	 * Esegue una delle operazioni preliminari per lo spoglio: setta a 0 i voti ricevuti da ogni candidato ed ogni opzione
	 * presenti sulla procedura corrente.
	 * @param procCode Codice della procedura selezionata
	 * @throws PEException Se l'applicazione non riesce a connettersi al DB, se riscontra errori nell'esecuzione delle query SQL
	 */
	private void resetVotesReceived(int procCode) throws PEException {
		String resetCandidatesQuery = "UPDATE Running SET VotesReceived = 0 WHERE ProcedureCode = ? ;";
		String resetOptionsQuery = "UPDATE ReferendumOption SET VotesReceived = 0 WHERE ProcedureCode = ? ;";
		
		ConnectionManager cm = dbms.getConnectionManager();
		try {
			cm.startTransaction();
			cm.executeUpdate(resetCandidatesQuery, procCode);
			cm.executeUpdate(resetOptionsQuery, procCode);
			cm.commit();
		} 
		catch (PEException e) {
			cm.rollback();
			throw DBException.DB_0(e);
		} 
		finally {
			cm.close();
		}
	}
	
	/**
	 * Crea e popola la mappa dei voti memorizzati nel DB. La mappa è indicizzata sul codice della scheda 
	 * e contiene, per ciascuna scheda, l'elenco dei pacchetti di voto appartenenti a quella scheda, sotto forma di ArrayList di 
	 * {@link model.VotePacket VotePacket}.
	 * @param procCode Codice della procedura selezionata
	 * @return Mappa dei voti presenti nel database
	 * @throws PEException Se l'applicazione non riesce a connettersi al DB, se riscontra errori nell'esecuzione delle query SQL, 
	 * se riscontra errori nel recupero dei dati desiderati dal DB
	 */
	private Map<Integer, ArrayList<VotePacket>> getVotesMap(int procCode) throws PEException {
		Map<Integer, ArrayList<VotePacket>> votesMap = getEmptyVotesMap(procCode);
		
		//Si recuperano dal DB tutti i pacchetti di voto corrispondenti alla procedura selezionata
		String query = "SELECT * FROM Vote WHERE ProcedureCode = ? ORDER BY BallotCode;";
		
		try (ConnectionManager cm = dbms.getConnectionManager()) {
			ResultSet rs = cm.executeQuery(query, procCode);
			
			while (rs.next()) {
				//Per ogni voto si crea l'oggetto VotePacket e lo si inserisce all'interno della mappa votesMap
				int ballotCode = rs.getInt("BallotCode");
				
				VotePacket currVote = new VotePacket(
						rs.getString("EncryptedString"), rs.getString("EncryptedKey"), rs.getString("EncryptedIV"), 
						rs.getString("EncryptedNonce"), rs.getString("Signature")
		    	);
				
				if (votesMap.get(ballotCode) != null)
					votesMap.get(ballotCode).add(currVote);
			}
			
		} catch (SQLException e) {
			throw DBException.DB_0(e);
		}
		
		return votesMap;
	}
	
	/**
	 * Recupera le chiavi RSA necessarie allo spoglio (chiave pubblica 2 e chiave privata 1) e richiama, 
	 * per ogni pacchetto di voto, la funzione {@link #processVotePacket(int, int, VotePacket, byte[], byte[])} passandogli il codice della procedura
	 * , il codice della scheda, il pacchetto di voto cifrato e le chiavi RSA. Questa funzione esegue 
	 * tutte le procedure necessarie alla verifica, decifratura e conteggio del voto.
	 * @param procCode Codice della procedura selezionata
	 * @param votesMap Mappa contenente i voti presenti nel database per ogni scheda
	 * @param user Username dell'utente loggato
	 * @param psw Password dell'utente loggato
	 * @return Numero di pacchetti di voto che hanno causato errori e non sono stati conteggiati per lo spoglio
	 * @throws PEException Se l'applicazione non riesce a connettersi al DB, se riscontra errori nell'esecuzione delle query SQL, 
	 * se riscontra errori nel recupero dei dati desiderati dal DB, se non esistono chiavi RSA per l'utente loggato
	 */
	private int processVotes(int procCode, Map<Integer, ArrayList<VotePacket>> votesMap, String user, String psw) throws PEException {
		byte[] Kpu_rp = getRSAKey("PublicKey2", user, psw);
		byte[] Kpr_rp = getRSAKey("EncryptedPrivateKey1", user, psw);
		
		//Se una delle due chiavi è null non si può procedere con lo spoglio, quindi viene lanciata l'eccezione FLR_16
		if (Kpu_rp == null || Kpr_rp == null)
			throw FLRException.FLR_16(user, Kpu_rp == null, Kpr_rp == null);
		
		int votesFailed = 0;
		
		nullVotesMap = new HashMap<>();
		for (Integer ballotCode : votesMap.keySet()) {
			nullVotesMap.put(ballotCode, new Integer[]{0,0});
			
			for (VotePacket currVote : votesMap.get(ballotCode)) {
				try {
					//Per ogni pacchetto di voto, si prova a verificare la firma e decifrare la preferenza espressa
					processVotePacket(procCode, ballotCode, currVote, Kpu_rp, Kpr_rp);
				}
				catch (PEException e) {
					//In caso venga lanciata un'eccezione, si aumenta il contatore di pacchetti non conteggiati e si stampa il messaggio 
					System.out.println(new java.util.Date() + " - Errore nella verifica e/o nel conteggio di un pacchetto");
					System.out.println("\t--> "+e.getSpecific());
					votesFailed++;
				}
			}
		}
		
		return votesFailed;
	}
	
	/**
	 * Esegue lo spoglio di un singolo voto. Verifica la firma apposta sul pacchetto, decifra il voto e conteggia la preferenza
	 * espressa. Se il voto corrisponde ad una scheda bianca o ad una preferenza non espressa, incrementa il relativo contatore mantenuto
	 * all'interno della classe.
	 * @param procCode Codice della procedura selezionata
	 * @param ballotCode Codice della scheda a cui appartiene il voto
	 * @param votePacket Pacchetto di voto cifrato
	 * @param Kpu_rp Chiave pubblica 2 del responsabile, utilizzata per verificare la firma
	 * @param Kpr_rp Chiave privata 1 del responsabile, utilizzata per decifrare il voto
	 * @throws PEException Se l'applicazione non riesce a connettersi al DB, se riscontra errori nell'esecuzione delle query SQL, 
	 * se riscontra errori nella verifica della firma del pacchetto, se riscontra errori nella decifratura del voto
	 */
	private void processVotePacket(int procCode, int ballotCode, VotePacket votePacket, byte[] Kpu_rp, byte[] Kpr_rp) throws PEException {
 		if (!VoteEncryption.verifyPacketSignature(votePacket, Kpu_rp)) {
 			//Si verifica la firma apposta sul pacchetto. Se la verifica è negativa viene lanciata l'eccezione ENC_8
			throw ENCException.ENC_8(null);
 		}
		
 		//Si decifra la preferenza espressa col voto. Se la decifratura non riesce, viene lanciata un'eccezione PEException
		String voteString = VoteEncryption.decrypt(votePacket, Kpr_rp);
		
		if (voteString.equals(Protocol.emptyBallot) || voteString.equals(Protocol.emptyPreference)) {
			//In caso di scheda bianca, incremento il relativo contatore
			if (voteString.equals(Protocol.emptyBallot))
				nullVotesMap.get(ballotCode)[0]++;
		
			nullVotesMap.get(ballotCode)[1]++;
			return;
		}
		
		String query;
		//A seconda della preferenza espressa, incremento il relativo campo nel DB per i voti ricevuti
		if (!voteString.startsWith(Protocol.isOption)) {
			query = "UPDATE Running SET VotesReceived = VotesReceived + 1 "
					+ "WHERE ProcedureCode = ? AND BallotCode = ? AND CandidateID = ? ;";
		}
		else {
			query = "UPDATE ReferendumOption SET VotesReceived = VotesReceived + 1 "
					+ "WHERE ProcedureCode = ? AND BallotCode = ? AND Text = ? ;";
			
			voteString = voteString.substring(Protocol.isOption.length());
		}
		
		ConnectionManager cm = dbms.getConnectionManager();
		try {
			cm.startTransaction();
			cm.executeUpdate(query, procCode, ballotCode, voteString);
			cm.commit();
		}
		catch (PEException e) {
			cm.rollback();
			throw DBException.DB_0(e);
		}
		finally {
			cm.close();
		}
		
		System.out.println(new java.util.Date() + " - Nuovo pacchetto verificato e correttamente conteggiato");
	}
	
	/**
	 * Inizializza la mappa dei voti presenti nel DB. Per ogni scheda, inserisce nella mappa una lista vuota.
	 * @param procCode Codice della procedura selezionata
 	 * @return Mappa dei voti presenti nel DB, vuota
	 * @throws PEException Se l'applicazione non riesce a connettersi al DB, se riscontra errori nell'esecuzione delle query SQL, 
	 * se riscontra errori nel recupero dei dati desiderati dal DB
	 */
	private Map<Integer, ArrayList<VotePacket>> getEmptyVotesMap(int procCode) throws PEException {
		Map<Integer, ArrayList<VotePacket>> emptyVotesMap = new HashMap<>();
		String query = "SELECT * FROM Ballot WHERE ProcedureCode = ?;";
		
		try (ConnectionManager cm = dbms.getConnectionManager()) {
			ResultSet rs = cm.executeQuery(query, procCode);
			
			while (rs.next()) {
				//Per ogni scheda appartenente alla procedura selezionata viene inizializzata una lista all'interno della mappa
				emptyVotesMap.put(rs.getInt("Code"), new ArrayList<VotePacket>());
			}
			
		} catch (SQLException e) {
			throw DBException.DB_0(e);
		}
		
		return emptyVotesMap;
	}
}
