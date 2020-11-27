package db;

import exceptions.PEException;
import utils.CfgManager;
import utils.Constants;

public class DBMS {
	private String host, port, schema, user, psw;
	
	public DBMS (String host, String port, String schema, String terminal) throws PEException {
		this.host = host;
		this.port = port;
		this.schema = schema;
		this.user = CfgManager.getPassword("dbu");
		this.psw = CfgManager.getPassword("dbp");
	}
	
	public ConnectionManager getConnectionManager() throws PEException {
		String connectionString = "jdbc:mysql://" + host + ":" + port + "/" + schema + 
				"?user=" + user + "&password=" + psw + 
				"&serverTimezone=CET&useSSL="+Constants.dbSSL;
		
		if(Constants.dbSSL)
			connectionString += "&requireSSL=true&verifyServerCertificate=true";
		
		ConnectionManager cm = new ConnectionManager(connectionString);
		cm.connect();
		return cm;
	}
}