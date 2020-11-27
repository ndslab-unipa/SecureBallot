package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import exceptions.CSTException;
import exceptions.DBException;
import exceptions.DEVException;
import exceptions.PEException;

public class ConnectionManager implements AutoCloseable {
	private String connectionString;
	
	private Connection connection = null;
	private ArrayList<PreparedStatement> statements = new ArrayList<>();
	private ArrayList<ResultSet> results = new ArrayList<>();
	
	public ConnectionManager(String connString) {
		this.connectionString = connString;
	}
	
	public void connect() throws PEException {
		if(connection != null) {
			System.err.println("Connection to the DB has already been established.");
			return;
		}
		
		try {
			connection = DriverManager.getConnection(connectionString);
			statements = new ArrayList<>();
			results = new ArrayList<>();
		} catch(SQLException e) {
			throw DBException.DB_01(e);
		}
		
	}
	
	@SuppressWarnings("exports")
	public ResultSet executeQuery(String query, Object...args) throws PEException {
		PreparedStatement stm = createStatement(query, args);
		
		try {
			ResultSet rs = stm.executeQuery();
			results.add(rs);
			return rs;
		} catch (SQLException e) {
			throw DBException.DB_0(e);
		}
	}
	
	public int executeUpdate(String update, Object...args) throws PEException {
		PreparedStatement stm = createStatement(update, args);
		
		try {
			return stm.executeUpdate();
		} catch (SQLException e) {
			throw DBException.DB_0(e);
		}
	}
	
	public void startTransaction() throws PEException {	
		if (connection == null)
			throw DEVException.DEV_09();
		
		try {
			connection.createStatement().execute("START TRANSACTION;");
		} catch (SQLException e) {
			throw DBException.DB_03(e);
		}
	}
	
	public void commit() throws PEException {
		if (connection == null)
			throw DEVException.DEV_09();
		
		try {
			connection.createStatement().execute("COMMIT;");
		} catch (SQLException e) {
			throw DBException.DB_03(e);
		}
	}
	
	public void rollback() throws PEException {
		if (connection == null)
			throw DEVException.DEV_09();
		
		try {
			connection.createStatement().execute("ROLLBACK;");
		} catch (SQLException e) {
			throw DBException.DB_03(e);
		}
	}
	
	@Override
	public void close() throws PEException {
		try {
			
			if(results != null) {
				for(ResultSet rs : results) {
					rs.close();
				}
			}
			
			if(statements != null) {
				for(PreparedStatement stm : statements) {
					stm.close();
				}
			}
			
			if (connection != null)
				connection.close();
			
			results = null;
			statements = null;
			connection = null;
			
		} catch (SQLException e) {
			throw DBException.DB_02(e);
		}
	}
	
	private PreparedStatement createStatement(String sql, Object... args) throws PEException {
		if (connection == null)
			throw DEVException.DEV_09();
		
		try {
			PreparedStatement stm = connection.prepareStatement(sql);
			
			int x = 1;
			for(Object arg : args) {
				
				Class<?> c = arg.getClass();
				
				boolean typeFound = false;
				
				if( typeFound = c == String.class ) {
					stm.setString(x, (String) arg);
				}
				
				if( !typeFound && (typeFound |= (c == Integer.class || c == int.class)) ) {
					stm.setInt(x, (Integer) arg);
				}
				
				if( !typeFound && (typeFound |= (c == byte[].class)) ) {
					stm.setBytes(x, (byte[]) arg);
				}
				
				if( !typeFound && (typeFound |= (c == java.sql.Date.class)) ) {
					stm.setDate(x, (java.sql.Date) arg);
				}

				if( !typeFound && (typeFound |= (c == LocalDate.class)) ) {
					stm.setDate(x, java.sql.Date.valueOf((LocalDate) arg));
				}

				if( !typeFound && (typeFound |= (c == LocalDateTime.class)) ) {
					stm.setTimestamp(x, java.sql.Timestamp.valueOf((LocalDateTime) arg));
				}
				
				if( !typeFound && (typeFound |= (c == java.sql.Timestamp.class)) ) {
					stm.setTimestamp(x, (java.sql.Timestamp) arg);
				}
				
				if( !typeFound ) {
					throw CSTException.CST_01(c);
				}
				
				x++;
			}
			
			statements.add(stm);
			return stm;
			
		} catch (SQLException e) {
			throw DBException.DB_03(e);
		}
	}
}
