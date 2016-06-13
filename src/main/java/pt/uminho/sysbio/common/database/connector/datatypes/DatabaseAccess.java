package pt.uminho.sysbio.common.database.connector.datatypes;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public interface DatabaseAccess {

	
	public Connection openConnection() throws SQLException;

	/**
	 * close MySQL connection
	 * 
	 * @param conn
	 * @throws SQLException 
	 */
	public void closeConnection(Connection conn) throws SQLException;


	/**
	 * @return

	public boolean openConnection(){	
		try 
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (Exception e)
		{e.printStackTrace(); return false;}

		try
		{
			this.connection = 
				DriverManager.getConnection("jdbc:mysql://"+this.db_host+":"+this.db_port+"/"+this.db_name+"?"+"user="+this.db_usr+"&password="+this.db_pwd);
		} catch (SQLException ex) {
			// handle any errors
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			return false;
		}
		return true;
	}
	 */

	/**
	 * @return
	 */
	public boolean closeConnection();

	/**
	 * @param pstmt
	 * @return
	 * @throws SQLException
	 */
	public PreparedStatement prepareStatement(String pstmt) throws SQLException;

//	/**
//	 * @return
//	 * @throws SQLException
//	 */
//	public Statement createStatement() throws SQLException {
//		
//		Statement statement = null;
//		try {
//			
//			if(this.connection==null || this.connection.isClosed()) {
//				
//				this.openConnection();
//			}
//				statement = this.connection.createStatement();
//				statement.isClosed();
//				statement.isPoolable();
//				statement.execute("SHOW TABLES;");
//		}
//		catch (CommunicationsException e) {
//		
//			System.err.println("CommunicationsException\t"+e.getMessage());
//			this.openConnection();
//			statement = this.connection.createStatement();
//		}
//		
//		return statement;
//	}

	/**
	 * @param statement
	 * @throws SQLException
	 */
	public void closeStatement(Statement statement) throws SQLException;

	/**
	 * @param query
	 * @return
	 * @throws SQLException 
	 */
	public String[][] select(String query) throws SQLException;

	/**
	 * @param query
	 * @return
	 */
	public ResultSet selectRS(String query);

	/**
	 * @param newtable
	 * @param columns
	 * @return
	 */
	public boolean creatTable(String newtable, String columns);

	/**
	 * @param statement
	 * @return
	 */
	public boolean delete(String statement);

	/**
	 * @param table
	 * @return
	 */
	public boolean dropTable(String table);

	/**
	 * @param table
	 * @param values
	 * @return
	 */
	public boolean insert(String table, String values);

	/**
	 * @param table
	 * @param columns
	 * @param values
	 * @return
	 */
	public boolean insertInto(String table, String columns, String values);

	/**
	 * @param tblName
	 * @return
	 */
	public String describeTable(String tblName);

	/**
	 * @param tblName
	 * @return
	 */
	public String describeTablePK(String tblName);

	/**
	 * @param tblName
	 * @return
	 */
	public String fieldsTable(String tblName);

	/**
	 * @param tblName
	 * @return
	 */
	public boolean existTable(String tblName);

	/**
	 * @param table
	 * @param values
	 * @return
	 */
	public boolean update(String table, String values);

	/**
	 * @return
	 * @throws SQLException 
	 */
	public String[] showTables() throws SQLException;

	/**
	 * @param table
	 * @return
	 * @throws SQLException 
	 */
	public String[] getMeta(String table, pt.uminho.sysbio.common.database.connector.datatypes.Connection connection) throws SQLException;


	public String get_database_host();

	public String get_database_port();

	public String get_database_name();

	public String get_database_password();

	public String get_database_user();
	
	public String get_database_type();

	/**
	 * @param database_host the database_host to set
	 */
	public void setDatabase_host(String database_host);

	/**
	 * @param database_port the database_port to set
	 */
	public void setDatabase_port(String database_port);

	/**
	 * @param database_name the database_name to set
	 */
	public void setDatabase_name(String database_name);

	/**
	 * @param database_user the database_user to set
	 */
	public void setDatabase_user(String database_user);

	/**
	 * @param database_password the database_password to set
	 */
	public void setDatabase_password(String database_password);

	void readExternal(ObjectInput arg0) throws IOException, ClassNotFoundException;

	void writeExternal(ObjectOutput arg0) throws IOException;


}