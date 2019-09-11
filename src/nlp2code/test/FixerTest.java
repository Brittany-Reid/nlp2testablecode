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
	
	@Test
	public void testGetVariableName(){
		ReflectionTypeSolver solver = new ReflectionTypeSolver();
		ParserConfiguration parserConfiguration = new ParserConfiguration().setSymbolResolver( new JavaSymbolSolver(solver)); 
		JavaParser parser = new JavaParser(parserConfiguration);
		//static surrounding code
		String before = "class Main {\n public static void main(String[] args) {\n";
		String after = "return;\n}\n}\n";
		
		String snippet;
	
		
		//for a method call where unresolved is the name
		snippet = "String s = x;\n";
		CompilationUnit cu = parser.parse(before + snippet + after).getResult().get();
		Statement nodeStatement =  UnresolvedElementFixes.getStatementFromLine(cu, 3);
		String name = "x";
		Expression expression = UnresolvedElementFixes.findContainingInStatement(name, nodeStatement);
		String type = UnresolvedElementFixes.extractTypeFromExpression(expression, name);
		System.out.println(type);
	}

}
