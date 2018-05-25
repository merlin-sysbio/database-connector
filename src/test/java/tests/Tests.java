package tests;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.ModelAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.Connection;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.DatabaseAccess;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.MySQLDatabaseAccess;
import pt.uminho.ceb.biosystems.merlin.utilities.Utilities;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.pair.Pair;

public class Tests {

	@Test
	public void test() throws SQLException {

		String database = "r6", 
				organism = "spn_r6";
		String host = "193.137.11.210";
		DatabaseAccess connector = new MySQLDatabaseAccess("", "",  host, 3306, database);

		Connection connection = new Connection(connector);
		Statement s  = connection.createStatement(),
				s2  = connection.createStatement(),
				s3  = connection.createStatement();


		Map<Integer, Pair<String, String>> g = ModelAPI.getGenesFromDatabase(s);

		ResultSet r = s.executeQuery("select idreaction, boolean_rule from reaction WHERE boolean_rule IS NOT null AND NOT originalReaction;");

		while (r.next()) {

			int id = r.getInt(1);

			List<List<Pair<String, String>>> l = ModelAPI.getBooleanRuleFromReaction(id, s2); 

			Set<Set<String>> lout = new HashSet<>();

			//or rule
			for(List<Pair<String, String>> ll : l) {

				Set<String> llout = new HashSet<>();
				//and rule
				for(Pair<String, String> p : ll) {

					for(int i : g.keySet()) {

						if(g.get(i).getA().equals(p.getA()))
							llout.add(String.valueOf(i));
					}
				}
				lout.add(llout);
			}

			String boolean_rule = r.getString(2);
			String newRule = Utilities.parseRuleToString(lout);

			if(!newRule.isEmpty()) {

				System.out.println("old rule "+ boolean_rule);
				System.out.println("new rule "+newRule);
				s3.execute("UPDATE reaction SET boolean_rule = '"+newRule+"' WHERE boolean_rule = '"+boolean_rule+"'");
			}
			else {
				System.out.println("old rule "+ boolean_rule);
			}
		}
		
		r.close();		
		s.close();
		s2.close();
		s3.close();
		connection.closeConnection();
	}

}
