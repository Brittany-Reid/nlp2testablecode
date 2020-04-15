package nlp3code.tests.unittests;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import nlp3code.DocHandler;
import nlp3code.Evaluator;
import nlp3code.InputHandler;
import nlp3code.code.Snippet;

public class EvaluatorTests {
	String before = "class Main{\npublic static void main(String args[]) {\n";
	String after = "}\n}\n";
	
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
		
		String test = "assertEquals(test(\"1\"), 1);\n";
		//String function = TypeHandler.functionStart+test+TypeHandler.functionEnd;
		List<Snippet> snippets = new ArrayList<Snippet>();
		snippets.add(snippet);
		
		Evaluator.testSnippets(null, snippets, before, after, test, null, types);
		
		//must pass
		assertEquals(1, Evaluator.passed);
	}

}
