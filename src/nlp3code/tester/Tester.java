package nlp3code.tester;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;

import nlp3code.Evaluator;
import nlp3code.InputHandler;
import nlp3code.code.Snippet;
import nlp3code.compiler.IMCompiler;

/** Class Tester
 * Handles Testing of code snippets through calls to the test function.
 */
public class Tester {
	//the name to use for the test function
	public final static String FUNCTIONNAME = "test";
	public final static String JUNITTESTNAME = "nlp3code_test";
	private static String className = "nlp3codeMain";
	private static String junitTest = null;
	
	//types
	public static String returnType = null;
	public static List<String> argTypes = null;
	private static BlockStmt block = null;
	private static NodeList<ImportDeclaration> docImports;
	
	//compile and parser references
	private static IMCompiler compiler = null;
	public static JavaParser parser = null;
	public static int testable = 0;

	/**
	 * Tests a given snippet.
	 * @param snippet The snippet to test.
	 * @param before Code before the snippet.
	 * @param after Code after the snippet.
	 * @param test Test case, body of the test function.
	 * @param imports List of import statements from file.
	 * @param types	Selected return and argument types for constructing function.
	 * @return Number of passed tests (currently only binary 1 or 0).
	 */
	public static int test(Snippet snippet, String before, String after, String test, List<String> imports, List<String> types) {
		//get types
		returnType = types.get(0);
		argTypes = new ArrayList<String>();
		for(int i=1; i<types.size(); i++) {
			argTypes.add(types.get(i));
		}
		
		//initialize parser
		if(parser == null) {
			initializeParser();
		}
		
		//get an ast of the function
		block = getSnippetAST(snippet, before, after);
		if(block == null) return 0;
		
		//collate imports
		List<String> allImports = imports;
		if(allImports == null) allImports = new ArrayList<>();
		if(snippet.getImportList() != null || snippet.getImportList().isEmpty()) {
			allImports.addAll(snippet.getImportList());
		}
		
		//construct file to test
		String code = null;
		try {
			code = constructFile(allImports, test);
		} catch (Exception e) {
			//catch any parsing errors to continue on
			e.printStackTrace();
		}
		if(code == null) return 0;
		
		//System.out.println(code);
		
		//initialize compiler
		if(compiler == null) {
			initializeCompiler();
		}
		Integer errors = -1;
		compiler.clearSaved();
		compiler.addSource(className, code);
		compiler.compileAll();
		errors = compiler.getErrors();
		
		if(errors == 0) {
			testable++;
			int pass = run(code);
			//int pass = 0;
			return pass;
		}else {
//			System.err.println("Error: Problem compiling test! Is JUnit on the project classpath?");
//			for(Diagnostic d : compiler.getDiagnostics().getDiagnostics()) {
//				System.out.println(d.getMessage(null));
//			}
		}
		
		return 0;
	}
	
	/** 
	 * 	Run test.
	 * 	TestProcessRunner will spawn a new process.
	 */
	private static int run(String code) {
		int passed = 0;
		
		try {
			passed = TestProcessRunner.exec(10000, "junittest", className, code);
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			//again... handle errors properly.
			e.printStackTrace();
			return 0;
		}
		
		return passed;
	}
	
	/**
	 * Given a snippet, parses and returns the snippet's AST as a block statement.
	 */
	public static BlockStmt getSnippetAST(Snippet snippet, String before, String after) {
		BlockStmt blockStmt = null;
		
		blockStmt = TypeSuggestions.getSnippetAST(snippet);
		
		return blockStmt;
	}
	
	private static String constructFile(List<String> imports, String test) throws Exception{
		//construct function from snippet
		MethodDeclaration snippet = constructSnippetFunction();
		if(snippet == null) return null;
		
		MethodDeclaration testMethod = constructJunit(test);
		if(testMethod == null) return null;
		
		//construct our file
		CompilationUnit newCu = new CompilationUnit();
		
		//public class
		ClassOrInterfaceDeclaration newC = newCu.addClass(className).setPublic(true);
		
		//add the snippet
		newC.getMembers().add(snippet);
		
		
		boolean junit = false;
		//add import statements
		for(String importStr : imports) {
			ParseResult result = parser.parseImport(importStr);
			if(!result.getResult().isPresent()) {
				continue;
			}
			ImportDeclaration imNode = (ImportDeclaration) result.getResult().get();
			newCu.addImport(imNode);
			if(importStr.contains("junit")) junit = true;
		}
		//if no junit imports add default
		if(junit == false) {
			newCu.addImport("org.junit.Assert.*", true, false);
			newCu.addImport("org.junit.Test");
		}
		
		//add junit function
		newC.getMembers().add(testMethod);
		
		return newCu.toString();
	}
	
	private static MethodDeclaration constructSnippetFunction() {
		//get arguments
		List<String> arguments = getArguments();
		if(arguments == null) return null;
		
		int err = getAndAddReturn();
		if(err != 0) return null;
		
		//construct signature
		MethodDeclaration methodDeclaration = new MethodDeclaration();
		methodDeclaration.setName(FUNCTIONNAME);
		methodDeclaration.setPublic(true);
		methodDeclaration.setStatic(true);
		methodDeclaration.setType(returnType);
		for(int i=0; i<arguments.size(); i++) {
			methodDeclaration.addParameter(argTypes.get(i), arguments.get(i));
		}
		
		//add our body
		methodDeclaration.setBody(block);
		
		return methodDeclaration;
	}
	
	/**
	 * Using the given type information, find arguments from variable declarations within the snippet
	 * and remove them from the working AST.
	 * @return The argument names as a List of Strings.
	 */
	private static List<String> getArguments(){
		List<String> arguments = new ArrayList<String>();
		
		VariableDeclarator toRemove = null;
		Statement toRemoveS = null;
		Boolean toBreak = false;
		
		//for each argument
		for(int i = 0; i<argTypes.size(); i++) {
			//get statements
			List<Statement> statements = block.getStatements();
			
			//search statements that are expression statements
			for(Statement statement : statements) {
				if(statement.isExpressionStmt()) {
					Expression expression = statement.asExpressionStmt().getExpression();
					
					//arguments should be a variable declaration
					if(expression.isVariableDeclarationExpr()) {
						//get variables from declaration
						List<VariableDeclarator> vars = ((VariableDeclarationExpr) expression).getVariables();
						//go through all variables
						for(VariableDeclarator v : vars) {
							if(isType(v, argTypes.get(i))) {
								//add variable name to arguments list
								arguments.add(v.getNameAsString());
								
								//remove to avoid recounting
								if(vars.size() > 1) toRemove = v;
								else toRemoveS = statement;
								
								
								//done for this argument type
								toBreak = true;
								break;
							}
						}
						//break for this argument type
						if(toBreak == true) break;
					}
				}
			}
			
			//remove argument declarations from block
			if(toRemove != null) toRemove.remove();
			if(toRemoveS != null) toRemoveS.remove();
		}
		
		//each argument type must have a corresponding argument
		if(arguments.size() != argTypes.size()) {
			return null;
		}
		
		return arguments;
	}
	
	/**
	 * Using the given type information, adds a return statement for the last candidate variable to the
	 * working AST.
	 * @return 0 on success, -1 if no return was found.
	 */
	private static int getAndAddReturn() {
		//get statements
		List<Statement> statements = block.getStatements();
		
		//travel up looking for a return statement, returns when done
		for(int i = statements.size()-1; i>=0; i--) {
			Statement statement = statements.get(i);
			
			//is statement an expression?
			if(statement.isExpressionStmt()) {
				Expression expression = statement.asExpressionStmt().getExpression();
				
				//1: A variable declaration
				if(expression.isVariableDeclarationExpr()) {
					List<VariableDeclarator> vars = expression.asVariableDeclarationExpr().getVariables();
					//go through list 
					for(int j=vars.size()-1; j>=0; j--) {
						//if matches our type
						if(isType(vars.get(j), returnType)){
							//append a return statement
							ReturnStmt returnStmt = new ReturnStmt((Expression)new NameExpr(vars.get(j).getName()));
							block.addStatement(returnStmt);
							
							return 0;
						}
					}
				}
				
				//2: an assignment
				if(expression.isAssignExpr()) {
					AssignExpr assign = expression.asAssignExpr();
					
					//construct our return statement from target
					ReturnStmt returnStmt = new ReturnStmt(assign.getTarget());
					block.addStatement(returnStmt);
					
					return 0;
				}
				
				//3: a method
				if(expression.isMethodCallExpr()) {
					MethodCallExpr methodCall = expression.asMethodCallExpr();
					
					//3.1 print statement, get argument
					if(methodCall.getScope().isPresent() && methodCall.getScope().get().toString().equals("System.out")) {
						if(methodCall.getNameAsString().equals("print") || methodCall.getNameAsString().equals("println")) {
							
							//check if an argument exists
							if(!methodCall.getArguments().isEmpty()) {
								//construct return statement using
								ReturnStmt returnStmt = new ReturnStmt(methodCall.getArgument(0));
								//append
								block.addStatement(returnStmt);
								return 0;
							}
						}
					}
					
					//3.2 other methods, append return
					else {
						//construct return statement
						ReturnStmt returnStmt = new ReturnStmt(expression);
						
						//remove the call
						statement.remove();
						
						//append the return
						block.addStatement(returnStmt);
						return 0;
					}
				}
			}
		}
		
		//none found
		return -1;
	}
	
	/**
	 *   Constructs JUnit test function from our in/out pairs
	 */
	private static MethodDeclaration constructJunit(String test) {
		//construct declaration
		MethodDeclaration method = new MethodDeclaration();
		method.setName("junittest");
		method.setPublic(true);
		method.setType("void");
		method.addMarkerAnnotation("Test");
		
		//parse user input
		ParseResult result = parser.parseBlock("{" + test + "}");
		if(!result.getResult().isPresent()) return null;
		BlockStmt body = (BlockStmt) result.getResult().get();
		
		//add contents to method
		method.setBody(body);
				
		return method;
	}
	
	/**
	 * Checks if two elements are the same type, including primitive vs non-primitive.
	 */
	private static Boolean isType(VariableDeclarator var, String type) {
		String vType = var.getTypeAsString();
		
		//exact match
		if(vType.equals(type)) {
			return true;
		}
		
		//primitives
		if(vType.equals("int") && type.equals("Integer")) return true;
		if(vType.equals("long") && type.equals("Long")) return true;
		if(vType.equals("double") && type.equals("Double")) return true;
	
		//non primitives
		if(vType.equals("Integer") && type.equals("int")) return true;
		if(vType.equals("Long") && type.equals("long")) return true;
		if(vType.equals("Double") && type.equals("double")) return true;
		
		return false;
	}
	
	/**
	 * Initialize the parser on first use.
	 */
	private static void initializeParser() {
		//initialize the global parser if not already
		if(Evaluator.parser == null) {
			Evaluator.initializeParser();
		}
		//use global parser
		parser = Evaluator.parser;
	}
	
	private static void initializeCompiler() {
		//use full classpath
		compiler = Evaluator.initializeCompiler(true);
	}
	
	/**Returns String classpath for testing: this is the classpath we use for our cache*/
	public static String getClassPath() {
		//get classpath from iproject: exceptionininitializererror for javacore.create
		//side effect of loading an external jar for jdt.core?
		//update: yes it was, its fixed now
		//instead lets get some default values from the currently open file
		//cacheclassloader contains code to get bin dir for now, migrate out later
		//update: i dont want to mess with this right now lol
		//todo: try out the same classpath used for the compiler
		
		String classPath = null;
		
		//get out original editor
		IEditorPart epart = InputHandler.editor;
		if(epart == null) {
			System.err.println("Error: No editor was saved in memory.");
			//throw new NullPointerException();
			return Evaluator.getJUnitClassPath();
		}
		
		//use to get classpath from file
		IFile file = ((IFileEditorInput)epart.getEditorInput()).getFile();
		File actualFile = file.getLocation().toFile();
		classPath = actualFile.getParentFile().getAbsoluteFile().getAbsolutePath();
		
		//add junit
		String junitPath = Evaluator.getJUnitClassPath();
		classPath = classPath + ";" + junitPath;
		
		return classPath;
	}

}
