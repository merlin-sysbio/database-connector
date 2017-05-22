package pt.uminho.sysbio.common.database.connector.databaseAPI;

import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import pt.uminho.sysbio.common.database.connector.datatypes.Connection;

/**
 * @author Oscar Dias
 *
 */
public class ProjectAPI {

	/**
	 * Get project id.
	 * 
	 * @param statement
	 * @param genomeID
	 * @return
	 * @throws IOException
	 * @throws SQLException
	 */
	public static int getProjectID(Statement statement, long genomeID) throws IOException, SQLException {

		int project_id = -1;

		ResultSet rs = statement.executeQuery("SELECT id FROM projects WHERE organism_id = "+genomeID+" AND latest_version;");

		if(!rs.next()) {

			rs = statement.executeQuery("SELECT MAX(version) FROM projects WHERE organism_id = "+genomeID+";");

			int version = 1;

			if(rs.next()) {

				version += rs.getInt(1);
				statement.execute("UPDATE projects SET latest_version=false WHERE organism_id = "+genomeID+";");
			}

			long time = System.currentTimeMillis();
			Timestamp timestamp = new Timestamp(time);
			statement.execute("INSERT INTO projects (organism_id, date, latest_version, version) VALUES("+genomeID+",'"+timestamp+"',true,"+version+");");
			rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
			rs.next();
		}

		project_id = rs.getInt(1);
		rs.close();
		return project_id;
	}

	public static String getCompartmentsTool(int projectID, Statement statement) {

		ResultSet rs;
		String ret = null;
		try {

			rs = statement.executeQuery("SELECT compartments_tool FROM projects WHERE id=" + projectID);

			if(rs.next())
				ret = rs.getString(1);
			rs.close();

		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return ret;
	}

	/**
	 * @param connection
	 * @return
	 */
	public static boolean isCompartmentalisedModel(Connection connection) {

		boolean ret = false;

		try  {

			Statement stmt = connection.createStatement();

			ret = ProjectAPI.isCompartmentalisedModel(stmt);

			stmt.close();
		} 
		catch (SQLException e) {e.printStackTrace();}
		return ret;
	}

	/**
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static boolean isCompartmentalisedModel(Statement statement) throws SQLException {

		boolean ret = false;

		String query = "SELECT * FROM reaction WHERE NOT originalReaction;";

		ResultSet rs = statement.executeQuery(query);

		if(rs.next())
			ret=true;

		return ret;
	}

	/**
	 * Is the transported search performed.
	 * 
	 * @param dsa
	 * @return
	 */
	public static boolean isSW_TransportersSearch(Connection connection) {
		boolean ret = false;

		try  {

			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM sw_similarities;");

			if(rs.next())
				ret=true;

			if(ret) {

				rs = stmt.executeQuery("SELECT * FROM sw_reports WHERE status = 'PROCESSING';");

				if(rs.next())
					ret=false;
			}

			stmt.close();
		} 
		catch (SQLException e) {e.printStackTrace();}
		return ret;
	}

	/**
	 * @param dsa
	 * @return
	 */
	public static boolean isDatabaseGenesDataLoaded(Connection connection) {
		boolean ret = false;

		try  {

			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM gene;");

			if(rs.next())
				ret=true;

			stmt.close();
		} 
		catch (SQLException e) {e.printStackTrace();}
		return ret;
	}

	/**
	 * @param dsa
	 * @return
	 */
	public static boolean isMetabolicDataLoaded(Connection connection) {
		boolean ret = false;

		try  {

			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM compound;");

			if(rs.next())
				ret=true;

			stmt.close();
		} 
		catch (SQLException e) {e.printStackTrace();}
		return ret;
	}

	/**
	 * @param dsa
	 * @return
	 */
	public static boolean findComparmtents(Connection connection) {
		boolean ret = false;

		try  {

			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM psort_reports;");

			if(rs.next())
				ret=true;

			stmt.close();
		} 
		catch (SQLException e) {e.printStackTrace();}
		return ret;
	}

	/**
	 * Are the transporters loaded.
	 * 
	 * @param connection
	 * @return
	 */
	public static boolean isTransporterLoaded(Connection connection) {
		boolean ret = false;

		try  {

			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM pathway WHERE code='T0001';");

			if(rs.next()) {

				ret=true;
			}
			stmt.close();
		} 
		catch (SQLException e) {e.printStackTrace();}
		return ret;
	}

	/**
	 * Check wether transporters are integrated in internal database.
	 * 
	 * @param connection
	 * @param project_id 
	 * @return
	 */
	public static boolean isTransportersIntegrated(Connection connection, int project_id) {
		boolean ret = false;

		try  {

			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM genes WHERE project_id = "+project_id+ ";");

			if(rs.next())
				ret=true;

			stmt.close();
		} 
		catch (SQLException e) {e.printStackTrace();}
		return ret;
	}

	public static boolean checkTableName(Connection connection, String oldName, String newName){
		
		try {
			DatabaseMetaData meta;
			meta = connection.getMetaData();
			ResultSet rs = meta.getTables(null, null, newName, new String[] {"TABLE","VIEW"});
			if(rs.next()) {
				return true;
			}
			else {
				rs = meta.getTables(null, null, oldName, new String[] {"TABLE","VIEW"});
				if(rs.next()) {
					Statement stmt = connection.createStatement();
					stmt.execute("ALTER TABLE "+oldName+" RENAME TO "+newName+";");
					stmt.close();
					return true;
				}
			}
			rs.close();
			
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

}
