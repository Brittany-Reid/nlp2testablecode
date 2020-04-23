package nlp2testablecode.tests.unittests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jface.text.IDocument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nlp2testablecode.DocHandler;
import nlp2testablecode.tests.TestEnvironment;

public class DocHandlerTests {
	String before = TestEnvironment.before;
	String after = TestEnvironment.after;
	
	/**
	 * Create a clean dummy project for testing before each test.
	 */
	@Before
	public void setUp() throws Exception {
	
		TestEnvironment.setupWorkspace();
		
	}
	
	/**
	 * Delete previous project after each test.
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		TestEnvironment.cleanWorkspace();
	}
	
	/**
	 * Can we get an open document?
	 */
	@Test
	public void getOpenDocument() {
		IDocument document = DocHandler.getDocument();
		assertNotNull(document);
	}
	
	/**
	 * Test ability to get classpath
	 * @throws Exception 
	 */
	@Test
	public void classpathTest() throws Exception {
		TestEnvironment.addLibrary("lib/guava-28.2-jre.jar");
		String classpath = DocHandler.getClassPath();
		assertTrue(classpath.contains("Test/guava-28.2-jre.jar"));
	}
	
	/**
	 * Test the ability to find a function within file.
	 */
	@Test
	public void findFunction() {
		MethodDeclaration function = DocHandler.findFunction("main");
		assertEquals("public static void main(String args[]){\n}\n", function.toString());
	}

	
	/**
	 * Test adding function.
	 */
	@Test
	public void testAddFunction() throws Exception{
		DocHandler.addFunction("public void a() {\n}\n");
		MethodDeclaration function = DocHandler.findFunction("a");
		System.out.println(function);
	}
	
	/**
	 * Test adding import statements.
	 */
	@Test
	public void testAddImports() {
//		List<String> imports = new ArrayList<>();
//		DocHandler.addImportStatements(imports);
//		DocHandler.documentChanged();
//		
//		imports.add("import java.util.List;");
//		DocHandler.addImportStatements(imports);
//		DocHandler.documentChanged();
		
		//We're running in the UI thread so usual UI modifications wait until we're done
	}
	
	/**
	 * Initialize the parser.
	 */
	@Test
	public void initParser() {
		DocHandler.initializeEclipseParser();
	}

}
