package nlp3code.tester;

import java.util.ArrayList;

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
	/**
	 * Constructs a test from the given type input on String line.
	 */
	public static int constructTest(int lineOffset, String line) {

		//get whitespace
		whitespaceBefore = line.substring(0, line.indexOf(line.trim()));
		
		String query = InputHandler.getQuery(line, TypeRecommender.testChar);
		if (query.length() == 0) return -1;
		
		int lineNum = DocHandler.getLineOfOffset(lineOffset);
		int offset = DocHandler.getLineOffset(lineNum);
		int length = DocHandler.getLineLength(lineNum);
		if (offset < 0 || offset > QueryDocListener.currentDocument.getLength()) return -1;
		if (length > QueryDocListener.currentDocument.getLength() || offset + length > QueryDocListener.currentDocument.getLength()) return -1;
		
		//do test
		String test = Tester.generateTestCase(line);
		String function = functionStart+test+functionEnd;
		//System.out.println(test);
		
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
		
		return 0;
	}
}
