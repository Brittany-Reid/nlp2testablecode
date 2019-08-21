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
	static public IMCompiler compiler;
	static public List<String> options;
	static boolean fix = true;
	
	static public String className;
	static private HashMap<String, DiagnosticCollector<JavaFileObject>> diagnosticMap;
	public static String testOutput;
	public static List<String> testInput;
	
	static public Integer passed = 0;
	static public Integer compiled = 0;
	static public Integer retrieved = 0;
	
	/**
	 * Sets up options for the compiler.
	 */
	public static void setupOptions(String classPath, Boolean useOptions) {
		String fullClassPath;
		String junit;
		
		if(useOptions == false) compiler  = new IMCompiler(javaCompiler, null);
		else {
			junit = getJUnitClassPath();
			
			if(classPath == null) {
				fullClassPath = System.getProperty("java.class.path") + ";" + junit;
			}
			else {
				fullClassPath = classPath + ";" + System.getProperty("java.class.path") + ";" + junit;
			}
			
			options = Arrays.asList("-classpath", fullClassPath);
			compiler = new IMCompiler(javaCompiler, options);
		}
	}
	
	/* Returns an ordered vector of snippets
	 * Based on evaluation metrics. */
	public static List<Snippet> evaluate(List<Snippet> snippets, String before, String after){
		retrieved = snippets.size();
		
		//set up options if first run
		if(options == null) setupOptions(null, false);
		
		//compile snippet set
		snippets = compileSnippets(snippets, before, after);
		
		//attempt fixes on snippet fix
		if(fix == true) snippets = fixSnippets(snippets, before, after);
		
		//sort snippet set (this uses comparator defined in Snippet class)
		Collections.sort(snippets);
		
		return snippets;
		
//		
//		Vector<String> snippets = new Vector<String>();
//		for(Snippet s : snippetsx) {
//			snippets.add(s.getFormattedCode());
//		}
//		
//		Long start;
//		Vector<String> orderedByEvaluation;
//		List<String> sorted;
//		retrieved = snippets.size();
//		
//		//set up a new diagnostic map
//		diagnosticMap = new HashMap<String, DiagnosticCollector<JavaFileObject>>();
//		
//		//compile snippets and get map of snippets to errors
//		start = System.currentTimeMillis();
//		compilerErrors = getCompilerErrors(snippets, before, after);
//		
//		//attempt to fix snippets with errors
//		if(fix == false) {
//			start = System.currentTimeMillis();
//			fixedErrors = runFixes(compilerErrors, before, after);
//			//overwrite compilerErrors map with new values
//			compilerErrors = fixedErrors;
//			System.out.println("Fix Time: " + (System.currentTimeMillis() - start));
//		}
//		
//		//set our finals for the compare function
//		final HashMap<String, Integer> finalErrors = compilerErrors;
//		
//		start = System.currentTimeMillis();
//		//final HashMap<String, Integer> passedTests = getPassedTests(compilerErrors, before, after);
//		final HashMap<String, Integer> passedTests = new HashMap<String, Integer>();
//		
//		start = System.currentTimeMillis();
//		sorted = new ArrayList<String>(compilerErrors.keySet());
//		Collections.sort(sorted, new Comparator<String>() {
//		    public int compare(String left, String right) {
//		    	Integer compare = Integer.compare(finalErrors.get(left), finalErrors.get(right));
////		    	//if tied, look at the passed tests
//		    	if(compare == 0 && !passedTests.isEmpty()) {
//		    		compare = Integer.compare(passedTests.get(right), passedTests.get(left));
//		    	}
//		    	return compare;
//		    }
//		});
	}
	
	public static List<Snippet> compileSnippets(List<Snippet> snippets, String before, String after){
		Integer errors;
		
		//get the className from code
		findClassName(before+after);
		
		compiled = 0;
		for(int i=0; i<snippets.size(); i++) {
			compiler.clearSaved();
			
			compiler.addSource(className, before+snippets.get(i).getCode()+after);
			compiler.compileAll();
			
			//get errors
			errors = compiler.getErrors();
			
			//update information
			if(errors == 0) compiled++;
			snippets.get(i).updateErrors(errors, compiler.getDiagnostics().getDiagnostics());
		}
		
		
		return snippets;
	}

	public static List<Snippet> fixSnippets(List<Snippet> snippets, String before, String after){
		
		//for each snippet
		for(int i=0; i<snippets.size(); i++) {
			int errors = snippets.get(i).getErrors();
			
			//try fix snippets with errors
			if(errors > 0) {
				//overwrite current snippet with result of fix
				snippets.set(i, Fixer.deletion(snippets.get(i), before, after));
				errors = snippets.get(i).getErrors();
				
				//if we fixed 
				if(errors == 0) {
					compiled++;
				}
			}
			
		}
		
		return snippets;
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
					//snippet = Fixer.tryDeletion(b, snippet, a, errors);
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
	
	/**
	 * Finds class name from given String, sets the global classname to this value.
	 * @param code The code to extract className from.
	 */
	static public void findClassName(String code) {
		//get className from surrounding code
		JavaParser fileParser = new JavaParser();
		ParseResult<CompilationUnit> fileResult = fileParser.parse(code);
		CompilationUnit cu = fileResult.getResult().get();
		for (Node childNode : cu.getChildNodes()) {
			if(childNode instanceof ClassOrInterfaceDeclaration) {
				ClassOrInterfaceDeclaration c = (ClassOrInterfaceDeclaration) childNode;
				className = c.getNameAsString();
			}
		}
	}
	
}