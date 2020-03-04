package nlp3code.tests.unittests;

import static org.junit.Assert.*;

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
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nlp3code.DocHandler;

public class DocHandlerTests {
	String before = "class Main{\npublic static void main(String args[]) {\n";
	String after = "}\n}\n";
	
	/**
	 * Create a clean dummy project for testing before each test.
	 */
	@Before
	public void setUp() throws Exception {
	
		//create project
		IProject project = null;
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		project = workspace.getRoot().getProject("Test");
		
		if (project.exists()) {
			// Clean up any old project information.
			project.delete(true, true, null);
		}
		
		project.create(null);
		project.open(null);
		
		IProjectDescription description = project.getDescription();
		description.setNatureIds(new String[] { JavaCore.NATURE_ID });
		project.setDescription(description, null);
		
		final IJavaProject javaProject = JavaCore.create(project);
		
		//set bin
		IFolder binFolder = project.getFolder("bin");
		binFolder.create(false, true, null);
		javaProject.setOutputLocation(binFolder.getFullPath(), null);
		
		//set source
		IFolder sourceFolder = project.getFolder("src");
		sourceFolder.create(false, true, null);
		

		IPackageFragment pack = javaProject.getPackageFragmentRoot(sourceFolder).createPackageFragment("Test", false, null);
		
		//code
		String source = before + after;
		StringBuffer buffer = new StringBuffer();
		buffer.append("package " + pack.getElementName() + ";\n");
		buffer.append("\n");
		buffer.append(source);
		
		//add
		ICompilationUnit cu = pack.createCompilationUnit("Main.java", buffer.toString(), false, null);
		
		//open
		IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = workbenchWindow.getActivePage();
		
		IPath path = new Path("Test/src/Test/Main.java");
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		
		IDE.openEditor(page, file, true);
		
	}
	
	/**
	 * Delete previous project after each test.
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		IProject project = null;
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		project = workspace.getRoot().getProject("Test");
		
		if (project.exists()) {
			// Clean up any old project information.
			project.delete(true, true, null);
		}
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
	 * Initialize the parser.
	 */
	@Test
	public void initParser() {
		DocHandler.initializeEclipseParser();
	}

}
