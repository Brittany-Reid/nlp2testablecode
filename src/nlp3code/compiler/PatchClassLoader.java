package nlp3code.compiler;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.commons.lang3.ArrayUtils;

/**
 * The OSGI bundle of org.jdt.core will also load in the required bundle containing
 * the buggy ECJ. This loads in before our ECJfix.jar, so we need to use a URLClassLoader
 * to specify where the class should come from. It also needs to be custom as typical
 * behaviour is to look inside the parent first. Again we define a custom classloader
 * that looks inside itself first, then in other classloaders.
 * This code is based on the CacheClassLoader.
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=541269
 */
public class PatchClassLoader extends URLClassLoader {
	private URL[] providedClassPath;
	
	public PatchClassLoader(URL[] classPaths){
		super(addSystemClassPath(classPaths), null);
	    providedClassPath = classPaths;
	}
	
	@Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
    	//get this class from the url classloader
        try {
        	return super.findClass(name);
        } catch(ClassNotFoundException e) {
        	//continue onto other classloaders
        }
        
        //otherwise system class loader
        try {
            ClassLoader system = ClassLoader.getSystemClassLoader();
            Class<?> fromSystemCall = system.loadClass(name);
            return fromSystemCall;
        //if not system, look at the plugin classloader
        //this is needed to load our plugin classes, eclipse doesn't use the system classloader
        } catch (ClassNotFoundException e) {
        	try {
        		ClassLoader plugin = this.getClass().getClassLoader();
        		Class<?> fromPlugin = plugin.loadClass(name);
        		return fromPlugin;
        	//if not either, try our classpath
        	}catch(ClassNotFoundException e2) {
	            return super.findClass(name);
        	}
        }
	}
	
	public static final URL[] addSystemClassPath(URL[] projectClasspath) {
		String classPath = System.getProperty("java.class.path");
		 String[] paths = classPath.split(File.pathSeparator);
		 URL[] urls = new URL[paths.length];
		 
		 int counter = 0;
		 for (String path : paths){
			 try {
				 urls[counter] = new File(path).toURI().toURL();
				 counter++;
		     } catch (MalformedURLException e) {
		    	 System.out.println("MalformedURL");
		     }
		 }
		 
		 return ArrayUtils.addAll(projectClasspath, urls);
	    }
}