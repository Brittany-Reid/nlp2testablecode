package nlp2testablecode.compiler;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;

/**
 * Handles in memory compilation of potential code.
 */

public class IMCompiler{
	private JavaCompiler compiler;
	private List<String> options;
	private List<JavaSourceFromString> sources = new ArrayList<JavaSourceFromString>();
	//writer to handle eclipse still printing
	Writer out;
	public DiagnosticCollector<JavaFileObject> diagnostics;
	public IMFileManager fileManager;
	private Integer errorCount;
	public static Boolean logging = true;
	
	/**
	 * Constructor
	 * 	Creates a new IMCompiler with the specified compiler type.
	 */
	public IMCompiler(JavaCompiler compiler, List<String> options, Writer out) {
		this.compiler = compiler;
		this.options = options;
		this.out = out;
		this.fileManager = new IMFileManager(compiler.getStandardFileManager(null, null, null));
		errorCount = -1;
	}
	
	/**
	 * Function useParentClassLoader
	 * @param parent
	 *   The ClassLoader to use as parent.
	 */
	public void useParentClassLoader(ClassLoader parent) {
		//this.classLoader = new IMClassLoader(parent);
	}
	
	/**
	 * Function addSource
	 * 	 Adds source list of sources to be compiled.
	 */
	public void addSource(String className, String source) {
		sources.add(new JavaSourceFromString(className, source));
	}
	
	public String getClasspath() {
		String fullClasspath = null;
		
		if(options != null) {
			for(int i=0; i<options.size(); i++) {
				if(options.get(i).equals("-classpath")) {
					fullClasspath = options.get(i+1);
				}
			}
		}
		
		return fullClasspath;
	}
	
	public void setClasspath(String classpath) {
		if(classpath == null) {
			options = null;
			return;
		}
		options = new ArrayList<String>(Arrays.asList("-classpath", classpath));
	}
	
	/**
	 * Function compileAll
	 * 	Compiles all added sources.
	 * @return Null if no sources exist, else a map of class names to classes.
	 */
	public Integer compileAll(){
		//no sources
		if(sources == null) return 0;
		
		errorCount = -1;
		
		//add sources to our compilation units
		Iterable<? extends JavaFileObject> compilationUnits = sources;
		diagnostics = new DiagnosticCollector<JavaFileObject>();
		JavaCompiler.CompilationTask task = compiler.getTask(out, fileManager, diagnostics, options, null, compilationUnits);
		fileManager.setClassLoader(new IMClassLoader());
		
		//compile
		Boolean success = task.call();
		
		return 0;
	}

	public Integer getErrors() {
		//if error count was previously calculated just return
		if(errorCount != -1) {
			return errorCount;
		}
		
		//otherwise we count the diagnostics
		errorCount = 0;
	    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
	    	//if error
	    	if(diagnostic.getKind() == Diagnostic.Kind.ERROR) {
		    	errorCount++;
	    	}
	    }
		
		return errorCount;
	}
	
	/**
	 * Function getDiagnostics()
	 *   Gets the diagnostics of the last compile.
	 */
	public DiagnosticCollector<JavaFileObject> getDiagnostics() {
		return diagnostics;
	}
	
	public void clearSaved() {
		errorCount = -1;
		sources.clear();
	}

}