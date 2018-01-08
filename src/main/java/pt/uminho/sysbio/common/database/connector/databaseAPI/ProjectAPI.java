package pt.uminho.sysbio.common.database.connector.databaseAPI;

import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import pt.uminho.ceb.biosystems.mew.utilities.datastructures.pair.Pair;
import pt.uminho.sysbio.common.database.connector.datatypes.Connection;
import pt.uminho.sysbio.merlin.utilities.containers.model.MetaboliteContainer;

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

	/**
	 * @param projectID
	 * @param statement
	 * @return
	 */
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
	 * @param connection
	 * @return
	 */
	public static boolean hasDrains(Connection connection) {

		boolean ret = false;

		try  {

			Statement stmt = connection.createStatement();

			ret = ProjectAPI.hasDrains(stmt);

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
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static boolean hasDrains(Statement statement) throws SQLException {

		boolean ret = false;

		String query = "SELECT * FROM pathway WHERE code='D0001';";

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

	/**
	 * @param connection
	 * @param oldName
	 * @param newName
	 * @return
	 */
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

	/**
	 * Get the name of a reaction for a given id.
	 * @param conn
	 * @param reaction
	 * @return
	 */
	public static Set<String> getReactionName(List<String> reaction, Statement statement){

		Set<String> results  = new HashSet<String>();
		
		try {
			for(String names : reaction){
				ResultSet rs = statement.executeQuery("SELECT name FROM reaction WHERE idreaction="+names+";");
				while(rs.next())
					results.add(rs.getString("name"));
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return results;
	}

	/**
	 * Get organismID.
	 * @param conn
	 * @return
	 */
	public static String getOrganismID(Statement statement){

		try  {

			ResultSet rs = statement.executeQuery("SELECT organism_id FROM projects WHERE latest_version = TRUE;");

			if(rs.next()){
				return rs.getString("organism_id");
			}
		} 
		catch (SQLException e) {e.printStackTrace();}

		return null;
	}


	/**
	 * Update organismID.
	 * @param conn
	 * @param orgID
	 */
	public static void updateOrganismID(Connection conn, String orgID) {

		int version = 1;

		try  {

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM projects WHERE organism_id ="+orgID+";");

			if (rs.next()){
				stmt.execute("UPDATE projects SET latest_version = FALSE;");
				stmt.execute("UPDATE projects SET latest_version = TRUE WHERE organism_id ="+orgID+";");
			}
			else{
				long time = System.currentTimeMillis();
				Timestamp timestamp = new Timestamp(time);
				stmt.execute("INSERT INTO projects (organism_id, date, latest_version, version) VALUES('"+orgID+"','"+timestamp+"',true,'"+ version +"');");
				updateOrganismID(conn, orgID);
			}

			rs.close();
			stmt.close();
		} 
		catch (SQLException e) {e.printStackTrace();}

	}

	/**
	 * @param id
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getRowInfo(String id, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT c.name, pc.score FROM psort_reports_has_compartments  AS pc "
				+ " INNER JOIN compartments AS c ON pc.compartment_id=c.id "
				+ " INNER JOIN psort_reports AS pr ON pc.psort_report_id=pr.id "
				+ " WHERE pr.locus_tag='"+id+"' ORDER BY pc.score DESC");

		while(rs.next()){
			String[] list = new String[2];
			list[0]=rs.getString(1);
			list[1]=rs.getString(2);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * Gets compounds that participate in reactions.
	 * @param id
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getCompoundIDsFromStoichiometry(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT compound_idcompound, stoichiometric_coefficient FROM stoichiometry;");

		while(rs.next()){
			String[] list = new String[2];
			list[0]=rs.getString(1);
			list[1]=rs.getString(2);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * Get all information from compound table.
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getAllFromCompound(String table, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT * FROM " + table);

		while(rs.next()){
			String[] list = new String[5];
			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);
			list[3]=rs.getString(4);
			list[4]=rs.getString(5);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * @param table
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getCompoundInformation(String table, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT * FROM "+ table + " ORDER BY entry_type, name");

		while(rs.next()){
			String[] list = new String[6];
			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);
			list[3]=rs.getString(4);
			list[4]=rs.getString(5);
			list[5]=rs.getString(6);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * Get alias from aliases table with class 'c' for a given entity.
	 * @param table
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String> getAliasClassC(String id, Statement stmt) throws SQLException{

		ArrayList<String> res = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT alias FROM aliases WHERE class = 'c' AND entity = " + id);

		while(rs.next())
			res.add(rs.getString(1));
		
		if(res.size() == 0)
			res.add("");

		rs.close();
		return res;
	}

	/**
	 * Get alias from aliases table with class 'tu' for a given entity.
	 * @param table
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String> getAliasClassTU(String id, Statement stmt) throws SQLException{

		ArrayList<String> res = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT alias FROM aliases WHERE class = 'tu' AND entity = " + id);

		while(rs.next())
			res.add(rs.getString(1));
		
		rs.close();
		return res;
	}

	/**
	 * Get alias from aliases table with class 'g' for a given entity.
	 * @param table
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String> getAliasClassG(String id, Statement stmt) throws SQLException{

		ArrayList<String> res = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT alias FROM aliases WHERE class = 'g' AND entity = " + id);

		while(rs.next())
			res.add(rs.getString(1));
		
		rs.close();
		return res;
	}


	/**
	 * Get alias from aliases table with class 'r' for a given entity.
	 * @param table
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String> getAliasClassR(int id, Statement stmt) throws SQLException{

		ArrayList<String> res = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT alias FROM aliases WHERE class = 'r' AND entity = "+id);

		while(rs.next())
			res.add(rs.getString(1));

		rs.close();
		return res;
	}

	/**
	 * Get alias from aliases table with class 'r' for a given entity.
	 * @param table
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String> getAliasClassP(String id, Statement stmt) throws SQLException{

		ArrayList<String> res = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT alias FROM aliases WHERE class = 'p' AND entity = "+id);

		while(rs.next())
			res.add(rs.getString(1));

		rs.close();
		return res;
	}

	/**
	 * Get entry_type from compound table
	 * @param id
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String> getEntryType(String id, Statement stmt) throws SQLException{

		ArrayList<String> res = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT entry_type FROM compound WHERE idCompound = " + id );

		while(rs.next())
			res.add(rs.getString(1));

		rs.close();
		return res;
	}

	/**
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getCompoundInformation2(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT compound.name, compound.formula, COUNT(reaction_idreaction) AS numR, kegg_id, idcompound "+
				" FROM compound " +
				" INNER JOIN stoichiometry ON (compound_idcompound=idcompound) "+
				" GROUP BY kegg_id "+
				" ORDER BY kegg_id, numR DESC;");

		while(rs.next()){
			String[] list = new String[5];
			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);
			list[3]=rs.getString(4);
			list[4]=rs.getString(5);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * Gets compounds with biological roles.
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getCompoundWithBiologicalRoles(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT * FROM compound "
				+ " WHERE idcompound NOT IN (SELECT DISTINCT(compound_idcompound) FROM stoichiometry) "
				+ " AND hasBiologicalRoles "
				+ " ORDER BY kegg_id;");

		while(rs.next()){
			String[] list = new String[6];
			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);
			list[3]=rs.getString(4);
			list[4]=rs.getString(5);
			list[5]=rs.getString(6);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * Get reaction of a given compound.
	 * @param id
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getCompoundReactions(String id, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT compound.name, compound.formula, reaction.name, reaction.equation "+
				"FROM (stoichiometry " +
				"JOIN(compound) "+
				"ON (compound_idcompound=idcompound) " +
				"JOIN (reaction) "+
				"ON (reaction_idreaction=idreaction)) "+
				"WHERE idcompound = '"+id+"';");

		while(rs.next()){
			String[] list = new String[4];
			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);
			list[3]=rs.getString(4);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * Get compounds statistics.
	 * @param table
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getCompoundStats(String table, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT * FROM "+table);

		while(rs.next()){
			String[] list = new String[5];
			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);
			list[3]=rs.getString(4);
			list[4]=rs.getString(5);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * Get all information from pathway table.
	 * @param table
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getAllFromPathWay(String table, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT * FROM "+table);

		while(rs.next()){
			String[] list = new String[3];
			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * Get pathway ID, code and name.
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getPathwayID(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT idpathway, code, name FROM pathway ORDER BY name;");

		while(rs.next()){
			String[] list = new String[3];
			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * Count reactions by pathwayID.
	 * @param qls
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static HashMap<String,String[]> countReactionsByPathwayID(HashMap<String,String[]> qls, Statement stmt) throws SQLException{

		ResultSet rs = stmt.executeQuery("SELECT pathway_idpathway, count(reaction_idreaction) " +
				"FROM pathway " +
				"RIGHT JOIN pathway_has_reaction ON pathway_idpathway=pathway.idpathway " +
				"GROUP BY pathway_idpathway ORDER BY name;");

		while(rs.next())
			qls.get(rs.getString(1))[2] = rs.getString(2);

		rs.close();
		return qls;
	}

	/**
	 * Count proteinID by pathwayID.
	 * @param qls
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static HashMap<String,String[]> countProteinIdByPathwayID(HashMap<String,String[]> qls, Statement stmt) throws SQLException{

		ResultSet rs = stmt.executeQuery("SELECT pathway_idpathway, count(enzyme_protein_idprotein) " +
				"FROM pathway " +
				"RIGHT JOIN pathway_has_enzyme ON pathway_idpathway=pathway.idpathway " +
				"GROUP BY pathway_idpathway ORDER BY name;");

		while(rs.next()) 
			qls.get(rs.getString(1))[3] = rs.getString(2);

		rs.close();
		return qls;
	}

	/**
	 * Count reactions.
	 * @param id
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> countReactions(String id, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT distinct(reaction.idreaction), name, equation " +
				"FROM pathway_has_reaction " +
				"INNER JOIN reaction ON idreaction = reaction_idreaction " +
				"WHERE pathway_idpathway = " + id + " " +
				"ORDER BY name;");

		while(rs.next()){
			String[] list = new String[3];
			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * Get data from enzyme table.
	 * @param id
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getDataFromEnzyme(String id, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT distinct(enzyme.ecnumber), protein.name, class, inModel FROM enzyme " +
				"INNER JOIN pathway_has_enzyme ON pathway_has_enzyme.enzyme_ecnumber = enzyme.ecnumber " +
				"INNER JOIN protein ON protein.idprotein = enzyme.protein_idprotein " +
				"WHERE pathway_idpathway="+id+ " " +
				"ORDER BY enzyme.ecnumber;");

		while(rs.next()){
			String[] list = new String[4];
			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);
			list[3]=rs.getBoolean(4)+"";

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**Executes a given SQL query.
	 * @param query
	 * @param statement
	 * @throws SQLException
	 */
	public static void executeQuery(String query, Statement stmt) throws SQLException{

		stmt.execute(query);
	}

	//	/**
	//	 *Execute and get last insertID.
	//	 * 
	//	 * @param query
	//	 * @param stmt
	//	 * @return
	//	 * @throws SQLException
	//	 */
	//	public static String executeAndGetLastInsertID(String query, Statement stmt) throws SQLException{
	//
	//		String idNew = null;
	//
	//		synchronized (stmt) {
	//			
	//			stmt.execute(query);
	//			ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
	//
	//			if(rs.next())
	//				idNew = rs.getString(1);	
	//			rs.close();
	//		}
	//
	//		return idNew;
	//	}


	/**
	 *Execute and get last insertID.
	 * 
	 * @param query
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static int executeAndGetLastInsertID(String query, Statement stmt) throws SQLException{

		//stmt.execute(query);
		stmt.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
		ResultSet rs = stmt.getGeneratedKeys();
		if(rs.next())
			return rs.getInt(1);

		return -1;
	}
	
	


	/**
	 * Count compounds with a given name.
	 * @param name
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static int countCompoundsByName(String name, Statement stmt) throws SQLException{

		int res = 0;

		ResultSet rs = stmt.executeQuery("SELECT COUNT(name) FROM compound WHERE compound.name ='" + name + "';");

		while(rs.next())
		{
			res = Integer.parseInt(rs.getString(1));
		}

		rs.close();
		return res;
	}

	/**
	 * Get data of a givem metabolite.
	 * @param name
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String> getMetaboliteData(String name, Statement stmt) throws SQLException{

		ArrayList<String> res = new ArrayList<String>();

		ResultSet rs = stmt.executeQuery("SELECT * FROM compound WHERE name = '" + name + "';");

		while(rs.next())
		{
			res.add(rs.getString("name"));
			res.add(rs.getString("entry_type"));
			res.add(rs.getString("formula"));
			res.add(rs.getString("molecular_weight"));
			res.add(rs.getString("charge"));
		}

		rs.close();
		return res;
	}

	/**
	 * Get reactions related with a given metabolite.
	 * @param name
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String> getRelatedReactions(String name, Statement stmt) throws SQLException{

		ArrayList<String> reactions = new ArrayList<String>();

		ResultSet rs = stmt.executeQuery("SELECT reaction.name FROM reaction INNER JOIN stoichiometry "
				+ "ON reaction_idreaction = idreaction INNER JOIN compound "
				+ "ON compound_idcompound=idcompound WHERE compound.name = '" + name + "';");

		while(rs.next())
			reactions.add(rs.getString("name"));

		rs.close();
		return reactions;
	}

	/**
	 * Get reactions data.
	 * @param aux
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getReactionsData(String aux, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT * FROM reaction"+aux);

		while(rs.next()){
			String[] list = new String[3];
			list[0]=rs.getString(2);
			list[1]=rs.getString(3);
			list[2]=rs.getString(4);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * Get number of reactants and products.
	 * @param aux
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static Pair<Double, Double> getReactantsAndProducts(String aux, Statement stmt) throws SQLException{

		Pair<Double, Double> res = new Pair<Double, Double>(0.0, 0.0);
		double nreactants = 0.0;
		double nproducts = 0.0;

		ResultSet rs = stmt.executeQuery("SELECT stoichiometric_coefficient, count(distinct(compound_idcompound)), "
				+ " reaction_idreaction " +
				"FROM stoichiometry " +
				"INNER JOIN reaction ON (reaction_idreaction = reaction.idreaction) " +
				aux+" " +
				"GROUP BY reaction_idreaction, stoichiometric_coefficient");

		while(rs.next()) {

			//			if(Double.parseDouble(rs.getString(1))<0)
			//				nreactants += rs.getDouble(2);
			//			else if(Double.parseDouble(rs.getString(1))>0)
			//				nproducts += rs.getDouble(2);

			if(rs.getString(1).startsWith("-"))
				nreactants += rs.getDouble(2);
			else
				nproducts += rs.getDouble(2);	

		}

		res.setA(nreactants);
		res.setB(nproducts);

		rs.close();
		return res;
	}

	/**
	 * Get pathways names.
	 * @param aux
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static Map <String, String> getPathwaysNames(Statement statement) throws SQLException{

		Map <String, String> pathways = new TreeMap <String, String>();

		ResultSet rs = statement.executeQuery("SELECT idpathway, name FROM pathway");

		while(rs.next())
			pathways.put(rs.getString(1), rs.getString(2));

		rs.close();
		return pathways;
	}

	/**
	 * @param query
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getPathways(String query, Statement statement) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = statement.executeQuery(query);

		while(rs.next()){
			String[] list = new String[3];
			list[0]=rs.getString(1);
			list[1]=rs.getString(2);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * Get reactionID for a given reaction name.
	 * @param name
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static int getReactionIdByName(String name, Statement stmt) throws SQLException{

		int res = -1;

		ResultSet rs = stmt.executeQuery("SELECT idreaction FROM reaction WHERE name = '"+name+"'");

		if(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 * Check if a reaction is in model.
	 * @param id
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static boolean isReactionInModel(int id, Statement statement) throws SQLException{

		ResultSet rs = statement.executeQuery("SELECT inModel FROM reaction WHERE idreaction='"+id+"';");

		rs.next();
		boolean inModelReaction=rs.getBoolean(1);


		rs.close();
		return inModelReaction;
	}

	/**
	 * Get Existing PathwaysID.
	 * @param idReaction
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static Map<Integer,String> getExistingPathwaysID(int idReaction, Statement statement) throws SQLException{

		Map<Integer,String> existingPathwaysID = new TreeMap<>();

		ResultSet rs = statement.executeQuery("SELECT pathway_idpathway FROM pathway_has_reaction WHERE reaction_idreaction = "+idReaction);

		while(rs.next()) 
			existingPathwaysID.put(rs.getInt(1),"");

		rs.close();
		return existingPathwaysID;
	}

	/**
	 * Get Existing EnzymesID.
	 * @param idReaction
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static List<String> getExistingEnzymesID(int idReaction, Statement statement) throws SQLException{

		List<String> existingEnzymesID = new ArrayList<String>();

		ResultSet rs = statement.executeQuery("SELECT enzyme_ecnumber, reaction_has_enzyme.enzyme_protein_idprotein, protein.name FROM reaction_has_enzyme " +
				" INNER JOIN protein ON protein.idprotein = reaction_has_enzyme.enzyme_protein_idprotein " +
				" WHERE reaction_idreaction = "+idReaction);

		while(rs.next()) 
			existingEnzymesID.add(rs.getString(1)+"___"+rs.getString(3)+"___"+rs.getString(2));

		rs.close();
		return existingEnzymesID;
	}

	/**
	 * Check if the the information already exists in the table.
	 * @param ecnumber
	 * @param idProtein
	 * @param idReaction
	 * @param statement
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkReactionHasEnzymeData(String ecnumber, int idProtein, int idReaction, Statement statement) throws SQLException{

		boolean exists = false;

		ResultSet rs = statement.executeQuery("SELECT * FROM reaction_has_enzyme WHERE enzyme_ecnumber='" +
				ecnumber+ "' AND enzyme_protein_idprotein = "+idProtein+" AND reaction_idreaction = "+idReaction);

		if(rs.next()) 
			exists = true;

		rs.close();
		return exists;
	}


	/**
	 * Get Existing EnzymesID.
	 * @param idReaction
	 * @param statement
	 * @return List<String>
	 * @throws SQLException
	 */
	public static List<String> getExistingEnzymesID2(int idReaction, Statement statement) throws SQLException{

		List<String> existingEnzymesID = new ArrayList<String>();

		ResultSet rss = statement.executeQuery("SELECT enzyme_ecnumber, protein.idprotein, protein.name" +
				" FROM reaction_has_enzyme " +
				" INNER JOIN protein ON protein.idprotein = enzyme_protein_idprotein " +
				" WHERE reaction_idreaction = "+idReaction);

		while(rss.next()) 
			existingEnzymesID.add(rss.getString(1)+"___"+rss.getString(3)+"___"+rss.getString(2));

		rss.close();
		return existingEnzymesID;
	}

	/**
	 * Get Existing PathwaysID.
	 * @param idReaction
	 * @param statement
	 * @return Map<String,String>
	 * @throws SQLException
	 */
	public static Map<Integer,String> getExistingPathwaysID2(Map<Integer,String> existingPathwaysID, int idReaction, Statement statement) throws SQLException{

		ResultSet rs = statement.executeQuery("SELECT pathway_idpathway FROM pathway_has_reaction WHERE reaction_idreaction = "+idReaction);

		while(rs.next()) 
			existingPathwaysID.put(rs.getInt(1),"");

		rs.close();
		return existingPathwaysID;
	}

	/**
	 * Get pathwayID for a given name.
	 * @param aux
	 * @param statement
	 * @return String
	 * @throws SQLException
	 */
	public static int getPathwayID(String aux, Statement statement) throws SQLException{

		int res = -1;

		ResultSet rs = statement.executeQuery("SELECT idpathway FROM pathway WHERE name = '" + aux + "'");

		if(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 * Check if the the information already exists in the table.
	 * @param ecnumber
	 * @param idProtein
	 * @param rowID
	 * @param statement
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkPathwayHasEnzymeData(String ecnumber, int idProtein, int pathway, Statement statement) throws SQLException{

		boolean exists = false;

		ResultSet rs = statement.executeQuery("SELECT * FROM pathway_has_enzyme WHERE enzyme_ecnumber='" +
				ecnumber+ "' AND enzyme_protein_idprotein = "+idProtein+" AND pathway_idpathway = "+pathway);

		if(rs.next()) 
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 * Get data for the reaction container.
	 * @param rowID
	 * @param statement
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static String[] getDataForReactionContainer(int rowID, Statement statement) throws SQLException{

		String[] list = new String[12];

		ResultSet rs = statement.executeQuery(
				"SELECT reaction.name, equation, reversible, pathway.name, inModel, compartment.name, isSpontaneous, isNonEnzymatic, isGeneric,"
						+ " lowerBound, upperBound, boolean_rule " +
						" FROM reaction " +
						" LEFT JOIN pathway_has_reaction ON idreaction=reaction_idreaction " +
						" LEFT JOIN pathway ON pathway_idpathway = idpathway " +
						" LEFT JOIN compartment ON compartment_idcompartment = compartment.idcompartment " +
						" WHERE idreaction = "+rowID);

		while(rs.next()){
			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getBoolean(3)+"";
			list[3]=rs.getString(4);
			list[4]=rs.getBoolean(5)+"";
			list[5]=rs.getString(6);
			list[6]=rs.getBoolean(7)+"";
			list[7]=rs.getBoolean(8)+"";
			list[8]=rs.getBoolean(9)+"";
			list[9]=rs.getString(10);
			list[10]=rs.getString(11);
			list[11]=rs.getString(12);

		}
		rs.close();
		return list;
	}

	/**
	 * @param rowID
	 * @param statement
	 * @return String[]
	 * @throws SQLException
	 */
	public static Map<String, MetaboliteContainer> getStoichiometryData(int rowID, Statement statement) throws SQLException{

		Map<String, MetaboliteContainer> res = new TreeMap<String, MetaboliteContainer>();

		ResultSet rs = statement.executeQuery("SELECT idcompound, compound.name, compound.formula, " +
				"stoichiometric_coefficient, " +
				"numberofchains, " +
				"compartment.name " +
				", idstoichiometry " +
				"FROM stoichiometry " +
				"JOIN compound " +
				"ON idcompound=compound_idcompound " +
				"JOIN compartment " +
				"ON (compartment_idcompartment=idcompartment) " +
				"WHERE reaction_idreaction = '"+rowID+"';");

		while(rs.next()) {
			
			MetaboliteContainer metaboliteContainer = new MetaboliteContainer(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6)) ;
			res.put(rs.getString(7), metaboliteContainer);
		}

		rs.close();
		return res;
	}

	/**
	 * @param res
	 * @param aux
	 * @param statement
	 * @return String[][]
	 * @throws SQLException
	 */
	public static String[][] getAllMetabolites(String[][] res, String aux, Statement statement) throws SQLException{

		ResultSet rs = statement.executeQuery("SELECT name, formula, idcompound, kegg_id FROM compound ORDER BY "+aux+", name;");

		rs.last();
		res[0] = new String[rs.getRow()+1];
		res[1] = new String[rs.getRow()+1];
		res[2] = new String[rs.getRow()+1];
		res[3] = new String[rs.getRow()+1];
		rs.beforeFirst();

		int m=1;
		while(rs.next()) {

			res[0][m] = rs.getString(3);

			if(rs.getString(1)!= null && rs.getString(2)!= null) {

				res[1][m] = rs.getString(1)+"__"+rs.getString(2)+"__"+rs.getString(4);
				res[3][m] = rs.getString(4)+"_"+rs.getString(1)+"_"+rs.getString(2);
			}
			else if(rs.getString(1) == null && rs.getString(2)!= null) {

				res[1][m] = rs.getString(2)+"__"+rs.getString(4);
				res[3][m] = rs.getString(4)+"_"+rs.getString(2);
			}
			else if(rs.getString(1)!= null && rs.getString(2) == null) {

				res[1][m] = rs.getString(1)+"__"+rs.getString(4);
				res[3][m] = rs.getString(4)+"_"+rs.getString(1);
			}
			else {

				res[1][m] = rs.getString(4);
				res[3][m] = rs.getString(4);
			}

			res[2][m] = rs.getString(1);

			m++;
		}
		rs.close();
		return res;
	}

	/**
	 * @param aux
	 * @param pathwayID
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getPathwayHasEnzymeData(String aux, int pathwayID, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT reaction_has_enzyme.enzyme_ecnumber, reaction_has_enzyme.enzyme_protein_idprotein, reaction.inModel, reaction.name, enzyme.inModel, protein.name, kegg_id "
				+ " FROM pathway_has_enzyme "+
				" INNER JOIN reaction_has_enzyme ON (pathway_has_enzyme.enzyme_ecnumber = reaction_has_enzyme.enzyme_ecnumber "
				+ "AND pathway_has_enzyme.enzyme_protein_idprotein = reaction_has_enzyme.enzyme_protein_idprotein) "+
				" INNER JOIN reaction ON (reaction.idreaction = reaction_idreaction) "+
				" INNER JOIN stoichiometry ON stoichiometry.reaction_idreaction = reaction.idreaction " +
				" INNER JOIN compound ON (compound_idcompound = compound.idcompound) " +
				" INNER JOIN pathway_has_reaction ON (pathway_has_reaction.reaction_idreaction = reaction.idreaction) "+
				" INNER JOIN enzyme ON (enzyme.ecnumber = reaction_has_enzyme.enzyme_ecnumber "
				+ "AND enzyme.protein_idprotein = reaction_has_enzyme.enzyme_protein_idprotein) "+
				" INNER JOIN protein ON protein.idprotein = protein_idprotein " + aux +
				" AND pathway_has_enzyme.pathway_idpathway = "+pathwayID+" AND pathway_has_reaction.pathway_idpathway = "+pathwayID);

		while(rs.next()){
			String[] list = new String[7];
			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getBoolean(3)+"";
			list[3]=rs.getString(4);
			list[4]=rs.getBoolean(5)+"";
			list[5]=rs.getString(6);
			list[6]=rs.getString(7);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * @param aux
	 * @param aux2
	 * @param pathwayID
	 * @param statement
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getReactionsList(String aux, String aux2, int pathwayID, Statement statement) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = statement.executeQuery("SELECT reaction.name, kegg_id FROM pathway_has_reaction "
				+ " INNER JOIN reaction ON (pathway_has_reaction.reaction_idreaction = reaction.idreaction) "
				+ " INNER JOIN stoichiometry ON stoichiometry.reaction_idreaction = reaction.idreaction "
				+ " INNER JOIN compound ON (compound_idcompound = compound.idcompound) "
				+ " LEFT JOIN reaction_has_enzyme ON (reaction_has_enzyme.reaction_idreaction = reaction.idreaction) "+ aux +
				" AND inModel " + aux2 + " AND pathway_idpathway = "+pathwayID);

		while(rs.next()){
			String[] list = new String[2];
			list[0]=rs.getString(1);
			list[1]=rs.getString(2);

			result.add(list);
		}
		rs.close();
		return result;
	}	

	/**
	 * Check if a reaction of a given name exists.
	 * @param name
	 * @param statement
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkReactionExistence(String name, Statement statement) throws SQLException{

		boolean exists = false;

		ResultSet rs = statement.executeQuery("SELECT * FROM reaction WHERE name = '"+name+"'");

		if(rs.next()){
			exists = true;
		}
		rs.close();
		return exists;
	}	

	/**
	 * Check if any gene exists.
	 * @param name
	 * @param statement
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkGenes(Statement statement) throws SQLException{

		boolean exists = false;

		ResultSet rs = statement.executeQuery("SELECT * FROM gene;");

		if(rs.next()){
			exists = true;
		}
		rs.close();
		return exists;
	}	

	/**
	 * @param table
	 * @param stmt
	 * @return String
	 * @throws SQLException
	 */
	public static String getStats(String table, Statement stmt) throws SQLException{

		String num = "";

		ResultSet rs = stmt.executeQuery("SELECT count(*) FROM "+table);

		if(rs.next())
			num = rs.getString(1);

		rs.close();
		return num;
	}

	/**
	 * @param table
	 * @param stmt
	 * @return ArrayList<String>
	 * @throws SQLException
	 */
	public static ArrayList<String> getLocusTagAndName(String table, Statement stmt) throws SQLException{

		ArrayList<String> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT DISTINCT(locusTag), name FROM "+table);

		while(rs.next())
			result.add(rs.getString(2));

		rs.close();
		return result;
	}

	/**
	 * Count genes synonyms.
	 * @param table
	 * @param stmt
	 * @return String
	 * @throws SQLException
	 */
	public static String countGenesSynonyms(Statement stmt) throws SQLException{

		String snumgenes ="";

		ResultSet rs = stmt.executeQuery("SELECT count(*) FROM aliases where class = 'g'");

		if(rs.next())
			snumgenes = rs.getString(1);

		rs.close();
		return snumgenes;
	}

	/**
	 * Count the number of genes that encode proteins.
	 * @param stmt
	 * @return int
	 * @throws SQLException
	 */
	public static int countGenesEncodingProteins(Statement stmt) throws SQLException{

		int prot = 0;

		ResultSet rs = stmt.executeQuery("SELECT COUNT(DISTINCT(gene_idgene)) AS counter FROM subunit");

		if(rs.next())
			prot=rs.getInt(1);

		rs.close();
		return prot;
	}

	/**Count the number of genes that encoding enzymes and transporters.
	 * @param stmt
	 * @return Pair<Integer, Integer>
	 * @throws SQLException
	 */
	public static Pair<Integer, Integer> countGenesEncodingEnzymesAndTransporters(Statement stmt) throws SQLException{

		int enz=0, trp=0;

		Pair<Integer, Integer> res = new Pair<Integer, Integer>(0, 0);

		ResultSet rs = stmt.executeQuery("SELECT COUNT(DISTINCT(gene_idgene)) AS counter, source FROM subunit " +
				"INNER JOIN enzyme ON enzyme.protein_idprotein = subunit.enzyme_protein_idprotein " +
				"AND subunit.enzyme_ecnumber = enzyme.ecnumber " 
				+"GROUP BY SOURCE");

		while (rs.next()) {

			if(rs.getString("source").equalsIgnoreCase("TRANSPORTERS"))
				trp=rs.getInt("counter");
			else
				enz+=rs.getInt("counter");
		}

		res.setA(enz);
		res.setB(trp);

		rs.close();
		return res;
	}

	/**
	 * Count number of genes in model.
	 * @param stmt
	 * @return int
	 * @throws SQLException
	 */
	public static int countGenesInModel(Statement stmt) throws SQLException{

		int inModel = 0;

		ResultSet rs = stmt.executeQuery("SELECT COUNT(DISTINCT(gene_idgene)) FROM subunit "+
				"INNER JOIN enzyme ON subunit.enzyme_protein_idprotein = enzyme.protein_idprotein WHERE inModel");

		if(rs.next())
			inModel=rs.getInt(1);

		rs.close();
		return inModel;
	}	

	/**
	 * Get proteinIDs for a given geneID.
	 * @param id
	 * @param stmt
	 * @return ArrayList<String>
	 * @throws SQLException
	 */
	public static ArrayList<String> getProteinIDs(String id, Statement stmt) throws SQLException{

		ArrayList<String> res = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT protein.name " +
				"FROM regulatory_event as event, transcription_unit, transcription_unit_gene " +
				"AS tug, transcription_unit_promoter as tup, promoter,gene, protein " +
				"WHERE event.promoter_idpromoter=idpromoter AND tup.promoter_idpromoter=idpromoter " +
				"AND tup.transcription_unit_idtranscription_unit=idtranscription_unit " +
				"AND tug.transcription_unit_idtranscription_unit=idtranscription_unit AND " +
				"gene_idgene=idgene AND protein.idprotein = event.protein_idprotein AND idgene = "+id);

		while(rs.next())
			res.add(rs.getString(1));

		rs.close();
		return res;
	}

	/**
	 * Get data from subunit table for a given geneID.
	 * @param id
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getDataFromSubunit(String id, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT protein.name, protein.class, enzyme_ecnumber FROM subunit JOIN protein ON " +
				"subunit.enzyme_protein_idprotein = protein.idprotein WHERE subunit.gene_idgene = "+id);

		while(rs.next()){
			String[] list = new String[3];
			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);

			result.add(list);
		}
		rs.close();
		return result;
	}	

	/**
	 * Get data from gene_has_orthology table for a given geneID.
	 * @param id
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getDataFromGeneHasOrthology(String id, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT entry_id, locus_id, similarity FROM gene_has_orthology " +
				" JOIN orthology ON orthology_id = orthology.id " +
				" WHERE gene_idgene = "+id);

		while(rs.next()){
			String[] list = new String[3];
			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * Get data from gene table for a given geneID.
	 * @param id
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getDataFromGene(String id, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT idgene, compartment.name, primaryLocation, score " +
				" FROM gene " +
				" INNER JOIN gene_has_compartment ON (idgene = gene_has_compartment.gene_idgene) " +
				" INNER JOIN compartment ON (idcompartment = compartment_idcompartment) " +
				" WHERE idgene = " + id);

		while(rs.next()){
			String[] list = new String[4];
			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getBoolean(3)+"";
			list[3]=rs.getString(4);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * Get gene data.
	 * @param id
	 * @param stmt
	 * @return String[]
	 * @throws SQLException
	 */
	public static String[] getGeneData(String id, Statement stmt) throws SQLException{

		String[] data = new String[8];

		ResultSet rs = stmt.executeQuery("SELECT idgene, chromosome.name, gene.name, transcription_direction, left_end_position, right_end_position, boolean_rule, locusTag " +
				" FROM gene LEFT JOIN chromosome ON (idchromosome = chromosome_idchromosome) WHERE idgene ="+id);

		if(rs.next()) {

			data[0] = rs.getString(1);
			data[1] = rs.getString(2);
			data[2] = rs.getString(3);
			data[3] = rs.getString(4);
			data[4] = rs.getString(5);
			data[5] = rs.getString(6);
			data[6] = rs.getString(7);
			data[7] = rs.getString(8);
		}
		rs.close();
		return data;
	}

	/**
	 * Get subunits.
	 * @param id
	 * @param stmt
	 * @return String[][]
	 * @throws SQLException
	 */
	public static String[][] getSubunits(String id, Statement stmt) throws SQLException{

		String[][] res = null;

		ResultSet rs = stmt.executeQuery( "SELECT enzyme_protein_idProtein, enzyme_ecNumber FROM subunit WHERE gene_idgene ="+id);

		ResultSetMetaData rsmd = rs.getMetaData();
		rs.last();
		res = new String[rs.getRow()][rsmd.getColumnCount()];
		rs.first();

		int row=0;
		while(row<res.length) {

			res[row][0] = rs.getString(1)+"__"+rs.getString(2);
			rs.next();
			row++;
		}
		rs.close();
		return res;
	}

	/**
	 * Check if chromosome table has entries.
	 * @param idChromosome
	 * @param stmt
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkChromosomeData(String idChromosome, Statement stmt) throws SQLException{

		boolean next = false;

		ResultSet rs = stmt.executeQuery("SELECT * FROM chromosome WHERE idchromosome = "+idChromosome);

		if(!rs.next())
			next = true;

		rs.close();
		return next;
	}

	/**
	 * Get reaction ID.
	 * @param proteinID
	 * @param e
	 * @param stmt
	 * @return Set<String>
	 * @throws SQLException
	 */
	public static Set<String> getReactionID(String proteinID, String e, Statement stmt) throws SQLException{

		Set<String> reactionsIDs = new HashSet<String>();

		ResultSet rs = stmt.executeQuery("SELECT DISTINCT idreaction FROM reaction " +
				"INNER JOIN reaction_has_enzyme ON reaction_has_enzyme.reaction_idreaction = idreaction " +
				"INNER JOIN pathway_has_enzyme ON pathway_has_enzyme.enzyme_protein_idprotein = reaction_has_enzyme.enzyme_protein_idprotein  " +
				"INNER JOIN pathway_has_reaction ON pathway_has_enzyme.pathway_idpathway = pathway_has_reaction.pathway_idpathway  " +
				"WHERE pathway_has_reaction.reaction_idreaction = idreaction " +
				"AND reaction_has_enzyme.enzyme_protein_idprotein = "+ proteinID +" " +
				"AND reaction_has_enzyme.enzyme_ecnumber = '"+e+"'");

		while(rs.next())
			reactionsIDs.add(rs.getString(1));

		rs.close();
		return reactionsIDs;
	}

	/**
	 * Get reaction ID.
	 * @param reactionIDs
	 * @param proteinID
	 * @param e
	 * @param stmt
	 * @return Set<String>
	 * @throws SQLException
	 */
	public static Set<String> getReactionID2(Set<String> reactionIDs, String proteinID, String e, Statement stmt) throws SQLException{

		Set<String> reactionsIDs = new HashSet<String>();

		ResultSet rs= stmt.executeQuery("SELECT idreaction FROM reactions_view_noPath_or_noEC " +
				"INNER JOIN reaction_has_enzyme ON reaction_has_enzyme.reaction_idreaction=idreaction " +
				"WHERE enzyme_protein_idprotein = "+proteinID+" AND enzyme_ecnumber = '"+e+"'");

		while(rs.next())
			reactionsIDs.add(rs.getString(1));

		rs.close();
		return reactionsIDs;
	}

	/**
	 * Check if chromosome table has entries for a given proteinID.
	 * @param idChromosome
	 * @param stmt
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkSubunitData(String id, Statement stmt) throws SQLException{

		boolean exists = false;

		ResultSet rs = stmt.executeQuery("SELECT * FROM subunit WHERE enzyme_protein_idprotein = " + id.split("__")[0]);

		if(!rs.next())
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 * Calculate number of proteins.
	 * @param stmt
	 * @return int[]
	 * @throws SQLException
	 */
	public static int[] countProteins(Statement stmt) throws SQLException{

		int[] res = new int[2];

		int num = 0;
		int noname = 0;

		ResultSet rs = stmt.executeQuery("SELECT * FROM protein");

		while(rs.next()) {

			num++;
			if(rs.getString("name")==null) {

				noname++;
			}
		}

		res[0] = num;
		res[1] = noname;

		rs.close();
		return res;
	}

	/**
	 * Calculate number of proteins synonyms.
	 * @param stmt
	 * @return double
	 * @throws SQLException
	 */
	public static int countProteinsSynonyms(Statement stmt) throws SQLException{

		int snumproteins = 0;

		ResultSet rs = stmt.executeQuery("SELECT count(*) FROM aliases where class = 'p'");

		if(rs.next())
			snumproteins = rs.getInt(1);

		rs.close();
		return snumproteins;
	}

	/**
	 * Calculate number of proteins that are enzymes.
	 * @param stmt
	 * @return String
	 * @throws SQLException
	 */
	public static String countProteinsEnzymes(Statement stmt) throws SQLException{

		String num = "";

		ResultSet rs = stmt.executeQuery("SELECT count(distinct(protein_idprotein)) FROM enzyme WHERE source NOT LIKE 'TRANSPORTERS'");

		if(rs.next())
			num = rs.getString(1);

		rs.close();
		return num;
	}

	/**
	 * Calculate number of proteins that are Transporters.
	 * @param stmt
	 * @return String
	 * @throws SQLException
	 */
	public static String countProteinsTransporters(Statement stmt) throws SQLException{

		String num = "";

		ResultSet rs = stmt.executeQuery("SELECT count(distinct(protein_idprotein)) FROM enzyme WHERE source LIKE 'TRANSPORTERS'");

		if(rs.next())
			num = rs.getString(1);

		rs.close();
		return num;
	}

	/**
	 * Calculate number of proteins that are complexes.
	 * @param stmt
	 * @return String
	 * @throws SQLException
	 */
	public static String countProteinsComplexes(Statement stmt) throws SQLException{

		String num = "";

		ResultSet rs = stmt.executeQuery("SELECT count(distinct(protein_idprotein)) FROM protein_composition");

		if(rs.next())
			num = rs.getString(1);

		rs.close();
		return num;
	}

	/**
	 * Calculate number of proteins associated to genes.
	 * @param stmt
	 * @return int
	 * @throws SQLException
	 */
	public static int countProteinsAssociatedToGenes(Statement stmt) throws SQLException{

		int p_g = 0;

		ResultSet rs = stmt.executeQuery("SELECT DISTINCT enzyme_protein_idprotein, enzyme_ecnumber FROM subunit");

		while(rs.next()) 
			p_g++;

		rs.close();
		return p_g;
	}

	/**
	 * @param stmt
	 * @return HashMap<String,Integer>
	 * @throws SQLException
	 */
	public static HashMap<String,Integer> getProteins(Statement stmt) throws SQLException{

		HashMap<String,Integer> index = new HashMap<String,Integer>();

		ResultSet rs = stmt.executeQuery("SELECT subunit.enzyme_protein_idprotein " +
				"FROM gene JOIN subunit ON gene.idgene = subunit.gene_idgene");

		while(rs.next()) {

			if(!index.containsKey(rs.getString(1))) {

				index.put(rs.getString(1), new Integer(1));
			}
			else {

				Integer ne = new Integer(index.get(rs.getString(1)).intValue() + 1);
				index.put(rs.getString(1), ne);
			}
		}
		rs.close();
		return index;
	}

	/**
	 * Get all proteins.
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getAllProteins(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT idprotein, name, class FROM protein ORDER BY name");

		while(rs.next()){
			String[] list = new String[3];
			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * @param stmt
	 * @return ArrayList<String[]> 
	 * @throws SQLException
	 */
	public static ArrayList<String[]> loadData(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT protein_idprotein, promoter_idpromoter, ri_function_idri_function, " +
				"binding_site_position, protein.name, promoter.name, " +
				"symbol FROM regulatory_event JOIN protein ON protein_idprotein = " +
				"idprotein JOIN promoter ON promoter_idpromoter = idpromoter " +
				"JOIN ri_function ON ri_function_idri_function = idri_function " +
				"ORDER BY protein_idprotein,promoter_idpromoter");

		while(rs.next()){
			String[] list = new String[7];
			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);
			list[3]=rs.getString(4);
			list[4]=rs.getString(5);
			list[5]=rs.getString(6);
			list[6]=rs.getString(7);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * Get all data from promoters table.
	 * @param aux
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getAllFromPromoters(String aux, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT * FROM " + aux);

		while(rs.next()){
			String[] list = new String[4];

			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);
			list[3]=rs.getString(4);


			result.add(list);
		}
		rs.close();
		return result;
	}


	/**
	 * Get protein name from regulatory_event table for a given promoterID.
	 * @param id
	 * @param stmt
	 * @return ArrayList<String>
	 * @throws SQLException
	 */
	public static ArrayList<String> getProteinName(String id, Statement stmt) throws SQLException{

		ArrayList<String> ql = new ArrayList<String>();

		ResultSet rs = stmt.executeQuery("SELECT protein.name FROM regulatory_event JOIN protein ON idprotein = protein_idprotein " +
				"WHERE promoter_idpromoter = " + id);

		while(rs.next())
			ql.add(rs.getString(1));

		rs.close();
		return ql;
	}

	/**
	 * Get name from sigma_promoter table for a given promoterID.
	 * @param id
	 * @param stmt
	 * @return ArrayList<String>
	 * @throws SQLException
	 */
	public static ArrayList<String> getNameFromSigmaPromoterTable(String id, Statement stmt) throws SQLException{

		ArrayList<String> ql = new ArrayList<String>();

		ResultSet rs = stmt.executeQuery("SELECT distinct(protein_idprotein), name FROM sigma_promoter " +
				"JOIN protein ON idprotein = protein_idprotein where promoter_idpromoter = "+id);

		while(rs.next())
			ql.add(rs.getString(2));

		rs.close();
		return ql;
	}

	/**
	 * Get name from transcription_unit_promoter table for a given promoterID.
	 * @param id
	 * @param stmt
	 * @return ArrayList<String>
	 * @throws SQLException
	 */
	public static ArrayList<String> getNameFromTranscriptionUnitPromoterTable(String id, Statement stmt) throws SQLException{

		ArrayList<String> ql = new ArrayList<String>();

		ResultSet rs = stmt.executeQuery("SELECT name FROM transcription_unit_promoter JOIN transcription_unit ON " +
				"idtranscription_unit = transcription_unit_idtranscription_unit WHERE promoter_idpromoter = "+id);

		while(rs.next())
			ql.add(rs.getString(1));

		rs.close();
		return ql;
	}

	/**
	 * Get Sigma genes statistics.
	 * @param stmt
	 * @return Integer[]
	 * @throws SQLException
	 */
	public static Integer[] getSigmaGenesStats(Statement stmt) throws SQLException{

		Integer[] list = new Integer[5];

		int num=0;
		int noseq=0;
		int noname=0;
		int nobnumber=0;
		int nboolean_rule=0;

		ResultSet rs = stmt.executeQuery("SELECT distinct(idgene), gene.name, gene.sequence_idSequence, transcription_direction, boolean_rule "
				+ "FROM gene JOIN subunit ON gene.idgene = subunit.gene_idgene JOIN protein "
				+ "ON protein.idprotein = subunit.protein_idprotein JOIN sigma_promoter "
				+ "ON protein.idprotein = sigma_promoter.protein_idprotein");

		while(rs.next()){
			num++;
			if(rs.getString(2)==null) noseq++;
			if(rs.getString(3)==null) noname++;
			if(rs.getString(4)==null) nobnumber++;
			if(rs.getString(5)==null) nboolean_rule++;
		}

		list[0] = num;
		list[1] = noseq;
		list[2] = noname;
		list[3] = nobnumber;
		list[4] = nboolean_rule;

		rs.close();
		return list;
	}

	/**
	 * Get all genes.
	 * @param stmt
	 * @return ArrayList<String[]> 
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getAllGenes(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT distinct(idgene), gene.name, gene.sequence_idSequence, transcription_direction, boolean_rule "
				+ "FROM gene JOIN subunit ON gene.idgene = subunit.gene_idgene JOIN protein "
				+ "ON protein.idprotein = subunit.protein_idprotein JOIN sigma_promoter "
				+ "ON protein.idprotein = sigma_promoter.protein_idprotein");

		while(rs.next()){
			String[] list = new String[5];

			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);
			list[3]=rs.getString(4);
			list[3]=rs.getString(5);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * Get all from TF
	 * @param table
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getAllFromTF(String table, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT * FROM " + table);

		while(rs.next()){
			String[] list = new String[2];

			list[0]=rs.getString(1);
			list[1]=rs.getString(2);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * Count number of TUs.
	 * @param aux
	 * @param stmt
	 * @return Integer[]
	 * @throws SQLException
	 */
	public static Integer[] countTUs(String aux, Statement stmt) throws SQLException{

		Integer[] list = new Integer[2];

		int num = 0;
		int noname = 0;

		ResultSet rs = stmt.executeQuery("SELECT * FROM " + aux);

		while (rs.next()) {
			num++;
			if (rs.getString(2) == null)
				noname++;
		}

		list[0] = num;
		list[1] = noname;

		rs.close();
		return list;
	}

	/**
	 * Count genes associated with TUs.
	 * @param stmt
	 * @return String
	 * @throws SQLException
	 */
	public static String countGenesAssociatedWithTUs(Statement stmt) throws SQLException{

		String snumgenes = "";

		ResultSet rs = stmt.executeQuery("SELECT count(distinct(gene_idgene)) FROM transcription_unit JOIN "
				+ "transcription_unit_gene ON "
				+ "transcription_unit.idtranscription_unit = "
				+ "transcription_unit_gene.transcription_unit_idtranscription_unit");

		if(rs.next())
			snumgenes = rs.getString(1);

		rs.close();
		return snumgenes;
	}

	/**
	 * Count genes associated with TUs.
	 * @param stmt
	 * @return String
	 * @throws SQLException
	 */
	public static String countTUsWithGenesAssociated(Statement stmt) throws SQLException{

		String snumtus = "";

		ResultSet rs = stmt.executeQuery("SELECT count(distinct(transcription_unit.idtranscription_unit)) "
				+ "FROM transcription_unit JOIN transcription_unit_gene "
				+ "ON transcription_unit.idtranscription_unit = "
				+ "transcription_unit_gene.transcription_unit_idtranscription_unit");

		if(rs.next())
			snumtus = rs.getString(1);

		rs.close();
		return snumtus;
	}

	/**
	 * Get average number of promoters by TU
	 * @param int
	 * @param stmt
	 * @return double
	 * @throws SQLException
	 */
	public static double getAvarageNumberOfPromotersByTU(int num, Statement stmt) throws SQLException{

		double promoters_by_tus = 0.0;

		ResultSet rs = stmt.executeQuery("SELECT count(distinct(promoter_idpromoter)) FROM transcription_unit_promoter");

		if(rs.next())
			promoters_by_tus = (new Double(rs.getString(1)).doubleValue()) / (new Double(num).doubleValue());

		rs.close();
		return promoters_by_tus;
	}

	/**
	 * Get average number of genes by TU.
	 * @param stmt
	 * @return int
	 * @throws SQLException
	 */
	public static int getAvarageNumberOfGenesByTU(Statement stmt) throws SQLException{

		int gens_tu = 0;

		ResultSet rs = stmt.executeQuery("SELECT count(distinct(gene_idgene)) FROM transcription_unit_gene");

		if(rs.next())
			gens_tu = new Integer(rs.getString(1)).intValue();

		rs.close();
		return gens_tu;
	}

	/**
	 * Get all data from TU table.
	 * @param table
	 * @param stmt
	 * @return ArrayList<String[]> 
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getAllFromTU(String table, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT * FROM " + table);

		while(rs.next()){
			String[] list = new String[2];

			list[0]=rs.getString(1);
			list[1]=rs.getString(2);

			result.add(list);
		}

		rs.close();
		return result;
	}

	/**
	 * Check protein_composition existence.
	 * @param stmt
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkTables(Statement stmt) throws SQLException{

		boolean go = false;

		ResultSet rs = stmt.executeQuery("SHOW tables;");

		while(rs.next())
		{
			if(rs.getString(1).equalsIgnoreCase("protein_composition"))
			{
				go=true;
			}
		}

		rs.close();
		return go;
	}

	/**
	 * Get all data from protein_composition table.
	 * @param table
	 * @param stmt
	 * @return ArrayList<String[]> 
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getAllFromProteinComposition(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT * FROM protein_composition;");

		while(rs.next()){
			String[] list = new String[2];

			list[0]=rs.getString(1);
			list[1]=rs.getString(2);

			result.add(list);
		}

		rs.close();
		return result;
	}

	/**
	 * Get pathawaysIDs for a given reactionID.
	 * @param idReaction
	 * @param stmt
	 * @return List<String>
	 * @throws SQLException
	 */
	public static List<String> getPathwaysIDsByReactionID(String idReaction, Statement statement) throws SQLException{

		List<String> pathwayID = new ArrayList<String>();

		ResultSet rs = statement.executeQuery("SELECT pathway_idpathway FROM pathway_has_reaction WHERE reaction_idreaction = "+idReaction+" ;");

		while (rs.next())
			pathwayID.add(rs.getString(1));

		rs.close();
		return pathwayID;
	}

	/**
	 * Get reactionID and PathwayID "WHERE source = 'TRANSPORTERS' AND originalReaction;".
	 * @param statement
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getReactionIdAndPathwayID(Statement statement) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = statement.executeQuery("SELECT reaction_idreaction, pathway_idpathway FROM pathway_has_reaction"+
				"  INNER JOIN reaction ON pathway_has_reaction.reaction_idreaction = reaction.idreaction " +
				" WHERE source = 'TRANSPORTERS' AND originalReaction;");

		while(rs.next()){
			String[] list = new String[2];

			list[0]=rs.getString(1);
			list[1]=rs.getString(2);

			result.add(list);
		}

		rs.close();
		return result;
	}

	/**
	 * Get reactions.
	 * @param conditions
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getReactions(String conditions, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();	

		ResultSet rs = stmt.executeQuery("SELECT DISTINCT idreaction, name, equation, reversible, compartment_idcompartment, "
				+ "notes, lowerBound, upperBound, boolean_rule " +
				"FROM reaction WHERE inModel AND " +conditions );

		while(rs.next()){
			String[] list = new String[9];

			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);
			list[3]=rs.getBoolean(4)+"";
			list[4]=rs.getString(5);
			list[5]=rs.getString(6);
			list[6]=rs.getString(7);
			list[7]=rs.getString(8);
			list[8]=rs.getString(9);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * Get all data from table.
	 * @param conditions
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static Pair<ArrayList<String[]>, Integer> getAllFromTable(String table, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();	

		ResultSet rs = stmt.executeQuery("SELECT * FROM "+table);

		int ncols = rs.getMetaData().getColumnCount();


		while(rs.next()){
			String[] list = new String[ncols];
			for(int i=0;i<ncols;i++)
				list[i]=rs.getString(i+1);

			result.add(list);
		}

		Pair<ArrayList<String[]>, Integer> data = new Pair<>(result, ncols);

		rs.close();
		return data;
	}

	/**
	 * Count all entries of a given table.
	 * @param table
	 * @param stmt
	 * @return String
	 * @throws SQLException
	 */
	public static String countTableEntries(String table, Statement stmt) throws SQLException{

		String result = "";

		ResultSet rs = stmt.executeQuery("SELECT count(*) FROM "+table+";");
		if(rs.next())
			result = rs.getString(1);
		rs.close();
		return result;
	}

	/**
	 * Get ecNumbers for a given pathwayID.
	 * @param idPathway
	 * @param stmt
	 * @return Set<String>
	 * @throws SQLException
	 */
	public static Set<String> getEcNumberByPathwayID(int idPathway, Statement stmt) throws SQLException{

		Set<String> ecnumbers = new TreeSet<String>();

		ResultSet rs = stmt.executeQuery("SELECT DISTINCT enzyme_ecnumber FROM pathway_has_enzyme "
				+ "WHERE pathway_idpathway = '" + idPathway+ "'");

		while(rs.next())
			ecnumbers.add(rs.getString(1));

		rs.close();
		return ecnumbers;
	}

	/**
	 * Get reactionIDs for a given pathwayID.
	 * @param idPathway
	 * @param stmt
	 * @return Set<String>
	 * @throws SQLException
	 */
	public static Set<String> getReactionIdByPathwayID(int idPathway, Statement stmt) throws SQLException{

		Set<String> reactions = new TreeSet<String>();

		ResultSet rs = stmt.executeQuery("SELECT DISTINCT reaction_idreaction "
				+ "FROM pathway_has_reaction WHERE pathway_idpathway = '" + idPathway+ "'");

		while(rs.next())
			reactions.add(rs.getString(1));

		rs.close();
		return reactions;
	}

	/**
	 * Check if an entry exists in pathway_has_enzyme table for a given ecNumber.
	 * @param ecnumber
	 * @param stmt
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkPathwayHasEnzymeEntryByECnumber(String ecnumber, Statement stmt) throws SQLException{

		boolean exists = false;

		ResultSet rs = stmt.executeQuery("SELECT * FROM pathway_has_enzyme WHERE enzyme_ecnumber='"+ecnumber+"'");

		if(rs.next())
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 * Check if an entry exists in pathway_has_enzyme table for a given reactionID.
	 * @param reaction
	 * @param stmt
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkPathwayHasEnzymeEntryByReactionID(String reaction, Statement stmt) throws SQLException{

		boolean exists = false;

		ResultSet rs = stmt.executeQuery("SELECT * FROM pathway_has_reaction WHERE reaction_idreaction='"+reaction+"'");

		if(rs.next())
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 * Get data from reaction.
	 * @param query
	 * @param stmt
	 * @return Map<String, String> (idreaction: equation)
	 * @throws SQLException
	 */
	public static Map<String, String> getDataFromReaction(String query, Statement stmt) throws SQLException{

		Map<String, String> reactions = new HashMap<String,String>();

		ResultSet rs = stmt.executeQuery(query);
		if(rs.next())
			reactions.put(rs.getString("idreaction"), rs.getString("equation"));

		rs.close();
		return reactions;
	}

	/**
	 * Get s_key from organism table for a given organism.
	 * @param aux
	 * @param statement
	 * @return String
	 * @throws SQLException
	 */
	public static int getSKeyFromOrganism(String aux, Statement statement) throws SQLException{

		int res = -1;

		ResultSet rs = statement.executeQuery("SELECT * FROM organism where organism = '"+ aux);


		if(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 * Check if fastaSequence table has entries for a given geneHomology_s_key.
	 * @param query
	 * @param statement
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkfastaSequenceBySkey(int geneHomology_s_key, Statement statement) throws SQLException{

		boolean exists = false;

		ResultSet rs = statement.executeQuery("SELECT * FROM fastaSequence WHERE geneHomology_s_key = '"+geneHomology_s_key+"'");

		if(rs.next())
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 *  Get s_key from ecNumber table for a given ecNumber.
	 * @param query
	 * @param statement
	 * @return boolean
	 * @throws SQLException
	 */
	public static int getecNumberSkey(String ecNumber, Statement statement) throws SQLException{

		int res = -1;

		ResultSet rs = statement.executeQuery("SELECT * FROM ecNumber WHERE ecNumber = '"+ecNumber+"'");

		if(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 *  Get s_key from productRank table for a given geneHomology_s_key, productName and rank.
	 * @param query
	 * @param statement
	 * @return boolean
	 * @throws SQLException
	 */
	public static int getProductRankSkey(int geneHomology_s_key, String aux, int aux2, Statement statement) throws SQLException{

		int res = -1;

		ResultSet rs = statement.executeQuery("SELECT * FROM productRank WHERE geneHomology_s_key = "
				+ geneHomology_s_key +" AND productName = '"+ aux +"' AND rank = '"+ aux2 +"'");

		if(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 *  Get s_key from productRank_has_organism table for a given productRank_s_key and organism_s_key.
	 * @param query
	 * @param statement
	 * @return boolean
	 * @throws SQLException
	 */
	public static int getProductRankHasOrganismSkey(int prodKey, int orgKey, Statement statement) throws SQLException{

		int res = -1;

		ResultSet rs = statement.executeQuery("SELECT * FROM productRank_has_organism WHERE productRank_s_key = '"
				+ prodKey +"' AND organism_s_key = '"+orgKey+"'");

		if(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 *  Get s_key from ecNumberRank table for a given geneHomology_s_key, ecNumber and rank.
	 * @param query
	 * @param statement
	 * @return boolean
	 * @throws SQLException
	 */
	public static int getEcNumberRankSkey(int geneHomology_s_key, String concatEC, int ecnumber, Statement statement) throws SQLException{

		int res = -1;

		ResultSet rs = statement.executeQuery("SELECT * FROM ecNumberRank " +
				"WHERE geneHomology_s_key = '"+geneHomology_s_key+
				"' AND ecNumber = '"+concatEC+"' AND rank = '"+ecnumber+"'");

		if(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 * Get chromosomeID and name.
	 * @param stmt
	 * @return ConcurrentHashMap<String,Integer>
	 * @throws SQLException
	 */
	public static ConcurrentHashMap<String,Integer> getChromosomeData(Statement stmt) throws SQLException{

		ConcurrentHashMap<String,Integer> map = new ConcurrentHashMap<String, Integer>();

		ResultSet rs = stmt.executeQuery("SELECT name, idchromosome FROM chromosome");

		while(rs.next())
			map.put(rs.getString(1), rs.getInt(2));

		rs.close();
		return map;
	}

	/**
	 * Get data from a given query and insert it into a map.
	 * @param query
	 * @param stmt
	 * @return ConcurrentHashMap<String,Integer
	 * @throws SQLException
	 */
	public static ConcurrentHashMap<String,Integer> databaseInitialData(String query, Statement stmt) throws SQLException{

		ConcurrentHashMap<String,Integer> map = new ConcurrentHashMap<String, Integer>();

		ResultSet rs = stmt.executeQuery(query);

		while(rs.next())
			map.put(rs.getString(1), rs.getInt(2));

		rs.close();
		return map;
	}

	/**
	 * Get data from a given query and insert it into a map.
	 * 
	 * @param query
	 * @param stmt
	 * @return ConcurrentHashMap<String,List<Integer>>
	 * @throws SQLException
	 */
	public static ConcurrentHashMap<String, List<Integer>> databaseInitialDataList(String query, Statement stmt) throws SQLException{

		ConcurrentHashMap<String,List<Integer>> map = new ConcurrentHashMap<>();

		ResultSet rs = stmt.executeQuery(query);

		while(rs.next()) {
			
			String key = rs.getString(1);
			int value = rs.getInt(2);
			
			List<Integer> l = new ArrayList<>();
			
			if(map.containsKey(key))
				l = map.get(key);
			
			l.add(value);
			
			map.put(key, l);
		}

		rs.close();
		return map;
	}
	
	/**
	 * Check if an internalID exists for a given internal_id and external_database and class.
	 * 
	 * @param geneID
	 * @param database
	 * @param stmt
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkInternalIdFromDblinks(String cl, int internal, String database, Statement stmt) throws SQLException{

		boolean exists = false;

		ResultSet rs = stmt.executeQuery("SELECT internal_id FROM dblinks WHERE class= '"+ cl +"' AND internal_id="+internal+" AND external_database='"+database+"'");

		if(rs.next())
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 * Check if an entity exists for a given entity and alias and class.
	 * @param geneID
	 * @param aux
	 * @param stmt
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkEntityFromAliases(String cl, int entity, String aux, Statement stmt) throws SQLException{

		boolean exists = false;

		ResultSet rs = stmt.executeQuery("SELECT entity FROM aliases WHERE class='"+ cl + "' AND entity="+entity+" AND alias='" + aux +"'");

		if(rs.next())
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 * Check if an gene_idgene exists for a given gene_idgene and type.
	 * @param geneID
	 * @param type
	 * @param stmt
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkGeneIDFromSequence(int geneID, String type, Statement stmt) throws SQLException{

		boolean exists = false;

		ResultSet rs = stmt.executeQuery("SELECT gene_idgene FROM sequence WHERE gene_idgene="+geneID+" AND sequence_type='"+ type +"'");

		if(rs.next())
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 * Check if an entry exists for a given gene_idgene and orthology_id.
	 * @param geneID
	 * @param type
	 * @param stmt
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkGeneHasOrthologyEntries(int geneID, int aux, Statement stmt) throws SQLException{

		boolean exists = false;

		ResultSet rs = stmt.executeQuery("SELECT * FROM gene_has_orthology WHERE gene_idgene="+geneID+" AND orthology_id="+aux);

		if(rs.next())
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 * Check data from same_as table for a given metabolite_id and similar_metabolite_id.
	 * @param metaboliteID
	 * @param aux
	 * @param stmt
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkDataFromSameAs(int metaboliteID, int aux, Statement stmt) throws SQLException{

		boolean exists = false;

		ResultSet rs = stmt.executeQuery("SELECT metabolite_id, similar_metabolite_id FROM same_as WHERE metabolite_id="+metaboliteID+
				" AND similar_metabolite_id="+ aux);

		if(rs.next())
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 * Get geneID from gene table for a given locusTag.
	 * @param query
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int getGeneID(String gene, Statement stmt) throws SQLException{

		int res = -1;

		ResultSet rs = stmt.executeQuery("SELECT idgene from gene WHERE locusTag='"+gene+"'");

		if(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 * Check modules.
	 * @param gene
	 * @param protein_id
	 * @param aux
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static boolean checkModules(int gene, int protein_id, String aux, Statement stmt) throws SQLException{

		boolean exists = false;

		ResultSet rs = stmt.executeQuery("SELECT enzyme_protein_idprotein from subunit WHERE enzyme_protein_idprotein="+ protein_id +" " +
				"AND gene_idgene="+ gene +" AND enzyme_ecnumber='"+ aux +"'");

		if(rs.next())
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 * Get idcompound from compound table for a given kegg_id.
	 * @param query
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int getCompoundIDbyKeggID(String kegg, Statement stmt) throws SQLException{

		int res = -1;

		ResultSet rs = stmt.executeQuery("SELECT idcompound from compound WHERE kegg_id='"+kegg+"'");

		if(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 * Check ProteinID from enzymatic_cofactor table for a given protein_idprotein and compound_idcompound.
	 * @param cofactor_string
	 * @param protein_id
	 * @param stmt
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkProteinIdFromEnzymaticCofactor(int cofactor_string, int protein_id, Statement stmt) throws SQLException{

		boolean exists = false;

		ResultSet rs = stmt.executeQuery("SELECT protein_idprotein from enzymatic_cofactor WHERE protein_idprotein="+protein_id
				+ "AND compound_idcompound="+cofactor_string);


		if(rs.next())
			exists = true;

		rs.close();
		return exists;
	}
	
	/**
	 * Get idprotein from protein table for a given EC number.
	 * @param query
	 * @param statement
	 * @return String
	 * @throws SQLException
	 */
	public static int getProteinIDFromEC_Number(String ecNumber, Statement stmt) throws SQLException{

		int res = -1;

		ResultSet rs = stmt.executeQuery("SELECT idprotein FROM protein WHERE ecnumber='"+ ecNumber +"'");

		if(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}
	
	/**
	 * Get idprotein from protein table for a given EC number.
	 * @param query
	 * @param statement
	 * @return String
	 * @throws SQLException
	 */
	public static int getProteinIDFromName(String name, Statement stmt) throws SQLException{

		int res = -1;

		ResultSet rs = stmt.executeQuery("SELECT idprotein FROM protein WHERE name = '"+ name +"'");

		if(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 * Get idprotein from protein table for a given name and class.
	 * @param query
	 * @param statement
	 * @return String
	 * @throws SQLException
	 */
	public static int getProteinIDFromProtein(String enzyme, Statement stmt) throws SQLException{

		int res = -1;

		ResultSet rs = stmt.executeQuery("SELECT idprotein FROM protein WHERE name='-' AND class='"+ enzyme +"'");

		if(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/** 
	 * Check enzyme table data for a given protein_idprotein and ecnumber.
	 * @param id
	 * @param ec
	 * @param stmt
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkEnzymeData(int id, String ec, Statement stmt) throws SQLException{

		boolean exists = false;

		ResultSet rs = stmt.executeQuery("SELECT * FROM enzyme WHERE protein_idprotein="+ id +" AND ecnumber='"+ ec +"'");

		if(rs.next())
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 * Get data from pathway table for a given name and code.
	 * @param name
	 * @param code
	 * @param stmt
	 * @return String
	 * @throws SQLException
	 */
	public static int getPathwayData(String name, String code, Statement stmt) throws SQLException{

		int res = -1;

		ResultSet rs = stmt.executeQuery("SELECT * FROM pathway WHERE name='"+ name +"' AND code='"+ code +"'");

		if(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 * Retrieve data of a given query.
	 * @param query
	 * @param stmt
	 * @return String
	 * @throws SQLException
	 */
	public static int getData(String query, Statement stmt) throws SQLException{

		int res = -1;

		ResultSet rs = stmt.executeQuery(query);

		if(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/** 
	 * Check module_has_orthology table data for a given module_id and orthology_id.
	 * @param id
	 * @param ec
	 * @param stmt
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkModuleHasOrthology(int moduleID, int orthologueID, Statement stmt) throws SQLException{

		boolean exists = false;

		ResultSet rs = stmt.executeQuery("SELECT * FROM module_has_orthology WHERE module_id="+moduleID+" AND orthology_id="+orthologueID);

		if(rs.next())
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 * Get data from superpathway table for a given pathway_idpathway and superpathway.
	 * @param intermediary_pathway_id
	 * @param super_pathway_id
	 * @param statement
	 * @return String
	 * @throws SQLException
	 */
	public static int getSuperPathwayData(int intermediary_pathway_id, int super_pathway_id, Statement statement) throws SQLException{

		int res = -1;

		ResultSet rs = statement.executeQuery("SELECT * FROM superpathway WHERE pathway_idpathway="+intermediary_pathway_id+" AND superpathway="+super_pathway_id);

		if(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 * Check pathway_has_reaction data for a given reaction_idreaction and pathway_idpathway.
	 * @param idreaction
	 * @param pathwayID
	 * @param stmt
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkPathwayHasReactionData(int idreaction, int pathwayID, Statement stmt) throws SQLException{

		boolean exists = false;

		ResultSet rs = stmt.executeQuery("SELECT reaction_idreaction FROM pathway_has_reaction "
				+ "WHERE reaction_idreaction="+idreaction+" AND pathway_idpathway="+pathwayID);

		if(rs.next())
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 * Check pathway_has_enzyme data for a given enzyme_protein_idprotein and pathway_idpathway.
	 * 
	 * @param proteinID
	 * @param pathwayID
	 * @param stmt
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkPathwayHasEnzymeData(int proteinID, int pathwayID, Statement stmt) throws SQLException{

		boolean exists = false;

		ResultSet rs = stmt.executeQuery("SELECT * FROM pathway_has_enzyme WHERE enzyme_protein_idprotein="+ proteinID +" AND pathway_idpathway="+ pathwayID);

		if(rs.next())
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 * Check pathway_has_module data for a given module_id and pathway_idpathway.
	 * @param moduleID
	 * @param pathwayID
	 * @param stmt
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkPathwayHasModuleData(int moduleID, int pathwayID, Statement stmt) throws SQLException{

		boolean exists = false;

		ResultSet rs = stmt.executeQuery("SELECT module_id FROM pathway_has_module "
				+ "WHERE module_id="+moduleID+" and pathway_idpathway="+pathwayID);

		if(rs.next())
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 * Check pathway_has_compound data for a given compound_idcompound and pathway_idpathway.
	 * @param metaboliteID
	 * @param pathwayID
	 * @param stmt
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkPathwayHasCompoundData(int metaboliteID, int pathwayID, Statement stmt) throws SQLException{

		boolean exists = false;

		ResultSet rs = stmt.executeQuery("SELECT compound_idcompound FROM pathway_has_compound "
				+ "WHERE compound_idcompound="+metaboliteID+" and pathway_idpathway="+ pathwayID);

		if(rs.next())
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 * Count gene_idgene in gene_has_compartment table.
	 * @param stmt
	 * @return int 
	 * @throws SQLException
	 */
	public static int countGenesInGeneHasCompartment(Statement stmt) throws SQLException{

		int res = 0;

		ResultSet rs = stmt.executeQuery("SELECT COUNT(DISTINCT(gene_idgene)) FROM gene_has_compartment;");
		if(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 * Count idgene in gene table.
	 * @param stmt
	 * @return int 
	 * @throws SQLException
	 */
	public static int countGenes(Statement stmt) throws SQLException{

		int res = 0;

		ResultSet rs = stmt.executeQuery("SELECT COUNT(idgene) FROM gene;");

		if(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 * Check database metadata.
	 * @param stmt
	 * @return boolean 
	 * @throws SQLException
	 */
	public static boolean checkDatabaseMetadata(DatabaseMetaData metadata) throws SQLException{

		boolean exists = false;

		ResultSet rs = metadata.getColumns(null, null, "projects", "compartments_tool");

		if(rs.next())
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 * Check if all databases are up-to-date
	 * 
	 * @param data
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static Set<Integer> checkIfUpdated(Map<Integer, String> data, Statement statement) throws SQLException{

		Set<Integer> forUpdate = new HashSet<>();

		for (int id : data.keySet()){
			if(!forUpdate.contains(id))
				forUpdate.add(id);
		}

		ResultSet rs = statement.executeQuery("SELECT * FROM updates");

		while(rs.next()){
			if(forUpdate.contains(rs.getInt(1)))
				forUpdate.remove(rs.getInt(1));
		}

		rs.close();
		return forUpdate;
	}

	/**
	 * Method for execute queries read from the "updates" file in the folder conf
	 * 
	 * @param forUpdate
	 * @param data
	 * @param statement
	 * @throws SQLException
	 */
	public static void update(Set<Integer> forUpdate, Map<Integer, String> data, Statement statement) throws SQLException{

		long time = System.currentTimeMillis();
		Timestamp timestamp = new Timestamp(time);

		for(int code : forUpdate){
			statement.execute(data.get(code));
			statement.execute("INSERT INTO updates(id , date) values (" + code + ", '" + timestamp + "');");
		}
	}

	/**
	 * Check if table updates already exists, if not, it will be created with columns 'id' and 'date'
	 * 
	 * @param table
	 * @param statement
	 * @throws SQLException
	 */
	public static void checkIfTableUpdatesExists(Statement statement) throws SQLException{

		statement.execute("CREATE TABLE IF NOT EXISTS `updates` ( `id` INT NOT NULL, `date` DATETIME NOT NULL, PRIMARY KEY (`id`));");

	}

	/**
	 * Method to retrieve information about the organism from the table projects
	 * 
	 * @param taxonomyID
	 * @param statement
	 * @return
	 * @throws SQLException 
	 */
	public static String[] getOrganismData(long taxonomyID, Statement statement) throws SQLException{

		String[] res = new String[2];

		ResultSet rs = statement.executeQuery("SELECT organism_name, organism_lineage FROM projects WHERE organism_id = " + taxonomyID +";"); 

//		System.out.println(rs.getString(1));
//		System.out.println(rs.getString(2));
		
		if(rs.next() && !rs.getString(1).equalsIgnoreCase("-1")){

			res[0] = rs.getString(1);
			res[1] = rs.getString(2);
			return res;
		}

		rs.close();
		return null;

	}

	/**
	 * Update organism name and lineage in the table projects
	 * 
	 * @param taxonomyID
	 * @param data
	 * @param statement
	 * @throws SQLException
	 */
	public static void updateOrganismData(long taxonomyID, String[] data, Statement statement) throws SQLException{

		if(data != null)
			statement.execute("UPDATE projects SET organism_name = '" + data[0] + "', organism_lineage = '" + data[1] + "' WHERE organism_id = " + taxonomyID +";"); 

	}


	/**
	 * Method to drop table 'updates'.
	 * 
	 * @param taxonomyID
	 * @param data
	 * @param statement
	 * @throws SQLException
	 */
	public static void dropTableUpdates(Statement statement) throws SQLException{

		statement.execute("DROP TABLE updates");

	}

	/**
	 * Method to retrieve all information about the organism from the table projects
	 * 
	 * @param taxonomyID
	 * @param statement
	 * @return
	 * @throws SQLException 
	 */
	public static Object[] getAllOrganismData(long taxonomyID, Statement statement) throws SQLException{

		Object[] res = new String[5];

		ResultSet rs = statement.executeQuery("SELECT * FROM projects WHERE organism_id = " + taxonomyID +";"); 

		if(rs.next()){

			res[0] = rs.getString(2);
			res[1] = rs.getString(3);
			res[2] = rs.getString(5);
			res[3] = rs.getString(6);
			res[4] = rs.getString(7);
			
			return res;
		}

		rs.close();
		return null;

	}

	/**
	 * Method to insert data into projects table.
	 * 
	 * @param data
	 * @param statement
	 * @throws SQLException
	 */
	public static void setOrganismData(Object[] data, Statement statement) throws SQLException{

		long time = System.currentTimeMillis();
		Timestamp timestamp = new Timestamp(time);
		
		statement.execute("INSERT INTO projects(organism_id, latest_version, date, version, organism_name, organism_lineage) "
				+ "values(" + data[0] + ", " + data[1] + ", '" + timestamp + "', " + data[2] +", '" + data[3] + "', '" + data[4] + "');");
		
	}
	
	/**
	 * @param statement
	 * @throws SQLException
	 */
	public static void restoreOrganismColumns(Statement statement) throws SQLException{

		statement.execute("ALTER TABLE `projects` ADD COLUMN `organism_name` VARCHAR(200) NULL DEFAULT '-1' AFTER `version`;");
		statement.execute("ALTER TABLE `projects` ADD COLUMN `organism_lineage` VARCHAR(1000) NULL DEFAULT '-1' AFTER `organism_name`;");

	}
	
	/**
	 * @param statement
	 * @throws SQLException
	 */
	public static void restoreColumnCompartmentsTools(Statement statement) throws SQLException{

		statement.execute("ALTER TABLE `projects` ADD `compartments_tool` VARCHAR(60);");

	}

}
