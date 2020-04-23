package nlp2testablecode.fixer;

import java.util.List;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

import nlp2testablecode.DocHandler;
import nlp2testablecode.Evaluator;
import nlp2testablecode.InputHandler;
import nlp2testablecode.code.Snippet;
import nlp2testablecode.compiler.IMCompiler;

public class Integrator {
	private static JavaParser parser = null;
	private static IMCompiler compiler = null;
	//snippet kinds
	public static final int SNIPPET = 0;
	public static final int METHOD = 1;
	public static final int CLASS = 2;
	public static final int MULTIMETHOD = 3;
	public static final int MULTICLASS = 4;
	public static BodyDeclaration body;
	
	/**
	 * Removes and shifts elements based on context.
	 */
	public static Snippet integrate(Snippet snippet, String before, String after) {
		//we currently can only integrate for functions
		if(!(InputHandler.insertionContext == InputHandler.MAIN || InputHandler.insertionContext == InputHandler.FUNCTION)) return snippet;
		
		//initialize parser
		if(parser == null) initializeParser();
		
		//configure compiler
		if(compiler == null) compiler = Evaluator.compiler;
		
		//get the context
		int context = Integrator.getType(snippet);
		
		//if just a snippet, make no changes
		if(context == SNIPPET) return snippet;
		
		//otherwise
		Snippet copy = new Snippet(snippet);
		
		//handle method
		if(context == METHOD) {
			copy = Integrator.integrateMethod(copy, before, after);
		}
		//handle class
		else if(context == CLASS) {
			copy = Integrator.integrateClass(copy, before, after);
		}
		else {
			copy = null;
		}
		
		//compile
		if(copy != null) {
			int errors = snippet.getErrors();
			//compile
			compiler.clearSaved();
			compiler.addSource(DocHandler.getFileName(), Snippet.insert(copy, before+after, before.length()));
			
			compiler.compileAll();
			int testErrors = compiler.getErrors();
			if(testErrors < errors) {
				copy.updateErrors(testErrors, compiler.getDiagnostics().getDiagnostics());
				
				//set snippet to copy
				snippet = copy;
			}
			else {
				//System.out.println(snippet.getCode());
			}
		}
		
		//return snippet
		return snippet;
	}

	/**
	 * Returns the type of snippet, one of SNIPPET, METHOD or CLASS.
	 */
	public static int getType(Snippet snippet) {
		String code = snippet.getCode();
		
		ParseResult result;
		
		//parse as body declaration, methods and classes
		CompilationUnit cu;
		result = parser.parseBodyDeclaration(code);
		if(result.getResult().isPresent()) {
			body = (BodyDeclaration<?>) result.getResult().get();
			
			//contains a class
			if(body.isClassOrInterfaceDeclaration()) {
				return CLASS;
			}
			
			//contains methods
			List<MethodDeclaration> methods = body.findAll(MethodDeclaration.class);
			if(methods != null && methods.size() == 1) {
				return METHOD;
			}
		}
		
		//add class and try
		result = parser.parseBodyDeclaration("class A{\n" + code + "}\n");
		if(result.getResult().isPresent()) {
			body = (BodyDeclaration) result.getResult().get();
			List<MethodDeclaration> methods = body.findAll(MethodDeclaration.class);
			if(methods != null && methods.size() == 1) {
				return METHOD;
			}
		}

		
		return 0;
	}
	
	
	/**
	 * This function returns if a passed method is main.
	 * @param method A MethodDeclaration object to check.
	 * @return True or false.
	 */
	public static boolean isMain(MethodDeclaration method) {
		
		//public
		if(!method.isPublic()) return false;
		
		//static
		if(!method.isStatic()) return false;
		
		//void
		if(!method.getType().isVoidType()) return false;
		
		//main
		if(!method.getNameAsString().equals("main")) return false;
		
		//(String[] args)
		NodeList<Parameter> parameters = method.getParameters();
		if(parameters != null && parameters.size() == 1) {
			if(parameters.get(0).getType().asString().equals("String[]")){
				if(parameters.get(0).getNameAsString().equals("args")) {
					return true;
				}
			}
			
		}
		
		return false;
	}
	
	/**
	 * Integrates a function.
	 */
	public static Snippet integrateMethod(Snippet snippet, String before, String after) {
		List<MethodDeclaration> methods = body.findAll(MethodDeclaration.class);
		//handle main function
		if(isMain(methods.get(0))) {
			snippet = integrateMain(snippet, before, after);
			return snippet;
		}
		return null;
	}
	
	/**
	 * Given a non-compiling snippet containing a class, will integrate the snippet.
	 */
	public static Snippet integrateClass(Snippet snippet, String before, String after) {
		ClassOrInterfaceDeclaration c = body.asClassOrInterfaceDeclaration();
		
		//process fields
		List<FieldDeclaration> fields = c.getFields();
		if(fields != null && fields.size() > 0) {
			//for now, ignore
			return null;
		}
		
		//process methods
		List<MethodDeclaration> methods = c.getMethods();
		if(methods == null || methods.isEmpty()) return null;
		
		//a single method
		if(methods.size() == 1) {
			//pass to the integrate method class
			snippet = integrateMethod(snippet, before, after);
			return snippet;
		}
		
		return null;
	}
	
	public static Snippet integrateMain(Snippet snippet, String before, String after) {
		String code = null;
		//integerate main into main
		if(InputHandler.insertionContext == InputHandler.MAIN) {
			code = integrateMainIntoMain(snippet);
		}
		
		if(code != null) {
			snippet.setCode(code);
			return snippet;
		}
		return null;
	}
	
	/**
	 * Returns a String of code intended to be inserted within a main function.
	 * Strips the method declaration and return statement.
	 */
	private static String integrateMainIntoMain(Snippet snippet) {
		List<MethodDeclaration> methods = body.findAll(MethodDeclaration.class);
		MethodDeclaration main = null;
		String code = "";
		
		//find the main method
		for(MethodDeclaration method : methods) {
			if(isMain(method)) {
				main = method;
			}
		}
		if(main == null) return null;
		
		BlockStmt body = null;
		if(main.getBody().isPresent()) {
			body = main.getBody().get();
		}
		if(body == null) return null;
		
		for(Statement statement : body.getStatements()) {
			if(!statement.isReturnStmt()) {
				if(statement.getComment().isPresent()) {
					code += statement.getComment().get().toString();
				}
				code += statement.toString();
			}
		}
		
		return code;
	}
	
	/**
	 * Initializes the parser.
	 */
	public static void initializeParser(){
		if(parser != null) return;
		if(Evaluator.parser == null) Evaluator.initializeParser();
		parser = Evaluator.parser;
	}
}
