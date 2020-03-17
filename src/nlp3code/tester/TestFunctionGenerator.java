package nlp3code.tester;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;

import nlp3code.fixer.UnresolvedElementFixes;

/**
 * This class handles functionality for generating a default JUnit test function.
 */
public class TestFunctionGenerator {
	public static String junitName = "nlp3code_test";
	
	/**
	 * Process type string into List, with return type at beginning.
	 * @param typeString The string to process.
	 */
	public static List<String> getTypeList(String typeString){
		List<String> types = new ArrayList<String>();
		
		typeString = typeString.replace("$", "").trim();
		
		//split the string by commas
		types = Arrays.asList(typeString.split(", "));
		
		return types;
	}
	
	/**
	 * Generates a default test case from a given list of types in the format:
	 * 		assertEquals(0, function("0"));
	 * 
	 * @param types The list of types, with the first representing the return type.
	 * @name name Name of the function to be tested.
	 * @return The constructed JUnit test case.
	 */
	public static Statement generateTestCase(List<String> types, String name) {		
		MethodCallExpr assertStatement = new MethodCallExpr(null, "assertEquals");
		
		//return will be first element
		String returnType = types.get(0);
		//get arguments from remaining
		List<String> argumentTypes = new ArrayList<>();
		for(int i=1; i<types.size(); i++) {
			argumentTypes.add(types.get(i));
		}
		
		//get default value of the return type
		Expression value = UnresolvedElementFixes.getDefaultValue(returnType);
		assertStatement.addArgument(value);
		String comment = "Format: assertEquals(" + returnType + ", " + name + "(";
		
		MethodCallExpr testFunction = new MethodCallExpr(null, name);
		//add each argument
		for(int i=0; i<argumentTypes.size(); i++) {
			value = UnresolvedElementFixes.getDefaultValue(argumentTypes.get(i));
			testFunction.addArgument(value.toString());
			comment += argumentTypes.get(i);
			//add seperator until last element
			if(i != argumentTypes.size()-1) {
				comment += ", ";
			}
		}
		
		//end
		comment+="));\n";
		assertStatement.addArgument(testFunction);
		
		//add comment
		assertStatement.setComment(new LineComment(comment));
		
		return new ExpressionStmt(assertStatement);
	}

	/**
	 * Generates an annotated JUnit test function using JavaParser.
	 * @param types Argument and Return types, the first represents return.
	 * @param name Name of the function to test.
	 * @return The JavaParser method declaration node.
	 */
	public static MethodDeclaration generateTestFunction(List<String> types, String name) {
		String function = null;
		
		MethodDeclaration testFunction = new MethodDeclaration();
		testFunction.setName(junitName);
		testFunction.setPublic(true);
		testFunction.setType("void");
		testFunction.addAnnotation("Test");
		
		//get the default test case
		Statement testCase = generateTestCase(types, name);
		
		//add to block
		BlockStmt block = new BlockStmt();
		
		block.addStatement(testCase);
		
		testFunction.setBody(block);
		
		return testFunction;
	}
}
