package pt.uminho.sysbio.common.database.connector.databaseAPI;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import pt.uminho.sysbio.common.database.connector.datatypes.Connection;
import pt.uminho.sysbio.common.database.connector.datatypes.DatabaseUtilities;
import pt.uminho.sysbio.common.database.connector.datatypes.Enumerators.DatabaseType;

/**
 * @author Oscar Dias
 *
 */
public class TransportersAPI {


	/**
	 * Set transport alignments as processed.
	 * 
	 * @param idLocusTag
	 * @param conn
	 * @param status 
	 * @throws SQLException
	 */
	public static void setProcessed(String idLocusTag, Connection conn, String status) throws SQLException {

		Statement stmt = conn.createStatement();

		stmt.execute("UPDATE sw_reports SET status='"+status+"'  WHERE id =" +idLocusTag);

		stmt.close();
		stmt=null;
	}

	/**
	 * Get the transmembrane helices already for genes already loaded.
	 * 
	 * @param conn
	 * @param projectID 
	 * @param status 
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, Integer> getGenesTransmembraneHelices(Connection conn, int projectID, String status) throws SQLException {

		Map<String, Integer> ret = new HashMap<>();

		Statement statement = conn.createStatement();

		ResultSet rs = statement.executeQuery("SELECT * FROM sw_reports WHERE status = '"+status+"' AND project_id = "+projectID);

		while(rs.next())
			ret.put(rs.getString(3), rs.getInt(6));

		statement.close();
		return ret;
	}

	/**
	 * Get the transmembrane helices already for genes already loaded.
	 * 
	 * @param conn
	 * @param projectID 
	 * @param status 
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, Integer> getGenesTransmembraneHelices(Connection conn, int projectID) throws SQLException {

		Map<String, Integer> ret = new HashMap<>();

		Statement statement = conn.createStatement();

		ResultSet rs = statement.executeQuery("SELECT * FROM sw_reports WHERE project_id = "+projectID);

		while(rs.next())
			ret.put(rs.getString(3), rs.getInt(6));

		statement.close();
		return ret;
	}

	/**
	 * Load Transport Alignments Genes
	 * 
	 * @param locus_tag
	 * @param matrix
	 * @param tmd
	 * @param conn
	 * @param locus_ids
	 * @param status
	 * @param project_id
	 * @return
	 * @throws SQLException
	 */
	public static String loadTransportAlignmentsGenes(String locus_tag, String matrix, int tmd, Connection conn, ConcurrentHashMap<String,String> locus_ids, String status, int project_id) throws SQLException {

		String result = null;
		if(locus_ids.contains(locus_tag)) {

			result=locus_ids.get(locus_tag);
		}
		else {

			Date sqlToday = new Date((new java.util.Date()).getTime());
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT id, number_TMD FROM sw_reports WHERE locus_tag='"+locus_tag+"' AND project_id = "+project_id);

			if(rs.next()) {
				result = rs.getString(1);

				stmt.execute("UPDATE sw_reports SET "
						+ " date = '"+sqlToday+"', "
						+ " matrix= '"+matrix+"', "
						+ " number_TMD = '"+tmd+"', "
						+ " project_id = "+project_id+", "
						+ " status ='"+status+"' " +
						" WHERE locus_tag = '"+locus_tag+"'");
			}
			else{

				stmt.execute("INSERT INTO sw_reports (locus_tag, date, matrix, number_TMD, project_id, status) " +
						"VALUES ('"+locus_tag+"','"+sqlToday+"','"+matrix+"','"+tmd+"',"+project_id+",'"+status+"')");
				rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
				rs.next();
				result = rs.getString(1);
			}

			rs.close();
			stmt=null;
			locus_ids.put(locus_tag,result);
		}
		return result;
	}

	/**
	 * Make File with genes from the transport alignment.
	 * 
	 * @param msmt 
	 * @param dataSource
	 * @throws IOException 
	 * @throws SQLException 
	 */
	public static void makeFile(String output_file_name, Connection conn) throws IOException, SQLException {

		FileWriter fstream = new FileWriter(output_file_name);
		BufferedWriter out = new BufferedWriter(fstream);

		Statement stmt = conn.createStatement();

		ResultSet rs = stmt.executeQuery("SELECT * FROM sw_reports "
				+"INNER JOIN sw_similarities ON sw_reports.id=sw_similarities.sw_reports_id "
				+"INNER JOIN sw_hits ON sw_hits.id=sw_similarities.sw_hits_id "
				+"ORDER BY sw_reports.locus_tag, similarity DESC ");

		out.write("locus tag\tsimilarity\thomologue ID\tTCDB ID\tnumber of helices\n");
		String locus="";
		while(rs.next()) {

			if(!locus.equals(rs.getString(1)) && rs.getString(8)!=null)
				locus=rs.getString(1);

			if(rs.getString(8)!=null)
				out.write(rs.getString(2)+"\t"+rs.getString(8)+"\t"+rs.getString(10)+"\t"+rs.getString(11)+"\t"+rs.getString(5)+"\n");
		}
		//Close the output stream
		out.close();
		stmt.close();
	}	


	/**
	 * Method for retrieving all genes loaded on the transport alignments.
	 * 
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	public static Set<String> retrieveAllTransportAlignmentGenes(Connection conn) throws SQLException{

		Set<String> processedGenes  = new HashSet<String>();

		Statement statement = conn.createStatement();

		ResultSet rs = statement.executeQuery("SELECT locus_tag FROM sw_reports WHERE status <> 'PROCESSING'");

		while(rs.next())
			processedGenes.add(rs.getString(1));

		statement.close();
		return processedGenes;
	}

	/**
	 * Method for retrieving processed genes loaded on the transport alignments.
	 * 
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	public static Set<String> retrieveProcessedTransportAlignmentGenes(Connection conn) throws SQLException{

		Set<String> processedGenes  = new HashSet<String>();

		Statement statement = conn.createStatement();

		ResultSet rs = statement.executeQuery("SELECT locus_tag FROM sw_reports WHERE status <> 'PROCESSING'");

		while(rs.next())
			processedGenes.add(rs.getString(1));

		statement.close();
		return processedGenes;
	}

	/**
	 * Load transmembrane predictions.
	 * 
	 * @param result
	 * @param projectID
	 * @param connection 
	 * @throws SQLException 
	 */
	public static void loadTransmembraneHelicesMap(Map<String, Integer> result, int projectID, Connection connection) throws SQLException {

		Statement statement = connection.createStatement();

		for(String locusTag : result.keySet()) {

			int tmd = result.get(locusTag);

			String status = "PROCESSING";
			if(tmd==0)
				status = "PROCESSED";

			statement.execute("INSERT INTO sw_reports (locus_tag, number_TMD, project_id, status) " +
					"VALUES ('"+locusTag+"', '"+tmd+"',"+projectID+",'"+status+"')");

		}
		statement.close();
	}

	/**
	 * @param connection
	 * @return
	 * @throws SQLException
	 */
	public static boolean checkReactionData(Connection connection) throws SQLException{

		Statement statement = connection.createStatement();

		ResultSet rs = statement.executeQuery("SELECT * FROM reaction");
		if(rs.next()){
			return true;
		}
		else{
			return false;
		}
	}



	/**
	 * Add Transporters pathway.
	 * 
	 * @param name
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static String addPathway(String name, Statement stmt) throws SQLException {

		ResultSet rs = stmt.executeQuery("SELECT idpathway FROM pathway WHERE name = '"+name+"';");

		if(!rs.next()) {

			stmt.execute("INSERT INTO pathway (name, code) VALUES('"+name+"','T0001')");
			rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
			rs.next();
		}
		String idpathway = rs.getString(1);

		return idpathway;
	}

	/**
	 * Add transport reaction
	 * 
	 * @param reactionID
	 * @param equation
	 * @param compartmentID
	 * @param isReversible
	 * @param ontology
	 * @param reactionInModel
	 * @param databaseType
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static String addReactionID(String reactionID, String equation, int compartmentID, boolean isReversible, boolean ontology, boolean reactionInModel, DatabaseType databaseType, Statement stmt) throws SQLException {

		String note = "";
		String notes ="";

		if(ontology) {

			notes=",notes";
			note=",'ontology'";
			reactionInModel=false;
		}

		ResultSet rs = stmt.executeQuery("SELECT idreaction FROM reaction WHERE name = '"+reactionID+"';");

		if(!rs.next()){

			stmt.execute("INSERT INTO reaction (name, reversible, inModel, equation, source, isGeneric, isSpontaneous, isNonEnzymatic,originalReaction,compartment_idcompartment"+notes+") " +
					"VALUES('"+reactionID+"',"+isReversible+","+reactionInModel+"," +
					"'"+DatabaseUtilities.databaseStrConverter(equation,databaseType)+"', 'TRANSPORTERS',false,false,false,true,"+compartmentID+note+")");

			rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
			rs.next();
		}
		String idreaction = rs.getString(1);

		return idreaction;
	}

	/**
	 * Get identification for genes in database
	 * 
	 * @param geneID
	 * @param geneDatabaseIDs
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, String> getGenesDatabaseIDs(String geneID, Map<String, String> geneDatabaseIDs, Statement stmt) throws SQLException {

		ResultSet rs;

		if(geneDatabaseIDs==null) {

			geneDatabaseIDs = new HashMap<String, String>();

			rs = stmt.executeQuery("SELECT sequence_id, idgene FROM gene;");

			while(rs.next())
				geneDatabaseIDs.put(rs.getString(1),rs.getString(2));

			rs.close();
		}

		boolean add = true;

		if(geneDatabaseIDs.containsKey(geneID)) {

			add = false;
		}
		else {

			for(String locus : geneDatabaseIDs.keySet())
				if(locus.replace("_", "").equalsIgnoreCase(geneID.replace("_", "")))
					add=false;
		}

		if(add) {

			rs = stmt.executeQuery("SELECT idgene FROM gene WHERE sequence_id = '"+geneID+"'");

			if(!rs.next()) {

				rs = stmt.executeQuery("SELECT idchromosome FROM chromosome WHERE name = 'DEFAULT'");
				String idchromosome;

				if(rs.next()) {

					idchromosome = rs.getString(1);
				}
				else {

					stmt.execute("INSERT INTO chromosome (name) VALUES('DEFAULT')");
					rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
					rs.next();
					idchromosome = rs.getString(1);
				}

				String locusTag = ModelAPI.getLocusTagFromHomologyData(stmt, geneID);

								if(locusTag==null)
									locusTag=geneID;
				//					UniProtAPI.getEntryData(geneID, this.taxonomyID).getLocusTag();

				stmt.execute("INSERT INTO gene (locusTag,chromosome_idchromosome,origin, sequence_id) VALUES('"+locusTag+"','"+idchromosome+"','TRANSPORTERS', '"+ geneID+"')");
				rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
				rs.next();
			}
			geneDatabaseIDs.put(geneID, rs.getString(1));
			rs.close();
		}

		return geneDatabaseIDs;
	}


	/**
	 * Add transport protein to model. 
	 * 
	 * @param tcnumber
	 * @param reactionID
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static String addProteinIDs(String tcnumber, String reactionID, Statement stmt) throws SQLException {

		ResultSet rs;

		String class_name = "";
		if (tcnumber.startsWith("1.")) {

			class_name = "Channel/Pore";
		}
		else if (tcnumber.startsWith("2.")) {

			class_name = "Electrochemical Potential-driven Transporter";
		}
		else if (tcnumber.startsWith("3.")) {

			class_name = "Primary Active Transporter";
		}
		else if (tcnumber.startsWith("4.")) {

			class_name = "Group Translocator";
		}
		else if (tcnumber.startsWith("5.")) {

			class_name = "Transmembrane Electron Carrier";
		}
		else if (tcnumber.startsWith("8.")) {

			class_name = "Accessory Factor Involved in Transport";
		}
		else if (tcnumber.startsWith("9.")) {

			class_name = "Incompletely Characterized Transport System";
		}

		String reaction_code = reactionID;

		if(reactionID.contains("_")) {

			reaction_code = reactionID.split("_")[0];
		}

		String name = "Transport protein for reaction "+reaction_code;

		rs = stmt.executeQuery("SELECT idprotein FROM protein " +
				"WHERE name = '"+name+"' " +
				"AND class = '"+class_name+"';");

		if (!rs.next()) {

			stmt.execute("INSERT INTO protein (name, class) " +
					"VALUES('"+name+"','"+class_name+"');");
			rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
			rs.next();
		}

		String idprotein =  rs.getString(1);

		rs = stmt.executeQuery("SELECT inModel FROM enzyme " +
				"WHERE protein_idprotein = '"+idprotein+"' " +
				"AND ecnumber = '"+tcnumber+"';");

		if(rs.next()) {

			if(!rs.getBoolean(1))
				stmt.execute("UPDATE enzyme SET inModel = true WHERE protein_idprotein='"+idprotein+"' AND ecnumber='"+tcnumber+"' AND source='TRANSPORTERS'");
		}
		else {

			stmt.execute("INSERT INTO enzyme (protein_idprotein, ecnumber, inModel, source)  VALUES('"+idprotein+"','"+tcnumber+"', true,'TRANSPORTERS')");
		}

		return idprotein;
	}

	/**
	 * Add pathway has reaction.
	 * 
	 * @param idPathway
	 * @param idReaction
	 * @param stmt
	 * @return
	 * @throws SQLException
	 * same method as in modelAPI
	 */
	@Deprecated 
	public static boolean addPathway_has_Reaction(String idPathway, String idReaction, Statement stmt) throws SQLException {

		ResultSet rs = stmt.executeQuery("SELECT * FROM pathway_has_reaction " +
				"WHERE reaction_idreaction = '"+idReaction+"' " +
				"AND pathway_idpathway = '"+idPathway+"';");

		if(rs.next()) {

			return false;
		}
		else {

			stmt.execute("INSERT INTO pathway_has_reaction (reaction_idreaction, pathway_idpathway) " +
					"VALUES('"+idReaction+"','"+idPathway+"');");
		}

		return true;
	}

	/**
	 * Add reaction has enzyme.
	 * 
	 * @param idprotein
	 * @param tcNumber
	 * @param idReaction
	 * @param stmt
	 * @return
	 * @throws SQLException
	 * 
	 * same method as in modelAPI
	 */
	@Deprecated 
	public static boolean addReaction_has_Enzyme(String idprotein, String tcNumber, String idReaction, Statement stmt) throws SQLException {

		ResultSet rs = stmt.executeQuery("SELECT * FROM reaction_has_enzyme " +
				"WHERE reaction_idreaction = '"+idReaction+"' " +
				"AND enzyme_protein_idprotein = '"+idprotein+"' " +
				"AND enzyme_ecnumber = '"+tcNumber+"';");

		if(rs.next()) {

			return false;
		}
		else {

			stmt.execute("INSERT INTO reaction_has_enzyme (reaction_idreaction, enzyme_protein_idprotein, enzyme_ecnumber) " +
					"VALUES('"+idReaction+"','"+idprotein+"','"+tcNumber+"');");
		}

		return true;
	}


	/**
	 * Add pathway has enzyme.
	 * 
	 * @param idprotein
	 * @param tcNumber
	 * @param idPathway
	 * @param stmt
	 * @return
	 * @throws SQLException
	 * 
	 * same method as in modelAPI
	 */
	@Deprecated 
	public static boolean addPathway_has_Enzyme(String idprotein, String tcNumber, String idPathway, Statement stmt) throws SQLException {

		ResultSet rs = stmt.executeQuery("SELECT * FROM pathway_has_enzyme " +
				"WHERE pathway_idpathway = '"+idPathway+"' " +
				"AND enzyme_protein_idprotein = '"+idprotein+"' " +
				"AND enzyme_ecnumber = '"+tcNumber+"';");

		if(rs.next()) {

			return false;
		}
		else {

			stmt.execute("INSERT INTO pathway_has_enzyme (pathway_idpathway, enzyme_protein_idprotein, enzyme_ecnumber) " +
					"VALUES('"+idPathway+"','"+idprotein+"','"+tcNumber+"');");
		}

		return true;
	}


	/**
	 * Add subunit to model
	 * @param idProtein
	 * @param tcNumber
	 * @param idgene
	 * @param stmt
	 * @return
	 * @throws SQLException
	 * 
	 * same method as in modelAPI
	 */
	@Deprecated 
	public static boolean addSubunit(String idProtein, String tcNumber, String idgene, Statement stmt) throws SQLException {

		ResultSet rs = stmt.executeQuery("SELECT * FROM subunit " +
				"WHERE gene_idgene = '"+idgene+"' " +
				"AND enzyme_protein_idprotein = '"+idProtein+"' " +
				"AND enzyme_ecnumber = '"+tcNumber+"';");

		if(rs.next()) {

			return false;
		}
		else {

			stmt.execute("INSERT INTO subunit (gene_idgene, enzyme_protein_idprotein, enzyme_ecnumber) " +
					"VALUES('"+idgene+"','"+idProtein+"','"+tcNumber+"');");
		}

		return true;
	}

	/**
	 * Add stoichiometry
	 * 
	 * @param idReaction
	 * @param idCompound
	 * @param idCompartment
	 * @param stoichiometry
	 * @param stmt
	 * @return
	 * @throws SQLException
	 * 
	 * same method as in modelAPI
	 */
	@Deprecated 
	public static boolean addStoichiometry(String idReaction, String idCompound, String idCompartment, double stoichiometry, Statement stmt) throws SQLException {

		ResultSet rs = stmt.executeQuery("SELECT idstoichiometry FROM stoichiometry " +
				"WHERE reaction_idreaction = '"+idReaction+"' " +
				"AND compound_idcompound = '"+idCompound+"' " +
				"AND compartment_idcompartment = '"+idCompartment+"' " +
				"AND stoichiometric_coefficient = '"+stoichiometry+"' ;");

		if(!rs.next()) {

			stmt.execute("INSERT INTO stoichiometry (reaction_idreaction, compound_idcompound, compartment_idcompartment, stoichiometric_coefficient, numberofchains) " +
					"VALUES('"+idReaction+"','"+idCompound+"','"+idCompartment+"','"+stoichiometry+"','1')");
		}

		return true;
	}


	/**
	 * Get compound id from kegg id
	 * 
	 * @param keggID
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static String getCompoundID(String keggID, Statement stmt) throws SQLException {

		ResultSet rs = stmt.executeQuery("SELECT idcompound FROM compound " +
				"WHERE kegg_id = '"+keggID+"' ;");

		if(rs.next())
			return rs.getString(1);
		else 
			return null;
	}

	/**
	 * Get compartment identifier.
	 * 
	 * @param compartment
	 * @param compartments_ids
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static String getCompartmentsID(String compartment, Map<String, String> compartments_ids, Statement stmt) throws SQLException {

		String idcompartment = null;

		if(compartments_ids.containsKey(compartment)) {

			idcompartment = compartments_ids.get(compartment);
		}
		else {

			ResultSet rs = stmt.executeQuery("SELECT idCompartment FROM compartment " +
					"WHERE name = '"+compartment+"' ;");

			if(!rs.next()) {

				String abb = compartment;

				if(compartment.length()>3) {

					abb = compartment.substring(0, 3);
				}

				stmt.execute("INSERT INTO compartment (name, abbreviation) " +
						"VALUES('"+compartment+"','"+abb+"');");
				rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
				rs.next();
			}
			idcompartment = rs.getString(1);

		}
		return idcompartment;
	}

}
