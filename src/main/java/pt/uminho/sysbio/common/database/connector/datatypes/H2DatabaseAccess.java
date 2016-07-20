package pt.uminho.sysbio.common.database.connector.datatypes;

	import java.io.File;
import java.io.IOException;
	import java.io.ObjectInput;
	import java.io.ObjectOutput;
	import java.sql.Connection;
	import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
	import java.sql.ResultSet;
	import java.sql.ResultSetMetaData;
	import java.sql.SQLException;
	import java.sql.Statement;
	import java.util.LinkedList;
	
	import org.h2.jdbcx.JdbcConnectionPool;
	
	import org.h2.jdbcx.JdbcDataSource;

import pt.uminho.ceb.biosystems.mew.utilities.io.FileUtils;
import pt.uminho.sysbio.common.database.connector.datatypes.Enumerators.DatabaseType;

//	import javax.sql.ConnectionPoolDataSource;
//	import javax.naming.Context;
//	import javax.naming.InitialContext;
//	import javax.naming.NamingException;

	public class H2DatabaseAccess implements DatabaseAccess {

		/**
		 * 
		 */
		private String database_host, database_port, database_name, database_user, database_password;
		private Connection connection;
		private JdbcDataSource dataSource;
//		private ConnectionPoolDataSource dataSource;

		//	public DatabaseAccess(String host, String port,String db, String usr, String pwd){	
		//		
		//	}

//		/**
//		 * 
//		 */
//		public MySQLDatabaseAccess() {
//			
//			this.dataSource = new MysqlConnectionPoolDataSource();
//		}

		/**
		 * @param user
		 * @param password
		 * @param server
		 * @param port
		 * @param database
		 * @throws NamingException 
		 */
		public H2DatabaseAccess(String user, String password, String url){
			
			this.database_host=url;
			this.database_user=user;
			this.database_password=password;

//			this.dataSource = new ConnectionPoolDataSource();
//			this.dataSource = new JdbcDataSource();
//			this.dataSource.setUser(user);
//			this.dataSource.setPassword(password);
//			this.dataSource.setURL(url);
//			Context ctx = new InitialContext();
//			ctx.bind("jdbc/dataSourceName",dataSource);
			
		}

		/**
		 * open MySQL connection
		 * 
		 * @return
		 * @throws SQLException 
		 */
		public Connection openConnection() throws SQLException {
			
//			try {
//				Class.forName("org.h2.Driver");
//			} catch (ClassNotFoundException e) {
//				e.printStackTrace();
//			}

//			JdbcConnectionPool connect = JdbcConnectionPool.create(this.dataSource);
			String path = new File(FileUtils.getCurrentDirectory()).getParentFile().getParent();
			JdbcConnectionPool connect = JdbcConnectionPool.create("jdbc:h2:"+path+"/h2Database/"+this.database_host+";MODE=MySQL;DATABASE_TO_UPPER=FALSE;",this.database_user,this.database_password);
			this.connection=connect.getConnection();
			return this.connection;
		}

		/**
		 * close MySQL connection
		 * 
		 * @param conn
		 * @throws SQLException 
		 */
		public void closeConnection(Connection conn) throws SQLException {
			
			conn.close();
		}


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
		public boolean closeConnection() {
			
			try {
				
				this.connection.close();
			}
			catch (SQLException ex) {
				// handle any errors
				ex.printStackTrace();
				return false;
			}
			return true;
		}

		/**
		 * @param pstmt
		 * @return
		 * @throws SQLException
		 */
		public PreparedStatement prepareStatement(String pstmt) throws SQLException {
			
			PreparedStatement preparedStatement = connection.prepareStatement(pstmt);

			return preparedStatement;
		}

//		/**
//		 * @return
//		 * @throws SQLException
//		 */
//		public Statement createStatement() throws SQLException {
//			
//			Statement statement = null;
//			try {
//				
//				if(this.connection==null || this.connection.isClosed()) {
//					
//					this.openConnection();
//				}
//					statement = this.connection.createStatement();
//					statement.isClosed();
//					statement.isPoolable();
//					statement.execute("SHOW TABLES;");
//			}
//			catch (CommunicationsException e) {
//			
//				System.err.println("CommunicationsException\t"+e.getMessage());
//				this.openConnection();
//				statement = this.connection.createStatement();
//			}
//			
//			return statement;
//		}

		/**
		 * @param statement
		 * @throws SQLException
		 */
		public void closeStatement(Statement statement) throws SQLException {
			
			statement.close();
			this.closeConnection();
		}

		/**
		 * @param query
		 * @return
		 * @throws SQLException 
		 */
		public String[][] select(String query) throws SQLException {

			if(this.connection==null) {
				this.openConnection();
			}
			String[][] rset = null;
			try 
			{
				Statement stmt = this.connection.createStatement();
				ResultSet rs=stmt.executeQuery(query);

				ResultSetMetaData rsmd = rs.getMetaData();
				rs.last();
				rset = new String[rs.getRow()][rsmd.getColumnCount()];
				rs.first();

				int row=0;
				while(row<rset.length)
				{
					int col=1;
					while(col<rsmd.getColumnCount()+1)
					{

						rset[row][col-1] = rs.getString(col);
						col++;
					}
					rs.next();
					row++;
				}

				rs.close();            
				stmt.close();

			} catch (SQLException ex)
			{
				System.out.println(query);
				// handle any errors
				//			System.out.println("SQLException: " + ex.getMessage());
				//			System.out.println("SQLState: " + ex.getSQLState());
				//			System.out.println("VendorError: " + ex.getErrorCode());
				ex.printStackTrace();
			}

			//		this.closeConnection();
			return rset; 
		}

		/**
		 * @param query
		 * @return
		 */
		public ResultSet selectRS(String query) {
			
			ResultSet rs = null;
			try
			{
				Statement stmt = this.connection.createStatement();
				rs = stmt.executeQuery(query);
				//			rs.close();         
				//			stmt.close();
			}
			catch (SQLException ex)
			{
				// handle any errors
				ex.printStackTrace();
			}		
			return rs;
		}

		/**
		 * @param newtable
		 * @param columns
		 * @return
		 */
		public boolean creatTable(String newtable, String columns)
		{
			try {
				Statement stmt = connection.createStatement();
				stmt.execute("CREATE TABLE "+newtable+" ("+columns+")");
				stmt.close();
			} catch (SQLException ex) {
				// handle any errors
				ex.printStackTrace();
				return false;
			}

			return true;
		}

		/**
		 * @param statement
		 * @return
		 */
		public boolean delete(String statement)
		{
			try {
				Statement stmt = connection.createStatement();
				stmt.execute(statement);
				stmt.close();
			} catch (SQLException ex) {
				// handle any errors
				ex.printStackTrace();
				return false;
			}

			return true;
		}

		/**
		 * @param table
		 * @return
		 */
		public boolean dropTable(String table)
		{
			try {
				Statement stmt = connection.createStatement();
				stmt.execute("DROP TABLE "+table);
				stmt.close();
			} catch (SQLException ex) {
				// handle any errors
				ex.printStackTrace();
				return false;
			}

			return true;
		}

		/**
		 * @param table
		 * @param values
		 * @return
		 */
		public boolean insert(String table, String values)
		{
			try {
				Statement stmt = connection.createStatement();
				stmt.execute("INSERT "+table+" VALUES("+values+")");
				stmt.close();

			} catch (SQLException ex) {
				// handle any errors
				System.out.println("INSERT "+table+" VALUES("+values+")");
				ex.printStackTrace();
				return false;
			}

			return true;
		}

		/**
		 * @param table
		 * @param columns
		 * @param values
		 * @return
		 */
		public boolean insertInto(String table, String columns, String values)
		{
			try {
				Statement stmt = connection.createStatement();
				stmt.execute("INSERT INTO "+table+" ("+columns+") VALUES("+values+")");
				stmt.close();
			} catch (SQLException ex) {
				// handle any errors
				System.out.println("INSERT INTO "+table+" ("+columns+") VALUES("+values+")");
				ex.printStackTrace();
				return false;
			}
			return true;
		}

		/**
		 * @param tblName
		 * @return
		 */
		public String describeTable(String tblName){
			String description="";
			try {				
				PreparedStatement pstmt = connection.prepareStatement("DESCRIBE "+tblName);			
				ResultSet res=pstmt.executeQuery();

				String primaryKey="";
				while(res.next()){
					String fieldName=res.getString(1);
					String fieldType=res.getString(2);

					if(res.getString(4).equals("PRI")){
						primaryKey=primaryKey+fieldName+",";
					}				
					description=description+","+fieldName+" "+fieldType;
				}
				primaryKey=primaryKey.substring(0,primaryKey.lastIndexOf(','));
				description=description.substring(description.indexOf(",")+1);

				res.close();
				pstmt.close();
			} catch (SQLException e) {
				System.out.println("SQLException: " + e.getMessage());
				e.printStackTrace();
				e.printStackTrace();
			}		
			return description;
		}

		/**
		 * @param tblName
		 * @return
		 */
		public String describeTablePK(String tblName){
			String description="";
			try {				
				PreparedStatement pstmt = connection.prepareStatement("DESCRIBE "+tblName);			
				ResultSet res=pstmt.executeQuery();

				String primaryKey="";
				while(res.next()){
					String fieldName=res.getString(1);
					String fieldType=res.getString(2);

					if(res.getString(4).equals("PRI")){
						primaryKey=primaryKey+fieldName+",";
					}				
					description=description+","+fieldName+" "+fieldType;
				}
				primaryKey=primaryKey.substring(0,primaryKey.lastIndexOf(','));
				description=description.substring(description.indexOf(",")+1);

				description=description+", PRIMARY KEY ("+primaryKey+")";
				res.close();
				pstmt.close();
			} catch (SQLException e) {
				System.out.println("SQLException: " + e.getMessage());
				e.printStackTrace();
				e.printStackTrace();
			}		
			return description;
		}

		/**
		 * @param tblName
		 * @return
		 */
		public String fieldsTable(String tblName){
			String description="";
			try {				
				PreparedStatement pstmt = connection.prepareStatement("DESCRIBE "+tblName);			
				ResultSet res=pstmt.executeQuery();

				while(res.next()){
					String fieldName=res.getString(1);

					description=description+","+fieldName;
				}

				description=description.substring(description.indexOf(",")+1);

				res.close();
				pstmt.close();
			} catch (SQLException e) {
				System.out.println("SQLException: " + e.getMessage());
				e.printStackTrace();
				e.printStackTrace();
			}		
			return description;
		}

		/**
		 * @param tblName
		 * @return
		 */
		public boolean existTable(String tblName){
			boolean exists=false;

			try {
				PreparedStatement pstmt = connection.prepareStatement("SHOW TABLES LIKE '"+tblName+"'");			
				ResultSet res=pstmt.executeQuery();

				while(res.next()){
					exists=true;	
				}
				res.close();
				pstmt.close();
			} catch (SQLException e) {
				System.out.println("Error! While verifying table existence: ");
				e.printStackTrace();
			}		

			return exists;
		}

		/**
		 * @param table
		 * @param values
		 * @return
		 */
		public boolean update(String table, String values)
		{
			try 
			{
				Statement stmt = connection.createStatement();
				stmt.execute("UPDATE "+table+" SET "+values);
				stmt.close();

			} catch (SQLException ex) {
				// handle any errors
				System.out.println("UPDATE "+table+" SET "+values);
				ex.printStackTrace();
				return false;
			}

			return true;
		}

		/**
		 * @return
		 * @throws SQLException 
		 */
		public String[] showTables() throws SQLException
		{
			this.openConnection();
			LinkedList<String> res = new LinkedList<String>();

			try 
			{
				DatabaseMetaData md = connection.getMetaData();
				ResultSet rs = md.getTables(null, null, "%", null);


				while(rs.next()){
					res.add(rs.getString(3));
					System.out.println(rs.getString(3));
				}
				rs.close();

			} catch (SQLException ex) {
				// handle any errors
				ex.printStackTrace();
			}

			String[] rez = new String[res.size()];

			for(int i=0;i<res.size();i++) rez[i] = res.get(i);

			this.closeConnection();
			return rez;
		}

		/**
		 * @param table
		 * @return
		 * @throws SQLException 
		 */
		public String[] getMeta(String table, pt.uminho.sysbio.common.database.connector.datatypes.Connection connection) throws SQLException {

			String[] res = null;

			try {

				DatabaseMetaData meta = connection.getMetaData();
				ResultSet results = null;
				results = meta.getColumns(null, null, table, null) ;

				LinkedList<String> names = new LinkedList<String>();

				while (results.next()) {

					names.add(results.getString("COLUMN_NAME"));
				}

				res = new String[names.size()];

				for(int i=0;i<names.size();i++) res[i] = names.get(i);
			}

			catch (SQLException ex) {
				ex.printStackTrace();
			}

			return res;

		}


		public String get_database_host() {
			return database_host;
		}

		public String get_database_port() {
			return database_port;
		}

		public String get_database_name() {
			return database_host;
		}

		public String get_database_password() {
			return database_password;
		}

		public String get_database_user() {
			return database_user;
		}
		
		public DatabaseType get_database_type() {
			return DatabaseType.H2;
		}

		/**
		 * @param database_host the database_host to set
		 */
		public void setDatabase_host(String database_host) {
			this.database_host = database_host;
		}

		/**
		 * @param database_port the database_port to set
		 */
		public void setDatabase_port(String database_port) {
			this.database_port = database_port;
		}

		/**
		 * @param database_name the database_name to set
		 */
		public void setDatabase_name(String database_name) {
			this.database_name = database_name;
		}

		/**
		 * @param database_user the database_user to set
		 */
		public void setDatabase_user(String database_user) {
			this.database_user = database_user;
			this.dataSource.setUser(database_user);
		}

		/**
		 * @param database_password the database_password to set
		 */
		public void setDatabase_password(String database_password) {
			this.database_password = database_password;
			this.dataSource.setPassword(database_password);
		}

		@Override
		public void readExternal(ObjectInput arg0) throws IOException,	ClassNotFoundException {
			this.database_host=arg0.readUTF();	
			this.database_password=arg0.readUTF();
			this.database_user=arg0.readUTF();	

			this.dataSource.setUser(this.database_user);
			this.dataSource.setPassword(this.database_password);
			this.dataSource.setURL(this.database_host);
		}

		@Override
		public void writeExternal(ObjectOutput arg0) throws IOException {
			arg0.writeUTF(this.database_host);
			arg0.writeUTF(this.database_password);
			arg0.writeUTF(this.database_user);
		}

}
