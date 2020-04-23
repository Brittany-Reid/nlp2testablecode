package nlp2testablecode.tester;

import java.util.ArrayList;
import java.util.List;

import nlp2testablecode.DocHandler;
import nlp2testablecode.listeners.QueryDocListener;
import nlp2testablecode.recommenders.TypeRecommender;

/**
 * Processes type information from type query.
 */
public class TypeHandler {
	public static String whitespaceBefore = null;
	private static ArrayList<String> imports = null;
	
	/**
	 * Adds test function to file with given name, based on the type query written in line.
	 * @param lineOffset The offset for the line to calculate whitespace.
	 * @param line The query line.
	 * @param name	Function name.
	 * @return A list of types.
	 */
	public static List<String> addTestFunction(int lineOffset, String line, String name) {
		List<String> types = null;
		
		//get whitespace
		whitespaceBefore = line.substring(0, line.indexOf(line.trim()));
		
		String query = getQuery(line, TypeRecommender.testChar);
		if (query.length() == 0) return null;
		
		int lineNum = DocHandler.getLineOfOffset(lineOffset);
		int offset = DocHandler.getLineOffset(lineNum);
		int length = DocHandler.getLineLength(lineNum);
		if (offset < 0 || offset > QueryDocListener.currentDocument.getLength()) return null;
		if (length > QueryDocListener.currentDocument.getLength() || offset + length > QueryDocListener.currentDocument.getLength()) return null;
		
		types = TestFunctionGenerator.getTypeList(query);
		String function = TestFunctionGenerator.generateTestFunction(types, name).toString();
		
		//remove query
		DocHandler.replace("", offset, length);
		
		DocHandler.addFunction(function);
		
		//add junit imports
		if(imports == null) {
			imports = new ArrayList<String>();
			imports.add("import static org.junit.Assert.*;");
			imports.add("import org.junit.Test;");
		}
		DocHandler.addImportStatements(imports);
		
		return types;
	}
	
	/**
	 * Function that extracts query from line.
	 * @param line The line to extract query from.
	 * @return A string query.
	 */
	public static String getQuery(String line, String queryChar) {
		String query;
		
		//trim whitespace
		query = line.trim();
		
		//extract
		if (query.endsWith(queryChar)) query = query.substring(0, query.length()-1);
		if (query.startsWith(queryChar)) query = query.substring(1);
		
		//trim any whitespace between query char
		query = query.trim();
		
		//if there are any invalid characters, return empty
		if (!query.matches("[aAbBcCdDeEfgGhHiIjJkKlLmMnNoOpPqQrRsStTuUvVwWxXyYzZ,<>\\]\\[ ]*")) {
			return "";
		}
		
		return query;
	}
}
