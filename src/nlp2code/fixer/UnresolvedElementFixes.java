package nlp2code.fixer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;

import nlp2code.QueryDocListener;
import nlp2code.Snippet;

/**
 * This class contains fixes for solving unresolved element errors.
 */
class UnresovledElementFixes {
	//A Hashmap of possible types to packages (fully qualified names) from project classpath.
	private static Map<String, List<String>> classCache = new HashMap<>();
	
	public static Snippet fixUnresolvedType(Snippet snippet, Diagnostic<? extends JavaFileObject> diagnostic, int offset) {
		String type = Fixer.getCovered(snippet.getCode(), diagnostic.getStartPosition(), diagnostic.getEndPosition(), offset);
		
		List<String> packages = findPackagesForType(type);
		
		//if we couldn't find a type, no change
		if(packages == null || packages.isEmpty()) return null;
		
		//sort so java packages come first
		Collections.sort(packages, new Comparator<String>() {
		    @Override
		    public int compare(String o1, String o2) {
		    	if(o1.startsWith("java") && !o2.startsWith("java")) {
		    		return -1;
		    	}
		    	else if(o2.startsWith("java") && !o1.startsWith("java")) {
		    		return 1;
		    	}
		        return o1.compareTo(o2);
		    }
		});
		
		//for now, use the first we found
		snippet.addImportStatement("import " + packages.get(0) + ";");
		
		return snippet;
	}
	
	
	/**
	 * Given a String type, returns a list of possible string packages.
	 * @param type The String type.
	 * @return A list of possible packages.
	 */
	public static List<String> findPackagesForType(String type) {
		List<String> packages = null;
		if(!classCache.isEmpty()) {
			packages = classCache.get(type);
		}
		if(classCache.isEmpty()) {
			packages = new ArrayList();
			
			//get the java project from open editor
			IEditorInput input2 = QueryDocListener.editorPart.getEditorInput();
			IResource file2 = ((IFileEditorInput)input2).getFile();
			IProject pp = file2.getProject();
			IJavaProject jp = (IJavaProject) JavaCore.create(pp);
			
			//search through all elements on the classpath
			try {
				IPackageFragmentRoot[] roots = jp.getPackageFragmentRoots();
				for (int i = 0; i < roots.length; i++) {
					roots[i].open(null);
					for(IJavaElement child : roots[i].getChildren()) {
						if (child.getElementType()==IJavaElement.PACKAGE_FRAGMENT) {
							IPackageFragment packageFragment = (IPackageFragment) child;
							IClassFile[] classFiles = packageFragment.getAllClassFiles();
							
							//for all classfiles in a package
							for(IClassFile file : classFiles) {
								//confirm this is an ordinary class file we can get a type from
								if(file instanceof IOrdinaryClassFile) {
									//convert to an ocf
									IOrdinaryClassFile ocf = (IOrdinaryClassFile) file;
									//get the type object
									IType typeObject = ocf.getType();
									
									//if type is not anonymous and is public
									if(!typeObject.isAnonymous() && Flags.isPublic(typeObject.getFlags())) {
										//if the cache doesnt already contain this type
										if(!classCache.containsKey(typeObject.getElementName())) {
											List<String> typePackages = new ArrayList<>();
											typePackages.add(typeObject.getFullyQualifiedName());
											classCache.put(typeObject.getElementName(), typePackages);
										}
										else {
											List<String> typePackages = classCache.get(typeObject.getElementName());
											typePackages.add(typeObject.getFullyQualifiedName());
											classCache.put(typeObject.getElementName(), typePackages);
										}
										
										//if matches our type
										if(type.equals(typeObject.getElementName())){
											packages.add(typeObject.getFullyQualifiedName());
										}
										
									}
								}
							}
							
						}
							
							
					}
				}
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return packages;
	}
	
	/**
	 * Function to reset the cache. Currently unused but the intention is to clear the cache
	 * whenever the user updates their classPath / if we can't reliably listen for this whenever
	 * possible for this to happen.
	 */
	public static void clearCache() {
		classCache = new HashMap<>();
	}
	
}