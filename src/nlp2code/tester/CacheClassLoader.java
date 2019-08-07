package nlp2code.tester;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import nlp2code.compiler.*;

/** Class CacheClassLoader
 * 	Custom classLoader that enables JUnit to work. Ensures we load all classes within the same classLoader.
 * 	See: https://stackoverflow.com/questions/24319697
 */
class CacheClassLoader extends URLClassLoader {
	private static final String BRIDGE_CLASS_NAME = nlp2code.tester.JUnitBridge.class.getName();
	 protected Map<String, IMCompiledCode> classes = new HashMap<>();
	 private URL[] providedClassPath;
	 
	 /**
	  * Constructor, taking an array of URL classPaths.
	  */
	 CacheClassLoader(URL[] classPaths){
		 super(addSystemClassPath(classPaths), null);
	     providedClassPath = classPaths;
	 }
	
	 /**
	  * Constructor for String classPath, we convert these to URLs then delegate back to the URL constructor.
	  */
	public CacheClassLoader(String classpath) {
		this(classPathToURLs(classpath));
	}
	
	/**
	 * Getter for the URL classPath
	 */
	public URL[] getProvidedClassPath() {
		return providedClassPath;
	}
	
	/**
	 * Add In Memory Compiled Code
	 */
    public void addCompiledCode(String className, IMCompiledCode code) {
        this.classes.put(className, code);
    }
    
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
    	// I have to intervene here, to ensure JUnitBridge uses me in the future.
        if (name.equals(BRIDGE_CLASS_NAME)) {
            return super.findClass(name);
        }
        
        // Our IMCompiled Code
        if (classes.containsKey(name)) {
            IMCompiledCode cc = classes.get(name);
            byte[] byteCode = cc.getBytes();
            return defineClass(name, byteCode, 0, byteCode.length);
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

	// Utility method to convert a : separated classpath into an array of URLs
	private static final URL[] classPathToURLs(String classPath) {
		if (classPath == null) {
		    return new URL[0];
		}
		
		String[] dirs = classPath.split(";");
		List<URL> urls = new ArrayList<>();
		
		//include plugin bin so we can find JUnitBridge
		URL u;
		u = (FileLocator.find(Platform.getBundle("nlp2code"), new Path("bin/"), null));
		try {
			u = (FileLocator.resolve(u));
			u = new File(u.toString().replace("file:/", "")).toURI().toURL();
			urls.add(u);
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String dir: dirs) {
		    try {		
		        URL url = new File(dir).toURI().toURL();		
		        urls.add(url);		
		    } catch (MalformedURLException e) {		
		        System.out.println("Error converted classpath to URL, malformed: " + dir);
		    }
		}
		URL[] urlArray = new URL[urls.size()];

		return urls.toArray(urlArray);
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