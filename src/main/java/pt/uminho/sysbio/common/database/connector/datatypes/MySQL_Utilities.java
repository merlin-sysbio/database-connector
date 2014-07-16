/**
 * 
 */
package pt.uminho.sysbio.common.database.connector.datatypes;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author oscar
 *
 */
public class MySQL_Utilities {
	
	private static Map<String, String> sqlTokens;
	private static Pattern sqlTokenPattern;


	/**
	 * @param boolean_string
	 * @return
	 */
	public static int get_boolean_int(String boolean_string){
		if(boolean_string.equalsIgnoreCase("true"))
		{
			return 1;
		}
		else if(boolean_string.equalsIgnoreCase("false"))
		{
			return 0;
		}
		return -1;
	}

	/**
	 * @param boolean_int
	 * @return
	 */
	public static boolean get_boolean_int_to_boolean(String boolean_int){

		if(boolean_int.equalsIgnoreCase("1"))
		{
			return true;
		}
		return false;
	}

	/**
	 * @param boolean_int
	 * @return
	 */
	public static boolean get_boolean_string_to_boolean(String boolean_string){

		if(boolean_string.equalsIgnoreCase("true"))
		{
			return true;
		}
		return false;
	}
	
//	/**
//	 * @param input
//	 * @return
//	 */
//	public static String mysqlStrConverter(String input) {
//	
//		if(input == null) {
//
//			return null;
//		} 
//		else {
//			
//			return input.replace("\\'","'").replace("\\","\\\\").replace("'","\\").replace("-","\\-").replace("/","\\/");
//		}
//	}
	
	static
	{           
        //MySQL escape sequences: http://dev.mysql.com/doc/refman/5.1/en/string-syntax.html
        String[][] search_regex_replacement = new String[][]
                {
                //search string     search regex        sql replacement regex
                {   "\u0000"    ,       "\\x00"     ,       "\\\\0"     },
                {   "'"         ,       "'"         ,       "\\\\'"     },
                {   "\""        ,       "\""        ,       "\\\\\""    },
                {   "\b"        ,       "\\x08"     ,       "\\\\b"     },
                {   "\n"        ,       "\\n"       ,       "\\\\n"     },
                {   "\r"        ,       "\\r"       ,       "\\\\r"     },
                {   "\t"        ,       "\\t"       ,       "\\\\t"     },
                {   "\u001A"    ,       "\\x1A"     ,       "\\\\Z"     },
                {   "\\"        ,       "\\\\"      ,       "\\\\\\\\"  }
                };

        sqlTokens = new HashMap<>();
        String patternStr = "";

        for (String[] srr : search_regex_replacement)
        {
            sqlTokens.put(srr[0], srr[2]);
            patternStr += (patternStr.isEmpty() ? "" : "|") + srr[1];            
        }
        sqlTokenPattern = Pattern.compile('(' + patternStr + ')');
    }


    //public static String escape(String s) {
    public static String mysqlStrConverter(String s) {

        //System.out.println(s);
        
        if(s!=null) {

            Matcher matcher = sqlTokenPattern.matcher(s);
            StringBuffer sb = new StringBuffer();
            while(matcher.find())
            {
                matcher.appendReplacement(sb, sqlTokens.get(matcher.group(1)));
            }
            matcher.appendTail(sb);
            //System.out.println(sb.toString());
            s = sb.toString();
        }
        return s;
    }


}
