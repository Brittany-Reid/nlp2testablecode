package nlp2code.fixer;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;

import nlp2code.Snippet;

/**
 * Functions for integrating code snippets.
 */
public class Integrator {
	//snippet kind
	public static final int SNIPPET = 0;
	public static final int METHOD = 1;
	public static final int CLASS = 2;
	public static final int MULTIMETHOD = 3;
	//insert points
	public static final int MAINMETHOD = 0;
	public static JavaParser parser = new JavaParser();
	public static BodyDeclaration body;
	
	/**
	 * Returns the type of snippet, one of SNIPPET, METHOD or CLASS.
	 */
	public static int getType(Snippet snippet) {
		String code = snippet.getCode();
		
		//System.out.println("class A{\n" + code + "}\n");
		
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
	
	public static Snippet integrateMain(Snippet snippet, String before, String after) {
		int insertPoint = getInsertPoint(before, after);
		String code = null;
		//integerate main into main
		if(insertPoint == MAINMETHOD) {
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
	
	public static int getInsertPoint(String before, String after) {
		AtomicReference<Integer> aPos = new AtomicReference<Integer>();
		aPos.set(-1);
		int pos = -1;
		CompilationUnit cu = null;
		
		String comment = "// NLP3Code parsing comment\n";
		
		//parse context
		ParseResult result = parser.parse(before + comment + after);
		if(result.getResult().isPresent()) {
			cu = (CompilationUnit) result.getResult().get();
		}
		if(cu == null) return -1;
		
		//go through all nodes
		cu.walk(node->{
			for(Comment c : node.getAllContainedComments()) {
				//find comment
				if(c.toString().trim().equals(comment.trim())) {
					//if contained in a method
					if(node.getClass() == MethodDeclaration.class) {
						MethodDeclaration method = (MethodDeclaration) node;
						//main method
						if(isMain(method)) {
							aPos.set(MAINMETHOD);
						}
					}
				}
			}
		});
		
		pos = aPos.get();
		return pos;
	}
	
	private static boolean isMain(MethodDeclaration method) {
		String name = method.getNameAsString();
		String access = method.getAccessSpecifier().asString();
		boolean isStatic = method.isStatic();
		//public
		if(method.isPublic()) {
			//static
			if(method.isStatic()) {
				//void
				if(method.getType().isVoidType()) {
					//main
					if(method.getNameAsString().equals("main")) {
						//(String[] args)
						NodeList<Parameter> parameters = method.getParameters();
						if(parameters != null && parameters.size() == 1) {
							if(parameters.get(0).getType().asString().equals("String[]")){
								if(parameters.get(0).getNameAsString().equals("args")) {
									return true;
								}
							}
							
						}
					}
				}
			}
		}
		return false;
	}
}
