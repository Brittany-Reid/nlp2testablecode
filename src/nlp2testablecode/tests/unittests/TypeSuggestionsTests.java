package nlp2testablecode.tests.unittests;

import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import nlp2testablecode.Activator;
import nlp2testablecode.DocHandler;
import nlp2testablecode.InputHandler;
import nlp2testablecode.code.Snippet;
import nlp2testablecode.tester.TypeSuggestions;

public class TypeSuggestionsTests {
	String before = "class Main{\npublic static void main(String args[]) {\n";
	String after = "}\n}\n";
	
	@Test
	public void testGeneration(){
		//our code, with a line missing a semicolon
		String code = "int i=0;\nint b = 0;\n";
		Snippet snippet = new Snippet(code, 0);
		DocHandler.setFileName("Test.java");
		Activator.random = new Random();
		InputHandler.before = before;
		InputHandler.after = after;
		Snippet done = TypeSuggestions.generate(snippet);
		//System.out.println(done.getArgumentTypes().get(0) + done.getReturn());
		assertTrue(done.getArgumentTypes().get(0).equals("int"));
		assertTrue(done.getReturn().equals("int"));
	}
}
