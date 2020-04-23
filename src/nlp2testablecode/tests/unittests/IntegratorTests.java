package nlp2testablecode.tests.unittests;

import org.junit.Test;

import nlp2testablecode.DocHandler;
import nlp2testablecode.code.Snippet;
import nlp2testablecode.fixer.Integrator;

public class IntegratorTests {
	String before = "class Main{\npublic static void main(String args[]) {\n";
	String after = "}\n}\n";
	
	@Test
	public void testMainInMain(){
		//our code, with a line missing a semicolon
		String code = "public static void main(String[] args) {\nint i=0;\nint b = 0;\n}\n";
		Snippet snippet = new Snippet(code, 0);
		snippet.updateErrors(1, null);
		DocHandler.setFileName("Test.java");
		snippet = Integrator.integrate(snippet, before, after);
		//System.out.println(snippet.getCode());
	}
}
