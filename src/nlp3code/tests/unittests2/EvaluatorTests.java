package nlp3code.tests.unittests2;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import nlp3code.DataHandler;
import nlp3code.DocHandler;
import nlp3code.Evaluator;
import nlp3code.InputHandler;
import nlp3code.Searcher;
import nlp3code.code.Snippet;
import nlp3code.compiler.IMCompiler;

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
}
