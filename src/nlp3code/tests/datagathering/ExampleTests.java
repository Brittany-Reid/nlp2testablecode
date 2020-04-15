package nlp3code.tests.datagathering;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nlp3code.Activator;
import nlp3code.Evaluator;
import nlp3code.InputHandler;
import nlp3code.code.Snippet;
import nlp3code.tests.TestEnvironment;

/**
 * Tests for the paper example.
 */
public class ExampleTests {
	static String before = TestEnvironment.before;
	static String after = TestEnvironment.after;
	
	/**
	 * Before each test.
	 */
	@Before
	public void setUp() throws Exception {
		//setup workspace
		TestEnvironment.setupWorkspace();
		TestEnvironment.addLibrary("lib/guava-28.2-jre.jar");
	}
	
	/**
	 * After each test.
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception{
		TestEnvironment.cleanWorkspace();
	}

	/**
	 * Test automatic integration.
	 */
	@Test
	public void integrationExample() {
		Activator.useProjectPackages = true;
		Evaluator.compiler = Evaluator.initializeCompiler(false);
		InputHandler.insertionContext = InputHandler.MAIN;
		
		String code = "import com.google.common.primitives.Ints;\nimport java.util.Optional;\nint foo = 0;\n foo = Optional.ofNullable(myString).map(Ints::tryParse).orElse(0);\n";
		
		List<Snippet> snippets = new ArrayList<Snippet>();
		snippets.add(new Snippet(code, 0));
		snippets = Evaluator.evaluate(null, snippets, before, after);
		
		System.out.print(Snippet.insertFormatted(snippets.get(0), before+after, before.length()));
	}
	
}
