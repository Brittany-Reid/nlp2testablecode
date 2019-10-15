package nlp2code;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import nlp2code.compiler.IMCompiler;
import nlp2code.compiler.PatchClassLoader;
import nlp2code.tester.Tester;
import nlp2code.tester.TypeRecommendations;
import nlp2code.fixer.Fixer;

/**
 * Evaluator Class
 *   Handles evaluation of snippets and evaluation pipeline.
 */
public class Evaluator{
	static Logger logger = Activator.getLogger();
	static public JavaCompiler javaCompiler = null;
	static public IMCompiler compiler = null;
	static public JavaParser parser = null;
	static public List<String> options;
	static boolean fix = true;
	static boolean test = true;
	
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
		
		//ensure we use the correct eclipse compiler patch
		if(javaCompiler == null) {
			usePatch();
		}
		
		//if we want to compile without any options
		if(useOptions == false) compiler  = new IMCompiler(javaCompiler, null);
		else {
			//get the junit classpath from the plugin
			junit = getJUnitClassPath();
			
			if(classPath == null) {
				fullClassPath = System.getProperty("java.class.path") + ";" + junit;
			}
			else {
				fullClassPath = classPath + ";" + System.getProperty("java.class.path") + ";" + junit;
			}
			
			options = new ArrayList<String>(Arrays.asList("-classpath", fullClassPath));
			//add version
			options.add("--release");
			options.add(Activator.version);
			compiler = new IMCompiler(javaCompiler, options);
		}
	}
	
	/** 
	 * Returns an ordered list of snippets, after compiling, fixing and testing.
	 */
	public static List<Snippet> evaluate(List<Snippet> snippets, String before, String after){
		retrieved = snippets.size();
		
		//set up options if first run, using project classPath
		if(compiler == null) setupOptions(DocumentHandler.getClassPath(), true);
		
		//compile snippet set
		snippets = compileSnippets(snippets, before, after);
		
		//attempt fixes on snippets
		if(fix == true) snippets = fixSnippets(snippets, before, after);
		
		//test snippets
		if(test == true) snippets = generateTypes(snippets, before, after);
		
		//sort snippet set (this uses comparator defined in Snippet class)
		Collections.sort(snippets);
		
		return snippets;
	}
	
	public static List<Snippet> compileSnippets(List<Snippet> snippets, String before, String after){
		Integer errors;
		
		//get the className from code
		findClassName(before+after);
		
		compiled = 0;
		for(int i=0; i<snippets.size(); i++) {
			compiler.clearSaved();
			
			//if we have import statements, get the before with these inserted at top
			String proposedBefore;
			if(snippets.get(i).getImportList().size() > 0) {
				proposedBefore = Snippet.addImportToBefore(snippets.get(i), before);
			}
			else {
				proposedBefore = before;
			}
			
			compiler.addSource(className, proposedBefore+snippets.get(i).getCode()+after);
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
			Snippet snippet = snippets.get(i);
			int errors = snippet.getErrors();
			
			//try fix snippets with errors
			if(errors > 0) {
				
				//if we have import statements, get the before with these inserted
				String proposedBefore;
				if(snippets.get(i).getImportList().size() > 0) {
					proposedBefore = Snippet.addImportToBefore(snippets.get(i), before);
				}
				else {
					proposedBefore = before;
				}
				
				//run integrator
				snippet = Fixer.integrate(snippet, proposedBefore, after);
				snippets.set(i, snippet);
				errors = snippet.getErrors();
				
				//if we still have errors, try targeted fixes
				if(errors > 0) {
					snippet = Fixer.errorFixes(snippet, proposedBefore, after);
					snippets.set(i, snippet);
					errors = snippet.getErrors();
				}
				
				//again, try deletion
				if(errors > 0) {
					//recompute proposed before
					if(snippets.get(i).getImportList().size() > 0) {
						proposedBefore = Snippet.addImportToBefore(snippets.get(i), before);
					}
					snippet = Fixer.deletion(snippet, proposedBefore, after);
					snippets.set(i, snippet);
					errors = snippet.getErrors();
				}
				
				//if we fixed the snippet
				if(errors == 0 && !snippet.getCode().trim().equals("")) {
					compiled++;
				}
			}
			
		}
		
		return snippets;
	}

	public static List<Snippet> generateTypes(List<Snippet> snippets, String before, String after){
		int passedTests = 0;
		passed = 0;
		
		//for each snippet
		for(int i=0; i<snippets.size(); i++) {
			Snippet snippet = snippets.get(i);
			
			//test compilable snippets
			if(snippet.getErrors() == 0) {
				snippet = TypeRecommendations.generate(snippet, before, after);
				if(snippet == null) continue;
				if(snippet.getPassed() > 0) {
					passed++;
				}
				snippets.set(i, snippet);
			}
		}
		
		return snippets;
	}
	
	public static List<Snippet> testSnippets(List<Snippet> snippets, String before, String after, String test){
		
		passed = 0;
		for(int i=0; i<snippets.size(); i++) {
			//get snippet
			Snippet snippet = snippets.get(i);
			//when the sorted list reaches non-compiling snippets, finish
			if(snippet.getErrors() != 0) break;
			
			//otherwise test and replace
			snippet.setPassed(Tester.test(snippet, before, after, test));
			if(snippet.getPassed() > 0) {
				passed++;
			}
			snippets.set(i, snippet);
		}
		
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
	
	/**
	 * Ensures that our EclipseCompiler object is created using our patched jar.
	 * See {@link PathClassLoader} for more details.
	 */
	static public void usePatch() {
		ClassLoader cl;
		try {
			cl = new PatchClassLoader(new URL[] {new URL("platform:/plugin/nlp2code/lib/ecj-3.18.0_fix.jar")});
			Class<?> c1 = cl.loadClass("org.eclipse.jdt.internal.compiler.tool.EclipseCompiler");
			javaCompiler = (JavaCompiler) c1.newInstance();
		} catch(MalformedURLException e) {
			//for testing purposes, platform is plug in dependent and will fail so use a different URL
			try {
				cl = new PatchClassLoader(new URL[] {new File("libs/ecj-3.18.0_fix.jar").toURL()});
				Class<?> c1 = cl.loadClass("org.eclipse.jdt.internal.compiler.tool.EclipseCompiler");
				javaCompiler = (JavaCompiler) c1.newInstance();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * This function sets up a parser with internal classes for type resolution.
	 */
	static public void initializeParser() {
		ReflectionTypeSolver solver = new ReflectionTypeSolver();
		ParserConfiguration parserConfiguration = new ParserConfiguration().setSymbolResolver( new JavaSymbolSolver(solver)); 
		parser = new JavaParser(parserConfiguration);
	}
	
}