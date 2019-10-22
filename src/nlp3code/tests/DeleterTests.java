package nlp3code.tests;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import nlp3code.DocHandler;
import nlp3code.code.Snippet;
import nlp3code.fixer.Deleter;

public class DeleterTests {
	String before = "class Main{\npublic static void main(String args[]) {\n";
	String after = "}\n}\n";
	
	@Test
	public void testDeletion(){
		//our code, with a line missing a semicolon
		String code = "int i=0;\nint b = 0\n";
		Snippet snippet = new Snippet(code, 0);
		snippet.updateErrors(1, null);
		DocHandler.setFileName("Test.java");
		snippet = Deleter.deletion(snippet, before, after);
		assertTrue(snippet.isDeleted(2));
	}
}
