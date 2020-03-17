package nlp3code.tests.datagathering;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import nlp3code.DataHandler;
import nlp3code.DocHandler;
import nlp3code.Evaluator;
import nlp3code.InputHandler;
import nlp3code.Searcher;
import nlp3code.code.Snippet;
import nlp3code.fixer.Deleter;
import nlp3code.recommenders.TypeRecommender;
import nlp3code.tester.TypeSuggestions;
import nlp3code.tests.TestEnvironment;


/**
 * This set of tests gathers results of the plug-in. All data tests from other files should be moved here.
 * Run this as a plug-in test with an open workspace, project and file:
 * https://stackoverflow.com/questions/27088926/run-eclipse-plug-in-test-with-included-workspace-projects
 */
public class ResultsTests {
	static String before = TestEnvironment.before;
	static String after = TestEnvironment.after;
	
	//setup for all tests
	@BeforeClass
	public static void setup() throws Exception{
		//reset db
		DataHandler.clear();
		
		//DataHandler.limit = 10000L; //reduce db size
		Evaluator.compiler = Evaluator.initializeCompiler(false);
		DocHandler.setFileName("Main");
		InputHandler.insertionContext = InputHandler.MAIN;
		DataHandler.processing = DataHandler.LEMMATIZE; //use lemma
		InputHandler.before = before;
		InputHandler.after = after;
		
		//load database
		DataHandler.loadStopWords();
		DataHandler.loadTasks(null);
		DataHandler.loadQuestions(null);
		DataHandler.loadAnswers(null);
	}
	
	/**
	 * Before each test.
	 */
	@Before
	public void setUp() throws Exception {
		//setup workspace
		TestEnvironment.setupWorkspace();
	}
	
	/**
	 * After each test.
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception{
		Evaluator.targetted = true;
		Evaluator.deletion = true;
		Evaluator.integrate = true;
		Deleter.setOptions(true, true, true);
		
		TestEnvironment.cleanWorkspace();
	}
	
	/**
	 * How many snippets compile with no fixes.
	 */
	//@Test
	public void testCompiling(){
		int compiling = 0;
		int errors = 0;
		Evaluator.targetted = false;
		Evaluator.deletion = false;
		Evaluator.integrate = false;
		
		System.out.println("Compiling:");
		
		//begin timing
		long start = System.currentTimeMillis();
		for(String query : DataHandler.queries) {
			List<Snippet> snippets = Searcher.getSnippets(query);
			int taskC = 0;
			int taskE = 0;
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
		System.out.print("TOTAL: " + compiling +", " + errors + "\n");
	}
	
	/**
	 * How many snippets compile after integration is completed.
	 */
	//@Test
	public void testIntegration(){
		int compiling = 0;
		int errors = 0;
		Evaluator.integrate = true;
		Evaluator.targetted = false;
		Evaluator.deletion = false;
		
		System.out.println("Integrating:");
		
		//begin timing
		long start = System.currentTimeMillis();
		for(String query : DataHandler.queries) {
			List<Snippet> snippets = Searcher.getSnippets(query);
			int taskC = 0;
			int taskE = 0;
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
		System.out.print("TOTAL: " + compiling +", " + errors + "\n");
	}
	
	
	/**
	 * How many snippets compile after targeted fixes are applied.
	 */
	//@Test
	public void testFixing(){
		int compiling = 0;
		int errors = 0;
		Evaluator.targetted = true;
		Evaluator.deletion = false;
		Evaluator.integrate = true;
		
		System.out.println("FIXING:");
		
		//begin timing
		long start = System.currentTimeMillis();
		for(String query : DataHandler.queries) {
			List<Snippet> snippets = Searcher.getSnippets(query);
			int taskC = 0;
			int taskE = 0;
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
		System.out.print("TOTAL: " + compiling +", " + errors + "\n");
	}
	
	
	/**
	 * Test different deletion algorithms.
	 */
	@Test
	public void deletionTests() {
		Evaluator.integrate = true;
		Evaluator.targetted = true;
		Evaluator.deletion = true;
		long start;
		
		for(int i=0; i<8; i++) {
			//set deletion options
			if(i==0){
				System.out.println("SETTING: DESC, STRICT, SINGLE\n");
				Deleter.setOptions(false, false, false);
			}
			if(i==1){
				System.out.println("SETTING: ASC, STRICT, SINGLE\n");
				Deleter.setOptions(true, false, false);
			}
			if(i==2){
				System.out.println("SETTING: DESC, NONSTRICT, SINGLE\n");
				Deleter.setOptions(false, true, false);
			}
			if(i==3){
				System.out.println("SETTING: ASC, NONSTRICT, SINGLE\n");
				Deleter.setOptions(true, true, false);
			}
			if(i==4){
				System.out.println("SETTING: DESC, STRICT, LOOP\n");
				Deleter.setOptions(false, false, true);
			}
			if(i==5){
				System.out.println("SETTING: ASC, NONSTRICT, LOOP\n");
				Deleter.setOptions(true, true, true);
			}
			if(i==6){
				System.out.println("SETTING: ASC, STRICT, LOOP\n");
				Deleter.setOptions(true, false, true);
			}
			if(i==7){
				System.out.println("SETTING: DESC, NONSTRICT, LOOP\n");
				Deleter.setOptions(false, true, true);
			}
			
			//get results for each task
			start = System.currentTimeMillis();
			int compiled;
			int totalCompiled = 0;
			for(String task : DataHandler.queries) {
				compiled = 0;
				
				//find the snippets
				List<Snippet> snippets;
				snippets = Searcher.getSnippets(task);
				if(snippets == null) continue;
				
				//call evaluator
				snippets = Evaluator.evaluate(null, snippets, before, after);
			 
				//check errors
				for(Snippet snippet : snippets) {
					
					if(snippet.getErrors() == 0) {
						compiled++;
					}
				}
				//System.out.println("TASK: " + task + ", " + compiled + "\n"); //for per task breakdown
				totalCompiled += compiled;
				
			}
			
			System.out.println("TIME: " + (System.currentTimeMillis() - start) + "ms\n");
			System.out.println("TOTAL: " + totalCompiled + "\n");
		}
	}
	
	/**
	 * How many snippets compile after the best performing line deletion algorithm is applied. 
	 * This is the final number of compilable snippets.
	 */
	//@Test
	public void testFinalDeletion(){
		int compiling = 0;
		int errors = 0;
		Evaluator.integrate = true;
		Evaluator.targetted = true;
		Evaluator.deletion = true;
		
		System.out.println("Deletion:");
		
		//begin timing
		long start = System.currentTimeMillis();
		for(String query : DataHandler.queries) {
			List<Snippet> snippets = Searcher.getSnippets(query);
			int taskC = 0;
			int taskE = 0;
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
		System.out.print("TOTAL: " + compiling +", " + errors + "\n");
	}
	
	/**
	 * Tests type recommendations.
	 */
	//@Test
	public void testTypeRecommendations() {
		Evaluator.integrate = true;
		Evaluator.targetted = true;
		Evaluator.deletion = true;
		
		System.out.println("Type Recommendations:");
		
		//begin timing
		long start = System.currentTimeMillis();
		for(String query : DataHandler.queries) {
			List<Snippet> snippets = Searcher.getSnippets(query);
			snippets = Evaluator.evaluate(null, snippets, before, after);
			System.out.print("TASK: " + query + "\n");
			
			//get type suggestions
			snippets = TypeSuggestions.getTypeSuggestions(snippets, before, after, null);
			
			
			List<String> types = TypeRecommender.sortIOTypes(snippets);
			for(String type : types) {
				System.out.print("TYPE: " + type + "\n");
			}
		}
		long end = System.currentTimeMillis() - start;
		System.out.print("TIME: " + end + "ms\n");
	}
	
	/**
	 * Run tests for 47 tasks.
	 */
	//@Test
	public void testing() {
		Evaluator.integrate = true;
		Evaluator.targetted = true;
		Evaluator.deletion = true;
		
		System.out.println("Testing:");
		
		long start = System.currentTimeMillis();
		for(String query : DataHandler.queries) {
			List<Snippet> snippets = Searcher.getSnippets(query);
			snippets = Evaluator.evaluate(null, snippets, before, after);
			System.out.print("TASK: " + query + "\n");
			List<String> types = new ArrayList<>();
			String test = null;
			
			switch(query) {
				case "switch statement":
					types.add("int");
					types.add("int");
					test = "assertEquals(3, test(3));";
					break;
				case "get index of substring":
					types.add("int");
					types.add("String");
					types.add("String");
					test = "assertEquals(0, test(\"a\", \"abc\"));";
					break;
				case "check equality for strings":
					types.add("boolean");
					types.add("String");
					types.add("String");
					test = "assertEquals(true, test(\"a\", \"a\"));";
					break;
				case "add for loop":
					types.add("int");
					types.add("int");
					types.add("int");
					test = "assertEquals(10, test(0, 10));";
					break;
				case "split string by whitespaces":
					types.add("String[]");
					types.add("String");
					test = "assertEquals(new String[] {\"a\", \"b\"};, test(\"a b\"));";
					break;
				case "convert string to integer":
					types.add("Integer");
					types.add("String");
					test = "assertEquals(10, test(\"10\"));";
					break;
				default:
					break;
			}
			
			snippets = Evaluator.testSnippets(null, snippets, before, after, test, null, types);
			
			System.out.println("PASSED: " + Evaluator.passed);
			
			
		}
		long end = System.currentTimeMillis() - start;
		System.out.print("TIME: " + end + "ms\n");
	}
	
}
