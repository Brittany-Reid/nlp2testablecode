package nlp2code.test;

import org.junit.Test;

import nlp2code.Snippet;
import nlp2code.tester.Tester;

public class TesterTest {
	String before = "class Main {\n public static void main(String[] args) {\n";
	String after = "return;\n}\n}\n";
	
	@Test
	public void testGetAST(){
		if(Tester.parser == null) {
			Tester.initializeParser();
		}
		//Snippet snippet = new Snippet("int i=0;\nint b;\nb=1;\nint a = i;\nwhile(true){\nint c=0;}\n", 0);
		Snippet snippet = new Snippet("int a = new Object(i);\nInteger.parseInt(\"aaaa\");\n int i=0;\n", 0);
		Tester.block = Tester.getSnippetAST(snippet, before, after);
		Tester.findTestIO();
	}
	
	
}
