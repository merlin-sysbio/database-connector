package pt.uminho.sysbio.common.database.connector.databaseAPI;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.uminho.ceb.biosystems.mew.utilities.datastructures.pair.Pair;
import pt.uminho.sysbio.common.database.connector.datatypes.Connection;

/**
 * @author Oscar Dias
 *
 */
public class ProjectAPI {

	/**
	 * @param project
	 * @param newVersion
	 * @return
	 * @throws IOException
	 * @throws SQLException
	 */
	public static int getProjectID(Connection conn, long genomeID, boolean newVersion) throws IOException, SQLException {

		int project_id = -1;

		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT id FROM projects WHERE organism_id = "+genomeID+" AND latest_version;");

		if(!rs.next() || newVersion) {

			rs = stmt.executeQuery("SELECT MAX(version) FROM projects WHERE organism_id = "+genomeID+";");

			int version = 1;

			if(rs.next()) {

				version += rs.getInt(1);

				stmt.execute("UPDATE projects SET latest_version=false WHERE organism_id = "+genomeID+";");
			}


			long time = System.currentTimeMillis();
			Timestamp timestamp = new Timestamp(time);
			stmt.execute("INSERT INTO projects (organism_id, date, latest_version, version) VALUES("+genomeID+",'"+timestamp+"',true,"+version+");");
			rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
			rs.next();
		}

		project_id = rs.getInt(1);
		rs.close();
		stmt.close();

		return project_id;
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

		ResultSet rs = statement.executeQuery("SELECT * FROM reaction WHERE NOT originalReaction;");

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
	 * Get information for e-biomass.
	 * 
	 * @param data
	 * @param statment
	 * @return
	 */
	public static Map<String, Pair<String, Double>> getModelInformationForBiomass(List<String> metaboliteIDs, Statement statment) {

		Map<String, Pair<String, Double>> map = new HashMap<>();

		ResultSet rs;

		for(String name : metaboliteIDs) {

			try {

				rs = statment.executeQuery("SELECT idcompound, molecular_weight FROM compound WHERE kegg_id = '"+name+"';");

				if(rs.next()) {

					Pair<String, Double> pair = new Pair<>(rs.getString(1), rs.getDouble(2));
					map.put(name, pair);
				}

				rs.close();
			}
			catch (SQLException e) {

				e.printStackTrace();
			}
		}
		return map;
	}
}
