package pt.uminho.sysbio.common.database.connector.databaseAPI.capsules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReactionsCapsule {

	private Map<Integer, String> ids; 
	private HashMap<String,String> namesIndex;
	private Set<String> activeReactions;
	private HashMap<String,String> formulasIndex;
	private List<String> pathwaysList;
	private Map <String, Integer> pathID;
	private Set<String> pathwaysSet;
	private Integer[] tableColumnsSize;
	private ArrayList<Object> reactionsData;
	
	public ReactionsCapsule(Map<Integer, String> ids, HashMap<String,String> namesIndex, Set<String> activeReactions, HashMap<String,String> formulasIndex,
			List<String> pathwaysList, Map <String, Integer> pathID, Set<String> pathwaysSet, Integer[] tableColumnsSize, ArrayList<Object> reactionsData){
		
		this.ids = ids;
		this.namesIndex = namesIndex;
		this.activeReactions = activeReactions;
		this.formulasIndex = formulasIndex;
		this.pathwaysList = pathwaysList;
		this.pathID = pathID;
		this.pathwaysSet = pathwaysSet;
		this.tableColumnsSize = tableColumnsSize;
		this.reactionsData = reactionsData;
	}
	
	public Map<Integer, String> getIds() {
		return ids;
	}

	public void setIds(Map<Integer, String> ids) {
		this.ids = ids;
	}

	public HashMap<String, String> getNamesIndex() {
		return namesIndex;
	}

	public void setNamesIndex(HashMap<String, String> namesIndex) {
		this.namesIndex = namesIndex;
	}

	public Set<String> getActiveReactions() {
		return activeReactions;
	}

	public void setActiveReactions(Set<String> activeReactions) {
		this.activeReactions = activeReactions;
	}

	public HashMap<String, String> getFormulasIndex() {
		return formulasIndex;
	}

	public void setFormulasIndex(HashMap<String, String> formulasIndex) {
		this.formulasIndex = formulasIndex;
	}

	public ArrayList<Object> getReactionsData() {
		return reactionsData;
	}

	public void setReactionsData(ArrayList<Object> reactionsData) {
		this.reactionsData = reactionsData;
	}

	public List<String> getPathwaysList() {
		return pathwaysList;
	}

	public void setPathwaysList(List<String> pathwaysList) {
		this.pathwaysList = pathwaysList;
	}

	public Map<String, Integer> getPathID() {
		return pathID;
	}

	public void setPathID(Map<String, Integer> pathID) {
		this.pathID = pathID;
	}

	public Set<String> getPathwaysSet() {
		return pathwaysSet;
	}

	public void setPathwaysSet(Set<String> pathwaysSet) {
		this.pathwaysSet = pathwaysSet;
	}
	
	public Integer[] getTableColumnsSize() {
		return tableColumnsSize;
	}

	public void setTableColumnsSize(Integer[] tableColumnsSize) {
		this.tableColumnsSize = tableColumnsSize;
	}
}
