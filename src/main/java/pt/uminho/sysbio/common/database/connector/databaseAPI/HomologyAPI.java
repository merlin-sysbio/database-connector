package pt.uminho.sysbio.common.database.connector.databaseAPI;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.mew.utilities.datastructures.pair.Pair;
import pt.uminho.sysbio.common.database.connector.datatypes.DatabaseUtilities;
import pt.uminho.sysbio.common.database.connector.datatypes.Enumerators.DatabaseType;


/**
 * @author Oscar Dias
 *
 */
public class HomologyAPI {

	final static Logger logger = LoggerFactory.getLogger(HomologyAPI.class);

	/**
	 * Get gene locus from homology data.
	 * 
	 * @param sequence_id
	 * @param statement
	 * @param informationType 
	 * @return
	 * @throws SQLException
	 */
	public static Pair<String,String> getGeneLocusFromHomologyData (String sequence_id, Statement statement) throws SQLException {

		String locusTag = sequence_id, name = null;

		ResultSet rs = statement.executeQuery("SELECT locusTag, gene FROM geneHomology WHERE query = '"+sequence_id+"';");

		if(rs.next()) {

			locusTag = rs.getString(1);
			name = rs.getString(2);
		}

		Pair <String, String> ret = new Pair<>(locusTag, name);
		return ret;
	}
	
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
				"AND (LOWER(program) LIKE '"+tool1+"' OR LOWER(program) LIKE '"+tool2+"' OR LOWER(program) LIKE '"+tool3+"')");

		while(resultSet.next()) {

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
				ql.add("");
				ql.add("");
				ql.add("");
				ql.add("");
				ql.add("");
				ql.add("");
				result.add(ql);
			}
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
				"SELECT organism,taxonomy " +
						"FROM homologySetup " +
						"INNER JOIN geneHomology ON (homologySetup.s_key = homologySetup_s_key)" +
						"INNER JOIN geneHomology_has_homologues ON (geneHomology_s_key = geneHomology.s_key)" +
						"INNER JOIN homologues ON (homologues_s_key = homologues.s_key)" +
						"INNER JOIN organism ON (organism_s_key = organism.s_key) " +
						"WHERE query = '" + query +"' " +
						"AND (LOWER(program) LIKE '"+tool1+"' OR LOWER(program) LIKE '"+tool2+"' OR LOWER(program) LIKE '"+tool3+"')");

		while (resultSet.next()) {

			ql = new ArrayList<String>();
			ql.add(resultSet.getString(1));
			ql.add(resultSet.getString(2));
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

			ql = new ArrayList<String>();
			ql.add(resultSet.getString(1));
			ql.add(resultSet.getString(2));
			ql.add(resultSet.getString(3));
			ql.add(resultSet.getString(4));
			ql.add(resultSet.getString(5));
			ql.add(resultSet.getString(6));
			ql.add(resultSet.getString(7));
			ql.add(resultSet.getString(8));
			ql.add(resultSet.getString(9));
			ql.add(resultSet.getString(10));
			ql.add(resultSet.getString(11));
			ql.add(resultSet.getString(12));
			ql.add(resultSet.getString(13));
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

		while(rs.next()) {

			ret.add(rs.getString(2));
		}

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
	public static int loadIntroProAnnotation(String query, String querySequence, String mostLikelyEC,
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

		return rs.getInt(1);
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

		ResultSet rs = statement.executeQuery("SELECT * FROM interpro_result WHERE interpro_result.database = '"+database+"' AND accession = '"+accession+"' AND results_id = '"+resultsID+"';");

		if(!rs.next()) {

			statement.execute("INSERT INTO interpro_result (tool, eValue, score, familyName, accession, name, ec, goName, localization, interpro_result.database, results_id) "
					+ "VALUES('"+tool+"', '"+eValue+"',  '"+score+"', '"+family+"', '"+accession+"', '"+name+"', '"+ec+"', '"+goName+"', '"+localization+"', '"+database+"', '"+resultsID+"')");
			rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
			rs.next();
		}
		return rs.getInt(1);
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
		return rs.getInt(1);
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
		ResultSet rs = statement.executeQuery("SELECT * FROM interpro_xRef WHERE external_id = '"+id+"' AND entry_id = '"+entryID+"';");

		if(!rs.next()) {

			statement.execute("INSERT INTO interpro_xRef (category, interpro_xRef.database, name, external_id, entry_id) "
					+ " VALUES('"+category+"', '"+database+"', '"+name+"', '"+id+"', '"+entryID+"')");
		}
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

		if(!rs.next()) {

			statement.execute("INSERT INTO interpro_location (start, end, score, hmmstart, hmmend, eValue, envstart, envend, hmmlength, result_id) "
					+ "VALUES('"+start+"', '"+end+"', '"+score+"', '"+hmmstart+"', '"+hmmend+"', '"+eValue+"', '"+envstart+"', '"+envend+"', '"+hmmlength+"', '"+resultID+"')");
		}
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

		if(!rs.next()) {

			statement.execute("INSERT INTO interpro_model (accession, description, name) VALUES('"+accession+"', '"+description+"', '"+name+"')");
			rs = statement.executeQuery("SELECT accession FROM interpro_model WHERE accession = '"+accession+"';");
			rs.next();
		}
		
		ret = rs.getString(1);
		
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

		if(!rs.next()) {

			statement.execute("INSERT INTO interpro_result_has_entry (result_id, entry_id) VALUES('"+resultID+"', '"+entryID+"')");
		}
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

		statement.execute("DELETE from interpro_results WHERE status = '"+status+"';");
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
		
		return ret;
	}


}
