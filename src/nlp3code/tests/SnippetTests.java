package nlp3code.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import nlp3code.DocHandler;
import nlp3code.code.Snippet;

/**
 * Test snippet creation and modification.
 * Use of the Eclipse parser fails without jdt.core jar
 * The error is likely a version difference due to the ecj patch but I don't know why
 * it persists even if the bundle version is the same.
 * Make sure the JUnit classpath has plug-in dependencies before jdt.core but after ecj
 */
public class SnippetTests {
	String before = "class Main{\npublic static void main(String args[]) {\n";
	String after = "}\n}\n";
	int offset = before.length();
	String surrounding = before + after;
	
	@Test
	public void statement() {
		String code = "int i=0;\n";
		Snippet snippet = new Snippet(code, 0);
		assertEquals(1, snippet.getLOC());
		assertEquals(code, snippet.getCode());
		assertEquals(snippet.size(), snippet.getLOC());
	}
	
	@Test
	public void tryInsert() {
		String code = "int i=0;\n";
		Snippet snippet = new Snippet(code, 0);
		String combined = Snippet.insert(snippet, surrounding, offset);
		assertEquals(before+code+after, combined);
	}
	
	@Test
	public void deleteLine() {
		String code = "int i=0;\n";
		Snippet snippet = new Snippet(code, 0);
		snippet.deleteLine(1);
		assertEquals(1, snippet.size());
		assertEquals(0, snippet.getLOC());
		
		//check inserted
		String combined = Snippet.insert(snippet, surrounding, offset);
		assertEquals(before+after, combined);
	}
	
	@Test
	public void testFormatted() {
		String code = "int i=0;\n";
		Snippet snippet = new Snippet(code, 0);
		String combined = Snippet.insert(snippet, surrounding, offset);
		//formatted
		String formatted = Snippet.insertFormatted(snippet, surrounding, offset);
		assertNotEquals(formatted, combined);
	}
	
	@Test 
	public void statementAndImport() {
		String code = "import java.lang.*;\nint i=0;\n";
		Snippet snippet = new Snippet(code, 0);
		String combined = Snippet.insert(snippet, surrounding, offset);
		assertEquals(2, snippet.getLOC());
	}
	
	/**Sometimes a SO answer contains multiple 'files', which will collate*/
	@Test
	public void filterDuplicateImports() {
		String im = "import java.lang.*;\nimport java.lang.*;\n";
		String code = "int i=0;\n";
		Snippet snippet = new Snippet(im+code, 0);
		String combined = Snippet.insert(snippet, surrounding, offset);
		assertEquals(2, snippet.getLOC());
		assertEquals("import java.lang.*;\n" + before + code + after, combined);
	}
	
	@Test
	public void deleteDuplicateImports() {
		DocHandler.documentChanged();
		String im = "import java.lang.*;\n";
		String code = "int i=0;\n";
		Snippet snippet = new Snippet(im+code, 0);
		String combined = Snippet.insert(snippet, im+surrounding, offset+im.length());
		assertEquals(im+before+code+after, combined);
		DocHandler.documentChanged();
	}
	
	@Test
	public void deleteMultipleLines() {
		String im = "import java.lang.*;\n";
		String code = "int i=0;\nint a=0;\nint b=0;\n";
		Snippet snippet = new Snippet(im+code, 0);
		String combined = Snippet.insert(snippet, surrounding, offset);
		
		snippet.deleteLine(1);
		
		combined = Snippet.insert(snippet, surrounding, offset);
		assertFalse(combined.contains("im"));
		
		snippet.deleteLine(4);
		combined = Snippet.insert(snippet, surrounding, offset);
		assertFalse(combined.contains("b=0"));
		
		//test cache
		assertEquals(snippet.getLOC(), snippet.getLOC());
		assertEquals(snippet.size(), snippet.size());
	}
	
	@Test
	public void getLastLine() {
		String im = "import java.lang.*;\n";
		String code = "int i=0;\nint a=0;\nint b=0;\n";
		Snippet snippet = new Snippet(im+code, 0);
		String line = snippet.getLine(4);
		assertEquals("int b=0;", line);
	}
	
	@Test
	public void checkIsDeleted() {
		String im = "import java.lang.*;\n";
		String code = "int i=0;\nint a=0;\nint b=0;\n";
		Snippet snippet = new Snippet(im+code, 0);
		snippet.deleteLine(1);
		assertTrue(snippet.isDeleted(1));
	}
	
	@Test
	public void copyTest() {
		String code = "int i=0;\nint a=0;\nint b=0;\n";
		Snippet snippet = new Snippet(code, 0);
		snippet.getLOC();
		Snippet snippet2 = new Snippet(snippet);
		
		snippet2.deleteLine(2);
		
		assertNotEquals(snippet.getLOC(), snippet2.getLOC());
	}
	
	@Test
	public void getSnippet() {
		String im = "import java.lang.*;\n";
		String code = "int i=0;\nint a=0;\nint b=0;\n";
		Snippet snippet = new Snippet(im+code, 0);
		assertEquals(code, snippet.getCode());
	}
	
	
	@Test
	public void setCode() {
		String im = "import java.lang.*;\n";
		String code = "int i=0;\nint a=0;\nint b=0;\n";
		Snippet snippet = new Snippet(im+code, 0);
		snippet.setCode("int c = 0;\n");
		assertNotEquals(code, snippet.getCode());
	}
}
