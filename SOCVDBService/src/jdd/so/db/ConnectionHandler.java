package jdd.so.db;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

/**
 * Quick and dirty, but structure to handle pool if we need
 * @author Petter Friberg
 *
 */
public class ConnectionHandler {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(ConnectionHandler.class);

	private String url;
	private String user;
	private String password;

	public ConnectionHandler(String driver, String url, String user, String password){
		this.url = url;
		this.user = user;
		this.password = password;
		registrerDriver(driver);
	}
	
	private void registrerDriver(String driverClassName){
		try {
			Driver driver = (Driver) Class.forName(driverClassName).newInstance();
			DriverManager.registerDriver(driver);
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException e) {
			logger.error("registrerDriver(String)", e);
		}
	}
	
	public Connection getConnection() throws SQLException{
		return DriverManager.getConnection(url, user, password);
	}
	

	
	public void closeConnection(Connection connection){
		if (connection!=null){
			try {
				connection.close();
			} catch (SQLException e) {
				// well let it be then
			}
		}
	}
	
	public static void cleanUpQuery(Statement std, ResultSet rs) {
		if (std!=null){
			try {
				std.close();
			} catch (SQLException e) {
				//Nothing
			}
		}
		if (rs!=null){
			try {
				rs.close();
			} catch (SQLException e) {
				//Nothing
			}
		}
	}
	
}
