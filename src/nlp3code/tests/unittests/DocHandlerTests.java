package nlp3code.tests.unittests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
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
