package pt.uminho.sysbio.common.database.connector.databaseAPI;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author davidelagoa
 *
 */
public class CompartmentsAPI {
	
	/**
	 * Initialize compartments if missing.
	 * @param compartments
	 * @param statement
	 * @throws SQLException
	 */
	public static void initCompartments(Map<String, String> compartments, Statement statement) throws SQLException{
		
		ResultSet rs = null;
		
		for(String abbreviation:compartments.keySet()) {
			
			rs = statement.executeQuery("Select * FROM compartments WHERE name='"+compartments.get(abbreviation)+"'");
			
			if(!rs.next())
				statement.execute("INSERT INTO compartments (name,abbreviation) VALUES('"+compartments.get(abbreviation)+"', '"+abbreviation.toUpperCase()+"')");
		}
		
		rs.close();
		
	}

	/**
	 * Get the number of reactants in compartment.
	 * @param aux
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getReactantsInCompartment(String aux, Statement stmt) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = stmt.executeQuery("SELECT compartment.name, count(distinct(compound_idcompound)) " +
				"FROM stoichiometry " +
				"JOIN compartment ON compartment.idcompartment =  stoichiometry.compartment_idcompartment " +
				"INNER JOIN reaction ON (reaction.idreaction=reaction_idreaction) "+
				aux+ " AND stoichiometric_coefficient REGEXP '(^-)' " +
				"GROUP BY compartment.name");
		
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
	 * Get the number of reactants in compartment.
	 * @param aux
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getProductsInCompartment(String aux, Statement stmt) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = stmt.executeQuery("SELECT compartment.name, count(distinct(compound_idcompound)) " +
				"FROM stoichiometry " +
				"JOIN compartment ON compartment.idcompartment =  stoichiometry.compartment_idcompartment " +
				"INNER JOIN reaction ON (reaction.idreaction=reaction_idreaction) "+
				aux+ " AND  stoichiometric_coefficient NOT REGEXP '(^-)' " +
				"GROUP BY compartment.name");
		
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
	 * @param localisation
	 * @param statement
	 * @return String
	 * @throws SQLException
	 */
	public static int getCompartmentID(String localisation, Statement statement) throws SQLException{
		
		int idCompartment = -1;
		
		ResultSet rs = statement.executeQuery("SELECT idcompartment FROM compartment WHERE name = '" + localisation + "'");
		
		if(rs.next())
			idCompartment = rs.getInt(1);
		
		rs.close();
		return idCompartment;
	}
	
	/**
	 * Get compartments.
	 * @param isMetabolites
	 * @param isCompartmentalisedModel
	 * @param statement
	 * @return ArrayList<String>
	 * @throws SQLException
	 */
	public static ArrayList<String> getCompartments(boolean isMetabolites, boolean isCompartmentalisedModel, Statement statement) throws SQLException{
	
		ArrayList<String> cls = new ArrayList<String>();
		
		ResultSet rs = statement.executeQuery("SELECT idcompartment, name FROM compartment");

		while(rs.next()) {

			boolean addCompartment = true; 

			if(isCompartmentalisedModel && 
					(rs.getString(2).contains("inside") || rs.getString(2).contains("ouside")))
				addCompartment = false;

			if(isMetabolites && rs.getString(2).contains("membrane"))
				addCompartment = false;

			if(addCompartment)
				cls.add(rs.getString(2));
		}
		rs.close();
		return cls;
	}
	
	/**
	 * Get comparment abbreviation
	 * @param interior
	 * @param statement
	 * @return String
	 * @throws SQLException
	 */
	public static String getCompartmentAbbreviation(String interior, Statement statement) throws SQLException{
		
		ResultSet rs = statement.executeQuery("SELECT idcompartment, abbreviation FROM compartment;");
		
		while(rs.next()) {


			if( rs.getString(2).equalsIgnoreCase("cyto")) {

				interior = "cyto";
			}

			if( rs.getString(2).equalsIgnoreCase("cytop")) {

				interior = "cytop";
			}
		}
		
		rs.close();
		return interior;
	}
	
	/**
	 * Get compartmentIDs and ecNumbers.
	 * @param statement
	 * @return Map<String, List<String>>
	 * @throws SQLException
	 */
	public static Map<String, List<Integer>> getEnzymesCompartments(Statement statement) throws SQLException{
		
		Map<String, List<Integer>> enzymesCompartments = new HashMap<>();
		List<Integer> compartments;
		
		ResultSet rs = statement.executeQuery("SELECT DISTINCT compartment_idcompartment, enzyme_ecnumber, enzyme_protein_idprotein " +
				" FROM subunit " +
				" INNER JOIN gene_has_compartment ON subunit.gene_idgene = gene_has_compartment.gene_idgene " +
				" ORDER BY enzyme_ecnumber;");

		while(rs.next()) {

			compartments = new ArrayList<>();

			if(enzymesCompartments.containsKey(rs.getString(2)))
				compartments = enzymesCompartments.get(rs.getString(2));	

			compartments.add(rs.getInt(1));
			enzymesCompartments.put(rs.getString(2),compartments);
		}
		
		rs.close();
		return enzymesCompartments;
	}
	
	/**
	 * Get compartmentIDs and ecNumbers.
	 * @param statement
	 * @return Map<String, List<String>>
	 * @throws SQLException
	 */
	public static Map<String, List<Integer>> getTransportProteinsCompartments(Statement statement) throws SQLException{
		
		Map<String, List<Integer>> transportProteinsCompartments = new HashMap<>();
		List<Integer> compartments;
		
		ResultSet rs = statement.executeQuery("SELECT DISTINCT compartment_idcompartment, enzyme_ecnumber, enzyme_protein_idprotein FROM subunit " +
				"INNER JOIN gene_has_compartment ON subunit.gene_idgene = gene_has_compartment.gene_idgene " +
				"ORDER BY enzyme_ecnumber;");

		while(rs.next()) {

			String key = rs.getString(2).concat("_").concat(rs.getString(3));
			compartments = new ArrayList<>();

			if(transportProteinsCompartments.containsKey(key))
				compartments = transportProteinsCompartments.get(key);	

			compartments.add(rs.getInt(1));
			transportProteinsCompartments.put(key,compartments);
		}
		
		rs.close();
		return transportProteinsCompartments;
	}
	
	/**
	 * Get compartments.
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getCompartments2(Statement stmt) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = stmt.executeQuery("SELECT idcompartment, name, abbreviation FROM compartment");
		
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
	 * Get compartment data for a given name.
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getCompartmentDataByName(String aux, Statement stmt) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = stmt.executeQuery("SELECT idcompartment, name, abbreviation FROM compartment "+aux);
		
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
	 * Calculate total number of compartments.
	 * @param stmt
	 * @return int
	 * @throws SQLException
	 */
	public static int getNumberOfCompartments(Statement stmt) throws SQLException{
		
		int num_comp = 0;
		ResultSet 	rs = stmt.executeQuery("SELECT DISTINCT compartment_id FROM psort_reports_has_compartments;");
		
		while(rs.next())
			num_comp++;
		
		rs.close();
		return num_comp;
	}
	
	/**
	 * Select CompartmentID.
	 * @param compartment
	 * @param abbreviation
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static int selectCompartmentID(String compartment, String abbreviation, Statement stmt) throws SQLException{
		
		ResultSet rs = stmt.executeQuery("SELECT idcompartment FROM compartment WHERE "
				+ "name ='"+compartment+"' AND abbreviation ='"+abbreviation+"'");

		if(!rs.next()) {

			stmt.execute("INSERT INTO compartment(name, abbreviation) VALUES('"+compartment+"','"+abbreviation+"')");
			rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
			rs.next();
		}
		int id = rs.getInt(1);
		rs.close();
				
		return id;
	}

	/**
	 * Retrieves information from compartment table
	 * 
	 * @param stmt
	 * @return Map<Compartment_ID, name, abbreviation
	 * @throws SQLException
	 */
	public static Map<String, ArrayList<String>> getCompartmentsInfo (Statement stmt) throws SQLException{
		
		ResultSet rs = stmt.executeQuery("SELECT idcompartment, name, abbreviation FROM compartment");
		
		Map<String, ArrayList<String>> result = new HashMap<>();
		ArrayList<String> list = new ArrayList<>();
		
		while(rs.next()) {
			
			list = new ArrayList<>();
			list.add(rs.getString(2));
			list.add(rs.getString(3));
			result.put(rs.getString(1),list);
		}
		
		rs.close();
		return result;
	}
	
	/**
	 * Method to get the locusTag foa a given query.
	 * 
	 * @param query
	 * @param stmt
	 * @return String with locusTag for a given query
	 * @throws SQLException
	 */
	public static Map<String, String> getAllLocusTag(Statement statement) throws SQLException{
		
		Map<String, String> results = new HashMap<>();
		
		ResultSet rs = statement.executeQuery("SELECT id, query, locusTag FROM geneHomology "
				+ " INNER JOIN psort_reports ON psort_reports.locus_tag = query;");
		
		while(rs.next()){
			
			if(rs.getString(3).isEmpty() || rs.getString(3) == null)
				results.put(rs.getString(1), rs.getString(2));
			else 
				results.put(rs.getString(1), rs.getString(3));
		}
		
		rs.close();
		
		return results;
	}
	
	/**
	 * Get reactionIDs and ecNumbers.
	 * @param statement
	 * @return Map<String, List<String>>
	 * @throws SQLException
	 */
	public static Map<String, List<String>> getEnzymesReactions2(Statement statement) throws SQLException{
		
		Map<String, List<String>> enzymesReactions = new HashMap<String, List<String>>();
		List<String> reactionsIDs = null;
		
		ResultSet rs = statement.executeQuery("SELECT idreaction, enzyme_ecnumber, enzyme_protein_idprotein FROM reaction " +
				"INNER JOIN reaction_has_enzyme ON reaction_has_enzyme.reaction_idreaction = idreaction " +
				"WHERE (source <> 'TRANSPORTERS' AND reaction_has_enzyme.enzyme_ecnumber IS NOT NULL AND originalReaction)");
		
		while(rs.next()) {

			reactionsIDs = new ArrayList<String>();

			if(enzymesReactions.containsKey(rs.getString(2)))
				reactionsIDs = enzymesReactions.get(rs.getString(2));

			reactionsIDs.add(rs.getString(1));
			enzymesReactions.put(rs.getString(2),reactionsIDs);
		}
		
		rs.close();
		return enzymesReactions;
	}
	
	/**
	 * Get reactionsIDs WHERE source <> 'TRANSPORTERS'.
	 * @param statement
	 * @return List<String>
	 * @throws SQLException
	 */
	public static List<String> getReactionID(Statement statement) throws SQLException{
	
		List<String> reactionsIDs = null;
		reactionsIDs = new ArrayList<String>();
		
		ResultSet rs = statement.executeQuery("SELECT distinct idreaction " +
				" FROM reaction "+
				" LEFT JOIN reaction_has_enzyme ON reaction.idreaction = reaction_has_enzyme.reaction_idreaction " +
				" WHERE source <> 'TRANSPORTERS' AND reaction_has_enzyme.enzyme_ecnumber IS NULL AND originalReaction;");

		while(rs.next())
			reactionsIDs.add(rs.getString(1));
		
		rs.close();
		return reactionsIDs;
	}
	
	/**
	 * Check the existence of biochemical reactions.
	 * 
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static boolean checkBiochemicalReactions(Statement statement) throws SQLException{
	
		ResultSet rs = statement.executeQuery("SELECT * FROM reaction WHERE NOT originalReaction AND source <> 'TRANSPORTERS';");

		if(rs.next())
			return true;
		
		rs.close();
		return false;
	}
	
	
	/**
	 * Check the existence of transporters reactions.
	 * 
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static boolean checkTransportersReactions(Statement statement) throws SQLException{
	
		ResultSet rs = statement.executeQuery("SELECT * FROM reaction WHERE NOT originalReaction AND source = 'TRANSPORTERS';");

		if(rs.next())
			return true;
		
		rs.close();
		return false;
	}
	
	/**
	 * Removes all reactions assigned to the model.
	 * 
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static void removeNotOriginalReactions(Statement statement) throws SQLException{
	
		statement.execute("DELETE FROM reaction WHERE NOT originalReaction;");

	}
	
	/**
	 * Method to retrieve all distinct compartments and abbreviations in gene_has_compartment table.
	 * 
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, String>  getCompartments(Statement statement) throws SQLException{
		
		Map<String, String> comp = new HashMap<>();
		
		ResultSet rs = statement.executeQuery("SELECT DISTINCT(name), abbreviation FROM psort_reports_has_compartments INNER JOIN compartments ON id = compartment_id;");
		
		while(rs.next())
			comp.put(rs.getString(1), rs.getString(2));
		
		rs.close();
		
		return comp;
	}
	
	
}
