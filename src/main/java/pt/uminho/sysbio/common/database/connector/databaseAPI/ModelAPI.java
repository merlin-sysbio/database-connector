package pt.uminho.sysbio.common.database.connector.databaseAPI;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import pt.uminho.ceb.biosystems.mew.utilities.datastructures.pair.Pair;
import pt.uminho.sysbio.common.database.connector.databaseAPI.containers.gpr.GeneAssociation;
import pt.uminho.sysbio.common.database.connector.databaseAPI.containers.gpr.ModuleCI;
import pt.uminho.sysbio.common.database.connector.databaseAPI.containers.gpr.ProteinsGPR_CI;
import pt.uminho.sysbio.common.database.connector.databaseAPI.containers.gpr.ReactionProteinGeneAssociation;
import pt.uminho.sysbio.common.database.connector.databaseAPI.containers.gpr.ReactionsGPR_CI;
import pt.uminho.sysbio.common.database.connector.datatypes.Connection;
import pt.uminho.sysbio.common.database.connector.datatypes.DatabaseUtilities;
import pt.uminho.sysbio.common.database.connector.datatypes.Enumerators.DatabaseType;


/**
 * @author Oscar Dias
 *
 */
public class ModelAPI {



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

		ResultSet rs = statement.executeQuery("SELECT idgene FROM gene WHERE locusTag = '"+locusTag+"' AND sequence_id = '"+sequence_id+"';");

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
	public static Map<String, String> getQueries(Statement statement) throws SQLException {

		Map<String, String> ret = new HashMap<>();


		ResultSet rs = statement.executeQuery("SELECT locusTag, query FROM geneHomology;");

		while(rs.next())	
			ret.put(rs.getString(1), rs.getString(2));

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

		ResultSet rs = statement.executeQuery("SELECT locusTag FROM gene "//WHERE origin<>'HOMOLOGY'"
				);

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

		ResultSet rs = statement.executeQuery("SELECT gene.name, locusTag, chromosome.name FROM gene "+
				"INNER JOIN chromosome ON (idchromosome=chromosome_idchromosome) " //+
				//"WHERE origin='KEGG'"
				);

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

		ResultSet rs = statement.executeQuery("SELECT locusTag, alias FROM gene " +
				"INNER JOIN aliases ON (idgene=aliases.entity) " +
				"WHERE class='g' "
				);

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

		ResultSet rs = statement.executeQuery("SELECT locusTag, enzyme_ecNumber FROM gene " +
				"INNER JOIN subunit ON (idgene=gene_idgene) "
				);

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

		ResultSet 	rs = statement.executeQuery("SELECT locusTag, protein.name FROM gene " +
				"INNER JOIN subunit ON (idgene=gene_idgene) " +
				"INNER JOIN protein ON (subunit.enzyme_protein_idprotein=idprotein) "
				);

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

		ResultSet rs = statement.executeQuery("SELECT locusTag, alias FROM gene " +
				"INNER JOIN subunit ON (idgene=gene_idgene) " +
				"INNER JOIN aliases ON (subunit.enzyme_protein_idprotein=aliases.entity)" +
				" WHERE class='p' "
				);

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

		ResultSet rs = statement.executeQuery("SELECT pathway.idpathway, pathway.name, pathway_has_enzyme.enzyme_ecnumber FROM pathway " +
				"INNER JOIN pathway_has_enzyme ON (pathway.idpathway=pathway_idpathway) " +
				"ORDER BY idpathway");

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

		ResultSet rs = statement.executeQuery("SELECT pathway.idpathway, pathway.name, enzyme.ecnumber FROM pathway " +
				"INNER JOIN pathway_has_enzyme ON (pathway.idpathway=pathway_idpathway) " +
				"INNER JOIN enzyme ON (enzyme.ecnumber=pathway_has_enzyme.enzyme_ecnumber) " +
				"WHERE enzyme.inModel ORDER BY idpathway");

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
								"INNER JOIN reaction_has_enzyme ON reaction_has_enzyme.reaction_idreaction = idreaction " +
								"INNER JOIN pathway_has_enzyme ON pathway_has_enzyme.enzyme_protein_idprotein = reaction_has_enzyme.enzyme_protein_idprotein  " +
								"INNER JOIN pathway_has_reaction ON pathway_has_enzyme.pathway_idpathway = pathway_has_reaction.pathway_idpathway  " +
								"WHERE pathway_has_reaction.reaction_idreaction = idreaction " +
								"AND reaction_has_enzyme.enzyme_protein_idprotein = '"+idProtein+"' " +
								"AND reaction_has_enzyme.enzyme_ecnumber = '"+enzyme+"'");

						while(resultSet.next())
							reactions_ids.add(resultSet.getString(1));

						resultSet= statement.executeQuery("SELECT idreaction FROM reactions_view_noPath_or_noEC " +
								"INNER JOIN reaction_has_enzyme ON reaction_has_enzyme.reaction_idreaction=idreaction " +
								"WHERE enzyme_protein_idprotein = "+idProtein+" AND enzyme_ecnumber = '"+enzyme+"'");

						while(resultSet.next())
							reactions_ids.add(resultSet.getString(1));

						for(String idreaction: reactions_ids)
							statement.execute("UPDATE reaction SET inModel = true, source = 'HOMOLOGY' WHERE idreaction = '"+idreaction+"'");

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

		DecimalFormat df = new DecimalFormat("#.##");

		ResultSet rs = statement.executeQuery("SELECT gene_idgene FROM gene_has_compartment " +
				"WHERE gene_idgene = "+idGene+" AND primaryLocation;");

		if(!rs.next())
			statement.execute("INSERT INTO gene_has_compartment (gene_idgene, compartment_idcompartment, primaryLocation, score) " +
					"VALUES("+idGene+","+compartmentsDatabaseIDs.get(primaryCompartment)+","+true+",'"+df.format(scorePrimaryCompartment)+"')");

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
						"VALUES("+idGene+","+compartmentsDatabaseIDs.get(compartment)+",false,'"+df.format(secondaryCompartmens.get(compartment))+"')");
		}
		rs.close();
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
	public static void loadReaction(String idCompartment, boolean inModel, String ecNumber, Statement statement, boolean isTransport, DatabaseType databaseType, String name, String equation, boolean reversible, boolean generic, boolean spontaneous, 
			boolean nonEnzymatic, String reactionSource, String notes, List<String> proteins, List<String> enzymes, Map<String, List<String>> ecNumbers, List<String> pathways, List<String> compounds, List<String> compartments, List<String> stoichiometry, 
			List<String> chains) throws SQLException {

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

		boolean addCompounds = false;

		if(rs.next()) {

			addCompounds = false;
		}
		else {

			String source = reactionSource;
			if(!inModel && !isTransport)
				source = "KEGG";

			statement.execute("INSERT INTO reaction (name, equation, reversible, inModel, isGeneric, isSpontaneous, isNonEnzymatic, source, originalReaction,compartment_idcompartment, notes) " +
					"VALUES('"+DatabaseUtilities.databaseStrConverter(name+"_C"+idCompartment, databaseType)+"','"+DatabaseUtilities.databaseStrConverter(equation, databaseType)+"',"
					+reversible+","+inModel+","+generic+","+spontaneous+","+nonEnzymatic+",'"+source+"',false,"+idCompartment+", '"+notes+"');");

			rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
			rs.next();
			addCompounds = true;

		}

		String newReactionID = rs.getString(1);

		if(ecNumber==null)
			for(int j = 0; j< proteins.size(); j++)
				ModelAPI.addReaction_has_Enzyme(proteins.get(j), enzymes.get(j), newReactionID, statement);
		else
			for(String protein_id : ecNumbers.get(ecNumber))
				ModelAPI.addReaction_has_Enzyme(protein_id, ecNumber, newReactionID, statement);

		for(String idPathway : pathways)
			ModelAPI.addPathway_has_Reaction(idPathway, newReactionID, statement);

		if(addCompounds) {

			for(int j = 0 ; j < compounds.size(); j++ ) {

				String newCOmpartmentID = idCompartment;

				if(isTransport)
					newCOmpartmentID = compartments.get(j);

				rs = statement.executeQuery("SELECT * FROM stoichiometry" +
						" WHERE reaction_idreaction = "+newReactionID+
						" AND compartment_idcompartment = "+newCOmpartmentID+
						" AND compound_idcompound = "+compounds.get(j)+
						" AND stoichiometric_coefficient = '"+stoichiometry.get(j)+ "' "+
						" AND numberofchains = '"+chains.get(j)+ "' ;");

				if(!rs.next())
					statement.execute("INSERT INTO stoichiometry (reaction_idreaction, compound_idcompound, compartment_idcompartment, stoichiometric_coefficient, numberofchains) " +
							"VALUES("+newReactionID+","+compounds.get(j)+","+newCOmpartmentID+",'"+stoichiometry.get(j)+ "','"+chains.get(j)+ "');");
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
	 * 
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


		List<Pair<String, String>> proteinsPairs = new ArrayList<>();
		rs = statement.executeQuery("SELECT reaction_idreaction, enzyme_protein_idprotein, enzyme_ecnumber  " +
				" FROM reaction_has_enzyme;");
		while (rs.next())
			if(reactionsMap.containsKey(rs.getString(1)))			
				proteinsPairs.add(new  Pair<>(rs.getString(2), rs.getString(3)));

		reactionsMap.get(rs.getString(1)).put("proteins", proteinsPairs);


		List<String> pathways = new ArrayList<>();
		rs = statement.executeQuery("SELECT reaction_idreaction, pathway_idpathway FROM pathway_has_reaction;");
		while (rs.next())
			if(reactionsMap.containsKey(rs.getString(1)))
				pathways.add(rs.getString(2));

		reactionsMap.get(rs.getString(1)).put("pathways",pathways);


		List<String[]> entry = new ArrayList<>();
		rs = statement.executeQuery("SELECT * FROM stoichiometry "
				+ " INNER JOIN reaction ON stoichiometry.reaction_idreaction = reaction.idreaction " +
				" WHERE source <> 'TRANSPORTERS' AND originalReaction;");

		while (rs.next()) {

			if(reactionsMap.containsKey(rs.getString(2))) {

				String[] ent = new String[4];
				ent[0] = rs.getString(3);
				ent[1] = rs.getString(5);
				ent[2] = rs.getString(6);
				ent[3] = rs.getString(4);
				entry.add(ent);
			}
		}

		reactionsMap.get(rs.getString(2)).put("entry",entry);
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
				+ " WHERE idReaction = "+idReaction+";");

		Map<String, Object> drc = new HashMap<>();

		if (rs.next()) {

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

			List<Pair<String, String>> proteinsPairs = new ArrayList<>();
			rs = statement.executeQuery("SELECT reaction_idreaction, enzyme_protein_idprotein, enzyme_ecnumber  " +
					"FROM reaction_has_enzyme WHERE idReaction = "+idReaction+";");
			while (rs.next())
				proteinsPairs.add(new  Pair<>(rs.getString(2), rs.getString(3)));

			subMap.put("proteins", proteinsPairs);

			List<String> pathways = new ArrayList<>();
			rs = statement.executeQuery("SELECT reaction_idreaction, pathway_idpathway FROM pathway_has_reaction "
					+ " WHERE idReaction = "+idReaction+";");
			while (rs.next())
				pathways.add(rs.getString(2));

			subMap.put("pathways", pathways);

			List<String[]> entry = new ArrayList<>();
			rs = statement.executeQuery("SELECT * FROM stoichiometry "
					+ "INNER JOIN reaction ON stoichiometry.reaction_idreaction = reaction.idreaction " +
					" WHERE idReaction = "+idReaction+";");
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

		return drc;
	}

	/**
	 * Method for retrieving compartments abbreviations from compartments identifiers.
	 * 
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static Map<String,String> getIdCompartmentAbbMap(Statement statement) throws SQLException {

		Map<String,String> idCompartmentMap = new HashMap<String, String>();

		ResultSet rs = statement.executeQuery("SELECT idcompartment, abbreviation FROM compartment;");

		while(rs.next())
			idCompartmentMap.put(rs.getString(1), rs.getString(2));

		return idCompartmentMap;
	}

	/**
	 * Method for retrieving compartments identifiers from compartments abbreviations.
	 * 
	 * @return
	 * @throws SQLException 
	 */
	public static Map<String,String> getCompartmentAbbIdMap(Statement statement) throws SQLException {

		Map<String,String> idCompartmentAbbIdMap = new HashMap<String, String>();

		ResultSet rs = statement.executeQuery("SELECT idcompartment, abbreviation FROM compartment;");

		while(rs.next())
			idCompartmentAbbIdMap.put(rs.getString(2).toLowerCase(),rs.getString(1));

		return idCompartmentAbbIdMap;
	}

	/**
	 * Method for returning existing compartments.
	 * 
	 * @param statement
	 * @return
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
	 * @return
	 * @throws SQLException
	 */
	public static List<String> getEnzymeCompartments(String ecNumber, Statement statement) throws SQLException {

		List<String> compartments = new ArrayList<String>();
		ResultSet rs = statement.executeQuery("SELECT DISTINCT compartment_idcompartment, enzyme_ecnumber, enzyme_protein_idprotein " +
				" FROM subunit " +
				" INNER JOIN gene_has_compartment ON subunit.gene_idgene = gene_has_compartment.gene_idgene " +
				" WHERE BY enzyme_ecnumber = '"+ecNumber+"';");

		while(rs.next())
			compartments.add(rs.getString(1));

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
	 * @return
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
	 * @return
	 * @throws SQLException
	 */
	public static String loadGene(String locusTag, String sequence_id, String geneName, String chromosome, String direction, String left_end, String right_end, Statement statement, DatabaseType databaseType, String informationType) throws SQLException {

		ResultSet rs = statement.executeQuery("SELECT idgene FROM gene WHERE locusTag = '"+locusTag+"' AND sequence_id = '"+sequence_id+"';");

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
			statement.execute("INSERT INTO gene (locusTag, sequence_id,"+aux1+"origin, transcription_direction, left_end_position, right_end_position) VALUES('"+locusTag+"','"+sequence_id+"' "+aux2+",'"+informationType+"','"+direction+"','"+left_end+"','"+right_end+"')");
			rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
			rs.next();

		}
		String geneID = rs.getString(1);
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
	 * @return
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
	public static void updateECNumberNote(Connection conn, String ec_number, int module_id, String note) throws SQLException {

		Statement stmt = conn.createStatement();

		String string = "";
		boolean update = true, notExists = true;

		if(module_id > 0 ) {

			string = ",module_id ="+module_id;

			ResultSet rs = stmt.executeQuery("SELECT module_id FROM subunit WHERE enzyme_ecnumber = '"+ec_number+"'");

			while (rs.next()) {

				if(rs.getInt(1)>0) {

					update=false;

					if(rs.getInt(1)==module_id)
						notExists = false;

				}
			}
		}

		if(update)
			stmt.execute("UPDATE subunit SET note = '"+note+"'" +string +" WHERE enzyme_ecnumber='"+ec_number+"'");
		else
			if(notExists) {

				ResultSet rs = stmt.executeQuery("SELECT DISTINCT gene_idgene, enzyme_protein_idprotein FROM subunit WHERE enzyme_ecnumber = '"+ec_number+"'");

				Set<Pair<String,String>> genes_proteins = new HashSet<Pair<String,String>>();

				while (rs.next()) {

					Pair<String,String> pair = new Pair<>(rs.getString(1), rs.getString(2));

					genes_proteins.add(pair);
				}

				for(Pair<String,String> pair : genes_proteins) {

					stmt.execute("INSERT INTO subunit (gene_idgene, enzyme_protein_idprotein, enzyme_ecnumber, note, module_id) VALUES(" + pair.getA() + ", "+pair.getB() + ", '"+ec_number+"', '"+note+"'," +module_id+")");

				}
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
	public static void updateECNumberStatus(Connection conn, String ec_number, String status) throws SQLException {

		Statement stmt = conn.createStatement();

		stmt.execute("UPDATE subunit SET gpr_status = '"+status+"' WHERE enzyme_ecnumber='"+ec_number+"'");

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
	 * Get locus tag from model. 
	 * 
	 * @param conn
	 * @param query
	 * @return
	 * @throws SQLException 
	 */
	public static List<String> checkDatabase(Connection conn, String query) throws SQLException {

		List<String> ret = new ArrayList<String>();

		Statement stmt = conn.createStatement();

		ResultSet rs = stmt.executeQuery("SELECT locusTag FROM gene " +
				"INNER JOIN gene_has_orthology ON (idgene = gene_idgene)" +
				"INNER JOIN orthology ON (orthology_id = orthology.id)" +
				" WHERE entry_id ='"+query+"'");

		while (rs.next())
			ret.add(rs.getString(1));

		stmt.close();
		stmt=null;
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

		ResultSet rs = stmt.executeQuery("SELECT DISTINCT(enzyme_ecnumber) FROM subunit WHERE gpr_status = 'PROCESSED'");

		while(rs.next())
			ec_numbers.add(rs.getString(1));

		rs.close();
		stmt.close();

		return ec_numbers;
	}

	/**
	 * Load module to model.
	 * 
	 * @param conn
	 * @param result
	 * @throws SQLException
	 */
	public static Map<String, Set<String>> loadModule(Connection conn, Map<String, List<ReactionProteinGeneAssociation>> result) throws SQLException {

		Map<String, Set<String>> genes_ko_modules = new HashMap<>();

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

							String idModule = rs.getString(1);

							for(String gene : geneAssociation.getGenes()) {

								rs = stmt.executeQuery("SELECT * FROM orthology WHERE entry_id='"+gene+"'");

								boolean noEntry = true;
								Set<Integer> ids = new HashSet<>();

								while(rs.next()) {

									noEntry = false;
									ids.add(rs.getInt(1));
								}

								if(noEntry) { 

									stmt.execute("INSERT INTO orthology (entry_id) VALUES('"+gene+"')");
									rs = stmt.executeQuery("SELECT LAST_INSERT_ID();");
									rs.next();
									ids.add(rs.getInt(1));
								}

								for (int idGene : ids) {

									rs = stmt.executeQuery("SELECT * FROM module_has_orthology WHERE module_id="+idModule+" AND orthology_id = "+idGene+"");

									if(!rs.next()) {

										stmt.execute("INSERT INTO module_has_orthology (module_id, orthology_id) VALUES('"+idModule+"', '"+idGene+"')");
										rs = stmt.executeQuery("SELECT LAST_INSERT_ID();");
										rs.next();
									}

									Set<String> modules = new HashSet<>();

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

		ResultSet rs = stmt.executeQuery("SELECT DISTINCT reaction, enzyme_ecnumber, definition, idgene, " +
				" orthology.entry_id, locusTag, gene.name, note, similarity " +
				" FROM module" +
				" INNER JOIN subunit ON (subunit.module_id = module.id)" +
				" INNER JOIN module_has_orthology ON (module_has_orthology.module_id = module.id)"+
				" INNER JOIN orthology ON (module_has_orthology.orthology_id = orthology.id)"+
				" INNER JOIN gene_has_orthology ON (gene_has_orthology.orthology_id = module_has_orthology.orthology_id AND gene_has_orthology.gene_idgene = subunit.gene_idgene)" +
				" INNER JOIN gene ON (gene_has_orthology.gene_idgene = gene.idgene)" 
				//+" WHERE similarity >= "+threshold				
				);

		Map<String, ReactionsGPR_CI> rpgs = new HashMap<>();

		while (rs.next()) {

			if(rs.getString("note")==null || !rs.getString("note").equalsIgnoreCase("unannotated") || (rs.getString("note").equalsIgnoreCase("unannotated") && rs.getDouble("similarity")>=threshold)) {

				ReactionsGPR_CI rpg = new ReactionsGPR_CI(rs.getString(1));

				if(rpgs.containsKey(rs.getString(1)))
					rpg  = rpgs.get(rs.getString(1));

				{
					ProteinsGPR_CI pga = new ProteinsGPR_CI(rs.getString(2), rs.getString(3));
					pga.addSubunit(rs.getString(3).split(" OR "));

					if(rpg.getProteins()!= null && rpg.getProteins().containsKey(rs.getString(2)))
						pga = rpg.getProteins().get(rs.getString(2));

					String geneSurrogateName = rs.getString(6);

					if(rs.getString(7)!=null && !rs.getString(7).isEmpty() && !rs.getString(7).equalsIgnoreCase("null"))
						geneSurrogateName = rs.getString(7)+"_"+geneSurrogateName;

					pga.addLocusTag(rs.getString(5), geneSurrogateName);

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

			statement.execute("INSERT INTO compound (name, kegg_id, entry_type, molecular_weight, hasBiologicalRoles) "
					+ "VALUES ('"+name+"','"+name+"','BIOMASS','"+molecularWeight+"',TRUE);");
			rs = statement.executeQuery("SELECT * FROM compound WHERE name = '"+name+"'");
			rs.next();
		}
		String ret = rs.getString(1);
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

			rs = statement.executeQuery("SELECT idreaction FROM reaction WHERE name = '" + DatabaseUtilities.databaseStrConverter(name, databaseType)+ "'");
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
						+originalReaction+", "+isGeneric+", "+lowerBound+", "+upperBound+",'"+boolean_rule+"')");

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

				int biomass_id = -1;
				rs = statement.executeQuery("SELECT idcompound FROM compound WHERE name LIKE 'Biomass'");
				if(rs.next())
					biomass_id = rs.getInt("idcompound");

				for(String m :metabolitesStoichiometry.keySet()) {

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
			rs.close();
			statement.close();
		}
		catch (SQLException ex) {

			ex.printStackTrace();
		}
	}
	
	

}
