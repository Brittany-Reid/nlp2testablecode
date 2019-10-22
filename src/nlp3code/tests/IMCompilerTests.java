package nlp3code.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nlp3code.Evaluator;
import nlp3code.compiler.IMCompiler;

/**
 * Due to ecj mess, ensure the JUnit classpath loads jdt.core and ecj before plug-in dependencies.
 */
public class IMCompilerTests {
	
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
