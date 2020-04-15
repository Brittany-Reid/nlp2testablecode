package nlp3code.tests;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;


import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.jdt.launching.JavaRuntime;

/**
 * This class contains values and functions for use during testing.
 */
public class TestEnvironment {
	public static final String before = "public class Main{\npublic static void main(String args[]) {\n";
	public static final String after = "}\n}\n";
	public static final String filename = "Main.java";

	public static void addLibrary(String path) throws Exception {
		IProject project = null;
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		project = workspace.getRoot().getProject("Test");
		
		IJavaProject javaProject = JavaCore.create(project);
		
		URL url = FileLocator.find(Platform.getBundle("nlp3code"), new Path(path), null);
		url = FileLocator.resolve(url);
		File file = URIUtil.toFile(URIUtil.toURI(url));
		
		InputStream jarLibraryInputStream = new BufferedInputStream(new FileInputStream(file));
		IFile libFile = javaProject.getProject().getFile(file.getName());
		libFile.create(jarLibraryInputStream, false, null);
		
		IClasspathEntry relativeLibraryEntry = new org.eclipse.jdt.internal.core.ClasspathEntry(
		        IPackageFragmentRoot.K_BINARY,
		        IClasspathEntry.CPE_LIBRARY, libFile.getLocation(),
		        ClasspathEntry.INCLUDE_ALL, // inclusion patterns
		        ClasspathEntry.EXCLUDE_NONE, // exclusion patterns
		        null, null, null, // specific output folder
		        false, // exported
		        ClasspathEntry.NO_ACCESS_RULES, false, // no access rules to combine
		        ClasspathEntry.NO_EXTRA_ATTRIBUTES);

		// add the new classpath entry to the project's existing entries
		IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
		IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
		System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
		newEntries[oldEntries.length] = relativeLibraryEntry;
		javaProject.setRawClasspath(newEntries, null);
	}
	
	/**
	 * Creates a clean project for testing.
	 * @throws Exception
	 */
	public static void setupWorkspace() throws Exception {
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
		
		//set classpath
		IClasspathEntry[] buildPath = {JavaCore.newSourceEntry(project.getFullPath().append("src")), JavaRuntime.getDefaultJREContainerEntry() };
		 
		javaProject.setRawClasspath(buildPath, project.getFullPath().append("bin"), null);
		

		IPackageFragment pack = javaProject.getPackageFragmentRoot(sourceFolder).createPackageFragment(IPackageFragment.DEFAULT_PACKAGE_NAME, false, null);
		
		//code
		String source = before + after;
		StringBuffer buffer = new StringBuffer();
//		buffer.append("package " + pack.getElementName() + ";\n");
//		buffer.append("\n");
		buffer.append(source);
		
		//add
		ICompilationUnit cu = pack.createCompilationUnit(filename, buffer.toString(), false, null);
		
		
		//open
		IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = workbenchWindow.getActivePage();
		
		IPath path = new Path("Test/src/" + filename);
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		
		IDE.openEditor(page, file, true);
	}
	
	/**
	 * Cleans workspace.
	 */
	public static void cleanWorkspace() {
		IProject project = null;
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		project = workspace.getRoot().getProject("Test");
		
		if (project.exists()) {
			// Clean up any old project information.
			try {
				project.delete(true, true, null);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		
		//no document open
		IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = workbenchWindow.getActivePage();
	
		page.closeAllEditors(false);
	}
}
