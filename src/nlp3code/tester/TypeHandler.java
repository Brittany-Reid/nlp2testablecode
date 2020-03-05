package nlp3code.tester;

import java.util.ArrayList;
import java.util.List;

import nlp3code.DocHandler;
import nlp3code.InputHandler;
import nlp3code.listeners.QueryDocListener;
import nlp3code.recommenders.TaskRecommender;
import nlp3code.recommenders.TypeRecommender;

public class TypeHandler {
	public static String whitespaceBefore = null;
	public static String functionStart = "\r\n\r\n	@Test\r\n	public void nlp3code_test(){\r\n		//--START EDITING\r\n";
	public static String functionEnd = "		//--END EDITING\r\n	}";
	private static ArrayList<String> imports = null;
	
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
	
//	/**
//	 * Constructs a test from the given type input on String line.
//	 */
//	public static int constructTest(int lineOffset, String line) {
//
//		//get whitespace
//		whitespaceBefore = line.substring(0, line.indexOf(line.trim()));
//		
//		String query = getQuery(line, TypeRecommender.testChar);
//		if (query.length() == 0) return -1;
//		
//		int lineNum = DocHandler.getLineOfOffset(lineOffset);
//		int offset = DocHandler.getLineOffset(lineNum);
//		int length = DocHandler.getLineLength(lineNum);
//		if (offset < 0 || offset > QueryDocListener.currentDocument.getLength()) return -1;
//		if (length > QueryDocListener.currentDocument.getLength() || offset + length > QueryDocListener.currentDocument.getLength()) return -1;
//		
//		//do test
//		String test = Tester.generateTestCase(line);
//		if(test == null) return -1;
//		String function = functionStart+test+functionEnd;
//		//System.out.println(test);
//		
//		//remove query
//		DocHandler.replace("", offset, length);
//		
//		DocHandler.addFunction(function);
//		
//		//add junit imports
//		if(imports == null) {
//			imports = new ArrayList<String>();
//			imports.add("import static org.junit.Assert.*;");
//			imports.add("import org.junit.Test;");
//		}
//		DocHandler.addImportStatements(imports);
//		
//		return 0;
//	}
	
	/**
	 * Function that extracts query from line.
	 * @param line The line to extract query from.
	 * @return A string query.
	 */
	public static String getQuery(String line, String queryChar) {
		String query;
		
		//trim whitespace
		query = line.trim();
		query = query.toLowerCase();
		
		//extract
		if (query.endsWith(queryChar)) query = query.substring(0, query.length()-1);
		if (query.startsWith(queryChar)) query = query.substring(1);
		
		//trim any whitespace between question mark
		query = query.trim();
		
		//if there are any invalid characters, return empty
		if (!query.matches("[abcdefghijklmnopqrstuvwxyz,<>\\]\\[ ]*")) return "";
		
		return query;
	}
}
