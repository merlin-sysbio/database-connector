package pt.uminho.sysbio.common.database.connector.datatypes;

import java.io.Serializable;

public class Enumerators {

	
	public enum DatabaseType implements Serializable {
		
		H2,
		MYSQL
	}
	
	
	public enum ModuleType {

		Complex,
		Pathway
	}
	
}
