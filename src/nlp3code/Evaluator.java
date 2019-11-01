package nlp3code;

import org.eclipse.core.runtime.Platform;
import nlp3code.code.Snippet;
import nlp3code.compiler.IMCompiler;
import nlp3code.compiler.PatchClassLoader;
import nlp3code.fixer.Deleter;
import nlp3code.fixer.Fixer;
import nlp3code.fixer.Integrator;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.tools.JavaCompiler;

import org.apache.commons.io.output.NullOutputStream;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;
import org.eclipse.jface.text.IDocument;

import com.github.javaparser.*;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import nlp3code.listeners.QueryDocListener;
import nlp3code.recommenders.TaskRecommender;
import nlp3code.recommenders.TypeRecommender;
import nlp3code.tester.Tester;

public class Evaluator {
	//the global javaCompiler object
	public static JavaCompiler javaCompiler = null;
	//the global in memory compiler
	static public IMCompiler compiler = null;
	//the global parser
	public static JavaParser parser = null;
	//global compiling set
	public static List<Snippet>compilingSnippets = new ArrayList<Snippet>();
	public static List<Snippet>nonCompSnippets = new ArrayList<Snippet>();
	public static int retrieved = 0;
	public static int compiled = 0;
	public static int passed = 0;
	//options
	public static boolean fix = true;
	public static boolean integrate = true;
	public static boolean deletion = true;
	public static boolean targetted = true;
	
	/** 
	 * Returns an ordered list of snippets by quality, after compiling and fixing.
	 */
	public static List<Snippet> evaluate(IProgressMonitor monitor, List<Snippet> snippets, String before, String after){
		//snippets = new ArrayList<Snippet>();
		//test complex types
//		Snippet snippet = new Snippet("import java.util.List;\nimport java.util.ArrayList;\nList<String> list = new ArrayList<String>();\r\n" + 
//				"list.add(\"a\");\r\n" + 
//				"String str = list.get(0);", 0);
		//test timeouts
//		Snippet snippet = new Snippet("int a = 0;\r\n" + 
//				"		boolean test = true;\r\n" + 
//				"		while(test == true) {\r\n" + 
//				"			System.out.println(\"evil loop of death\");\r\n" + 
//				"		}\r\n" + 
//				"		int b = 0;\r\n" + 
//				"		", 0);
//		
//		snippets.add(snippet);
		
		SubMonitor sub = null;
		if(monitor != null) sub = SubMonitor.convert(monitor, 100);
		
		//set up options if first run, using project classPath
		if(compiler == null) compiler = initializeCompiler(false);
		
		//record retrieved
		retrieved = snippets.size();
		passed = 0;
		compiled = 0;
		
		//reset real-time compiling set
		compilingSnippets = new ArrayList<Snippet>();
		
		
		//compile snippet set
		SubMonitor child1 = null;
		if(sub != null) child1 = sub.split(20);
		snippets = compileSnippets(child1, snippets, before, after);
		
		//attempt fixes on snippets
		SubMonitor child2 = null;
		if(sub != null) child2 = sub.split(80);
		if(fix == true) snippets = fixSnippets(child2, snippets, before, after);
		
		//sort snippet set (this uses comparator defined in Snippet class)
		Collections.sort(nonCompSnippets);
		
		//collate final sets
		if(!nonCompSnippets.isEmpty()) {
			compilingSnippets.addAll(nonCompSnippets);
		}
		
		return compilingSnippets;
	}
	
	public static void canTest() {
		//wait to test until evaluation is done
		TypeRecommender.canRecommend = true;
		IDocument document = DocHandler.getDocument();
		document.addDocumentListener(InputHandler.beginTestingListener);
		TypeRecommender.generated = null;
		document.addDocumentListener(InputHandler.typeDocListener);
	}
	
	/**
	 * Compiles the given snippet list.
	 * @return
	 */
	public static List<Snippet> compileSnippets(IProgressMonitor monitor, List<Snippet> snippets, String before, String after){
		SubMonitor sub = null;
		if(monitor != null) sub = SubMonitor.convert(monitor, snippets.size());
		
		//reset number of compiled
		compiled = 0;
		nonCompSnippets = new ArrayList<Snippet>();
		
		Snippet snippet;
		int errors;
		String code;
		for(int i=0; i<snippets.size(); i++) {
			snippet = snippets.get(i);
			
			compiler.clearSaved();
			
			//generate the candidate code
			code = Snippet.insert(snippet, before+after, before.length());
	
			compiler.addSource(DocHandler.getFileName(), code);
			compiler.compileAll();
			
			//get errors
			errors = compiler.getErrors();
			snippet.updateErrors(errors, compiler.getDiagnostics().getDiagnostics());
			
			//if compiles
			if(errors == 0) {
				updateCompiling(snippet);
			}
			else {
				//add to non compiling list
				nonCompSnippets.add(snippet);
			}
			
			//update the result list of this function
			snippets.set(i, snippet);
			
			//update progress on tick
			if(sub != null) sub.split(1);
		}
		
		
		return snippets;
	}
	
	
	/**
	 * Attempts to fix non-compiling snippets.
	 * @return
	 */
	public static List<Snippet> fixSnippets(IProgressMonitor monitor, List<Snippet> snippets, String before, String after){
		SubMonitor sub = null;
		if(monitor != null) sub = SubMonitor.convert(monitor, snippets.size());
		
		//reset the noncomp list
		nonCompSnippets = new ArrayList<Snippet>();
		
		Snippet snippet;
		int errors;
		//for each snippet
		for(int i=0; i<snippets.size(); i++) {
			snippet = snippets.get(i);
			errors = snippet.getErrors();
			
			
			//process snippets with errors
			if(errors > 0) {
				
				//run integration
				if(integrate == true) {
					snippet = Integrator.integrate(snippet, before, after);
					errors = snippet.getErrors();
				}
				
				//Targeted fixes
				if(errors > 0) {
				
					if(targetted == true) {
						snippet = Fixer.errorFixes(snippet, before, after);
						errors = snippet.getErrors();
					}
					
					//if we didn't
					if(errors > 0) {
						
						if(deletion == true) {
							//run deletion
							snippet = Deleter.deletion(snippet, before, after);
							errors = snippet.getErrors();
						}
					}
				}
				
				//if we fixed a snippet
				if(errors == 0) {
					//if empty add an error 
					if(snippet.getLOC() < 1) {
						snippet.updateErrors(1, null);
					}else {
						//otheriwse, update compiling
						updateCompiling(snippet);
					}
				}
			}
			
			if(errors > 0) nonCompSnippets.add(snippet);
			
			snippets.set(i, snippet);
			
			
			//update progress on tick
			if(sub != null) sub.split(1);
		}
		
		return snippets;
	}
	/**
	 * Update the number and queue of compiling snippets.
	 * @param snippet
	 */
	private static void updateCompiling(Snippet snippet) {
		compiled++;
		//update our queue
		compilingSnippets.add(snippet);
		//update inputhandler
		InputHandler.previousSnippets = compilingSnippets;
	}
	
	/**
	 * Ensures that our EclipseCompiler object is created using our patched jar.
	 * See {@link PathClassLoader} for more details.
	 */
	private static void usePatch() {
		//get the url of our patch
		URL url = DataHandler.getURL("lib/ecj-3.18.0_fix.jar");
		
		//use our patch classloader that ensures we load from this file
		ClassLoader classLoader = new PatchClassLoader(new URL[] {url});
		
		//try to load the compiler class
		try {
			Class<?> ecjClass = classLoader.loadClass("org.eclipse.jdt.internal.compiler.tool.EclipseCompiler");
			javaCompiler = (JavaCompiler) ecjClass.newInstance();
		} catch (ClassCastException e) {
			//we may be running from junit, try:
			javaCompiler = new EclipseCompiler();
		} catch (ClassNotFoundException e) {
			//any other exception, exit
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Initialize the compiler.
	 */
	public static IMCompiler initializeCompiler(boolean testing) {
		
		//use patch
		usePatch();
		
		//construct classpath
		String fullClasspath = null;
		
		//get the system classpath
		String systemClasspath = System.getProperty("java.class.path");
		
		//get the open projects classptah
		String projectClasspath = null;
		projectClasspath = DocHandler.getClassPath();
		
		if(testing == true) {
			if(projectClasspath == null) {
				fullClasspath = systemClasspath;
			}
			else {
				fullClasspath = projectClasspath + ";" + systemClasspath;
			}
		}
		else {
			if(projectClasspath == null) {
				fullClasspath = "";
			}
			else {
				fullClasspath = projectClasspath;
			}
		}
		
		//construct options
		List<String> options = null;
		if(fullClasspath != null) {
			options = new ArrayList<String>(Arrays.asList("-classpath", fullClasspath));
		}
		
		//get in memory compiler object
		return new IMCompiler(javaCompiler, options, new OutputStreamWriter(new NullOutputStream()));
	}
	
	/**
	 * Initializes the parser.
	 */
	public static void initializeParser(){
		if(parser != null) return;
		ReflectionTypeSolver reflection = new ReflectionTypeSolver();
		if(DocHandler.currentProject != null) {
			JarTypeSolver jar;
		}
		CombinedTypeSolver solver = new CombinedTypeSolver(reflection);
		
		//ReflectionTypeSolver solver = new ReflectionTypeSolver();
		ParserConfiguration parserConfiguration = new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(solver)); 
		parser = new JavaParser(parserConfiguration);
	}

	public static List<Snippet> testSnippets(IProgressMonitor monitor, List<Snippet> snippets, String before, String after, String test, List<String> imports) {
		SubMonitor sub = null;
		if(monitor != null) sub = SubMonitor.convert(monitor, snippets.size());
		
		passed = 0;
		for(int i=0; i<snippets.size(); i++) {
			//get snippet
			Snippet snippet = snippets.get(i);
			//when the sorted list reaches non-compiling snippets, finish
			if(snippet.getErrors() != 0) break;
			
			//otherwise test and replace
			snippet.setPassedTests(Tester.test(snippet, before, after, test, imports));
			if(snippet.getPassedTests() > 0) {
				passed++;
			}
			snippets.set(i, snippet);
			
			if(sub != null) sub.split(1);
		}
		System.out.println("Compilable test functions: " + Tester.testable);
		Tester.testable = 0;
		
		Collections.sort(snippets);
		
		return snippets;
	}

	/**
	 * Function getClassPath
	 * 	 Constructs the classPath including junit and dependancies.
	 */
	static public String getJUnitClassPath(){
		//there is probably a better way to do this
		String junitDir = null;
		String hamcrestDir = null;
		try {
			URL url = FileLocator.find(Platform.getBundle("nlp3code"), new Path("lib/junit-4.12.jar"), null);
			url = FileLocator.resolve(url);
			junitDir = url.toString().replace("file:/", "");
			//hamcrest jar should be in the same directory
			hamcrestDir = junitDir.replace("junit-4.12.jar", "hamcrest-core-1.3.jar");
		} catch (IOException e) {
			System.err.println("Could not resolve a jar directory.");
			e.printStackTrace();
		}
		
		return junitDir + ";" + hamcrestDir;
		
	}
}
