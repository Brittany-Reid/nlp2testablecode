package nlp3code.tests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import nlp3code.DataHandler;
import nlp3code.Evaluator;
import nlp3code.Searcher;
import nlp3code.code.Snippet;
import nlp3code.compiler.IMCompiler;

/**
 * Due to ecj mess, ensure the JUnit classpath loads jdt.core and ecj before plug-in dependencies.
 */
public class IMCompilerTests {
	String before = "class Main{\npublic static void main(String args[]) {\n";
	String after = "}\n}\n";
	
	//data tests
	@Test
	public void numberOfCompiling() {
		Evaluator.compiler = Evaluator.initializeCompiler(false);
		IMCompiler compiler = Evaluator.compiler;
		System.out.println("LEMMA: ");
		DataHandler.limit = 9999999999L;
		DataHandler.processing = DataHandler.LEMMATIZE;
		DataHandler.clear();
		DataHandler.loadStopWords();
		DataHandler.loadTasks(null);
		DataHandler.loadQuestions(null);
		DataHandler.loadAnswers(null);
		int compiling = 0;
		long start = System.currentTimeMillis();
		for(String query : DataHandler.queries) {
			List<Snippet> snippets = Searcher.getSnippets(query);
			int taskC = 0;
			for(Snippet snippet : snippets) {
				compiler.clearSaved();
				compiler.addSource("Main", Snippet.insert(snippet, before+after, before.length()));
				compiler.compileAll();
				if(compiler.getErrors() == 0) {
					taskC++;
					compiling++;
				}
			}
			System.out.print("TASK: " + query + ", " + taskC+"\n");
		}
		long end = System.currentTimeMillis() - start;
		System.out.print("TIME: " + end + "ms\n");
		DataHandler.processing = DataHandler.STEM;
		System.out.print("TOTAL: " + compiling +"\n");
	}
	
	
	//function tests
	
	@Test
	public void testCompilerPatch(){
		Evaluator.compiler = Evaluator.initializeCompiler(false);
		IMCompiler compiler = Evaluator.compiler;
		compiler.clearSaved();
		compiler.addSource("Main", "class Main{\nint i = 0;\n}\n");
		compiler.compileAll();
		int errors = compiler.getErrors();
		assertEquals(0, errors);
	}
	
	@Test
	public void testCompilerErrors(){
		Evaluator.compiler = Evaluator.initializeCompiler(false);
		IMCompiler compiler = Evaluator.compiler;
		compiler.clearSaved();
		compiler.addSource("Main", "class Main{\nint i = 0\n}\n");
		compiler.compileAll();
		int errors = compiler.getErrors();
		assertEquals(1, errors);
	}
	
	@Test
	public void testJUnit() {
		Evaluator.compiler = Evaluator.initializeCompiler(false);
		IMCompiler compiler = Evaluator.compiler;
		compiler.clearSaved();
		String code = "import static org.junit.Assert.*;\nimport org.junit.Test;\npublic class Tests{\n@Test\npublic void test() {\n}\n}\n";
		compiler.addSource("Tests", code);
		compiler.compileAll();
		int errors = compiler.getErrors();
		assertEquals(0, errors);
	}
	
	@Test
	public void testSystemClasses() {
		Evaluator.compiler = Evaluator.initializeCompiler(false);
		IMCompiler compiler = Evaluator.compiler;
		compiler.clearSaved();
		String code = "package nlp3code;\nimport nlp3code.code.Snippet;\nclass Main{\n public void test() {\nSnippet snippet;\n}\n}\n";
		compiler.addSource("Main", code);
		compiler.compileAll();
		int errors = compiler.getErrors();
		assertEquals(0, errors);
	}
}
