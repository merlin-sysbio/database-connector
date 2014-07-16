/**
 * 
 */
package pt.uminho.sysbio.common.database.connector.datatypes;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException;

/**
 * @author ODias
 *
 */
public class Connection implements Externalizable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1308617527146150210L;
	private java.sql.Connection connection;
	private String database_host, database_port, database_name, database_user, database_password;
	private MySQLMultiThread mysqlMutithread;

	
	/**
	 * @throws SQLException
	 */
	public Connection() throws SQLException {
		
	}
	
	/**
	 * @param host
	 * @param port
	 * @param databaseName
	 * @param user
	 * @param password
	 * @throws SQLException 
	 */
	public Connection(String host, String port, String databaseName,String user, String password) throws SQLException {
		
		this.database_host=host;
		this.database_port=port;
		this.database_name=databaseName;
		this.database_user=user;
		this.database_password=password;
		
		this.mysqlMutithread = new MySQLMultiThread(user, password, host, port, databaseName);
		this.connection = this.mysqlMutithread.openConnection();
	}
	
	/**
	 * @param mysqlMutithread
	 * @throws SQLException 
	 */
	public Connection(MySQLMultiThread mysqlMutithread) throws SQLException {
	
		this.database_host=mysqlMutithread.get_database_host();
		this.database_port=mysqlMutithread.get_database_port();
		this.database_name=mysqlMutithread.get_database_name();
		this.database_user=mysqlMutithread.get_database_user();
		this.database_password=mysqlMutithread.get_database_password();
		
		this.mysqlMutithread = mysqlMutithread;
		this.connection = this.mysqlMutithread .openConnection();
	}
	
	/**
	 * @return
	 * @throws SQLException
	 */
	public Statement createStatement() throws SQLException {
		
		Statement statement = null;
		
		try {
			
			if(this.connection==null || this.connection.isClosed()) {
				
				this.connection = this.mysqlMutithread.openConnection();
			}
				statement = this.connection.createStatement();
				statement.isClosed();
				statement.isPoolable();
				statement.executeQuery("SHOW TABLES;");
		}
		catch (CommunicationsException e) {
		
			System.err.println("CommunicationsException\t"+e.getMessage());
			this.connection = this.mysqlMutithread.openConnection();
			statement = this.connection.createStatement();
		}
		
		return statement;
		
	}

	/**
	 * @return
	 * @throws SQLException 
	 */
	public DatabaseMetaData getMetaData() throws SQLException {
		if(this.connection==null) {
			this.connection = this.mysqlMutithread.openConnection();
		}
		return this.connection.getMetaData();
	}
	
	
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF(this.database_host);
		out.writeUTF(this.database_name);
		out.writeUTF(this.database_password);
		out.writeUTF(this.database_port);
		out.writeUTF(this.database_user);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		this.database_host=in.readUTF();	
		this.database_name=in.readUTF();	
		this.database_password=in.readUTF();	
		this.database_port=in.readUTF();	
		this.database_user=in.readUTF();
		
		this.mysqlMutithread = new MySQLMultiThread(this.database_user, this.database_password, this.database_host, this.database_port, this.database_name);
	}

	/**
	 * @param input
	 * @return
	 */
	public static String mysqlStrConverter(String input){
		return input.replace("\\'","'").replace("-","\\-").replace("'","\\'").replace("[","\\[").replace("]","\\]");
	}
	
	/**
	 * @return
	 * @throws SQLException 
	 */
	public void closeConnection() throws SQLException{
		
		this.connection.close();	
	}
}
