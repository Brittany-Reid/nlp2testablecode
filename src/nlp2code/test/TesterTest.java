package nlp2code.test;

import org.junit.Test;

import nlp2code.Snippet;
import nlp2code.tester.Tester;

public class TesterTest {
	String before = "class Main {\n public static void main(String[] args) {\n";
	String after = "return;\n}\n}\n";
	
	@Test
	public void testGetAST(){
		//Snippet snippet = new Snippet("int i=0;\nint b;\nb=1;\nint a = i;\nwhile(true){\nint c=0;}\n", 0);
		Snippet snippet = new Snippet("Integer.parseInt(\"aaaa\");\n", 0);
	
		
		Tester.test(snippet, before, after);
	}
	
	
}
