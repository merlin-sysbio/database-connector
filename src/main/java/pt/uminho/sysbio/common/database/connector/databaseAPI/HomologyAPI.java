package pt.uminho.sysbio.common.database.connector.databaseAPI;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import pt.uminho.ceb.biosystems.mew.utilities.datastructures.pair.Pair;


public class HomologyAPI {

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
		
		Pair <String, String> ret= new Pair<>(locusTag, name);
		return ret;
	}

}
