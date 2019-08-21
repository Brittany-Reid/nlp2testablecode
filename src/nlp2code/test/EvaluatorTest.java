package nlp2code.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nlp2code.*;

import org.junit.Test;

public class EvaluatorTest {

	@Test
	public void testCompileSnippets() {
		List<Snippet> snippets = new ArrayList<Snippet>();
		String before = "class Main{\n";
		String after = "}\n";
		
		Evaluator.setupOptions(null, false);
		
		snippets.add(new Snippet("public int i\n", 0));
		snippets.add(new Snippet("public int i;\n", 0));
		Evaluator.compileSnippets(snippets, before, after);
		assertEquals(snippets.get(0).getErrors(), 1);
	}

}
