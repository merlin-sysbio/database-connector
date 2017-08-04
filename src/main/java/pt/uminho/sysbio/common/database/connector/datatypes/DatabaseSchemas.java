package pt.uminho.sysbio.common.database.connector.datatypes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import pt.uminho.ceb.biosystems.mew.utilities.io.FileUtils;
import pt.uminho.sysbio.common.database.connector.datatypes.Enumerators.DatabaseType;

public class DatabaseSchemas {

	private String username;
	private String password;
	private String host;
	private String port;
	private DatabaseType dbType;


	/**
	 * Set a new DatabaseSchemas Instance
	 * 
	 * @param username
	 * @param password
	 * @param host
	 * @param port
	 * @param dbType
	 */
	public DatabaseSchemas(String username, String password, String host, String port, DatabaseType dbType){

		this.username = username;
		this.password = password;
		this.host = host;
		this.port = port;
		this.dbType = dbType;
	}

	/**
	 * @param DatabaseSchemas
	 */
	public DatabaseSchemas(DatabaseAccess databaseBase) {
		
		this.username = databaseBase.get_database_user();
		this.password = databaseBase.get_database_password();
		this.host = databaseBase.get_database_host();
		this.port = databaseBase.get_database_port();
	}

	/**
	 * Creates a connection to a given schema, on the database, once provided with the following parameters.
	 * 
	 * @param schema
	 * @return
	 * @throws SQLException 
	 */
	private Connection createConnection(String schema) throws SQLException{
		
		String driver_class_name;
		String url_db_connection;

		if (this.dbType.equals(DatabaseType.MYSQL)) {
			
			driver_class_name = "com.mysql.jdbc.Driver";
			url_db_connection = "jdbc:mysql://"+this.host+":"+this.port+"/"+schema;
		}
		else {
			
			String prefix = "jdbc:h2:";
			String path = new File(FileUtils.getCurrentDirectory()).getParentFile().getParent();
			driver_class_name = "org.h2.Driver";
			url_db_connection = prefix+path+"/h2Database/"+schema+";MODE=MySQL;DATABASE_TO_UPPER=FALSE;AUTO_SERVER=TRUE";
			
		}
		
		Connection connection = null;

		try {

			Class.forName(driver_class_name).newInstance();
			connection = (Connection) DriverManager.getConnection(url_db_connection, this.username, this.password);
			
//			if (this.dbType.equals(DatabaseType.H2)) { Server server = Server.createTcpServer("-tcpAllowOthers").start();}
		} 
		catch (SQLException ex) {

			ex.printStackTrace();
		}
		catch (InstantiationException e) {

			e.printStackTrace();
		} 
		catch (IllegalAccessException e) {

			e.printStackTrace();
		}
		catch (ClassNotFoundException e) {

			e.printStackTrace();
		}
		return connection;

	}

	/**
	 Creates a connection to a default schema, on the MySQL database, once provided with the following parameters.
	 * 
	 * @return
	 * @throws SQLException 
	 */
	private Connection createConnection(){
		
		String driver_class_name;
		String url_db_connection;
		
		if (this.dbType.equals(DatabaseType.MYSQL)) {
			driver_class_name = "com.mysql.jdbc.Driver";
			url_db_connection = "jdbc:mysql://"+this.host+":"+this.port;
		}else{
			String path = new File(FileUtils.getCurrentDirectory()).getParentFile().getParent();
			driver_class_name = "org.h2.Driver";
			url_db_connection = "jdbc:h2:"+path+"/h2Database;MODE=MySQL;DATABASE_TO_UPPER=FALSE;AUTO_SERVER=TRUE";
		}
		Connection connection = null;
		try  {
			Class.forName(driver_class_name).newInstance();
			connection = (Connection) DriverManager.getConnection(url_db_connection, this.username, this.password);
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return connection;
	}

	/**
	 * Close the connection to a given schema, on the database.
	 * 
	 * @param connection
	 */
	private void closeConnection(Connection connection){

		try {

			connection.close();
		} 
		catch (SQLException ex) {

			ex.printStackTrace();
		}
	}

	/**
	 * Creates a Schema in the database.
	 * 
	 * @param connection'0
	 * @param schema
	 */
	private Connection createSchema(Connection connection, String schema){

		Statement statement = null;
		try {
			statement = connection.createStatement();			
			String aux ="";
			//if(mysql)
			//aux=" CHARACTER SET utf8 COLLATE utf8_bin;"
			statement.execute( "CREATE SCHEMA IF NOT EXISTS " + schema + aux);
			statement.close();
			
			return this.createConnection(schema);
		}
		catch (SQLException ex) {

			ex.printStackTrace();
		}
		return null;
	}

	/**
	 * Checks whether a table exists or not in a given database.
	 * 
	 * @param schema
	 * @param table
	 * @return
	 * @throws SQLException
	 */
	public boolean checkTable(String schema, String table) throws SQLException {
		
		Connection connection = this.createConnection(schema);
		Statement statement = null;
		ResultSet rs = null;
		boolean result = false;

		try {
			
			statement = connection.createStatement();

			statement.execute( "show tables");
			rs=statement.getResultSet();

			while(rs.next())
				if(rs.getString(1).equalsIgnoreCase(table))
					result =true;
			
			this.closeConnection(connection);
		}
		catch (SQLException ex) 
		{
			ex.printStackTrace();
			//			System.out.println("SQLException: " + ex.getMessage());
			//			System.out.println("SQLState: " + ex.getSQLState());
			//			System.out.println("VendorError: " + ex.getErrorCode());
		}
		return result;
	}

	/**
	 * Parser for a SQL script, which creates a set of tables in the MySQL database.
	 * 
	 * @param connection
	 * @param filePath
	 * @throws FileNotFoundException 
	 */
	private void sqlByStrBuffer(Connection connection, String filePath) throws Exception {
		
		FileReader fr = new FileReader(new File(filePath));
		BufferedReader br = new BufferedReader(fr);
		this.sqlByStrBuffer(connection, br);
		fr.close();
	}

	/**
	 * Parser for a SQL script, which creates a set of tables in the database.
	 * 
	 * @param connection
	 * @param filePath
	 * @throws FileNotFoundException 
	 */
	private void sqlByStrBuffer(Connection connection, BufferedReader br) throws Exception {

		String str = null;
				
		try {
			StringBuffer stat = new StringBuffer();

			str = br.readLine();
			Statement statement = connection.createStatement();
			
			while(str!=null) {
				
				if(str.compareTo("")!=0 ) {

					stat.append(str+"\n");
					if(str.startsWith("DELIMITER")) {

						stat=new StringBuffer();
						String delimiter = str.replace("DELIMITER", "").trim();
						str = br.readLine();

						String query="";

						while(str!=null && !str.trim().equals(delimiter)) {

							query+=" "+str;
							str = br.readLine();
						}

						try  {
							
							statement.execute(query.trim()+";");
						} 
						catch (SQLException e) {

							System.out.println(query);
							e.printStackTrace();
							br.close();
							throw new Exception();
						}
					}

					if(str.contains(";")) {

						String text = stat.toString();
						try  {
							
							if (dbType.equals(DatabaseType.H2))
								text = text.replace("\\'","''");
							else
								text = text.replace("\"", "");
							statement.execute(text);
						} 
						catch (SQLException e) {

							System.out.println(text);
							e.printStackTrace();
							br.close();
							throw new Exception();
						}
						stat=new StringBuffer();
					}
				}
				str = br.readLine();
			}
			br.close();
		} 
		catch (Exception e) {

			e.printStackTrace();
			throw e;
		}

	}

	/**
	 * Retrieves all the existing Schemas in the database.
	 * 
	 * @return
	 * @throws SQLException 
	 */
	public List<String> getSchemas() throws SQLException {

		List<String> list= new ArrayList<String>();
		List<String> schemasList = new ArrayList<String>();

		Connection connection = this.createConnection();
		ResultSet rs;
		Statement statement = null;

		try {	
				statement = (Statement) connection.createStatement();
				statement.execute( "SHOW DATABASES ");
				rs = statement.getResultSet();

				while(rs.next()) {
					
					list.add(rs.getString(1));
				}
				
				for(String s: list) {
					if(checkTable(s,"enzymes_annotation_geneHomology") || checkTable(s,"genehomology") || checkTable(s,"geneblast")) {
						
						schemasList.add(s);
					}
				}
		}
		catch (SQLException ex) {
			ex.printStackTrace();
		}
		this.closeConnection(connection);
		return schemasList;
	}

	/**
	 * @param schemaName
	 * @return
	 */
	public boolean existsSchema(String schemaName) {

		boolean ret = false;
		try {

			Connection connection = this.createConnection();
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery( "SHOW DATABASES LIKE '"+connection+"'");
			ret=rs.next();
			statement.close();
			this.closeConnection(connection);
		}
		catch (Exception e) {

			e.printStackTrace();
		}
		return ret;
	}

	/**
	 * Creates a new Schema and with a set of tables defined in a SQL script. 
	 * 
	 * @param schema
	 * @param filePath
	 * @return
	 */
	public boolean newSchemaAndScript(String schema, String filePath[]){

		try {

			Connection conn = this.createConnection();
			conn = this.createSchema(conn, schema);
			for(int i=0;i<filePath.length;i++)
				this.sqlByStrBuffer(conn, filePath[i]);
			
			this.closeConnection(conn);	

			return true;
		}
		catch (Exception e) {

			e.printStackTrace();
			return false;
		}
	}


	/**
	 * Implements a SQL script
	 * 
	 * @param schema
	 * @param filePath
	 * @return
	 */
	public boolean cleanSchema(String schema, String[] filePath){

		try {

			Connection connection = this.createConnection(schema);

			for(int i=0;i<filePath.length;i++)
				this.sqlByStrBuffer(connection, filePath[i]);

			this.closeConnection(connection);	

			return true;
		}
		catch (Exception e) {

			e.printStackTrace();
			return false;
		}
	}

	/**
	 * @return if exists connection
	 * @throws SQLException 
	 */
	public boolean isConnected() throws SQLException {

		//try {
			
			boolean isConnected = false;
			
			Connection connection = this.createConnection();
			
			if(connection != null) {
				
				isConnected=true;
				this.closeConnection(connection);
			}

			return isConnected;
//		}
//		catch (Exception e) {
//			
//			e.printStackTrace();
//			return false;
//		}
	}

	/**
	 * DROP the given database
	 * 
	 * @param database
	 * @return
	 */
	public boolean dropDatabase(String database){
		
		try {
			
			Connection connection = this.createConnection();
			Statement statement = (Statement) connection.createStatement();
			if (this.dbType.equals(DatabaseType.H2)) {
				String[] filePath = new String[6];;
				String path = FileUtils.getCurrentLibDirectory()+"/../utilities";
				
				filePath[0]=path +"/schema_model.sql";
				filePath[1]=path +"/schema_enzymes_annotation.sql";
				filePath[2]=path +"/schema_interpro.sql";
				filePath[3]=path +"/schema_transporters_annotation.sql";
				filePath[4]=path +"/schema_compartments_annotation.sql";
				filePath[5]=path +"/schema_transporters_identification.sql";
				filePath[6]=path +"/triage_database.sql";
				
				this.cleanSchema(database, filePath);
			}
			statement.execute("DROP SCHEMA "+database);
			statement.close();
			connection.close();
			
			return true;
		}
		catch (SQLException ex) {
			
			ex.printStackTrace();
			return false;
		}


	}

}


