package nlp2code;

import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.security.SecureClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * class IMCompiler
 * Handles in memory compilation of code snippets.
 */

class IMCompiler{
	static Logger logger;
	static Integer totalErrors;
	private JavaCompiler compiler;
	private List<String> options;
	private JavaFileManager fileManager;
	private String before;
	private String after;
	private String fullName = "Main";
	/*holds information about errors for this task*/
	public static HashMap<String, Integer> errorKinds = new HashMap<String, Integer>();
	
	/*Constructor, setup compiler*/
	public IMCompiler(String b, String a){
		logger = Activator.getLogger();
		compiler = ToolProvider.getSystemJavaCompiler();
		options = Arrays.asList("-Xlint");
		fileManager = new ClassFileManager(compiler.getStandardFileManager(null, null, null));
		before = b;
		after = a;
		totalErrors = 0;
	}
	
	/*Returns snippet with least compiler errors*/
	public Vector<String> getLeastCompilerErrorSnippet(Vector<String> snippets) {
		Vector<String> snippet = new Vector<String>();
		final long startTime = System.currentTimeMillis();
		final long endTime = 100000;
		totalErrors = 0;
		Integer errorCount;
		Integer storedCount = -1;
		Integer i = 0;
		
		for(String s : snippets) {
			i++;
			//break loop if takes more than endTime ms
			if(System.currentTimeMillis() - startTime > endTime) {
				break;
			}
			logger.debug("Snippet " + i + ", ");
			errorCount = compile(s);
			if(storedCount == -1) {
				snippet.add(s);
				storedCount = errorCount;
			}
			else {
				if(errorCount < storedCount) {
					snippet.clear();
					snippet.add(s);
					storedCount = errorCount;
				}
			}
		}
		
		logger.debug("total for task " + totalErrors);
		for(String key : errorKinds.keySet()) {
			logger.debug(", " + key + " " + errorKinds.get(key));
		}
		logger.debug("\n");
		
		return snippet;
	}
	
	/*Accepts a string of code, returns number of errors*/
	public int compile(String code) {
		HashMap<String, Integer> snippetErrorKinds = new HashMap<String, Integer>();
		//System.out.println("\n" + before+code+after + "\n");
		Integer errors;
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		
		//create file from string
		JavaFileObject file = new JavaSourceFromString("Snippet", before+code+after);
		//add to compilation units
		Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);
		
		//run compilation task
		CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);

	    boolean success = task.call();
	    errors = 0;
	    //System.out.println("------------------");
	    for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
	        //get first line of message
	    	String message = diagnostic.getMessage(null);
	    	if(message.contains("\n")){
	    			message = message.substring(0, message.indexOf("\n"));
	    	}	
	    	
	    	Integer errorCount = 1;
	    	//add to error kinds
	        if(errorKinds.containsKey(message)) {
	        	errorCount = errorCount + errorKinds.get(message);
	        	errorKinds.replace(message, errorCount);
	        }
	        else {
	        	errorKinds.put(message, errorCount);
	        }
	        
	        errorCount = 1;
	        //add to snippetErrorKinds
	        if(snippetErrorKinds.containsKey(message)) {
	        	errorCount = errorCount + snippetErrorKinds.get(message);
	        	snippetErrorKinds.replace(message, errorCount);
	        }
	        else {
	        	snippetErrorKinds.put(message, errorCount);
	        }
	        
	        
	    	if(diagnostic.getKind() == Diagnostic.Kind.ERROR) {
	    		errors++;
	    	}
	    }
	    totalErrors += errors;
	    logger.debug("total " + errors);
	    for(String key : snippetErrorKinds.keySet()) {
	    	logger.debug(", " + key + " " + snippetErrorKinds.get(key));
	    }
	    logger.debug("\n");
	    
	    if(success == true) {
//	    	try {
//	    		run();
//	    	} catch (Exception e) {
//	    		
//	    	}
	    }
	    //System.out.println("Success: " + success);
		
		return errors;
	}

	/*Runs compiled code
	 * UNTESTED, DO NOT RUN OUTSIDE VM
	public void run() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
            fileManager
                    .getClassLoader(null)
                    .loadClass(fullName)
                    .getDeclaredMethod("main", new Class[]{String[].class})
                    .invoke(null, new Object[]{null});
        } catch (InvocationTargetException e) {
            System.out.print("InvocationTargetException");
            //logger.error("InvocationTargetException:", e);
        } catch (NoSuchMethodException e) {
            System.out.print("NoSuchMethodException ");
            //logger.error("NoSuchMethodException:", e);
        }
    }
    */
}

	

class JavaClassObject extends SimpleJavaFileObject {
    protected final ByteArrayOutputStream bos =
            new ByteArrayOutputStream();

    public JavaClassObject(String name, Kind kind) {
        super(URI.create("string:///" + name.replace('.', '/')
                + kind.extension), kind);
    }

    public byte[] getBytes() {
        return bos.toByteArray();
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return bos;
    }
}

class ClassFileManager extends ForwardingJavaFileManager {
    private JavaClassObject javaClassObject;

    public ClassFileManager(StandardJavaFileManager standardManager) {
        super(standardManager);
    }

    @Override
    public ClassLoader getClassLoader(Location location) {
        return new SecureClassLoader() {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] b = javaClassObject.getBytes();
                return super.defineClass(name, javaClassObject.getBytes(), 0, b.length);
            }
        };
    }

    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        this.javaClassObject = new JavaClassObject(className, kind);
        return this.javaClassObject;
    }
}


class JavaSourceFromString extends SimpleJavaFileObject {
	  final String code;

	  JavaSourceFromString(String name, String code) {
	    super(URI.create("string:///" + name.replace('.','/') + Kind.SOURCE.extension),Kind.SOURCE);
	    this.code = code;
	  }

	  @Override
	  public CharSequence getCharContent(boolean ignoreEncodingErrors) {
	    return code;
	  }
}