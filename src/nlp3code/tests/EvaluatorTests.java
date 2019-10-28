package nlp3code.tests;

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
	
	//data tests
	
	//@Test
	public void testIntegration(){
		Evaluator.compiler = Evaluator.initializeCompiler(false);
		IMCompiler compiler = Evaluator.compiler;
		DocHandler.setFileName("Main.java");
		InputHandler.insertionContext = InputHandler.MAIN;
		System.out.println("INTEGRATION:");
		DataHandler.limit = 999999999L;
		DataHandler.processing = DataHandler.LEMMATIZE;
		DataHandler.clear();
		DataHandler.loadStopWords();
		DataHandler.loadTasks(null);
		DataHandler.loadQuestions(null);
		DataHandler.loadAnswers(null);
		int compiling = 0;
		int errors = 0;
		long start = System.currentTimeMillis();
		for(String query : DataHandler.queries) {
			List<Snippet> snippets = Searcher.getSnippets(query);
			int taskC = 0;
			int taskE = 0;
			Evaluator.targetted = false;
			Evaluator.deletion = false;
			Evaluator.integrate = true;
			snippets = Evaluator.evaluate(null, snippets, before, after);
			taskC = Evaluator.compiled;
			for(Snippet snippet : snippets) {
				taskE += snippet.getErrors();
			}
			errors += taskE;
			System.out.print("TASK: " + query + ", " + taskC+ ", " + taskE + "\n");
			compiling += taskC;
		}
		long end = System.currentTimeMillis() - start;
		System.out.print("TIME: " + end + "ms\n");
		DataHandler.processing = DataHandler.STEM;
		System.out.print("TOTAL: " + compiling +", " + errors + "\n");
		Evaluator.targetted = true;
		Evaluator.deletion = true;
		Evaluator.integrate = true;
	}
	
	//the others require ui elements :( 
	
	//function tests
	
	//if deletion gives empty snippets, these dont count as compilable
	@Test
	public void testEmptyLines(){
		Evaluator.compiler = Evaluator.initializeCompiler(false);
		IMCompiler compiler = Evaluator.compiler;
		DocHandler.setFileName("Main.java");
		InputHandler.insertionContext = InputHandler.MAIN;

		int compiling = 0;
		int errors = 0;
		long start = System.currentTimeMillis();
		
		List<Snippet> snippets = new ArrayList<Snippet>();
		snippets.add(new Snippet("int i=0\n", 0));
		Evaluator.targetted = false;
		Evaluator.deletion = true;
		Evaluator.integrate = false;
		snippets = Evaluator.evaluate(null, snippets, before, after);
		compiling = Evaluator.compiled;
		long end = System.currentTimeMillis() - start;
		
		DataHandler.processing = DataHandler.STEM;
		Evaluator.targetted = true;
		Evaluator.deletion = true;
		Evaluator.integrate = true;
		
		assertEquals(0, compiling);
	}
}
