package nlp2code.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

import nlp2code.*;
import nlp2code.compiler.*;
import nlp2code.fixer.Fixer;
import nlp2code.fixer.Integrator;
import nlp2code.fixer.UnresolvedElementFixes;

import org.junit.Test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

public class FixerTest {
	
	ReflectionTypeSolver solver = new ReflectionTypeSolver();
	ParserConfiguration parserConfiguration = new ParserConfiguration().setSymbolResolver( new JavaSymbolSolver(solver)); 
	JavaParser parser = new JavaParser(parserConfiguration);
	//static surrounding code
	String before = "class Main {\n public static void main(String[] args) {\n";
	String after = "return;\n}\n}\n";
	String snippet, name, type;
	CompilationUnit cu;
	Statement nodeStatement;
	Expression expression;
	
	@Test
	public void testResolveSimpleCondition(){
		snippet = "if(i == 1) {\n return;\n}\n";
		cu = parser.parse(before + snippet + after).getResult().get();
		nodeStatement =  UnresolvedElementFixes.getStatementFromLine(cu, 3);
		name = "i";
		expression = UnresolvedElementFixes.findContainingInStatement(name, nodeStatement);
		type = UnresolvedElementFixes.extractTypeFromExpression(expression, name);
		//System.out.println(type);
	}
	
	@Test
	public void testResolveSimpleWhile(){
		snippet = "while(i==true){\n return;\n}\n";
		cu = parser.parse(before + snippet + after).getResult().get();
		nodeStatement =  UnresolvedElementFixes.getStatementFromLine(cu, 3);
		name = "i";
		expression = UnresolvedElementFixes.findContainingInStatement(name, nodeStatement);
		type = UnresolvedElementFixes.extractTypeFromExpression(expression, name);
		//System.out.println(type);
	}
	
	@Test
	public void testResolveFunctionArgument(){
		snippet = "System.out.println(i + \"a\");\n";
		cu = parser.parse(before + snippet + after).getResult().get();
		nodeStatement =  UnresolvedElementFixes.getStatementFromLine(cu, 3);
		name = "i";
		expression = UnresolvedElementFixes.findContainingInStatement(name, nodeStatement);
		type = UnresolvedElementFixes.extractTypeFromExpression(expression, name);
		//System.out.println(type);
	}
	
	@Test
	public void testIntegration() {
		snippet = "class A{\npublic static void main(String[] args) {\nint a;\n}\n}\n";
		Snippet s = new Snippet(snippet, 0);
		int context = Integrator.getType(s);
		Integrator.integrateClass(s, before, after);
		//Fixer.integrate(s, before, after);
	}

}
