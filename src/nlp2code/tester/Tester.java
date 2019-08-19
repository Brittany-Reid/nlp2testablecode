package nlp2code.tester;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import nlp2code.Evaluator;
import nlp2code.QueryDocListener;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParseStart;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StreamProvider;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.Node.TreeTraversal;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithIdentifier;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.expr.SimpleName;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import nlp2code.compiler.*;

/* Class Tester
 * Handles Testing of code snippets through public function test
 */

public class Tester{
	private static String before;
	private static String after;
	
	public static String className;
	private static String classPath;
	
	private static String functionName;
	
	private static IMCompiler compiler;
	private static BlockStmt block;
	
	/**	
	 * Function to test a snippet
	 * Returns the number of passed tests. 
	 */
	public static Integer test(String s, String b, String a, List<String> argumentTypes, String returnType) {
		//DEBUG: test snippet
		//s = "String s = \"1\";\nString b = \"2\" + s;\nint i = 0;\ni = Integer.parseInt(s);\nSystem.out.print(i);\nInteger.parseInt(s);\n";
		
		System.out.println(s);
		
		
		//set up parser with internal classes for type solver
		ReflectionTypeSolver solver = new ReflectionTypeSolver();
		ParserConfiguration parserConfiguration = new ParserConfiguration().setSymbolResolver( new JavaSymbolSolver(solver)); 
		JavaParser parser = new JavaParser(parserConfiguration);
		
		//use a compilation unit so we can resolve, add a comment and brackets so we can find our block statement
		CompilationUnit cu = parser.parse(b + "//snippetbracket\n{\n" + s + "}\n" + a).getResult().get();
		AtomicReference<Node> walkedResult = new AtomicReference<>();
		
		//get our snippet block by walking until we find it
		cu.walk(node -> {
			if(node.getClass() == BlockStmt.class) {
				if(node.getComment().isPresent()) {
					if(node.getComment().get().getContent().equals("snippetbracket")) {
						walkedResult.set(node);
						return; //done
					}
				}
			}
		});
		block = (BlockStmt) walkedResult.get();
	
		//set code fragments
		before = b;
		after = a;
		
		//construct a test function
		//MethodDeclaration methodDeclaration = constructFunction(returnType, argumentTypes);
		MethodDeclaration methodDeclaration = constructMethodDeclaration();
		//cannot construct function
		if(methodDeclaration == null) {
			//System.out.println("Can't construct test function.");
			return 0;
		}
		
		//construct file
		String code = constructFile(methodDeclaration);
		//cannot construct file
		if(code == null) return 0;
		
		//System.out.println(code);
		
		//try to compile our test
		compiler = new IMCompiler(Evaluator.javaCompiler, Evaluator.options);
		IMCompiler.logging = false;
		Integer errors = -1;
		try {
			compiler.addSource(Evaluator.className, code);
			compiler.compileAll();
			errors = compiler.getErrors();
		}catch(Exception e) {
			e.printStackTrace();
		}
		IMCompiler.logging = true;
		
		//if compilation fails, we assume some type mismatch with test and say no passes
		if(errors != 0) {
			System.out.println("Compilation failed, errors: " + errors);
			for (Diagnostic<? extends JavaFileObject> diagnostic : compiler.diagnostics.getDiagnostics()) {
				System.out.println(diagnostic.getMessage(null));
			}
			return 0;
		}
		
		//attempt to run tests
		return run();
		
	}

	
	/**
	 * Constructs a method declaration, determining the arguments
	 * and return type automatically.
	 */
	private static MethodDeclaration constructMethodDeclaration() {
		String returnType = findReturn();
		System.out.println(returnType);
		return null;
	}
	
	
	/**Finds return type automatically*/
	private static String findReturn() {
		//break lambda :)
		AtomicReference<Node> lastStatement = new AtomicReference<>();
		AtomicReference<List<Node>> inLoops = new AtomicReference<>();
		inLoops.set(new ArrayList<>());
		AtomicReference<List<Node>> argumentNodes = new AtomicReference<>();
		argumentNodes.set(new ArrayList<>());
		AtomicReference<List<String>> arguments = new AtomicReference<>();
		arguments.set(new ArrayList<>());
		AtomicReference<Node> lastVar = new AtomicReference<>();
		AtomicReference<String> returnType = new AtomicReference<>();
		String returnString = "void";
		//get a list of statements
		List<Statement> statements = block.getStatements();
		List<Node> nodes = block.getChildNodes();
		
		//walk all nodes within block
		block.walk(TreeTraversal.PREORDER, node -> {
			List<Node> currentArgs = argumentNodes.get();
			List<String> args = arguments.get();
			//get the class of current node
			Class<? extends Node> nodeClass = node.getClass();
			
			//exclude loop vars
			if(nodeClass == WhileStmt.class) {
				WhileStmt w = (WhileStmt) node;
				Expression condition = w.getCondition();
				condition.walk(node2 ->{
					if(node2.getClass() == SimpleName.class) {
						List<Node> loop = inLoops.get();
						loop.add(node2);
						inLoops.set(loop);
					}
				});
			}
			
			//look at the statement level for 
			if(node.getParentNode().isPresent() && node.getParentNode().get().getClass() == ExpressionStmt.class) {
				//variable declarations
				if(nodeClass == VariableDeclarationExpr.class) {
					lastStatement.set(node);
					VariableDeclarationExpr declaration = (VariableDeclarationExpr) node;
					for(VariableDeclarator v : declaration.getVariables()) {
						//arguments should be some var that has a value
						Expression init;
						if(v.getInitializer().isPresent()) {
							init = v.getInitializer().get();
						}
						else {
							continue;
						}
						Boolean independant = true;
						AtomicReference<Boolean> inde = new AtomicReference<>();
						inde.set(true);
						if(init != null) {
							if(init.isNameExpr()) {
								independant = false;
							}
							init.walk(node2 -> {
					            if(node2.getClass() == NameExpr.class) {
					            	inde.set(false);
					            }
					        });
							
							independant = inde.get();
							if(independant == true) {
								currentArgs.add(v);
								args.add(v.getTypeAsString());
							}
						}
					}
				}
				
				//method declarations
				if(nodeClass == MethodCallExpr.class) {
					lastStatement.set(node);
				}
				
				//assignments
				if(nodeClass == AssignExpr.class) {
					lastStatement.set(node);
				}
				
				//add any arguments to our argument set
				argumentNodes.set(currentArgs);
				arguments.set(args);
			}
        });
		
		Node returnNode = lastStatement.get();
		Class<? extends Node> nodeClass = returnNode.getClass();
		if(nodeClass == VariableDeclarationExpr.class) {
			VariableDeclarator var = ((VariableDeclarationExpr)returnNode).getVariable(0);
			returnString = var.getTypeAsString();
		}
		else if(nodeClass == AssignExpr.class) {
			Expression target = ((AssignExpr)returnNode).getTarget();
			try {
				returnString = target.calculateResolvedType().toString();
			}catch(Exception e) {
				returnString = null;
			}
		}
		else if(nodeClass == MethodCallExpr.class) {
			MethodCallExpr methodCall = ((MethodCallExpr)returnNode);
			//if we have a system out we can accept a really common case
			if(methodCall.getScope().isPresent() && methodCall.getScope().get().toString().equals("System.out")) {
				if(methodCall.getNameAsString().equals("print") || methodCall.getNameAsString().equals("println")) {
					ResolvedType type  = methodCall.getArgument(0).calculateResolvedType();
					returnString = processResolvedType(type);
				}
			}
			//otherwise, try to get return type
			else {
				try {
					//resolve method
					ResolvedMethodDeclaration methodDeclaration = methodCall.resolve();
					methodDeclaration.getQualifiedSignature();
					ResolvedType type = methodDeclaration.getReturnType();
					returnString = processResolvedType(type);
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		//search bottom up for acceptable candidate
//		for(int i=nodes.size()-1; i>=0; i--) {
//			Node node = nodes.get(i);
//			System.out.println(i);
//			System.out.println(node);
			
//			Statement statement = statements.get(i);
//			Node node = nodes.get(i);
//			System.out.println(nodes.get(i).toString());
//			
//			
//			
//			if(statement.isExpressionStmt()) {
//				Expression expression = statement.asExpressionStmt().getExpression();
//				
//				//check if expression is a variable declaration
//				VariableDeclarator var = processVariableDeclaration(expression);
//				if(var != null) { 
//					return var.getTypeAsString();
//				}
//				
//				//an assignment
//				Expression target = processAssignment(expression);
//				if(target != null) {
//					try {
//						target.calculateResolvedType().toString();
//					} catch (IllegalStateException e) {
//						continue;
//					}
//				}
//				
//				//a method
//				else if(expression.isMethodCallExpr()) {
//					MethodCallExpr methodCall = expression.asMethodCallExpr();
//					
//					//derive return from print statement
//					if(methodCall.getScope().isPresent() && methodCall.getScope().get().toString().equals("System.out")) {
//						if(methodCall.getNameAsString().equals("print") || methodCall.getNameAsString().equals("println")) {
//							
//							//check if an argument exists
//							if(!methodCall.getArguments().isEmpty()) {
//								try {
//									return null;
//									//return methodCall.getTypeArguments().get().get(0).toString();
//								}
//								catch(Exception e) {
//									e.printStackTrace();
//								}
//							}
//						}
//					}
//					return null;
//					//return methodCall.calculateResolvedType().toString();
//				}
//			}
		
//		}
		
		//print our arguments for now
		String argumentString = "Argument types: ";
		for(int i=0; i<arguments.get().size(); i++) {
			argumentString += arguments.get().get(i) + " ";
		}
		System.out.println(argumentString);
		
		//found none
		return "Return type: " + returnString;
	}
	
	
	/**
	 * Returns the String type of a ResolvedType Object.
	 */
	private static String processResolvedType(ResolvedType resolvedType) {
		String type = null;
		
		if(resolvedType.isReferenceType()) {
			ResolvedReferenceType reference = resolvedType.asReferenceType();
			//attempt to get the class name
			String[] segments = reference.getQualifiedName().split(".");
			if(segments.length > 0) 
				type = segments[segments.length-1];
			else
				type = reference.getQualifiedName();
		}
		else if(resolvedType.isPrimitive()) {
			ResolvedPrimitiveType prim = resolvedType.asPrimitive();
			type = prim.describe();
		}
		
		return type;
	}
	
	/**
	 * Function constructJunit
	 *   Constructs JUnit test function from our in/out pairs
	 */
	private static MethodDeclaration constructJunit() {
		//construct declaration
		MethodDeclaration methodDeclaration = new MethodDeclaration();
		methodDeclaration.setName("junittest");
		methodDeclaration.setPublic(true);
		//methodDeclaration.setStatic(true);
		methodDeclaration.setType("void");
		
		//add test
		methodDeclaration.addMarkerAnnotation("Test");
		
		//construct contents
		BlockStmt blockStmt = new BlockStmt();
		
		//TODO: Need a function that converts input/output info to JavaParser Expressions based on type
		
		//construct call to test function
		MethodCallExpr testMethod = new MethodCallExpr(null, "test");
		//add arguments
		StringLiteralExpr inputString = new StringLiteralExpr(Evaluator.testInput.get(0));
		testMethod.addArgument(inputString);
		
		//construct call to assertEquals
		MethodCallExpr method = new MethodCallExpr(null, "assertEquals");
		//add arguments
		IntegerLiteralExpr outputInteger = new IntegerLiteralExpr(Integer.parseInt(Evaluator.testOutput));
		//need to cast for assert
		CastExpr outputIntegerCast = new CastExpr();
		outputIntegerCast.setType("Integer");
		outputIntegerCast.setExpression(outputInteger);
		method.addArgument(testMethod);
		method.addArgument(outputIntegerCast);
		
		blockStmt.addAndGetStatement(method);
		
		//add contents to method
		methodDeclaration.setBody(blockStmt);
		
		//now we have a test method constructed we could store it for this query and reuse
		return methodDeclaration;
	}
	
	/** 
	 * Function run
	 *   Runs our test case.
	 */
	private static Integer run() {
		setupTestEnvironment();
		
		TestRunner testRunner = new TestRunner(className, classPath, null);
		
		IMClassLoader classLoader = null;
		classLoader = (IMClassLoader) compiler.fileManager.getClassLoader(null);
		
		Integer passed = 0;
		
		//run test runner
		UnitTestResultSet unitTestResultSet = testRunner.runTests(classLoader.getCompiled(className));
		passed = unitTestResultSet.getSuccessful();
		
		
		return passed;
	}
	
	
	/** 
	 * Function constructFunction
	 *   Constructs a function for testing with supplied return and argument types.
	 */
	private static MethodDeclaration constructFunction(String returnType, List<String> argumentTypes) {
		List<String> arguments;

		//get arguments
		arguments = getArguments(argumentTypes);
		//if we couldn't find all arguments, fail
		if(arguments == null) return null;
		
		//get return
		Integer e = addReturn(returnType);
		//couldn't find return, fail
		if(e != 0) return null;
		
		//for now use test but to avoid conflicts check if free
		functionName = "test";
		
		//construct method declaration
		MethodDeclaration methodDeclaration = new MethodDeclaration();
		methodDeclaration.setName(functionName);
		methodDeclaration.setPublic(true);
		methodDeclaration.setStatic(true);
		methodDeclaration.setType(returnType);
		for(int i=0; i<arguments.size(); i++) {
			methodDeclaration.addParameter(argumentTypes.get(i), arguments.get(i));
		}
		
		//add our modified block
		methodDeclaration.setBody(block);
		
		return methodDeclaration;
	}

	
	/* 
	 * Function constructFile
	 *   Builds a new file including test function.
	 */
	private static String constructFile(MethodDeclaration methodDeclaration) {
		
		//Get class name from file
		JavaParser fileParser = new JavaParser();
		ParseResult<CompilationUnit> fileResult = fileParser.parse(before+after);
		CompilationUnit cu = fileResult.getResult().get();
		for (Node childNode : cu.getChildNodes()) {
			if(childNode instanceof ClassOrInterfaceDeclaration) {
				ClassOrInterfaceDeclaration c = (ClassOrInterfaceDeclaration) childNode;
				className = c.getNameAsString();
			}
		}
		
		//construct our file
		CompilationUnit newCu = new CompilationUnit();
		//class declaration
		ClassOrInterfaceDeclaration newC = newCu.addClass(className).setPublic(true);
		//add function
		newC.getMembers().add(methodDeclaration);
		
		//add import for junit
		newCu.addImport("org.junit.Assert.assertEquals", true, false);
		newCu.addImport("org.junit.Test");
		
		//add junit function
		newC.getMembers().add(constructJunit());
		
		return newCu.toString();
	}
	
	/**
	 * Function getArguments
	 *   Parse snippet to find arguments. Return null if this fails.
	 *  
	 *   Currently, number of arguments and their types are supplied.
	 *   We work from the assumption that a snippet declares important variables first.
	 */
	private static List<String> getArguments(List<String> argumentTypes) {
		List<String> arguments = new ArrayList<String>();
		
		//for each argument
		for(int i = 0; i<argumentTypes.size(); i++) {
			VariableDeclarator toRemove = null;
			Statement toRemoveS = null;
			Boolean toBreak = false;
			
			//for each statement
			List<Statement> statements = block.getStatements();
			for(Statement statement : statements) {
				//is our statement an expression
				if(statement.isExpressionStmt()) {
					Expression expression = statement.asExpressionStmt().getExpression();
					//is our expression a variable declaration?
					if(expression.isVariableDeclarationExpr()) {
						//get variables from declaration
						List<VariableDeclarator> vars = ((VariableDeclarationExpr) expression).getVariables();
						//go through all variables
						for(VariableDeclarator v : vars) {
							if(v.getType().toString().equals(argumentTypes.get(i))) {
								//add variable to arguments list
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
			
			if(toRemove != null) toRemove.remove();
			if(toRemoveS != null) toRemoveS.remove();
		}
		
		//each argument type must have a corresponding argument
		if(arguments.size() != argumentTypes.size()) {
			return null;
		}
		
		return arguments;
	}
	
	private static Boolean isType(VariableDeclarator var, String type) {
		String vType = var.getTypeAsString();
		
		//exact match
		if(vType.equals(type)) {
			return true;
		}
		
		//primatives
		if(vType.equals("int") && type.equals("Integer")) return true;
		if(vType.equals("long") && type.equals("Long")) return true;
		if(vType.equals("double") && type.equals("Double")) return true;
	
		//non primatives
		if(vType.equals("Integer") && type.equals("int")) return true;
		if(vType.equals("Long") && type.equals("long")) return true;
		if(vType.equals("Double") && type.equals("double")) return true;
		
		return false;
	}
	
	/* 
	 * Function addReturn
	 * 	 Searches for last statements to find a valid return.
	 * 	 Returns 0 on success.
	 */
	private static Integer addReturn(String type) {
		List<Statement> statements = block.getStatements();
		
		//travel up looking for a return statement
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
						if(isType(vars.get(j), type)){
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
		
		//if we never found a valid return
		return -1;
	}
	
	private static void setupTestEnvironment() {
		//get classpath from iproject: exceptionininitializererror for javacore.create
		//side effect of loading an external jar for jdt.core?
		//instead lets get some default values from the currently open file
		//cacheclassloader contains code to get bin dir for now, migrate out later
		
		//get out original editor
		IEditorPart epart = QueryDocListener.epart;
		//use to get classpath from file
		IFile file = ((IFileEditorInput)epart.getEditorInput()).getFile();
		File actualFile = file.getLocation().toFile();
		classPath = actualFile.getParentFile().getAbsoluteFile().getAbsolutePath();
		
		//add junit
		String junitPath = Evaluator.getJUnitClassPath();
		classPath = classPath + ";" + junitPath;
		
	}
}