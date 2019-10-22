package nlp3code.tests;

import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import nlp3code.Activator;
import nlp3code.DocHandler;
import nlp3code.InputHandler;
import nlp3code.code.Snippet;
import nlp3code.fixer.Deleter;
import nlp3code.tester.TypeSuggestions;

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
		TypeSuggestions.generate(snippet);
	}
}
