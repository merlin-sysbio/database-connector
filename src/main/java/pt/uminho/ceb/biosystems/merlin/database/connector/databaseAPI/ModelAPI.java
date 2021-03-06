package pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.biojava.nbio.core.exceptions.CompoundNotFoundException;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.ProteinSequence;
import org.biojava.nbio.core.sequence.RNASequence;
import org.biojava.nbio.core.sequence.template.AbstractSequence;

import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException;

import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.Connection;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.DatabaseUtilities;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.Enumerators.DatabaseType;
import pt.uminho.ceb.biosystems.merlin.utilities.Enumerators.SequenceType;
import pt.uminho.ceb.biosystems.merlin.utilities.containers.capsules.ReactionsCapsule;
import pt.uminho.ceb.biosystems.merlin.utilities.containers.gpr.GeneAssociation;
import pt.uminho.ceb.biosystems.merlin.utilities.containers.gpr.ModuleCI;
import pt.uminho.ceb.biosystems.merlin.utilities.containers.gpr.ProteinsGPR_CI;
import pt.uminho.ceb.biosystems.merlin.utilities.containers.gpr.ReactionProteinGeneAssociation;
import pt.uminho.ceb.biosystems.merlin.utilities.containers.gpr.ReactionsGPR_CI;
import pt.uminho.ceb.biosystems.merlin.utilities.containers.model.MetaboliteContainer;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.pair.Pair;


/**
 * @author Oscar Dias
 *
 */
public class ModelAPI {


	private static final int BATCH_SIZE = 500;
	
	
	public static void swapReactantsAndProducts(String reactionID, Statement statement) throws SQLException {
		
		
		ResultSet rs = statement.executeQuery("SELECT * FROM stoichiometry WHERE reaction_idreaction = '"+reactionID+"';");
		
		Map<String, Set<String>> stoichiometryMetabolite = new HashMap<>();
		
		while (rs.next()) {
			
			Set<String> set = new HashSet<>();
			if(stoichiometryMetabolite.containsKey(rs.getString("stoichiometric_coefficient")))
					set = stoichiometryMetabolite.get(rs.getString("stoichiometric_coefficient"));
			
			set.add(rs.getString("compound_idcompound"));
					
			stoichiometryMetabolite.put(rs.getString("stoichiometric_coefficient"),set);
			
		}
		
		for (String key : stoichiometryMetabolite.keySet()) {
			
			String stoichiometry = key;
			
			if(stoichiometry.startsWith("-"))
				stoichiometry = stoichiometry.replace("-", "");
			else
				stoichiometry = "-".concat(stoichiometry);
			
			for (String metabliteID : stoichiometryMetabolite.get(key))
				statement.execute("UPDATE stoichiometry SET stoichiometric_coefficient = '"+stoichiometry+"' WHERE reaction_idreaction = '"+reactionID+"' AND  compound_idcompound = "+metabliteID+" ;");
		}
	}

	/**
	 * Load gene to model.
	 * 
	 * @param geneNames
	 * @param sequence_id
	 * @param chromosome
	 * @param statement
	 * @param informationType
	 * @param databaseType
	 * @return
	 * @throws SQLException
	 */
	public static String loadGene(Pair<String,String> geneNames, String sequence_id, String chromosome,
			Statement statement, String informationType, DatabaseType databaseType) throws SQLException {

		String locusTag = geneNames.getA();
		String geneName = geneNames.getB();

		//ResultSet rs = statement.executeQuery("SELECT idgene FROM gene WHERE locusTag = '"+locusTag+"' AND sequence_id = '"+sequence_id+"';");
		ResultSet rs = statement.executeQuery("SELECT idgene FROM gene WHERE sequence_id = '"+sequence_id+"';");

		if(!rs.next()) {

			String aux1 = "", aux2 = "";

			if(chromosome!=null && !chromosome.isEmpty()) {

				rs.close();
				rs = statement.executeQuery("SELECT idchromosome FROM chromosome WHERE name = '"+chromosome+"'");

				if(!rs.next()) {

					statement.execute("INSERT INTO chromosome (name) VALUES('"+chromosome+"')");
					rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
					rs.next();
				}

				aux1 = "chromosome_idchromosome, ";
				aux2 = ","+ rs.getString(1);
			}
			statement.execute("INSERT INTO gene (locusTag, sequence_id,"+aux1+"origin) VALUES('"+locusTag+"','"+sequence_id+"' "+aux2+",'"+informationType+"')");
			rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
			rs.next();

		}
		String geneID = rs.getString(1);
		rs.close();

		if(geneName!=null)
			statement.execute("UPDATE gene SET name = '"+DatabaseUtilities.databaseStrConverter(geneName,databaseType)+"' WHERE sequence_id = '"+sequence_id+"'");

		return geneID;
	}

	/**
	 * Retrieve queries from model.
	 * 
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static Map<Integer, String> getQueries(Statement statement) throws SQLException {

		Map<Integer, String> ret = new HashMap<>();

		String query = "SELECT s_key, query FROM geneHomology;";

		ResultSet rs = statement.executeQuery(query);

		while(rs.next())	
			ret.put(rs.getInt(1), rs.getString(2));

		return ret;
	}

	/**
	 * Retrieve all database genes not integrated from homology.
	 * 
	 * @param statement
	 * @return
	 * @throws SQLException 
	 */
	public static Set<String> getAllDatabaseGenes(Statement statement) throws SQLException{

		Set<String> locusTag = new TreeSet<String>();

		//		String query = "SELECT locusTag FROM gene";
		String query = "SELECT sequence_id FROM gene";

		ResultSet rs = statement.executeQuery(query);

		while(rs.next())
			locusTag.add(rs.getString(1));

		rs.close();

		return locusTag;
	}


	/**
	 * Retrieve all chromosomes.
	 *
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, String> getChromosomes(Statement statement) throws SQLException {

		Map<String, String> existingChromosome = new HashMap<> ();

		//		 "SELECT gene.name, locusTag, chromosome.name FROM gene "+
		//			"INNER JOIN chromosome ON (idchromosome=chromosome_idchromosome) ";

		String query = "SELECT gene.name, sequence_id, chromosome.name FROM gene "+
				"INNER JOIN chromosome ON (idchromosome=chromosome_idchromosome) ";

		ResultSet rs = statement.executeQuery(query);

		while(rs.next()) {

			//			String name = "";
			//			if(rs.getString(1)!=null)
			//				name = rs.getString(1);

			//existingNames.put(rs.getString(2), name);
			//this.existingECNumbers.put(rs.getString(2), new TreeSet<String>());
			//existingProducts.put(rs.getString(2), "");
			existingChromosome.put(rs.getString(2), rs.getString(3));
		}

		return existingChromosome;
	}

	/**
	 * Retrieve all gene name aliases.
	 *
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, Set<String>> getGeneNamesAliases(Statement statement) throws SQLException {

		Map<String, Set<String>> existingGeneNamesAlias = new HashMap<> ();

		//String query = "SELECT locusTag, alias FROM gene INNER JOIN aliases ON (idgene=aliases.entity) WHERE class='g' ";
		String query = "SELECT sequence_id, alias FROM gene INNER JOIN aliases ON (idgene=aliases.entity) WHERE class='g' ";

		ResultSet rs = statement.executeQuery(query);

		while(rs.next()) {

			Set<String> aliases = new TreeSet<>();

			if(existingGeneNamesAlias.containsKey(rs.getString(1)))
				aliases = existingGeneNamesAlias.get(rs.getString(1));

			aliases.add(rs.getString(2));
			existingGeneNamesAlias.put(rs.getString(1), aliases);
		}

		return existingGeneNamesAlias;
	}

	/**
	 * Retrieve all ec numbers associated to each gene locus tag.
	 *
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, Set<String>> getECNumbers(Statement statement) throws SQLException {

		Map<String, Set<String>> existingECNumbers = new HashMap<> ();

		//		String query = "SELECT locusTag, enzyme_ecNumber FROM gene " +
		//				"INNER JOIN subunit ON (idgene=gene_idgene) ";
		String query = "SELECT sequence_id, enzyme_ecNumber FROM gene " +
				"INNER JOIN subunit ON (idgene=gene_idgene) ";

		ResultSet rs = statement.executeQuery(query);

		while(rs.next()) {

			Set<String> enzymes  = new TreeSet<>();

			if(existingECNumbers.containsKey(rs.getString(1)))
				enzymes = existingECNumbers.get(rs.getString(1));

			enzymes.add(rs.getString(2));
			existingECNumbers.put(rs.getString(1), enzymes);
		}

		return existingECNumbers;
	}

	/**
	 * Retrieve all products.
	 *
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, String> getProducts(Statement statement) throws SQLException {

		Map<String, String> existingProducts = new HashMap<> ();

		//		String query = "SELECT locusTag, protein.name FROM gene " +
		//		"INNER JOIN subunit ON (idgene=gene_idgene) " +
		//		"INNER JOIN protein ON (subunit.enzyme_protein_idprotein=idprotein) ";
		String query = "SELECT sequence_id, protein.name FROM gene " +
				" INNER JOIN subunit ON (idgene=gene_idgene) " +
				" INNER JOIN protein ON (subunit.enzyme_protein_idprotein=idprotein) ";

		ResultSet rs = statement.executeQuery(query);

		while(rs.next())
			existingProducts.put(rs.getString(1), rs.getString(2));

		return existingProducts;
	}

	/**
	 * Retrieve all products aliases.
	 *
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, Set<String>> getProductsAliases(Statement statement) throws SQLException {

		Map<String, Set<String>> existingProductsAlias = new HashMap<> ();

		//		String query = "SELECT locusTag, alias FROM gene " +
		//				"INNER JOIN subunit ON (idgene=gene_idgene) " +
		//				"INNER JOIN aliases ON (subunit.enzyme_protein_idprotein=aliases.entity)" +
		//				" WHERE class='p' ";

		String query = "SELECT sequence_id, alias FROM gene INNER JOIN subunit ON (idgene=gene_idgene) " +
				" INNER JOIN aliases ON (subunit.enzyme_protein_idprotein=aliases.entity)" +
				" WHERE class='p' ";

		ResultSet rs = statement.executeQuery(query);

		while(rs.next()) {

			Set<String> aliases = new TreeSet<>();

			if(existingProductsAlias.containsKey(rs.getString(1)))
				aliases = existingProductsAlias.get(rs.getString(1));

			aliases.add(rs.getString(2));
			existingProductsAlias.put(rs.getString(1), aliases);
		}
		return existingProductsAlias;
	}

	/**
	 * Retrieve all pathways and the enzymes associated to each pathway.
	 *
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, Set<String>> getAllPathways(Statement statement) throws SQLException {

		Map<String, Set<String>> allPathways = new HashMap<> ();

		//		"SELECT pathway.idpathway, pathway.name, pathway_has_enzyme.enzyme_ecnumber FROM pathway " +
		//		"INNER JOIN pathway_has_enzyme ON (pathway.idpathway=pathway_idpathway) " +
		//		"ORDER BY idpathway"

		String query = "SELECT pathway.idpathway, pathway.name, pathway_has_enzyme.enzyme_ecnumber FROM pathway " +
				"INNER JOIN pathway_has_enzyme ON (pathway.idpathway=pathway_idpathway) " +
				"ORDER BY idpathway";

		ResultSet rs = statement.executeQuery(query);

		//for each enzyme in the pathways
		while(rs.next()) {

			Set<String> enz= new TreeSet<String>();

			if(allPathways.containsKey(rs.getString(2)))
				enz = allPathways.get(rs.getString(2)); 

			enz.add(rs.getString(3));
			allPathways.put(rs.getString(2), enz);
		}
		return allPathways;
	}

	/**
	 * Retrieve the pathways in Model and the enzymes associated to each pathway.
	 * 
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, Set<String>> getEnzymesPathways(Statement statement) throws SQLException {

		Map<String, Set<String>> existsPathway = new HashMap<>();

		String query = "SELECT pathway.idpathway, pathway.name, enzyme.ecnumber FROM pathway " +
				"INNER JOIN pathway_has_enzyme ON (pathway.idpathway=pathway_idpathway) " +
				"INNER JOIN enzyme ON (enzyme.ecnumber=pathway_has_enzyme.enzyme_ecnumber) " +
				"WHERE enzyme.inModel ORDER BY idpathway";

		ResultSet rs = statement.executeQuery(query);

		//for each enzyme in the pathways in the model
		while(rs.next()) {

			Set<String> enz= new TreeSet<String>();

			if(existsPathway.containsKey(rs.getString(2)))
				enz = existsPathway.get(rs.getString(2)); 

			enz.add(rs.getString(3));
			existsPathway.put(rs.getString(2), enz);
		}
		return existsPathway;
	}

	/**
	 * Load Enzyme Information
	 * Returns reactions associated to the given enzymes in database.
	 * 
	 * @param idGene
	 * @param ecNumber
	 * @param proteinName
	 * @param statement
	 * @param integratePartial
	 * @param integrateFull
	 * @param insertProductNames
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, List<String>> loadEnzymeGetReactions(String idGene, Set<String> ecNumber, String proteinName, Statement statement, boolean integratePartial, boolean integrateFull, boolean insertProductNames, DatabaseType databaseType) throws SQLException {

		String aux = " AND originalReaction = " + !ProjectAPI.isCompartmentalisedModel(statement);

		String idProtein = null;
		Map<String, List<String>> enzymesReactions = new HashMap<>();
		ResultSet resultSet = statement.executeQuery("SELECT enzyme_ecnumber FROM subunit WHERE gene_idgene = "+idGene);
		Set<String> ecs = new HashSet<>();
		while(resultSet.next())
			ecs.add(resultSet.getString(1));

		for(String ec: ecs)
			if(!ecNumber.contains(ec))
				statement.execute("DELETE FROM subunit WHERE gene_idgene = "+idGene+" AND enzyme_ecnumber='"+ec+"'");

		resultSet = statement.executeQuery("SELECT * FROM subunit WHERE gene_idgene = "+idGene);

		for(String enzyme : ecNumber) {
			
			List<String> reactions_ids = new ArrayList<String>();

			if(((enzyme.contains(".-") && integratePartial) || (!enzyme.contains(".-") && integrateFull)) && !enzyme.isEmpty()) {

				resultSet = statement.executeQuery("SELECT protein_idprotein FROM enzyme WHERE ecnumber = '"+enzyme+"'");

				boolean go = false;
				
				if(resultSet.next()) {

					idProtein = resultSet.getString(1);
					
					resultSet= statement.executeQuery("SELECT inModel FROM enzyme WHERE protein_idprotein="+idProtein+" AND ecnumber='"+enzyme+"'");
					resultSet.next();
					go = !resultSet.getBoolean(1);
					
				}
				else {

					if(proteinName==null)
						proteinName = enzyme;

					ResultSet rSet = statement.executeQuery("SELECT idprotein FROM protein WHERE name = '"+DatabaseUtilities.databaseStrConverter(proteinName, databaseType)+"'");
					if(!rSet.next()) {

						statement.execute("INSERT INTO protein (name) VALUES('"+DatabaseUtilities.databaseStrConverter(proteinName, databaseType)+"')");
						rSet = statement.executeQuery("SELECT LAST_INSERT_ID()");
						rSet.next();
						insertProductNames = false;
					}
					idProtein = rSet.getString(1);
					rSet.close();
					
					statement.execute("INSERT INTO enzyme (protein_idprotein, ecnumber, inModel, source) VALUES("+idProtein+",'"+enzyme+"',true,'HOMOLOGY')");
					go = true;
				}	
				
				if(go) {
					
					statement.execute("UPDATE enzyme SET inModel = true, source = 'HOMOLOGY' WHERE protein_idprotein="+idProtein+" AND ecnumber='"+enzyme+"'");

					if(!enzyme.contains(".-")) {

						resultSet= statement.executeQuery("SELECT DISTINCT idreaction FROM reaction " +
								" INNER JOIN reaction_has_enzyme ON reaction_has_enzyme.reaction_idreaction = idreaction " +
								" INNER JOIN pathway_has_enzyme ON pathway_has_enzyme.enzyme_protein_idprotein = reaction_has_enzyme.enzyme_protein_idprotein  " +
								" INNER JOIN pathway_has_reaction ON pathway_has_enzyme.pathway_idpathway = pathway_has_reaction.pathway_idpathway  " +
								" WHERE pathway_has_reaction.reaction_idreaction = idreaction " + aux  +
								" AND reaction_has_enzyme.enzyme_protein_idprotein = '"+idProtein+"' " +
								" AND reaction_has_enzyme.enzyme_ecnumber = '"+enzyme+"'");
						
						while(resultSet.next())
							reactions_ids.add(resultSet.getString(1));

						resultSet= statement.executeQuery("SELECT idreaction FROM reactions_view_noPath_or_noEC " +
								"INNER JOIN reaction_has_enzyme ON reaction_has_enzyme.reaction_idreaction=idreaction " +
								"WHERE enzyme_protein_idprotein = "+idProtein+" AND enzyme_ecnumber = '"+enzyme+"'");

						while(resultSet.next())
							reactions_ids.add(resultSet.getString(1));

						for(String idreaction: reactions_ids){
							statement.execute("UPDATE reaction SET inModel = true, source = 'HOMOLOGY' WHERE idreaction = '"+idreaction+"'");
						}

					}
				}

				resultSet = statement.executeQuery("SELECT enzyme_protein_idprotein FROM subunit WHERE gene_idgene = "+idGene+" AND enzyme_protein_idprotein = "+idProtein+" AND enzyme_ecnumber='"+enzyme+"'");
				if(!resultSet.next())
					statement.execute("INSERT INTO subunit (gene_idgene, enzyme_protein_idprotein, enzyme_ecnumber) VALUES ("+idGene+","+idProtein+",'"+enzyme+"')");


				if(insertProductNames)
					statement.execute("INSERT INTO aliases (class, entity, alias) VALUES ('p',"+idProtein+",'"+DatabaseUtilities.databaseStrConverter(proteinName, databaseType)+"')");
			}
			enzymesReactions.put(enzyme, reactions_ids);
		}
		resultSet.close();
		return enzymesReactions;
	}


	/**
	 * Method for loading compartments into database.
	 * It requires a Map previously obtained with the database IDs of the compartments.
	 * 
	 * @param idGene
	 * @param compartmentsDatabaseIDs
	 * @param statement
	 * @param primaryCompartment
	 * @param scorePrimaryCompartment
	 * @param secondaryCompartmens
	 * @throws SQLException
	 */
	public static void loadGenesCompartments(String idGene, Map<String,String> compartmentsDatabaseIDs, Statement statement, String primaryCompartment, double scorePrimaryCompartment, Map<String, Double> secondaryCompartmens) throws SQLException {

		DecimalFormatSymbols separator = new DecimalFormatSymbols();
		separator.setDecimalSeparator('.');
		DecimalFormat df = new DecimalFormat("#.##", separator);

		try {

			ResultSet rs = statement.executeQuery("SELECT gene_idgene FROM gene_has_compartment " +
					"WHERE gene_idgene = "+idGene+" AND primaryLocation;");

			if(!rs.next())
				statement.execute("INSERT INTO gene_has_compartment (gene_idgene, compartment_idcompartment, primaryLocation, score) " +
						"VALUES("+idGene+","+compartmentsDatabaseIDs.get(primaryCompartment)+","+true+","+df.format(scorePrimaryCompartment)+")");

			List<String> compartments = new ArrayList<>();

			for(String loc : secondaryCompartmens.keySet())
				compartments.add(loc);

			for(String compartment:compartments) {

				rs = statement.executeQuery("SELECT gene_idgene " +
						"FROM gene_has_compartment " +
						"WHERE gene_idgene = "+idGene+" " +
						"AND compartment_idcompartment = "+compartmentsDatabaseIDs.get(compartment)+"  ;");

				if(!rs.next())
					statement.execute("INSERT INTO gene_has_compartment (gene_idgene, compartment_idcompartment, primaryLocation, score) " +
							"VALUES("+idGene+","+compartmentsDatabaseIDs.get(compartment)+",false,"+df.format(secondaryCompartmens.get(compartment))+")");
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Method for retrieving the compartments database identifiers.
	 * 
	 * @param primaryCompartment
	 * @param primaryCompartmentAbb
	 * @param secondaryCompartmens
	 * @param secondaryCompartmensAbb
	 * @param compartmentsDatabaseIDs
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static Map<String,String> getCompartmentsDatabaseIDs(String primaryCompartment, String primaryCompartmentAbb, Map<String, Double> secondaryCompartmens, Map<String, String> secondaryCompartmensAbb, Map<String,String> compartmentsDatabaseIDs, Statement statement) throws SQLException {

		List<String> compartments = new ArrayList<String>();
		compartments.add(primaryCompartment);

		for(String loc : secondaryCompartmens.keySet())
			compartments.add(loc);

		for(String compartment:compartments) {

			String abb = primaryCompartmentAbb;

			if(secondaryCompartmensAbb.containsKey(compartment))
				abb = secondaryCompartmensAbb.get(compartment);

			abb = abb.toUpperCase();

			if(!compartmentsDatabaseIDs.containsKey(compartment)) {

				ResultSet rs = statement.executeQuery("SELECT idcompartment FROM compartment WHERE name = '"+compartment+"';");

				if(!rs.next()) {

					statement.execute("INSERT INTO compartment (name, abbreviation) VALUES('"+compartment+"','"+abb+"')");
					rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
					rs.next();
				}
				compartmentsDatabaseIDs.put(compartment, rs.getString(1));
				rs.close();
			}
		}
		return compartmentsDatabaseIDs;
	} 

	/**
	 * Method for loading not original reactions into database.
	 * 
	 * @param idCompartment
	 * @param inModel
	 * @param ecNumber
	 * @param statement
	 * @param isTransport
	 * @param databaseType
	 * @param name
	 * @param equation
	 * @param reversible
	 * @param generic
	 * @param spontaneous
	 * @param nonEnzymatic
	 * @param reactionSource
	 * @param notes
	 * @param proteins
	 * @param enzymes
	 * @param ecNumbers
	 * @param pathways
	 * @param compounds
	 * @param compartments
	 * @param stoichiometry
	 * @param chains
	 * @throws SQLException
	 */
	public static void loadReaction(int idCompartment, String boolean_rule, boolean inModel, String ecNumber, Statement statement, boolean isTransport, DatabaseType databaseType, String name, String equation, boolean reversible, boolean generic, boolean spontaneous, 
			boolean nonEnzymatic, String reactionSource, String notes, List<String> proteins, List<String> enzymes, Map<String, List<String>> ecNumbers, List<String> pathways, List<Integer> compounds, List<Integer> compartments, List<String> stoichiometry, 
			List<String> chains) throws SQLException {

		String saveNote = "";

		if(notes != null)
			saveNote = notes;

		String aux ="name = '"+DatabaseUtilities.databaseStrConverter(name, databaseType)+"_C"+idCompartment+"' AND ";

		if(isTransport)
			aux ="";

		ResultSet rs = statement.executeQuery("SELECT * FROM reaction  " +
				" WHERE "
				+ aux
				+ "equation = '"+DatabaseUtilities.databaseStrConverter(equation, databaseType)+"'" +
				" AND reversible = "+reversible+
				" AND inModel = "+inModel+
				" AND isGeneric = "+generic+
				" AND isSpontaneous = "+spontaneous+
				" AND isNonEnzymatic = "+nonEnzymatic+
				" AND source = '"+reactionSource+"'" +
				" AND NOT originalReaction ;");

		boolean addCompounds = true;

		if(rs.next()) {

			addCompounds = false;
		}
		else {

			String source = reactionSource;
			if(!inModel && !isTransport)
				source = "KEGG";

			statement.execute("INSERT INTO reaction (name, equation, reversible, boolean_rule, inModel, isGeneric, isSpontaneous, isNonEnzymatic, source, originalReaction,compartment_idcompartment, notes) " +
					"VALUES('"+DatabaseUtilities.databaseStrConverter(name+"_C"+idCompartment, databaseType)+"','"+DatabaseUtilities.databaseStrConverter(equation, databaseType)+"',"
					+reversible+","+boolean_rule+","+inModel+","+generic+","+spontaneous+","+nonEnzymatic+",'"+source+"',false,"+idCompartment+", '"+saveNote+"');");

			rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
			rs.next();
			addCompounds = true;

		}

		String newReactionID = rs.getString(1);
		

		if(ecNumber==null && !proteins.isEmpty() && !enzymes.isEmpty() && proteins!=null && enzymes!=null){
//			System.out.println("Insert into reaction_has_enzyme, 1st option");
			for(int j = 0; j< proteins.size(); j++)
				ModelAPI.addReaction_has_Enzyme(proteins.get(j), enzymes.get(j), newReactionID, statement);
		}
		else if(ecNumber!=null && ecNumber!=null && !ecNumbers.isEmpty()){
//			System.out.println("Insert into reaction_has_enzyme, 2nd option");
			for(String protein_id : ecNumbers.get(ecNumber))
				ModelAPI.addReaction_has_Enzyme(protein_id, ecNumber, newReactionID, statement);
		}
		
		if(pathways!=null && !pathways.isEmpty()){
			
//			System.out.println("Insert into pathway_has_reaction");

			for(String idPathway : pathways)
				ModelAPI.addPathway_has_Reaction(idPathway, newReactionID, statement);
		}
		
		if(addCompounds) {

			for(int j = 0 ; j < compounds.size(); j++ ) {

				int newCOmpartmentID = idCompartment;

				if(isTransport)
					newCOmpartmentID = compartments.get(j);

				rs = statement.executeQuery("SELECT * FROM stoichiometry" +
						" WHERE reaction_idreaction = "+newReactionID+
						" AND compartment_idcompartment = "+newCOmpartmentID+
						" AND compound_idcompound = "+compounds.get(j)+
						" AND stoichiometric_coefficient = '"+stoichiometry.get(j)+ "' "+
						" AND numberofchains = '"+chains.get(j)+ "' ;");

				if(!rs.next()){
//					System.out.println("Insert into stoichiometry");

					statement.execute("INSERT INTO stoichiometry (reaction_idreaction, compound_idcompound, compartment_idcompartment, stoichiometric_coefficient, numberofchains) " +
							"VALUES("+newReactionID+","+compounds.get(j)+","+newCOmpartmentID+",'"+stoichiometry.get(j)+ "','"+chains.get(j)+ "');");
				}
			}
		}

		rs.close();
	}


	/**
	 * Reaction has enzyme loader.
	 * 
	 * @param idprotein
	 * @param ecNumber
	 * @param idReaction
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	private static boolean addReaction_has_Enzyme(String idprotein, String ecNumber, String idReaction, Statement statement) throws SQLException {

		ResultSet rs = statement.executeQuery("SELECT * FROM reaction_has_enzyme " +
				"WHERE reaction_idreaction = "+idReaction+" " +
				"AND enzyme_protein_idprotein = "+idprotein+" " +
				"AND enzyme_ecnumber = '"+ecNumber+"';");

		if(rs.next())
			return false;
		else
			statement.execute("INSERT INTO reaction_has_enzyme (reaction_idreaction, enzyme_protein_idprotein, enzyme_ecnumber) " +
					"VALUES("+idReaction+","+idprotein+",'"+ecNumber+"');");

		rs.close();

		return true;
	}


	/**
	 * Pathway has reaction loader.
	 * 
	 * @param idPathway
	 * @param idReaction
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	private static boolean addPathway_has_Reaction(String idPathway, String idReaction, Statement statement) throws SQLException {

		ResultSet rs = statement.executeQuery("SELECT * FROM pathway_has_reaction " +
				"WHERE reaction_idreaction = '"+idReaction+"' " +
				"AND pathway_idpathway = '"+idPathway+"';");

		if(rs.next())
			return false;
		else
			statement.execute("INSERT INTO pathway_has_reaction (reaction_idreaction, pathway_idpathway) " +
					"VALUES("+idReaction+","+idPathway+");");

		rs.close();
		return true;
	}

	/**
	 * Determine if compartment information for gene is loaded.
	 * @param idGene
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static boolean isGeneCompartmentLoaded(String idGene, Statement statement) throws SQLException {

		boolean ret = false;

		ResultSet rs = statement.executeQuery("SELECT * FROM gene_has_compartment WHERE gene_idgene="+idGene+";");

		if(rs.next())
			ret=true;

		return ret;
	}


	/**
	 * Method for retrieving reaction containers associated to reactions.
	 * 
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Map<String, Object>> getEnzymesReactionsMap(Statement statement, boolean isTransporters ) throws SQLException {

		String aux = "<>";
		if(isTransporters)
			aux="=";

		Map<String, Map<String, Object>> reactionsMap = new HashMap<>();

		ResultSet rs = statement.executeQuery("SELECT name, equation, reversible, inModel, isGeneric, isSpontaneous, isNonEnzymatic, source, idreaction, lowerBound, upperBound, notes " +
				" FROM reaction " 
				+ " WHERE source "+aux+" 'TRANSPORTERS' AND originalReaction;"
				);

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

			Map<String, Object> subMap = new HashMap<>();

			subMap.put("name", name);

			subMap.put("equation", equation);

			subMap.put("reversible", reversible);

			subMap.put("inModel", inModel);

			subMap.put("isGeneric", isGeneric);

			subMap.put("isSpontaneous", isSpontaneous);

			subMap.put("isNonEnzymatic", isNonEnzymatic);

			subMap.put("source", source);

			subMap.put("id", id);

			subMap.put("lowerBound", lowerBound);

			subMap.put("upperBound", upperBound);

			subMap.put("notes", notes);

			reactionsMap.put(id, subMap);
		}



		rs = statement.executeQuery("SELECT reaction_idreaction, enzyme_protein_idprotein, enzyme_ecnumber  FROM reaction_has_enzyme;");

		while (rs.next()) {

			List<Pair<String, String>> proteinsPairs = new ArrayList<>();

			if(reactionsMap.containsKey(rs.getString(1))) {

				if(reactionsMap.get(rs.getString(1)).containsKey("proteins"))			
					proteinsPairs = (List<Pair<String, String>>) reactionsMap.get(rs.getString(1)).get("proteins");

				proteinsPairs.add(new  Pair<>(rs.getString(2), rs.getString(3)));
				reactionsMap.get(rs.getString(1)).put("proteins", proteinsPairs);
			}

		}

		rs = statement.executeQuery("SELECT reaction_idreaction, pathway_idpathway FROM pathway_has_reaction;");
		while (rs.next()){

			List<String> pathways = new ArrayList<>();

			if(reactionsMap.containsKey(rs.getString(1))) {

				if(reactionsMap.get(rs.getString(1)).containsKey("pathways"))
					pathways = (List<String>) reactionsMap.get(rs.getString(1)).get("pathways");

				pathways.add(rs.getString(2));

				reactionsMap.get(rs.getString(1)).put("pathways",pathways);
			}
		}



		rs = statement.executeQuery("SELECT * FROM stoichiometry "
				+ " INNER JOIN reaction ON stoichiometry.reaction_idreaction = reaction.idreaction " +
				" WHERE source <> 'TRANSPORTERS' AND originalReaction;");

		while (rs.next()) {
			List<String[]> entry = new ArrayList<>();
			if(reactionsMap.containsKey(rs.getString(2))) {

				if(reactionsMap.get(rs.getString(2)).containsKey("entry"))
					entry = (List<String[]>) reactionsMap.get(rs.getString(2)).get("entry");

				String[] ent = new String[4];
				ent[0] = rs.getString(3);
				ent[1] = rs.getString(5);
				ent[2] = rs.getString(6);
				ent[3] = rs.getString(4);
				entry.add(ent);


				reactionsMap.get(rs.getString(2)).put("entry",entry);
			}
		}
		return reactionsMap;
	}


	/**
	 * Method for retrieving the reactions container for a reaction.
	 * 
	 * @param idReaction
	 * @param statement
	 * @return
	 * @throws SQLException 
	 */
	public static Map<String, Object> getDatabaseReactionContainer(String idReaction, Statement statement) throws SQLException {

		ResultSet rs = statement.executeQuery("SELECT name, equation, reversible, inModel, isGeneric, isSpontaneous, isNonEnzymatic, source, idreaction, lowerBound, upperBound, notes " +
				" FROM reaction " 
				+ " WHERE idreaction = "+idReaction+";");
		
		Map<String, Object> subMap = new HashMap<>();

		if (rs.next()) {
			
//			System.out.println("Getting reactionContainer for: "+rs.getString(1));
			
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

//			Map<String, Object> subMap = new HashMap<>();

			subMap.put("name", name);

			subMap.put("equation", equation);

			subMap.put("reversible", reversible);

			subMap.put("inModel", inModel);

			subMap.put("isGeneric", isGeneric);

			subMap.put("isSpontaneous", isSpontaneous);

			subMap.put("isNonEnzymatic", isNonEnzymatic);

			subMap.put("source", source);

			subMap.put("id", id);

			subMap.put("lowerBound", lowerBound);

			subMap.put("upperBound", upperBound);

			subMap.put("notes", notes);

			List<Pair<String, String>> proteinsPairs = new ArrayList<>();
			rs = statement.executeQuery("SELECT reaction_idreaction, enzyme_protein_idprotein, enzyme_ecnumber  " +
					"FROM reaction_has_enzyme WHERE reaction_idreaction = "+idReaction+";");
			
			while (rs.next())
				proteinsPairs.add(new  Pair<>(rs.getString(2), rs.getString(3)));
			
			subMap.put("proteins", proteinsPairs);

			List<String> pathways = new ArrayList<>();
			rs = statement.executeQuery("SELECT reaction_idreaction, pathway_idpathway FROM pathway_has_reaction "
					+ " WHERE reaction_idreaction = "+idReaction+";");
			
			while (rs.next())
				pathways.add(rs.getString(2));
			
			subMap.put("pathways", pathways);

			List<String[]> entry = new ArrayList<>();
			rs = statement.executeQuery("SELECT * FROM stoichiometry "
					+ "INNER JOIN reaction ON stoichiometry.reaction_idreaction = reaction.idreaction " +
					" WHERE idreaction = "+idReaction+";");
			while (rs.next()) {

				String[] ent = new String[4];
				ent[0] = rs.getString(3);
				ent[1] = rs.getString(5);
				ent[2] = rs.getString(6);
				ent[3] = rs.getString(4);
				entry.add(ent);
			}
			
			subMap.put("entry", entry);
		}

		rs.close();

		return subMap;
	}

	/**
	 * Method for retrieving compartments abbreviations from compartments identifiers.
	 * 
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static Map<Integer,String> getIdCompartmentAbbMap(Statement statement) throws SQLException {

		Map<Integer,String> idCompartmentMap = new HashMap<>();

		ResultSet rs = statement.executeQuery("SELECT idcompartment, abbreviation FROM compartment;");

		while(rs.next())
			idCompartmentMap.put(rs.getInt(1), rs.getString(2));

		return idCompartmentMap;
	}

	/**
	 * Method for retrieving compartments identifiers from compartments abbreviations.
	 * 
	 * @return Map<String,String>
	 * @throws SQLException 
	 */
	public static Map<String,Integer> getCompartmentAbbIdMap(Statement statement) throws SQLException {

		Map<String,Integer> idCompartmentAbbIdMap = new HashMap<>();

		ResultSet rs = statement.executeQuery("SELECT idcompartment, abbreviation FROM compartment;");

		while(rs.next())
			idCompartmentAbbIdMap.put(rs.getString(2).toLowerCase(),rs.getInt(1));

		return idCompartmentAbbIdMap;
	}

	/**
	 * Method for returning existing compartments.
	 * 
	 * @param statement
	 * @return Set<String>
	 * @throws SQLException
	 */
	public static Set<String> getCompartments(Statement statement) throws SQLException {

		Set<String> compartments = new HashSet<>();

		ResultSet rs = statement.executeQuery("SELECT name FROM compartment;");

		while (rs.next())
			compartments.add(rs.getString(1));

		rs.close();
		return compartments;
	}

	/**
	 * Retrieve the compartments allocation for a given enzyme.
	 * 
	 * @param ecNumber
	 * @param statement
	 * @return List<String>
	 * @throws SQLException
	 */
	public static List<Integer> getEnzymeCompartments(String ecNumber, Statement statement) throws SQLException {

		List<Integer> compartments = new ArrayList<>();
		
		ResultSet rs = statement.executeQuery("SELECT DISTINCT compartment_idcompartment, enzyme_ecnumber, enzyme_protein_idprotein" +
				" FROM subunit" +
				" INNER JOIN gene_has_compartment ON subunit.gene_idgene = gene_has_compartment.gene_idgene" +
				" WHERE enzyme_ecnumber = '"+ecNumber+"';");

		while(rs.next())
			compartments.add(rs.getInt(1));

		return compartments;
	}

	/**
	 * method for updating the Locus Tag
	 * 
	 * @param oldLocusTag
	 * @param newLocusTag
	 * @param statement
	 * @throws SQLException 
	 */
	public static void updateLocusTag(String oldLocusTag, String newLocusTag, Statement statement) throws SQLException {

		ResultSet rs = statement.executeQuery("SELECT idgene FROM gene WHERE locusTag = '"+oldLocusTag+"';");
		rs.next();
		String idgene = rs.getString(1);

		rs = statement.executeQuery("SELECT idgene FROM gene WHERE locusTag = '"+newLocusTag+"';");

		if(rs.next()) {

			String newLocusTagID = rs.getString(1);
			statement.execute("UPDATE subunit SET gene_idgene = '"+newLocusTagID+"' WHERE gene_idgene = '"+idgene+"'");
			statement.execute("DELETE FROM gene WHERE idgene = '"+idgene+"'");
		}
		else {

			statement.execute("UPDATE gene SET locusTag = '"+newLocusTag+"' WHERE idgene = '"+idgene+"'");
		}

		statement.execute("INSERT INTO aliases (class, entity, alias) VALUES('g','"+idgene+"','"+oldLocusTag+"')");
		rs.close();

	}

	/**
	 * Method for loading gene information retrieved from homology data for a given sequence_id.
	 * 
	 * @param sequence_id
	 * @param statement
	 * @param informationType
	 * @return String
	 * @throws SQLException
	 */
	public static String loadGeneLocusFromHomologyData (String sequence_id, Statement statement, DatabaseType databaseType, String informationType) throws SQLException {

		String locusTag = sequence_id, name = null;

		ResultSet rs = statement.executeQuery("SELECT locusTag, gene FROM geneHomology WHERE query = '"+sequence_id+"';");

		if(rs.next()) {

			locusTag = rs.getString(1);
			name = rs.getString(2);
		}

		return ModelAPI.loadGene(locusTag, sequence_id, name, null, null, null, null, statement, databaseType, informationType);
	}


	/**
	 * Load Gene Information
	 * Returns gene id in database.
	 * 
	 * @param locusTag
	 * @param sequence_id
	 * @param geneName
	 * @param chromosome
	 * @param statement
	 * @param informationType
	 * @return String
	 * @throws SQLException
	 */
	public static String loadGene(String locusTag, String sequence_id, String geneName, String chromosome, String direction, String left_end, String right_end, Statement statement, DatabaseType databaseType, String informationType) throws SQLException {

		//"SELECT idgene, origin FROM gene WHERE locusTag = '"+locusTag+"' AND sequence_id = '"+sequence_id+"';"
		String query = "SELECT idgene, origin FROM gene WHERE sequence_id = '"+sequence_id+"';";

		ResultSet rs = statement.executeQuery(query);
		String geneID = null;
		if(rs.next()) {

			String informationType_db = rs.getString(2);

			geneID = rs.getString(1);

			if(!informationType.equalsIgnoreCase(informationType_db))
				statement.execute("UPDATE gene SET origin = '"+informationType+"' WHERE sequence_id = '"+sequence_id+"'");

			query = "SELECT idgene FROM gene WHERE locusTag = '"+locusTag+"' AND sequence_id = '"+sequence_id+"';";

			rs = statement.executeQuery(query);

			if(!rs.next() && !locusTag.equalsIgnoreCase(sequence_id))
				statement.execute("UPDATE gene SET locusTag = '"+locusTag+"' WHERE sequence_id = '"+sequence_id+"'");
		}
		else {

			String aux1 = "", aux2 = "", aux3 = "", aux4 = "", aux5 = "", aux6 = "", aux7 = "", aux8 = "" ;

			if(chromosome!=null && !chromosome.isEmpty()) {

				rs.close();
				rs = statement.executeQuery("SELECT idchromosome FROM chromosome WHERE name = '"+chromosome+"'");

				if(!rs.next()) {

					statement.execute("INSERT INTO chromosome (name) VALUES('"+chromosome+"')");
					rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
					rs.next();
				}

				aux1 = "chromosome_idchromosome, ";
				aux2 = rs.getString(1)+", ";
			}

			if(direction!=null) {

				aux3 = ", transcription_direction";
				aux4 = ",'"+direction+"'";
			}

			if(direction!=null) {

				aux5 = ", left_end_position";
				aux6 = ",'"+left_end+"'";
			}

			if(direction!=null) {

				aux7 = ", right_end_position";
				aux8 = ",'"+right_end+"'";
			}


			statement.execute("INSERT INTO gene (locusTag, sequence_id, "+aux1+" origin"+aux3+aux5+aux7+") "
					+ "VALUES('"+locusTag+"','"+sequence_id+"', "+aux2+"'"+informationType+"'"+aux4+aux6+aux8+")");
			rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
			rs.next();
			geneID = rs.getString(1);
		}

		rs.close();

		if(geneName!=null)
			statement.execute("UPDATE gene SET name = '"+DatabaseUtilities.databaseStrConverter(geneName, databaseType)+"' WHERE sequence_id = '"+sequence_id+"'");

		return geneID;
	}

	/**
	 * @param connection
	 * @return
	 */
	public static String getEbiBlastDatabase(Connection connection) {

		String ret = null;
		try  {

			Statement stmt = connection.createStatement();

			ResultSet rs = stmt.executeQuery("SELECT program, databaseID FROM homologySetup;");

			while(rs.next()) {

				if(rs.getString(1).equalsIgnoreCase("ebi-blastp"))
					ret=rs.getString(2);
			}
			stmt.close();
		} 
		catch (SQLException e) {e.printStackTrace();}
		return ret;
	}

	/**
	 * @param connection
	 * @return
	 */
	public static String getNcbiBlastDatabase(Connection connection) {

		String ret = null;
		try  {

			Statement stmt = connection.createStatement();

			ResultSet rs = stmt.executeQuery("SELECT program, databaseID FROM homologySetup;");

			while(rs.next())
				if(rs.getString(1).equalsIgnoreCase("ncbi-blastp"))
					ret=rs.getString(2);

			stmt.close();
		} 
		catch (SQLException e) {e.printStackTrace();}
		return ret;
	}

	/**
	 * @param connection
	 * @return
	 */
	public static String getHmmerDatabase(Connection connection) {

		String ret = null;
		try  {

			Statement stmt = connection.createStatement();

			ResultSet rs = stmt.executeQuery("SELECT program, databaseID FROM homologySetup;");

			while(rs.next()) {

				if(rs.getString(1).equalsIgnoreCase("hmmer"))
					ret=rs.getString(2);
			}
			stmt.close();
		} 
		catch (SQLException e) {e.printStackTrace();}
		return ret;
	}

	/**
	 * @param connection
	 * @param sequence_id
	 * @return
	 */
	public static String getLocusTagFromHomologyData(Connection connection, String sequence_id) {

		String ret = null;

		Statement stmt;
		try {

			stmt = connection.createStatement();
			ret = ModelAPI.getLocusTagFromHomologyData(stmt, sequence_id);
			stmt.close();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}

		return ret;
	}

	/**
	 * @param stmt
	 * @param sequence_id
	 * @return String
	 * @throws SQLException 
	 */
	public static String getLocusTagFromHomologyData(Statement stmt, String sequence_id) throws SQLException {

		String ret = null;

		ResultSet rs = stmt.executeQuery("SELECT locusTag FROM geneHomology WHERE query = '"+sequence_id+"';");

		if(rs.next())
			ret = rs.getString(1);
		rs.close();

		return ret;
	}

	/**
	 * Update ec numbers note.
	 * 
	 * @param conn
	 * @param ec_number
	 * @param module_id
	 * @param note
	 * @throws SQLException
	 */
	public static void updateECNumberModule(Connection conn, String ec_number, int module_id) throws SQLException {

		Statement stmt = conn.createStatement();

		Set<Integer> modules = new HashSet<>(), proteins = new HashSet<>();

		if(module_id>0) {

			ResultSet rs = stmt.executeQuery("SELECT module_id, note FROM enzyme_has_module WHERE enzyme_ecnumber = '"+ec_number+"'");

			while (rs.next()) {

				if(rs.getInt(1)>0) {

					int module = rs.getInt(1);
					modules.add(module);
				}
			}
		}

		if(modules.contains(module_id)) {

			//				stmt.execute("UPDATE enzyme_has_module SET note = '"+note+"' WHERE enzyme_ecnumber='"+ec_number+"' and module_id ="+module_id);
		}
		else {

			ResultSet rs = stmt.executeQuery("SELECT protein_idprotein FROM enzyme WHERE ecnumber = '"+ec_number+"'");

			while (rs.next())
				proteins.add(rs.getInt(1));

			for(int protein : proteins)
				stmt.execute("INSERT INTO enzyme_has_module (enzyme_protein_idprotein, enzyme_ecnumber, module_id) VALUES(" + protein + ", '"+ec_number+"', " +module_id+")");
		}

		stmt.close();
	}

	/**
	 * Update ec number status.
	 * 
	 * @param conn
	 * @param ec_number
	 * @throws SQLException
	 */
	public static void updateECNumberModuleStatus(Connection conn, String ec_number, String status) throws SQLException {

		Statement stmt = conn.createStatement();

		Set<Integer> proteins = new HashSet<>();
		ResultSet rs = stmt.executeQuery("SELECT protein_idprotein FROM enzyme WHERE ecnumber = '"+ec_number+"'");

		while (rs.next())
			proteins.add(rs.getInt(1));

		for(int protein : proteins)
			stmt.execute("UPDATE enzyme SET gpr_status = '"+status+"' WHERE enzyme.ecnumber='"+ec_number+"' AND protein_idprotein="+protein);

		stmt.close();
	}

	
	/**
	 * Get locus tag ec numbers from database.
	 * 
	 * @param dba
	 * @param originalReactions 
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, List<String>> getGPRsECNumbers(Connection connection) throws SQLException {

		String originalReaction = "";
		if(ProjectAPI.isCompartmentalisedModel(connection))
			originalReaction = originalReaction.concat(" WHERE NOT originalReaction ");
		else
			originalReaction = originalReaction.concat(" WHERE originalReaction ");
		
		Map<String, List<String>> ec_numbers = new HashMap<>();

		Statement stmt = connection.createStatement();

		ResultSet rs = stmt.executeQuery("SELECT locusTag, enzyme.ecnumber, sequence_id FROM subunit "
				+ " INNER JOIN gene ON (gene.idgene = gene_idgene) "
				+ " INNER JOIN enzyme ON (subunit.enzyme_protein_idprotein = enzyme.protein_idprotein  AND subunit.enzyme_ecnumber  = enzyme.ecnumber)"
				+ " INNER JOIN reaction_has_enzyme ON ecnumber = reaction_has_enzyme.enzyme_ecnumber AND enzyme.protein_idprotein = reaction_has_enzyme.enzyme_protein_idprotein "
				+ " INNER JOIN reaction ON reaction.idreaction = reaction_has_enzyme.reaction_idreaction "
				+ originalReaction + "  AND enzyme.inModel AND reaction.inModel;"
				);

		while(rs.next()) {

			List<String> genes = new ArrayList<>();

			String gene = rs.getString(1);
			String enzyme = rs.getString(2);
			
			String seqID = rs.getString(3);

			if(ec_numbers.containsKey(enzyme))
				genes = ec_numbers.get(enzyme);
			
			if(seqID!=null && !seqID.isEmpty())
				genes.add(seqID);

			else
				genes.add(gene);

			ec_numbers.put(enzyme, genes);

		}
		rs.close();
		stmt.close();

		return ec_numbers;
	}

	
	
	/**
	 * Get locus tag ec numbers from database.
	 * 
	 * @param dba
	 * @param originalReactions 
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, List<String>> getECNumbers(Connection connection) throws SQLException {

		Map<String, List<String>> ec_numbers = new HashMap<>();

		Statement stmt = connection.createStatement();

		ResultSet rs = stmt.executeQuery("SELECT locusTag, enzyme_ecnumber FROM subunit " +
				"INNER JOIN gene ON (gene.idgene = gene_idgene)"
				);

		while(rs.next()) {

			List<String> genes = new ArrayList<>();

			String gene = rs.getString(1);
			String enzyme = rs.getString(2);

			if(ec_numbers.containsKey(enzyme))
				genes = ec_numbers.get(enzyme);

			genes.add(gene);

			ec_numbers.put(enzyme, genes);

		}
		rs.close();
		stmt.close();

		return ec_numbers;
	}

	/**
	 * Get locus tag orthologs from database.
	 * 
	 * @param ko
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, Set<String>> getOrthologs(String ko, Connection conn) throws SQLException {

		Map<String, Set<String>> ret = new HashMap<>();
		Set<String> ret_set = new HashSet<>();

		ret_set.add(ko);

		Statement stmt = conn.createStatement();

		ResultSet rs = stmt.executeQuery("SELECT locus_id FROM orthology where entry_id = '"+ko+"'");

		while(rs.next()) {

			if(rs.getString(1)!=null)
				ret.put(" :"+rs.getString(1), ret_set);
		}

		return ret;
	}


	/**
	 * Get sequenceIds from model. 
	 * 
	 * @param statement
	 * @return
	 * @throws SQLException 
	 */
	public static Map<String, List<String>> getSequenceIds(Statement statement) throws SQLException {

		Map<String, List<String>> ret = new HashMap<>();

		ResultSet rs = statement.executeQuery("SELECT entry_id, sequence_id FROM gene " +
				"INNER JOIN gene_has_orthology ON (idgene = gene_idgene)" +
				"INNER JOIN orthology ON (orthology_id = orthology.id);");

		while (rs.next()){

			List<String> sequences = new ArrayList<>();

			if(ret.containsKey(rs.getString(1))) {

				sequences = ret.get(rs.getString(1));
				sequences.add(rs.getString(2));
				ret.put(rs.getString(1), sequences);
			}
			else {

				sequences.add(rs.getString(2));
				ret.put(rs.getString(1), sequences);
			}
		}

		rs.close();
		return ret;
	}


	/**
	 * Get ec numbers with modules.
	 * 
	 * @param dba
	 * @return
	 * @throws SQLException 
	 */
	public static Set<String> getECNumbersWithModules(Connection conn) throws SQLException {

		Set<String> ec_numbers = new HashSet<>();

		Statement stmt = conn.createStatement();

		ResultSet rs = stmt.executeQuery("SELECT DISTINCT(enzyme.ecnumber) FROM enzyme WHERE gpr_status = 'PROCESSED'");

		while(rs.next())
			ec_numbers.add(rs.getString(1));

		rs.close();

		return ec_numbers;
	}

	/**
	 * Load module to model.
	 * 
	 * @param conn
	 * @param result
	 * @throws SQLException
	 */
	public static Map<String, Set<Integer>> loadModule(Connection conn, Map<String, List<ReactionProteinGeneAssociation>> result) throws SQLException {

		Map<String, Set<Integer>> genes_ko_modules = new HashMap<>();

		Statement stmt = conn.createStatement();

		for(String reaction: result.keySet()) {

			for(int i=0; i<result.get(reaction).size(); i++) {

				for(String p : result.get(reaction).get(i).getProteinGeneAssociation().keySet()) {

					List<GeneAssociation> genes_list = result.get(reaction).get(i).getProteinGeneAssociation().get(p).getGenes();

					String definition = "";

					for(int index_list = 0; index_list< genes_list.size(); index_list++) {

						GeneAssociation g = genes_list.get(index_list);

						if(index_list!=0)
							definition += " OR ";

						for(int index = 0; index< g.getGenes().size(); index++) {

							String gene  = g.getGenes().get(index);

							if(index!=0)
								definition += " AND ";  

							definition += gene;
						}
					}

					for(GeneAssociation geneAssociation : genes_list) {

						for(ModuleCI mic : geneAssociation.getModules().values()) {

							ResultSet rs = stmt.executeQuery("SELECT id, definition FROM module WHERE entry_id='"+mic.getModule()+"' AND reaction='"+reaction+"' AND definition ='"+definition+"'");

							if(!rs.next()) {

								stmt.execute("INSERT INTO module (reaction, entry_id, name, definition, type) " +
										"VALUES ('"+reaction+"', '"+mic.getModule()+"', '"+mic.getName()+"', '"+definition+"', '"+mic.getModuleType().toString()+"')");
								rs = stmt.executeQuery("SELECT LAST_INSERT_ID();");
								rs.next();
							}

							int idModule = rs.getInt(1);

							for(String gene : geneAssociation.getGenes()) {

								rs = stmt.executeQuery("SELECT * FROM orthology WHERE entry_id='"+gene+"';");

								boolean noEntry = true;
								Set<Integer> ids = new HashSet<>();

								while(rs.next()) {

									noEntry = false;
									ids.add(rs.getInt(1));
								}

								if(noEntry) { 

									stmt.execute("INSERT INTO orthology (entry_id) VALUES('"+gene+"');");
									rs = stmt.executeQuery("SELECT LAST_INSERT_ID();");
									rs.next();
									ids.add(rs.getInt(1));
								}

								for (int idGene : ids) {
									rs = stmt.executeQuery("SELECT * FROM module_has_orthology WHERE module_id="+idModule+" AND orthology_id = "+idGene+";");

									if(!rs.next()) {
										stmt.execute("INSERT INTO module_has_orthology (module_id, orthology_id) VALUES('"+idModule+"', '"+idGene+"');");
										rs = stmt.executeQuery("SELECT LAST_INSERT_ID();");
										rs.next();
									}

									Set<Integer> modules = new HashSet<>();

									if(genes_ko_modules.containsKey(gene))
										modules = genes_ko_modules.get(gene);

									modules.add(idModule);
									genes_ko_modules.put(gene, modules);
								}
							}
							rs.close();
						}
					}
				}
			}
		}

		stmt.close();
		stmt=null;
		return genes_ko_modules;
	}


	/**
	 * Run gene-protein reactions assignment.
	 * 
	 * @param threshold 
	 * @throws SQLException
	 */
	public static Map<String, ReactionsGPR_CI> runGPRsAssignment(double threshold, Connection conn) throws SQLException {


		Statement stmt = conn.createStatement();

		ResultSet rs = stmt.executeQuery("SELECT DISTINCT reaction, subunit.enzyme_ecnumber, definition, idgene, " +
				" orthology.entry_id, locusTag, gene.name, note, similarity " +
				" FROM module" +
				" INNER JOIN enzyme_has_module ON (enzyme_has_module.module_id = module.id)" +				
				" INNER JOIN subunit ON (subunit.enzyme_protein_idprotein = enzyme_has_module.enzyme_protein_idprotein)" +
				" INNER JOIN module_has_orthology ON (module_has_orthology.module_id = module.id)"+
				" INNER JOIN orthology ON (module_has_orthology.orthology_id = orthology.id)"+
				" INNER JOIN gene_has_orthology ON (gene_has_orthology.orthology_id = module_has_orthology.orthology_id AND gene_has_orthology.gene_idgene = subunit.gene_idgene)" +
				" INNER JOIN gene ON (gene_has_orthology.gene_idgene = gene.idgene)" 				 
				//+" WHERE similarity >= "+threshold				
				);

		Map<String, ReactionsGPR_CI> rpgs = new HashMap<>();

		while (rs.next()) {

			if(//rs.getString("note")==null || !rs.getString("note").equalsIgnoreCase("unannotated") || (rs.getString("note").equalsIgnoreCase("unannotated") && 
					rs.getDouble("similarity")>=threshold)
				//				)
			{

				ReactionsGPR_CI rpg = new ReactionsGPR_CI(rs.getString(1));

				if(rpgs.containsKey(rs.getString(1)))
					rpg  = rpgs.get(rs.getString(1));

				{
					ProteinsGPR_CI pga = new ProteinsGPR_CI(rs.getString(2), rs.getString(3));
					pga.addSubunit(rs.getString(3).split(" OR "));

					if(rpg.getProteins()!= null && rpg.getProteins().containsKey(rs.getString(2)))
						pga = rpg.getProteins().get(rs.getString(2));

					String idGene = rs.getString(4);

					pga.addLocusTag(rs.getString(5), idGene);

					rpg.addProteinGPR_CI(pga);
				}

				rpgs.put(rpg.getReaction(), rpg);
			}
		}

		conn.closeConnection();
		return rpgs;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//biomass
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * Get information for e-biomass.
	 * 
	 * @param data
	 * @param statment
	 * @return the pair compound identifier to molecular weight
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

	/**
	 * Add the biomass pathway to model.
	 * @param statement 
	 * 
	 * @return The pathway database identifier.
	 * @throws SQLException 
	 */
	public static String addBiomassPathway(Statement statement) throws SQLException {

		ResultSet rs = statement.executeQuery("SELECT name FROM pathway WHERE name = 'Biomass Pathway'");

		if(!rs.next()) {

			statement.execute("INSERT INTO pathway (code, name) VALUES ('B0001','Biomass Pathway');");
			rs = statement.executeQuery("SELECT name FROM pathway WHERE name = 'Biomass Pathway'");
			rs.next();
		}
		String ret = rs.getString(1);
		rs.close();

		return ret;
	}

	/**
	 * Add biomass compound to model.
	 * 
	 * @param name 
	 * @param molecularWeight 
	 * @param statement 
	 * @return the compound database identifier.
	 * @throws SQLException 
	 */
	public static String insertCompoundToDatabase(String name, double molecularWeight, Statement statement) throws SQLException {


		ResultSet rs = statement.executeQuery("SELECT * FROM compound WHERE name = '"+name+"'");

		if(!rs.next()) {
			
			System.out.println("INSERIR COFACTOR: "+name);

			statement.execute("INSERT INTO compound (name, kegg_id, entry_type, molecular_weight, hasBiologicalRoles) "
					+ "VALUES ('"+name+"','"+name+"','BIOMASS','"+molecularWeight+"',TRUE);");
			rs = statement.executeQuery("SELECT * FROM compound WHERE name = '"+name+"'");
			rs.next();
		}
		String ret = rs.getString(1);
		
		System.out.println("Cofactor ID: "+ret);
		
		rs.close();

		return ret;
	}


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//reaction
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * Method for inserting new reactions in model database.
	 * 
	 * 
	 * @param name
	 * @param equation
	 * @param reversibility
	 * @param metabolitesChains
	 * @param metabolitesCompartments
	 * @param metabolitesStoichiometry
	 * @param inModel
	 * @param enzymesInPathway
	 * @param reactionCompartment
	 * @param isSpontaneous
	 * @param isNonEnzymatic
	 * @param isGeneric
	 * @param lowerBound
	 * @param upperBound
	 * @param source
	 * @param boolean_rule
	 * @param compartmentalisedModel
	 * @param databaseType
	 * @param statement
	 * @throws Exception
	 */
	public static void insertNewReaction(String name, String equation, boolean reversibility, //Set<String> pathways, Set<String> enzymes, 
			Map<String,String> metabolitesChains, Map<String, String > metabolitesCompartments, Map<String, String> metabolitesStoichiometry, boolean inModel, Map<String, 
			Set<String>> enzymesInPathway, String reactionCompartment, boolean isSpontaneous, boolean isNonEnzymatic,
			boolean isGeneric, double lowerBound, double upperBound, String source, String boolean_rule, boolean compartmentalisedModel, DatabaseType databaseType, Statement statement) throws Exception {

		try {

			if(boolean_rule!=null)
				boolean_rule = "'"+boolean_rule+"'";

			ResultSet rs;

			if(!name.startsWith("R") && !name.startsWith("T")&& !name.startsWith("K") && !name.toLowerCase().contains("biomass"))
				name = "R_"+name;

			if(name.toLowerCase().equals("biomass"))
				name = "R_"+name;

			rs = statement.executeQuery("SELECT idreaction FROM reaction WHERE name = '" + DatabaseUtilities.databaseStrConverter(name, databaseType)
				+ "' AND originalReaction="+ ProjectAPI.isCompartmentalisedModel(statement) +";");
			if(rs.next()) {

				throw new  Exception("Reaction with the same name ("+name+") already exists. Aborting operation!");
			}
			else {

				boolean originalReaction = true;

				if(compartmentalisedModel)
					originalReaction = false;

				rs = statement.executeQuery("SELECT idcompartment FROM compartment WHERE name = '" + reactionCompartment + "'");
				rs.next();
				String idCompartment = rs.getString(1);

				statement.execute("INSERT INTO reaction (name, equation, reversible, inModel, compartment_idcompartment, " +
						"source, isSpontaneous, isNonEnzymatic, originalReaction, isGeneric, lowerBound, upperBound, boolean_rule) " +
						"VALUES('" + DatabaseUtilities.databaseStrConverter(name,databaseType) + "', '" + DatabaseUtilities.databaseStrConverter(equation,databaseType) + "', " 
						+ reversibility + ", "+ inModel+","+idCompartment+",'"+source+"', "+isSpontaneous+","+isNonEnzymatic+", "
						+originalReaction+", "+isGeneric+", "+lowerBound+", "+upperBound+","+boolean_rule+")");

				//				String idNewReaction = (this.select("SELECT LAST_INSERT_ID()"))[0][0];
				ResultSet rs1=statement.executeQuery("SELECT LAST_INSERT_ID()");
				rs1.next();
				String idNewReaction = rs1.getString(1);

				//PATHWAYS AND ENZYMES PROCESSING
				{
					Map<String,Set<String>> newPathwaysID = new TreeMap<String,Set<String>>();
					enzymesInPathway.remove("");
					{
						if(enzymesInPathway.containsKey("-1allpathwaysinreaction") && enzymesInPathway.get("-1allpathwaysinreaction").size()>0) {

							for(String enzyme : enzymesInPathway.get("-1allpathwaysinreaction")) {

								String ecnumber = enzyme.split("___")[0];

								String idProtein = enzyme.split("___")[2];

								rs = statement.executeQuery("SELECT * FROM reaction_has_enzyme WHERE enzyme_ecnumber='" + ecnumber+ "' AND enzyme_protein_idprotein = "+idProtein+" AND reaction_idreaction = "+idNewReaction );

								if(!rs.next())
									statement.execute("INSERT INTO reaction_has_enzyme (enzyme_ecnumber,enzyme_protein_idprotein,reaction_idreaction) " +
											"VALUES ('" + ecnumber + "', " +idProtein+", "+idNewReaction+") ");
							}
						}
						enzymesInPathway.remove("-1allpathwaysinreaction");
					}

					if(enzymesInPathway.size()>0) {

						for(String pathway:enzymesInPathway.keySet()) {

							rs = statement.executeQuery("SELECT idpathway FROM pathway WHERE name = '" + DatabaseUtilities.databaseStrConverter(pathway,databaseType)+ "'");
							rs.next();

							Set<String> p = new TreeSet<String>();
							if (enzymesInPathway.get(pathway).size()>0)
								p =  new TreeSet<String>(enzymesInPathway.get(pathway));

							newPathwaysID.put(rs.getString(1), p);
						}

						//when pathways are deleted, they are just removed from the pathway has reaction association
						//insert the new pathways

						for(String pathway:newPathwaysID.keySet()) {

							rs = statement.executeQuery("SELECT * FROM pathway_has_reaction WHERE pathway_idpathway = '"+pathway+"' AND reaction_idreaction = "+idNewReaction);

							if(!rs.next()) {

								statement.execute("INSERT INTO pathway_has_reaction (pathway_idpathway, reaction_idreaction) " +
										"VALUES ("+pathway+","+idNewReaction+")");

								for(String enzyme: newPathwaysID.get(pathway)) {

									String ecnumber = enzyme.split("___")[0];

									String idProtein = enzyme.split("___")[2];

									rs = statement.executeQuery("SELECT * FROM pathway_has_enzyme WHERE enzyme_ecnumber='" + ecnumber+ "' AND pathway_idpathway = "+pathway+ " AND enzyme_protein_idprotein = "+idProtein);

									if(!rs.next()) {

										statement.execute("INSERT INTO pathway_has_enzyme (pathway_idpathway, enzyme_ecnumber,enzyme_protein_idprotein) " +
												"VALUES ("+pathway+",'"+ecnumber+"',"+idProtein+")");
									}

									rs = statement.executeQuery("SELECT * FROM reaction_has_enzyme WHERE enzyme_ecnumber = '"+ecnumber+"' AND reaction_idreaction = "+idNewReaction+" AND enzyme_protein_idprotein = "+idProtein);

									if(!rs.next()) {

										statement.execute("INSERT INTO reaction_has_enzyme (enzyme_ecnumber,enzyme_protein_idprotein,reaction_idreaction) " +
												"VALUES ('"+ecnumber+"',"+idProtein+","+idNewReaction+") ");
									}
								}
							}
						}
					}
				}

				int biomass_id = -1;
				rs = statement.executeQuery("SELECT idcompound FROM compound WHERE name LIKE 'Biomass'");
				if(rs.next())
					biomass_id = rs.getInt("idcompound");

				for(String m :metabolitesStoichiometry.keySet()) {
					
					if(m!=null && !m.equalsIgnoreCase("null")){

						rs = statement.executeQuery("SELECT idcompartment FROM compartment WHERE name = '" + metabolitesCompartments.get(m) + "'");
						rs.next();
						idCompartment = rs.getString(1);
						
						statement.execute("INSERT INTO stoichiometry (stoichiometric_coefficient, compartment_idcompartment, compound_idcompound, reaction_idreaction,numberofchains) " +
								"VALUES('" + metabolitesStoichiometry.get(m) + "', '" + idCompartment +	"', '" + m.replace("-", "") + "', '" + idNewReaction + "', '" + metabolitesChains.get(m) + "')");


						if(m.replace("-", "").equalsIgnoreCase(biomass_id+"")) {

							rs = statement.executeQuery("SELECT * FROM pathway WHERE name = 'Biomass Pathway'");
							if(!rs.next()) {						

								statement.execute("INSERT INTO pathway (name, code) VALUES('Biomass Pathway', 'B0001' );");
								rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
								rs.next();
							}
							String idBiomassPath= rs.getString(1);
							rs = statement.executeQuery("SELECT * FROM pathway_has_reaction WHERE pathway_idpathway = "+idBiomassPath+ " AND reaction_idreaction = "+idNewReaction);

							if(!rs.next()) {
								statement.execute("INSERT INTO pathway_has_reaction (pathway_idpathway, reaction_idreaction) " +
										"VALUES ("+idBiomassPath+","+idNewReaction+")");
							}
						}
					}
				}
			}
			rs.close();
		}
		catch (SQLException ex) {

			ex.printStackTrace();
		}
	}


	/** Remove the selected reaction.
	 * @param statement
	 * @param reaction_id
	 */
	public static void removeSelectedReaction(Statement statement, int reaction_id){

		try{

			statement.execute("DELETE FROM reaction WHERE idreaction="+reaction_id+";");
		}
		catch (SQLException ex) {

			ex.printStackTrace();
		}

	}

	/**
	 * Check undefined Stoichiometry.
	 * @param statement
	 * @return Set<String>
	 */
	public static Set<String> checkUndefinedStoichiometry(Statement statement) {

		Set<String> undefinedStoichiometry = new HashSet<>();

		try {
			String aux = "";

			if(ProjectAPI.isCompartmentalisedModel(statement))				
				aux = aux.concat("  NOT originalReaction");
			else	
				aux = aux.concat(" originalReaction");

			ResultSet rs = statement.executeQuery("SELECT name FROM reaction INNER JOIN stoichiometry ON idreaction=reaction_idreaction "
					+ " WHERE inModel AND stoichiometric_coefficient=0 AND "+aux+" ORDER BY idreaction ASC;");

			while (rs.next())
				undefinedStoichiometry.add(rs.getString(1));
		}
		catch (SQLException ex) {

			ex.printStackTrace();
		}

		return undefinedStoichiometry;
	}

	/**
	 * Get active reactions.
	 * @param statement
	 * @param encodedOnly
	 * @param compartimentalized
	 * @param dbType
	 * @return
	 */
	public static ReactionsCapsule getActiveReactions(Statement statement, boolean encodedOnly, boolean compartimentalized,  DatabaseType dbType) {

		Map<Integer, String> ids = new TreeMap<Integer,String>(); 
		HashMap<String,String> namesIndex = new HashMap<String,String>();
		Set<String> activeReactions = new HashSet<>();
		HashMap<String,String> formulasIndex = new HashMap<String,String>();
		List<String> pathwaysList = new ArrayList<String>();
		Map <String, Integer> pathID = new TreeMap<String, Integer>();
		Set<String> pathwaysSet=new TreeSet<String>();
		Integer[] tableColumnsSize = null;

		ArrayList<Object> reactionsData = new ArrayList<Object>();

		try {

			String aux = " WHERE ";
			if(encodedOnly) //{
				aux = aux.concat(" inModel AND ");
			//				tableColumnsSize = new Integer[]{320,150,1000,110,100,75,75};
			//			}
			//			else {
			//	
			//				tableColumnsSize = new Integer[]{320,150,1000,110,100,75,75};	
			//			}

			//the first column is ignored here
			tableColumnsSize = new Integer[]{320,150,1000,110,100,75,75};

			if(ProjectAPI.isCompartmentalisedModel(statement))				
				aux = aux.concat("  NOT originalReaction");
			else	
				aux = aux.concat(" originalReaction");

			String aux2 = " CASEWHEN (pathway_name is NULL, 1, 0), ";
			if(dbType.equals(DatabaseType.MYSQL)) 
				aux2 = " IF(ISNULL(pathway_name),1,0), ";


			String view1 , view2;

			view1 = " reactions_view ";
			view2 = " reactions_view_noPath_or_noEC ";

			ResultSet rs = statement.executeQuery(" SELECT * FROM "
					+ "(SELECT * FROM "+view1+aux+" "
					+ " UNION "
					+ " SELECT * FROM "+view2+aux+") as global"
					+ " ORDER BY " 
					+ aux2
					+ " pathway_name, reaction_name");

			int r=0;

			while(rs.next()) {

				ArrayList<Object> temp = new ArrayList<Object>();
				activeReactions.add(rs.getString(2));
				ids.put(r,rs.getString(1));
				r++;
				ArrayList<Object> ql = new ArrayList<Object>();
				ql.add("");

				if(rs.getString(6)!=null) {

					ql.add(rs.getString(6));
				}
				else {

					ql.add("");
				}

				ql.add(rs.getString(2));
				ql.add(rs.getString(3));
				ql.add(rs.getString(9));
				ql.add(rs.getString(12));
				ql.add(rs.getBoolean(4));
				//			ql.add(rs.getBoolean(8)); ---------------------generic
				//if(!encodedOnly) {
				ql.add(rs.getBoolean(7));
				//}

				temp.add(ql);
				temp.add(rs.getString(1));
				if(rs.getString(5)==null) {temp.add("0");}
				else {temp.add(rs.getString(5));}
				reactionsData.add(temp);
				namesIndex.put(rs.getString(1), rs.getString(2));
				formulasIndex.put(rs.getString(1), rs.getString(3));

				if(rs.getString(6) != null) {

					pathID.put(rs.getString(6), Integer.parseInt(rs.getString(5)));
					pathwaysSet.add(rs.getString(6));
				}
			}

			rs.close();
			statement.close();
		}
		catch (SQLException ex) {ex.printStackTrace();}

		ReactionsCapsule capsule = new ReactionsCapsule(ids, namesIndex, activeReactions, formulasIndex, pathwaysList,
				pathID, pathwaysSet, tableColumnsSize, reactionsData);

		return capsule;
	}

	/**
	 * Get enzymes statistics.
	 * @param originalReaction
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<Integer> getEnzymesStats(String originalReaction, Statement stmt) throws SQLException{

		ArrayList<Integer> result = new ArrayList<>();

		int enz_num=0,hom_num=0,kegg_num=0,man_num=0,trans_num=0;
		int encoded_enz=0, encoded_hom=0, encoded_kegg=0, encoded_man=0, encoded_trans=0;

		ResultSet rs = stmt.executeQuery("SELECT DISTINCT(ecnumber), ecnumber, protein_idprotein, optimal_ph, posttranslational_modification, enzyme.inModel, enzyme.source, idprotein, protein.name," +
				"class, inchi, molecular_weight, molecular_weight_exp, molecular_weight_kd, molecular_weight_seq, pi," +
				"reaction.inModel " +
				" FROM enzyme "+
				" INNER JOIN protein ON protein.idprotein = enzyme.protein_idprotein " +
				" INNER JOIN reaction_has_enzyme ON ecnumber = reaction_has_enzyme.enzyme_ecnumber " +
				" AND protein.idprotein = reaction_has_enzyme.enzyme_protein_idprotein " +
				" INNER JOIN reaction ON reaction.idreaction = reaction_has_enzyme.reaction_idreaction " +
				originalReaction+" AND reaction.inModel IS NOT null;");			

		while(rs.next()) {

			if(rs.getString("source").equalsIgnoreCase("TRANSPORTERS")) {

				trans_num++;

				if(rs.getBoolean("enzyme.inModel"))
					encoded_trans++;
			}
			else {

				if(rs.getString("source").equalsIgnoreCase("KEGG")) {

					kegg_num++;

					if(rs.getBoolean("enzyme.inModel")){

						encoded_kegg++;
						encoded_enz++;

					}
				}

				if(rs.getString("source").equalsIgnoreCase("HOMOLOGY")) {

					hom_num++;

					if(rs.getBoolean("enzyme.inModel")){

						encoded_hom++;
						encoded_enz++;
					}
				}

				if(rs.getString("source").equalsIgnoreCase("MANUAL")) {

					man_num++;

					if(rs.getBoolean("enzyme.inModel")){

						encoded_man++;
						encoded_enz++;
					}
				}

				enz_num++;
			}
		}
		result.add(enz_num);
		result.add(hom_num);
		result.add(kegg_num);
		result.add(man_num);
		result.add(trans_num);
		result.add(encoded_enz);
		result.add(encoded_hom);
		result.add(encoded_kegg);
		result.add(encoded_man);
		result.add(encoded_trans);

		//array structure: [enz_num, hom_num, kegg_num, man_num, trans_num, encoded_enz, encoded_hom, encoded_kegg, encoded_man, encoded_trans]
		return result;
	}

	/**
	 * @param isCompartmentalisedModel
	 * @param encoded
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getAllEnzymes(boolean isCompartmentalisedModel, boolean encoded, Statement stmt) throws SQLException{

		String originalReaction = "";
		if(isCompartmentalisedModel)
			originalReaction = originalReaction.concat(" WHERE NOT originalReaction ");
		else
			originalReaction = originalReaction.concat(" WHERE originalReaction ");

		String encodedEnzyme="";
		if(encoded)
			encodedEnzyme=" AND enzyme.inModel AND reaction.inModel";

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT protein.name, enzyme.ecnumber," +
				" COUNT(DISTINCT(reaction_has_enzyme.reaction_idreaction)), enzyme.source, enzyme.inModel, reaction.inModel, idprotein," +
				" COUNT(DISTINCT(reaction.inModel)), protein.idprotein" +
				" FROM enzyme " +
				" INNER JOIN protein ON protein.idprotein = enzyme.protein_idprotein " +
				" INNER JOIN reaction_has_enzyme ON ecnumber = reaction_has_enzyme.enzyme_ecnumber " +
				" AND protein.idprotein = reaction_has_enzyme.enzyme_protein_idprotein " +
				" INNER JOIN reaction ON reaction.idreaction = reaction_has_enzyme.reaction_idreaction" +
				originalReaction+encodedEnzyme+
				" GROUP BY idprotein, ecnumber , reaction.inModel " +
				" ORDER BY ecnumber  ASC, reaction.inModel DESC;");

		while(rs.next()) {

			String[] list = new String[8];

			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);
			list[3]=rs.getString(4);
			list[4]=rs.getBoolean(5)+"";
			list[5]=rs.getBoolean(6)+"";
			list[6]=rs.getString(7);
			list[7]=rs.getString(8);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * Get proteins data.
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getProteinsData(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT idprotein, protein.name, ecnumber, optimal_ph, posttranslational_modification " +
				"FROM enzyme " +
				"INNER JOIN protein ON protein.idprotein = enzyme.protein_idprotein;");

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
	 * Get proteins data.
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getProteinsData2(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT enzyme.protein_idprotein, COUNT(gene_idgene) " +
				"FROM enzyme " +
				"INNER JOIN subunit ON subunit.enzyme_protein_idprotein = enzyme.protein_idprotein " +
				"GROUP BY enzyme.protein_idprotein;");

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
	 * Get reactions data.
	 * @param ecnumber
	 * @param aux
	 * @param id
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getReactionsData(String ecnumber, String aux, String id, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT reaction.name, reaction.equation, source, inModel, reversible FROM reaction " +
				"INNER JOIN reaction_has_enzyme ON reaction_has_enzyme.reaction_idreaction = reaction.idreaction " +
				"WHERE reaction_has_enzyme.enzyme_ecnumber = '" + ecnumber+"' " +
				"AND reaction_has_enzyme.enzyme_protein_idprotein = " + id + aux+ "" +
				" ORDER BY inModel DESC, reversible DESC, name");

		while(rs.next()){
			String[] list = new String[5];

			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);
			list[3]=rs.getBoolean(4)+"";
			list[4]=rs.getBoolean(5)+"";

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * Get gene data.
	 * @param ecnumber
	 * @param id
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getGeneData(String ecnumber, String id, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT DISTINCT gene.name, gene.locusTag, orthology.entry_id, origin, similarity, locus_id FROM enzyme " +
				"INNER JOIN subunit ON subunit.enzyme_protein_idprotein = enzyme.protein_idprotein " +
				"INNER JOIN gene ON gene.idgene = subunit.gene_idgene " +
				"LEFT JOIN gene_has_orthology ON gene.idgene = gene_has_orthology.gene_idgene " +
				"LEFT JOIN orthology ON orthology.id = orthology_id " +
				"WHERE subunit.enzyme_ecnumber = '" + ecnumber+"' " +
				"AND subunit.enzyme_protein_idprotein = " + id+";");

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
	 * Retrieves reactions information from reaction table
	 * @param stmt
	 * @param conditions
	 * @return Reaction_ID, name, equation, reversible, compartment_idcompartment, notes, lowerBound, upperBound
	 * @throws SQLException
	 */		
	public static Map<String, ArrayList<String>> getReactions (Statement stmt, String conditions) throws SQLException {

		ResultSet rs = stmt.executeQuery("SELECT DISTINCT idreaction, name, equation, reversible, compartment_idcompartment, notes, lowerBound, upperBound, boolean_rule " +
				"FROM reaction WHERE inModel AND " +conditions+ ";" );

		Map<String, ArrayList<String>> result = new HashMap<>();


		while(rs.next()) {

			ArrayList<String> list = new ArrayList<>();
			list.add(rs.getString(2));
			list.add(rs.getString(3));
			list.add(rs.getBoolean(4)+"");
			list.add(rs.getString(5));
			list.add(rs.getString(6));
			list.add(rs.getString(7));
			list.add(rs.getString(8));
			list.add(rs.getString(9));
			result.put(rs.getString(1),list);
		}

		rs.close();
		return result;		

	}

	/**
	 * Retrieves information from reaction_has_enzyme table
	 * @param stmt
	 * @return reaction_idreaction, enzyme_protein_idprotein, enzyme_ecnumber
	 * @throws SQLException
	 */		
	public static ArrayList<String[]> getEnzymeHasReaction (Statement stmt) throws SQLException{

		ResultSet rs = stmt.executeQuery("SELECT reaction_idreaction, enzyme_protein_idprotein, enzyme_ecnumber FROM reaction_has_enzyme ORDER BY reaction_idreaction" );

		ArrayList<String[]> result = new ArrayList<>();

		while(rs.next()) {

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
	 * Retrieves information from pathways_has_enzyme and pathway table
	 * @param stmt
	 * @return reaction_idreaction, pathway_idpathway, pathway.name
	 * @throws SQLException
	 */	
	public static ArrayList<String[]> getReactionPathway (Statement stmt) throws SQLException {

		ResultSet rs = stmt.executeQuery("SELECT reaction_idreaction, pathway_idpathway, pathway.name " +
				"FROM pathway_has_reaction " +
				"INNER JOIN pathway ON (pathway_idpathway = pathway.idpathway)" +
				"ORDER BY reaction_idreaction" );

		ArrayList<String[]> result = new ArrayList<>();

		while(rs.next()) {
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
	 * Retrieves information from reaction_has_enzyme and subunit and gene table
	 * @param stmt
	 * @return reaction_idreaction, name, locusTag, subunit.enzyme_ecnumber
	 * @throws SQLException
	 */	
	public static ArrayList<String[]> getReactionHasEnzyme (Statement stmt) throws SQLException{

		ResultSet rs = stmt.executeQuery("SELECT DISTINCT reaction_idreaction, name, locusTag, subunit.enzyme_ecnumber " +
				"FROM reaction_has_enzyme " +
				"INNER JOIN subunit ON (subunit.enzyme_protein_idprotein = reaction_has_enzyme.enzyme_protein_idprotein "
				+ "AND subunit.enzyme_ecnumber = reaction_has_enzyme.enzyme_ecnumber) " +
				"INNER JOIN gene ON (gene_idgene = gene.idgene) " +
//				"WHERE (note is null OR note NOT LIKE 'unannotated') " +
				"ORDER BY reaction_idreaction;");

		ArrayList<String[]> result = new ArrayList<>();

		while(rs.next()) {
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
	 * Retrieves information from reaction and stoichiometry and compound table
	 * @param stmt
	 * @param conditions
	 * @return idstoichiometry, reaction_idreaction, compound_idcompound, stoichiometry.compartment_idcompartment, " +
				"stoichiometric_coefficient, numberofchains, compound.name, compound.formula, compound.kegg_id
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getStoichiometryInfo (Statement stmt, String conditions) throws SQLException{

		ResultSet rs = stmt.executeQuery("SELECT idstoichiometry, reaction_idreaction, compound_idcompound, stoichiometry.compartment_idcompartment, " +
				"stoichiometric_coefficient, numberofchains, compound.name, compound.formula, compound.kegg_id " +
				"FROM reaction " +
				"INNER JOIN stoichiometry ON (stoichiometry.reaction_idreaction = idreaction) " +
				"INNER JOIN compound ON (stoichiometry.compound_idcompound = compound.idcompound) " +
				"WHERE inModel AND " +conditions );

		ArrayList<String[]> result = new ArrayList<>();

		while(rs.next()) {
			String[] list = new String[9];

			list[0] = rs.getString(1);
			list[1] = rs.getString(2);
			list[2] = rs.getString(3);
			list[3] = rs.getString(4);
			list[4] = rs.getString(5);
			list[5] = rs.getString(6);
			list[6] = rs.getString(7);
			list[7] = rs.getString(8);
			list[8] = rs.getString(9);
			result.add(list);
		}
		rs.close();
		return result;
	}


	/**
	 * Get pathways.
	 * @param ecnumber
	 * @param id
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getPathways(String ecnumber, String id, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT DISTINCT code, name FROM pathway_has_enzyme " +
				"INNER JOIN pathway ON pathway_has_enzyme.pathway_idpathway = pathway.idpathway " +
				"WHERE pathway_has_enzyme.enzyme_ecnumber = '" + ecnumber+"' " +
				"AND pathway_has_enzyme.enzyme_protein_idprotein = " + id);

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
	 * @param originalreactions
	 * @return ArrayList<String[]>  name, enzyme_ecnumber
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getReactionsFromModel (Statement stmt, boolean originalreactions) throws SQLException {

		ResultSet rs = stmt.executeQuery("SELECT * FROM reaction " +
				"INNER JOIN reaction_has_enzyme  ON (reaction_idreaction = reaction.idreaction) " +
				" WHERE reaction.inModel AND originalReaction="+originalreactions);

		ArrayList<String[]> result = new ArrayList<>();

		while(rs.next()) {
			String[] list = new String[2];

			list[0] = rs.getString("name");
			list[1] = rs.getString("enzyme_ecnumber");
			result.add(list);
		}	

		return result;			
	}

	/**
	 * Get data from subunit table.
	 * @param ecnumber
	 * @param id
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getDataFromSubunit(String ecnumber, String id, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT DISTINCT gpr_status, reaction, definition, module.name FROM enzyme_has_module " +
				"INNER JOIN module ON (id = module_id) " +
				"INNER JOIN enzyme ON (enzyme_ecnumber = enzyme.ecnumber AND enzyme.protein_idprotein = enzyme_protein_idprotein) " +
				"WHERE enzyme_ecnumber = '" + ecnumber+"' AND enzyme_protein_idprotein = " + id);

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
	 * Get gene table data.
	 * @param ecnumber
	 * @param id
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getGeneData2(String ecnumber, String id, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT idgene, compartment.name, primaryLocation, score, locusTag " +
				"FROM gene " +
				"INNER JOIN gene_has_compartment ON (idgene = gene_has_compartment.gene_idgene) " +
				"INNER JOIN compartment ON (idcompartment = compartment_idcompartment)" +
				"INNER JOIN subunit ON subunit.gene_idgene = idgene " +
				"WHERE subunit.enzyme_ecnumber = '" + ecnumber+"' " +
				"AND subunit.enzyme_protein_idprotein = " + id);

		while(rs.next()){
			String[] list = new String[5];

			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getBoolean(3)+"";
			list[3]=rs.getString(4);
			list[4]=rs.getString(5);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * Gets compounds that participate in reactions.
	 * @param aux
	 * @param stmt
	 * @return reactants, products, reactionsReactants, productsReactants
	 * @throws SQLException
	 */
	public static ArrayList<Set<String>> getCompoundsReactions(String aux, Statement stmt) throws SQLException{

		ArrayList<Set<String>> result = new ArrayList<>();
		Set<String> reactants = new HashSet<String>();
		Set<String> products = new HashSet<String>();
		Set<String> reactionsReactants = new HashSet<String>();
		Set<String> productsReactants = new HashSet<String>();

		ResultSet rs = stmt.executeQuery("SELECT compound_idcompound,stoichiometric_coefficient,reaction_idreaction " +
				"FROM stoichiometry "+
				"INNER JOIN reaction ON (reaction.idreaction=reaction_idreaction) "+
				aux+";");

		while(rs.next()) {

			if(rs.getString(2).startsWith("-")){

				reactants.add(rs.getString(1));
				reactionsReactants.add(rs.getString(3));
			}
			else {

				products.add(rs.getString(1));productsReactants.add(rs.getString(3));
			}
		}

		result.add(reactants);
		result.add(products);
		result.add(reactionsReactants);
		result.add(productsReactants);

		rs.close();
		return result;
	}

	/**Count not transporters.
	 * @param aux
	 * @param aux2
	 * @param aux3
	 * @param id
	 * @param stmt
	 * @return ArrayList<String[]> 
	 * @throws SQLException
	 */
	public static ArrayList<String[]> countNotTransport(String aux, String aux2, String aux3, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = stmt.executeQuery("SELECT compound.idcompound, stoichiometry.compartment_idcompartment, " +
				" COUNT(DISTINCT(idreaction)) AS sum_not_transport "+
				" FROM compound " +
				" LEFT JOIN stoichiometry ON compound_idcompound=idcompound " +
				" INNER JOIN compartment ON stoichiometry.compartment_idcompartment=compartment.idcompartment " +
				" INNER JOIN reaction ON (reaction.idreaction=reaction_idreaction) "+
				aux + aux3 +" AND reaction.name NOT LIKE 'T%' "+
				" GROUP BY compound.name, compound.idcompound, kegg_id, stoichiometry.compartment_idcompartment "+
				" ORDER BY "+aux2+" compound.name, kegg_id ;");

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
	 * Count transporters.
	 * @param aux
	 * @param aux2
	 * @param aux3
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> countTransport(String aux, String aux2, String aux3, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT compound.idcompound, stoichiometry.compartment_idcompartment, " +
				" COUNT(DISTINCT(idreaction)) AS sum_transport "+
				" FROM compound " +
				" LEFT JOIN stoichiometry ON compound_idcompound=idcompound " +
				" INNER JOIN compartment ON stoichiometry.compartment_idcompartment=compartment.idcompartment " +
				" INNER JOIN reaction ON (reaction.idreaction=reaction_idreaction) "+
				aux+ aux3 +" AND reaction.name LIKE 'T%' "+
				" GROUP BY compound.name, compound.idcompound, kegg_id, stoichiometry.compartment_idcompartment "+
				" ORDER BY "+aux2+" compound.name, kegg_id ;");

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
	 * Get compounds reversibilities.
	 * @param aux
	 * @param aux2
	 * @param aux3
	 * @param stmt
	 * @return Set<Integer>
	 * @throws SQLException
	 */
	public static Set<Integer> getReversibilities(String aux, Statement stmt) throws SQLException{

		Set<Integer> reversibleCompounds = new HashSet<>();

		ResultSet reversibilities = stmt.executeQuery("SELECT DISTINCT(compound_idcompound) as c, reversible "
				+ " FROM stoichiometry "
				+ " INNER JOIN reaction ON (reaction.idreaction=reaction_idreaction) "
				+ " "+aux+" AND reversible ORDER BY c;");

		while(reversibilities.next())					
			reversibleCompounds.add(reversibilities.getInt("c"));

		reversibilities.close();
		return reversibleCompounds;
	}

	/**
	 * calculate which metabolites have both properties (product and reactant).
	 * @param aux
	 * @param aux2
	 * @param aux3
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getMetabolitesWithBothProperties(String aux, String aux2, String aux3, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();
		
		ResultSet rs = stmt.executeQuery("SELECT compound.name, formula, COUNT(DISTINCT SIGN(stoichiometric_coefficient)) as counter,"
				+ " compound.idcompound, kegg_id , COUNT(DISTINCT(idreaction)), compartment.name, stoichiometry.compartment_idcompartment " +
				" FROM compound " +
				" LEFT JOIN stoichiometry ON compound_idcompound=idcompound " +
				" INNER JOIN compartment ON stoichiometry.compartment_idcompartment=compartment.idcompartment " +
				" INNER JOIN reaction ON (reaction.idreaction=reaction_idreaction) "+
				aux+ aux3 +
				" GROUP BY compound.name, compound.idcompound, kegg_id, stoichiometry.compartment_idcompartment, compound.formula "+
				" ORDER BY "+aux2+" compound.name, kegg_id ;");

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
	 * Retrieves information about the intended reactions
	 * @param stmt
	 * @param name
	 * @return String [Notes, isSpontaneous, isNonEnzymatic, source]
	 * @throws SQLException
	 */
	public static String[] getReactionsInfo(Statement stmt, String name) throws SQLException {

		ResultSet rs = stmt.executeQuery("SELECT notes, isSpontaneous, isNonEnzymatic, source FROM reaction  WHERE reaction.name='"+name+"'");

		String[] result = new String[4];

		if(rs.next()) {

			result[0] = rs.getString(1);
			result[1] = rs.getBoolean(2)+"";
			result[2] = rs.getBoolean(3)+"";
			result[3] = rs.getString(4);
		}

		return result;

	}



	/**
	 * @param statement
	 * @param removed
	 * @param reactionsToKeep
	 * @param notes_map
	 * @throws SQLException
	 */
	public static void removeReactionsFromModel(PreparedStatement statement, Set<String> removed, Set<String> reactionsToKeep, Map<String, String> notes_map) throws SQLException {

		int i = 0;
		for (String name : removed) {

			if(!reactionsToKeep.contains(name)) {

				String note = "";

				if(notes_map.containsKey(name)) {

					note = notes_map.get(name)+ " | ";
				}

				note += "Removed by GPR";

				statement.setString(1, "false");
				statement.setString(2, note);
				statement.setString(3, name);

				statement.addBatch();

				if ((i + 1) % 1000 == 0) {

					statement.executeBatch(); // Execute every 1000 items.
				}
				i++;
			}
		}
		statement.executeBatch();
	}


	/**
	 * @param stmt
	 * @param kept
	 * @return Map<String, String> notes_map
	 * @throws SQLException
	 */
	public static Map<String, String> createNotesMap(Statement stmt, Set<String> kept) throws SQLException{

		Map<String, String> notes_map = new HashMap<>();

		for (String name : kept) {

			ResultSet rs = stmt.executeQuery("SELECT notes FROM reaction WHERE reaction.name='"+name+"'");

			if(rs.next() && rs.getString(1)!=null && !rs.getString(1).isEmpty())
				notes_map.put(name, rs.getString(1));
		}
		return notes_map;
	}

	/**
	 * Update Reaction table with reaction's annotation
	 * @param statement
	 * @param kept
	 * @param annotations
	 * @param notes_map
	 * @throws SQLException
	 */
	public static void updateReactionTable(PreparedStatement statement, Set<String> kept, Map<String, String> annotations, Map<String, String>notes_map) throws SQLException {

		int i = 0;
		for (String name : kept) {

			String note = annotations.get(name);

			String old_note = "automatic GPR";

			if(notes_map.containsKey(name)) {

				old_note = notes_map.get(name);

				if(!old_note.contains("automatic GPR"))
					old_note = old_note.trim().concat(" | automatic GPR");
			}

			statement.setString(1, note);
			statement.setString(2, old_note);
			statement.setString(3, name);
			statement.addBatch();

			if ((i + 1) % BATCH_SIZE == 0) {

				statement.executeBatch(); // Execute every 1000 items.
			}
			i++;
		}
		statement.executeBatch();
	}


	/**
	 * Update Reaction table with reaction's annotation
	 * @param statement
	 * @param keptWithDifferentAnnotation
	 * @param annotations
	 * @param notes_map
	 * @throws SQLException
	 */
	public static void updateReactionTableWithDifferentAnnotation (PreparedStatement statement, Set<String> keptWithDifferentAnnotation,
			Map<String, String> annotations, Map<String, String>notes_map) throws SQLException {

		int i = 0;
		for (String name : keptWithDifferentAnnotation) {

			//String note = "GENE_ASSOCIATION: " + this.annotations.get(name)+" | New Annotation. GPR set from tool.";
			String note = annotations.get(name);

			String old_note = "New Annotation. GPR set from tool";

			if(notes_map.containsKey(name)) {

				old_note = notes_map.get(name);

				if(!old_note.contains("New Annotation. GPR set from tool"))
					old_note = old_note.trim().concat(" | GPR set from tool");
			}

			statement.setString(1, note);
			statement.setString(2, old_note);
			statement.setString(3, name);
			statement.addBatch();

			if ((i + 1) % BATCH_SIZE == 0) {

				statement.executeBatch(); // Execute every 1000 items.
			}
			i++;
		}
		statement.executeBatch();
	}


	/**
	 * Load metabolites
	 * 
	 * @param metabolites
	 * @param metabolites_id
	 * @param concurrentLinkedQueue
	 * @param statement
	 * @param databaseType
	 * @throws SQLException
	 */
	public static void loadMetabolites(ConcurrentLinkedQueue<MetaboliteContainer> metabolites, ConcurrentHashMap<String,Integer> metabolites_id, 
			ConcurrentLinkedQueue<String> concurrentLinkedQueue, PreparedStatement statement, DatabaseType databaseType, boolean importedFromSBML) throws SQLException {

		//		"INSERT INTO compound(name, formula, molecular_weight, hasBiologicalRoles, entry_type, kegg_id) VALUES(?,?,?,?,?,?);"

		int i = 0;
		for (MetaboliteContainer metaboliteContainer : metabolites) {

			if(!metabolites_id.containsKey(metaboliteContainer.getEntryID())){

				String entry_type = null;
				
				if(importedFromSBML){
					entry_type="COMPOUND";
				}
				else{
					if(metaboliteContainer.getEntryID().startsWith("C"))
					{entry_type="COMPOUND";}
					if(metaboliteContainer.getEntryID().startsWith("G"))
					{entry_type="GLYCAN";}
					if(metaboliteContainer.getEntryID().startsWith("D"))
					{entry_type="DRUGS";}
					if(metaboliteContainer.getEntryID().startsWith("B"))
					{entry_type="BIOMASS";}
				}

				String name = null;
				String formula = null;
				String mw = null;
				boolean chbr = false;

				if(metaboliteContainer.getName()!=null)
					name = DatabaseUtilities.databaseStrConverter(metaboliteContainer.getName(), databaseType);

				if(metaboliteContainer.getFormula()!=null)
					formula = DatabaseUtilities.databaseStrConverter(metaboliteContainer.getFormula(), databaseType);

				if(metaboliteContainer.getMolecular_weight()!=null)
					mw = DatabaseUtilities.databaseStrConverter(metaboliteContainer.getMolecular_weight(), databaseType);

				if(concurrentLinkedQueue.contains(metaboliteContainer.getEntryID()))
					chbr = true;
				
				statement.setString(1, name);
				statement.setString(2, formula);
				statement.setString(3, mw);
				statement.setBoolean(4, chbr);
				statement.setString(5, entry_type);
				statement.setString(6, metaboliteContainer.getEntryID());
				statement.addBatch();

				if ((i + 1) % BATCH_SIZE == 0) {

					statement.executeBatch(); // Execute every 1000 items.
				}
				i++;
			}
		}
		statement.executeBatch();
	}

	/**
	 * @param metabolites
	 * @param pStatement
	 * @throws SQLException 
	 */
	public static void getCompoundID(ConcurrentLinkedQueue<MetaboliteContainer> metabolites, PreparedStatement pStatement) throws SQLException {

		for (MetaboliteContainer metaboliteContainer : metabolites) {

			pStatement.setString(1, metaboliteContainer.getEntryID());
			ResultSet rs = pStatement.executeQuery();

			if(rs.next())
				metaboliteContainer.setMetaboliteID(rs.getInt(1));
		}
	}


	//	/**
	//	 * @param metabolites
	//	 * @param statement
	//	 * @throws SQLException
	//	 */
	//	public static void  loadSameAs(ConcurrentLinkedQueue<MetaboliteContainer> metabolites, PreparedStatement statement) throws SQLException {
	//
	//		// "INSERT INTO same_as (metabolite_id, similar_metabolite_id) VALUES(?,?);"
	//
	//		int i = 0;
	//		for (MetaboliteContainer metaboliteContainer : metabolites) {
	//
	//			for(String same : metaboliteContainer.getSame_as()) {
	//
	//				statement.setInt(1, metaboliteContainer.getMetaboliteID());
	//				statement.setString(2, same);
	//				statement.addBatch();
	//
	//				if ((i + 1) % BATCH_SIZE == 0) {
	//
	//					statement.executeBatch(); // Execute every 1000 items.
	//				}
	//				i++;
	//			}
	//		}
	//		statement.executeBatch();
	//	}



	/**
	 * Load BATCH_SIZE aliases
	 * 
	 * @param metabolites
	 * @param statement
	 * @param databaseType
	 * @throws SQLException
	 */
	public static void  loadALiases(ConcurrentLinkedQueue<MetaboliteContainer> metabolites, PreparedStatement statement, DatabaseType databaseType) throws SQLException {

		//"INSERT INTO aliases(class,entity,alias) "+ "VALUES('c', ?, ?)"

		int i = 0;
		for (MetaboliteContainer metaboliteContainer : metabolites) {

			if(metaboliteContainer.getNames()!=null)	{
				for(String synonym:metaboliteContainer.getNames()) {

					String aux = DatabaseUtilities.databaseStrConverter(synonym,databaseType);

					statement.setInt(1, metaboliteContainer.getMetaboliteID());
					statement.setString(2, aux);
					statement.addBatch();

					if ((i + 1) % BATCH_SIZE == 0) {

						statement.executeBatch(); // Execute every 1000 items.
					}
					i++;
				}
			}
		}
		statement.executeBatch();
	}

	/**
	 * 
	 * 
	 * @param metabolites
	 * @param statement
	 * @throws SQLException
	 */
	public static void  load_dbLinks(ConcurrentLinkedQueue<MetaboliteContainer> metabolites, PreparedStatement statement) throws SQLException {

		//"INSERT INTO dblinks(class,internal_id,external_database,external_id) VALUES('c',?,?,?)"

		int i = 0;
		for (MetaboliteContainer metaboliteContainer : metabolites) {

			if(metaboliteContainer.getDblinks()!=null) {
				for(String dbLink : metaboliteContainer.getDblinks()) {

					String database = dbLink.split(":")[0], link = dbLink.split(":")[1];

					statement.setInt(1, metaboliteContainer.getMetaboliteID());
					statement.setString(2, database);
					statement.setString(3, link);
					statement.addBatch();

					if ((i + 1) % BATCH_SIZE == 0) {

						statement.executeBatch(); // Execute every 1000 items.
					}
					i++;
				}
			}
			statement.executeBatch();
		}
	}

	/**
	 * Calculate metabolites that have one property.
	 * @param aux
	 * @param aux2
	 * @param aux3
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getMetabolitesProperties(String aux, String aux2, String aux3, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT compound.name, formula, SIGN(stoichiometric_coefficient) as sign, compound.idcompound, kegg_id , COUNT(DISTINCT(idreaction)), " +
				" compartment.name, stoichiometry.compartment_idcompartment " +
				" FROM compound " +
				" LEFT JOIN stoichiometry ON compound_idcompound=idcompound " +
				" INNER JOIN compartment ON stoichiometry.compartment_idcompartment=compartment.idcompartment " +
				" INNER JOIN reaction ON (reaction.idreaction=reaction_idreaction) "+ aux + aux3 + 
				" GROUP BY compound.name, kegg_id, stoichiometry.compartment_idcompartment, SIGN(stoichiometric_coefficient) " +
				" ORDER BY "+aux2+" compound.name, kegg_id ;");

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
	 * Get metabolites not in model selected by entry_type.
	 * @param aux
	 * @param aux2
	 * @param aux3
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getMetabolitesNotInModel(String aux3, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT * FROM compound WHERE idcompound NOT IN (SELECT compound_idcompound FROM stoichiometry)" + aux3 + ";");

		while(rs.next()){
			String[] list = new String[4];

			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(4);
			list[3]=rs.getString(6);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * Get reactions.
	 * @param aux
	 * @param rec
	 * @param compartment
	 * @param stmt
	 * @return ArrayList<String>
	 * @throws SQLException
	 */
	public static List<ArrayList<String>> getReactions(String aux, String rec, String compartment, Statement stmt) throws SQLException{

		List<ArrayList<String>> ret = new ArrayList<>();


		ResultSet rs = stmt.executeQuery("SELECT distinct(idreaction), reaction.name, equation, source, inModel, reversible FROM reaction " +
				"INNER JOIN stoichiometry ON reaction.idreaction = stoichiometry.reaction_idreaction " +
				"INNER JOIN  compartment ON compartment.idcompartment =  stoichiometry.compartment_idcompartment " +
				aux+" AND compartment.name='"+compartment+"' " +
				" AND stoichiometry.compound_idcompound = '"+rec+"' "+
				" ORDER BY inModel DESC, source, reversible DESC, name ASC");

		while(rs.next()) {

			ArrayList<String> ql = new ArrayList<>();
			ql.add(rs.getString(2));
			ql.add(rs.getString(3));
			ql.add(rs.getString(4));

			if(rs.getBoolean(5))
				ql.add(rs.getBoolean(5)+"");
			else
				ql.add("-");

			if(rs.getBoolean(6))
				ql.add(rs.getBoolean(6)+"");
			else
				ql.add("-");

			ret.add(ql);
		}

		rs.close();
		return ret;
	}

	/**
	 * Count distinct reactions in model.
	 * @param stmt
	 * @return int
	 * @throws SQLException
	 */
	public static int countReactionsInModel(String aux, Statement statement) throws SQLException{

		int res = 0;

		ResultSet rs = statement.executeQuery("SELECT COUNT(DISTINCT(idreaction)) FROM reaction "+aux+" AND  inModel;");

		while(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 * Count distinct KEGG reactions.
	 * @param stmt
	 * @return int
	 * @throws SQLException
	 */
	public static int countReactionsKEGG(String aux, Statement statement) throws SQLException{

		int res = 0;

		ResultSet rs = statement.executeQuery("SELECT COUNT(DISTINCT(idreaction)) FROM reaction "+aux+" AND source='KEGG';");

		while(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 * Count distinct KEGG reactions in model.
	 * @param aux
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int countReactionsInModelKEGG(String aux, Statement statement) throws SQLException{

		int res = 0;

		ResultSet rs = statement.executeQuery("SELECT COUNT(DISTINCT(idreaction)) FROM reaction "+aux+" AND inModel AND source ='KEGG'");

		while(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 * Count distinct reactions in model inserted by homology.
	 * @param aux
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int countReactionsInModelHomology(String aux, Statement statement) throws SQLException{

		int res = 0;

		ResultSet rs = statement.executeQuery("SELECT COUNT(DISTINCT(idreaction)) FROM reaction "+aux+" AND inModel AND source ='HOMOLOGY'");

		while(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 * Count distinct reactions in model inserted by Transporters annotation tool.
	 * @param aux
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int countReactionsInModelTransporters(String aux, Statement statement) throws SQLException{

		int res = 0;

		ResultSet rs = statement.executeQuery("SELECT COUNT(DISTINCT(idreaction)) FROM reaction "+aux+" AND inModel AND source='TRANSPORTERS'");

		while(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 * Count distinct reactions inserted by Transporters annotation tool.
	 * @param aux
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int countReactionsTransporters(String aux, Statement statement) throws SQLException{

		int res = 0;

		ResultSet rs = statement.executeQuery("SELECT COUNT(DISTINCT(idreaction)) FROM reaction "+aux+" AND source='TRANSPORTERS'");

		while(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 * Count distinct reactions in model inserted manually.
	 * @param aux
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int countReactionsInModelManual(String aux, Statement statement) throws SQLException{

		int res = 0;

		ResultSet rs = statement.executeQuery("SELECT COUNT(DISTINCT(idreaction)) FROM reaction "+aux+" AND inModel AND source='MANUAL'");

		while(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 * Count pathways with associated reactions.
	 * @param aux
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int countPathwayHasReaction(String aux, Statement statement) throws SQLException{

		int res = 0;

		ResultSet rs = statement.executeQuery("SELECT count(distinct(idreaction)) FROM reaction JOIN pathway_has_reaction ON idreaction = reaction_idreaction "+aux);

		while(rs.next())
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 * Get locusTag and name from gene table.
	 * @param statement
	 * @return ArrayList<String> 
	 * @throws SQLException
	 */
	public static List<String> getGenesModel(Statement statement) throws SQLException{
		
		List<String> lls = new ArrayList<>();

		ResultSet rs = statement.executeQuery("SELECT locusTag, name, idgene FROM gene ORDER BY locusTag;");

		while(rs.next()) {

			String gene = rs.getString(1);

			if(rs.getString(2) != null && !rs.getString(2).trim().isEmpty())
				gene = gene.concat(" (").concat(rs.getString(2)).concat(")");

			lls.add(gene);
		}

		rs.close();
		return lls;
	}
	
	/**
	 * Get locusTag and name from gene table.
	 * @param statement
	 * @return ArrayList<String> 
	 * @throws SQLException
	 */
	public static Map<String, String> getGenesModelID(Statement statement) throws SQLException{
		
		Map<String, String> ret = new HashMap<>();
		ResultSet rs = statement.executeQuery("SELECT locusTag, name, idgene FROM gene;");

		while(rs.next()) {

			String gene = rs.getString(1);

			if(rs.getString(2) != null && !rs.getString(2).trim().isEmpty())
				gene = gene.concat(" (").concat(rs.getString(2)).concat(")");

			ret.put(gene, rs.getString(3));
		}

		rs.close();
		return ret;
	}

	/**
	 * Get data from enzyme table.
	 * @param statement
	 * @return ArrayList<String> 
	 * @throws SQLException
	 */
	public static ArrayList<String> getEnzymesModel(Statement statement) throws SQLException{

		ArrayList<String> lls = new ArrayList<String>();

		ResultSet rs = statement.executeQuery("SELECT ecnumber, protein_idprotein, protein.name FROM enzyme " +
				" INNER JOIN protein ON protein.idprotein = protein_idprotein " +
				"ORDER BY ecnumber");

		while(rs.next()) 
			lls.add(rs.getString(1)+"___"+rs.getString(3)+"___"+rs.getString(2));

		rs.close();
		return lls;
	}

	/**
	 * Get data from reaction_has_enzyme table.
	 * @param rowID
	 * @param statement
	 * @return Set<String>
	 * @throws SQLException
	 */
	public static Set<String> getEnzymesForReaction(int rowID, Statement statement) throws SQLException{

		Set<String> res = new TreeSet<String>();

		ResultSet rs = statement.executeQuery(
				"SELECT enzyme_ecnumber, enzyme_protein_idprotein, protein.name " +
						" FROM reaction_has_enzyme " +
						" INNER JOIN protein ON protein.idprotein = enzyme_protein_idprotein " +
						" WHERE reaction_idreaction = "+rowID);

		while(rs.next()) 
			res.add(rs.getString(1)+"___"+rs.getString(3)+"___"+rs.getString(2));

		rs.close();
		return res;
	}

	/**
	 * Get pathways by rowID.
	 * @param rowID
	 * @param statement
	 * @return String[]
	 * @throws SQLException
	 */
	public static String[] getPathwaysByRowID(int rowID, Statement statement) throws SQLException{

		String[] res = new String[0];

		ResultSet rs = statement.executeQuery(
				"SELECT pathway.name FROM pathway " +
						" INNER JOIN pathway_has_reaction ON (pathway_has_reaction.pathway_idpathway=pathway.idpathway ) " +
						" WHERE reaction_idreaction = "+rowID);

		boolean exists = rs.last();

		if(exists) {

			res = new String[rs.getRow()];
			rs.beforeFirst();

			int col=0;
			while(rs.next()) {

				res[col] = rs.getString(1);
				col++;
			}
		}
		rs.close();
		return res;
	}

	/**
	 * Get pathwayID from pathway table.
	 * @param aux
	 * @param statement
	 * @return List<String>
	 * @throws SQLException
	 */
	public static List<String> getPathways2(String aux, Statement statement) throws SQLException{

		List<String> lls = new ArrayList<String>();

		ResultSet rs = statement.executeQuery("SELECT DISTINCT(idpathway), pathway.name FROM pathway " +
				aux+ " ORDER BY name;");

		while(rs.next())
			lls.add(rs.getString(2));

		rs.close();
		return lls;
	}

	/**
	 * Get pathwayID from pathway table.
	 * @param query
	 * @param statement
	 * @return int
	 * @throws SQLException
	 */
	public static int getPathwayID(String query, Statement statement) throws SQLException{

		int res=-1;

		ResultSet rs = statement.executeQuery(query);

		while(rs.next()) 
			res = rs.getInt(1);

		rs.close();
		return res;
	}	

	/**
	 * Get pathway code.
	 * @param query
	 * @param statement
	 * @return String
	 * @throws SQLException
	 */
	public static String getPathwayCode(String query, Statement statement) throws SQLException{

		String res="";

		ResultSet rs = statement.executeQuery(query);

		while(rs.next()) 
			res=rs.getString(1);

		rs.close();
		return res;
	}	

	/**
	 * Get boolean_rule from reaction for a given reactionID.
	 * 
	 * @param id
	 * @param statement
	 * @return String
	 * @throws SQLException
	 */
	public static List<List<Pair<String, String>>> getBooleanRuleFromReaction(int idReaction, Statement statement) throws SQLException{

		List<List<Pair<String, String>>> res = null;
		String rawData = null;

		ResultSet rs = statement.executeQuery("SELECT boolean_rule FROM reaction WHERE idreaction = '" + idReaction+"';");

		if(rs.next()) {

			res = new ArrayList<>();
			rawData = rs.getString(1);
		}

		rs.close();
		
		res = ModelAPI.parseBooleanRule(rawData, statement);
		
		return res;
	}
	

	/**
	 * Parse boolean_rule from reaction for a given reactionID.
	 * 
	 * @param id
	 * @param statement
	 * @return String
	 * @throws SQLException
	 */
	public static List<List<Pair<String, String>>> parseBooleanRule(String rawData, Statement statement) throws SQLException{

		List<List<Pair<String, String>>> res = new ArrayList<>();;
		ResultSet rs = null;

		if(rawData != null) {

			String [] rules = rawData.split(" OR ");

			for(String rule : rules) {

				String [] ids = rule.split(" AND ");

				List<Pair<String, String>> pairList= new ArrayList<>();

				for(String idString : ids) {

					if(!idString.isEmpty()) {

						int geneId = Integer.parseInt(idString.trim());

						rs = statement.executeQuery("SELECT locusTag, name FROM gene WHERE idgene = " + geneId);

						while(rs.next()) {

							Pair<String, String> pair = new Pair<String, String> (rs.getString(1), rs.getString(2));
							pairList.add(pair);
						}
						
						rs.close();
					}
				}
				res.add(pairList);
			}
		}
		return res;
	}
	
	/**
	 * Get boolean_rule from reaction for a given reactionID.
	 * 
	 * @param id
	 * @param statement
	 * @return String
	 * @throws SQLException
	 */
	public static List<List<Pair<String, String>>> getOldBooleanRuleFromReaction(int id, Statement statement) throws SQLException{

		List<List<Pair<String, String>>> res = null;
		String rawData = null;

		ResultSet rs = statement.executeQuery("SELECT boolean_rule FROM reaction WHERE idreaction = " + id);

		if(rs.next()) {

			res = new ArrayList<>();
			rawData = rs.getString(1);
		}

		if(rawData != null) {

			String [] rules = rawData.split(" OR ");

			for(String rule : rules) {

				String [] ids = rule.split(" AND ");

				List<Pair<String, String>> pairList= new ArrayList<>();

				for(String idString : ids) {

					String locusTag = idString;
					if(idString.contains("_"))
						locusTag = idString.split("_")[1];
						
					rs = statement.executeQuery("SELECT locusTag, name FROM gene WHERE locusTag = '" +locusTag+"'");

					while(rs.next()) {

						Pair<String, String> pair = new Pair<String, String> (rs.getString(1), rs.getString(2));
						pairList.add(pair);
					}
				}
				res.add(pairList);
			}
		}
		
		rs.close();
		return res;
	}



	/**
	 * Get data from compound table for a given reactionID.
	 * @param id
	 * @param statement
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]>  getCompoundData(int id, Statement statement) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = statement.executeQuery("SELECT compound.name, compound.formula, compartment.name, stoichiometric_coefficient, numberofchains, kegg_id FROM stoichiometry " +
				"INNER JOIN compound ON compound_idcompound = compound.idcompound " +
				"INNER JOIN compartment ON compartment_idcompartment = idcompartment " +
				"WHERE reaction_idreaction = " + id);

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
	 * Get data from reaction_has_enzyme table for a given reactionID.
	 * @param id
	 * @param statement
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]>  getReactionHasEnzymeData(int id, Statement statement) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = statement.executeQuery("SELECT enzyme_ecnumber, name, inModel, enzyme_protein_idprotein " +
				" FROM reaction_has_enzyme " +
				" INNER JOIN enzyme ON reaction_has_enzyme.enzyme_protein_idprotein = enzyme.protein_idprotein "
				+ "AND reaction_has_enzyme.enzyme_ecnumber = enzyme.ecnumber " +
				" INNER JOIN protein ON reaction_has_enzyme.enzyme_protein_idprotein = protein.idprotein "
				+ "AND reaction_has_enzyme.enzyme_ecnumber = enzyme.ecnumber " +
				" WHERE reaction_idreaction = "+id);

		while(rs.next()){
			String[] list = new String[3];

			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getBoolean(3) +"";

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 *  Get data from reaction table for a given reactionID.
	 * @param id
	 * @param statement
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]>  getReactionData(int id, Statement statement) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = statement.executeQuery("SELECT isGeneric, isSpontaneous, isNonEnzymatic, lowerBound, upperBound, reversible, name, boolean_rule "
				+ "FROM reaction WHERE idreaction = " + id);

		while(rs.next()){
			String[] list = new String[7];

			list[0]=rs.getBoolean(1) +"";
			list[1]=rs.getBoolean(2) +"";
			list[2]=rs.getBoolean(3) +"";
			list[3]=rs.getString(4);
			list[4]=rs.getString(5);
			list[5]=rs.getBoolean(6) +"";
			list[6]=rs.getString(7);

			result.add(list);
		}
		rs.close();
		return result;
	}

	/**
	 * Get data from reaction table.
	 * @param reactionID
	 * @param statement
	 * @return String[]
	 * @throws SQLException
	 */
	public static String[] getReactionData2(int reactionID, Statement statement) throws SQLException{

		String[] list = new String[12];

		ResultSet rs = statement.executeQuery(
				"SELECT reaction.name, equation, reversible, "
				//+ " pathway.name, enzyme_ecnumber,"
				+ " inModel, compartment.name, isSpontaneous, isNonEnzymatic, isGeneric, lowerBound, upperBound, source, boolean_rule " +
				"FROM reaction " +
				//"INNER JOIN pathway_has_reaction ON idreaction = pathway_has_reaction.reaction_idreaction " +
				//"INNER JOIN pathway ON pathway_idpathway = pathway.idpathway " +
				"INNER JOIN compartment ON idcompartment = compartment_idcompartment " +
				//"INNER JOIN reaction_has_enzyme ON idreaction = reaction_has_enzyme.reaction_idreaction " +
				"WHERE idreaction="+reactionID);

		if(rs.next()){
			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getBoolean(3)+"";
			list[3]=rs.getBoolean(4)+"";
			list[4]=rs.getString(5);
			list[5]=rs.getBoolean(6)+"";
			list[6]=rs.getBoolean(7)+"";
			list[7]=rs.getBoolean(8)+"";
			list[8]=rs.getString(9);
			list[9]=rs.getString(10);
			list[10]=rs.getString(11);
			list[11]=rs.getString(12);
		}
		rs.close();
		return list;
	}

	/**
	 * Get data from stoichiometry data.
	 * @param reactionID
	 * @param statement
	 * @return String[]
	 * @throws SQLException
	 */
	public static String[] getStoichiometryData(int reactionID, Statement statement) throws SQLException{

		String[] list = new String[4];

		ResultSet rs = statement.executeQuery("SELECT compartment.name, stoichiometric_coefficient, numberofchains, compound_idcompound " +
				"FROM stoichiometry " +
				"INNER JOIN compartment ON idcompartment = compartment_idcompartment " +
				"WHERE reaction_idreaction = '" + reactionID+"'");

		if(rs.next()){
			list[0]=rs.getString(1);
			list[1]=rs.getString(2);
			list[2]=rs.getString(3);
			list[3]=rs.getString(4);

		}
		rs.close();
		return list;
	}

	/**
	 * Get reactionID for a given pathway, ecNumber and idProtein.
	 * @param pathway
	 * @param ecNumber
	 * @param idProtein
	 * @param statement
	 * @return Set<String>
	 * @throws SQLException
	 */
	public static Set<Integer> getReactionsID(int pathway, String ecNumber, int idProtein, Statement statement) throws SQLException{

		Set<Integer> reactionsID = new TreeSet<>();

		ResultSet rs = statement.executeQuery("SELECT reaction_has_enzyme.reaction_idreaction FROM pathway_has_enzyme " +
				"INNER JOIN reaction_has_enzyme ON pathway_has_enzyme.enzyme_protein_idprotein = reaction_has_enzyme.enzyme_protein_idprotein "
				+ "AND pathway_has_enzyme.enzyme_ecnumber = reaction_has_enzyme.enzyme_ecnumber " +
				"INNER JOIN pathway_has_reaction ON pathway_has_enzyme.pathway_idpathway = pathway_has_reaction.pathway_idpathway " +
				"WHERE reaction_has_enzyme.reaction_idreaction = pathway_has_reaction.reaction_idreaction " +
				"AND pathway_has_enzyme.enzyme_ecnumber = '"+ecNumber+"' AND pathway_has_enzyme.enzyme_protein_idprotein = '"+idProtein
				+"' AND pathway_has_enzyme.pathway_idpathway = "+pathway);

		while(rs.next()) 
			reactionsID.add(rs.getInt(1));

		rs.close();
		return reactionsID;
	}

	/**
	 * Get PathwayID for a given rowID, ecNumber and idProtein.
	 * @param pathway
	 * @param ecNumber
	 * @param idProtein
	 * @param statement
	 * @return Set<String>
	 * @throws SQLException
	 */
	public static Set<Integer> getPathwayID2(int idReaction, String ecNumber, int idProtein, Statement statement) throws SQLException{

		Set<Integer> pathwayID = new TreeSet<>();

		ResultSet rs = statement.executeQuery("SELECT pathway_has_enzyme.pathway_idpathway FROM pathway_has_enzyme " +
				"INNER JOIN reaction_has_enzyme ON pathway_has_enzyme.enzyme_protein_idprotein = reaction_has_enzyme.enzyme_protein_idprotein "
				+ "AND pathway_has_enzyme.enzyme_ecnumber = reaction_has_enzyme.enzyme_ecnumber " +
				"INNER JOIN pathway_has_reaction ON pathway_has_enzyme.pathway_idpathway = pathway_has_reaction.pathway_idpathway " +
				"WHERE reaction_has_enzyme.reaction_idreaction = pathway_has_reaction.reaction_idreaction " +
				"AND pathway_has_enzyme.enzyme_ecnumber = '"+ecNumber+"'  AND pathway_has_enzyme.enzyme_protein_idprotein = '"+idProtein+"' "
				+ "AND reaction_has_enzyme.reaction_idreaction = "+idReaction);

		while(rs.next()) 
			pathwayID.add(rs.getInt(1));

		rs.close();
		return pathwayID;
	}

	/**
	 * Get existing metabolitesID by rowID.
	 * @param idReaction
	 * @param statement
	 * @return Map<String,Pair<String,Pair<String,String>>>
	 * @throws SQLException
	 */
	public static Map<String,Pair<String,Pair<String,String>>> getExistingMetabolitesID(int idReaction, Statement statement) throws SQLException{

		Map<String,Pair<String,Pair<String,String>>> existingMetabolitesID = new HashMap<>();

		ResultSet rs = statement.executeQuery("SELECT compound_idcompound, idstoichiometry, stoichiometric_coefficient, compartment_idcompartment "
				+ "FROM stoichiometry WHERE reaction_idreaction = "+idReaction);


		while(rs.next()) {

			if(rs.getString(3).startsWith("-"))
				existingMetabolitesID.put("-"+rs.getString(1), new Pair<>(rs.getString(2), new Pair<>(rs.getString(3), rs.getString(4))));
			else
				existingMetabolitesID.put(rs.getString(1), new Pair<>(rs.getString(2), new Pair<>(rs.getString(3), rs.getString(4))));
		}

		rs.close();
		return existingMetabolitesID;
	}

	/**
	 * Get stoichiometryID from stoichiometry table.
	 * @param idReaction
	 * @param m
	 * @param idCompartment
	 * @param metabolite
	 * @param statement
	 * @return String
	 * @throws SQLException
	 */
	public static int getStoichiometryID(int idReaction, String m, int idCompartment, String metabolite, Statement statement) throws SQLException{

		int idstoichiometry = -1;

		ResultSet rs = statement.executeQuery("SELECT idstoichiometry FROM stoichiometry "
				+ " WHERE reaction_idreaction = "+idReaction+" "
				+ " AND compound_idcompound = " + m.replace("-", "")
				+ " AND compartment_idcompartment = " + idCompartment
				+ " AND stoichiometric_coefficient = '" + metabolite + "'");

		if(rs.next())
			idstoichiometry = rs.getInt(1);

		rs.close();
		return idstoichiometry;
	}

	/**
	 * Check if a reaction is reversible and in model.
	 * @param reactionID
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static Pair<Boolean, Boolean> checkReactionIsReversibleAndInModel(String reactionID, Statement statement) throws SQLException{

		Pair<Boolean, Boolean> res = new Pair<>(false, false);

		ResultSet rs = statement.executeQuery("SELECT reversible, isGeneric, inModel FROM reaction WHERE idreaction='"+reactionID+"';");

		if(rs.next()){
			res.setA(rs.getBoolean(1));
			res.setB(rs.getBoolean(3));
		}

		rs.close();
		return res;
	}

	/**
	 * Get equation and source from reaction table for a given reactionID.
	 * @param reactionID
	 * @param statement
	 * @return Pair<String, String>
	 * @throws SQLException
	 */
	public static Pair<String, String> getEquationAndSourceFromReaction(String reactionID, Statement statement) throws SQLException{

		Pair<String, String> res = new Pair<>("", "");

		ResultSet rs = statement.executeQuery("SELECT equation, source FROM reaction WHERE idreaction='"+reactionID+"';");

		if(rs.next()){
			res.setA(rs.getString(1));
			res.setB(rs.getString(2));
		}

		rs.close();
		return res;
	}

	/**
	 * Get all genes.
	 * @param id
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getAllGenes(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT idgene, locusTag, name, count(DISTINCT(module_has_orthology.module_id)), count(DISTINCT(enzyme_ecnumber)) "+
				" FROM gene LEFT JOIN subunit ON gene.idgene = gene_idgene "+
				" LEFT JOIN enzyme ON subunit.enzyme_protein_idprotein = enzyme.protein_idprotein "+
				" LEFT JOIN gene_has_orthology ON gene_has_orthology.gene_idgene = gene.idgene "+
				" LEFT JOIN module_has_orthology ON module_has_orthology.orthology_id = gene_has_orthology.orthology_id "+
				" GROUP BY idgene, locusTag;");

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
	 * Get all genes.
	 * @param id
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getRegulatoryGenes(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT name, locusTag, idgene FROM subunit JOIN gene " +
				"ON gene_idgene=idgene WHERE protein_idprotein IN (SELECT protein_idprotein " +
				"FROM regulatory_event) order by name");

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
	 * Get encoding genes.
	 * @param stmt
	 * @return ArrayList<String[]> 
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getEncodingGenes(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT idgene, locusTag, name, count(DISTINCT(module_has_orthology.module_id)), count(enzyme_ecnumber) "+
				" FROM gene LEFT JOIN subunit ON gene.idgene = gene_idgene "+
				" INNER JOIN enzyme ON subunit.enzyme_protein_idprotein = enzyme.protein_idprotein "+
				" LEFT JOIN gene_has_orthology ON gene_has_orthology.gene_idgene = gene.idgene "+
				" LEFT JOIN module_has_orthology ON module_has_orthology.orthology_id = gene_has_orthology.orthology_id "+
				"GROUP BY locusTag;");

		//		System.out.println("SELECT idgene, locusTag, name, count(DISTINCT(reaction)), count(enzyme_ecnumber) "+
		//				" FROM gene LEFT JOIN subunit ON gene.idgene = gene_idgene "+
		//				" INNER JOIN enzyme ON subunit.enzyme_protein_idprotein = enzyme.protein_idprotein "+
		//				" INNER JOIN gene_has_orthology ON gene_has_orthology.gene_idgene = gene.idgene "+
		//				" INNER JOIN module_has_orthology ON module_has_orthology.orthology_id = gene_has_orthology.orthology_id "+
		//				" INNER JOIN module ON module_has_orthology.module_id = module.id " +
		//				" GROUP BY locusTag;");

		while(rs.next()) {

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
	 * Get regulated genes.
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getRegulatedGenes(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT DISTINCT gene.name, bnumber, gene.idgene " +
				"FROM regulatory_event as event, transcription_unit, transcription_unit_gene " +
				"AS tug, transcription_unit_promoter as tup, promoter,gene WHERE " +
				"event.promoter_idpromoter=idpromoter AND tup.promoter_idpromoter=idpromoter AND " +
				"tup.transcription_unit_idtranscription_unit=idtranscription_unit AND " +
				"tug.transcription_unit_idtranscription_unit=idtranscription_unit AND gene_idgene=idgene " +
				"order by gene.name");

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
	 * Get proteins that are in Model.
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getProteinsInModel(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT DISTINCT(idprotein), name, class FROM protein " +
				"JOIN enzyme ON(enzyme.protein_idprotein=protein.idprotein) where inModel=1 ORDER BY name");

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
	 * Get enzymes.
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getEnzymes(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT distinct(idprotein), name, inchi " +
				"FROM protein JOIN enzyme ON enzyme_protein_idprotein=idprotein ORDER BY name");

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
	 * Get data from protein table.
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getTFs(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT distinct(idprotein), name, inchi " +
				"FROM protein " +
				"JOIN regulatory_event ON enzyme_protein_idprotein=idprotein ORDER BY name");

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
	 * Get data from protein table.
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getSigmas(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT distinct(idprotein), name, iupac_name, inchi, " +
				"cas_registry_name FROM protein " +
				"JOIN sigma_promoter ON protein_protein_idprotein=idprotein ORDER BY name");

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
	 * Get gene information (name, locusTag).
	 * @param id
	 * @param stmt
	 * @return ArrayList<String[]> 
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getGeneInfo(String id, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT name, locusTag FROM gene JOIN subunit ON " +
				"gene.idgene = subunit.gene_idgene WHERE enzyme_protein_idprotein = " + id);

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
	 * Get all from protein.
	 * @param id
	 * @param stmt
	 * @return ArrayList<String[]> 
	 * @throws SQLException
	 */
	public static Set<String> getAllFromProtein(String aux, Statement stmt) throws SQLException{

		Set<String> proteins = new HashSet<String>();

		ResultSet rs = stmt.executeQuery("SELECT * FROM protein"+aux);

		while(rs.next())
			proteins.add(rs.getString(1));

		rs.close();
		return proteins;
	}

	/**
	 * Get enzyme ECnumber and check if is in Model for a given proteinID.
	 * @param id
	 * @param stmt
	 * @return ArrayList<String[]> 
	 * @throws SQLException
	 */
	public static Pair<List<String>, Boolean[]> getECnumber(int proteinID, Statement stmt) throws SQLException{

		List<String> enzymesIDs = new ArrayList<String>();
		Pair<List<String>, Boolean[]> res = new Pair<List<String>, Boolean[]>(enzymesIDs, null);

		ResultSet rs = stmt.executeQuery( "SELECT ecnumber, inModel FROM enzyme WHERE protein_idprotein ="+proteinID);

		int i = 0;
		rs.last();
		Boolean[] inModel = new Boolean[rs.getRow()];
		rs.beforeFirst();
		while(rs.next()) {

			enzymesIDs.add(i,rs.getString(1));
			inModel[i] = rs.getBoolean(2);
		}

		res.setA(enzymesIDs);
		res.setB(inModel);

		rs.close();
		return res;
	}

	/**
	 * Get protein data.
	 * @param id
	 * @param stmt
	 * @return String[][]
	 * @throws SQLException
	 */
	public static String[][] getProteinData(int id, Statement stmt) throws SQLException{

		String[][] res = null;

		ResultSet rs = stmt.executeQuery("SELECT * FROM protein WHERE idprotein ="+id);

		ResultSetMetaData rsmd = rs.getMetaData();
		rs.last();
		res = new String[rs.getRow()][rsmd.getColumnCount()];
		rs.first();
		int row=0;

		while(row<res.length) {

			int col=1;
			while(col<rsmd.getColumnCount()+1) {

				res[row][col-1] = rs.getString(col);
				col++;
			}
			rs.next();
			row++;
		}

		rs.close();
		return res;
	}

	/**
	 * Get enzyme ECnumber and check if is in Model for a given proteinID.
	 * @param proteinID
	 * @param stmt
	 * @return String[]
	 * @throws SQLException
	 */
	public static String[] getECnumber2(int proteinID, Statement stmt) throws SQLException{

		String[] list = new String[2];

		String temp = "", tempBoolean="";

		ResultSet rs = stmt.executeQuery( "SELECT ecnumber, inModel FROM enzyme WHERE protein_idprotein ="+proteinID);

		while(rs.next()) {

			temp+=(rs.getString(1)+";");

			if(rs.getString(2).equals("1"))
				tempBoolean+=("true;");
			else
				tempBoolean+=("false;");
		}

		list[0] = temp;
		list[1] = tempBoolean;

		rs.close();
		return list;
	}

	/**
	 * Get synonyms.
	 * @param integer
	 * @param stmt
	 * @return String[][]
	 * @throws SQLException
	 */
	public static String[][] getSynonyms(Integer integer, Statement stmt) throws SQLException{

		String[][] res = null; 

		ResultSet rs = stmt.executeQuery("SELECT alias FROM aliases where class = 'p' AND entity ="+integer);

		ResultSetMetaData rsmd = rs.getMetaData();
		rs.last();
		res = new String[rs.getRow()][rsmd.getColumnCount()];
		rs.first();

		int row=0;
		while(row<res.length)
		{
			int col=1;
			while(col<rsmd.getColumnCount()+1)
			{

				res[row][col-1] = rs.getString(col);
				col++;
			}
			rs.next();
			row++;
		}

		rs.close();
		return res;
	}

	/**
	 * Get proteinID for a given name and class.
	 * @param classString
	 * @param name
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static int getProteinID(String classString, String name, Statement stmt) throws SQLException{

		ResultSet rs = stmt.executeQuery("SELECT * FROM protein WHERE name='" + name + "' AND class = '"+classString+"'");
		int res = -1;
		if(rs.next()) 
			res = rs.getInt(1);

		rs.close();
		return res;
	}

	/**
	 * Check if an entry for a given name and entity exists in aliases table.
	 * @param idNewProtein
	 * @param name
	 * @param stmt
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkAliasExistence(int idNewProtein, String name, Statement stmt) throws SQLException{

		boolean exists = false;
		ResultSet rs = stmt.executeQuery("SELECT * FROM aliases WHERE class='p' AND  alias='" + name +"' AND  entity=" + idNewProtein);

		if(rs.next()) 
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 * Check if an entry for a given proteinID and ECnumber exists in enzyme table.
	 * @param idNewProtein
	 * @param id
	 * @param stmt
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkEnzymeInModelExistence(int idNewProtein, String id, Statement stmt) throws SQLException{

		boolean exists = false;

		ResultSet rs = stmt.executeQuery("SELECT * FROM enzyme " +
				"WHERE inModel=true AND  source='MANUAL' " +
				"AND  protein_idprotein=" + idNewProtein+" AND ecnumber ='"+id+"'");

		if(rs.next()) 
			exists = true;

		rs.close();
		return exists;
	}

	/**
	 * Get reactions IDs for a given ProteinID and ecnumber.
	 * @param proteinID
	 * @param ec
	 * @param stmt
	 * @return Set<String>
	 * @throws SQLException
	 */
	public static Set<Integer> getReactionsIDs(int proteinID, String ec, Statement stmt) throws SQLException{

		Set<Integer> reactionsIDs = new HashSet<>();

		ResultSet rs= stmt.executeQuery("SELECT DISTINCT idreaction FROM reaction " +
				"INNER JOIN reaction_has_enzyme ON reaction_has_enzyme.reaction_idreaction = idreaction " +
				"INNER JOIN pathway_has_reaction ON reaction.idreaction = pathway_has_reaction.reaction_idreaction  " +
				"WHERE pathway_has_reaction.reaction_idreaction = idreaction " +
				"AND reaction_has_enzyme.enzyme_protein_idprotein = "+proteinID +" " +
				"AND reaction_has_enzyme.enzyme_ecnumber = '"+ec+"'");

		while(rs.next()) 
			reactionsIDs.add(rs.getInt(1));

		rs.close();
		return reactionsIDs;
	}

	/**
	 * Get reactions IDs for a given ProteinID and ecnumber.
	 * @param reactionsIDs
	 * @param proteinID
	 * @param ec
	 * @param stmt
	 * @return Set<String>
	 * @throws SQLException
	 */
	public static Set<Integer> getReactionsIDs2(Set<Integer> reactionsIDs, int proteinID, String ec, Statement stmt) throws SQLException{

		ResultSet rs= stmt.executeQuery("SELECT DISTINCT idreaction FROM reactions_view_noPath_or_noEC " +
				"INNER JOIN reaction_has_enzyme ON reaction_has_enzyme.reaction_idreaction=idreaction " +
				"WHERE enzyme_protein_idprotein = "+proteinID+" AND enzyme_ecnumber = '"+ec+"'");

		while(rs.next()) 
			reactionsIDs.add(rs.getInt(1));

		rs.close();
		return reactionsIDs;
	}

	/**
	 * Get data from reaction_has_enzyme table for a given idreaction.
	 * @param idreaction
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getReactionHasEnzymeData2(int idreaction, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs= stmt.executeQuery("SELECT enzyme_protein_idprotein, enzyme_ecnumber FROM reaction_has_enzyme " +
				"INNER JOIN enzyme ON (enzyme_protein_idprotein = enzyme.protein_idprotein AND enzyme_ecnumber = enzyme.ecnumber)"+
				"WHERE inModel AND reaction_idreaction = "+idreaction);

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
	 * Check existence of entries in enzime table for a given proteinID and ECnumber.
	 * @param idProtein
	 * @param ecnumber
	 * @param stmt
	 * @return boolean
	 * @throws SQLException
	 */
	public static boolean checkEnzyme(int idProtein, String ecnumber, Statement stmt) throws SQLException{

		boolean exists = false;

		ResultSet rs = stmt.executeQuery("SELECT * FROM enzyme WHERE protein_idprotein = "+idProtein+" AND ecnumber = '"+ecnumber+"';");

		if(rs.next()) 
			exists = true;

		rs.close();
		return exists;
	}	

	/**
	 * Get data from regulatory_event table.
	 * @param qls
	 * @param stmt
	 * @return HashMap<String,String[]>
	 * @throws SQLException
	 */
	public static HashMap<String,String[]> getDataFromRegulatoryEvent(HashMap<String,String[]> qls, Statement stmt) throws SQLException{

		ResultSet rs = stmt.executeQuery("SELECT promoter_idpromoter, COUNT(protein_idprotein) FROM regulatory_event " +
				"GROUP BY promoter_idpromoter ORDER BY promoter_idpromoter");

		while(rs.next())
			qls.get(rs.getString(1))[2] = rs.getString(2);

		rs.close();
		return qls;
	}

	/**
	 * Get data from sigma_promoter table.
	 * @param qls
	 * @param stmt
	 * @return HashMap<String,String[]>
	 * @throws SQLException
	 */
	public static HashMap<String,String[]> getDataFromSigmaPromoter(HashMap<String,String[]> qls, Statement stmt) throws SQLException{

		ResultSet rs = stmt.executeQuery("SELECT promoter_idpromoter, COUNT(protein_idprotein) FROM sigma_promoter " +
				"GROUP BY promoter_idpromoter ORDER BY promoter_idpromoter");

		while(rs.next())
			qls.get(rs.getString(1))[3] = rs.getString(2);

		rs.close();
		return qls;
	}

	/**
	 * Get data from transcription_unit_promoter table.
	 * @param qls
	 * @param stmt
	 * @return HashMap<String,String[]>
	 * @throws SQLException
	 */
	public static HashMap<String,String[]> getDataFromTranscriptUnitPromoter(HashMap<String,String[]> qls, Statement stmt) throws SQLException{

		ResultSet rs = stmt.executeQuery("SELECT promoter_idpromoter, COUNT(transcription_unit_idtranscription_unit) " +
				"FROM transcription_unit_promoter GROUP BY promoter_idpromoter ORDER BY promoter_idpromoter");

		while(rs.next())
			qls.get(rs.getString(1))[4] = rs.getString(2);

		rs.close();
		return qls;
	}

	/**
	 * Calculate number of promoters with with regulations by TFs.
	 * @param stmt
	 * @return String with the value
	 * @throws SQLException
	 */
	public static String countPromoterWithRegulationsByTFs(Statement stmt) throws SQLException{

		String res = "";

		ResultSet rs = stmt.executeQuery("SELECT count(distinct(transcription_unit_promoter.promoter_idpromoter)) " +
				"FROM transcription_unit_promoter " +
				"JOIN regulatory_event ON " +
				"transcription_unit_promoter.promoter_idpromoter = regulatory_event.promoter_idpromoter");

		if(rs.next())
			res = rs.getString(1);

		rs.close();
		return res;
	}

	/**
	 * Count number of promoters and number of promoters with no name associated.
	 * @param stmt
	 * @return Integer[]
	 * @throws SQLException
	 */
	public static Integer[] countPromoters(Statement stmt) throws SQLException{

		Integer[] list = new Integer[3];

		int num=0;
		int noname=0;
		int noap=0;

		ResultSet rs = stmt.executeQuery("SELECT protein_idprotein, promoter_idpromoter, ri_function_idri_function, " +
				"binding_site_position, protein.name, promoter.name, " +
				"symbol FROM regulatory_event JOIN protein ON protein_idprotein = " +
				"idprotein JOIN promoter ON promoter_idpromoter = idpromoter " +
				"JOIN ri_function ON ri_function_idri_function = idri_function " +
				"ORDER BY protein_idprotein,promoter_idpromoter");

		while(rs.next()) {
			num++;
			if(rs.getString(2)==null) noname++;
			if(rs.getString(3)==null) noap++;
		}

		list[0] = num;
		list[1] = noname;
		list[2] = noap;

		rs.close();
		return list;
	}

	/**
	 * Get data from reaction table.
	 * @param aux
	 * @param ecnumber
	 * @param stmt
	 * @return Set<String>
	 * @throws SQLException
	 */
	public static Set<String> getDataFromReaction(String aux, String ecnumber, Statement stmt) throws SQLException{

		Set<String> reactionsIDs = new HashSet<String>();

		ResultSet rs= stmt.executeQuery("SELECT DISTINCT idreaction FROM reaction " +
				"INNER JOIN reaction_has_enzyme ON reaction_has_enzyme.reaction_idreaction = idreaction " +
				//"INNER JOIN pathway_has_enzyme ON pathway_has_enzyme.enzyme_protein_idprotein = reaction_has_enzyme.enzyme_protein_idprotein  " +
				"INNER JOIN pathway_has_reaction ON idreaction = pathway_has_reaction.reaction_idreaction  " +
				"WHERE pathway_has_reaction.reaction_idreaction = idreaction " +
				aux+
				"AND reaction_has_enzyme.enzyme_ecnumber = '"+ecnumber+"'");

		while(rs.next())
			reactionsIDs.add(rs.getString(1));

		rs.close();
		return reactionsIDs;
	}

	/**
	 * Get data from reactions_view_noPath_or_noEC table.
	 * @param reactionsIDs
	 * @param aux
	 * @param ecnumber
	 * @param stmt
	 * @return Set<String>
	 * @throws SQLException
	 */
	public static Set<String> getDataFromReactionsViewNoPathOrNoEc(Set<String> reactionsIDs, String aux, String ecnumber, Statement stmt) throws SQLException{

		ResultSet rs= stmt.executeQuery("SELECT DISTINCT idreaction FROM reactions_view_noPath_or_noEC " +
				"INNER JOIN reaction_has_enzyme ON reaction_has_enzyme.reaction_idreaction=idreaction " +
				"WHERE enzyme_ecnumber = '"+ecnumber+"'"+aux);

		while(rs.next())
			reactionsIDs.add(rs.getString(1));

		rs.close();
		return reactionsIDs;
	}

	/**
	 * Get statistics of sigma factors.
	 * @param table
	 * @param stmt
	 * @return Integer[]
	 * @throws SQLException
	 */
	public static Integer[] countSigmaFactors(String table, Statement stmt) throws SQLException{

		Integer[] list = new Integer[3];

		LinkedList<String> proteinsids = new LinkedList<String>();
		LinkedList<String> promotersids = new LinkedList<String>();

		int num=0;
		int nproteins=0;
		int npromoter=0;

		ResultSet rs = stmt.executeQuery("SELECT * FROM "+ table);

		while(rs.next())
		{
			num++;
			if(!proteinsids.contains(rs.getString(1)))
			{
				nproteins++;
				proteinsids.add(rs.getString(1));
			}
			if(!promotersids.contains(rs.getString(2)))
			{
				npromoter++;
				promotersids.add(rs.getString(2));
			}
		}

		list[0] = num;
		list[1] = nproteins;
		list[2] = npromoter;

		rs.close();
		return list;
	}

	/**
	 * Get data from Sigma_Promoter table.
	 * @param stmt
	 * @return HashMap<String,ArrayList<String[]>>
	 * @throws SQLException
	 */
	public static HashMap<String,ArrayList<String[]>> getDataFromSigmaPromoter(Statement stmt) throws SQLException{

		HashMap<String,ArrayList<String[]>> index = new HashMap<String,ArrayList<String[]>>();

		ArrayList<String> check = new ArrayList<String>();

		ResultSet rs = stmt.executeQuery("SELECT idprotein, gene.idgene, gene.name " +
				"FROM sigma_promoter JOIN protein ON sigma_promoter.protein_idprotein = protein.idprotein " +
				"JOIN subunit ON protein.idprotein = subunit.protein_idprotein " +
				"JOIN gene ON gene.idgene = subunit.gene_idgene " +
				"ORDER BY idprotein");

		while(rs.next()) {
			if(!check.contains(rs.getString(1)+"."+rs.getString(2)))
			{
				check.add(rs.getString(1)+"."+rs.getString(2));
				if(index.containsKey(rs.getString(1)))
					index.get(rs.getString(1)).add(new String[]{rs.getString(1),rs.getString(2),rs.getString(3)});

				else
				{
					ArrayList<String[]> lis = new ArrayList<String[]>();
					lis.add(new String[]{rs.getString(1),rs.getString(2),rs.getString(3)});
					index.put(rs.getString(1),lis);
				}
			}
		}
		rs.close();
		return index;
	}


	/**
	 * Get data from Sigma_Promoter table.
	 * @param stmt
	 * @return ArrayList<String[]> 
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getDataFromSigmaPromoter2(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT protein.idprotein, protein.name, gene.idgene, gene.name " +
				"FROM sigma_promoter as event, transcription_unit, transcription_unit_gene AS tug, " +
				"transcription_unit_promoter as tup, promoter,gene,protein " +
				"WHERE protein_idprotein=idprotein AND event.promoter_idpromoter=idpromoter " +
				"AND tup.promoter_idpromoter=idpromoter AND " +
				"tup.transcription_unit_idtranscription_unit=idtranscription_unit AND " +
				"tug.transcription_unit_idtranscription_unit=idtranscription_unit AND " +
				"gene_idgene=idgene " +
				"ORDER BY protein.idprotein");

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
	 * Get row informations from sigma_promoter table for a given proteinID.
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getRowInfoSigmaPromoter(String id, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT distinct(gene.idgene), gene.name FROM sigma_promoter AS " +
				"event, transcription_unit, transcription_unit_gene AS tug, " +
				"transcription_unit_promoter AS tup, promoter,gene,protein " +
				"WHERE protein_idprotein=idprotein AND event.promoter_idpromoter=idpromoter " +
				"AND tup.promoter_idpromoter=idpromoter AND " +
				"tup.transcription_unit_idtranscription_unit=idtranscription_unit AND " +
				"tug.transcription_unit_idtranscription_unit=idtranscription_unit AND " +
				"gene_idgene=idgene AND protein.idprotein = " + id +
				" ORDER BY gene.name");

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
	 * Count number of regulated genes.
	 * @param stmt
	 * @return String
	 * @throws SQLException
	 */
	public static String countNumberOfRegulatedGenes(Statement stmt) throws SQLException{

		String value = "";

		ResultSet rs = stmt.executeQuery("SELECT count(distinct(gene.idgene)) " +
				"FROM regulatory_event as event, transcription_unit, " +
				"transcription_unit_gene AS tug, transcription_unit_promoter " +
				"as tup, promoter,gene WHERE event.promoter_idpromoter=idpromoter " +
				"AND tup.promoter_idpromoter=idpromoter AND " +
				"tup.transcription_unit_idtranscription_unit=idtranscription_unit " +
				"AND tug.transcription_unit_idtranscription_unit=idtranscription_unit " +
				"AND gene_idgene=idgene");

		if(rs.next())
			value = rs.getString(1);

		rs.close();
		return value;
	}

	/**
	 * Calculate TFs encoded by multiple genes.
	 * @param stmt
	 * @return String
	 * @throws SQLException
	 */
	public static int countTFsEncodedByGenes(Statement stmt) throws SQLException{

		int numultiplegene=0;

		ResultSet rs = stmt.executeQuery("SELECT count(distinct(gene.idgene)) " +
				"FROM regulatory_event as event, transcription_unit, " +
				"transcription_unit_gene AS tug, transcription_unit_promoter " +
				"as tup, promoter,gene WHERE event.promoter_idpromoter=idpromoter " +
				"AND tup.promoter_idpromoter=idpromoter AND " +
				"tup.transcription_unit_idtranscription_unit=idtranscription_unit " +
				"AND tug.transcription_unit_idtranscription_unit=idtranscription_unit " +
				"AND gene_idgene=idgene");

		while(rs.next()){
			if(rs.getInt(2)>1) 
				numultiplegene++;
		}

		rs.close();
		return numultiplegene;
	}

	/**
	 * Get data from regulatory_event table.
	 * @param stmt
	 * @return HashMap<String,ArrayList<String[]>>
	 * @throws SQLException
	 */
	public static HashMap<String,ArrayList<String[]>> getDataFromRegulatoryEvent(Statement stmt) throws SQLException{

		HashMap<String,ArrayList<String[]>> index = new HashMap<String,ArrayList<String[]>>();

		ArrayList<String> check = new ArrayList<String>();		

		ResultSet rs = stmt.executeQuery("SELECT idprotein, gene.idgene, gene.name " +
				"FROM regulatory_event JOIN protein ON regulatory_event.protein_idprotein " +
				"= protein.idprotein JOIN subunit ON protein.idprotein = " +
				"subunit.protein_idprotein JOIN gene ON gene.idgene = subunit.gene_idgene " +
				"ORDER BY idprotein");

		while(rs.next())
		{
			if(!check.contains(rs.getString(1)+"."+rs.getString(2)))
			{
				check.add(rs.getString(1)+"."+rs.getString(2));
				if(index.containsKey(rs.getString(1)))
				{
					index.get(rs.getString(1)).add(new 
							String[]{rs.getString(1),rs.getString(2),rs.getString(3)}
							);
				}
				else
				{
					ArrayList<String[]> lis = new ArrayList<String[]>();
					lis.add(new String[]{rs.getString(1),rs.getString(2),rs.getString(3)});
					index.put(rs.getString(1),lis);
				}
			}
		}

		rs.close();
		return index;
	}

	/**
	 * Get data from regulatory_event table.
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getDataFromRegulatoryEvent2(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT protein.name, protein.idprotein, count(gene.idgene) " +
				"FROM regulatory_event as event,transcription_unit, transcription_unit_gene " +
				"AS tug, transcription_unit_promoter as tup, promoter,gene,protein, " +
				"ri_function WHERE ri_function_idri_function=idri_function AND " +
				"protein_idprotein=idprotein AND event.promoter_idpromoter=idpromoter " +
				"AND tup.promoter_idpromoter=idpromoter AND " +
				"tup.transcription_unit_idtranscription_unit=idtranscription_unit " +
				"AND tug.transcription_unit_idtranscription_unit=idtranscription_unit " +
				"AND gene_idgene=idgene GROUP BY protein.idprotein order by protein.idprotein");

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
	 * Get row informations from TFs table for a given proteinID.
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getRowInfoTFs(String id, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT distinct(gene.idgene), gene.name FROM regulatory_event " +
				"JOIN protein ON regulatory_event.protein_idprotein = protein.idprotein " +
				"JOIN subunit ON protein.idprotein = subunit.protein_idprotein " +
				"JOIN gene ON gene.idgene = subunit.gene_idgene " +
				"WHERE idprotein = "+id+" ORDER BY idprotein");

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
	 * Get TFs data for a given proteinID.
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getTFsData(String id, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT distinct(gene.idgene), gene.name FROM regulatory_event as event,transcription_unit, " +
				"transcription_unit_gene AS tug, transcription_unit_promoter as tup, " +
				"promoter,gene,protein, ri_function " +
				"WHERE ri_function_idri_function=idri_function AND protein_idprotein=idprotein AND " +
				"event.promoter_idpromoter=idpromoter AND tup.promoter_idpromoter=idpromoter AND " +
				"tup.transcription_unit_idtranscription_unit=idtranscription_unit AND " +
				"tug.transcription_unit_idtranscription_unit=idtranscription_unit AND " +
				"gene_idgene=idgene AND protein.idprotein = "+id);

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
	 * Get data from transcription_unit table.
	 * @param stmt
	 * @return HashMap<String, String[]> 
	 * @throws SQLException
	 */
	public static HashMap<String, String[]> getDataFromTU(HashMap<String, String[]> qls, Statement stmt) throws SQLException{

		ResultSet rs = stmt.executeQuery("SELECT idtranscription_unit, "
				+ "COUNT(gene_idgene) FROM transcription_unit JOIN transcription_unit_gene "
				+ "ON transcription_unit.idtranscription_unit = "
				+ "transcription_unit_gene.transcription_unit_idtranscription_unit "
				+ "GROUP BY idtranscription_unit");

		while (rs.next())
			qls.get(rs.getString(1))[1] = rs.getString(2);

		rs.close();
		return qls;
	}

	/**
	 * Get data from transcription_unit table.
	 * @param stmt
	 * @return HashMap<String, String[]> 
	 * @throws SQLException
	 */
	public static HashMap<String, String[]> getDataFromTU2(HashMap<String, String[]> qls, Statement stmt) throws SQLException{

		ResultSet rs = stmt.executeQuery("SELECT idtranscription_unit, COUNT(promoter_idpromoter) "
				+ "FROM transcription_unit JOIN transcription_unit_promoter ON "
				+ "transcription_unit.idtranscription_unit = "
				+ "transcription_unit_promoter.transcription_unit_idtranscription_unit "
				+ "GROUP BY idtranscription_unit");

		while (rs.next())
			qls.get(rs.getString(1))[2] = rs.getString(2);

		rs.close();
		return qls;
	}

	/**
	 * Get gene name for a given idtranscription_unit.
	 * @param id
	 * @param stmt
	 * @return ArrayList<String>
	 * @throws SQLException
	 */
	public static ArrayList<String> getGeneNameFromTU(String id, Statement stmt) throws SQLException{

		ArrayList<String> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT gene.name FROM transcription_unit JOIN transcription_unit_gene "
				+ "ON transcription_unit.idtranscription_unit = transcription_unit_gene.transcription_unit_idtranscription_unit "
				+ "JOIN gene ON idgene = gene_idgene WHERE idtranscription_unit = "+ id);

		while(rs.next())
			result.add(rs.getString(1));

		rs.close();
		return result;
	}

	/**
	 * Get promoter name for a given idtranscription_unit.
	 * @param id
	 * @param stmt
	 * @return ArrayList<String>
	 * @throws SQLException
	 */
	public static ArrayList<String> getPromoterNameFromTU(String id, Statement stmt) throws SQLException{

		ArrayList<String> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT promoter.name FROM transcription_unit JOIN "
				+ "transcription_unit_promoter ON transcription_unit.idtranscription_unit = "
				+ "transcription_unit_promoter.transcription_unit_idtranscription_unit JOIN promoter "
				+ "ON idpromoter = promoter_idpromoter WHERE idtranscription_unit = "+ id);

		while(rs.next())
			result.add(rs.getString(1));

		rs.close();
		return result;
	}

	/**
	 * Get protein composition.
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getProteinComposition(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT gene.idgene, gene.name, protein_composition.subunit " +
				"FROM subunit JOIN protein_composition ON subunit.enzyme_protein_idprotein = protein_composition.subunit " +
				"JOIN gene ON idgene = subunit.gene_idgene;");

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
	 * get metabolites in model.
	 * @param statement
	 * @return List<String>
	 * @throws SQLException
	 */
	public static List<String> getMetabolitesInModel(Statement statement) throws SQLException{

		List<String> metabolites = new ArrayList<String>();

		ResultSet rs = statement.executeQuery("SELECT DISTINCT(compound_idcompound) FROM stoichiometry " +
				"INNER JOIN reaction ON reaction_idreaction = reaction.idreaction " +
				"WHERE inModel");

		while (rs.next())
			metabolites.add(rs.getString(1));

		rs.close();
		return metabolites;
	}

	/**
	 * Get data from reaction_has_enzyme table.
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getReactionHasEnzymeData3(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT reaction_idreaction, enzyme_protein_idprotein, enzyme_ecnumber FROM reaction_has_enzyme "
				+ " INNER JOIN enzyme on (enzyme.protein_idprotein = enzyme_protein_idprotein AND enzyme.ecnumber = enzyme_ecnumber) "
				+ " WHERE inModel "
				+ " ORDER BY reaction_idreaction" );

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
	 * Get data from pathway_has_reaction table.
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getPathwayHasReactionData(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT reaction_idreaction, pathway_idpathway, pathway.name " +
				"FROM pathway_has_reaction " +
				"INNER JOIN pathway ON (pathway_idpathway = pathway.idpathway)" +
				"ORDER BY reaction_idreaction" );

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
	 * Get data from reaction_has_enzyme table.
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getReactionGenes(Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT DISTINCT reaction_idreaction, name, locusTag, subunit.enzyme_ecnumber " +
				"FROM reaction_has_enzyme " +
				"INNER JOIN subunit ON (subunit.enzyme_protein_idprotein = reaction_has_enzyme.enzyme_protein_idprotein AND subunit.enzyme_ecnumber = reaction_has_enzyme.enzyme_ecnumber) " +
				"INNER JOIN gene ON (gene_idgene = gene.idgene) " +
				//"WHERE (note is null OR note NOT LIKE 'unannotated') " +
				"ORDER BY reaction_idreaction;");

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
	 * Get stoichiometry.
	 * @param conditions
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getStoichiometry(String conditions, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT idstoichiometry, reaction_idreaction, compound_idcompound, stoichiometry.compartment_idcompartment, " +
				"stoichiometric_coefficient, numberofchains, compound.name, compound.formula, compound.kegg_id " +
				"FROM reaction " +
				"INNER JOIN stoichiometry ON (stoichiometry.reaction_idreaction = idreaction) " +
				"INNER JOIN compound ON (stoichiometry.compound_idcompound = compound.idcompound) " +
				"WHERE inModel AND " +conditions );

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
	 * SELECT name FROM pathway WHERE name = 'Drains pathway'
	 * @param conditions
	 * @param stmt
	 * @return String
	 * @throws SQLException
	 */
	public static String getDrainsPathway(Statement stmt) throws SQLException{

		String res = null;

		ResultSet rs = stmt.executeQuery("SELECT name FROM pathway WHERE name = 'Drains pathway'");

		if(rs.next())
			res = rs.getString(1);

		rs.close();
		return res;
	}

	/**
	 * Get data in model from reaction table.
	 * @param aux
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getDataFromReaction2(String aux, Statement stmt) throws SQLException{

		ArrayList<String[]> result = new ArrayList<>();

		ResultSet rs = stmt.executeQuery("SELECT reaction.name, kegg_id, idreaction FROM reaction "
				+ " INNER JOIN stoichiometry ON stoichiometry.reaction_idreaction = reaction.idreaction "
				+ " INNER JOIN compound ON (compound_idcompound = compound.idcompound) " +
				aux + " AND inModel ");

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
	 * Retrieves idGene and locusTag from gene table
	 * @param stmt
	 * @return ArrayList<String[]>
	 * @throws SQLException
	 */
	public static ArrayList<String[]> getGeneIdLocusTag (Statement stmt) throws SQLException {

		ResultSet rs = stmt.executeQuery("SELECT idgene, locusTag, sequence_id FROM gene;");
		ArrayList<String[]> result = new ArrayList<>();

		while(rs.next()) {
			String[] list = new String[3];

			list[0] = rs.getString(1);
			list[1] = rs.getString(2);
			list[2] = rs.getString(3);
			
			result.add(list);
		}

		return result;
	}


	/**
	 * Retrives all the querys in geneHomology + homologySetup table that have more that appears more than one once
	 * @param stmt
	 * @return ArrayList<String> querys
	 * @throws SQLException
	 */
	public static ArrayList<String> getDuplicatedQuerys(Statement stmt) throws SQLException {

		ResultSet rs = stmt.executeQuery("SELECT COUNT(*), query, locusTag, program FROM geneHomology " +
				"INNER JOIN homologySetup ON (homologySetup.s_key = homologySetup_s_key)" +
				"GROUP by program, query "+
				"HAVING COUNT(*)>1");
		ArrayList<String> querys = new ArrayList<>();

		while(rs.next())
			querys.add(rs.getString("query"));

		return querys;
	}

	/**
	 * D
	 * @param statement
	 * @param query
	 * @throws SQLException
	 */
	public static void deleteDuplicatedQuerys(Statement statement, String query) throws SQLException {

		statement.execute("DELETE FROM geneHomology WHERE query ='"+query+"'");

	}

	/**
	 * get a map with all locusTag and respective geneid
	 * 
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, Integer> getGeneIds(Statement statement) throws SQLException{

		Map<String, Integer> result = new HashMap<>();

		ResultSet rs = statement.executeQuery("SELECT sequence_id, idgene FROM gene;");

		while(rs.next())
			result.put(rs.getString(1), rs.getInt(2));

		return result;

	}

	/**
	 * Get locus_tag for a given sequenceID.
	 * 
	 * @param sequenceID
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static String getGeneLocusTag(String sequenceID, Statement statement) throws SQLException{

		ResultSet rs = statement.executeQuery("SELECT locusTag FROM gene WHERE  sequence_id = '" + sequenceID + "';");

		if(rs.next())
			return rs.getString(1);

		return null;
	}
	
	
	/**
	 * Get geneID for a given sequenceID.
	 * 
	 * @param sequenceID
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static String getGeneId(String sequenceID, Statement statement) throws SQLException{

		ResultSet rs = statement.executeQuery("SELECT idgene FROM gene WHERE  sequence_id = '" + sequenceID + "';");

		if(rs.next())
			return rs.getString(1);

		return null;

	}
	

	/**
	 * Get genes and locus from database
	 * 
	 * @param statement
	 * @return
	 * @throws SQLException 
	 */
	public static Map<Integer, Pair<String, String>> getGenesFromDatabase(Statement statement) throws SQLException {

		ResultSet rs = statement.executeQuery("SELECT locusTag, name, idgene FROM gene;");

		Map<Integer, Pair<String, String>> pairMap = new HashMap<>();
		while(rs.next()) {

			Pair<String, String> pair = new Pair<String, String> (rs.getString(1), rs.getString(2));
			pairMap.put(rs.getInt(3), pair);
		}

		return pairMap;
	}


	/**
	 * Get genes and locus from database
	 * 
	 * @param statement
	 * @return
	 * @throws SQLException 
	 */
	public static Map<Integer, Pair<String, String>> getGeneFromDatabase(int geneId, Statement statement) throws SQLException {

		ResultSet rs = statement.executeQuery("SELECT locusTag, name, idgene FROM gene WHERE idgene = " + geneId);

		Map<Integer, Pair<String, String>> pairMap = new HashMap<>();
		while(rs.next()) {

			Pair<String, String> pair = new Pair<String, String> (rs.getString(1), rs.getString(2));
			pairMap.put(rs.getInt(3), pair);
		}

		return pairMap;
	}
	
	/**
	 * Get drains in model
	 * 
	 * @param statement
	 * @return
	 * @throws SQLException 
	 */
	public static Set<Integer> getModelDrains(Statement statement) throws SQLException {

		ResultSet rs = statement.executeQuery("SELECT idreaction, COUNT(stoichiometry.compound_idcompound),  COUNT(stoichiometry.compartment_idcompartment) FROM reaction "
				+ " INNER JOIN stoichiometry on reaction_idreaction = idreaction "
				+ " GROUP BY idreaction");

		Set<Integer> ret = new HashSet<>();
		
		while(rs.next())
			if(rs.getInt(2)==1 && rs.getInt(3)==1)
				ret.add(rs.getInt(1));
		
		return ret;
	}
	
	/**
	 * retrieve the auto increment value for a given table
	 * 
	 * @param stmt
	 * @param tableName
	 * @return
	 * @throws SQLException
	 */
	public static int getAutoIncrementValue(Statement stmt, String databaseName, String tableName) throws SQLException{
		
		ResultSet rs = stmt.executeQuery("SELECT `AUTO_INCREMENT` FROM  INFORMATION_SCHEMA.TABLES "
				+ "WHERE TABLE_SCHEMA = '"+ databaseName +"' AND TABLE_NAME = '"+ tableName +"';");
		
		int res = -1;
		
		if(rs.next())
			res = rs.getInt(1);
		
		rs.close();
		return res;
	}
	
	/**
	 * return last inserted id for a given table in the database
	 * 
	 * @param stmt
	 * @param tableName
	 * @return
	 * @throws SQLException
	 */
	public static int getLastInsertedIdMySQL(Statement stmt, String databaseName, String tableName) throws SQLException{
		
		int id = getAutoIncrementValue(stmt, databaseName, tableName) - 1;
		
		return id;
	}

	
	/**
	 * return the last position id value for a given table in H2 database
	 * 
	 * @param stmt
	 * @param tableName
	 * @param sKey
	 * @return
	 * @throws SQLException
	 */
	public static int getLastInsertedIdH2(Statement stmt, String tableName, String pKey) throws SQLException{
		
		ResultSet rs = stmt.executeQuery("SELECT MAX(" + pKey +") FROM " + tableName + ";");
		
		int id = -1;
		
		if(rs.next())
			id = rs.getInt(1);
		
		rs.close();
		return id;
	}
	
	
	/**
	 * Update locusTags using protein_ids
	 * 
	 * @param locusTagsByQueries
	 * @param statement
	 * @param databaseType
	 * @throws SQLException
	 */
	public static void updateLocusTags(Map<String,String> locusTagsByQueries, PreparedStatement statement, DatabaseType databaseType) throws SQLException {
		
		
		int i = 0;
		for (String query : locusTagsByQueries.keySet()) {
			
			String locusTag = locusTagsByQueries.get(query).trim();//.toUpperCase().trim();

			statement.setString(1, locusTag);
			statement.setString(2, query);
			statement.addBatch();

			if ((i + 1) % BATCH_SIZE == 0) {

				statement.executeBatch(); // Execute every 500 items.
			}
			i++;
		}
		statement.executeBatch();
	}
	
	
	/**
	 * @param sequences
	 * @param pStmt
	 * @throws SQLException
	 */
	public static void loadFastaSequences(Map<Integer, String[]> sequences, SequenceType seqType, java.sql.Connection conn) throws SQLException{
		
		PreparedStatement pStmt;
		int i;
		
		if(seqType.equals(SequenceType.RNA) || seqType.equals(SequenceType.RRNA) || seqType.equals(SequenceType.TRNA)){
			
			pStmt = conn.prepareStatement("INSERT INTO sequence (sequence_type,sequence,sequence_length) VALUES(?,?,?);");
			
			i = 0;
			for (Integer geneID : sequences.keySet()) {
				
				String[] seqInfo = sequences.get(geneID);
				
				pStmt.setString(1, seqType.toString());
				pStmt.setString(2, seqInfo[0]);
				pStmt.setInt(3, Integer.parseInt(seqInfo[1]));
				pStmt.addBatch();

				if ((i + 1) % BATCH_SIZE == 0) {

					pStmt.executeBatch(); // Execute every 500 items.
				}
				i++;
			}
			
			pStmt.executeBatch();
		}
		else{
			pStmt = conn.prepareStatement("INSERT INTO sequence (gene_idgene,sequence_type,sequence,sequence_length) VALUES(?,?,?,?);");
				
			i = 0;
			for (Integer geneID : sequences.keySet()) {

				String[] seqInfo = sequences.get(geneID);

				pStmt.setInt(1, geneID);
				pStmt.setString(2, seqType.toString());
				pStmt.setString(3, seqInfo[0]);
				pStmt.setInt(4, Integer.parseInt(seqInfo[1]));
				pStmt.addBatch();

				if ((i + 1) % BATCH_SIZE == 0) {

					pStmt.executeBatch(); // Execute every 500 items.
				}
				i++;
			}
			
			pStmt.executeBatch();
		}
		
	}
	
	/**
	 * retrieve the number of reactions associated with each gene using subunit and reaction_has_enzyme tables
	 * 
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static Map<String,Integer> countGenesReactionsBySubunit(Statement stmt) throws SQLException{
		
		Map<String, Integer> res = new HashMap<>();

		ResultSet rs = stmt.executeQuery("SELECT gene.sequence_id, COUNT(DISTINCT(reaction_has_enzyme.reaction_idreaction))"
				+" FROM subunit INNER JOIN reaction_has_enzyme ON (subunit.enzyme_protein_idprotein = reaction_has_enzyme.enzyme_protein_idprotein"
				+" AND subunit.enzyme_ecnumber = reaction_has_enzyme.enzyme_ecnumber)"
				+" INNER JOIN gene ON gene.idgene = subunit.gene_idgene"
				+" GROUP BY gene.sequence_id "
				+" ORDER BY  gene.sequence_id;");

		while(rs.next())
			res.put(rs.getString(1), rs.getInt(2));
		
		rs.close();
		return res;
	
	}
	
	
	/**
	 * For each gene, retrieve the number of reactions that have the geneID in their boolean_rule
	 * 
	 * @param geneID
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, Integer> countGenesReactionsByBooleanRule(Statement stmt) throws SQLException{
		
		Map<String, Integer> res = new HashMap<>();
				
		ResultSet rs = stmt.executeQuery("SELECT idgene, gene.locusTag, (Select count(*)"
				+ " FROM reaction WHERE reaction.boolean_rule REGEXP CONCAT('^', gene.idgene, ' ')"
				+ " OR reaction.boolean_rule REGEXP CONCAT(' ', gene.idgene, ' ')"
				+ " OR reaction.boolean_rule REGEXP CONCAT(' ', gene.idgene, '$')"
				+ " OR reaction.boolean_rule LIKE gene.idgene) FROM gene;");
		
		while(rs.next())
			res.put(rs.getString(1), rs.getInt(3));
		
		rs.close();
		return res;
	}
	
	
	/**
	 * @param stmt
	 * @return
	 * @throws SQLException
	 * @throws IOException 
	 */
	public static boolean checkManualyInsertedCompartments(Statement stmt) throws SQLException, IOException{


		ResultSet rs = stmt.executeQuery("SELECT DISTINCT(compartment_idcompartment), compartment.abbreviation FROM stoichiometry "
				+ "INNER JOIN compartment ON compartment.idcompartment = stoichiometry.compartment_idcompartment;");

		while(rs.next()){

			if(!rs.getString(2).equals("in") && !rs.getString(2).equals("out"))
				return false;
		}
		
		return true;
	}

	/**
	 * @param stmt
	 * @param text
	 * @return
	 * @throws SQLException
	 */
	public static String getCompartmentAbbreviation(Statement stmt, String compartmentName) throws SQLException {
		
		String abb = null;
		
		ResultSet rs = stmt.executeQuery("SELECT abbreviation FROM compartments where name = '"+  compartmentName  + "';");
		
		if(rs.next())
			abb = rs.getString(1);
		
		return abb;
	}
	
//	/**
//	 * @param stmt
//	 * @return
//	 * @throws SQLException
//	 * @throws IOException 
//	 */
//	public static List<String> checkManualyInsertedCompartments(Statement stmt) throws SQLException, IOException{
//		
//		Map<Integer,String> manualCompartments = new HashMap<>();
//		
//		ResultSet rs = stmt.executeQuery("SELECT DISTINCT(compartment_idcompartment), compartment.abbreviation, compartment.name FROM stoichiometry "
//				+ "INNER JOIN compartment ON compartment.idcompartment = stoichiometry.compartment_idcompartment;");
//		
//		while(rs.next()){
//			
//			if(!rs.getString(2).equals("in") && !rs.getString(2).equals("out"))
//				manualCompartments.put(rs.getInt(1),rs.getString(3));
//			
//		}
//		
//		String getReactionsQuery = "SELECT DISTINCT(reaction_idreaction), stoichiometry.compartment_idcompartment, reaction.name FROM stoichiometry "
//				+ "INNER JOIN reaction ON stoichiometry.reaction_idreaction=reaction.idreaction WHERE stoichiometry.compartment_idcompartment = ";
//		
//		String deleteReactionsQuery = "DELETE FROM stoichiometry WHERE compartment_idcompartment = ";
//		
//		if(!manualCompartments.isEmpty()){
//			
//			for(Integer compartmentID : manualCompartments.keySet()){
//				
//				getReactionsQuery.concat(Integer.toString(compartmentID)).concat(" OR compartment_idcompartment = ");
//				
//				deleteReactionsQuery.concat(Integer.toString(compartmentID)).concat(" OR compartment_idcompartment = ");
//			}
//		}
//		getReactionsQuery = getReactionsQuery.substring(0, getReactionsQuery.lastIndexOf(" OR ")).trim().concat(";");
//		deleteReactionsQuery = getReactionsQuery.substring(0, getReactionsQuery.lastIndexOf(" OR ")).trim().concat(";");
//
//		
//		List<String> reactionsInfo = new ArrayList<>();
//		
//		rs = stmt.executeQuery(getReactionsQuery);
//		while(rs.next()){
//			reactionsInfo.add(rs.getString(3).concat("\t").concat(manualCompartments.get(rs.getInt(2))));
//		}
//		
//		rs = stmt.executeQuery(deleteReactionsQuery);
//		
////		FileWriter writer = new FileWriter("output.txt"); 
////		for(String info: reactionsInfo) {
////		  writer.write(str);
////		}
////		writer.close();
//		
//		return reactionsInfo;
//		
//	}
	
	
/////////////////Create model from exisiting models/////////////////////
	
	/**
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static boolean isModelEmpty(Statement stmt) throws SQLException{
		
		ResultSet rs = stmt.executeQuery("Select * from compound;");
		
		if(rs.next())
			return false;
		
		rs = stmt.executeQuery("Select * from reaction;");
		
		if(rs.next())
			return false;
		
		return true;
	}
	
	
	/**
	 * @param stmt
	 * @param reactionsInModel
	 * @throws SQLException
	 */
	public static void setAllReactionsInModel(Statement stmt, boolean setInModel, boolean keepSpontaneousReactions) throws SQLException{
		
		if(setInModel){
			stmt.execute("UPDATE reaction SET inModel=true;");
		}
		else if(keepSpontaneousReactions){
			stmt.execute("UPDATE reaction SET boolean_rule=null;");
			stmt.execute("UPDATE reaction SET inModel=false WHERE not isSpontaneous;");
		}
		else{
			stmt.execute("UPDATE reaction SET inModel=false, boolean_rule=null;");
		}
		
	}
	
	/**
	 * @param pStmt
	 * @param reactionsToAdd
	 * @throws SQLException 
	 */
	public static void addReactionsInModel(PreparedStatement pStmt, Set<String> reactionsToAdd, String addedByNote) throws SQLException{

		int i = 0;
		for (String reaction : reactionsToAdd) {

			pStmt.setString(1, "true");
			pStmt.setString(2, addedByNote);
			pStmt.setString(3, reaction);

			pStmt.addBatch();

			if ((i + 1) % 1000 == 0) {

				pStmt.executeBatch(); // Execute every 1000 items.
			}
			i++;
		}
		pStmt.executeBatch();
	}
	
	
	
	/**
	 * @param pStmt
	 * @param reactionsToAdd
	 * @param addedByNote
	 * @throws SQLException
	 */
	public static void updateReactionsInModel(PreparedStatement pStmt, Map<String,String> reactionsToAdd, String addedByNote) throws SQLException{

		//PreparedStatement: "UPDATE reaction SET inModel=?, notes=?, boolean_rule=? WHERE reaction.name=?"
		
		int i = 0;
		for (String reaction : reactionsToAdd.keySet()) {
			
			String booleanRule = reactionsToAdd.get(reaction);

			pStmt.setBoolean(1, true);
			pStmt.setString(2, addedByNote);
			if(booleanRule.equals(""))
				pStmt.setString(3,null);
			else
				pStmt.setString(3,booleanRule);
			pStmt.setString(4, reaction);

			pStmt.addBatch();

			if ((i + 1) % 1000 == 0) {

				pStmt.executeBatch(); // Execute every 1000 items.
			}
			i++;
		}
		pStmt.executeBatch();
	}
	
	/**
	 * @param stmt
	 * @param genesSet
	 * @return
	 * @throws SQLException 
	 */
	public static Map<String,Map<String,List<String>>> getGenesReactionsByBooleanRule(Statement stmt, DatabaseType dbType) throws SQLException{
		
		Map<String, Map<String,List<String>>> genesReactions = new HashMap<>();
		
		if(dbType.equals(DatabaseType.MYSQL))
			stmt.execute("SET GLOBAL group_concat_max_len=20000");
		
		ResultSet rs = stmt.executeQuery("SELECT gene.locusTag, gene.sequence_id,"
				+ " (Select GROUP_CONCAT(CONCAT(reaction.idreaction, '|', reaction.name, '|',reaction.boolean_rule))"
				+ " FROM reaction WHERE reaction.boolean_rule REGEXP CONCAT('^', gene.idgene, ' ')"
				+ " OR reaction.boolean_rule REGEXP CONCAT(' ', gene.idgene, ' ')"
				+ " OR reaction.boolean_rule REGEXP CONCAT(' ', gene.idgene, '$')"
				+ " OR reaction.boolean_rule LIKE gene.idgene) FROM gene;");
		
		while(rs.next()){
			
			Map<String, List<String>> reactions = new HashMap<>();
			String gene = rs.getString(2);
			
			if(rs.getString(3)!=null){
				
				String[] splitedReactions = rs.getString(3).split(",");
				
				for(String info : splitedReactions){
					
					String[] splitedInfo = info.split("\\|");
					List<String> reactionInfo = new ArrayList<>();
					
					reactionInfo.add(splitedInfo[0]); //reaction ID
					reactionInfo.add(splitedInfo[2]); //reaction boolean_rule
					
					reactions.put(splitedInfo[1], reactionInfo);
				}
				
			}
			
			genesReactions.put(gene, reactions);
		}
		
		rs.close();
		
		return genesReactions;
	}
	
	
	/**
	 * retrieve the reactions associated with each gene using subunit and reaction_has_enzyme tables
	 * 
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	public static Map<String,List<String>> getGenesReactionsBySubunit(Statement stmt) throws SQLException{
		
		Map<String, List<String>> genesReactions = new HashMap<>();

		ResultSet rs = stmt.executeQuery("SELECT gene.sequence_id, gene.locusTag, GROUP_CONCAT(DISTINCT(reaction.name))"
				+" FROM subunit INNER JOIN reaction_has_enzyme ON (subunit.enzyme_protein_idprotein = reaction_has_enzyme.enzyme_protein_idprotein"
				+" AND subunit.enzyme_ecnumber = reaction_has_enzyme.enzyme_ecnumber)"
				+" INNER JOIN gene ON gene.idgene = subunit.gene_idgene"
				+" INNER JOIN reaction ON reaction.idreaction = reaction_has_enzyme.reaction_idreaction"
				+" GROUP BY gene.sequence_id"
				+" ORDER BY gene.locusTag;");

		while(rs.next()){
			
			List<String> reactions = new ArrayList<>();

			if(rs.getString(3)!=null)
				reactions = Arrays.asList(rs.getString(3).split(","));

			genesReactions.put(rs.getString(1), reactions);
		}
		
		rs.close();
		return genesReactions;
	
	}
	
	
//	/**
//	 * @param stmt
//	 * @param geneIdsToKeep
//	 * @throws SQLException 
//	 */
//	public static void deleteGenesIDsSkeysFromTables(Statement stmt, Set<String> tables, Set<Integer> geneIdsToKeep) throws SQLException{
//		
//		for(String table : tables){
//
//			String query = "DELETE FROM ";
//
//			query = query.concat(table).concat(" WHERE ");
//
//			for(Integer geneId : geneIdsToKeep){
//
//				query = query.concat("gene_idgene not like ").concat(Integer.toString(geneId).concat(" AND "));
//			}
//
//			query = query.substring(0, query.lastIndexOf(" AND "));
//			query = query.concat(";");
//
//			stmt.execute(query);
//		}
//	}
	
	
	/**
	 * @param stmt
	 * @param tablesNames
	 * @param key
	 * @param incrementValue
	 * @throws SQLException
	 */
	public static void incrementTablesIds(Statement stmt, Set<String> tables, String key, Integer incrementValue, String dbName, DatabaseType dbType) throws SQLException{
		
		String query;
		
		for(String table : tables){
			
			if(table.equals("gene_has_compartment") && dbType.equals(DatabaseType.MYSQL))
				stmt.execute("ALTER TABLE `"+ dbName +"`.`gene_has_compartment` DROP PRIMARY KEY;");
		
			query = "UPDATE " + table + " SET " + key + " = " + key + "+" + incrementValue + ";";
			
			stmt.executeUpdate(query);
		}
		
	}
	
	
//	/**
//	 * @param pStmt
//	 * @param oldNewGeneIdsMap
//	 * @throws SQLException
//	 */
//	public static void updateGenesIds(PreparedStatement pStmt, String[] tables, Map<Integer,Integer> oldNewGeneIdsMap) throws SQLException{
//
//		//PreparedStatement: "UPDATE ? SET gene_idgene=? WHERE gene_idgene=?"
//		int i = 0;
//		
//		for(String table : tables){
//		
//			for (Integer oldID : oldNewGeneIdsMap.keySet()) {
//				
//				pStmt.setString(1, table);
//				pStmt.setInt(2, oldNewGeneIdsMap.get(oldID));
//				pStmt.setInt(3, oldID);
//
//				pStmt.addBatch();
//
//				if ((i + 1) % 1000 == 0) {
//
//					pStmt.executeBatch(); // Execute every 1000 items.
//				}
//				i++;
//			}
//		}
//		pStmt.executeBatch();
//	}
	
//	/**
//	 * @param pStmt
//	 * @param oldNewGeneIdsMap
//	 * @throws SQLException
//	 */
//	public static void updateGenesIds(PreparedStatement pStmt, Map<Integer,Integer> oldNewGeneIdsMap) throws SQLException{
//
//		//PreparedStatement: "UPDATE <table> SET gene_idgene=? WHERE gene_idgene=?"
//		int i = 0;
//
//		for (Integer oldID : oldNewGeneIdsMap.keySet()) {
//			
//			pStmt.setInt(1, oldNewGeneIdsMap.get(oldID));
//			pStmt.setInt(2, oldID);
//
//			pStmt.addBatch();
//
//			if ((i + 1) % 1000 == 0) {
//
//				pStmt.executeBatch(); // Execute every 1000 items.
//			}
//			i++;
//		}
//		pStmt.executeBatch();
//	}
	
	/**
	 * @param pStmt
	 * @param oldNewGeneIdsMap
	 * @throws SQLException
	 */
	public static void updateGenesIds(PreparedStatement pStmt, Map<Integer,List<Integer>> oldNewGeneIdsMap) throws SQLException{

		//PreparedStatement: "INSERT INTO <table> (gene_idgene, <otherColumns>) SELECT ?, <otherColumns> FROM <table> WHERE gene_idgene = ?;"
		int i = 0;

		for (Integer oldID : oldNewGeneIdsMap.keySet()) {
			
			for(Integer newID : oldNewGeneIdsMap.get(oldID)){

				pStmt.setInt(1, newID);
				pStmt.setInt(2, oldID);

				pStmt.addBatch();

				if ((i + 1) % 1000 == 0) {

					pStmt.executeBatch(); // Execute every 1000 items.
				}
				i++;
			}
		}
		pStmt.executeBatch();
	}
	
	
	/**
	 * @param stmt
	 * @param table
	 * @param condition
	 * @throws SQLException
	 */
	public static void deleteEntriesFromTable(Statement stmt, String table, String condition) throws SQLException{
		
		String query = "DELETE FROM ".concat(table).concat(" WHERE ").concat(condition).concat(";");
		
		stmt.execute(query);
	}
	
	
	/**
	 * @param stmt
	 * @param tables
	 * @param condition
	 * @throws SQLException
	 */
	public static void deleteEntriesFromTables(Statement stmt, Set<String> tables, String condition) throws SQLException{
		
		for(String table : tables){
			
			String query = "DELETE FROM ".concat(table).concat(" WHERE ").concat(condition).concat(";");
			
			stmt.execute(query);
		}
	}
	
	/**
	 * for a given set of reactions retriev a set of reactions not present in database
	 * 
	 * @param stmt
	 * @param reactions
	 * @return
	 * @throws SQLException
	 */
	public static Set<String> filterReactionsNotInDatabase(Statement stmt, Set<String> reactions) throws SQLException{
		
		Set<String> reactionsNotInDb = new HashSet<>();
		
		ResultSet rs;
		
//		System.out.println("-----------filterReactionsNotInDatabase---------");
		
		for(String reactionName :  reactions){
			
//			System.out.println(reactionName);
			
			rs = stmt.executeQuery("SELECT * FROM reaction WHERE name='"+reactionName+"';");
			
			if(!rs.next()){
//				System.out.println("Reaction not present in database!");
				reactionsNotInDb.add(reactionName);
			}
//			else{
//				System.out.println("Reaction present in database!");
//			}
			
			rs.close();
		}
		
		return reactionsNotInDb;
	}
	
	
	/**
	 * retrieve a list of reactions Ids for a given set of reactions names
	 * 
	 * @param stmt
	 * @param reactionsNames
	 * @return
	 * @throws SQLException 
	 */
	public static List<Integer> getReactionsIDsByName(Statement stmt, Set<String> reactionsNames) throws SQLException{
		
		List<Integer> ids =  new ArrayList<>();
		ResultSet rs = null;
		
		for(String reactionName :  reactionsNames){
			
			rs = stmt.executeQuery("SELECT idreaction FROM reaction WHERE name='"+reactionName+"';");
			
			while(rs.next()){
				ids.add(rs.getInt(1));
			}
		}
		
		rs.close();
		
		return ids;
	}
	
	
	/**
	 * @param newDbstmt
	 * @param oldDbStmt
	 * @param oldMetId
	 * @return
	 * @throws SQLException
	 */
	public static Integer checkAndInsertMetaboliteIfNeeded(Statement newDbstmt, Statement oldDbStmt, String oldMetId, DatabaseType dbType) throws SQLException{
		
		ResultSet rs = oldDbStmt.executeQuery("SELECT * FROM compound WHERE idcompound="+oldMetId+";");
		rs.next();
		
		String metaboliteName = DatabaseUtilities.databaseStrConverter(rs.getString(2),dbType);

		String sourceDbId = null;
		if(rs.getString(4)!=null && !rs.getString(4).isEmpty())
			sourceDbId = rs.getString(4);
		
		String formula = null;
		if(rs.getString(6)!=null && !rs.getString(6).isEmpty())
			formula = rs.getString(6);
		
		Double molecularWeight = null;
		if(rs.getDouble(7)!=0)
			molecularWeight = rs.getDouble(7);
		
		Integer newMetaboliteId = -1;
		
		ResultSet rs2 = null; 
		
//		System.out.println("Searching for source id "+ sourceDbId+"...");
		
		if(sourceDbId!=null){
			rs2 = newDbstmt.executeQuery("SELECT idcompound FROM compound WHERE kegg_id='"+sourceDbId+"';");
			
			if(rs2.next()){
				newMetaboliteId = rs2.getInt(1);
				
//				System.out.println("Source id matched! New metabolite id = "+newMetaboliteId);
			}
		}
		
		if(newMetaboliteId==-1 && metaboliteName!=null && !metaboliteName.isEmpty()){
//			System.out.println("Searching for metabolite name "+ metaboliteName+"...");
			
			rs2 = newDbstmt.executeQuery("SELECT idcompound FROM compound WHERE name='"+metaboliteName+"';");
			
			if(rs2.next()){
				newMetaboliteId = rs2.getInt(1);
				
//				System.out.println("metabolite name matched! New metabolite id = "+newMetaboliteId);
			}
		}

		if(newMetaboliteId==-1 && formula!=null){
//			System.out.println("Searching for formula "+ formula+"...");

			rs2 = newDbstmt.executeQuery("SELECT idcompound FROM compound WHERE formula='"+formula+"';");
			
			if(rs2.next()){
				newMetaboliteId = rs2.getInt(1);
				
//				System.out.println("Formula matched! New metabolite id = "+newMetaboliteId);

			}
		}
		
//		if(newMetaboliteId==-1 && molecularWeight!=null){
//			System.out.println("Searching for molecular weight "+ molecularWeight +"...");
//
//			rs2 = newDbstmt.executeQuery("SELECT idcompound FROM compound WHERE molecular_weight="+molecularWeight+";");
//			
//			if(rs2.next()){
//				newMetaboliteId = rs2.getInt(1);
//				
//				System.out.println("Molecular weight matched! New metabolite id = "+newMetaboliteId);
//
//			}
//		}
		
		if(newMetaboliteId==-1){
			
//			System.out.println("Inserting new compound...");
			
			Integer charge = null;
			if(rs.getInt(9)!=0)
				charge = rs.getInt(9);
			
//			System.out.println("Molecular weight: "+molecularWeight+"\t charge: "+charge);
//			
//			System.out.println("Query: INSERT INTO compound (name,kegg_id,formula,molecular_weight,charge)"
//					+ " VALUES('" + metaboliteName + "','" + sourceDbId + "','" + formula + "'," + molecularWeight + "," + charge +");");
			
			newDbstmt.execute("INSERT INTO compound (name,kegg_id,formula,molecular_weight,charge)"
					+ " VALUES('" + metaboliteName + "','" + sourceDbId + "','" + formula + "'," + molecularWeight + "," + charge +");");
			
			rs2 = newDbstmt.executeQuery("SELECT LAST_INSERT_ID()");
			rs2.next();
			newMetaboliteId = rs2.getInt(1);
			
//			System.out.println("New compound inserted with idcompound="+newMetaboliteId);
		}
		
		rs.close();
		rs2.close();
		
		return newMetaboliteId;
	}
	
	
	/**
	 * @param newDbstmt
	 * @param oldDbStmt
	 * @param oldPathwayId
	 * @return
	 * @throws SQLException
	 */
	public static Integer checkAndInsertPathwayIfNeeded(Statement newDbstmt, Statement refDbStmt, String oldPathwayId, DatabaseType dbType) throws SQLException{
		
		ResultSet rs = refDbStmt.executeQuery("SELECT * FROM pathway WHERE idpathway="+oldPathwayId+";");
		
		String pathwayName = "";
		String pathwayCode = "";
		
		if(rs.next()){
			pathwayName = DatabaseUtilities.databaseStrConverter(rs.getString(3), dbType) ;
			pathwayCode = rs.getString(2);
		}
		
		Integer newPathwayId = -1;
		
		ResultSet rs2 = null;
		
//		System.out.println("Pathway code: "+pathwayCode+"\t pathwayName: "+pathwayName);
		
		if(pathwayCode!=null & !pathwayCode.isEmpty()){
			rs2 = newDbstmt.executeQuery("SELECT idpathway FROM pathway WHERE code='"+pathwayCode+"';");
			
//			System.out.println("Search for pathwayCode...");
			
			if(rs2.next()){
				newPathwayId = rs2.getInt(1);
				
//				System.out.println("Pathway code matched! New Pathway id: "+newPathwayId);
			}
		}
		
		if(newPathwayId==-1 && pathwayName!=null && !pathwayName.isEmpty()){
			
//			System.out.println("Search for pathway name...");
			
			rs2 = newDbstmt.executeQuery("SELECT idpathway FROM pathway WHERE name='"+pathwayName+"';");
			
			if(rs2.next()){
				newPathwayId = rs2.getInt(1);
				
				System.out.println("Pathway name matched! New Pathway id: "+newPathwayId);
			}
		}
		
		if(newPathwayId==-1){
			
//			System.out.println("Inserting pathway...");
//			System.out.println("Query: INSERT INTO pathway (code,name) VALUES('" + pathwayCode + "','" + pathwayName + "');");
			
			newDbstmt.execute("INSERT INTO pathway (code,name) VALUES('" + pathwayCode + "','" + pathwayName + "');");
			
			rs2 = newDbstmt.executeQuery("SELECT LAST_INSERT_ID()");
			rs2.next();
			newPathwayId = rs2.getInt(1);
			
//			System.out.println("Pathway inserted with idpathway = "+newPathwayId);
		}
		
		rs.close();
		rs2.close();
		
		return newPathwayId;
	}
	
	
	/**
	 * @param newDbstmt
	 * @param refDbStmt
	 * @param oldProteinEnzymePair
	 * @return
	 * @throws SQLException
	 */
	public static Pair<String,String> checkAndInsertProteinEnzymePairIfNeeded(Statement newDbstmt, Statement refDbStmt, 
			Pair<String,String> oldProteinEnzymePair, DatabaseType dbType) throws SQLException{
		
		System.out.println("SELECT * FROM protein WHERE idprotein="+ oldProteinEnzymePair.getA() +";");
		
		ResultSet rs = refDbStmt.executeQuery("SELECT * FROM protein WHERE idprotein="+ oldProteinEnzymePair.getA() +";");
		rs.next();
		
		String proteinName = DatabaseUtilities.databaseStrConverter(rs.getString(2),dbType);
		String proteinClass = rs.getString(3);
		String inchi = null;
		if(rs.getString(4)!=null && !rs.getString(4).isEmpty())
			inchi = rs.getString(4);
		Integer molecularWeight = rs.getInt(5);
		if(molecularWeight==0)
			molecularWeight=null;
		
		
		System.out.println("oldName: "+rs.getString(2));
		
		System.out.println("Protein name: "+proteinName+"\t proteinClass: "+proteinClass+"\t inchi: "+inchi+"\t molecularWeight: "+molecularWeight);
		System.out.println((inchi==null)+"\t"+(molecularWeight==null));
		
		String newProteinId = "";
		
		System.out.println("Query: SELECT idprotein FROM protein WHERE name='"+proteinName+"';");
		
		ResultSet rs2 = newDbstmt.executeQuery("SELECT idprotein FROM protein WHERE name='"+proteinName+"';");
		
		System.out.println("Trying protein name...");
		
		if(rs2.next()){
			
			newProteinId = Integer.toString(rs2.getInt(1));
			
			System.out.println("Protein name matched! New id: "+newProteinId);
		}
		
		if(newProteinId.equals("") && inchi!=null && !inchi.isEmpty()){
			
			System.out.println("Trying inchi...");
			
			rs2 = newDbstmt.executeQuery("SELECT idprotein FROM protein WHERE inchi='"+inchi+"';");

			if(rs2.next()){

				newProteinId = Integer.toString(rs2.getInt(1));
				
				System.out.println("Inchi matched! New id: "+newProteinId);

			}
		}
		
		if(newProteinId.equals("") && molecularWeight!=null){
			
			System.out.println("Trying molecular weight...");
			
			rs2 = newDbstmt.executeQuery("SELECT idprotein FROM protein WHERE molecular_weight="+molecularWeight+";");

			if(rs2.next()){
				
				newProteinId = Integer.toString(rs2.getInt(1));

				System.out.println("Molecular weight matched! New id: "+newProteinId);

			}
		}
		
		if(newProteinId.equals("")){
			
			System.out.println("Inserting protein "+ proteinName+"...");
			System.out.println("Query: INSERT INTO protein (name,class,inchi,molecular_weight) VALUES('" + proteinName + "','" + proteinClass 
					+ "','" + inchi + "'," + molecularWeight + ");");
			
			newDbstmt.execute("INSERT INTO protein (name,class,inchi,molecular_weight) VALUES('" + proteinName + "','" + proteinClass 
					+ "','" + inchi + "'," + molecularWeight + ");");
			
			rs2 = newDbstmt.executeQuery("SELECT LAST_INSERT_ID()");
			rs2.next();
			newProteinId = Integer.toString(rs2.getInt(1));
			
			System.out.println("Protein inserted! New id: "+newProteinId);
		}
		
		String ecNumber = oldProteinEnzymePair.getB();
		
		rs = refDbStmt.executeQuery("SELECT * FROM enzyme WHERE ecnumber='"+ ecNumber +"';");
		rs.next();
		
		String gprStatus = null;
		if(rs.getString(5)!=null && !rs.getString(5).isEmpty())
			gprStatus = "'".concat(rs.getString(5)).concat("'");
		
		rs2 = newDbstmt.executeQuery("SELECT * FROM enzyme WHERE ecnumber='"+ ecNumber +"' AND protein_idprotein="+ newProteinId +";");
		
		if(!rs2.next()){
			
			System.out.println("Inserting enzyme "+ecNumber+"...");
			System.out.println("Query: INSERT INTO enzyme (ecnumber,protein_idprotein,inModel,source,gpr_status) VALUES('" + ecNumber + "'," 
					+ newProteinId + "," + true + ",'Models_merge'," + gprStatus + ");");
			
			newDbstmt.execute("INSERT INTO enzyme (ecnumber,protein_idprotein,inModel,source,gpr_status) VALUES('" + ecNumber + "'," 
					+ newProteinId + "," + true + ",'Models_merge'," + gprStatus + ");");
		}
		else{
			
			System.out.println("Updating enzyme "+ecNumber+"...");
			System.out.println("Query: UPDATE enzyme SET inModel=true, source='Models_merge', gpr_status="+ gprStatus +");");
			
			newDbstmt.execute("UPDATE enzyme SET inModel=true, source='Models_merge', gpr_status="+ gprStatus +" WHERE ecnumber='"+ ecNumber +"' AND protein_idprotein="+ newProteinId +";");
		}
		
		Pair<String,String> newProteinEnzymePair = new Pair<String, String>(newProteinId, ecNumber);
		
		System.out.println("New Pair: "+newProteinEnzymePair);
		
		rs.close();
		rs2.close();
		
		return newProteinEnzymePair;
	}
	
	
	/**
	 * @param newDbStmt
	 * @param refDbStmt
	 * @throws SQLException
	 */
	public static void mergeCompartmentTables(Statement newDbStmt, Statement refDbStmt) throws SQLException{
		
		ResultSet rs = refDbStmt.executeQuery("SELECT * FROM compartment;");
		
		while(rs.next()){
			
			String name = rs.getString(2);
			String abb = rs.getString(3);
			
			ResultSet rs2 = newDbStmt.executeQuery("SELECT * FROM compartment WHERE name='" + name + "';");
			
			if(!rs2.next()){
				
				rs2 = newDbStmt.executeQuery("SELECT * FROM compartment WHERE abbreviation='" + abb + "';");
				
				if(!rs2.next()){
					newDbStmt.execute("INSERT INTO compartment (name,abbreviation) VALUES('"+ name +"','"+ abb +"');");
				}
			}
		}
		
		rs.close();
	}
	
	
	/**
	 * @param stmt
	 * @param reaction
	 * @return
	 * @throws SQLException
	 */
	public static List<String> getReactionEcNumbers(Statement stmt, String reaction) throws SQLException{
		
		List<String> ecNumbers = new ArrayList<>();
		
		ResultSet rs = stmt.executeQuery("SELECT enzyme_ecnumber FROM reaction_has_enzyme"
				+ " INNER JOIN reaction ON reaction.idreaction=reaction_has_enzyme.reaction_idreaction"
				+ " WHERE reaction.name='"+ reaction + "';");
		
		while(rs.next())
			ecNumbers.add(rs.getString(1));
			
		rs.close();
			
		return ecNumbers;
	}

	/**
	 * @param newModelStmt
	 * @param refModelStmt
	 * @param enzymeCompartments
	 * @return
	 * @throws SQLException
	 */
	public static List<Integer> convertOldCompartmentIds(Statement newModelStmt, Statement refModelStmt,
			List<Integer> enzymeCompartments) throws SQLException {
		
		List<Integer> compartmentIdsUpdated = new ArrayList<>();
		
		ResultSet rs =null ,rs2 = null;
		
		for(Integer compartmentId : enzymeCompartments){
			
			rs = refModelStmt.executeQuery("SELECT name FROM compartment WHERE idcompartment="+compartmentId+";");
			
			if(rs.next()){
				rs2 = newModelStmt.executeQuery("SELECT idcompartment FROM compartment WHERE name='"+rs.getString(1)+"';");
				
				if(rs2.next())
					compartmentIdsUpdated.add(rs2.getInt(1));
			}
		}
		
		rs.close();
		rs2.close();
		
		return compartmentIdsUpdated;
	}

	/**
	 * @param stmt
	 * @param reactionName
	 * @return
	 * @throws SQLException
	 */
	public static Pair<String,String> getCompartmentFromReactionTable(Statement stmt, String reactionName) throws SQLException {
		
		ResultSet rs = stmt.executeQuery("SELECT compartment.name, compartment.abbreviation FROM reaction INNER JOIN compartment"
				+ " ON compartment.idcompartment=reaction.compartment_idcompartment WHERE reaction.name='"+reactionName+"';");
		
		String compartmentName = "inside";
		String abb =  "in";
				
		if(rs.next()){
			compartmentName = rs.getString(1);
			abb = rs.getString(2);
		}
		
		Pair<String,String> compartment = new Pair<String, String>(compartmentName, abb);
		
		rs.close();
		
		return compartment;
	}

	/**
	 * @param stmt
	 * @param compartmentName
	 * @return
	 * @throws SQLException
	 */
	public static Integer getCompartmentId(Statement stmt, Pair<String,String> compartment) throws SQLException {
		
		String compartmentName = compartment.getA();
		String abb = compartment.getB();
		
		Integer compartmentId = -1;
		
		ResultSet rs = stmt.executeQuery("SELECT idcompartment FROM compartment WHERE name='"+compartmentName+"';");
		
		if(!rs.next()){
			
			rs = stmt.executeQuery("SELECT idcompartment FROM compartment WHERE abbreviation='"+abb+"';");
			
			if(rs.next())
				compartmentId = rs.getInt(1);
		}
		else{
			compartmentId = rs.getInt(1);
		}
		
		rs.close();
		
		return compartmentId;	
	}
	
	
	/**
	 * @param newDbStmt
	 * @param refDbStmt
	 * @param oldGeneID
	 * @param newGeneID
	 * @throws SQLException
	 */
	public static void trasnferEntryIfAbsentToSubunit(Statement newDbStmt, Statement refDbStmt, String oldGeneID, String newGeneID) throws SQLException{
		
		System.out.println("---------------------------------------------");
		System.out.println("oldGeneID: "+oldGeneID+"\t newGeneID: "+newGeneID);
		
		System.out.println("Query: SELECT enzyme_ecnumber FROM subunit WHERE gene_idgene="+oldGeneID+";");
		
		ResultSet rs = refDbStmt.executeQuery("SELECT enzyme_ecnumber FROM subunit WHERE gene_idgene="+oldGeneID+";");

		while(rs.next()){

			String ecNumber = rs.getString(1);
			
			System.out.println("ecNumber: "+ecNumber);
			
			System.out.println("Query: SELECT protein_idprotein FROM enzyme WHERE ecnumber='"+ecNumber+"';");

			ResultSet rs2 = newDbStmt.executeQuery("SELECT protein_idprotein FROM enzyme WHERE ecnumber='"+ecNumber+"';");
			
			ConcurrentLinkedQueue<String> proteinIds = new ConcurrentLinkedQueue<>();
			
			while(rs2.next())
				proteinIds.add(rs2.getString(1));
			
			System.out.println("proteinIds: "+proteinIds);
			
			while(!proteinIds.isEmpty()){
				
				String proteinID =  proteinIds.poll();
				
				System.out.println("Protein id: "+proteinID);
				
				System.out.println("proteinIds: "+proteinIds);
				
				System.out.println("Query: SELECT * FROM subunit WHERE gene_idgene="+newGeneID+" AND enzyme_protein_idprotein="+proteinID
						+" AND enzyme_ecnumber='"+ecNumber+"';");

				rs2 = newDbStmt.executeQuery("SELECT * FROM subunit WHERE gene_idgene="+newGeneID+" AND enzyme_protein_idprotein="+proteinID
						+" AND enzyme_ecnumber='"+ecNumber+"';");

				if(!rs2.next()){
					
					System.out.println("Query: INSERT INTO subunit (gene_idgene,enzyme_protein_idprotein,enzyme_ecnumber)"
							+ " VALUES("+ newGeneID +","+ proteinID +",'"+ ecNumber +"');");

					newDbStmt.execute("INSERT INTO subunit (gene_idgene,enzyme_protein_idprotein,enzyme_ecnumber)"
							+ " VALUES("+ newGeneID +","+ proteinID +",'"+ ecNumber +"');");
				}
			}
			
			rs2.close();
		}
		
		rs.close();
	}

	/**
	 * @param targetDbStmt
	 * @param refDbStmt
	 * @param targetGenes
	 * @param orthologsGenesIDsMap
	 * @throws SQLException
	 */
	public static void transferGenesHasCompartments(Statement targetDbStmt, Statement refDbStmt, Set<Integer> targetGenes, Map<Integer, List<Integer>> orthologsGenesIDsMap) throws SQLException {
		
		for(Integer orthologId : targetGenes){
			
			List<Map<String,Object>> orthologHasCompartmentInfo = new ArrayList<>();
			
			ResultSet rs = refDbStmt.executeQuery("SELECT * FROM gene_has_compartment WHERE gene_idgene="+orthologId+";");
			
			while(rs.next()){
				
				Map<String,Object> rowInfo = new HashMap<>();
				
				rowInfo.put("idcompartment", rs.getInt(2));
				rowInfo.put("primaryLocation", rs.getBoolean(3));
				rowInfo.put("score", rs.getInt(4));
				
				orthologHasCompartmentInfo.add(rowInfo);
			}
			
			for(Map<String,Object> rowData : orthologHasCompartmentInfo){
				
				Integer oldCompartmentId = (Integer) rowData.get("idcompartment");
				boolean primaryLocation = (boolean) rowData.get("primaryLocation");
				Integer score = (Integer) rowData.get("score");
				
				rs = refDbStmt.executeQuery("SELECT * FROM compartment WHERE idcompartment=" + oldCompartmentId + ";");
				rs.next();
				
				String compartmentName = rs.getString(2);
				String compartmentAbb = rs.getString(3);
				
				ResultSet rs2 = targetDbStmt.executeQuery("SELECT idcompartment FROM compartment WHERE name='" + compartmentName + "';");	
				
				if(!rs2.next()){
					rs2 = targetDbStmt.executeQuery("SELECT idcompartment FROM compartment WHERE abbreviation='" + compartmentAbb + "';");
					
					if(!rs2.next()){
						targetDbStmt.execute("INSERT INTO compartment (name,abbreviation) VALUES('"+ compartmentName +"','"+ compartmentAbb +"');");
						rs2 = targetDbStmt.executeQuery("SELECT LAST_INSERT_ID()");
						rs2.next();
					}
				}
				
				Integer newCompartmentId = rs2.getInt(1);
				
				for(Integer newGeneId : orthologsGenesIDsMap.get(orthologId)){
					
					System.out.println("gene_has_compartent new entry: "+newGeneId+"\t"+newCompartmentId+"\t"+primaryLocation+"\t"+score);
					
					targetDbStmt.execute("INSERT INTO gene_has_compartment (gene_idgene,compartment_idcompartment,primaryLocation,score)"
							+ " VALUES("+ newGeneId +","+ newCompartmentId +","+ primaryLocation +","+ score +");");
				}
				
				rs2.close();
			}
			
			rs.close();
		}
	}
	
	/**
	 * @param stmt
	 * @param genesIds
	 * @return
	 * @throws SQLException
	 */
	public static Map<Integer,String> getGenesNames(Statement stmt, Set<Integer>genesIds) throws SQLException{
		
		Map<Integer,String> genesNames = new HashMap<>();
		
		ResultSet rs = stmt.executeQuery("SELECT idgene,name,locusTag,sequence_id FROM gene;");
		
		while(rs.next()){
			
			Integer geneId =  rs.getInt(1);
			
			if(genesIds.contains(geneId))
				genesNames.put(geneId, rs.getString(2));
			
		}
		
		return genesNames;
	}
	
	/**
	 * @param pStmt
	 * @param genesIdsNamesMap
	 * @throws SQLException
	 */
	public static void insertGenesNames(PreparedStatement pStmt, Map<Integer,String> genesIdsNamesMap) throws SQLException{
		
		//PreparedStatement: INSERT INTO gene (name) VALUES(?) WHERE idgene=?;
		
		int i = 0;

		for (Integer geneId : genesIdsNamesMap.keySet()) {
			
			String geneName = genesIdsNamesMap.get(geneId);
			
			pStmt.setString(1, geneName);
			pStmt.setInt(2, geneId);

			pStmt.addBatch();

			if ((i + 1) % 1000 == 0) {

				pStmt.executeBatch(); // Execute every 1000 items.
			}
			i++;
		}
		pStmt.executeBatch();
	}

	/**
	 * @param stmt
	 * @param reactionName
	 * @param booleanRule
	 * @throws SQLException
	 */
	public static void addRuleToReactionBooleanRule(Statement stmt, String reactionName, String booleanRule) throws SQLException {
		
		ResultSet rs = stmt.executeQuery("SELECT boolean_rule FROM reaction WHERE name='"+reactionName+"';");
		
		if(rs.next()){
			
			String newBooleanRule = rs.getString(1);
			
			if(newBooleanRule!=null && !newBooleanRule.isEmpty())
				newBooleanRule = newBooleanRule.concat(" OR ").concat(booleanRule);
			else
				newBooleanRule = booleanRule;
			
			stmt.execute("UPDATE reaction SET boolean_rule='"+newBooleanRule+"' WHERE name='"+reactionName+"';");
		}
		
		rs.close();
		
	}

	/**
	 * @param stmt
	 * @param reactionName
	 * @param geneIds
	 * @throws SQLException 
	 */
	public static void addEntriesToReactionHasEnzyme(Statement stmt, String reactionName, Set<Integer> geneIds) throws SQLException {
		
		for(Integer idgene : geneIds){
			
			ResultSet rs = stmt.executeQuery("SELECT * FROM subunit INNER JOIN reaction_has_enzyme"
				+" ON (subunit.enzyme_protein_idprotein = reaction_has_enzyme.enzyme_protein_idprotein"
				+" AND subunit.enzyme_ecnumber = reaction_has_enzyme.enzyme_ecnumber)"
				+" INNER JOIN reaction ON reaction_has_enzyme.reaction_idreaction = reaction.idreaction"
				+" WHERE subunit.gene_idgene="+ idgene +" AND reaction.name='"+ reactionName +"';");
			
			
			if(!rs.next()){
				
				rs = stmt.executeQuery("SELECT idreaction FROM reaction WHERE name='"+ reactionName +"';");
				
				if(rs.next()){
					
					Integer idReaction = rs.getInt(1);
					
					rs = stmt.executeQuery("SELECT DISTINCT enzyme_protein_idprotein,enzyme_ecnumber FROM subunit WHERE gene_idgene="+ idgene +";");
					
					while(rs.next()){
						
						Integer proteinId = rs.getInt(1);
						String ecNumber = rs.getString(2);
						
						stmt.execute("INSERT INTO reaction_has_enzyme (reaction_idreaction,enzyme_ecnumber,enzyme_protein_idprotein)"
								+ " VALUES("+ idReaction +",'"+ ecNumber +"',"+ proteinId +");");
						
					}
					
				}
			}
			
			rs.close();
		}
	}
	
	
	/**
	 * @param stmt
	 * @param compartment
	 * @return
	 * @throws SQLException
	 */
	public static Integer insertCompartment(Statement stmt, Pair<String,String> compartment) throws SQLException{
		
		String compartmentName = compartment.getA();
		String compartmentAbb = compartment.getB();
		
		stmt.executeQuery("INSERT INTO compartment (name,abbreviation) VALUES('"+ compartmentName +"','"+ compartmentAbb +"');");	
		
		ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
		rs.next();
		
		Integer compartmentID = rs.getInt(1);
		
		rs.close();
		
		return compartmentID;
	}
	
	
	/**
	 * Retrieve a mapa where the keys are the genes sequenceIds and the values its locusTags
	 * 
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, String> getGenesLocusTagSeqIdMap(Statement statement) throws SQLException {
		
		ResultSet rs = statement.executeQuery("SELECT distinct(sequence_id), locusTag FROM gene;");
		
		Map<String,String> locusTagSeqIdMap = new HashMap<>();
		
		while(rs.next())
			locusTagSeqIdMap.put(rs.getString(1), rs.getString(2));
		
		rs.close();
		
		return locusTagSeqIdMap;
	}
	
	///////////////////////////////////////////////////
	/**
	 * Method to clean Gene table if it isn't empty already
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	public static void cleanGeneTable(Statement stmt) throws SQLException {
		
		ResultSet rs = stmt.executeQuery("SELECT * FROM gene;");
		
		if(rs.next()){
			stmt.execute("DELETE sequence FROM sequence INNER JOIN gene ON gene.idgene=sequence.gene_idgene;");
			stmt.execute("DELETE FROM gene;");
			
			resetAutoIncrementValue(stmt, "gene");
		}
		
		rs.close();
	}

	
	/**
	 * @param stmt
	 * @param table
	 * @throws SQLException
	 */
	public static void resetAutoIncrementValue(Statement stmt, String table) throws SQLException{
		
		try {
			stmt.execute("ALTER TABLE gene ALTER COLUMN idgene RESTART WITH 1;");
			
		} catch (MySQLSyntaxErrorException e){
			
			stmt.execute("ALTER TABLE gene AUTO_INCREMENT = 1;");
		}			
	}
	
	/**
	 * Method to clean Sequence table if it isn't empty already
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	public static void cleanSequenceTable(Statement stmt) throws SQLException {
		
		ResultSet rs = stmt.executeQuery("SELECT * FROM sequence;");
		
		if(!rs.next())
			stmt.execute("DROP FROM sequence;");
		
		rs.close();			
	}
	
	/**
	 * delete all data from Gene and Sequence tables
	 * 
	 * @param stmt
	 * @throws SQLException 
	 */
	public static void cleanGeneAndSequenceTables(Statement stmt) throws SQLException{
		
		stmt.execute("DELETE FROM sequence");
		resetAutoIncrementValue(stmt, "sequence");
		
		stmt.execute("DELETE FROM gene;");
		resetAutoIncrementValue(stmt, "gene");
	}
	
	/**
	 * delete specific type of sequences (specified in aux String) on sequence table
	 * 
	 * @param stmt
	 * @param sequenceType
	 * @throws SQLException
	 */
	public static void deleteSequencesFromSequenceTable(Statement stmt, SequenceType sequenceType) throws SQLException{

		if(sequenceType.equals(SequenceType.RNA)){
			
			stmt.execute("DELETE FROM sequence WHERE sequence_type='"+ SequenceType.RRNA.toString() +"'"
					+ " OR sequence_type='"+ SequenceType.TRNA.toString() +"';");
		}
		else{
			stmt.execute("DELETE FROM sequence WHERE sequence_type='"+ sequenceType.toString() +"';");
		}

	}
	
	
//	/**
//	 * @param stmt
//	 * @param seqType
//	 * @throws SQLException
//	 */
//	public static void deleteGeneEntriesBySequenceType(Statement stmt, SequenceType seqType) throws SQLException{
//		
//		if(seqType.equals(SequenceType.RNA)){
//			
//			stmt.execute("DELETE gene FROM gene INNER JOIN sequence ON sequence.gene_idgene=gene.idgene "
//					+ "WHERE sequence.sequence_type='"+ SequenceType.RRNA.toString() +"'"
//					+ " OR sequence.sequence_type='"+ SequenceType.TRNA.toString() +"';");
//			
//		}
//		else{
//			stmt.execute("DELETE gene FROM gene INNER JOIN sequence ON sequence.gene_idgene=gene.idgene "
//					+ "WHERE sequence.sequence_type='"+ seqType.toString() +"';");
//		}
//	}
	
	/**
	 * @param stmt
	 * @param seqType
	 * @throws SQLException
	 */
	public static void deleteGenesAndSequencesByType(Statement stmt, SequenceType seqType) throws SQLException{
		
		if(seqType.equals(SequenceType.RNA)){
			
			stmt.execute("DELETE gene FROM gene INNER JOIN sequence ON sequence.gene_idgene=gene.idgene "
					+ "WHERE sequence.sequence_type='"+ SequenceType.RRNA.toString() +"'"
					+ " OR sequence.sequence_type='"+ SequenceType.TRNA.toString() +"';");
			
		}
		else{
			stmt.execute("DELETE gene FROM gene INNER JOIN sequence ON sequence.gene_idgene=gene.idgene "
					+ "WHERE sequence.sequence_type='"+ seqType.toString() +"';");
			
		}
		
		deleteSequencesFromSequenceTable(stmt, seqType);
	}
	
	
	/**
	 * @param stmt
	 * @param seqType
	 * @return
	 * @throws SQLException
	 * @throws CompoundNotFoundException
	 */
	public static Map<String, AbstractSequence<?>> getGenomeFromDatabase(Statement stmt, SequenceType seqType) throws SQLException, CompoundNotFoundException{
		
		Map<String, AbstractSequence<?>> genomeSequences = new HashMap<>();
		ResultSet rs;
		
		//protein.faa
		if(seqType.equals(SequenceType.PROTEIN)){
		
			rs = stmt.executeQuery("SELECT gene.sequence_id, sequence.sequence FROM sequence"
					+ " INNER JOIN gene ON gene.idgene=sequence.gene_idgene"
					+ " WHERE sequence.sequence_type='"+ seqType.toString() +"';");
			
			AbstractSequence<?> sequence;
			
			while(rs.next()){
				
				sequence = new ProteinSequence(rs.getString(2));
				sequence.setOriginalHeader(rs.getString(1));
				
				genomeSequences.put(rs.getString(1), sequence);
			}
		}
		
		//cds_from_genomic.faa
		else if(seqType.equals(SequenceType.CDS_DNA)){
			
			rs = stmt.executeQuery("SELECT gene.sequence_id, sequence.sequence FROM sequence"
					+ " INNER JOIN gene ON gene.idgene=sequence.gene_idgene"
					+ " WHERE sequence.sequence_type='"+ seqType.toString() +"';");
			
			AbstractSequence<?> sequence;
			
			while(rs.next()){
				
				sequence = new DNASequence(rs.getString(2));
				sequence.setOriginalHeader(rs.getString(1));
				
				genomeSequences.put(rs.getString(1), sequence);
			}
		}
		
		//rna_from_genomic.fna and genomic.fna
		else{
			
			rs = stmt.executeQuery("SELECT idsequence, sequence FROM sequence"
					+ " WHERE sequence.sequence_type='"+ seqType.toString() +"';");
			
			AbstractSequence<?> sequence;

			if(seqType.equals(SequenceType.GENOMIC_DNA)){

				while(rs.next()){

					String key = seqType.toString().concat("_").concat(rs.getString(1));

					sequence = new DNASequence(rs.getString(2));

					genomeSequences.put(key, sequence);
				}
			} 
			
			else {
				
				while(rs.next()){

					String key = seqType.toString().concat("_").concat(rs.getString(1));

					sequence = new RNASequence(rs.getString(2));

					genomeSequences.put(key, sequence);
				}
			}
		}
		
		rs.close();

		return genomeSequences;
	}

	/**
	 * @param stmt
	 * @return
	 * @throws SQLException 
	 */
	public static boolean checkGenomeSequences(Statement stmt, SequenceType seqType) throws SQLException {
		
		ResultSet rs = stmt.executeQuery("SELECT * FROM sequence WHERE sequence.sequence_type='"+ seqType.toString() +"';");
		
		if(rs.next()){
			rs.close();
			return true;
		}
		else{
			rs.close();
			return false;
		}
	}
}	
