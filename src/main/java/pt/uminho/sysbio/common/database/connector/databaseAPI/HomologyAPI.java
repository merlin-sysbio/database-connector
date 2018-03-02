package pt.uminho.sysbio.common.database.connector.databaseAPI;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.h2.jdbc.JdbcSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException;

import pt.uminho.sysbio.common.database.connector.datatypes.Connection;
import pt.uminho.sysbio.common.database.connector.datatypes.DatabaseAccess;
import pt.uminho.sysbio.common.database.connector.datatypes.DatabaseUtilities;
import pt.uminho.sysbio.common.database.connector.datatypes.Enumerators.DatabaseType;
import pt.uminho.sysbio.merlin.utilities.containers.capsules.AlignmentCapsule;


/**
 * @author Oscar Dias
 *
 */
public class HomologyAPI {

	final static Logger logger = LoggerFactory.getLogger(HomologyAPI.class);

	//		/**
	//		 * Get gene locus from homology data.
	//		 * 
	//		 * @param sequence_id
	//		 * @param statement
	//		 * @param informationType 
	//		 * @return
	//		 * @throws SQLException
	//		 */
	//		public static Pair<String,String> getGeneLocusFromHomologyData (String sequence_id, Statement statement) throws SQLException {
	//	
	//			String locusTag = sequence_id, name = null;
	//	
	//			ResultSet rs = statement.executeQuery("SELECT locusTag, gene FROM geneHomology WHERE query = '"+sequence_id+"';");
	//	
	//			locusTag = sequence_id;
	//			
	//			while(rs.next()) {
	//	
	//				if(!rs.getString(1).equalsIgnoreCase(sequence_id)) {
	//				
	//					locusTag = rs.getString(1);
	//					name = rs.getString(2);
	//				}
	//			}
	//	
	//			Pair <String, String> ret = new Pair<>(locusTag, name);
	//			return ret;
	//		}

	/**
	 * Retrieve mapping of queries to database ids.
	 * 
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, Integer> getLocusKeys(Statement statement) throws SQLException {

		Map<String, Integer> ret = new HashMap<>();

		ResultSet rs = statement.executeQuery("SELECT s_key, query FROM geneHomology;");

		while(rs.next())
			ret.put(rs.getString(2), rs.getInt(1));

		return ret;
	}

	/**
	 *Retrieve homology availabilities for gene key.
	 * 
	 * @param key
	 * @param statement
	 * @return
	 * @throws SQLException 
	 */
	public static Map<String, Boolean> getHomologyAvailabilities(int key, Statement statement) throws SQLException {

		ResultSet rs1=statement.executeQuery("SELECT query FROM geneHomology WHERE geneHomology.s_key = '" + key+"' ");

		String query = "";

		while (rs1.next())
			query = rs1.getString(1);

		boolean gene_hmmerAvailable = false, gene_blastPAvailable = false, gene_blastXAvailable = false;

		ResultSet rset = statement.executeQuery("SELECT program" +
				" FROM homologySetup " +
				"INNER JOIN geneHomology ON (homologySetup.s_key = homologySetup_s_key) " +
				"WHERE query = '" + query +"' ");

		while (rset .next()) {

			if(rset.getString(1).equalsIgnoreCase("hmmer"))
				gene_hmmerAvailable = true;

			if(rset.getString(1).equalsIgnoreCase("ncbi-blastp")
					|| rset.getString(1).equalsIgnoreCase("blastp")
					|| rset.getString(1).equalsIgnoreCase("ebi-blastp"))
				gene_blastPAvailable = true;

			if(rset.getString(1).equalsIgnoreCase("ncbi-blastx")
					|| rset.getString(1).equalsIgnoreCase("blastx")
					|| rset.getString(1).equalsIgnoreCase("ebi-blastx"))
				gene_blastXAvailable = true;
		}

		Map<String, Boolean> ret = new HashMap<>();
		ret.put("gene_blastXAvailable", gene_blastXAvailable);
		ret.put("gene_blastPAvailable", gene_blastPAvailable);
		ret.put("gene_hmmerAvailable", gene_hmmerAvailable);

		return ret;
	}

	/**
	 *Retrieve InterPro availability for gene key.
	 * 
	 * @param key
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static boolean getInterproAvailability(int key, Statement statement) throws SQLException {

		ResultSet rs1=statement.executeQuery("SELECT query FROM geneHomology WHERE geneHomology.s_key = '" + key+"' ");

		String query = "";

		while (rs1.next())
			query = rs1.getString(1); 

		ResultSet rset = statement.executeQuery("SELECT *" +
				" FROM interpro_results " +
				" WHERE query = '" + query +"' ");

		return rset .next();

	}


	/**
	 * Get homology results for gene key.
	 * 
	 * @param key
	 * @param statement
	 * @param string3 
	 * @param string2 
	 * @param string 
	 * @return
	 * @throws SQLException
	 */
	public static List< ArrayList<String>> getHomologyResults(int key, Statement statement, String tool1, String tool2, String tool3) throws SQLException {

		ResultSet rs1=statement.executeQuery("SELECT query FROM geneHomology WHERE geneHomology.s_key = '" + key+"' ");

		String query = "";

		while (rs1.next())
			query = rs1.getString(1); 

		List< ArrayList<String>> result = new ArrayList<>();

		ArrayList<String> ql = null;

		String previous_homology_s_key="";
		String ecnumber="";
		ql = new ArrayList<String>();

		ResultSet resultSet = statement.executeQuery("SELECT referenceID, locusID, organism, geneHomology_has_homologues.eValue, bits, product," +
				" homologues.s_key, ecnumber, homologues.uniprot_star, program " +
				" FROM homologySetup " +
				" INNER JOIN geneHomology ON (homologySetup.s_key = homologySetup_s_key)" +
				" INNER JOIN geneHomology_has_homologues ON (geneHomology_s_key = geneHomology.s_key)" +
				" INNER JOIN homologues on (homologues.s_key = geneHomology_has_homologues.homologues_s_key)" +
				" INNER JOIN organism on (homologues.organism_s_key = organism.s_key)" +
				" LEFT JOIN homologues_has_ecNumber on (homologues.s_key = homologues_has_ecNumber.homologues_s_key)" +
				" LEFT JOIN ecNumber on (homologues_has_ecNumber.ecNumber_s_key = ecNumber.s_key)" +
				" WHERE query = '" + query +"' " +
				"AND (LOWER(program) LIKE '"+tool1+"' OR LOWER(program) LIKE '"+tool2+"' OR LOWER(program) LIKE '"+tool3+"')"
				+ " ORDER BY bits DESC, geneHomology_has_homologues.eValue ASC");

		boolean go = true;

		while(resultSet.next()) {

			go = false;

			String s_key = "";
			if(resultSet.getString(7) != null) {

				s_key = resultSet.getString(7);

				if(previous_homology_s_key.equals(s_key)) {

					ecnumber+=", "+resultSet.getString(8);
				}
				else {

					previous_homology_s_key=s_key;
					if(!ql.isEmpty()) {

						ql.add(ecnumber);
						result.add(ql);
					}
					ecnumber="";
					ql = new ArrayList<String>();
					ql.add(resultSet.getString(1));
					ql.add(resultSet.getString(2));
					ql.add(resultSet.getString(9));
					ql.add(resultSet.getString(3));
					ql.add(resultSet.getString(4));
					ql.add(resultSet.getString(5));
					ql.add(resultSet.getString(6));

					if(resultSet.getString(8)!=null)
						ecnumber=resultSet.getString(8);
				}

				if(resultSet.isLast()) {

					ql.add(ecnumber);
					result.add(ql);
				}		
			}
			else {

				ql = new ArrayList<String>();
				ql.add("");
				ql.add("");
				ql.add("-1");
				ql.add("");
				ql.add("");
				ql.add("");
				ql.add("");
				ql.add("");
				result.add(ql);
			}
		}

		if(go){

			ql = new ArrayList<String>();
			ql.add("");
			ql.add("");
			ql.add("-1");
			ql.add("");
			ql.add("");
			ql.add("");
			ql.add("");
			ql.add("");
			result.add(ql);
		}

		return result;
	}

	/**
	 * Retrieve homologies taxonomy for gene key. 
	 * 
	 * @param key
	 * @param statement
	 * @param tool3 
	 * @param tool2 
	 * @param tool1 
	 * @return
	 * @throws SQLException
	 */
	public static List< ArrayList<String>> getHomologyTaxonomy(int key, Statement statement, String tool1, String tool2, String tool3) throws SQLException {

		ResultSet rs1=statement.executeQuery("SELECT query FROM geneHomology WHERE geneHomology.s_key = '" + key+"' ");

		String query = "";

		while (rs1.next())
			query = rs1.getString(1); 

		List< ArrayList<String>> result = new ArrayList<>();

		ArrayList<String> ql = null;

		ResultSet resultSet = statement.executeQuery(						
				"SELECT organism,taxonomy, geneHomology_has_homologues.eValue " +
						"FROM homologySetup " +
						"INNER JOIN geneHomology ON (homologySetup.s_key = homologySetup_s_key)" +
						"INNER JOIN geneHomology_has_homologues ON (geneHomology_s_key = geneHomology.s_key)" +
						"INNER JOIN homologues ON (homologues_s_key = homologues.s_key)" +
						"INNER JOIN organism ON (organism_s_key = organism.s_key) " +
						"WHERE query = '" + query +"' " +
						"AND (LOWER(program) LIKE '"+tool1+"' OR LOWER(program) LIKE '"+tool2+"' OR LOWER(program) LIKE '"+tool3+"')"
						+ " ORDER BY bits DESC, geneHomology_has_homologues.eValue ASC");

		boolean go = true;

		while (resultSet.next()) {

			go = false;

			ql = new ArrayList<String>();
			ql.add(resultSet.getString(1));
			ql.add(resultSet.getString(2));
			result.add(ql);
		}

		if(go){
			ql = new ArrayList<String>();
			ql.add("");
			ql.add("");
			result.add(ql);
		}

		return result;
	}


	/**
	 * Retrieve homologies query sequence for gene key.  
	 * 
	 * @param key
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static String getHomologySequence(int key, Statement statement) throws SQLException {

		ResultSet rs1=statement.executeQuery("SELECT query FROM geneHomology WHERE geneHomology.s_key = '" + key+"' ");

		String query = "";

		while (rs1.next())
			query = rs1.getString(1);

		ResultSet rs5=statement.executeQuery(
				"SELECT DISTINCT(sequence) " +
						"FROM fastaSequence " +
						"INNER JOIN geneHomology ON (fastaSequence.geneHomology_s_key = geneHomology.s_key)" +
						"WHERE query = '" + query +"'");

		if (rs5.next())
			return rs5.getString(1);

		return null;
	}

	/**
	 * Retrieve homology setup for key.
	 * 
	 * @param key
	 * @param statement
	 * @return
	 * @throws SQLException 
	 */
	public static List< ArrayList<String>> getHomologySetup(int key, Statement statement) throws SQLException {

		ResultSet rs1=statement.executeQuery("SELECT query FROM geneHomology WHERE geneHomology.s_key = '" + key+"' ");

		String query = "";

		while (rs1.next())
			query = rs1.getString(1); 

		List< ArrayList<String>> result = new ArrayList<>();

		ArrayList<String> ql = null;

		ResultSet resultSet = statement.executeQuery(
				"SELECT program, version, databaseID, eValue, matrix, wordSize, gapCosts, maxNumberOfAlignments " +
						"FROM homologySetup " +
						"INNER JOIN geneHomology ON (homologySetup_s_key = homologySetup.s_key)" +
						"WHERE query = '" + query +"' " );

		while (resultSet.next()) {

			ql = new ArrayList<String>();
			ql.add(resultSet.getString(1));
			ql.add(resultSet.getString(2));
			ql.add(resultSet.getString(3));
			ql.add(resultSet.getString(4));
			ql.add(resultSet.getString(5));
			ql.add(resultSet.getString(6));
			ql.add(resultSet.getString(7));
			ql.add(resultSet.getString(8));
			result.add(ql);
		}
		return result;
	}

	/**
	 * Retrieve InterPro results for key.
	 * 
	 * @param key
	 * @param statement
	 * @return
	 * @throws SQLException 
	 */
	public static List< ArrayList<String>> getInterProResult(int key, Statement statement) throws SQLException {

		ResultSet rs1=statement.executeQuery("SELECT query FROM geneHomology WHERE geneHomology.s_key = '" + key+"' ");

		String query = "";

		while (rs1.next())
			query = rs1.getString(1); 

		List< ArrayList<String>> result = new ArrayList<>();

		ArrayList<String> ql = null;

		ResultSet resultSet = statement.executeQuery(
				"SELECT interpro_result.database, interpro_result.accession,  interpro_result.name, interpro_result.eValue, "
						+ " interpro_result.ec, interpro_result.name, interpro_result.goName, interpro_result.localization, interpro_entry.accession, "
						+ " interpro_entry.name, interpro_entry.description, "
						+ " interpro_location.start, interpro_location.end "
						+ " FROM interpro_results "
						+ " INNER JOIN interpro_result ON (interpro_results.id = interpro_result.results_id) "
						+ " INNER JOIN interpro_result_has_entry ON (interpro_result.id = interpro_result_has_entry.result_id) "
						+ " INNER JOIN interpro_entry ON (interpro_result_has_entry.entry_id = interpro_entry.id) "
						+ " INNER JOIN interpro_location ON (interpro_result.id = interpro_location.result_id) "
						+ " WHERE query = '" + query +"' " );

		while (resultSet.next()) {

			String out = "";
			int counter=0;
			ql = new ArrayList<String>();

			while (counter<13) {

				out = "";
				counter++;
				if(resultSet.getString(counter)!= null && !resultSet.getString(counter).equals("null"))
					out = resultSet.getString(counter);	
				ql.add(out);
			}
			counter = 1;
			result.add(ql);
		}

		return result;
	}


	/**
	 * Retrieve loaded InterPro annotations with the status processed.
	 * 
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static Set<String> getLoadedIntroProAnnotations(Statement statement) throws SQLException {

		Set<String> ret = new TreeSet<>();

		ResultSet rs = statement.executeQuery("SELECT * FROM interpro_results WHERE status = 'PROCESSED';");

		while(rs.next())
			ret.add(rs.getString(2));

		return ret;
	}

	/**
	 * Load InterPro annotation.
	 * 
	 * @param query
	 * @param querySequence
	 * @param mostLikelyEC
	 * @param mostLikelyLocalization
	 * @param name
	 * @param statement
	 * @param databaseType 
	 * @return
	 * @throws SQLException 
	 */
	public static int loadInterProAnnotation(String query, String querySequence, String mostLikelyEC,
			String mostLikelyLocalization, String name, Statement statement, DatabaseType databaseType) throws SQLException {


		if(name!= null) {

			int size = name.length();

			if (size>250)
				size=249;

			name = DatabaseUtilities.databaseStrConverter(name, databaseType).substring(0, size);
		}

		ResultSet rs = statement.executeQuery("SELECT * FROM interpro_results WHERE query = '"+query+"';");

		if(!rs.next()) {

			statement.execute("INSERT INTO interpro_results (query, querySequence, mostLikelyEc, mostLikelyLocalization, mostLikelyName, status) "
					+ "VALUES('"+query+"', '"+querySequence+"', '"+mostLikelyEC+"', '"+mostLikelyLocalization+"', '"+name+"','PROCESSING')");
			rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
			rs.next();
		}

		int ret = rs.getInt(1);
		rs.close();

		return ret;
	}

	/**
	 * Load InterPro result.
	 * 
	 * @param tool
	 * @param eValue
	 * @param score
	 * @param family
	 * @param accession
	 * @param name
	 * @param ec
	 * @param goName
	 * @param localization
	 * @param database
	 * @param resultsID
	 * @param statement
	 * @param databaseType 
	 * @return
	 * @throws SQLException
	 */
	public static int loadInterProResult(String tool, double eValue, double score, String family, String accession,
			String name, String ec, String goName, String localization, String database, int resultsID, 
			Statement statement, DatabaseType databaseType) throws SQLException {


		if(name!= null) {

			int size = name.length();

			if (size>250)
				size=249;
			name = DatabaseUtilities.databaseStrConverter(name, databaseType).substring(0, size);
		}

		if(goName!= null) {

			int size = goName.length();

			size = goName.length();
			if (size>250)
				size=249;

			goName = DatabaseUtilities.databaseStrConverter(goName, databaseType).substring(0, size);
		}

		String aux = "";
		if(databaseType.equals(DatabaseType.MYSQL))
			aux = "interpro_result.";

		ResultSet rs = statement.executeQuery("SELECT * FROM interpro_result WHERE interpro_result.database = '"+database+"' AND accession = '"+accession+"' AND results_id = '"+resultsID+"';");

		if(!rs.next()) {

			statement.execute("INSERT INTO interpro_result (tool, eValue, score, familyName, accession, name, ec, goName, localization, "+aux+"database, results_id) "
					+ "VALUES('"+tool+"', '"+eValue+"',  '"+score+"', '"+family+"', '"+accession+"', '"+name+"', '"+ec+"', '"+goName+"', '"+localization+"', '"+database+"', '"+resultsID+"')");
			rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
			rs.next();
		}

		int ret = rs.getInt(1);
		rs.close();

		return ret;
	}

	/**
	 * Load InterPro entries.
	 * 
	 * @param accession
	 * @param description
	 * @param name
	 * @param type
	 * @param statement
	 * @param databaseType 
	 * @return
	 * @throws SQLException
	 */
	public static int loadInterProEntry(String accession, String description, String name, String type, Statement statement, DatabaseType databaseType) throws SQLException {


		if(name!= null) {

			int size = name.length();

			if (size>250)
				size=249;

			name = DatabaseUtilities.databaseStrConverter(name, databaseType).substring(0, size);
		}

		if(description!= null) {

			int size = description.length();

			if (size>250)
				size=249;

			description = DatabaseUtilities.databaseStrConverter(description, databaseType).substring(0, size);
		}

		ResultSet rs = statement.executeQuery("SELECT * FROM interpro_entry WHERE accession = '"+accession+"';");

		if(!rs.next()) {

			statement.execute("INSERT INTO interpro_entry (accession, name, description, type) "
					+ "VALUES('"+accession+"', '"+name+"', '"+description+"','"+type+"')");
			rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
			rs.next();
		}

		int ret = rs.getInt(1);
		rs.close();

		return ret;
	}

	/**
	 * Load InterPro entry xRefs.
	 * 
	 * @param category
	 * @param database
	 * @param name
	 * @param id
	 * @param entryID
	 * @param statement
	 * @param databaseType 
	 * @throws SQLException
	 */
	public static void loadXrefs(String category, String database, String name, String id, int entryID, Statement statement, DatabaseType databaseType) throws SQLException {


		if(name!= null) {

			int size = name.length();

			if (size>250)
				size=249;

			name = DatabaseUtilities.databaseStrConverter(name, databaseType).substring(0, size);
		}

		String aux = "";
		if(databaseType.equals(DatabaseType.MYSQL))
			aux = "interpro_xRef.";

		ResultSet rs = statement.executeQuery("SELECT * FROM interpro_xRef WHERE external_id = '"+id+"' AND entry_id = '"+entryID+"';");

		if(!rs.next()) {

			statement.execute("INSERT INTO interpro_xRef (category, "+aux+"database, name, external_id, entry_id) "
					+ " VALUES('"+category+"', '"+database+"', '"+name+"', '"+id+"', '"+entryID+"')");
		}

		rs.close();
	}

	/**
	 * Load InterPro location features.
	 * 
	 * @param start
	 * @param end
	 * @param score
	 * @param hmmstart
	 * @param hmmend
	 * @param eValue
	 * @param envstart
	 * @param envend
	 * @param hmmlength
	 * @param resultID
	 * @param statement
	 * @throws SQLException
	 */
	public static void loadInterProLocation(int start, int end, float score, int hmmstart, int hmmend, float eValue,
			int envstart, int envend, int hmmlength, int resultID, Statement statement) throws SQLException {

		ResultSet rs = statement.executeQuery("SELECT * FROM interpro_location WHERE result_id = '"+resultID+"' AND start = '"+start+"'  AND end = '"+end+"'  AND score = '"+score+"' AND eValue = '"+eValue+"';");

		boolean go = rs.next();

		if(!go) {

			statement.execute("INSERT INTO interpro_location (start, end, score, hmmstart, hmmend, eValue, envstart, envend, hmmlength, result_id) "
					+ "VALUES('"+start+"', '"+end+"', '"+score+"', '"+hmmstart+"', '"+hmmend+"', '"+eValue+"', '"+envstart+"', '"+envend+"', '"+hmmlength+"', '"+resultID+"')");
		}
		rs.close();
	}

	/**
	 * Load InterPro model.
	 * 
	 * @param accession
	 * @param name
	 * @param description
	 * @param statement
	 * @param databaseType
	 * @return
	 * @throws SQLException
	 */
	public static String loadInterProModel(String accession, String name, String description, Statement statement, DatabaseType databaseType) throws SQLException {

		if(name!= null) {

			int size = 0;
			size = name.length();

			if (size>250)
				size=249;

			name = DatabaseUtilities.databaseStrConverter(name, databaseType).substring(0, size);
		}

		if(description!= null) {

			int size = description.length();

			if (size>250)
				size=249;

			description = DatabaseUtilities.databaseStrConverter(description, databaseType).substring(0, size);
		}

		String ret = null;
		ResultSet rs = statement.executeQuery("SELECT accession FROM interpro_model WHERE accession = '"+accession+"';");

		boolean go = rs.next();

		if(!go) {

			rs.close();

			try {

				statement.execute("INSERT INTO interpro_model (accession, description, name) VALUES('"+accession+"', '"+description+"', '"+name+"')");
				rs = statement.executeQuery("SELECT accession FROM interpro_model WHERE accession = '"+accession+"';");
				rs.next();
			} 
			catch (JdbcSQLException e) {

				rs = statement.executeQuery("SELECT accession FROM interpro_model WHERE accession = '"+accession+"';");
				if(rs.next())					
					logger.warn("Entry exists {}", accession);
			}
		}

		ret = rs.getString(1);
		rs.close();

		return ret;
	}

	/**
	 * Load InterPro result has model,
	 * 
	 * @param resultID
	 * @param modelAccession
	 * @param statement
	 * @throws SQLException
	 */
	public static void loadInterProResultHasModel(int resultID, String modelAccession, Statement statement) throws SQLException {

		ResultSet rs = statement.executeQuery("SELECT * FROM interpro_result_has_model WHERE result_id = '"+resultID+"' AND model_accession = '"+modelAccession+"';");

		if(!rs.next()) {

			statement.execute("INSERT INTO interpro_result_has_model (result_id, model_accession) "
					+ "VALUES('"+resultID+"', '"+modelAccession+"')");
		}

		rs.close();
	}

	/**
	 * Load InterPro result has entry,
	 * 
	 * @param resultID
	 * @param entryID
	 * @param statement
	 * @throws SQLException
	 */
	public static void loadInterProResultHasEntry(int resultID, int entryID, Statement statement) throws SQLException {

		ResultSet rs = statement.executeQuery("SELECT * FROM interpro_result_has_entry WHERE result_id = '"+resultID+"' AND entry_id = '"+entryID+"';");

		//H2 demands this check not to fail
		if(!rs.next()) {

			statement.execute("INSERT INTO interpro_result_has_entry (result_id, entry_id) VALUES('"+resultID+"', '"+entryID+"')");
		}
		rs.close();
	}

	/**
	 * Set IntroPro result loading status.
	 * 
	 * @param id
	 * @param status
	 * @param statement
	 * @throws SQLException
	 */
	public static void setInterProStatus(int id, String status, Statement statement) throws SQLException {

		statement.execute(" UPDATE interpro_results SET status = '"+status+"' WHERE id = "+id+";");
	}

	/**
	 * Delete IntroPro results with given status.
	 * 
	 * @param status
	 * @param statement
	 * @throws SQLException
	 */
	public static void deleteInterProEntries(String status, Statement statement) throws SQLException {

		statement.execute("DELETE FROM interpro_results WHERE status = '"+status+"';");
	}

	/**
	 * Retrieve genes with InterPro entries.
	 * 
	 * @return
	 * @throws SQLException 
	 */
	public static List<String> getInterProGenes(Statement statement) throws SQLException {

		List<String> ret = new ArrayList<>();

		ResultSet resultSet = statement.executeQuery("SELECT query FROM interpro_results WHERE status = 'PROCESSED'");

		while(resultSet.next())
			ret.add(resultSet.getString(1));

		resultSet.close();

		return ret;
	}


	/**
	 * Delete a set of genes from homology searches.
	 * 
	 * @param deleteGenes
	 * @param statement
	 * @throws SQLException 
	 */
	/**
	 * @param deleteGenes
	 * @param statement
	 * @throws SQLException
	 */
	public static void deleteSetOfGenes(Set<String> deleteGenes, Statement statement) throws SQLException {

		for(String s_key : deleteGenes)
			statement.execute("DELETE FROM geneHomology WHERE s_key='"+s_key+"'");
	}


	/**
	 * Retrieve genes available in homology database.
	 * 
	 * @param eVal
	 * @param matrix
	 * @param numberOfAlignments
	 * @param word
	 * @param program
	 * @param databaseID
	 * @param deleteProcessing
	 * @param statement
	 * @return
	 */
	public static Set<String> getGenesFromDatabase(String eVal, String matrix, int numberOfAlignments, short word, String program, String databaseID, boolean deleteProcessing, Statement statement) {

		Set<String> loadedGenes = new HashSet<String>();

		try  {

			Set<String> deleteGenes = new HashSet<String>();

			// get processing genes
			deleteGenes.addAll(HomologyAPI.getProcessingGenes(program, statement));

			// get processed genes
			ResultSet rs =statement.executeQuery("SELECT query, program FROM geneHomology "
					+ " INNER JOIN homologySetup ON (homologySetup.s_key = homologySetup_s_key) "
					+ " WHERE status = 'PROCESSED';");

			while(rs.next()) {

				if(rs.getString(2).contains(program))
					loadedGenes.add(rs.getString(1));
			}

			// get NO_SIMILARITY genes
			rs =statement.executeQuery("SELECT query, program FROM geneHomology " 
					+ " INNER JOIN homologySetup ON (homologySetup.s_key = homologySetup_s_key) "
					+ " WHERE status = 'NO_SIMILARITY';");

			while(rs.next())
				if(rs.getString(2).contains(program) )
					loadedGenes.add(rs.getString(1));

			// get NO_SIMILARITY genes and delete if new eVal > setup eVal
			rs =statement.executeQuery("SELECT geneHomology.s_key, query, program  " +
					" FROM geneHomology " +
					" INNER JOIN homologySetup ON (homologySetup.s_key = homologySetup_s_key) " +
					" WHERE status = 'NO_SIMILARITY' " +
					" AND eValue < "+eVal+" " +
					" AND matrix = '"+matrix+"' " +
					" AND wordSize = '"+word+"' " +
					" AND maxNumberOfAlignments = '"+numberOfAlignments+"';");

			while(rs.next()) {

				if(rs.getString(3).contains(program) ) {

					loadedGenes.remove(rs.getString(2));
					deleteGenes.add(rs.getString(1));
				}
			}

			// get NO_SIMILARITY genes and delete if new database <> setup database
			rs =statement.executeQuery("SELECT geneHomology.s_key, query, program  " +
					" FROM geneHomology " +
					" INNER JOIN homologySetup ON (homologySetup.s_key = homologySetup_s_key) " +
					" WHERE status = 'NO_SIMILARITY' " +
					" AND databaseID <> '"+databaseID+"';");

			while(rs.next()) {

				if(rs.getString(3).contains(program) ) {

					loadedGenes.remove(rs.getString(2));
					deleteGenes.add(rs.getString(1));
				}
			}

			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			//			// get processed genes
			//			rs =statement.executeQuery("SELECT query, program FROM geneHomology " +
			//					"INNER JOIN homologySetup ON (homologySetup.s_key = homologySetup_s_key) " +
			//					"WHERE status = 'PROCESSED' " +
			//					"AND matrix = '"+matrix+"' " +
			//					"AND wordSize = 'wordSize';");
			//
			//			while(rs.next())
			//				if(rs.getString(2).contains(program) )
			//					loadedGenes.add(rs.getString(1));


			//get genes with less than numberOfAlignments hits if new eVal > setup eVal and hit eVal< eVal 
			rs =statement.executeQuery("SELECT geneHomology.s_key, COUNT(referenceID), query, program " +
					"FROM homologySetup " +
					"INNER JOIN geneHomology ON (homologySetup.s_key = homologySetup_s_key) " +
					"INNER JOIN geneHomology_has_homologues ON (geneHomology.s_key = geneHomology_s_key) " +
					"WHERE status='PROCESSED' " +
					"AND geneHomology_has_homologues.eValue <= "+eVal+"  " +
					"AND homologySetup.eValue >  "+eVal+"  " +
					"AND matrix = '"+matrix+"' AND wordSize = '"+word+"' " +
					"GROUP BY geneHomology.s_key;");

			while(rs.next()) {

				if(rs.getInt(2) < numberOfAlignments && rs.getString(4).contains(program) ) {

					loadedGenes.remove(rs.getString(3));
					deleteGenes.add(rs.getString(1));
				}
			}

			HomologyAPI.deleteSetOfGenes(deleteGenes, statement);

			rs.close();
		}
		catch (SQLException e) {

			logger.error("SQL connection error!");
			e.printStackTrace();
			return null;
		}
		return loadedGenes;
	}

	/**
	 * Retrieve genes available in homology database.
	 * 
	 * @param program
	 * @param databaseID
	 * @param deleteProcessing
	 * @param statement
	 * @return
	 */
	public static Set<String> getGenesFromDatabase(String program, boolean deleteProcessing, Statement statement) {

		Set<String> loadedGenes = new HashSet<String>();

		try  {

			Set<String> deleteGenes = new HashSet<String>();

			// get processing genes
			deleteGenes.addAll(HomologyAPI.getProcessingGenes(program, statement));

			// get processed genes
			ResultSet rs =statement.executeQuery("SELECT query, program FROM geneHomology "
					+ " INNER JOIN homologySetup ON (homologySetup.s_key = homologySetup_s_key) "
					+ " WHERE status = 'PROCESSED';");

			while(rs.next()) {

				if(rs.getString(2).contains(program))
					loadedGenes.add(rs.getString(1));
			}

			// get NO_SIMILARITY genes
			rs =statement.executeQuery("SELECT query, program FROM geneHomology " 
					+ " INNER JOIN homologySetup ON (homologySetup.s_key = homologySetup_s_key) "
					+ " WHERE status = 'NO_SIMILARITY';");

			while(rs.next())
				if(rs.getString(2).contains(program) )
					loadedGenes.add(rs.getString(1));

			HomologyAPI.deleteSetOfGenes(deleteGenes, statement);

			rs.close();
		}
		catch (SQLException e) {

			logger.error("SQL connection error!");
			e.printStackTrace();
			return null;
		}
		return loadedGenes;
	}

	/**
	 * Retrieve set of genes that did not finished processing.
	 * 
	 * @param program
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static Set<String> getProcessingGenes(String program, Statement statement) throws SQLException {

		Set<String> deleteGenes = new HashSet<String>();

		ResultSet rs = statement.executeQuery("SELECT geneHomology.s_key, program " +
				" FROM geneHomology INNER JOIN homologySetup ON (homologySetup.s_key = homologySetup_s_key) " +
				" WHERE status = 'PROCESSING';");

		while(rs.next())
			if(rs.getString(2).contains(program) )
				deleteGenes.add(rs.getString(1));

		return deleteGenes;
	}

	/**
	 * Retrieve set of gene queries from set of gene keys.
	 * 
	 * @param genes
	 * @param statement
	 * @return
	 * @throws SQLException 
	 */
	public static Set<String> getQueriesFromKeys(Set<String> genes, Statement statement) throws SQLException {

		Set<String> ret = new HashSet<String>();

		ResultSet rs = statement.executeQuery("SELECT s_key, query FROM geneHomology;");

		while(rs.next())			
			if(genes.contains(rs.getString(1)))
				ret.add(rs.getString(2));

		return ret;
	}

	/**
	 * Calculate total number of genes.
	 * @param table
	 * @param stmt
	 * @return int
	 * @throws SQLException
	 */
	public static int getNumberOfGenes(String table, Statement stmt) throws SQLException{

		int num = 0;
		ResultSet rs = stmt.executeQuery("SELECT * FROM "+table);

		while(rs.next())
			num++;

		rs.close();
		return num;
	}



	/**
	 * Get program from homologySetup table.
	 * @param stmt
	 * @return ArrayList<String>
	 * @throws SQLException
	 */
	public static ArrayList<String> getProgramFromHomologySetup(Statement stmt) throws SQLException{

		ArrayList<String> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT program" +
				" FROM homologySetup " +
				"INNER JOIN geneHomology ON (homologySetup.s_key = homologySetup_s_key) " +
				"WHERE status = 'PROCESSED' OR status = 'NO_SIMILARITY'");

		while(rs.next())
			result.add(rs.getString(1));

		rs.close();
		return result;
	}

	/**
	 * Get stats from geneHomology table.
	 * @param program
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getSpecificStats(String program, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT geneHomology.s_key, locusTag, query, gene, chromosome, organelle " +
				" FROM geneHomology" +
				" INNER JOIN homologySetup ON (homologySetup.s_key = homologySetup_s_key)" +
				" WHERE status<>'NO_SIMILARITY' AND (LOWER(program) LIKE "+program+" )");

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
	 * Get all genes from geneHomology table.
	 * @param program
	 * @param stmt
	 * @return ArrayList<String>
	 * @throws SQLException
	 */
	public static ArrayList<String> getAllFromGeneHomology(String program, Statement stmt) throws SQLException{

		ArrayList<String> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT *" +
				" FROM geneHomology" +
				" INNER JOIN homologySetup ON (homologySetup.s_key = homologySetup_s_key)" +
				" WHERE status='NO_SIMILARITY' AND (LOWER(program) LIKE "+program+" )");

		while(rs.next())
			result.add(rs.getString(1));

		rs.close();
		return result;
	}

	/**
	 * Count number of homologues genes.
	 * @param program
	 * @param stmt
	 * @return integer
	 * @throws SQLException
	 */
	public static Integer getNumberOfHomologueGenes(String program, Statement stmt) throws SQLException{

		int result = 0;

		ResultSet rs = stmt.executeQuery("SELECT count(*)" +
				" FROM homologySetup " +
				" INNER JOIN geneHomology ON (homologySetup.s_key = homologySetup_s_key)" +
				" INNER JOIN geneHomology_has_homologues ON (geneHomology_s_key = geneHomology.s_key)" +
				" WHERE LOWER(program) LIKE "+program+" ");
		if(rs.next())
			result = rs.getInt(1);

		rs.close();
		return result;
	}

	/**
	 * get taxonomy data from homologySetup table.
	 * @param program
	 * @param stmt
	 * @return ArrayList<String>
	 * @throws SQLException
	 */
	public static ArrayList<String> getTaxonomy(String program, Statement stmt) throws SQLException{

		ArrayList<String> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT DISTINCT taxonomy, organism "+
				" FROM homologySetup" +
				" INNER JOIN geneHomology ON (homologySetup.s_key = homologySetup_s_key)" +
				" INNER JOIN geneHomology_has_homologues ON (geneHomology_s_key = geneHomology.s_key)" +
				" INNER JOIN homologues ON (homologues_s_key = homologues.s_key)" +
				" INNER JOIN organism ON (organism_s_key = organism.s_key)" +
				" WHERE LOWER(program) LIKE "+program+" ");

		while(rs.next())
			result.add(rs.getString(1));

		rs.close();
		return result;
	}

	/**
	 * Get data from geneHomology_has_homologues table.
	 * @param locus
	 * @param stmt
	 * @return String
	 * @throws SQLException
	 */
	public static String geneHomologyHasHomologues(String locus, Statement stmt) throws SQLException{

		String result = "";

		ResultSet rs =stmt.executeQuery("SELECT geneHomology.query FROM geneHomology_has_homologues "+
				" INNER JOIN geneHomology ON (geneHomology_s_key = geneHomology.s_key)"+
				" INNER JOIN homologySetup ON (homologySetup.s_key = homologySetup_s_key)" +
				" WHERE locusTag = '" + locus+"' " +
				" GROUP BY program");

		while (rs.next()) 
			result=rs.getString(1);

		rs.close();
		return result;
	}

	/**
	 * Get data from ecNumberRank table.
	 * @param query
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getProgram(String query, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs =stmt.executeQuery("SELECT ecNumberRank.s_key, ecNumber, rank, program " +
				" FROM ecNumberRank "+
				" INNER JOIN geneHomology ON (geneHomology_s_key = geneHomology.s_key) " +
				" INNER JOIN homologySetup ON (homologySetup.s_key = homologySetup_s_key) " +
				" WHERE query = '" + query+"' " +
				" ORDER BY program, rank DESC; ");

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
	 * Get data from productRank table.
	 * @param locus
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getProductRankData(String locus, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs=stmt.executeQuery("SELECT productRank.s_key, productName, rank, program " +
				"FROM productRank " +
				" INNER JOIN geneHomology ON (geneHomology_s_key = geneHomology.s_key)" +
				" INNER JOIN homologySetup ON (homologySetup.s_key = homologySetup_s_key)" +
				" WHERE locusTag = '" + locus+"' " +
				"ORDER BY program, rank DESC;");

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
	 * Get locusTag and uniprot_ecnumber from geneHomology table.
	 * @param stmt
	 * @return Map<String, List<String>>
	 * @throws SQLException
	 */
	public static Map<String, List<String>> getUniprotEcNumbers(Statement stmt) throws SQLException{

		Map<String, List<String>> result = new HashMap<String, List<String>>();

		ResultSet rs = stmt.executeQuery("SELECT DISTINCT locusTag, uniprot_ecnumber FROM geneHomology");

		while(rs.next()) {

			if(rs.getString(2)!= null && !rs.getString(2).isEmpty()) {

				List<String> ecnumbers = new ArrayList<String>();
				String[] ecs = rs.getString(2).split(", ");

				for(String ec : ecs) {

					ecnumbers.add(ec.trim());
				}
				result.put(rs.getString(1), ecnumbers);
			}
		}
		rs.close();
		return result;
	}

	/**
	 * Get all data from scorerConfig table.
	 * @param stmt
	 * @return ArrayList<String>
	 * @throws SQLException
	 */
	public static ArrayList<String> getCommitedScorerData(Statement stmt) throws SQLException{

		ArrayList<String> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT * FROM scorerConfig;");

		while(rs.next()){
			result.add(rs.getString(1));
			result.add(rs.getString(2));
			result.add(rs.getString(3));
			result.add(rs.getString(4));
			result.add(rs.getString(5));
		}

		rs.close();
		return result;
	}

	/**
	 * Get all data from homologyData table.
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getCommittedHomologyData(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT * FROM homologyData ");

		while(rs.next()){
			String[] list = new String[9];

			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);
			list[3]=rs.getString(4);
			list[4]=rs.getString(5);
			list[5]=rs.getString(6);
			list[6]=rs.getBoolean(7)+"";
			list[7]=rs.getString(8);
			list[8]=rs.getString(9);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * Get data from homologyData table.
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getCommittedHomologyData2(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT geneHomology_s_key, otherNames FROM homologyData " +
				"RIGHT JOIN productList ON (homologyData.s_key = homologyData_s_key);");

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
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getCommittedHomologyData3(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT geneHomology_s_key, otherECnumbers FROM homologyData " +
				"RIGHT JOIN ecNumberList ON (homologyData.s_key = homologyData_s_key);");

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
	 * Executes a given query.
	 * @param query
	 * @param stmt
	 * @throws SQLException
	 */
	public static void executeQuery(String query, Statement stmt) throws SQLException{

		stmt.execute(query);
	}

	/**
	 * 
	 * @param stmt
	 * @return 
	 * @throws SQLException
	 */
	public static Map<Integer, String> getDatabaseLocus(Statement stmt) throws SQLException{

		Map<Integer, String> database_locus = new HashMap<Integer, String>();

		ResultSet rs = stmt.executeQuery("SELECT s_key, locusTag FROM geneHomology");

		while(rs.next())
			database_locus.put(rs.getInt(1), rs.getString(2));

		rs.close();
		return database_locus;
	}

	/**
	 * Check if a given geneHomology_s_key exists at homologyData table.
	 * @param key
	 * @param stmt
	 * @return A boolean indicating if the key exists or not at homologyData table.
	 * @throws SQLException
	 */
	public static boolean homologyDataHasKey(int key, Statement stmt) throws SQLException{

		boolean exists = false;

		ResultSet rs = stmt.executeQuery("SELECT * FROM homologyData WHERE geneHomology_s_key='"+key+"';");

		if(rs.next())
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 * Get the string in the first column of the homologyData table for a given geneHomology_s_key.
	 * @param key
	 * @param stmt
	 * @return the string in the first column
	 * @throws SQLException
	 */
	public static String getHomologyDataKey(int key, Statement stmt) throws SQLException{

		String result = "";

		ResultSet rs = stmt.executeQuery("SELECT * FROM homologyData WHERE geneHomology_s_key='"+key+"';");

		while(rs.next())
			result = rs.getString(1);

		rs.close();
		return result;
	}

	/**
	 * Executes a given query, returning the last entry in the first column.
	 * @param query
	 * @param stmt
	 * @return String of the last entry in the first column.
	 * @throws SQLException
	 */
	public static String insertIntoHomologyData(String query, Statement stmt) throws SQLException{

		String result = "";

		stmt.execute(query);

		ResultSet rs=stmt.executeQuery("SELECT last_insert_id()");
		if(rs.next())
			result = rs.getString(1);

		rs.close();
		return result;
	}

	/**
	 * Check if a given homologyData_s_key exists at productList table.
	 * @param s_key
	 * @param stmt
	 * @return A boolean indicating if the key exists or not at productList table.
	 * @throws SQLException
	 */
	public static boolean productListHasKey(String s_key, Statement stmt) throws SQLException{

		boolean exists = false;

		ResultSet rs = stmt.executeQuery("SELECT * FROM productList WHERE homologyData_s_key=\'"+s_key+"\'");

		if(rs.next())
			exists = true;

		rs.close();
		return exists;
	}

	/**Check if a given homologyData_s_key exists at ecNumberList table.
	 * @param s_key
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static boolean ecNumberListHasKey(String s_key, Statement stmt) throws SQLException{

		boolean exists = false;

		ResultSet rs = stmt.executeQuery("SELECT * FROM ecNumberList WHERE homologyData_s_key=\'"+s_key+"\'");

		if(rs.next())
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 * Retrieves information from ecNumberRank table.
	 * @param stmt
	 * @return ecNumberRank.s_key, geneHomology_s_key, ecNumber, rank from ecNumberReank table.
	 * @throws SQLException
	 */
	/**
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getDataFromecNumberRank(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT ecNumberRank.s_key, geneHomology_s_key, ecNumber, rank FROM ecNumberRank;");

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
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getEcRank(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT ecNumberRank.s_key, taxRank FROM ecNumberRank " +
				" INNER JOIN ecNumberRank_has_organism ON(ecNumberRank_s_key=ecNumberRank.s_key) " +
				" INNER JOIN organism ON (organism.s_key=organism_s_key);");

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
	 * Retrieves the taxRank maximum at the organism table.
	 * @param stmt
	 * @return String of the max value.
	 * @throws SQLException
	 */
	public static String getMaxTaxRank(Statement stmt) throws SQLException{

		String result = "";

		ResultSet rs = stmt.executeQuery("SELECT MAX(taxRank) FROM organism;");

		while(rs.next())
			result = rs.getString(1);

		rs.close();
		return result;
	}

	/**
	 * Get the homologues count.
	 * @param stmt
	 * @return Map with the homologues count by ecNumberRank.s_key.
	 * @throws SQLException
	 */
	public static Map<String,Double> getHomologuesCountByEcNumber(Statement stmt) throws SQLException{

		Map<String,Double> homologuesCount = new TreeMap<String, Double>();

		ResultSet rs = stmt.executeQuery("SELECT  ecNumberRank.s_key, count(geneHomology_has_homologues.homologues_s_key) " +
				"FROM geneHomology_has_homologues " +
				"JOIN ecNumberRank ON (ecNumberRank.geneHomology_s_key=geneHomology_has_homologues.geneHomology_s_key) " +
				"GROUP BY ecNumberRank.s_key;");

		while(rs.next())
			homologuesCount.put(rs.getString(1), Double.parseDouble(rs.getString(2)));


		rs.close();
		return homologuesCount;
	}

	/**
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getProductRank(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT productRank.s_key, geneHomology_s_key, productName, rank FROM productRank;");

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

	public static ArrayList<String[]> getTaxRank(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT productRank_s_key, taxRank FROM productRank_has_organism " +
				" INNER JOIN organism ON(organism.s_key=organism_s_key) ORDER BY productRank_s_key");

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
	 * Get the homologues count.
	 * @param stmt
	 * @return Map with the homologues count by productRank.s_key.
	 * @throws SQLException
	 */
	public static Map<String,Double> getHomologuesCountByProductRank(Statement stmt) throws SQLException{

		Map<String,Double> homologuesCount = new TreeMap<String, Double>();

		ResultSet rs = stmt.executeQuery("SELECT productRank.s_key, count(geneHomology_has_homologues.homologues_s_key) " +
				"FROM geneHomology_has_homologues " +
				"INNER JOIN productRank ON (productRank.geneHomology_s_key=geneHomology_has_homologues.geneHomology_s_key) " +
				"GROUP BY productRank.s_key;");

		while(rs.next())
			homologuesCount.put(rs.getString(1), Double.parseDouble(rs.getString(2)));				

		rs.close();
		return homologuesCount;
	}

	/**
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getProductRank2(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT s_key, geneHomology_s_key, productName, rank  FROM productRank");

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
	 * Get gene information.
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getGenesInformation(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT geneHomology.s_key, locusTag, gene, chromosome, organelle, uniprot_star, program, query" +
				" FROM geneHomology" +
				" INNER JOIN homologySetup ON (homologySetup.s_key = homologySetup_s_key)" +
				" WHERE status = 'PROCESSED' OR status = 'NO_SIMILARITY' " +
				" ORDER BY locusTag, status DESC;");

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
	 * Get gene blast database information.
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, Set<Integer>> getGenesPerDatabase(Statement stmt) throws SQLException{

		Map<String, Set<Integer>> result = new TreeMap<>();
		
		ResultSet rs = stmt.executeQuery("SELECT geneHomology.s_key, databaseID" +
				" FROM geneHomology" +
				" INNER JOIN homologySetup ON (homologySetup.s_key = homologySetup_s_key)" +
				" WHERE (status = 'PROCESSED' OR status = 'NO_SIMILARITY') " +
				" ORDER BY locusTag, status DESC;");

		while(rs.next()) {
			
			if(result.containsKey(rs.getObject(2))) {
				
				Set<Integer> keys = result.get(rs.getString(2));
				keys.add(rs.getInt(1));
				
				result.put(rs.getString(2), keys);
			}
			else {
				Set<Integer> keys = new HashSet<>();
				keys.add(rs.getInt(1));
				
				result.put(rs.getString(2), keys);
			}
		}
		rs.close();
		return result;
	}

	/**
	 * Get enzymes by reaction.
	 * @param idReaction
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static String[] getEnzymesByReaction(int idReaction, Statement statement) throws SQLException{

		String[] res = null;

		ResultSet rs = statement.executeQuery(
				"SELECT enzyme_ecnumber, protein_idprotein, protein.name FROM reaction_has_enzyme " +
						" INNER JOIN protein ON protein.idprotein = protein_idprotein " +
						"WHERE reaction_idreaction='"+idReaction+"'");

		rs.last();
		res = new String[rs.getRow()];
		rs.first();
		int col=0;
		while(col<res.length)
		{
			res[col] = rs.getString(1)+"___"+rs.getString(3)+"___"+rs.getString(2);
			rs.next();
			col++;
		}
		rs.close();
		return res;
	}

	/**
	 * Get enzymes by given reaction and pathway.
	 * @param idReaction
	 * @param pathway
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static String[] getEnzymesByReactionAndPathway(int idReaction, int pathway, Statement statement) throws SQLException{

		String[] res = null;

		ResultSet rs = statement.executeQuery("SELECT DISTINCT enzyme.ecnumber, enzyme.protein_idprotein, protein.name FROM reaction" +
				" INNER JOIN reaction_has_enzyme ON idreaction = reaction_has_enzyme.reaction_idreaction" +
				" INNER JOIN enzyme ON reaction_has_enzyme.enzyme_ecnumber= enzyme.ecnumber "
				+ "AND reaction_has_enzyme.enzyme_protein_idprotein = enzyme.protein_idprotein " +
				" INNER JOIN pathway_has_reaction ON idreaction=pathway_has_reaction.reaction_idreaction" +
				" INNER JOIN pathway_has_enzyme ON enzyme.ecnumber=pathway_has_enzyme.enzyme_ecnumber "
				+ "AND enzyme.protein_idprotein=pathway_has_enzyme.enzyme_protein_idprotein" +
				" INNER JOIN protein ON protein.idprotein = protein_idprotein " +
				" WHERE pathway_has_reaction.pathway_idpathway=pathway_has_enzyme.pathway_idpathway" +
				" AND pathway_has_enzyme.pathway_idpathway=\'"+pathway+"\'" +
				" AND reaction.idreaction=\'"+idReaction+"\'");

		rs.last();
		res = new String[rs.getRow()];
		rs.first();
		int col=0;

		while(col<res.length) {

			res[col] = rs.getString(1)+"___"+rs.getString(3)+"___"+rs.getString(2);
			rs.next();
			col++;
		}
		rs.close();
		return res;
	}

	/**
	 * Get list of geneIDs.
	 * @param aux
	 * @param stmt
	 * @return Set<String>
	 * @throws SQLException
	 */
	public static Set<String> getGenesID(String aux, Statement stmt) throws SQLException{

		Set<String> genes = new HashSet<String>();

		ResultSet rs = stmt.executeQuery("SELECT * FROM gene "+aux);

		while(rs.next()) 
			genes.add(rs.getString(1));

		rs.close();
		return genes;
	}

	/**
	 * Get proteinID and ECnumber for a given geneID.
	 * @param geneID
	 * @param stmt
	 * @return Set<String>
	 * @throws SQLException
	 */
	public static Set<String> getProteinIdAndEcNumber(String geneID, Statement stmt) throws SQLException{

		Set<String> proteins = new HashSet<String>();

		ResultSet rs = stmt.executeQuery("SELECT enzyme_protein_idProtein, enzyme_ecNumber FROM subunit WHERE gene_idGene ="+geneID);

		while(rs.next()) 
			proteins.add(rs.getString(1)+"__"+rs.getString(2));

		rs.close();
		return proteins;
	}

	/**
	 * Get all Chromosomes
	 * @param stmt
	 * @return String[][]
	 * @throws SQLException
	 */
	public static String[][] getChromosomes(Statement stmt) throws SQLException{

		String[][] res = null;

		ResultSet rs = stmt.executeQuery("SELECT * FROM chromosome order by name");

		if (rs.next()) {

			ResultSetMetaData rsmd = rs.getMetaData();
			rs.last();
			res = new String[rs.getRow()][rsmd.getColumnCount()];
			rs.beforeFirst();

			int row=0;
			while(rs.next()) {

				res[row][0] = rs.getString(1);
				res[row][1] = rs.getString(2);
				row++;
			}
		}
		rs.close();
		return res;
	}

	/**
	 * Get proteins.
	 * @param stmt
	 * @return String[][]
	 * @throws SQLException
	 */
	public static String[][] getProteins(Statement stmt) throws SQLException{

		String[][] res = null;

		ResultSet rs = stmt.executeQuery("SELECT idProtein, name, ecnumber FROM protein " +
				" JOIN enzyme ON protein_idprotein=protein.idprotein "+
				" ORDER BY ecnumber;");

		ResultSetMetaData rsmd = rs.getMetaData();
		rs.last();
		res = new String[rs.getRow()][rsmd.getColumnCount()];
		rs.first();

		int row=0;

		while(row<res.length) {

			res[row][0] = rs.getString(1)+"__"+rs.getString(3);
			res[row][1] = rs.getString(3)+"	-	"+rs.getString(2);

			rs.next();
			row++;
		}
		rs.close();
		return res;
	}

	/**
	 * Get all geneID and sequenceID from gene table.
	 * @param stmt
	 * @return Map<String, String>
	 * @throws SQLException
	 */
	public static Map<String, String> getSequenceID(Statement stmt) throws SQLException{

		Map<String, String> sequenceIDGeneID = new HashMap<>();

		ResultSet rs = stmt.executeQuery("SELECT idgene, sequence_id FROM gene;");

		while(rs.next())
			sequenceIDGeneID.put(rs.getString(2), rs.getString(1));

		rs.close();
		return sequenceIDGeneID;
	}

	/**
	 * Get proteinID and ECnumber for a given idReaction.
	 * @param idReaction
	 * @param statement
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getEnzymeProteinID(String idReaction, Statement statement) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = statement.executeQuery("SELECT enzyme_protein_idprotein, enzyme_ecnumber FROM reaction_has_enzyme "
				+ "WHERE reaction_idreaction = "+idReaction+" ;");

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
	 * Get ecNumbers list.
	 * @param reactionID
	 * @param stmt
	 * @return Map<String, List<String>>
	 * @throws SQLException
	 */
	public static Map<String, List<String>> getEcNumbersList(String reactionID, Statement stmt) throws SQLException{

		Map<String, List<String>> listECs = new HashMap<>();

		ResultSet rs = stmt.executeQuery(
				"SELECT enzyme_ecnumber FROM reaction_has_enzyme WHERE reaction_idreaction = "+reactionID);

		while(rs.next())
			listECs.put(rs.getString(1),new ArrayList<String>());

		rs.close();
		return listECs;
	}

	/**
	 * Get s_key from genehomology table for a given query and homologySetup_s_key.
	 * @param aux
	 * @param homologySetupID
	 * @param statement
	 * @return String
	 * @throws SQLException
	 */
	public static int getGeneHomologySkey(String aux, int homologySetupID, Statement statement) throws SQLException{

		int res = -1;

		ResultSet rs = statement.executeQuery("SELECT * FROM geneHomology WHERE query = '"
				+ aux +"' AND homologySetup_s_key = " +homologySetupID);

		if(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 * Get s_key from Homologues table for a given organism_s_key.
	 * @param query
	 * @param statement
	 * @return String
	 * @throws SQLException
	 */
	public static int getHomologuesSkey(String query, Statement statement) throws SQLException{

		int res = -1;

		ResultSet rs = statement.executeQuery(query);

		if(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 * Check if GeneHomology_Has_Homologues has entries.
	 * @param query
	 * @param statement
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkGeneHomologyHasHomologues(String query, Statement statement) throws SQLException{

		boolean exists = false;

		ResultSet rs = statement.executeQuery(query);

		if(rs.next())
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 * Check if homologues_has_ecNumber has entries for a given homologues_s_key and ecNumber_s_key.
	 * @param query
	 * @param statement
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkHomologuesHasEcNumber(int homologues_s_key, int ecnumber_s_key, Statement statement) throws SQLException{

		boolean exists = false;

		ResultSet rs = statement.executeQuery("SELECT * FROM homologues_has_ecNumber " +
				"WHERE homologues_s_key = '"+homologues_s_key+"' AND " +
				"ecNumber_s_key = '"+ecnumber_s_key+"'");

		if(rs.next())
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 * Load data from geneHomology table.
	 * @param query
	 * @param statement
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean loadGeneHomologyData(String homologyDataClient, String program, Statement statement) throws SQLException{

		boolean exists = false;

		ResultSet rs = statement.executeQuery("SELECT * FROM geneHomology " +
				"INNER JOIN homologySetup ON (homologySetup.s_key = homologySetup_s_key)" +
				"WHERE query = '"+ homologyDataClient +"' " +
				"AND program = '"+ program +"' ");

		if(rs.next())
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 * Loads Orthologs data
	 * 
	 * @param capsule
	 * @param geneIds
	 * @param statement
	 * @throws SQLException
	 */
	public static void loadOrthologsInfo(AlignmentCapsule capsule, Map<String, Integer> geneIds, Statement statement) throws SQLException {

		String orthologLocus = capsule.getQuery().split(":")[1];

		double score = capsule.getScore();

		String ecnumber = capsule.getEcNumber();

		Map<String, Set<String>> modules = capsule.getModules();

		Map<String, Set<String>> closestOrthologs = capsule.getClosestOrthologues();

		String sequenceID = capsule.getTarget();

		int idGene = geneIds.get(sequenceID);

		ResultSet rs = null;

		for (String ortholog : closestOrthologs.get(capsule.getQuery())) {

			rs = statement.executeQuery("SELECT id FROM orthology WHERE entry_id ='"+ortholog+"' AND (locus_id is null OR locus_id = '');");

			String orthology_id = "";

			if(rs.next()) {

				orthology_id = rs.getString(1);
				statement.execute("UPDATE orthology SET locus_id = '"+orthologLocus+"' WHERE entry_id = '"+ortholog+"';");
			}
			else {

				rs = statement.executeQuery("SELECT id FROM orthology WHERE entry_id ='"+ortholog+"' AND locus_id ='"+orthologLocus+"';");

				if(!rs.next()) {

					statement.execute("INSERT INTO orthology (entry_id, locus_id) VALUES ('"+ortholog+"', '"+orthologLocus+"');");
					rs = statement.executeQuery("SELECT LAST_INSERT_ID();");
					rs.next();
				}
				orthology_id = rs.getString(1);
			}
			rs = statement.executeQuery("SELECT * FROM gene_has_orthology WHERE gene_idgene='"+idGene+"' AND orthology_id='"+orthology_id+"';");

			if(!rs.next())	
				statement.execute("INSERT INTO gene_has_orthology (gene_idgene,orthology_id, similarity) VALUES("+idGene+","+orthology_id+", "+ score +" );");

			rs = statement.executeQuery("SELECT protein_idprotein FROM enzyme WHERE ecnumber='"+ecnumber+"';");
			rs.next();
			int protein_idprotein = rs.getInt(1);
			rs = statement.executeQuery("SELECT module_id, note FROM subunit WHERE gene_idgene='"+idGene+"' AND enzyme_ecnumber = '"+ecnumber+"';");

			List<String> modules_ids = new ArrayList<String>();
			boolean exists = false, noModules=true;

			String note = "unannotated";

			while(rs.next()) {

				exists = true;

				if(rs.getInt(1)>0) {

					noModules = false;
					modules_ids.add(rs.getString(1));
				}

				if(rs.getString(2)!=null && !rs.getString(2).equalsIgnoreCase("null"))
					note = rs.getString(2);
				else
					note = "";
			}
			if(modules != null){
				for(String module_id : modules.get(ortholog)) {

					if(modules_ids.contains(module_id)) {

						if(exists) {

							if(noModules) {
								statement.execute("UPDATE subunit SET module_id = "+module_id+" WHERE gene_idgene = '"+idGene+"' AND enzyme_ecnumber = '"+ecnumber+"';");
								noModules = false;
								modules_ids.add(module_id);
							}
						}
						else {
							statement.execute("INSERT INTO subunit (module_id, gene_idgene, enzyme_ecnumber, enzyme_protein_idprotein, note)" +
									"VALUES("+module_id+", "+idGene+", '"+ecnumber+"', "+protein_idprotein+", '"+note+"');");
						}

					}
				}
			}
		}
		rs.close();
	}

	
	/**
	 * Method to return the databases used to perform the blast.
	 * 
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static String[] getBlastDatabases(Statement statement) throws SQLException{

		String[] databases = new String[1];
				
		ResultSet rs = statement.executeQuery("SELECT COUNT(DISTINCT(databaseID)) FROM homologySetup");

		if(rs.next())
			databases = new String[rs.getInt(1)+1];

		rs = statement.executeQuery("SELECT DISTINCT(databaseID) FROM homologySetup");

		int i = 1;
		
		while(rs.next()){
			databases[i] = rs.getString(1);
			i++;
		}

		databases[0] = "all databases";
		
		rs.close();
		return databases;
	}
		
		/**
		 * Method to check if the statement is still working. If not, the a new statement is generated. 
		 * 
		 * @param dbAccess
		 * @param statement
		 * @return
		 * @throws SQLException 
		 */
		public static Statement checkStatement(DatabaseAccess dbAccess, Statement statement) throws SQLException{
			
			try {
				
				statement.executeQuery("SELECT * FROM scorerConfig");
				
			} 
			catch(CommunicationsException e) {
				
				Connection connection = new Connection(dbAccess);
				
				statement = connection.createStatement();
				
				logger.info("New SQL connection generated due to communications exception.");
				
			}
			
			return statement;
		}
		
}
