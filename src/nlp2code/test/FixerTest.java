package nlp2code.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

import nlp2code.*;
import nlp2code.compiler.*;

import org.junit.Test;

public class FixerTest {

	@Test
	public void testFileSystem() {		
		//upgraded ecj_fix.jar to fix bug on this (only occurred with testing):
		new EclipseCompiler().getStandardFileManager(null, null, null);
		return;
	}
	
	@Test
	public void testDeletion() {
		List<Snippet> snippets = new ArrayList<Snippet>();
		String before = "class Main{\n";
		String after = "}\n";
		
		Evaluator.setupOptions(null, false);
		Snippet s = new Snippet("public int i\npublic int j;\n", 0);
		s.updateErrors(1, null);
		snippets.add(s);
		
		s = Fixer.deletion(snippets.get(0), before, after);
		System.out.println(s.getFormattedCode());
	}

}
