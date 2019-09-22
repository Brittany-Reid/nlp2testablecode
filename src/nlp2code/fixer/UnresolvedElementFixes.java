package nlp2code.fixer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import nlp2code.Evaluator;
import nlp2code.QueryDocListener;
import nlp2code.Snippet;
import nlp2code.compiler.IMCompiler;

/**
 * This class contains fixes for solving unresolved element errors.
 */
public class UnresolvedElementFixes {
	private static Map<String, List<String>> classCache = new HashMap<>(); //cache of short to qualified names
	private static String[] types = {"Integer", "String", "Character", "Boolean", "Double", "Float", "Long"}; //common types to brute force
	public static JavaParser parser;
	private static String before;
	private static String after;
	
	/**
	 * Fix unresolved. Using information from expression we can defer to other unresolved functions.
	 */
	public static Snippet fixUnresolved(Snippet snippet, Diagnostic<? extends JavaFileObject> diagnostic, int offset, String b, String a) {
		before = b;
		after = a;
		
		//get name from code
		String name = Fixer.getCovered(snippet.getCode(), diagnostic.getStartPosition(), diagnostic.getEndPosition(), offset);
		//if the name is lowercase, resolve as variable
		if(!Character.isUpperCase(name.trim().charAt(0))) {
			snippet = fixUnresolvedVariable(snippet, diagnostic, offset, before, after);
		}
		
		return snippet;
	}
	
	/**
	 * Function to fix unresolved types.
	 */
	public static Snippet fixUnresolvedType(Snippet snippet, Diagnostic<? extends JavaFileObject> diagnostic, int offset, String before, String after) {
		String type = Fixer.getCovered(snippet.getCode(), diagnostic.getStartPosition(), diagnostic.getEndPosition(), offset);
		
		snippet = addImportFor(snippet, type);
		
		return snippet;
	}
	
	/**
	 * Finds and adds an import statement for a given type.
	 */
	private static Snippet addImportFor(Snippet snippet, String type) {
		String importStatement = commonTypes(type);
		if(importStatement == null) {
		
			List<String> packages = findPackagesForType(type);
				
			//if we couldn't find a type, no change
			if(packages == null || packages.isEmpty()) return null;
			
			
			//sort so java packages come first
			Collections.sort(packages, new Comparator<String>() {
			    @Override
			    public int compare(String o1, String o2) {
			    	//preference java
			    	if(o1.startsWith("java") && !o2.startsWith("java")) {
			    		return -1;
			    	}
			    	else if(o2.startsWith("java") && !o1.startsWith("java")) {
			    		return 1;
			    	}
			    	//preference utils
			    	else {
			    		if(o1.contains(".util.") && !o2.contains(".util.")) {
			    			return -1;
			    		}
			    		if(o2.contains(".util.") && !o1.contains(".util.")) {
			    			return 1;
			    		}
			    	}
			        return o1.compareTo(o2);
			    }
			});
			importStatement = packages.get(0);
		}
		
		snippet.addImportStatement("import " + importStatement + ";");
		return snippet;
	}

	/**
	 * Function to fix an unresolved variable.
	 */
	public static Snippet fixUnresolvedVariable(Snippet snippet, Diagnostic<? extends JavaFileObject> diagnostic, int offset, String b, String a) {
		before = b;
		after = a;
		
		//set up parser with internal classes for type solver
		ReflectionTypeSolver solver = new ReflectionTypeSolver();
		ParserConfiguration parserConfiguration = new ParserConfiguration().setSymbolResolver( new JavaSymbolSolver(solver)); 
		parser = new JavaParser(parserConfiguration);
		
		//parse our code
		CompilationUnit cu = parser.parse(before + snippet.getCode() + after).getResult().get();
		
		//find node in ast
		Statement nodeStatement = getStatementFromLine(cu, (int)diagnostic.getLineNumber());
		if(nodeStatement == null) return snippet;
		
		//check that the statement contains the variable name, then get the expression containing it
		String name = Fixer.getCovered(snippet.getCode(), diagnostic.getStartPosition(), diagnostic.getEndPosition(), offset);
		Expression expression = findContainingInStatement(name, nodeStatement);
		if(expression == null) return snippet;
		
		//get the type
		String type = extractTypeFromExpression(expression, name);
		//if we were unable to do this, try a default object
		
		//if we failed to get a type, brute force 
		if(type == null) {
			Snippet test = tryDefaults(snippet, name, nodeStatement.getBegin().get().line-before.split("\n").length);
			if(test != null) return test;
		}
		
		//for complex types, try to gather information from variable name
		if(type == null) {
			type = typeFromName(name);
			if(type != null) {
				//add import
				snippet = addImportFor(snippet, type);
			}
		}
		
		
		//otherwise, just use object
		if(type == null) type = "Object";
		
		String variableDeclaration = getVariableDeclaration(type, name);
		int beforeLines = before.split("\n").length;
		String code = Fixer.addLineAt(snippet.getCode(), variableDeclaration, nodeStatement.getBegin().get().line-beforeLines);
		snippet.setCode(code);
		
		return snippet;
	}
	
	/**
	 * Checks if the variable name contains type information. Sometimes
	 * a variable name may just be a lowercase version of the type.
	 * @param name
	 * @return
	 */
	private static String typeFromName(String name) {
		//theoretical type
		String type = StringUtils.capitalize(name);
		//look for this type in packages
		List<String> packages = findPackagesForType(type);
		
		//if there exists a package for this name
		if(packages != null) {
			//for now just return
			return type;
		}
		
		return null;
	}
	
	private static Snippet tryDefaults(Snippet snippet, String name, int pos) {
		
		//try each type
		for(String type : types) {
			//construct code with this type
			Snippet test = new Snippet(snippet);
			String variableDeclaration = getVariableDeclaration(type, name);
			String code = Fixer.addLineAt(test.getCode(), variableDeclaration, pos);
			test.setCode(code);
			
			//compile
			IMCompiler compiler = Fixer.compiler;
			compiler.clearSaved();
			compiler.addSource(Evaluator.className, before+test.getCode()+after);
			compiler.compileAll();
			int errors = compiler.getErrors();
			
			//compare
			if(errors < snippet.getErrors()) {
				return test;
			}
		}
		
		return null;
	}
	
	/**
	 * Given a String type, returns a list of possible string packages.
	 * @param type The String type.
	 * @return A list of possible packages.
	 */
	public static List<String> findPackagesForType(String type) {
		List<String> packages = null;
		if(!classCache.isEmpty()) {
			packages = classCache.get(type);
		}
		if(classCache.isEmpty()) {
			packages = new ArrayList();
			
			//get the java project from open editor
			IEditorInput input2 = QueryDocListener.editorPart.getEditorInput();
			IResource file2 = ((IFileEditorInput)input2).getFile();
			IProject pp = file2.getProject();
			IJavaProject jp = (IJavaProject) JavaCore.create(pp);
			
			//search through all elements on the classpath
			try {
				IPackageFragmentRoot[] roots = jp.getPackageFragmentRoots();
				for (int i = 0; i < roots.length; i++) {
					roots[i].open(null);
					for(IJavaElement child : roots[i].getChildren()) {
						if (child.getElementType()==IJavaElement.PACKAGE_FRAGMENT) {
							IPackageFragment packageFragment = (IPackageFragment) child;
							IClassFile[] classFiles = packageFragment.getAllClassFiles();
							
							//for all classfiles in a package
							for(IClassFile file : classFiles) {
								//confirm this is an ordinary class file we can get a type from
								if(file instanceof IOrdinaryClassFile) {
									//convert to an ocf
									IOrdinaryClassFile ocf = (IOrdinaryClassFile) file;
									//get the type object
									IType typeObject = ocf.getType();
									
									//if type is not anonymous and is public
									if(!typeObject.isAnonymous() && Flags.isPublic(typeObject.getFlags())) {
										//if the cache doesnt already contain this type
										if(!classCache.containsKey(typeObject.getElementName())) {
											List<String> typePackages = new ArrayList<>();
											typePackages.add(typeObject.getFullyQualifiedName());
											classCache.put(typeObject.getElementName(), typePackages);
										}
										else {
											List<String> typePackages = classCache.get(typeObject.getElementName());
											typePackages.add(typeObject.getFullyQualifiedName());
											classCache.put(typeObject.getElementName(), typePackages);
										}
										
										//if matches our type
										if(type.equals(typeObject.getElementName())){
											packages.add(typeObject.getFullyQualifiedName());
										}
										
									}
								}
							}
							
						}
							
							
					}
				}
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return packages;
	}
	
	/**
	 * Some common types and their packages.
	 * @param type
	 * @return
	 */
	private static String commonTypes(String type) {
		switch(type) {
			case "List":
				return "java.util.List";
			case "ArrayList":
				return "java.util.ArrayList";
			default:
				return null;
		}
	}
	
	/**
	 * Function to reset the cache. Currently unused but the intention is to clear the cache
	 * whenever the user updates their classPath / if we can't reliably listen for this whenever
	 * possible for this to happen.
	 */
	public static void clearCache() {
		classCache = new HashMap<>();
	}
	
	/**
	 * Given a ResolvedType, will return a String representation of the Type.
	 * @param resolvedType the ResolvedType to convert.
	 * @return The String representation of the Type.
	 */
	public static String processResolvedType(ResolvedType resolvedType) {
		String type = null;
		
		//primitive types
		if(resolvedType.isPrimitive()) {
			ResolvedPrimitiveType primitive = resolvedType.asPrimitive();
			type = primitive.describe();
		}
		//reference types
		else if(resolvedType.isReferenceType()) {
			ResolvedReferenceType reference = resolvedType.asReferenceType();
			String qualifiedName = reference.getQualifiedName();
			//attempt to get the class name by splitting into segments
			String[] segments = qualifiedName.split("\\.");
			if(segments.length > 0) {
				type = segments[segments.length-1];
			}
			else
				type = qualifiedName;
		}
		
		return type;
	}
	
	public static String getVariableDeclaration(String type, String name) {
		Type varType = parser.parseType(type).getResult().get();
		
		//get a variable declaration
		VariableDeclarationExpr declaration = new VariableDeclarationExpr(varType, name);
		ExpressionStmt statement = new ExpressionStmt(declaration);
		String line = statement.toString();
		
		//get an initial value based on type
		Expression value = getDefaultValue(type);
		//construct an assignment
		AssignExpr assignment = new AssignExpr(declaration.getVariable(0).getNameAsExpression(), value, AssignExpr.Operator.ASSIGN);
		statement = new ExpressionStmt(assignment);
		line = line + "\n" + statement;

		return line;
	}
	
	/**
	 * Returns a default value expression for a given String type.
	 */
	public static Expression getDefaultValue(String type) {
		Expression value = null;
		
		switch(type) {
			case "int":
			case "Integer":
				value = new IntegerLiteralExpr();
				break;
			case "String":
				value = new StringLiteralExpr();
				break;
			case "Character":
			case "char":
				value = new CharLiteralExpr();
				break;
			case "Boolean":
			case "boolean":
				value = new BooleanLiteralExpr();
				break;
			case "Float":
			case "float":
			case "Double":
			case "double":
				value = new DoubleLiteralExpr();
				break;
			case "Long":
			case "long":
				value = new LongLiteralExpr();
				break;
			default:
				value = new NullLiteralExpr();
				break;
		}
		
		return value;
	}
	
	public static Snippet addVariableDeclaration(Snippet snippet, String type, String name) {
		//construct the statement using javaParser
		Type varType = parser.parseType(type).getResult().get();
		VariableDeclarationExpr declaration = new VariableDeclarationExpr(varType, name);
		ExpressionStmt statement = new ExpressionStmt(declaration);
		String line = statement.toString();
		
		//add the line above the statement containing the error
		String code = snippet.getCode();
		
		return snippet;
	}
	
	public static Statement getStatementFromLine(CompilationUnit cu, int line) {
		//get all statements within the compilation unit
		List<Statement> statements = cu.findAll(Statement.class);
		
		Statement nodeStatement = null;
		
		//go through, looking for a statement that begins that the given error line
		for(Statement statement : statements) {
			int test = statement.getBegin().get().line;
			if(test == line) {
				nodeStatement = statement;
				break;
			}
		}
		
		return nodeStatement;
	}
	
	/**
	 * Given a statement, will return the highest-level expression containing the given SimpleName.
	 * This is useful because we need an expression to call resolve on.
	 * @param name The SimpleName as a string.
	 * @param statement The statement to search through.
	 * @return The found expression.
	 */
	public static Expression findContainingInStatement(String name, Statement statement) {
		//we use atomic references so we can store variables during our walks
		AtomicReference<Expression> atomicExpression = new AtomicReference<>();
		Expression expression = null;
		
		//walk through all nodes in the statement
		statement.walk(node ->{
			//for all expressions within the statement
			if(node instanceof Expression) {
				//walk the node to see if it contains the name we're looking for
				//can probably do this with a single walk as we can't break from a walk
				if(atomicExpression.get() == null) {
					node.walk(child->{
						//for all simple names
						if(child.getClass() == SimpleName.class) {
							//check that simple name is our name
							SimpleName simpleName = (SimpleName) child;
							if(simpleName.asString().equals(name)) {
								if(atomicExpression.get() == null) atomicExpression.set((Expression)node);
							}
						}
					});
				}
			}
		});
		
		//extract from the atomic reference
		expression = atomicExpression.get();
		return expression;
	}
	
	
	/**
	 * Given an expression within a Compilation Unit, will attempt to resolve type for the given name.
	 * This function is used to infer type from a line where there is no explicit type information.
	 * @param expression The expression to resolve.
	 * @return The string representation of the type. Returns null if no type could be found.
	 */
	public static String extractTypeFromExpression(Expression expression, String name){
		String type = null;
		ResolvedType resolvedType = null;
		
		//assignment expression
		if(expression instanceof AssignExpr) {

			Expression toResolve = getResolvableFromAssignment(expression.asAssignExpr(), name);
			if(toResolve != null) {
				try {
					resolvedType = toResolve.calculateResolvedType();
					type = processResolvedType(resolvedType);
				} catch (Exception e) {
					//in the case of not being able to resolve, our type is null
					type = null;
				}
			}
		}
		//method call
		else if(expression instanceof MethodCallExpr) {
			MethodCallExpr call = (MethodCallExpr) expression;
			Expression toResolve = getResolvableFromMethodCall(call, name);
			if(toResolve != null) {
				try {
					resolvedType = toResolve.calculateResolvedType();
					type = processResolvedType(resolvedType);
				} catch (Exception e) {
					//in the case of not being able to resolve, our type is null
					type = null;
				}
			}
		}
		else if(expression instanceof VariableDeclarationExpr) {
			Expression toResolve = getResolvableFromDeclaration(expression.asVariableDeclarationExpr(), name);
			if(toResolve != null) {
				try {
					resolvedType = toResolve.calculateResolvedType();
					type = processResolvedType(resolvedType);
				} catch (Exception e) {
					//in the case of not being able to resolve, our type is null
					type = null;
				}
			}
		}
		//binary expression
		else if(expression instanceof BinaryExpr) {
			Expression toResolve = getResolvableFromBinaryExpr(expression.asBinaryExpr(), name);
			if(toResolve != null) {
				try {
					resolvedType = toResolve.calculateResolvedType();
					type = processResolvedType(resolvedType);
				} catch (Exception e) {
					//in the case of not being able to resolve, our type is null
					type = null;
				}
			}
		}
		//unary expression
		else if(expression instanceof UnaryExpr) {
			//we can assume this is an int
			type = "int";
		}
		else {
			//System.out.println(expression.getClass());
		}
		
		return type;
	}
	
	private static Expression getResolvableFromDeclaration(VariableDeclarationExpr declaration, String name) {
		Expression toResolve = null;
		
		List<VariableDeclarator> declarators = declaration.findAll(VariableDeclarator.class);
		if(declarators != null && declarators.size() > 0) {
			for(VariableDeclarator declarator : declarators) {
				if(declarator.getInitializer().isPresent()) {
					Expression initializer = declarator.getInitializer().get();
					//if some var is being set to our var
					if(initializer.isNameExpr() && initializer.toString().equals(name)) {
						//the resolvable is the declaration, not the name from declarator
						//otherwise we get an exception on name expr
						toResolve = declaration;
						break;
					}
					//if some initializer includes our var
					else if(containsName(initializer, name)) {
						if(initializer.isBinaryExpr()) {
							toResolve = getResolvableFromBinaryExpr(initializer.asBinaryExpr(), name);
						}
						else if(initializer.isEnclosedExpr()){
							toResolve = getResolvableFromEnclosedExpr(initializer.asEnclosedExpr(), name);
						}
					}
				}
			}
		}
		
		if(toResolve == null) {
			return declaration;
		}
		
		return toResolve;
	}
	
	private static Expression getResolvableFromAssignment(AssignExpr assignment, String name) {
		Expression toResolve = null;
		
		//get the target and value of this assignment 
		Expression target = assignment.getTarget();
		Expression value = assignment.getValue();
		
		//if we are just assigning something to name, look at the value
		if(target instanceof NameExpr && target.toString().equals(name)) {
			//literal, the simplest
			if(value.isLiteralExpr()) {
				toResolve = value;
			}
			//binary expression
			else if(value.isBinaryExpr()) {
				toResolve = getResolvableFromBinaryExpr(value.asBinaryExpr(), name);
			}
			else if(value.isObjectCreationExpr()) {
				toResolve = value;
			}
			else {
				//System.out.println(value.getClass());
			}
			
		}
		
		
//		//check target
//		List<SimpleName> names = target.findAll(SimpleName.class);
//		if(names == null) toResolve = target;
//		else if(names.isEmpty()) toResolve = target;
//		else {
//			//search for our name
//			boolean exists = false;
//			for(SimpleName n : names) {
//				if(n.asString().equals(name)) exists = true;
//			}
//			//otherwise
//			if(exists == false) toResolve = target;
//		}
//		
//		//if target did contain our name, then use value
//		if(toResolve == null) toResolve = assignment.getValue();
		return toResolve;
	}
	
	
	private static Expression getResolvableFromMethodCall(MethodCallExpr call, String name) {
		Expression toResolve = null;
		
		//if we have a scope
		if(call.getScope().isPresent()) {
			//look if our variable exists within the scope
			Expression scope = call.getScope().get();
			List<SimpleName> names = scope.findAll(SimpleName.class);
			for(SimpleName n : names) {
				//if any simple name is our var
				if(n.toString().equals(name)) {
					toResolve = scope;
				}
			}
		}
		
		//if our variable existed within the scope, try to resolve
		if(toResolve != null) {
			if(toResolve instanceof EnclosedExpr) {
				toResolve = getResolvableFromEnclosedExpr(toResolve.asEnclosedExpr(), name);
			}
		}
		
		//if it didn't
		if(toResolve == null) {
			//get arguments
			List<Expression> arguments = call.getArguments();
			for(Expression argument : arguments) {
				//when we find an argument that contains our name
				if(containsName(argument, name)) {
					//attempt to parse
					if(argument instanceof BinaryExpr) {
						toResolve = getResolvableFromBinaryExpr(argument.asBinaryExpr(), name);
					}
					
					break;
				}
			}
		}
		
		
		return toResolve;
	}
	
	/**
	 * Attempts to return a resolvable element from an enclosed expression.
	 * In man cases, an enclosed expression will contain some other expression we can use to get type.
	 * @param enclosedExpr The Enclosed Expression to look at.
	 * @return A potentially resolvable expression.
	 */
	private static Expression getResolvableFromEnclosedExpr(EnclosedExpr enclosedExpr, String name) {
		Expression toResolve = null;
		
		Expression inner = enclosedExpr.getInner();
		//binary expression
		if(inner instanceof BinaryExpr) {
			toResolve = getResolvableFromBinaryExpr(inner.asBinaryExpr(), name);
		}
		
		return toResolve;
	}
	
	/**
	 * Attempts to return a resolvable element from a binary expression.
	 */
	private static Expression getResolvableFromBinaryExpr(BinaryExpr binaryExpr, String name) {
		Expression toResolve = null;
		
		//get left and right parts
		Expression left = binaryExpr.getLeft();
		Expression right = binaryExpr.getRight();
		
		//if a side is a literal expression, thats what we use
		if(right.isLiteralExpr()) {
			toResolve = right;
		}
		else if(left.isLiteralExpr()) {
			toResolve = left;
		}
		//if the right contains our name look at left
		else if(containsName(right, name)) {
			toResolve = left;
		}
		//if the left contains our name look at right
		else if(containsName(left, name)) {
			toResolve = right;
		}
		
		return toResolve;
	}
	
	/**
	 * Checks if an expression contains a given name.
	 */
	private static boolean containsName(Expression expression, String name) {
		boolean contains = false;
		
		List<SimpleName> names = expression.findAll(SimpleName.class);
		if(names == null) return contains;
		
		for(SimpleName n : names) {
			//if any simple name is our var
			if(n.toString().equals(name)) {
				contains = true;
				break;
			}
		}
		
		return contains;
	}
}