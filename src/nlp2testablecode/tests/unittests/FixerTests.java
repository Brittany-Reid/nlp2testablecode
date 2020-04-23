package nlp2testablecode.tests.unittests;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import nlp2testablecode.DocHandler;
import nlp2testablecode.Evaluator;
import nlp2testablecode.code.Snippet;
import nlp2testablecode.compiler.IMCompiler;
import nlp2testablecode.fixer.Fixer;

public class FixerTests {
	String before = "class Main{\npublic static void main(String args[]) {\n";
	String after = "}\n}\n";
	
	@Test
	public void fixSnippetTest(){
		
		String code = "int i=0;\nint b = 0\n";
		Snippet snippet = new Snippet(code, 0);
		Evaluator.compiler = Evaluator.initializeCompiler(false);
		IMCompiler compiler = Evaluator.compiler;
		compiler.clearSaved();
		DocHandler.setFileName("Test.java");
		compiler.addSource(DocHandler.getFileName(), Snippet.insert(snippet, before+after, before.length()));
		compiler.compileAll();
		snippet.updateErrors(compiler.getErrors(), compiler.getDiagnostics().getDiagnostics());
		snippet = Fixer.errorFixes(snippet, before, after);
		assertEquals(snippet.getCode(), "int i=0;\nint b = 0;\n");
	}
	
	@Test
	public void multipleNonesTest(){
		String code = "int a= ;int i=0\nint b =;\n";
		Snippet snippet = new Snippet(code, 0);
		Evaluator.compiler = Evaluator.initializeCompiler(false);
		IMCompiler compiler = Evaluator.compiler;
		compiler.clearSaved();
		DocHandler.setFileName("Test.java");
		compiler.addSource(DocHandler.getFileName(), Snippet.insert(snippet, before+after, before.length()));
		compiler.compileAll();
		snippet.updateErrors(compiler.getErrors(), compiler.getDiagnostics().getDiagnostics());
		snippet = Fixer.errorFixes(snippet, before, after);
		//assertEquals(snippet.getCode(), "int i=0;\nint b = 0;\n");
	}
	
	@Test
	public void benchmark() {

		Evaluator.compiler = Evaluator.initializeCompiler(false);
		String code = "int i=0;\nint b = 0\n";
		DocHandler.setFileName("Test.java");
		Snippet snippet = new Snippet(code, 0);
		List<Snippet> snippets = new ArrayList<>();
		for(int i=0; i<10; i++) {
			snippets.add(new Snippet(snippet));
		}
		code = "xyz\nint i=0;\nint b = 0\n";
		snippet = new Snippet(code, 0);
		for(int i=0; i<10; i++) {
			snippets.add(new Snippet(snippet));
		}
		code = "xyz\nint i=0;";
		snippet = new Snippet(code, 0);
		for(int i=0; i<10; i++) {
			snippets.add(new Snippet(snippet));
		}
		code = "public static void main(String[] args) {\nint i=0;\n}\n";
		snippet = new Snippet(code, 0);
		for(int i=0; i<10; i++) {
			snippets.add(new Snippet(snippet));
		}
		
		Evaluator.evaluate(null, snippets, before, after);
		//System.out.println(Evaluator.compilingSnippets.size());
	}
}
