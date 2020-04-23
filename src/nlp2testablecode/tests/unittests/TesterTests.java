package nlp2testablecode.tests.unittests;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nlp2testablecode.Activator;
import nlp2testablecode.DocHandler;
import nlp2testablecode.InputHandler;
import nlp2testablecode.code.Snippet;
import nlp2testablecode.tester.Tester;

/**
 * Tests the code snippet tester.
 */
public class TesterTests {
	String before = "public class Main{\npublic static void main(String args[]) {\n";
	String after = "}\n}\n";
	
	@Before
	public void setUp() {
		Activator.testing = true;
	}
	
	@After
	public void tearDown() {
		Activator.testing = false;
	}
	
	/**
	 * Given a test that will run infinitely, ensure timeout works correctly.
	 * Fails if test takes longer than 15 seconds (should timeout in 10).
	 * If this fails a process may be hanging in the background!
	 * (If the timeout < 10s, JUnit does not properly end the process.)
	 */
	@Test(timeout=15000)
	public void timeoutTest() {
		Snippet snippet = new Snippet("int a = 0;\r\n" + 
				"		boolean test = true;\r\n" + 
				"		while(test == true) {\r\n" + 
				"			System.out.println(\"evil loop of death\");\r\n" + 
				"		}\r\n" + 
				"		int b = 0;\r\n" + 
				"		", 0);
		
		List<String> types = new ArrayList<>();
		types.add("int");
		types.add("int");
		
		String test = "assertEquals(0, test(0));";
		
		//setup
		snippet.updateErrors(0, null);
		DocHandler.setFileName("Main.java");
		InputHandler.before = before;
		InputHandler.after = after;
		
		//test
		Tester.test(snippet, before, after, test, null, types);
	}
	
	/**
	 * A basic case, convert string to int.
	 */
	//@Test
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
		
		//Evaluator.testSnippets(null, snippets, before, after, test, null);
	}
}
