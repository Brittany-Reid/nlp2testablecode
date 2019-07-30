package nlp2code;

import org.apache.logging.log4j.Logger;
import org.apache.commons.io.output.NullOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.ArrayList;
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

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;
import org.osgi.framework.Bundle;

/**
 * class IMCompiler
 * Handles in memory compilation of code snippets.
 */

class IMCompiler{
	static Logger logger;
	public Integer totalErrors;
	private JavaCompiler compiler;
	private List<String> options;
	public JavaFileManager fileManager;
	public static String before;
	public String after;
	public String fullName = "Main";
	public DiagnosticCollector<JavaFileObject> diagnostics;
	/*holds information about errors for this task*/
	public HashMap<String, Integer> errorKinds = new HashMap<String, Integer>();
	public HashMap<String, Integer> snippetsAffected = new HashMap<String, Integer>();
	public ArrayList<Integer> lineArray = new ArrayList<Integer>();
	public Boolean evaluating = false;
	public Integer errorFree = 0;
	public Integer finalLines;
	public Boolean o, n, l, f;
	public String temp;
	
	/*Constructor, setup compiler*/
	public IMCompiler(Boolean order, Boolean neutrality, Boolean loop){
		logger = Activator.getLogger();
		compiler = new EclipseCompiler();
		String junitDir = "";
		try {
			Bundle bundle = Platform.getBundle("nlp2code");
			Path path = new Path("lib/junit-4.12.jar");
			URL fileURL = FileLocator.find(bundle, path, null);
			fileURL = FileLocator.resolve(fileURL);
			junitDir = fileURL.toString().replace("file:/", "");
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		//class path will not accept absolute directory with double quotes
		//at C:\Users\IEUser\Desktop\
		options = Arrays.asList("-classpath", junitDir);
//		compiler = ToolProvider.getSystemJavaCompiler();
//		options = Arrays.asList("-Xlint");
		fileManager = new ClassFileManager(compiler.getStandardFileManager(null, null, null));
		totalErrors = 0;
		
		//options
		o = order;
		n = neutrality;
		l = loop;
		f = false;
	}
	
	/*Returns snippet with least compiler errors*/
	public Vector<String> getLeastCompilerErrorSnippet(Vector<String> snippets, String b, String a) {
		Vector<String> snippet = new Vector<String>();
		totalErrors = 0;
		Integer errorCount, errorCount2;
		Integer storedCount = -1;
		Integer i = 0;
		finalLines = 0;
		Integer lines = 0;
		lineArray.clear();
		
		before = b;
		after = a;
		
		//for each snippet
		for(String s : snippets) {
			//snippet number
			i++;
			logger.debug("Snippet " + i + ", ");
			
			//modify with fix
			f = true;
			//s = modify(s);
			
			//count compiler errors
			errorCount = compile(before+s+after);
			if(errorCount == 0) {
//				System.out.println("----");
//				System.out.print(s);
				errorFree++;
				lines = s.split("\n").length;
				finalLines += lines;
				lineArray.add(lines);
			}
			
			//if first snippet, new min
			if(storedCount == -1) {
				snippet.add(s);
				storedCount = errorCount;
			}
			//otherwiese
			else {
				//check if min
				if(errorCount < storedCount) {
					snippet.clear();
					snippet.add(s);
					storedCount = errorCount;
				}
			}
		}
		
		//error info for logger
		logger.debug("total for task " + totalErrors);
		for(String key : errorKinds.keySet()) {
			logger.debug(", \"" + key + "\", " + errorKinds.get(key));
		}
		logger.debug("\n");
		
		return snippet;
	}
	
	/*Accepts a string of code, returns number of errors*/
	public int compile(String code) {
		HashMap<String, Integer> snippetErrorKinds = new HashMap<String, Integer>();
		//System.out.println("\n" + before+code+after + "\n");
		Integer errors;
		diagnostics = new DiagnosticCollector<JavaFileObject>();
		
		//create file from string
		JavaFileObject file = new JavaSourceFromString("Main", code);
		//add to compilation units
		Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);
		
		//set the writer to null here, eclipse compiler treats null as to system.err
		Writer out = new OutputStreamWriter(new NullOutputStream());
		
		//run compilation task
		CompilationTask task = compiler.getTask(out, fileManager, diagnostics, options, null, compilationUnits);

	    boolean success = task.call();
	    errors = 0;
	    //System.out.println("------------------");
	    for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
	    	//if error
	    	if(diagnostic.getKind() == Diagnostic.Kind.ERROR) {
		    	String eCode = diagnostic.getCode();
		        //get first line of message
		    	String message = diagnostic.getMessage(null);
		    	if(message.contains("\n")){
		    			message = message.substring(0, message.indexOf("\n"));
		    	}	
		    	
		    	message = message + " " + eCode;
		    	
		    	//if recording errors
		    	if(evaluating == false) {
		    		Integer errorCount = 1;
			    	//add to error kinds
			        if(errorKinds.containsKey(message)) {
			        	errorCount = errorCount + errorKinds.get(message);
			        	errorKinds.replace(message, errorCount);
			        	errorCount = snippetsAffected.get(message);
			        	snippetsAffected.replace(message, errorCount+1);
			        }
			        else {
			        	errorKinds.put(message, errorCount);
			        	snippetsAffected.put(message, 1);
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
		    	}
		    	
		    	errors++;
	    	}
	    }
	    
	    //if recording errors
	    if(evaluating == false) {
		    totalErrors += errors;
		    logger.debug("total " + errors);
		    for(String key : snippetErrorKinds.keySet()) {
		    	logger.debug(", \"" + key + "\", " + snippetErrorKinds.get(key));
		    }
		    logger.debug("\n");
	    }
	    
	    if(success == true) {
//	    	try {
//	    		run();
//	    	} catch (Exception e) {
//	    		
//	    	}
	    }
		
		return errors;
	}

	/*Accepts a string of code, returns modification with least errors*/
	public String modify(String code) {
		String finalSnippet, modified;
		Integer errors, compare;
		
		//get errors
		evaluating = true;
		errors = compile(code);
		
		//if snippet is error free, return original
		if(errors == 0) {
			evaluating = false;
			return code;
		}
		

		
		finalSnippet = code;
		temp = "";
		
		if(f == true) {
			finalSnippet = Fixer.fix(code, diagnostics, before);
			errors = compile(finalSnippet);
			if(errors == 0) {
				evaluating = false;
				return finalSnippet;
			}
			temp = finalSnippet;
		}
		
		finalSnippet = deletion(finalSnippet, errors, o, n, l);
		//if(finalSnippet != code) {
			//logger.debug("MODIFYING SNIPPET:\n");
			//logger.debug("Original:\n---------------\n" + code + "\n--------------\n");
			//logger.debug("Modified:\n---------------\n" + finalSnippet + "\n--------------\n");
		//}
		evaluating = false;
		return finalSnippet;
	}
	
	/*deletion algorithm*/
	public String deletion(String code, Integer errors, Boolean order, Boolean neutrality, Boolean passes) {
		String finalSnippet;
		String modified;
		Integer minErrors;
		Integer testErrors;
		Integer lines;
		Integer toDelete;
		Boolean endCondition;
		
		//get starting state
		finalSnippet = code;
		minErrors = errors;
		lines = code.split("\n").length;
		
		//passes loop, end when deletions no longer happen
		endCondition = false;
		while(endCondition == false) {
			toDelete = 1;
			if(order == true) toDelete = lines - 1;
			endCondition = true;
			
			//until we hit end of snippet
			modified = deleteLine(finalSnippet, toDelete);
			while(modified != null) {
				testErrors = compile(modified);
				//logger.debug("deleting line " + toDelete + "\n----------\n" + modified + "\n----------\n");
				//if deletion removed errors
				if(testErrors < errors && neutrality == false){
					//new best
					finalSnippet = modified;
					errors = testErrors;
					//logger.debug("Reduced errors to: " + errors + "\n----------\n");
					if(passes) endCondition = false;
				}
				else if(testErrors <= errors && neutrality == true){
					//new best
					finalSnippet = modified;
					errors = testErrors;
					//logger.debug("Reduced errors to: " + errors + "\n----------\n");
					if(passes) endCondition = false;
				}
				//if it didn't
				else {
					//skip line
					if(order == false) toDelete++;
					//logger.debug("Reverted, skipping line.\n----------\n");
				}
				if(order == true) toDelete--;
				//break loop if we've reduced errors to 0
				if(errors == 0) break;
				
				//next
				modified = deleteLine(finalSnippet, toDelete);
			}
			
			//end after 1 pass if passes not enabled
			if(!passes) endCondition = true;
			
		}
		
		return finalSnippet;
	}
	
	/*Modifiy by deleting lines*/
	public String deleteLine(String code, Integer line) {
		String modified;
		String[] lines;
		Integer length;
		
		//split the snippet by nl for lines
		lines = code.split("\n");
		length = lines.length;
		
		//if selected line longer, return null
		if(line >= length || line < 0) {
			return null;
		}
		
		//delete line
		modified = "";
		for(Integer i = 0; i<length; i++) {
			if(i != line) {
				modified += lines[i] + "\n";
			}
		}
		
		return modified;
	}
	
	/*Runs compiled code
	 * UNTESTED, DO NOT RUN OUTSIDE VM */
	public void run() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
        	Method method = fileManager.getClassLoader(null).loadClass(fullName).getDeclaredMethod("test", new Class[] {});
        	method.setAccessible(true);
        	String result = (String) method.invoke(null);
        } catch(Exception e) {
        	e.printStackTrace();
        }
		
	}
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
	    super(URI.create("file:///" + name + Kind.SOURCE.extension),Kind.SOURCE);
	    this.code = code;
	  }

	  @Override
	  public CharSequence getCharContent(boolean ignoreEncodingErrors) {
	    return code;
	  }
}