package nlp3code.tests.unittests2;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import nlp3code.DocHandler;
import nlp3code.Evaluator;
import nlp3code.InputHandler;
import nlp3code.code.Snippet;
import nlp3code.tester.TypeHandler;

/**
 * Tests the source code tester.
 * Run this as a plug-in test with an open workspace, project and file:
 * https://stackoverflow.com/questions/27088926/run-eclipse-plug-in-test-with-included-workspace-projects
 */
public class TesterTests {
	String before = "class Main{\npublic static void main(String args[]) {\n";
	String after = "}\n}\n";
	
	/**
	 * A basic case, convert string to int.
	 */
	@Test
	public void stringToIntTest() {
		String code = "String s = \"1\";\nint i = Integer.parseInt(s);\n";
		Snippet snippet = new Snippet(code, 0);
		snippet.updateErrors(0, null);
		DocHandler.setFileName("Test.java");
		InputHandler.before = before;
		InputHandler.after = after;
		
		String test = "assertEquals(i, 1);\n";
		//String function = TypeHandler.functionStart+test+TypeHandler.functionEnd;
		List<Snippet> snippets = new ArrayList<Snippet>();
		snippets.add(snippet);
		
		Evaluator.testSnippets(null, snippets, before, after, test, null);
	}
}
