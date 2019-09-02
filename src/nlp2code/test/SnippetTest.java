package nlp2code.test;

import static org.junit.Assert.*;
import org.junit.Test;
import nlp2code.Snippet;

public class SnippetTest {

	@Test
	public void createTest() {
		Snippet snippet = new Snippet("class Main{\n}\n", 0);
		assertEquals(snippet.getLOC(), 2);
	}
	
	@Test
	public void copyTest() {
		//modify snippet while copy retains its info
		Snippet snippet = new Snippet("class Main{\n}\n", 0);
		Snippet copy = new Snippet(snippet);
		snippet.deleteLine(1);
		assertEquals(copy.getLOC(), 2);
	}
	
	
	@Test
	public void resetTest() {
		Snippet snippet = new Snippet("class Main{\n}\n", 0);
		snippet.setCode("class NewMain{\npublic int i;\n}\n");
		snippet.getFormattedCode();
		assertEquals(snippet.getLOC(), 3);
	}
	
	@Test
	public void deleteTest() {
		Snippet snippet = new Snippet("class NewMain{\npublic int i;\n}\n", 0);
		snippet.deleteLine(2);
		assertEquals(snippet.getLOC(), 2);
		assertEquals(snippet.size(), 3);
	}
	
	@Test
	public void commentRemovalTest() {
		//if we ever reconstruct the codeString, remove comments and empty lines
		Snippet snippet = new Snippet("//this is a comment\n\npublic int i;\n", 0);
		snippet.deleteLine(3);
		assertEquals(snippet.getCode(), "");
	}
	
	@Test
	public void getLineTest() {
		//test that we can get the line, where lines start at 1
		Snippet snippet = new Snippet("class Main{\npublic int i;\n}\n", 0);
		String line = snippet.getLine(2);
		assertEquals(line, "public int i;");
	}
	
	@Test
	public void importTest() {
		Snippet snippet = new Snippet("import java.io.File;\npublic int i;\n", 0);
		assertEquals(snippet.getCode(), "public int i;\n");
	}
}

