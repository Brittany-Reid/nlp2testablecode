package nlp2code;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * class IMCompiler
 * Handles In Memory Compilation of code snippets 
 */

class IMCompiler{
	JavaCompiler compiler;
	
	/*Constructor, setup compiler*/
	public IMCompiler(){
		compiler = ToolProvider.getSystemJavaCompiler();
	}
	
	/*Returns snippet with least compiler errors*/
	public Vector<String> getLeastCompilerErrorSnippet(Vector<String> snippets) {
		Vector<String> snippet = new Vector<String>();
		Integer errorCount;
		Integer storedCount = -1;
		
		for(String s : snippets) {
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
		
		return snippet;
	}
	
	/*Accepts a string of code, returns number of errors*/
	public int compile(String code) {
		Integer errors;
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		
		//create file from string
		JavaFileObject file = new JavaSourceFromString("Snippet", code);
		//add to compilation units
		Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);
		
		//run compilation task
		CompilationTask task = compiler.getTask(null, null, diagnostics, null, null, compilationUnits);

	    boolean success = task.call();
	    errors = 0;
	    for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
	    	if(diagnostic.getKind() == Diagnostic.Kind.ERROR) {
	    		errors++;
	    	}
	    }
	    //System.out.println("Success: " + success);
		
		return errors;
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