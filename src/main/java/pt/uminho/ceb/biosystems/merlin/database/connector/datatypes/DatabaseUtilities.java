/**
 * 
 */
package pt.uminho.ceb.biosystems.merlin.database.connector.datatypes;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.UIManager;

import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.Enumerators.DatabaseType;
import pt.uminho.ceb.biosystems.merlin.utilities.io.FileUtils;

/**
 * @author oscar
 *
 */
public class DatabaseUtilities {

	private static Map<String, String> sqlTokens;
	private static Pattern sqlTokenPattern;


	/**
	 * @param boolean_string
	 * @return
	 */
	public static int get_boolean_int(String boolean_string){
		if(boolean_string.equalsIgnoreCase("true"))
		{
			return 1;
		}
		else if(boolean_string.equalsIgnoreCase("false"))
		{
			return 0;
		}
		return -1;
	}

	/**
	 * @param boolean_int
	 * @return
	 */
	public static boolean get_boolean_int_to_boolean(String boolean_int){

		if(boolean_int.equalsIgnoreCase("1"))
		{
			return true;
		}
		return false;
	}

	/**
	 * @param boolean_int
	 * @return
	 */
	public static boolean get_boolean_string_to_boolean(String boolean_string){

		if(boolean_string.equalsIgnoreCase("true"))
		{
			return true;
		}
		return false;
	}

	//	/**
	//	 * @param input
	//	 * @return
	//	 */
	//	public static String mysqlStrConverter(String input) {
	//	
	//		if(input == null) {
	//
	//			return null;
	//		} 
	//		else {
	//			
	//			return input.replace("\\'","'").replace("\\","\\\\").replace("'","\\").replace("-","\\-").replace("/","\\/");
	//		}
	//	}

	static
	{           
		//MySQL escape sequences: http://dev.mysql.com/doc/refman/5.7/en/string-literals.html

		String[][] search_regex_replacement = new String[][]
				{
			//search string     search regex        sql replacement regex
			{   "\u0000"    ,       "\\x00"     ,       "\\\\0"     },
			{   "'"         ,       "'"         ,       "\\\\'"     },
			{   "\""        ,       "\""        ,       "\\\\\""    },
			{   "\b"        ,       "\\x08"     ,       "\\\\b"     },
			{   "\n"        ,       "\\n"       ,       "\\\\n"     },
			{   "\r"        ,       "\\r"       ,       "\\\\r"     },
			{   "\t"        ,       "\\t"       ,       "\\\\t"     },
			{   "\u001A"    ,       "\\x1A"     ,       "\\\\Z"     },
			{   "\\"        ,       "\\\\"      ,       "\\\\\\\\"  }
				};

				sqlTokens = new HashMap<>();
				String patternStr = "";

				for (String[] srr : search_regex_replacement)
				{
					sqlTokens.put(srr[0], srr[2]);
					patternStr += (patternStr.isEmpty() ? "" : "|") + srr[1];            
				}
				sqlTokenPattern = Pattern.compile('(' + patternStr + ')');
	}


	//public static String escape(String s) {
	public static String databaseStrConverter(String s, DatabaseType databaseType) {

		//System.out.println(s);
		if(s!=null) {

			Matcher matcher = sqlTokenPattern.matcher(s);
			StringBuffer sb = new StringBuffer();
			while(matcher.find())
			{
				matcher.appendReplacement(sb, sqlTokens.get(matcher.group(1)));
			}
			matcher.appendTail(sb);
			//System.out.println(sb.toString());
			s = sb.toString();

			if (databaseType.equals(DatabaseType.H2)){
				s = s.replace("\\'","''");
			}
		}
		return s;
	}

	public static void h2CleanDatabaseFiles() {

		String driver_class_name;
		String url_db_connection;

		String path = FileUtils.getHomeFolderPath();
		driver_class_name = "org.h2.Driver";
		//url_db_connection = "jdbc:h2://"+this.host+":"+this.port;
		url_db_connection = "jdbc:h2:"+path+"/h2Database;MODE=MySQL;DATABASE_TO_UPPER=FALSE;AUTO_SERVER=TRUE";

		Connection connection = null;

		try{
			Class.forName(driver_class_name).newInstance();
			connection = (Connection) DriverManager.getConnection(url_db_connection, "root", "password");


			List<String> list = new ArrayList<String>();
			ResultSet rs;
			Statement statement = null;

			statement = (Statement) connection.createStatement();
			statement.execute( "SHOW DATABASES;");
			rs = statement.getResultSet();

			while(rs.next()) {
				list.add(rs.getString(1)+".mv.db");
				list.add(rs.getString(1)+".trace.db");
			}
			//		System.out.println("-----------------------------");
			//		System.out.println(list);
			//		System.out.println("-----------------------------");		
			File h2directory = new File(path+"/h2Database");

			if(h2directory.exists())
				for (File fileEntry : h2directory.listFiles())
					if (!list.contains(fileEntry.getName()))
						fileEntry.delete();
			connection.close();
		}
		catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	
	/**
	 * @param tablesList
	 * @param originDb
	 * @param destinyDb
	 */
	public static void dumpSelectedTablesToDatabase(List<String> tablesList, String originDb, String destinyDb, DatabaseAccess dba){
		
		String user = dba.get_database_user();
		String password = dba.get_database_password();
		String host = dba.get_database_host();
		
		String tables = "";
		
		for(String table : tablesList)
			tables.concat(table).concat(" ");
		
//		String command = "mysqldump --no-create-info --no-create-db --user=" + user + " --host=" + host + " --password=" + password 
//				+ " " +  originDb + " " + tables.trim() + " | mysql --user=" + user + " --host=" + host + " --password=" + password + " " + destinyDb;
		
//		String command = "mysqldump --no-create-info --no-create-db --user=" + user + " --host=" + host + " -p " + originDb + " gene compound "
//				+ "| mysql --user=merlindev --host=193.137.11.210 -p " + destinyDb;
		
		String command = "mysqldump --no-create-info --no-create-db -u" + user + " -h" + host + " -p" + password + " " 
				+ originDb + " " + tables + " | mysql -u" + user + " -h" + host + " -p" + password + " " + destinyDb;
		
		String os_name = System.getProperty("os.name");
		
		if(os_name.contains("Windows"))
			command = "cmd /c \"".concat(command).concat("\"");
			
		
		try {
			Process process = Runtime.getRuntime().exec(command);
			
			int exitValue = process.waitFor();
			if (exitValue != 0) {
			    System.out.println("Abnormal process termination");
			}
			else{
				System.out.println("Dumped completed with success!");
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
