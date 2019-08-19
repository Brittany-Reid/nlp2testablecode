package nlp2code;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;

import org.apache.logging.log4j.Logger;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import nlp2code.compiler.IMCompiler;
import nlp2code.tester.Tester;

/**
 * Evaluator Class
 *   Handles evaluation of snippets and evaluation pipeline.
 */
public class Evaluator{
	static Logger logger = Activator.getLogger();
	static public JavaCompiler javaCompiler = new EclipseCompiler();
	static private IMCompiler compiler;
	static public List<String> options;
	static boolean fix = true;
	
	static public String className;
	static private HashMap<String, DiagnosticCollector<JavaFileObject>> diagnosticMap;
	public static String testOutput;
	public static List<String> testInput;
	
	static public Integer passed;
	static public Integer compiled;
	static public Integer retrieved;
	
	/**
	 * Sets up options for the compiler.
	 */
	public static void setupOptions(String classPath) {
		String fullClassPath;
		String junit = getJUnitClassPath();
		
		if(classPath == null) {
			fullClassPath = System.getProperty("java.class.path") + ";" + junit;
		}
		else {
			fullClassPath = classPath + ";" + System.getProperty("java.class.path") + ";" + junit;
		}
		
		options = Arrays.asList("-classpath", fullClassPath);
	}
	
	/* Returns an ordered vector of snippets
	 * Based on evaluation metrics. */
	public static Vector<String> evaluate(Vector<String> snippets, String before, String after){
		Long start;
		HashMap<String, Integer> compilerErrors, fixedErrors;
		Vector<String> orderedByEvaluation;
		List<String> sorted;
		retrieved = snippets.size();
		
		//set up options if first run
		if(options == null) setupOptions(null);
		
		//set up a new diagnostic map
		diagnosticMap = new HashMap<String, DiagnosticCollector<JavaFileObject>>();
		
		//compile snippets and get map of snippets to errors
		start = System.currentTimeMillis();
		compilerErrors = getCompilerErrors(snippets, before, after);
		
		//attempt to fix snippets with errors
		if(fix == true) {
			start = System.currentTimeMillis();
			fixedErrors = runFixes(compilerErrors, before, after);
			//overwrite compilerErrors map with new values
			compilerErrors = fixedErrors;
			System.out.println("Fix Time: " + (System.currentTimeMillis() - start));
		}
		
		//set our finals for the compare function
		final HashMap<String, Integer> finalErrors = compilerErrors;
		
		start = System.currentTimeMillis();
		//final HashMap<String, Integer> passedTests = getPassedTests(compilerErrors, before, after);
		final HashMap<String, Integer> passedTests = new HashMap<String, Integer>();
		
		start = System.currentTimeMillis();
		sorted = new ArrayList<String>(compilerErrors.keySet());
		Collections.sort(sorted, new Comparator<String>() {
		    public int compare(String left, String right) {
		    	Integer compare = Integer.compare(finalErrors.get(left), finalErrors.get(right));
//		    	//if tied, look at the passed tests
		    	if(compare == 0 && !passedTests.isEmpty()) {
		    		compare = Integer.compare(passedTests.get(right), passedTests.get(left));
		    	}
		    	return compare;
		    }
		});
		
		orderedByEvaluation = new Vector<String>(sorted);
		
		return orderedByEvaluation;
	}
	
	/*Returns a hashmap of code snippets to compiler errors*/
	private static HashMap<String, Integer> getCompilerErrors(Vector<String> snippets, String b, String a){
		HashMap<String, Integer> compilerErrors = new HashMap<String, Integer>();
		
		//get className from surrounding
		JavaParser fileParser = new JavaParser();
		ParseResult<CompilationUnit> fileResult = fileParser.parse(b+a);
		CompilationUnit cu = fileResult.getResult().get();
		for (Node childNode : cu.getChildNodes()) {
			if(childNode instanceof ClassOrInterfaceDeclaration) {
				ClassOrInterfaceDeclaration c = (ClassOrInterfaceDeclaration) childNode;
				className = c.getNameAsString();
			}
		}
		compiler  = new IMCompiler(javaCompiler, null);
		compiled = 0;
		for(String s : snippets) {

			//it would be here that we make sure to compile multiple files
			compiler.addSource(className, b+s+a);
			compiler.compileAll();
			
			Integer errorCount = compiler.getErrors();
			if(errorCount == 0) compiled++;
			
			//add snippet to hashmaps
			compilerErrors.put(s, errorCount);
			diagnosticMap.put(s, compiler.getDiagnostics());
			
			compiler.clearSaved();
		}
		
		return compilerErrors;
	}

	/**
	 * For snippets with compiler errors, tries to fix. 
	 * Returns modified Map.
	 */
	private static HashMap<String, Integer> runFixes(HashMap<String, Integer> snippets, String b, String a){
		HashMap<String, Integer> fixedMap = new HashMap<String, Integer>();
		
		for(String snippet : snippets.keySet()) {
			Integer errors = snippets.get(snippet);
			
			//only try to fix snippets with errors
			if(errors != 0) {
				//snippet = Fixer.heuristicFixes(b, snippet, a, errors);
				//errors = Fixer.getLastFixErrorCount();
				
				if(errors != 0) {
					//line deletion
					snippet = Fixer.tryDeletion(b, snippet, a, errors);
					System.out.println("done");
					errors = Fixer.getLastFixErrorCount();
				}
			}
			
			fixedMap.put(snippet, errors);
			
		}
		
		return fixedMap;
	}
	
	private static HashMap<String, Integer> getPassedTests(HashMap<String, Integer> snippets, String b, String a){
		HashMap<String, Integer> passedMap = new HashMap<String, Integer>();
		
//		if(QueryDocListener.testInput == null || QueryDocListener.testInput.size() < 4) {
//				System.out.println("Cannot generate test cases for this task.");
//				return passedMap;
//		}
//		//!InputHandler.previous_query.equals("convert string to integer")
//		
//		List<String> argumentTypes = new ArrayList<String>();
//		testInput = new ArrayList<String>();
//		String returnType;
//		
//		//get arguments
//		for(int i=0; i<QueryDocListener.testInput.size()-2; i++) {
//			if(i % 2 == 0) {
//				argumentTypes.add(QueryDocListener.testInput.get(i));
//			}
//			else {
//				testInput.add(QueryDocListener.testInput.get(i));
//			}
//		}
//		
//		//get return type
//		returnType = QueryDocListener.testInput.get(QueryDocListener.testInput.size()-2);
//		testOutput = QueryDocListener.testInput.get(QueryDocListener.testInput.size()-1);
		
		compiled = 0;
		passed = 0;
		
		for(String s : snippets.keySet()) {
			Integer passCount = 0;
			
			//if we had no compiler errors and the snippet isnt empty, try testing
			if(snippets.get(s) == 0 && s.split("\n").length > 1) {
				compiled++;
				passCount = Tester.test(s, b, a, null, null);
				//System.out.println("Passed: " + passCount);
				if(passCount > 0) {
					passed++;
				}
			}
			
			//add to pass hashmap
			passedMap.put(s, passCount);
		}
		
		return passedMap;
	}
	
	static public DiagnosticCollector<JavaFileObject> getDiagnostics(String snippet) {
		return diagnosticMap.get(snippet);
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
			URL url = FileLocator.find(Platform.getBundle("nlp2code"), new Path("lib/junit-4.12.jar"), null);
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