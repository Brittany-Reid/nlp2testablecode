package nlp3code.tests.unittests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jface.text.IDocument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nlp3code.DocHandler;
import nlp3code.tests.TestEnvironment;

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
	 * Test the ability to find a function within file.
	 */
	@Test
	public void findFunction() {
		
		MethodDeclaration function = DocHandler.findFunction("main");
		
		assertEquals("public static void main(String args[]){\n}\n", function.toString());
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
	 * Test adding function.
	 */
	@Test
	public void testAddFunction() throws Exception{
		//DocHandler.addFunction("public void a() {\n}\n");
	}
	
	/**
	 * Initialize the parser.
	 */
	@Test
	public void initParser() {
		DocHandler.initializeEclipseParser();
	}

}
