package nlp3code.tests.unittests;

import static org.junit.Assert.*;

import org.junit.Test;

import nlp3code.DocHandler;
import nlp3code.code.Snippet;

public class SnippetTests {
	String before = "class Main{\npublic static void main(String args[]) {\n";
	String after = "}\n}\n";
	int offset = before.length();
	String surrounding = before + after;
	
	/**
	 * When allowing failed compiles (err = -1), we get a comparison method 
	 * violates its general contract error.
	 */
	@Test
	public void testComparator() {
		Snippet s1, s2, s3;
		
		s1 = new Snippet("int a;\n", 0);
		s2 = new Snippet("", 0);
		s3 = new Snippet("int a;\nint b;\n", 0);
		
		int r1 = s1.compareTo(s2);
		int r2 = s2.compareTo(s1);
		assertTrue(r1 == -1); //s1 before s2
		assertTrue(r2 == 1); //s2 after s1
		
		r1 = s1.compareTo(s3);
		r2 = s3.compareTo(s1);
		assertTrue(r1 == 0); //s1 and s3 the same
		assertTrue(r2 == 0); //s3 and s1 the same
		
		s1.updateErrors(0, null);
		r1 = s1.compareTo(s3);
		r2 = s3.compareTo(s1);
		assertTrue(r1 == -1); //s1 before s3
		assertTrue(r2 == 1); //s3 after s1
		
		s3.updateErrors(1, null);
		r1 = s1.compareTo(s3);
		r2 = s3.compareTo(s1);
		assertTrue(r1 == -1); //s1 before s3
		assertTrue(r2 == 1); //s3 after s1
	}

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
