package nlp3code.tester;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nlp3code.compiler.IMCompiledCode;

public class ProcessClassLoader extends URLClassLoader {
    URL[] providedClassPath;
    private static final String BRIDGE_CLASS_NAME = nlp3code.tester.JUnitBridge.class.getName();
	protected Map<String, IMCompiledCode> classes = new HashMap<>();
	/**
	  * Constructor, taking an array of URL classPaths.
	  */
	public ProcessClassLoader(URL[] classPaths){
		super(addSystemClassPath(classPaths), null);
	    providedClassPath = classPaths;
	}

	/**
	  * Constructor for String classPath, we convert these to URLs then delegate back to the URL constructor.
	  */
	public ProcessClassLoader(String classpath) {
		this(classPathToURLs(classpath));
	}
	
	// If the class can't be found using parents (I don't have any) then drops back here.
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {

        // I have to intervene here, to ensure JUnitBridge uses me in the future.
        if (name.equals(BRIDGE_CLASS_NAME)) {
            return super.findClass(name);
        }

        // Modified class? Return the modified code.
        if (classes.containsKey(name)) {
            IMCompiledCode cc = classes.get(name);
            byte[] byteCode = cc.getBytes();
            return defineClass(name, byteCode, 0, byteCode.length);
        }

        // Otherwise, try the system class loader. If not there, must be part of the project, so load ourselves.
        try {
            ClassLoader system = ClassLoader.getSystemClassLoader();
            Class fromSystemCall = system.loadClass(name);
            return fromSystemCall;
        } catch (ClassNotFoundException e) {
            return super.findClass(name);
        }

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
    
 // Utility method to convert a : separated classpath into an array of URLs
 	private static final URL[] classPathToURLs(String classPath) {
 		if (classPath == null) {
 		    return new URL[0];
 		}
 		
 		String[] dirs = classPath.split(";");
 		List<URL> urls = new ArrayList<>();


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
 		 
		URL[] finalUrls = new URL[urls.length + projectClasspath.length];
		int i =0;
		for(URL url: projectClasspath) {
			finalUrls[i] = url;
			i++;
		}
 		 
		for(URL url: urls) {
			 finalUrls[i] = url;
			 i++;
		}
		
		return finalUrls;
 	}
    
}
