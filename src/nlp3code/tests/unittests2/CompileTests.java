package nlp3code.tests.unittests2;

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
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.junit.*;

import nlp3code.Evaluator;
import nlp3code.compiler.IMCompiler;

/**
 * Due to ecj mess, ensure the JUnit classpath loads jdt.core and ecj before plug-in dependencies.
 */
public class CompileTests {
	//define default surrounding code
	String before = "class Main{\npublic static void main(String args[]) {\n";
	String after = "}\n}\n";
	IProject project = null;
	
	/**
	 * Create a clean dummy project for testing before each test.
	 */
	@Before
	public void setUp() throws Exception {
	
		//create project
		project = null;
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
		project = null;
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		project = workspace.getRoot().getProject("Test");
		
		if (project.exists()) {
			// Clean up any old project information.
			project.delete(true, true, null);
		}
	}
	
	@Test
	public void testCompilerPatch() {
		Evaluator.compiler = Evaluator.initializeCompiler(false);
		IMCompiler compiler = Evaluator.compiler;
		compiler.clearSaved();
		compiler.addSource("Main", "class Main{\nint i = 0;\n}\n");
		compiler.compileAll();
		int errors = compiler.getErrors();
		assertEquals(0, errors);
	}
	
	@Test
	public void testCompilerErrors(){
		Evaluator.compiler = Evaluator.initializeCompiler(false);
		IMCompiler compiler = Evaluator.compiler;
		compiler.clearSaved();
		compiler.addSource("Main", "class Main{\nint i = 0\n}\n");
		compiler.compileAll();
		int errors = compiler.getErrors();
		assertEquals(1, errors);
	}
	
	/**
	 * Test that we can add JUnit to the compiler correctly.
	 */
	@Test
	public void testJUnit() {
		String classpath = Evaluator.getJUnitClassPath();
		Evaluator.compiler = Evaluator.initializeCompiler(false);
		IMCompiler compiler = Evaluator.compiler;
		
		String original = compiler.getClasspath();
		if(!original.trim().isEmpty()) {
			classpath = original + ";" + classpath;
		}
		compiler.setClasspath(classpath);
		
		
		compiler.clearSaved();
		String code = "import static org.junit.Assert.*;\nimport org.junit.Test;\npublic class Tests{\n@Test\npublic void test() {\n}\n}\n";
		compiler.addSource("Tests", code);
		compiler.compileAll();
		int errors = compiler.getErrors();
		assertEquals(0, errors);
		
		compiler.setClasspath(original);
	}
	
//	/**
//	 * Test that we can add plugin packages to the compiler correctly.
//	 */
//	@Test
//	public void testSystemClasses() {
//		Evaluator.compiler = Evaluator.initializeCompiler(false);
//		IMCompiler compiler = Evaluator.compiler;
//		compiler.clearSaved();
//		String code = "package nlp3code;\nimport nlp3code.code.Snippet;\nclass Main{\n public void test() {\nSnippet snippet;\n}\n}\n";
//		compiler.addSource("Main", code);
//		compiler.compileAll();
//		int errors = compiler.getErrors();
//		assertEquals(0, errors);
//	}
	
}
