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

}
