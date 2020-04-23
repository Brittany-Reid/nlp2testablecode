package nlp2testablecode.tests.unittests;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nlp2testablecode.Activator;
import nlp2testablecode.DocHandler;
import nlp2testablecode.Evaluator;
import nlp2testablecode.InputHandler;
import nlp2testablecode.code.Snippet;
import nlp2testablecode.tester.Tester;

public class EvaluatorTests {
	String before = "class Main{\npublic static void main(String args[]) {\n";
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
	 * If deletion gives empty snippets, these dont count as compilable.
	 */
	@Test
	public void testEmptyLines(){
		
		Evaluator.compiler = Evaluator.initializeCompiler(false);
		DocHandler.setFileName("Main.java");
		InputHandler.insertionContext = InputHandler.MAIN;

		int compiling = 0;
		
		List<Snippet> snippets = new ArrayList<Snippet>();
		snippets.add(new Snippet("int i=0\n", 0));
		Evaluator.targetted = false;
		Evaluator.deletion = true;
		Evaluator.integrate = false;
		snippets = Evaluator.evaluate(null, snippets, before, after);
		compiling = Evaluator.compiled;
		
		Evaluator.targetted = true;
		Evaluator.deletion = true;
		Evaluator.integrate = true;
		
		assertEquals(0, compiling);
	}
	
	/**
	 * A basic case, convert string to int.
	 */
	@Test
	public void testBulkTesting() {
		String code = "String s = \"1\";\nint i = Integer.parseInt(s);\n";
		Snippet snippet = new Snippet(code, 0);
		snippet.updateErrors(0, null);
		DocHandler.setFileName("Test.java");
		InputHandler.before = before;
		InputHandler.after = after;
		List<String> types = new ArrayList<String>();
		types.add("int");
		types.add("String");
		
		String test = "assertEquals(" + Tester.FUNCTIONNAME + "(\"1\"), 1);\n";
		//String function = TypeHandler.functionStart+test+TypeHandler.functionEnd;
		List<Snippet> snippets = new ArrayList<Snippet>();
		snippets.add(snippet);
		
		Evaluator.testSnippets(null, snippets, before, after, test, null, types);
		
		//must pass
		assertEquals(1, Evaluator.passed);
	}

}
