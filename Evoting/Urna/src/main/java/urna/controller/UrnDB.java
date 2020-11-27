package urna.controller;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import db.ConnectionManager;
import db.DBMS;
import db.DB;
import exceptions.DBException;
import exceptions.FLRException;
import exceptions.PEException;
import model.ElectoralList;
import model.EmptyBallot;
import model.Person;
import model.Procedure;
import model.Session;
import model.Terminals;
import model.VotePacket;
import model.WrittenBallot;

public class UrnDB extends DB {
	public UrnDB(String host, String port, String schema, String terminal) throws PEException {
		dbms = new DBMS(host, port, schema, terminal);
	}

	public ArrayList<Session> getSessions(String username) throws PEException {
		String query = 	"SELECT P.Code, P.Name, P.Supervisor, S.Code, S.StartsAt, S.EndsAt, (S.StartsAt <= NOW() AND S.EndsAt >= NOW()) AS Validity "
					+	"FROM `Procedure` P LEFT JOIN `Session` S ON P.Code = S.ProcedureCode "
					+   "WHERE P.Starts <= NOW() AND P.Ends >= NOW() AND P.Supervisor = ? "
					+ 	"ORDER BY P.Code, S.Code ;";

		ArrayList<Session> sessions = new ArrayList<>();
		try (ConnectionManager cm = dbms.getConnectionManager()) {
			ResultSet rs = cm.executeQuery(query, username);

			while(rs.next()) {
				Procedure p = new Procedure(rs.getInt("P.Code"), rs.getString("P.Name"), rs.getString("P.Supervisor"));
				LocalDateTime start = rs.getTimestamp("S.StartsAt").toLocalDateTime(), end = rs.getTimestamp("S.EndsAt").toLocalDateTime();
				sessions.add(new Session(rs.getInt("S.Code"), p, start, end, rs.getBoolean("Validity")));
			}

		} catch (SQLException e) {
			throw DBException.DB_0(e);
		}

		return sessions;
	}

	public EmptyBallot[] getEmptyBallots(int procedureCode) throws PEException { //rivedere e semplificare?
		//Recupero schede con candidati.
		/*String candidatesQuery = 	"SELECT * " +
										"FROM Candidate1 AS C " +
										"JOIN Running AS R ON C.ID = R.CandidateID " +
										"JOIN Ballot AS B ON (R.BallotCode = B.Code AND R.ProcedureCode = B.ProcedureCode) " +
										"LEFT JOIN  " +
										"	( " +
										"    SELECT * " +
										"    FROM ElectoralList AS EL " +
										"    JOIN Member AS M ON EL.Code = M.ElectoralListCode " +
										"    WHERE M.ProcedureCode = ? " +
										"	) AS PM ON C.ID = PM.CandidateID " +
										"WHERE B.ProcedureCode = ? " +
										"ORDER BY B.Code ;";*/

		String candidatesQuery = "SELECT * " +
									"FROM Candidate AS C  " +
									"JOIN Running AS R ON C.ProcedureCode = R.ProcedureCode AND C.ID = R.CandidateID  " +
									"JOIN Ballot AS B ON R.ProcedureCode = B.ProcedureCode AND R.BallotCode = B.Code " +
									"LEFT JOIN   " +
									"(  " +
									"SELECT M.*, EL.Name  " +
									"FROM ElectoralList AS EL  " +
									"JOIN Member AS M " +
									"        ON EL.ProcedureCode = M.ProcedureCode AND EL.Code = M.ElectoralListCode " +
									") AS PM ON R.ProcedureCode = PM.ProcedureCode AND C.ID = PM.CandidateID  " +
									"WHERE B.ProcedureCode = ? " +
									"ORDER BY B.Code ;";

		//Recupero schede con opzioni.
		String optionsQuery = "SELECT B.*, RO.* " +
									"FROM ReferendumOption AS RO " +
									"JOIN Ballot AS B ON (RO.BallotCode = B.Code AND RO.ProcedureCode = B.ProcedureCode) " +
									"WHERE B.ProcedureCode = ? ;";

		ArrayList<EmptyBallot> ballots = new ArrayList<>();
		try (ConnectionManager cManager = dbms.getConnectionManager()) {
			ResultSet rs = cManager.executeQuery(candidatesQuery, procedureCode);

			HashMap<Integer, HashMap<Integer, ElectoralList>> ballotListMap = new HashMap<>();
			HashMap<Integer, EmptyBallot> ballotMap = new HashMap<>();

			while(rs.next()) {
				int ballotCode = rs.getInt("B.Code");

				if(!ballotMap.containsKey(ballotCode)) {
					ballotMap.put(ballotCode, new EmptyBallot(rs.getString("B.Name"), rs.getInt("B.Code"), rs.getString("B.Description"), rs.getInt("B.MaxPreferences")));
				}

				HashMap<Integer, ElectoralList> ELMap = ballotListMap.get(ballotCode);
				if(ELMap == null) {
					ELMap = new HashMap<>();
					ballotListMap.put(ballotCode, ELMap);
				}

				int listCode = rs.getInt("PM.ElectoralListCode");

				ElectoralList eL = ELMap.get(listCode);
				if(eL == null) {

					String listName = rs.getString("PM.Name");
					if(listName == null) {
						listName = "Candidati non appartenenti ad alcuna lista.";
					}

					eL = new ElectoralList(listName);
					ELMap.put(listCode, eL);
				}

				java.sql.Date date = rs.getDate("DateOfBirth");
				String dateOfBirth = (date == null) ? null: date.toString();
				eL.addPerson(new Person(rs.getString("C.FirstName"), rs.getString("C.LastName"), dateOfBirth, rs.getString("C.ID")));
			}

			rs = cManager.executeQuery(optionsQuery, procedureCode);

			while(rs.next()) {
				int ballotCode = rs.getInt("B.Code");

				EmptyBallot eB = ballotMap.get(ballotCode);
				if(eB == null) {
					eB = new EmptyBallot(rs.getString("B.Name"), rs.getInt("B.Code"), rs.getString("B.Description"), rs.getInt("B.MaxPreferences"));
					ballotMap.put(ballotCode, eB);
				}

				eB.addOption(rs.getString("RO.Text"));
			}

			ballotListMap.forEach((ballotCode, bLmap)-> bLmap.forEach((listCode, list)->{
				ballotMap.get(ballotCode).addList(list);
			}));

			ballotMap.forEach((k, ballot)->ballots.add(ballot));

		} catch (SQLException e) {
			throw DBException.DB_0(e);
		}

		return ballots.toArray(new EmptyBallot[0]);
	}

	public int getNumOfEligibleVoters(int procCode) {
		return getNumRows("Voter", procCode);
	}

	public int getNumOfVoted(int procCode) {
		return getNumRows("HasVoted", procCode);
	}

	private int getNumRows(String table, int procCode) {
		String query = "SELECT COUNT(*) AS Cnt FROM "+table+" WHERE ProcedureCode = ? ;";

		try (ConnectionManager cm = dbms.getConnectionManager()) {
			ResultSet rs = cm.executeQuery(query, procCode);

			if (rs.next())
				return rs.getInt("Cnt");
		} catch (SQLException | PEException e) {
			e.printStackTrace();
		}

		return -1;
	}

	public String getTerminalSessionKey(int procedureCode, int sessionCode, InetAddress ipTerminal, Terminals.Type type) throws PEException {
		/*String query = 	"SELECT SymmetricKey " +
						"FROM Terminal AS T " +
						"JOIN SessionKey as SK on T.ID = SK.TerminalID " +
						"WHERE T.IPAddress = ? " +
						"AND Type = ? " +
						"AND SK.ProcedureCode = ? " +
						"AND SK.SessionCode = ? ;";*/

		String query = "SELECT SymmetricKey " +
						"FROM Terminal AS T " +
						"JOIN SessionKey AS SK " +
						"ON T.ProcedureCode = SK.ProcedureCode " +
						"AND T.SessionCode = SK.SessionCode " +
						"AND T.ID = SK.TerminalID " +
						"WHERE T.IPAddress = ? " +
						"AND T.Type = ? " +
						"AND SK.ProcedureCode = ? " +
						"AND SK.SessionCode = ? ;";

		try (ConnectionManager cManager = dbms.getConnectionManager()) {
			ResultSet rs = cManager.executeQuery(query, ipTerminal.getHostAddress(), type.toString(), procedureCode, sessionCode);

			if(rs.next())
				return rs.getString("SymmetricKey");
			else
				throw DBException.DB_12(false);
		}
		catch (SQLException e) {
			throw DBException.DB_0(e);
		}
	}

	public String getStationIP(int procedureCode, int sessionCode, InetAddress ipTerminal, boolean isPost) throws PEException {
		/*String query = 	"SELECT T.ID, S.IPAddress as IP " +
						"FROM Terminal as S " +
						"LEFT JOIN IsStationOf as ISO on S.ID = ISO.Station " +
						"LEFT JOIN Terminal as T on ISO.Terminal = T.ID " +
						"WHERE S.Type = 'Station' " +
				        "AND T.Type = ? " +
						"AND T.IPAddress = ? ;";*/

		String query = "SELECT T.ID, S.IPAddress as IP " +
						"FROM Terminal AS T " +
						"LEFT JOIN IsStationOf AS ISO " +
						"ON T.ProcedureCode = ISO.ProcedureCode " +
						"AND T.SessionCode = ISO.SessionCode " +
						"AND T.ID = ISO.Terminal " +
						"LEFT JOIN Terminal AS S " +
						"ON ISO.ProcedureCode = S.ProcedureCode " +
						"AND ISO.SessionCode = S.SessionCode " +
						"AND ISO.Station = S.ID " +
						"WHERE S.Type = 'Station' " +
						"AND T.ProcedureCode = ? " +
						"AND T.SessionCode = ? " +
						"AND T.Type = ? " +
						"AND T.IPAddress = ? ;";

		try (ConnectionManager connManager = dbms.getConnectionManager()) {
			ResultSet rs = connManager.executeQuery(query, procedureCode, sessionCode, isPost ? "Post" : "SubStation", ipTerminal.getHostAddress());

			if(rs.next())
				return rs.getString("IP");
			else
				throw DBException.DB_05(isPost, ipTerminal);
		}
		catch (SQLException e) {
			throw DBException.DB_0(e);
		}
	}

	public void getTerminalsIPs(int procedureCode, int sessionCode, InetAddress ipStation, ArrayList<InetAddress> ipPosts, ArrayList<InetAddress> ipSubStations) throws PEException {
		/*String query = 	"SELECT T.ID, T.IPAddress AS IP " +
						"FROM Terminal AS S " +
						"LEFT JOIN IsStationOf AS ISO ON S.ID = ISO.Station " +
						"LEFT JOIN Terminal AS T ON ISO.Terminal = T.ID " +
						"WHERE S.Type = 'Station' " +
				        "AND T.Type = ? " +
						"AND S.IPAddress = ? " +
						"ORDER BY T.ID ;";*/

		String query = "SELECT T.ID, T.IPAddress AS IP " +
						"FROM Terminal AS S " +
						"LEFT JOIN IsStationOf AS ISO " +
						"ON S.ProcedureCode = ISO.ProcedureCode " +
						"AND S.SessionCode = ISO.SessionCode " +
						"AND S.ID = ISO.Station " +
						"LEFT JOIN Terminal AS T " +
						"ON ISO.ProcedureCode = T.ProcedureCode " +
						"AND ISO.SessionCode = T.SessionCode " +
						"AND ISO.Terminal = T.ID " +
						"WHERE S.ProcedureCode = ? " +
						"AND S.SessionCode = ? " +
						"AND S.Type = 'Station' " +
						"AND S.IPAddress = ? " +
						"AND T.Type = ? " +
						"ORDER BY T.ID ;";

		String ipString = null;
		try (ConnectionManager connManager = dbms.getConnectionManager()) {
			ResultSet rs = connManager.executeQuery(query, procedureCode, sessionCode, ipStation.getHostAddress(), "Post");
			while(rs.next()) {
				//Salviamo quì la stringa ricevuta dal DB, così da averla disponibile per il catch se non dovesse essere un ip valido
				ipString = rs.getString("IP");
				ipPosts.add(InetAddress.getByName(ipString));
			}

			rs = connManager.executeQuery(query, procedureCode, sessionCode, ipStation.getHostAddress(), "SubStation");
			while(rs.next()) {
				//Salviamo quì la stringa ricevuta dal DB, così da averla disponibile per il catch se non dovesse essere un ip valido
				ipString = rs.getString("IP");
				ipSubStations.add(InetAddress.getByName(ipString));
			}
		}
		catch (SQLException e) {
			throw DBException.DB_0(e);
		}
		catch (UnknownHostException e) {
			throw exceptions.DBException.DB_04(ipString);
		}
	}

	public ArrayList<Person> searchPerson(int procedureCode, String similarFirstName, String similarLastName, int maxResults) throws PEException{
		String voterQuery = 	"SELECT V.*, " +
								"	EXISTS (" +
								"	SELECT * " +
								"	FROM HasVoted AS HV " +
								"	WHERE ProcedureCode = V.ProcedureCode " +
								"	AND VoterID = V.ID) AS AlreadyVoted " +
								"FROM Voter AS V " +
								"WHERE FirstName like ? AND LastName like ? " +
								"AND ProcedureCode = ? " +
								"LIMIT ? ;";

		String ballotsListQuery = 	"SELECT * " +
									"FROM VoterBallotsList AS VBL " +
									"WHERE VBL.VoterID = ? " +
									"AND VBL.ProcedureCode = ? ;" ;

		ArrayList<Person> voters = new ArrayList<>();
		try (ConnectionManager cManager = dbms.getConnectionManager()) {
			ResultSet rs = cManager.executeQuery(voterQuery, "%" + similarFirstName + "%", "%" + similarLastName + "%", procedureCode, maxResults + 1);

			String voterID;
			ArrayList<Integer> ballotCodesList;
			while(rs.next()) {
				voterID = rs.getString("V.ID");
				ballotCodesList = new ArrayList<>();

				ResultSet rs2 = cManager.executeQuery(ballotsListQuery, voterID, procedureCode);
				while(rs2.next())
					ballotCodesList.add(rs2.getInt("VBL.BallotCode"));

				int[] ballotCodes = new int[ballotCodesList.size()];
				for (int i = 0; i < ballotCodesList.size(); i++)
					ballotCodes[i] = ballotCodesList.get(i);

				voters.add(new Person(rs.getString("V.FirstName"), rs.getString("V.LastName"), voterID, ballotCodes, !rs.getBoolean("AlreadyVoted"), rs.getString("V.DateOfBirth")));
			}
		}
		catch (SQLException e) {
			throw DBException.DB_0(e);
		}

		return voters;
	}

	/**
	 * Funzione usata per verificare se esiste già un votante con l'id specificato, legato alla procedura attuale,
	 * in modo da poter verificare se è possibile l' aggiunzione di un nuovo votante con quell'ID.
	 * @param voterID	L'ID per cui effettuare la verifica.
	 * @return	Il votante con l'ID passato, se esiste o null altrimenti.
	 */
	public Person getVoter(int procedureCode, String voterID) throws PEException {
		String query = "SELECT * FROM Voter WHERE ProcedureCode = ? AND ID = ? ; ";

		try(ConnectionManager cManager = dbms.getConnectionManager()){
			ResultSet rs = cManager.executeQuery(query, procedureCode, voterID);

			if(rs.next())
				return new Person(rs.getString("FirstName"), rs.getString("LastName"), voterID, null, false, rs.getString("DateOfBirth"));
		}
		catch (SQLException e){
			throw DBException.DB_0(e);
		}

		return null;
	}

	public void registerNewVoter(int procCode, InetAddress ipStation, String id, String ln, String fn, String birthDate, int[] ballots) throws PEException {
		String voterUpdate = "INSERT INTO Voter (ProcedureCode, ID, FirstName, LastName, DateOfBirth, Added) VALUES (?,?,?,?,?,?);";
		String added = "Aggiunto o modificato durante la procedura " + procCode + " dal seggio " + ipStation;

		ConnectionManager cManager = dbms.getConnectionManager();
		try {
			cManager.startTransaction();

			cManager.executeUpdate(voterUpdate, procCode, id, fn, ln, LocalDate.parse(birthDate, DateTimeFormatter.ofPattern("d/M/yyyy")), added);
			enableUser(cManager, procCode, ipStation, id, ballots, true);
			
			cManager.commit();
		} catch (PEException e) {
			cManager.rollback();
			throw e;
		} catch (SQLException e) {
			cManager.rollback();
			throw DBException.DB_0(e);
		} finally {
			cManager.close();
		}
	}

	public void updateExistingVoter(int procCode, InetAddress ip, String id, int[] ballots) throws PEException {
		ConnectionManager cManager = dbms.getConnectionManager();
		try{
			cManager.startTransaction();
			enableUser(cManager, procCode, ip, id, ballots, false);
			cManager.commit();
		} catch (PEException e){
			cManager.rollback();
			throw e;
		} catch (SQLException e) {
			cManager.rollback();
			throw DBException.DB_0(e);
		} finally {
			cManager.close();
		}
	}

	private void enableUser(ConnectionManager cm, int procCode, InetAddress stationIp, String voterID, int[] ballots, boolean newVoter) throws PEException, SQLException {
		String hasVotedQuery = 	"SELECT * " +
								"FROM HasVoted " +
								"WHERE ProcedureCode = ? " +
								"AND VoterID = ? ;";
		
		ResultSet rs = cm.executeQuery(hasVotedQuery, procCode, voterID);

		if(rs.next())
			throw DBException.DB_15(voterID, true);
		
		if(!newVoter) {
			String removeBallotsUpdate =	"DELETE " +
											"FROM VoterBallotsList " +
											"WHERE ProcedureCode = ? " +
											"AND VoterID = ? ;";
			
			cm.executeUpdate(removeBallotsUpdate, procCode, voterID);
		}
	
		String addBallotsUpdate = "INSERT INTO VoterBallotsList(ProcedureCode, VoterID, BallotCode) VALUES (?,?,?);";

		for (int ballotCode : ballots)
			cm.executeUpdate(addBallotsUpdate, procCode, voterID, ballotCode);
	}

	public void verifyVoteData(int procedureCode, int sessionCode, String voterID, WrittenBallot[] ballots, String ipStation, String ipPost) throws PEException {
		if(verifySessionValidity(procedureCode, sessionCode)) {

			HashSet<Integer> ballotCodes = new HashSet<>();
			for(WrittenBallot ballot : ballots) {
				ballotCodes.add(ballot.getCode());
			}

			verifyVoter(procedureCode, voterID, ballotCodes);
		}

		verifyStationAndPost(procedureCode, sessionCode, ipStation, ipPost);
	}

	/**
	 * Verifica se la sessione è ancora valida in risposta alla ricezione di un voto,
	 * o se il voto è arrivato al di fuori della sessione.
	 *
	 * @return La stringa contenuta in Protocol.success in caso di successo, l'errore riscontrato altrimenti.
	 */
	private boolean verifySessionValidity(int procedureCode, int sessionCode) throws PEException {
		String query = "SELECT IF(NOW() >= S.StartsAt AND NOW() <= S.EndsAt, 1, 0) AS Valid " +
						"FROM `Procedure` as P " +
						"JOIN Session as S on P.Code = S.ProcedureCode " +
						"WHERE P.Code = ? AND S.Code = ? ;";

		try (ConnectionManager cManager = dbms.getConnectionManager()) {
			ResultSet rs = cManager.executeQuery(query, procedureCode, sessionCode);

			if(rs.next())
				return rs.getBoolean("Valid");
			else
				throw DBException.DB_06(sessionCode, procedureCode);
		}
		catch (SQLException e) {
			throw DBException.DB_0(e);
		}
	}

	/**
	 * Verifica l'esistenza del votante nel DB, la sua appartenenza all'elettorato attivo della procedura attuale,
	 * controlla che i codici delle schede (le cui preferenze sono cifrate) siano quelle che poteva votare,
	 * e controlla che non abbia già votato.
	 */
	private void verifyVoter(int procedureCode, String voterID, HashSet<Integer> ballotCodes) throws PEException {
		String voterQuery = "SELECT V.*, (HV.ProcedureCode IS NOT NULL) AS AlreadyVoted " +
							"FROM Voter AS V " +
							"LEFT JOIN HasVoted AS HV ON V.ProcedureCode = HV.ProcedureCode AND V.ID = HV.VoterID " +
							"WHERE V.ProcedureCode = ? AND V.ID =  ? ;";

		String ballotsListQuery = 	"SELECT * " +
									"FROM Voter AS V " +
									"JOIN VoterBallotsList AS VBL ON V.ProcedureCode = VBL.ProcedureCode AND V.ID = VBL.VoterID " +
									"WHERE V.ProcedureCode = ? AND V.ID = ? ;";

		try (ConnectionManager cManager = dbms.getConnectionManager()) {
			ResultSet rs = cManager.executeQuery(voterQuery, procedureCode, voterID);

			if(!rs.next())
				throw DBException.DB_07(voterID);

			String firstName = rs.getString("V.FirstName");
			String lastName = rs.getString("V.LastName");

			Person voter = new Person(firstName, lastName, voterID, null, false);
			if(rs.getBoolean("AlreadyVoted"))
				throw FLRException.FLR_11(voter, 0);

			rs = cManager.executeQuery(ballotsListQuery, procedureCode, voterID);

			int numBallots = 0;
			HashSet<Integer> foundCodes = new HashSet<>();

			while(rs.next()) {
				int code = rs.getInt("VBL.BallotCode");

				if(ballotCodes.contains(code) && !foundCodes.contains(code)) {
					numBallots++;
					foundCodes.add(code);
				}
				else
					throw FLRException.FLR_11(voter, 2);
			}

			if(numBallots != ballotCodes.size())
				throw FLRException.FLR_11(voter, 2);

		} catch (SQLException e) {
			throw DBException.DB_0(e);
		}
	}

	/**
	 * Verifica che gli ip appartengano a 2 terminali presenti nel DB, uno ad un seggio e l'altro ad una postazione.
	 * Controlla inoltre che nel DB quella specifica postazione sia relativa a quello specifico seggio.
	 */
	private void verifyStationAndPost(int procedureCode, int sessionCode, String ipStation, String ipPost) throws PEException {
		String terminalExistenceQuery =	"SELECT * " +
										"FROM Terminal " +
										"WHERE IPAddress = ? " +
										"AND Type = ? " +
										"AND ProcedureCode = ? " +
										"AND SessionCode = ? ;";

		String terminalPairingQuery = 	"SELECT T1.*, T2.* " +
										"FROM Terminal AS T1 " +
										"JOIN IsStationOf AS I " +
										"ON T1.ProcedureCode = I.ProcedureCode " +
										"AND T1.SessionCode = I.SessionCode " +
										"AND T1.ID = I.Station " +

										"JOIN Terminal AS T2 " +
										"ON I.ProcedureCode = T2.ProcedureCode " +
										"AND I.SessionCode = T2.SessionCode " +
										"AND I.Terminal = T2.ID " +

										"WHERE T1.Type = 'Station' " +
										"AND T2.Type = 'Post' " +
										"AND T1.IPAddress = ? " +
										"AND T2.IPAddress = ? " +
										"AND I.ProcedureCode = ? " +
										"AND I.SessionCode = ? ; ";

		try (ConnectionManager cManager = dbms.getConnectionManager()) {
			ResultSet rs = cManager.executeQuery(terminalExistenceQuery, ipStation, "Station", procedureCode, sessionCode);

			if(!rs.next())
				throw DBException.DB_08(0, ipStation);

			rs = cManager.executeQuery(terminalExistenceQuery, ipPost, "Post", procedureCode, sessionCode);
			if(!rs.next())
				throw DBException.DB_08(2, ipPost);

			rs = cManager.executeQuery(terminalPairingQuery, ipStation, ipPost, procedureCode, sessionCode);
			if(!rs.next())
				throw DBException.DB_09(ipPost, ipStation);
		}
		catch (SQLException e) {
			throw DBException.DB_0(e);
		}
	}

	public void storeVotes(int procedureCode, int sessionCode, Person voter, WrittenBallot[] ballots, InetAddress ipStation, InetAddress ipPost) throws PEException {
		String voterID = voter.getID(), docID = voter.getDocumentID();
		Person.DocumentType docType = voter.getDocumentType();

		ArrayList<String> missing = new ArrayList<>();
		if(voterID == null || voterID.isEmpty())
			missing.add("ID Votante");

		if(docType == null)
			missing.add("Tipo Documento");
		else
			if(!docType.equals(Person.DocumentType.CONOSCENZA_PERSONALE) && docID == null)
				missing.add("ID Documento");

		if(!missing.isEmpty())
			throw FLRException.FLR_10("seggio " + ipStation, missing, new ArrayList<>());

		if(docType.equals(Person.DocumentType.CONOSCENZA_PERSONALE))
			docID = "";

		/*String hasVotedUpdate = 	"INSERT INTO HasVoted(ProcedureCode, VoterID, StationID, DocumentType, DocumentID) VALUES(?, ?, " +
									"	(SELECT T.ID " +
									"    FROM Terminal AS T " +
									"    WHERE T.IPAddress = ? " +
									"    AND T.Type = 'Station'), ?, ?) ;";*/

		String hasVotedUpdate = "INSERT INTO HasVoted(ProcedureCode, VoterID, StationID, DocumentType, DocumentID) " +
								"VALUES(?, ?, " +
								"(SELECT T.ID " +
								"FROM Terminal AS T " +
								"WHERE T.ProcedureCode = ? " +
								"AND T.SessionCode = ? " +
								"AND T.IPAddress = ? " +
								"AND T.Type = 'Station'), ?, ?) ;";

		String nonceDiscriminatorQuery = 	"SELECT MAX(NonceDiscrim) AS MaxNonceDiscrim " +
												"FROM Vote " +
												"WHERE ProcedureCode = ? AND BallotCode = ? AND EncryptedNonce = ? ;";

		String voteUpdate = "INSERT INTO Vote(ProcedureCode, BallotCode, EncryptedNonce, NonceDiscrim, EncryptedString, EncryptedKey, EncryptedIV, Signature) "
								+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ;";

		ConnectionManager cManager = dbms.getConnectionManager();
		try {
			cManager.startTransaction();
			cManager.executeUpdate(hasVotedUpdate, procedureCode, voterID, procedureCode, sessionCode, ipStation.getHostAddress(), docType.toString(), docID);

			for(WrittenBallot ballot : ballots) {
				int ballotCode = ballot.getCode();
				ArrayList<VotePacket> packets = ballot.getEncryptedVotePackets();

				for(VotePacket packet : packets) {

					ResultSet rs = cManager.executeQuery(nonceDiscriminatorQuery, procedureCode, ballotCode, packet.getSolvedNonce());

					int nonceDiscriminator = 0;
					if(rs.next()) {
						nonceDiscriminator = rs.getInt("MaxNonceDiscrim");
					}

					cManager.executeUpdate(voteUpdate, procedureCode, ballotCode,
							packet.getSolvedNonce(), nonceDiscriminator, packet.getEncryptedVote(),
							packet.getEncryptedKi(), packet.getEncryptedIV(), packet.getSignature());
				}
			}

			cManager.commit();
		}
		catch(SQLException | PEException e) {
			cManager.rollback();
			throw DBException.DB_0(e);
		}
		finally {
			cManager.close();
		}
	}
}
