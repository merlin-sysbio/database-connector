package pt.uminho.sysbio.common.database.connector.databaseAPI;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
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

import pt.uminho.sysbio.common.database.connector.datatypes.Connection;
import pt.uminho.sysbio.common.database.connector.datatypes.DatabaseUtilities;
import pt.uminho.sysbio.common.database.connector.datatypes.Enumerators.DatabaseType;
import pt.uminho.sysbio.merlin.utilities.containers.capsules.AlignmentCapsule;
import pt.uminho.sysbio.merlin.utilities.containers.capsules.DatabaseReactionContainer;

/**
 * @author Oscar Dias
 *
 */
public class TransportersAPI {


	/**
	 * Set transport alignments as processed.
	 * 
	 * @param idLocusTag
	 * @param status
	 * @param statement
	 * @throws SQLException
	 */
	public static void setProcessed(int id, String status, Statement statement) throws SQLException {

		statement.execute("UPDATE sw_reports SET status='"+status+"'  WHERE id =" +id +";");
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
	 * @param status
	 * @param project_id
	 * @param statement
	 * @throws SQLException
	 */
	public static void loadTransportAlignmentsGenes(String locus_tag, String matrix, int tmd, String status, int project_id, Statement statement) throws SQLException {

		long time = System.currentTimeMillis();
		Timestamp sqlToday = new Timestamp(time);
		
		ResultSet rs = statement.executeQuery("SELECT id, number_TMD FROM sw_reports WHERE locus_tag='"+locus_tag+"' AND project_id = "+project_id+";");
		
		if(rs.next()) {

			statement.execute("UPDATE sw_reports SET "
					+ " date = '"+sqlToday+"', "
					+ " matrix= '"+matrix+"', "
					+ " number_TMD = '"+tmd+"', "
					+ " project_id = "+project_id+", "
					+ " status ='"+status+"' " +
					" WHERE locus_tag = '"+locus_tag+"'");
		}
		else{

			statement.execute("INSERT INTO sw_reports (locus_tag, date, matrix, number_TMD, project_id, status) " +
					"VALUES ('"+locus_tag+"','"+sqlToday+"','"+matrix+"','"+tmd+"',"+project_id+",'"+status+"');");
			rs = statement.executeQuery("SELECT LAST_INSERT_ID();");
			rs.next();
		}

		rs.close();
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
	public static Set<String> retrieveProcessingTransportAlignmentGenes(Statement statement) throws SQLException{

		Set<String> processedGenes  = new HashSet<String>();

		ResultSet rs = statement.executeQuery("SELECT locus_tag FROM sw_reports WHERE status <> 'PROCESSING'");

		while(rs.next())
			processedGenes.add(rs.getString(1));

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
	 * Check if the reaction table has data.
	 * 
	 * @param connection
	 * @return
	 * @throws SQLException
	 */
	public static boolean checkReactionData(Connection connection) throws SQLException{

		Statement statement = connection.createStatement();

		ResultSet rs = statement.executeQuery("SELECT * FROM reaction");
		if(rs.next()) {
			
			statement.close();
			return true;
		}
		else {
			
			statement.close();
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
	
	/**
	 * Get id from locus tag, for a given project.
	 * 
	 * @param locusTag
	 * @param projectId
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static String getIdLocusTag(String locusTag, int projectId, Statement statement) throws SQLException{
		
		java.sql.Date sqlToday = new java.sql.Date((new java.util.Date()).getTime());
		
		ResultSet rs = statement.executeQuery("SELECT id FROM psort_reports WHERE locus_tag='"+locusTag+"' AND project_id = "+projectId);
		if(!rs.next()) {
			
			statement.execute("INSERT INTO psort_reports (locus_tag, project_id, date) VALUES('"+locusTag+"', "+projectId+",'"+sqlToday+"')");
			rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
			rs.next();
		}
		
		String idLT = rs.getString(1);
		rs.close();
		
		return idLT;
	}
	
	/**
	 * Insert gene-compartment association.
	 * A new compartment is added if not available. 
	 * 
	 * @param geneID
	 * @param name
	 * @param abbreviation
	 * @param compartmentScore
	 * @param statement
	 * @throws SQLException
	 */
	public static void insertIntoCompartments(String geneID, String name, String abbreviation, Double compartmentScore, Statement statement) throws SQLException{
		
		ResultSet rs = statement.executeQuery("SELECT id FROM compartments WHERE abbreviation='"+ abbreviation +"'");

		if(!rs.next()) {

			statement.execute("INSERT INTO compartments (name,abbreviation) VALUES('"+ name.toUpperCase() +"', '"+ abbreviation +"')");
			rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
			rs.next();
		}
		String compartmentID = rs.getString(1);

		rs = statement.executeQuery("SELECT * FROM psort_reports_has_compartments WHERE psort_report_id='"+geneID+"' AND compartment_id='"+compartmentID+"'");

		if(!rs.next())
			statement.execute("INSERT INTO psort_reports_has_compartments (psort_report_id, compartment_id, score) VALUES("+geneID+","+compartmentID+","+compartmentScore+")");
	
		rs.close();
		
	}
	
	/**
	 * Get best comparment for gene.
	 * @param projectID
	 * @param statement
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getBestCompartmenForGene(int projectID, Statement statement) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery("SELECT psort_report_id, locus_tag, score, abbreviation, name FROM psort_reports_has_compartments " +
					"INNER JOIN psort_reports ON psort_reports.id=psort_report_id " +
					"INNER JOIN compartments ON compartments.id=compartment_id " +
					"WHERE project_id = "+projectID+" "+
					"ORDER BY psort_report_id ASC, score DESC;");
		
		while (rs.next()){
			String[] list = new String[5];
			
			list[0] = rs.getString(1);
			list[1] = rs.getString(2);
			list[2] = rs.getString(3);
			list[3] = rs.getString(4);
			list[4] = rs.getString(5);
			
			result.add(list);
		}
		
		rs.close();
		
		return result;
	}
	
	/**
	 * Get existing annotation data.
	 * @param uniprotID
	 * @param statement
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getExistingAnnotation(String uniprotID, Statement statement) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery("SELECT directions, equation, transport_systems.reversible, direction, metabolites.name  FROM tcdb_registries " +
				" INNER JOIN tc_numbers ON (tc_numbers.tc_number = tcdb_registries.tc_number AND tc_numbers.tc_version = tcdb_registries.tc_version )" +
				" INNER JOIN general_equation ON (tc_numbers.general_equation_id = general_equation.id ) " +
				" INNER JOIN tc_numbers_has_transport_systems ON (tc_numbers_has_transport_systems.tc_number = tc_numbers.tc_number AND tc_numbers_has_transport_systems.tc_version = tc_numbers.tc_version)" +
				" INNER JOIN transport_systems ON (tc_numbers_has_transport_systems.transport_system_id = transport_systems.id)" +
				" INNER JOIN transport_types ON (transport_types.id = transport_type_id)" +
				" INNER JOIN transported_metabolites_directions ON (transported_metabolites_directions.transport_system_id = transport_systems.id) " +
				" INNER JOIN directions ON (transported_metabolites_directions.direction_id = directions.id) " +
				" INNER JOIN metabolites ON (transported_metabolites_directions.metabolite_id = metabolites.id) " +
				" WHERE uniprot_id = '"+uniprotID+"' AND datatype = 'MANUAL'");
	
		while (rs.next()){
			String[] list = new String[5];
			
			list[0] = rs.getString(1);
			list[1] = rs.getString(2);
			list[2] = rs.getBoolean(3)+"";
			list[3] = rs.getString(4);
			list[4] = rs.getString(5);
			
			result.add(list);
		}
		
		rs.close();
		return result;
	
	}

	/**
	 * Select data from metabolites_ontology table.
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> selectMetabolitesOntology (Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = stmt.executeQuery("SELECT metabolites_ontology.id, metabolite_id, child_id " + 
				//, kegg_miriam as child_kegg_miriam, chebi_miriam as child_chebi_miriam " +
				" FROM metabolites_ontology " +
				" INNER JOIN metabolites ON (child_id = metabolites.id)");
		
		while (rs.next()){
			String[] list = new String[3];
			
			list[0] = rs.getString(1);
			list[1] = rs.getString(2);
			list[2] = rs.getString(3);
			
			result.add(list);
		}
		
		rs.close();

		return result;
	}
	
	/**
	 * Get data from metabolites table.
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> selectChebiMiriam (Statement stmt) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		String[] list = new String[3];
		
		ResultSet rs = stmt.executeQuery("SELECT id, kegg_miriam, chebi_miriam " +
				" FROM metabolites");
		
		while(rs.next()){
			
			list[0] = rs.getString(1);
			list[1] = rs.getString(2);
			list[2] = rs.getString(3);
			
			result.add(list);
		}
		
		rs.close();
		return result;
	}
	
	/**
	 * Get Transport type list.
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, String> getTransportTypeList(Statement stmt) throws SQLException {
		
		Map<String, String> transport_type_list = new TreeMap<>();
		ResultSet rs = stmt.executeQuery("SELECT * FROM transport_types");

		while(rs.next())
			transport_type_list.put(rs.getString(1),rs.getString(2));

		rs.close();
		return transport_type_list;
	}
		
	/**
	 * Execute sql function getTransportTypeTaxonomyScore().
	 * @param query
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getTransportTypeTaxonomyScore(String query, Statement stmt) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = stmt.executeQuery(query);
		
		while (rs.next()){
			String[] list = new String[4];
			
			list[0] = rs.getString(1);
			list[1] = rs.getString(2);
			list[2] = rs.getString(3);
			list[3] = rs.getString(4);
			
			result.add(list);
		}
		
		rs.close();

		return result;
	}
	
	/**
	 * Get tcNumber equations
	 * @param stmt
	 * @return Map<String, String>
	 * @throws SQLException
	 */
	public static Map<String, String> getTcNumersEquations(Statement stmt) throws SQLException {
		
		Map<String, String> tc_numbers_equations = new TreeMap<>();
		ResultSet rs = stmt.executeQuery("SELECT tc_number, equation FROM general_equation " +
				" INNER JOIN tc_numbers ON (general_equation.id = general_equation_id) ");

		while(rs.next())
			tc_numbers_equations.put(rs.getString(1),rs.getString(2));

		rs.close();
		return tc_numbers_equations;
	}
	
	/**
	 * Get data from gene_to_metabolite_direction table
	 * @param projectID
	 * @param stmt
	 * @return ArrayList<String[]> 
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getGeneToMetaboliteDirectionData(int projectID, Statement stmt) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs =	stmt.executeQuery("SELECT gene_id, locus_tag, tc_family, transport_reaction_id, metabolite_id, " +
				" stoichiometry, direction, metabolite_name, kegg_miriam, chebi_miriam, metabolite_kegg_name, tc_number,"
				+ " similarity, taxonomy_data_id, transport_type, reversible, uniprot_id " +
				" FROM gene_to_metabolite_direction " +
				" WHERE project_id = "+projectID+" " +
				" ORDER BY gene_id, transport_reaction_id, uniprot_id,  metabolite_id;");
		
		while (rs.next()){
			String[] list = new String[17];
			
			list[0] = rs.getString(1);
			list[1] = rs.getString(2);
			list[2] = rs.getString(3);
			list[3] = rs.getString(4);
			list[4] = rs.getString(5);
			list[5] = rs.getString(6);
			list[6] = rs.getString(7);
			list[7] = rs.getString(8);
			list[8] = rs.getString(9);
			list[9] = rs.getString(10);
			list[10] = rs.getString(11);
			list[11] = rs.getString(12);
			list[12] = rs.getString(13);
			list[13] = rs.getString(14);
			list[14] = rs.getString(15);
			list[15] = rs.getBoolean(16)+"";
			list[16] = rs.getString(17);
			
			result.add(list);
		}
		
		rs.close();
		return result;
	}
	
	/**
	 * Get metabolites formula.
	 * @param stmt
	 * @return Map<String,String>
	 * @throws SQLException
	 */
	public static Map<String,String> setMetaboltitesFormulas(Statement stmt) throws SQLException{
		
		Map<String, String> metabolitesFormula = new HashMap<String,String>();

		ResultSet rs = stmt.executeQuery("SELECT id, kegg_formula, chebi_formula FROM metabolites;");

		while(rs.next()) {

			String id = rs.getString(1);
			if(!rs.getString(2).equalsIgnoreCase("") && !rs.getString(2).equalsIgnoreCase("null") && rs.getString(2)!=null)
				metabolitesFormula.put(id, rs.getString(2));
			else if(!metabolitesFormula.containsKey(id) && !rs.getString(3).equalsIgnoreCase("") && !rs.getString(3).equalsIgnoreCase("null") && rs.getString(3)!=null)
				metabolitesFormula.put(id, rs.getString(3));
		}
		rs.close();

		return metabolitesFormula;
	}
	
	/**
	 * Get organisms taxonomy score
	 * @param originArray
	 * @param stmt
	 * @return Map<Integer, Double>
	 * @throws Exception
	 */
	public static Map<Integer, Double> getOrganismsTaxonomyScore(String[] originArray, Statement stmt) throws Exception {

		Map<Integer, Double> map = new HashMap<Integer, Double>();
		ResultSet rs = stmt.executeQuery("SELECT organism, taxonomy, id FROM taxonomy_data");

		while(rs.next()) {

			double counter=0;
			String[] other_array = rs.getString(2).replace("[", "").replace("]", "").split(",");

			for(int i=0;i<originArray.length;i++) {

				if(i==other_array.length) {

					if(originArray[originArray.length-1].equals(rs.getString(1)))
						counter++;

					i=originArray.length;
				}
				else if(originArray[i].trim().equals(other_array[i].trim())) {

					counter++;

				}
				else {

					i=originArray.length;
				}
			}
			map.put(rs.getInt(3),counter);
		}
		rs.close();

		return map;
	}
	
	/**
	 * Get similarities.
	 * @param stmt
	 * @return Map<String, Integer>
	 * @throws SQLException
	 */
	public static Map<String, Integer> getSimilarities(Statement stmt) throws SQLException{
		
		Map<String, Integer> similarities = new HashMap<>();

		ResultSet rs = stmt.executeQuery("SELECT gene_id, SUM(similarity) FROM genes_has_tcdb_registries GROUP BY gene_id;");

		while(rs.next())
			similarities.put(rs.getString(1), rs.getInt(2));
		
		rs.close();

		return similarities;
	}
	
	
	/**
	 * get Metabolites Score.
	 * @param stmt
	 * @return Map<String, ArrayList<String>>
	 * @throws SQLException
	 */
	public static Map<String, ArrayList<String>> getMetabolitesScore(Statement stmt) throws SQLException{
		
		Map<String, ArrayList<String>> table = new HashMap<String, ArrayList<String>>();
		
		ArrayList<String> list = new ArrayList<>();
		
		ResultSet rs =	stmt.executeQuery("SELECT metabolite_id, gene_id, similarity_score_sum, taxonomy_score_sum, frequency FROM genes_has_metabolites;");

		while(rs.next()){
			
			list.add(rs.getString(2));
			list.add(rs.getString(3));
			list.add(rs.getString(4));
			list.add(rs.getString(5));
			
			table.put(rs.getString(1), list);
		}
		
		rs.close();

		return table;
	}

	/**
	 * Get data from genes_has_tcdb_registries table.
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> geneHasTcdbRegestries(Statement stmt) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = stmt.executeQuery( "SELECT genes_has_tcdb_registries.gene_id, metabolite_id, SUM(similarity) FROM genes_has_tcdb_registries "
				+ " INNER JOIN genes_has_metabolites ON genes_has_tcdb_registries.gene_id=genes_has_metabolites.gene_id "
				+ " INNER JOIN genes ON genes.id = genes_has_tcdb_registries.gene_id "
				+ " INNER JOIN tcdb_registries ON (genes_has_tcdb_registries.uniprot_id=tcdb_registries.uniprot_id AND genes_has_tcdb_registries.version=tcdb_registries.version) "
				+ " GROUP BY genes_has_tcdb_registries.gene_id, metabolite_id;");
		
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
	 * Get transport type data.
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> transportTypeID(Statement stmt) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = stmt.executeQuery( "SELECT transport_type_id, metabolite_id, gene_id, transport_type_score_sum, taxonomy_score_sum, frequency "
				+ "FROM genes_has_metabolites_has_type ORDER BY gene_id, metabolite_id , transport_type_id");

		while(rs.next()){
			String[] list = new String[6];
			
			list[0] = rs.getString(1);
			list[1] = rs.getString(2);
			list[2] = rs.getString(3);
			list[3] = rs.getString(4);
			list[4] = rs.getString(5);
			list[5] = rs.getString(6);
			
			result.add(list);
		}

		rs.close();
		return result;
	}
	
	/**
	 * Get metabolites gene score.
	 * @param query
	 * @param stmt
	 * @return ArrayList<String[]> 
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getMetabolitesGeneScore(String query, Statement stmt) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = stmt.executeQuery(query);
		
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
	 * Get data from  metabolites_ontology table.
	 * @param id
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getChildID(String id, Statement stmt) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = stmt.executeQuery("SELECT child_id, name, kegg_miriam, kegg_name, chebi_miriam, chebi_name, datatype, metabolites_ontology.id FROM metabolites_ontology"
				+ " INNER JOIN metabolites ON metabolites.id= child_id WHERE metabolite_id='"+id+"'");
		
		while(rs.next()){
			String[] list = new String[8];

			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);
			list[3]=rs.getString(4);
			list[4]=rs.getString(5);
			list[5]=rs.getString(6);
			list[6]=rs.getString(7);
			list[7]=rs.getString(8);
			
			result.add(list);
		}
		rs.close();
		return result;
	}
	
	/**
	 * Load gene and insert it if don't exist.
	 * @param query
	 * @param query2
	 * @param locusTag
	 * @param projectID
	 * @param statement
	 * @return String,
	 * @throws SQLException
	 */
	public static String loadGene(String query, String query2, String locusTag, int projectID, Statement statement) throws SQLException{
		
		String result;
		ResultSet rs = statement.executeQuery(query);

		if(!rs.next()) {

			statement.clearWarnings();
			statement.execute(query2);
			rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
			rs.next();
		}
		result=rs.getString(1);
		rs.close();

		return result;
	}
	
	/**
	 * Get uniprot database IDs.
	 * @param statement
	 * @return Set<String>
	 * @throws SQLException
	 */
	public static Set<String> getUniprotDatabaseIDs(Statement statement) throws SQLException{
		
		Set<String> uniprotIDs = new TreeSet<String>();

		ResultSet rs = statement.executeQuery("SELECT DISTINCT(uniprot_id) FROM tcdb_registries;");

		while(rs.next())
			uniprotIDs.add(rs.getString(1));
		
		rs.close();

		return uniprotIDs;
	}
	
	/**
	 * get transport systems for a given tc_number and uniprot_id.
	 * @param uniprotID
	 * @param tcNumber
	 * @param statement
	 * @return Set<String>
	 * @throws SQLException
	 */
	public static Set<Integer> getTransportSystems(String uniprotID, String tcNumber, Statement statement) throws SQLException{
		
		Set<Integer> loadedTransportSystemIds = new TreeSet<Integer>(); 

		ResultSet rs = statement.executeQuery("SELECT transport_system_id FROM tcdb_registries " +
				" INNER JOIN tc_numbers_has_transport_systems " +
				" ON (tc_numbers_has_transport_systems.tc_version = tcdb_registries.tc_version " +
				"AND tc_numbers_has_transport_systems.tc_number = tcdb_registries.tc_number)" +
				" WHERE uniprot_id='"+uniprotID+"' AND tcdb_registries.tc_number='"+tcNumber+"' AND latest_version");

		while (rs.next())
			loadedTransportSystemIds.add(rs.getInt(1));

		rs.close();

		return loadedTransportSystemIds;
	}
	
	/**
	 * Get tcVersion for a given tc_number and uniprot_id.
	 * @param uniprotID
	 * @param tcNumber
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int getTC_version(String uniprotID, String tcNumber, Statement statement) throws SQLException{
		
		int tcVersion = -1;

		//		ResultSet rs = this.statement.executeQuery("SELECT MAX(tc_version) " +
		//				" FROM tc_numbers WHERE tc_number='"+tc_number+"';");

		ResultSet rs = statement.executeQuery("SELECT tc_version " +
				" FROM tcdb_registries WHERE tc_number='"+tcNumber+"' AND uniprot_id = '"+uniprotID+"' AND latest_version;");

		if(rs.next())
			if(rs.getInt(1)>0)
				tcVersion = rs.getInt(1);

		return tcVersion;
	}
	
	/**
	 * Add TCnumber.
	 * @param query1
	 * @param query2
	 * @param statement
	 * @throws SQLException
	 */
	public static void addTC_number(String query1, String query2, Statement statement) throws SQLException{
		
		ResultSet rs = statement.executeQuery(query1);

		if(!rs.next())
			statement.execute(query2);

	}
	
	/**
	 * Get tcdb_registries data.
	 * @param uniprotID
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int select_tcdb_registry(String uniprotID, Statement statement) throws SQLException{
		
		int currentVersion = 1;

		ResultSet rs = statement.executeQuery("SELECT MAX(version) " +
				" FROM tcdb_registries WHERE uniprot_id='"+uniprotID+"';");

		if(rs.next())
			if(rs.getInt(1)>0)
				currentVersion = currentVersion + rs.getInt(1);
		rs.close();
		return currentVersion;
		
	}
	
	/**
	 * Get taxonomy data.
	 * @param tcNumber
	 * @param statement
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getTaxonomyData(String tcNumber, Statement statement) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery("SELECT MAX(tc_version), taxonomy_data_id, tc_family, tc_location, affinity, "
				+ "general_equation_id FROM tc_numbers WHERE tc_number='"+tcNumber+"';");

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
	 * Get uniprotID for a given tc_number.
	 * @param tcNumber
	 * @param statement
	 * @return Set<String>
	 * @throws SQLException
	 */
	public static Set<String> selectUniprotIDs(String tcNumber, Statement statement) throws SQLException{
		
		Set<String> uniprotIDs = new HashSet<String>();
		ResultSet rs = statement.executeQuery("SELECT uniprot_id FROM tcdb_registries WHERE tc_number='"+tcNumber+"' AND latest_version");

		while(rs.next())
			uniprotIDs.add(rs.getString(1));
		rs.close();
		return uniprotIDs;
	}
	
	/**
	 * Get taxonomyID for a given organism.
	 * @param organism
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int selectTaxonomyID(String organism, Statement statement) throws SQLException{
		
		int result = -1;
		
		ResultSet rs = statement.executeQuery("SELECT id FROM taxonomy_data WHERE organism='"+organism+"';");
		if(rs.next()) 
			result = rs.getInt(1);

		return result;
	}
	
	/**
	 * Insert data into taxonomy_data table.
	 * @param organism
	 * @param taxonomy
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int insertTaxonomyID(String organism, String taxonomy, Statement statement) throws SQLException{
		
		int result = -1;
		
		statement.execute("INSERT INTO taxonomy_data (organism,taxonomy)" +
				" VALUES('"+organism+"','"+taxonomy+"')");
		ResultSet rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
		
		if(rs.next()) 
			result = rs.getInt(1);
		
		return result;
	}
	
	/**
	 * Get data from genes_has_metabolites table.
	 * @param geneID
	 * @param metabolitesID
	 * @param statement
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getSimilarityAndTaxonomyScore(String geneID, String metabolitesID, Statement statement) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery("SELECT similarity_score_sum, taxonomy_score_sum, frequency FROM genes_has_metabolites" +
				" WHERE gene_id='"+geneID+"' AND metabolite_id='"+metabolitesID+"';");


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
	 * Get data from genes_has_metabolites_has_type table.
	 * @param genesID
	 * @param metabolitesID
	 * @param typeID
	 * @param statement
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> selectGeneHasMetaboliteHasType(String genesID, String metabolitesID, String typeID, Statement statement) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery("SELECT transport_type_score_sum,taxonomy_score_sum,frequency FROM genes_has_metabolites_has_type" +
				" WHERE gene_id='"+genesID+"' AND transport_type_id='"+typeID+"' AND metabolite_id='"+metabolitesID+"';");

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
	 * Executes a given SQL query.
	 * @param query
	 * @param statement
	 * @throws SQLException
	 */
	public static void executeQuery(String query, Statement statement) throws SQLException{
		
		statement.execute(query);
	}
	
	/**
	 * Get data from tc_numbers_has_transport_systems table.
	 * @param tcNumberID
	 * @param transportSystemID
	 * @param tcVersion
	 * @param statement
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> loadTcNumberHasTransportSystem(String tcNumberID, int transportSystemID, int tcVersion, Statement statement) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery("SELECT * FROM tc_numbers_has_transport_systems" +
				" WHERE tc_number='"+tcNumberID+"' AND transport_system_id="+transportSystemID+" AND tc_version = "+tcVersion+";");


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
	 * Insert data into transport_systems table.
	 * @param transport_type_id
	 * @param reversibility
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int loadTransportSystem(int transport_type_id, boolean reversibility, Statement statement) throws SQLException{
		
		int result = 0;
		statement.execute("INSERT INTO transport_systems (transport_type_id, reversible) VALUES("+transport_type_id+","+reversibility+")");
		ResultSet rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
		if(rs.next());
			result=rs.getInt(1);
		rs.close();

		return result;
		
	}
	
	/**
	 * Get stoichiometry from transported_metabolites_directions table.
	 * @param metabolitesID
	 * @param directionID
	 * @param transportSystemID
	 * @param statement
	 * @return ArrayList<String>
	 * @throws SQLException
	 */
	public static ArrayList<String> loadTransportedMetabolitesDirection(int metabolitesID, String directionID, int transportSystemID, Statement statement) throws SQLException{
		
		ArrayList<String> result = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery("SELECT stoichiometry FROM transported_metabolites_directions " +
				"WHERE metabolite_id='"+metabolitesID+"' " +
				"AND transport_system_id='"+transportSystemID+"' " +
				"AND direction_id='"+directionID+"';");
		
		while(rs.next())
			result.add(rs.getString(1));
		
		rs.close();
		return result;
	}
	
	/**
	 * @param direction
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static String getDirection(String direction, Statement statement) throws SQLException{
		
		String result = "";
		ResultSet rs = statement.executeQuery("SELECT id FROM directions WHERE direction='"+direction+"';");

		if(rs.next())
			result=rs.getString(1);
		
		rs.close();
		return result;
	}
	
	/**
	 * Insert data into directions table.
	 * @param direction
	 * @param statement
	 * @return String
	 * @throws SQLException
	 */
	public static String insertDirection(String direction, Statement statement) throws SQLException{
		
		String result="";
		
		statement.execute("INSERT INTO directions (direction) VALUES('"+direction+"')");
		
		ResultSet rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
		if(rs.next())
			result=rs.getString(1);
		
		rs.close();
		return result;
	}
	
	/**
	 * Select id from transport_types table.
	 * @param transportType
	 * @param directions
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int selectTransportType(String transportType, String directions, Statement statement) throws SQLException{
		
		int result = -1;
		ResultSet rs = statement.executeQuery("SELECT id FROM transport_types WHERE name='"+transportType+"' AND directions='"+directions+"';");

		if(rs.next()) 
			result=rs.getInt(1); 
		
		rs.close();
		return result;
	}
	
	/**
	 * Insert data into transport_types table.
	 * @param transportType
	 * @param directions
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int insertTransportType(String transportType, String directions, Statement statement) throws SQLException{
		
		int result = 0;
		
		statement.execute("INSERT INTO transport_types	 (name,directions) VALUES('"+transportType+"','"+directions+"')");
		ResultSet rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
		if(rs.next())
			result = rs.getInt(1); 
		
		rs.close();
		return result;
		
	}
	
	/**
	 * Get data from metabolites table for a given chebi_miriam.
	 * @param kegg
	 * @param chebiMiriam
	 * @param statement
	 * @return ArrayList<String>
	 * @throws SQLException
	 */
	public static ArrayList<String> getDataFromMetabolites(String kegg, String chebiMiriam, Statement statement) throws SQLException{
		
		ArrayList<String> list = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery("SELECT name, datatype, id FROM metabolites WHERE kegg_miriam='"+kegg+"' AND chebi_miriam = '"+chebiMiriam+"'");

		while(rs.next()){
			list.add(rs.getString(1));
			list.add(rs.getString(2));
			list.add(rs.getString(3));
		}
		rs.close();
		return list;
	}
	
	/**
	 * Get data from metabolites table for a given kegg_miriam.
	 * @param kegg
	 * @param statement
	 * @return ArrayList<String>
	 * @throws SQLException
	 */
	public static ArrayList<String> getDataFromMetabolites2(String kegg, Statement statement) throws SQLException{
		
		ArrayList<String> list = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery("SELECT name, chebi_miriam, datatype, id FROM metabolites WHERE kegg_miriam='"+kegg+"'");

		while(rs.next()){
			list.add(rs.getString(1));
			list.add(rs.getString(2));
			list.add(rs.getString(3));
			list.add(rs.getString(4));

		}
		rs.close();
		return list;
	}
	
	
	/**
	 * Insert data into metabolites.
	 * @param query
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int insertIntoMetabolites(String query, Statement statement) throws SQLException{

		int result = 0;
		
		statement.executeQuery(query);

		ResultSet rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
		if(rs.next())
			result =  rs.getInt(1);

		rs.close();
		return result;
	}
	
	/**
	 * Get data from metabolites table for a given kegg_miriam.
	 * @param kegg
	 * @param statement
	 * @return ArrayList<String>
	 * @throws SQLException
	 */
	public static ArrayList<String> getDataFromMetabolites3(String kegg, Statement statement) throws SQLException{
		
		ArrayList<String> list = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery("SELECT name, id, datatype FROM metabolites WHERE kegg_miriam = '"+kegg+"';");

		while(rs.next()){
			list.add(rs.getString(1));
			list.add(rs.getString(2));
			list.add(rs.getString(3));
		}
		rs.close();
		return list;
	}
	
	/**
	 * get synonyms
	 * @param nameInDatabase
	 * @param statement
	 * @return ArrayList<String>
	 * @throws SQLException
	 */
	public static ArrayList<String> getSynonyms(String nameInDatabase, Statement statement) throws SQLException{
		
		ArrayList<String> list = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery("SELECT metabolite_id, datatype, name FROM synonyms WHERE name = '"+nameInDatabase+"';");

		while(rs.next()){
			list.add(rs.getString(1));
			list.add(rs.getString(2));
			list.add(rs.getString(3));
		}
		rs.close();
		return list;
	}
	
	/**
	 * Get data from metabolites table for a given chebi_miriam.
	 * @param chebi
	 * @param statement
	 * @return ArrayList<String>
	 * @throws SQLException
	 */
	public static ArrayList<String> getDataFromMetabolites4(String chebi, Statement statement) throws SQLException{
		
		ArrayList<String> list = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery("SELECT name, id, datatype FROM metabolites WHERE chebi_miriam = '"+chebi+"';");

		while(rs.next()){
			list.add(rs.getString(1));
			list.add(rs.getString(2));
			list.add(rs.getString(3));
		}
		rs.close();
		return list;
	}
	
	/**
	 * Get data from metabolites table.
	 * @param query
	 * @param statement
	 * @return ArrayList<String>
	 * @throws SQLException
	 */
	public static ArrayList<String> getDataFromMetabolites5(String query, Statement statement) throws SQLException{
		
		ArrayList<String> list = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery(query);

		while(rs.next()){
			list.add(rs.getString(1));
			list.add(rs.getString(2));
		}
		rs.close();
		return list;
	}
	
	/**
	 * Get synonyms data.
	 * @param query
	 * @param statement
	 * @return ArrayList<String>
	 * @throws SQLException
	 */
	public static ArrayList<String> existsSynonym(String query, Statement statement) throws SQLException{
		
		ArrayList<String> list = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery(query);

		while(rs.next()){
			list.add(rs.getString(1));
			list.add(rs.getString(2));
		}
		rs.close();
		return list;
	}
	
	/**
	 * Get metaboliteIDs.
	 * @param query
	 * @param statement
	 * @return ArrayList<String>
	 * @throws SQLException
	 */
	public static ArrayList<String> getMetaboliteIDs(String query, Statement statement) throws SQLException{
		
		ArrayList<String> list = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery(query);

		while(rs.next()){
			list.add(rs.getString(1));
		}
		rs.close();
		return list;
	}
	
	/**
	 * Get metaboliteID for a given query.
	 * @param query
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int getMetaboliteID(String query, Statement statement) throws SQLException{
		
		int result = -1;
		
		ResultSet rs = statement.executeQuery(query);

		while(rs.next()){
			result = rs.getInt(1);
		}
		rs.close();
		return result;
	}
	
	/**
	 * Get data from transport_types.
	 * @param uniprotID
	 * @param statement
	 * @return Set<String>
	 * @throws SQLException
	 */
	public static Set<String> getTransportTypeID(String uniprotID, Statement statement) throws SQLException {
		
		Set<String> result = new TreeSet<String>();

		ResultSet rs = statement.executeQuery("SELECT transport_types.id FROM transport_types " +
				"INNER JOIN transport_systems ON transport_types.id = transport_type_id " +
				"INNER JOIN tc_numbers_has_transport_systems ON transport_systems.id = tc_numbers_has_transport_systems.transport_system_id " +
				"INNER JOIN tcdb_registries ON (tcdb_registries.tc_number = tc_numbers_has_transport_systems.tc_number AND tcdb_registries.tc_version = tc_numbers_has_transport_systems.tc_version)" +
				"WHERE uniprot_id='"+uniprotID+"' AND latest_version");

		while(rs.next())
			result.add(rs.getString(1));

		rs.close();
		return result;
	}
	
	/**
	 * Get data from transported_metabolites_directions table for a given uniprotID.
	 * @param uniprotID
	 * @param statement
	 * @return Set<String>
	 * @throws SQLException
	 */
	public static Set<String> getMetabolitesID(String uniprotID, Statement statement) throws SQLException{

		Set<String> result = new TreeSet<String>();

		ResultSet rs = statement.executeQuery("SELECT metabolite_id FROM transported_metabolites_directions " +
				"INNER JOIN tc_numbers_has_transport_systems ON transported_metabolites_directions.transport_system_id = tc_numbers_has_transport_systems.transport_system_id " +
				"INNER JOIN tcdb_registries ON (tcdb_registries.tc_number = tc_numbers_has_transport_systems.tc_number AND tcdb_registries.tc_version = tc_numbers_has_transport_systems.tc_version)" +
				"WHERE uniprot_id='"+uniprotID+"' AND latest_version");

		while(rs.next())
			result.add(rs.getString(1));

		rs.close();
		return result;
	}
	
	/**
	 * Get data from transported_metabolites_directions table for a given metabolitesID.
	 * @param metabolitesID
	 * @param statement
	 * @return Set<String>
	 * @throws SQLException
	 */
	public static Set<String> getTransportTypesID(String metabolitesID, Statement statement) throws SQLException{
		
		Set<String> result = new TreeSet<String>();
		
		ResultSet rs = statement.executeQuery("SELECT transport_type.id FROM transported_metabolites_direction " +
				"INNER JOIN transport_system ON transport_system_id=transport_system.id " +
				"INNER JOIN transport_type ON transport_type_id=transport_type.id " +
				"WHERE metabolites_id='"+metabolitesID+"'");

		while(rs.next())
			result.add(rs.getString(1));

		rs.close();
		return result;
	}
	
	/**
	 * Get reversibility from transport_systems table for a given id.
	 * @param transportSystemID
	 * @param statement
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean getReversibility(int transportSystemID, Statement statement) throws SQLException{
		
		boolean result = false;
		
		ResultSet rs = statement.executeQuery("SELECT reversible FROM transport_systems WHERE id = "+transportSystemID);

		if(rs.next())
			result = rs.getBoolean(1);

		rs.close();
		return result;
	
	}
	
	/** 
	 * Get data from transported_metabolites_directions table for a given transportSystemID.
	 * @param transportSystemID
	 * @param statement
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getTmdscData(int transportSystemID, Statement statement) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery("SELECT metabolites.name, direction, stoichiometry, reversible, "
				+ "kegg_name, chebi_name, synonyms.name FROM transported_metabolites_directions " +
				"INNER JOIN metabolites ON metabolites.id = transported_metabolites_directions.metabolite_id " +
				"INNER JOIN synonyms ON metabolites.id = synonyms.metabolite_id " +
				"INNER JOIN directions ON directions.id = direction_id " +
				"INNER JOIN transport_systems ON transport_systems.id = transport_system_id " +
				"WHERE transport_system_id ="+transportSystemID);

		while(rs.next()){
			String[] list = new String[7];

			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);
			list[3]=rs.getBoolean(4)+"";
			list[4]=rs.getString(5);
			list[5]=rs.getString(6);
			list[6]=rs.getString(7);
			
			result.add(list);
		}
		rs.close();
		return result;
	}
	
	/**
	 * Get transport_systemsID.
	 * @param metabolitesName
	 * @param typeID
	 * @param statement
	 * @return Set<Integer>
	 * @throws SQLException
	 */
	public static Set<Integer> getTransporterIDs(String metabolitesName, int typeID, Statement statement) throws SQLException{
		
		Set<Integer> result = new TreeSet<Integer>();
		
		ResultSet rs = statement.executeQuery("SELECT transport_systems.id FROM transport_systems " +
				" INNER JOIN transported_metabolites_directions ON (transport_systems.id = transport_system_id ) " +
				" INNER JOIN metabolites ON metabolites.id= transported_metabolites_directions.metabolite_id " +
				" INNER JOIN synonyms ON transported_metabolites_directions.metabolite_id= synonyms.metabolite_id " +
				" INNER JOIN directions on transported_metabolites_directions.direction_id=directions.id " +
				" WHERE (" +
				" UPPER(metabolites.name) = UPPER('"+metabolitesName+"') OR " +
				" UPPER(synonyms.name) = UPPER('"+metabolitesName+"') OR " +
				" UPPER(kegg_name) = UPPER('"+metabolitesName+"') OR " +
				" UPPER(chebi_name) = UPPER('"+metabolitesName+"')" +
				")" +
				" AND direction <> 'reactant' " +
				" AND direction <> 'product' " +
				" AND transport_type_id = "+typeID );

		while(rs.next())
			result.add(rs.getInt(1));

		rs.close();
		return result;
	}
	
	/**
	 * Check data from MetabolitesOntology table.
	 * @param query
	 * @param query2
	 * @param query3
	 * @param statement
	 * @throws SQLException
	 */
	public static void selectIdFromMetabolitesOntology(String query, String query2, String query3, Statement statement) throws SQLException{
		
		ResultSet rs = statement.executeQuery(query);

		if(!rs.next()) {

			rs = statement.executeQuery(query2);

			if(!rs.next())
				statement.execute(query3);
		}
	}

	/**
	 * Insert data into generalEquation table.
	 * @param query
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int insertIntoGeneralEquation(String query, Statement statement) throws SQLException{

		int result = 0;
		
		statement.executeQuery(query);

		ResultSet rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
		if(rs.next())
			result =  rs.getInt(1);

		rs.close();
		return result;
	}
	
	/**
	 * Get loaded genes.
	 * @param query
	 * @param statement
	 * @return Set<String>
	 * @throws SQLException
	 */
	public static Set<String> getLoadedGenes(String query, Statement statement) throws SQLException {
		
		Set<String> result = new HashSet<String>();

		ResultSet rs = statement.executeQuery(query);

		while(rs.next())
			result.add(rs.getString(1));

		rs.close();
		return result;
	}
	
	/**
	 * Get loaded transporters.
	 * @param query
	 * @param statement
	 * @return Set<String>
	 * @throws SQLException
	 */
	public static Set<String> getLoadedTransporters(String query, Statement statement) throws SQLException {
		
		Set<String> result = new HashSet<String>();

		ResultSet rs = statement.executeQuery(query);

		while(rs.next())
			result.add(rs.getString(1)+"__"+rs.getString(2));

		rs.close();
		return result;
	}
	
	/**
	 * Update project version.
	 * @param version
	 * @param genomeID
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int updateProjectVersion(int version, int genomeID, Statement statement) throws SQLException {
		
		ResultSet rs = statement.executeQuery("SELECT id, version FROM projects WHERE latest_version AND organism_id="+genomeID+";");

		if(rs.next()) {

			version = rs.getInt(2)+1;
			statement.execute("UPDATE projects SET latest_version=false WHERE id= "+rs.getString(1));
		}
		rs.close();
		return version;
	}
	
	/**
	 * Insert data into projects table.
	 * @param query
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int insertIntoProjects(String query, Statement statement) throws SQLException{

		int projectID = 0;
		
		statement.executeQuery(query);

		ResultSet rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
		if(rs.next())
			projectID = rs.getInt(1);
		
		rs.close();
		return projectID;
	}
	
	/**
	 * Trace back reaction annotation.
	 * @param projectID
	 * @param locusTag
	 * @param statement
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> tracebackReactionAnnotation(int projectID, String locusTag, Statement statement) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery("SELECT genes_has_tcdb_registries.uniprot_id, tc_numbers_has_transport_systems.tc_number, metabolites.name, similarity, equation "+
				"FROM genes "+
				"INNER JOIN genes_has_tcdb_registries ON gene_id = genes.id "+
				"INNER JOIN tcdb_registries ON genes_has_tcdb_registries.uniprot_id = tcdb_registries.uniprot_id AND genes_has_tcdb_registries.version = tcdb_registries.version "+
				"INNER JOIN tc_numbers_has_transport_systems ON tcdb_registries.tc_number = tc_numbers_has_transport_systems.tc_number AND tcdb_registries.tc_version = tc_numbers_has_transport_systems.tc_version "+
				"INNER JOIN tc_numbers ON tcdb_registries.tc_number = tc_numbers.tc_number AND tcdb_registries.tc_version = tc_numbers.tc_version "+
				"INNER JOIN general_equation ON tc_numbers.general_equation_id = general_equation.id "+
				"INNER JOIN transported_metabolites_directions ON transported_metabolites_directions.transport_system_id = tc_numbers_has_transport_systems.transport_system_id "+
				"INNER JOIN metabolites ON metabolite_id = metabolites.id "+
				"WHERE project_id = "+projectID+" AND locus_tag = '"+locusTag+"';");

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
	 * Get projectID.
	 * @param projectID
	 * @param genomeID
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int getProjectID(int projectID, int genomeID, Statement statement) throws SQLException{

		ResultSet rs = statement.executeQuery("SELECT id FROM projects WHERE latest_version AND organism_id="+genomeID+";");

		if(rs.next()) {

			projectID = rs.getInt(1);
		}
		rs.close();
		return projectID;
	}
	
	/**
	 * Get all projectsIDs.
	 * @param genomeID
	 * @param statement
	 * @return Set<Integer>
	 * @throws SQLException
	 */
	public static Set<Integer> getAllProjectIDs(int genomeID, Statement statement) throws SQLException{
		
		Set<Integer> projectIDs = new TreeSet<Integer>();

		ResultSet rs = statement.executeQuery("SELECT id FROM projects WHERE organism_id="+genomeID+";");

		while(rs.next()) {
			projectIDs.add(rs.getInt(1));
		}

		rs.close();
		return projectIDs;
	}
	
	/**
	 * Get gene status for a given id.
	 * @param geneID
	 * @param statement
	 * @return String
	 * @throws SQLException
	 */
	public static String getGeneStatus(String geneID, Statement statement) throws SQLException{
		
		String result = "";

		ResultSet rs = statement.executeQuery("SELECT status FROM genes WHERE id = "+geneID+";");

		while(rs.next()) {
			result = rs.getString(1);
		}

		rs.close();
		return result;
	}
	
	/**
	 * Get data from genes_has_tcdb_registries table. 
	 * @param statement
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> selectGeneIdAndUniprotId(Statement statement) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery("SELECT gene_id, uniprot_id FROM genes_has_tcdb_registries;");
		
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
	 * Get data from tcdb_registries table.
	 * @param statement
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getUniprotVersion(Statement statement) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery("SELECT uniprot_id, version FROM tcdb_registries WHERE latest_version;");
		
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
	 * Get data from tcdb_registries table.
	 * @param statement
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getOrganismsTaxonomyScore(Statement statement) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery("SELECT uniprot_id, organism, taxonomy FROM tcdb_registries " +
				" INNER JOIN tc_numbers ON (tcdb_registries.tc_number = tc_numbers.tc_number AND tcdb_registries.tc_version = tc_numbers.tc_version)" +
				" INNER JOIN taxonomy_data ON (taxonomy_data.id = taxonomy_data_id)" +
				" WHERE latest_version;");

		
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
	 * @param result
	 * @param query
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int loadGeneralEquation(int result, String query, Statement statement) throws SQLException{

		statement.executeQuery(query);

		ResultSet rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
		if(rs.next())
			result =  rs.getInt(1);

		rs.close();
		return result;
	}
	
	/**
	 * Get data from sw_reports table.
	 * @param projectID
	 * @param statement
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getCandidatesFromDatabase(int projectID, Statement statement) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery("SELECT sw_reports.id, locus_tag, similarity, acc, tcdb_id FROM sw_reports " +
				" INNER JOIN sw_similarities ON sw_reports.id=sw_similarities.sw_report_id " +
				" INNER JOIN sw_hits ON sw_hits.id=sw_similarities.sw_hit_id " +
				" WHERE project_id = "+ projectID +
				" ORDER BY sw_reports.locus_tag, similarity DESC");
		
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
	 * Get data from tcdb_registries.
	 * @param statement
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getLatestVersion(Statement statement) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery("SELECT uniprot_id, tc_number, latest_version FROM tcdb_registries;");
		
		while(rs.next()){
			String[] list = new String[3];

			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getBoolean(3)+"";
			
			result.add(list);
		}
		rs.close();
		return result;
	}
	
	/**
	 * Get all data from metabolites table.
	 * @param statement
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getAllMetabolitesData(Statement statement) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery("SELECT * FROM metabolites");
		
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
	 * Get all data from synonyms table.
	 * @param statement
	 * @return  ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getAllSynonymsData(Statement statement) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery("SELECT * FROM synonyms JOIN metabolites ON (metabolite_id=metabolites.id);");
		
		while(rs.next()){
			String[] list = new String[9];

			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);
			list[3]=rs.getString(4);
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
	 * Get all transport types data.
	 * @param statement
	 * @return Map<Integer, String[]>
	 * @throws SQLException
	 */
	public static Map<Integer, String[]> getAllTransportTypesData(Statement statement) throws SQLException{
		
		Map<Integer, String[]> result = new HashMap<Integer, String[]>();
		
		ResultSet rs = statement.executeQuery("SELECT * FROM synonyms JOIN metabolites ON (metabolite_id=metabolites.id);");
		
		while(rs.next())
			result.put(rs.getInt(1), new String[] {rs.getString(2), rs.getString(3)});
		
		rs.close();
		return result;
	}
	
	/**
	 * @param statement
	 * @return List<Integer>
	 * @throws SQLException
	 */
	public static List<Integer> getTransportTypeID(Statement statement) throws SQLException{
		
		List<Integer> result = new ArrayList<Integer>();
		
		ResultSet rs = statement.executeQuery("SELECT DISTINCT transport_type_id FROM transport_systems WHERE NOT reversible;");
		
		while(rs.next())
			result.add(rs.getInt(1));
		
		rs.close();
		return result;
	}
	
	/**
	 * Get data from TransportTypes table.
	 * @param transportTypeNewId
	 * @param query
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int getFromTransportTypes(int transportTypeNewId, String query, Statement statement) throws SQLException{

		ResultSet rs = statement.executeQuery(query);

		if(rs.next()) 
			transportTypeNewId = rs.getInt(1);

		rs.close();
		return transportTypeNewId;
	}
	
	/**
	 * insert data into TransportTypes table.
	 * @param transportTypeNewId
	 * @param query
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int insertIntoTransportTypes(int transportTypeNewId, String query, Statement statement) throws SQLException{

		statement.executeQuery(query);

		ResultSet rs = statement.executeQuery("SELECT LAST_INSERT_ID();");
		if(rs.next())				
			transportTypeNewId = rs.getInt(1);
		
		rs.close();
		return transportTypeNewId;
	}
	
	/**
	 * Get the number of genes.
	 * @param stmt
	 * @return	String with the number of genes.
	 * @throws SQLException
	 */
	public static String getGenesCount(Statement stmt) throws SQLException{

		String genes = null;
		
		ResultSet rs = stmt.executeQuery("SELECT count(distinct(gene_id)) gid FROM genes_has_tcdb_registries;");
		
		while(rs.next())
			genes=rs.getString("gid");
		
		rs.close();
		return genes;
	}
	
	/**
	 * Get the number of metabolites.
	 * @param stmt
	 * @return String with the number of metabolites.
	 * @throws SQLException
	 */
	public static String getMetabolitesCount(Statement stmt) throws SQLException{

		String metabolites = null;
		
		ResultSet rs = stmt.executeQuery("SELECT count(distinct(metabolite_id)) met FROM genes_has_metabolites;");
		
		while(rs.next())
			metabolites=rs.getString("met");
		
		rs.close();
		return metabolites;
	}
	
	/**
	 * Get the number of uniprotIDs.
	 * @param stmt
	 * @return String with the number of uniprotIDs.
	 * @throws SQLException
	 */
	public static String getUniprotIDsCount(Statement stmt) throws SQLException{

		String uniprotID = null;
		
		ResultSet rs = stmt.executeQuery("SELECT count(distinct(uniprot_id)) uid FROM genes_has_tcdb_registries;");
		
		while(rs.next())
			uniprotID=rs.getString("uid");
		
		rs.close();
		return uniprotID;
	}
	
	/**
	 * Get the number of tcNumbers.
	 * @param stmt
	 * @return String with the number of tcNnumbers.
	 * @throws SQLException
	 */
	public static String getTcNumbersCount(Statement stmt) throws SQLException{

		String tcNumbers = null;
		
		ResultSet rs = stmt.executeQuery("SELECT count(distinct(tr.tc_number)) tc FROM tcdb_registries tr "
				+ "INNER JOIN genes_has_tcdb_registries gtr ON tr.uniprot_id=gtr.uniprot_id "
				+ "AND tr.version=gtr.version;");
		
		while(rs.next())
			tcNumbers=rs.getString("tc");
		
		rs.close();
		return tcNumbers;
	}
	
	/**
	 * Get metabolites.
	 * @param stmt
	 * @return Map<String, String>
	 * @throws SQLException
	 */
	public static Map<String, String> getMetabolites(Statement stmt) throws SQLException{

		Map<String, String> mets = new HashMap<>();
		
		ResultSet rs = stmt.executeQuery("SELECT locus_tag, count(distinct(metabolite_id)) FROM genes_has_metabolites "+
				" INNER JOIN genes ON (genes.id = gene_id) " +
				" INNER JOIN metabolites ON (metabolites.id = metabolite_id) " +
				" GROUP BY locus_tag " +
				" ORDER BY gene_id;");
		
		while(rs.next())
			mets.put(rs.getString("locus_tag"), rs.getString(2));
		
		rs.close();
		return mets;
	}
	
	/**
	 * Gets TCfamily.
	 * @param stmt
	 * @return Map<String, String>
	 * @throws SQLException
	 */
	public static Map<String, String> getTcFamily(Statement stmt) throws SQLException{

		Map<String, String>  tcn = new HashMap<>();
		
		ResultSet rs = stmt.executeQuery("SELECT locus_tag, tc_family, SUM(similarity) as score FROM genes " +
				" INNER JOIN genes_has_tcdb_registries ON (gene_id = genes.id)" +
				" INNER JOIN tcdb_registries ON (genes_has_tcdb_registries.uniprot_id = tcdb_registries.uniprot_id AND genes_has_tcdb_registries.version = tcdb_registries.version)" +
				" INNER JOIN tc_numbers ON (tcdb_registries.tc_number = tc_numbers.tc_number AND tc_numbers.tc_version = tcdb_registries.tc_version)" +
				" GROUP BY locus_tag, tc_family" +
				" ORDER BY gene_id;");
		
		double max = 0;
		while(rs.next()) {

			if(tcn.containsKey(rs.getString("locus_tag"))) {
				
				if (rs.getDouble("score")>max) {
				
					tcn.put(rs.getString("locus_tag"), rs.getString("tc_family"));
					max = rs.getDouble("score");
				}
			}
			else {
				
				max = 0;
				tcn.put(rs.getString("locus_tag"), rs.getString("tc_family"));
			}
		}
		
		rs.close();
		return tcn;
	}
	
	/**
	 * gets Transporters data from sw_transporters table.
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getTransportersData(Statement stmt) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = stmt.executeQuery("SELECT id as gene_id, sw_transporters.locus_tag, number_TMD, sw_transporters.tcdb_id,"
				+ " sw_transporters.acc FROM sw_transporters LEFT JOIN sw_reports ON sw_reports.locus_tag=sw_transporters.locus_tag "
				+ " GROUP BY sw_reports.locus_tag AND sw_transporters.tcdb_id "
				+ " ORDER BY sw_transporters.locus_tag "//, sw_transporters.similarity "
				+ " desc;");
		
		while(rs.next()){
			String[] list = new String[5];

			list[0]=rs.getString(1); // gene_id
			list[1]=rs.getString(2); // locus_tag
			list[2]=rs.getString(3); // number_TMD
			list[3]=rs.getString(4); // tcdb_id
			list[4]=rs.getString(5); // accession
			
			result.add(list);
		}
		rs.close();
		return result;
	}
	
	/**
	 * Gets gene total score.
	 * @param stmt
	 * @return double
	 * @throws SQLException
	 */
	public static double getGeneTotalScore(String id, Statement stmt) throws SQLException{
		
		double totalScore = 0;
		
		ResultSet rs = stmt.executeQuery("SELECT gene_id, SUM(similarity) FROM genes_has_tcdb_registries "
				+ " LEFT JOIN genes ON (genes.id = gene_id) "
				+ " WHERE locus_tag = '"+ id +"' GROUP BY gene_id;");
		
		if(rs.next())
			totalScore = rs.getDouble(2);

		rs.close();
		return totalScore;
	}
	
	/**
	 * @param id
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getMetaboliteFrequencyScore(String id, Statement stmt) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = stmt.executeQuery("SELECT metabolite_id, gene_id, similarity_score_sum, taxonomy_score_sum, frequency FROM genes_has_metabolites "
				+ " LEFT JOIN genes ON (genes.id = gene_id) "
				+ "  WHERE locus_tag = '"+ id +"' GROUP BY metabolite_id");
		
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
	 * Get metabolite total score.
	 * @param id
	 * @param stmt
	 * @return Map<String, Double>
	 * @throws SQLException
	 */
	public static Map<String, Double> getMetaboliteTotalScore(String id, Statement stmt) throws SQLException{

		Map<String, Double> metaboliteTotalScore = new HashMap<>();
		
		ResultSet rs = stmt.executeQuery("SELECT metabolite_id, SUM(similarity) FROM genes_has_tcdb_registries "
				+ " INNER JOIN genes_has_metabolites ON genes_has_tcdb_registries.gene_id=genes_has_metabolites.gene_id "
				+ " INNER JOIN genes ON genes.id = genes_has_tcdb_registries.gene_id "
				+ " INNER JOIN tcdb_registries ON (genes_has_tcdb_registries.uniprot_id=tcdb_registries.uniprot_id AND genes_has_tcdb_registries.version=tcdb_registries.version) "
				+ " WHERE locus_tag = '"+ id +"' GROUP BY genes_has_tcdb_registries.gene_id, metabolite_id;");
		
		while(rs.next())
			metaboliteTotalScore.put(rs.getString(1), rs.getDouble(2));
		
		rs.close();
		return metaboliteTotalScore;
	}
	
	/**
	 * @param id
	 * @param stmt
	 * @return ArrayList<String[]> 
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getTransportTypeScore(String id, Statement stmt) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = stmt.executeQuery("SELECT metabolite_id, gene_id, transport_type_id, transport_type_score_sum, taxonomy_score_sum, "
				+ "frequency, name FROM genes_has_metabolites_has_type "
				+ " LEFT JOIN genes ON (genes.id = gene_id) "
				+ " LEFT JOIN transport_types ON (transport_types.id = transport_type_id)"
				+ " WHERE locus_tag = '"+ id +"';");
		
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
	 * @param id
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getMetabolitesDirectionAndReversibility(String id, Statement stmt) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = stmt.executeQuery( "SELECT gene_id, transport_type, metabolite_id, metabolite_name, direction, reversible, sum(similarity) as sum_s, kegg_miriam "
				+ " FROM gene_to_metabolite_direction "
				+ " WHERE locus_tag = '"+ id +"' "
				+ " GROUP BY metabolite_id, transport_type, reversible, direction;");
		
		while(rs.next()){
			String[] list = new String[8];

			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);
			list[3]=rs.getString(4);
			list[4]=rs.getString(5);
			list[5]=rs.getBoolean(6)+"";
			list[6]=rs.getString(7);
			list[7]=rs.getString(8);
			
			result.add(list);
		}
		rs.close();
		return result;
	}
	
	/**
	 * @param id
	 * @param stmt
	 * @return Integer[]
	 * @throws SQLException
	 */
	public static Integer[] getStats(String table, Statement stmt) throws SQLException{
		
		Integer[] list = new Integer[3];
		
		int num=0;
		int noname=0;
		int compounds=0;
		
		ResultSet rs = stmt.executeQuery("SELECT * FROM "+ table);

		while(rs.next()) {
			
			num++;
			if(rs.getString(2)==null) noname++;
			else{compounds++;}
		}

		list[0] = num;
		list[1] = noname;
		list[2] = compounds;
		
		rs.close();
		return list;
	}
	
	/**
	 * Get all data from sw_transporters table.
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getSwTransportersData(Statement stmt) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = stmt.executeQuery("SELECT * FROM sw_transporters");
		
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
	
	public static Map<String, DatabaseReactionContainer> getDataFromReactionForTransp(Statement statement) throws SQLException{
		
		Map<String, DatabaseReactionContainer> reactionsMap = new HashMap<>();
		
		ResultSet rs = statement.executeQuery("SELECT name, equation, reversible, inModel, isGeneric, isSpontaneous, "
				+ "isNonEnzymatic, source, idreaction, lowerBound, upperBound, notes " +
				" FROM reaction WHERE source = 'TRANSPORTERS' AND originalReaction;");
		
		while (rs.next()) {

			String name = rs.getString(1);
			String equation = rs.getString(2);
			boolean reversible = rs.getBoolean(3);
			boolean inModel = rs.getBoolean(4);
			boolean isGeneric = rs.getBoolean(5);
			boolean isSpontaneous = rs.getBoolean(6);
			boolean isNonEnzymatic = rs.getBoolean(7);
			String source = rs.getString(8);
			String id = rs.getString(9);
			String lowerBound = rs.getString(10); 
			String upperBound = rs.getString(11);
			String notes ="";
			if(rs.getString(12)!= null)
				notes = rs.getString(12);

			DatabaseReactionContainer drc = new DatabaseReactionContainer(id, name, equation, source, reversible, inModel, isGeneric, isSpontaneous, isNonEnzymatic);
			drc.setUpperBound(upperBound);
			drc.setLowerBound(lowerBound);
			drc.setNotes(notes);
			reactionsMap.put(id, drc);
		}
		
		rs.close();
		return reactionsMap;
	}
	
	/**
	 * Get reactionIDs and ecNumbers WHERE source <> 'TRANSPORTERS'.
	 * @param statement
	 * @return ArrayList<String[]> 
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getTransportReactions(Statement statement) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery("SELECT idreaction, enzyme_ecnumber, enzyme_protein_idprotein FROM reaction " +
				"INNER JOIN reaction_has_enzyme ON reaction_has_enzyme.reaction_idreaction = idreaction " +
				"WHERE (source = 'TRANSPORTERS' AND reaction_has_enzyme.enzyme_ecnumber IS NOT NULL AND originalReaction)");
		
		while(rs.next()){
			String[] list = new String[12];

			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);
			
			
			result.add(list);
		}
		rs.close();
		return result;
	}
	
	/**
	 * Get all from stoichiometry 'WHERE source = 'TRANSPORTERS' AND originalReaction'.
	 * @param statement
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getAllFromStoichiometry(Statement statement) throws SQLException{
		
		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery("SELECT * FROM stoichiometry "
				+ " INNER JOIN reaction ON stoichiometry.reaction_idreaction = reaction.idreaction " +
				" WHERE source = 'TRANSPORTERS' AND originalReaction;");

		while(rs.next()){
			String[] list = new String[12];

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
	 * get reaction reversibilities WHERE source = 'TRANSPORTERS' AND reversible = TRUE.
	 * @param statement
	 * @return List<String>
	 * @throws SQLException
	 */
	public static List<String> getReactionReversibility(Statement statement) throws SQLException{
		
		List<String> reactionReversibility = new ArrayList<String>();
		
		ResultSet rs = statement.executeQuery("SELECT idreaction FROM reaction WHERE source = 'TRANSPORTERS' AND reversible;");

		while(rs.next())
			reactionReversibility.add(rs.getString(1));
		
		rs.close();
		return reactionReversibility;
	}
	
	/** Check if reaction table has entries for source <> 'TRANSPORTERS' AND NOT reversible.
	 * @param stmt
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkReactionNotReversible(Statement stmt) throws SQLException{
		
		boolean exists = false;
		
		ResultSet rs = stmt.executeQuery("SELECT * FROM reaction WHERE source <> 'TRANSPORTERS' AND NOT reversible");
		
		if(rs.next())
			exists = true;
		
		rs.close();
		return exists;
	}
	
	
	/**
	 * Method for retrieving gene identifiers on the transport alignments.
	 * 
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, Integer> getTransportAlignmentGenes(Statement statement) throws SQLException{

		Map<String, Integer> processedGenes  = new HashMap<>();

		ResultSet rs = statement.executeQuery("SELECT locus_tag, id FROM sw_reports;");

		while(rs.next())
			processedGenes.put(rs.getString(1), rs.getInt(2));

		return processedGenes;
	}
	
	/**
	 * Loads data about the transporter
	 * 
	 * @param alignmentContainer
	 * @param geneID
	 * @param stmt
	 * @throws SQLException
	 */
	public static void loadTransportInfo (AlignmentCapsule alignmentContainer, int geneID, Statement stmt) throws SQLException {
		
		ResultSet rs = stmt.executeQuery("SELECT id FROM sw_hits WHERE tcdb_id='"+alignmentContainer.getTcdbID()+"' AND acc='"+alignmentContainer.getTarget()+"'");

		if(!rs.next()) {

			stmt.execute("INSERT INTO sw_hits (acc,tcdb_id) VALUES ('"+ alignmentContainer.getTarget() +"', '"+ alignmentContainer.getTcdbID() +"')");
			rs = stmt.executeQuery("SELECT LAST_INSERT_ID();");
			rs.next();
		}

		int idHIT = rs.getInt(1);

		rs = stmt.executeQuery("SELECT * FROM sw_similarities WHERE sw_report_id="+geneID+" AND sw_hit_id="+idHIT+"");

		if(!rs.next()) {

			stmt.execute("INSERT INTO sw_similarities (sw_report_id,sw_hit_id,similarity) VALUES("+geneID+","+idHIT+","+alignmentContainer.getScore()+")");
		}

		rs.close();
	}
	
	/**
	 * Method to delete all entry from reaction where source = 'TRANSPORTERS'.
	 * 
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static void cleanIntegration(Statement statement) throws SQLException{


		statement.execute("DELETE FROM reaction WHERE source = 'TRANSPORTERS';");

	}
	
	/**
	 * Check the existence of transporters reactions.
	 * 
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static boolean checkTransporters(Statement statement) throws SQLException{
	
		ResultSet rs = statement.executeQuery("SELECT * FROM reaction WHERE source = 'TRANSPORTERS';");

		if(rs.next())
			return true;
		
		rs.close();
		return false;
	}
	
}
