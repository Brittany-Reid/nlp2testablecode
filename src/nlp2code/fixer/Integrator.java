package nlp2code.fixer;

import java.util.List;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import nlp2code.Snippet;

/**
 * Functions for integrating code snippets.
 */
public class Integrator {
	public static final int SNIPPET = 0;
	public static final int METHOD = 1;
	public static final int CLASS = 2;
	public static JavaParser parser = new JavaParser();
	public static BodyDeclaration body;
	
	/**
	 * Returns the type of snippet, one of SNIPPET, METHOD or CLASS.
	 */
	public static int getType(Snippet snippet) {
		String code = snippet.getCode();
		
		System.out.println("class A{\n" + code + "}\n");
		
		ParseResult result;
		
		//parse as body declaration, methods and classes
		CompilationUnit cu;
		result = parser.parseBodyDeclaration(code);
		if(result.getResult().isPresent()) {
			body = (BodyDeclaration) result.getResult().get();
			List<MethodDeclaration> methods = body.findAll(MethodDeclaration.class);
			if(methods != null && methods.size() == 1) {
				return 1;
			}
			//check if has class...
		}
		
		//add class and try
		result = parser.parseBodyDeclaration("class A{\n" + code + "}\n");
		if(result.getResult().isPresent()) {
			body = (BodyDeclaration) result.getResult().get();
			List<MethodDeclaration> methods = body.findAll(MethodDeclaration.class);
			if(methods != null && methods.size() == 1) {
				return 1;
			}
		}

		
		return 0;
	}

	/**
	 * Integrate methods.
	 */
	public static String integrateMethod(String before, String after) {
		//if method is main inside main...
		return null;
	}
}
