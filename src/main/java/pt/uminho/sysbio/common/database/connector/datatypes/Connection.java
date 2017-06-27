/**
 * 
 */
package pt.uminho.sysbio.common.database.connector.datatypes;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException;

import pt.uminho.ceb.biosystems.mew.utilities.io.FileUtils;
import pt.uminho.sysbio.common.database.connector.datatypes.Enumerators.DatabaseType;

/**
 * @author ODias
 *
 */
public class Connection implements Externalizable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private java.sql.Connection connection;
	private String database_host, database_port, database_name, database_user, database_password;
	private DatabaseType database_type;
	private DatabaseAccess dbAccess;


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
	 * @param dbType
	 * @throws SQLException 
	 */
	public Connection(String host, String port, String databaseName,String user, String password, DatabaseType dbType) throws SQLException {

		this.database_host=host;
		
		this.database_port=port;
		if(port==null)
			this.database_port="";
		
		this.database_name=databaseName;
		this.database_user=user;
		this.database_password=password;
		this.database_type=dbType;

		if (this.database_type.equals(DatabaseType.MYSQL))
			this.dbAccess = new MySQLDatabaseAccess(user, password, host, port, databaseName);
		else
			this.dbAccess = new H2DatabaseAccess(user, password, databaseName, null);

		this.connection = this.dbAccess.openConnection();
	}

	/**
	 * @param dbAccess
	 * @throws SQLException 
	 */
	public Connection(DatabaseAccess dbAccess) throws SQLException {

		this.database_host=dbAccess.get_database_host();
		this.database_port=dbAccess.get_database_port();
		if(dbAccess.get_database_port()==null)
			this.database_port="";
		
		this.database_name=dbAccess.get_database_name();
		this.database_user=dbAccess.get_database_user();
		this.database_password=dbAccess.get_database_password();
		this.database_type=dbAccess.get_database_type();
		

		if (this.database_type.equals(DatabaseType.MYSQL))
			this.dbAccess = new MySQLDatabaseAccess(this.database_user, this.database_password, this.database_host, this.database_port, this.database_name);
		else
			this.dbAccess = new H2DatabaseAccess(this.database_user, this.database_password, this.database_name, null);
		
		this.connection = this.dbAccess.openConnection();
	}

	/**
	 * @return
	 * @throws SQLException
	 */
	public Statement createStatement() throws SQLException {

		Statement statement = null;

		try {
			
			if(this.connection==null || this.connection.isClosed())
				this.connection = this.dbAccess.openConnection();

			statement = this.connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
//			statement.isClosed();
//			statement.isPoolable();
			statement.executeQuery("SHOW TABLES;");
		}
		catch (CommunicationsException e) {

			System.err.println("CommunicationsException\t"+e.getMessage());
			this.connection = this.dbAccess.openConnection();
			statement = this.connection.createStatement();
		}
		catch (SQLException se){
			System.out.println(se.getMessage());
			throw se;
		}

		return statement;

	}

	/**
	 * @return
	 * @throws SQLException 
	 */
	public DatabaseMetaData getMetaData() throws SQLException {
		if(this.connection==null) {
			this.connection = this.dbAccess.openConnection();
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
		out.writeUTF(this.database_type.toString());
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		this.database_host=in.readUTF();	
		this.database_name=in.readUTF();	
		this.database_password=in.readUTF();	
		this.database_port=in.readUTF();	
		this.database_user=in.readUTF();
		String temp =in.readUTF();
		
		this.database_type = DatabaseType.H2;
		if(temp.equalsIgnoreCase(DatabaseType.MYSQL.toString()))
			this.database_type = DatabaseType.MYSQL;

		if (this.database_type.equals(DatabaseType.MYSQL)) {
			this.dbAccess = new MySQLDatabaseAccess(this.database_user, this.database_password, this.database_host, this.database_port, this.database_name);
		}else{
			this.dbAccess = new H2DatabaseAccess(this.database_user, this.database_password, this.database_name, null);
		}
	}

//	/**
//	 * @param input
//	 * @return
//	 */
//	public static String mysqlStrConverter(String input){
//		return input.replace("\\'","'").replace("-","\\-").replace("'","\\'").replace("[","\\[").replace("]","\\]");
//	}

	/**
	 * @return
	 * @throws SQLException 
	 */
	public void closeConnection() throws SQLException{

		this.connection.close();	
	}

//	public void setDatabaseType(DatabaseType dbType) {
//		this.database_type=dbType;
//	}
//
	public DatabaseType getDatabaseType() {
		return this.database_type;
	}
}
