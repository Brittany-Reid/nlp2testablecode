package nlp2code;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Document listener for test type input/output.
 */
public class TestListener implements IDocumentListener {
	String line = null;
	public static String c = "$";
	//are we writing a function right now?
	public static boolean functionState = false;
	public static List<String> imports = null;
	public static String defaultTestCase = "		Assert.equals(input, output());\n";
	
	/**
	 * Listen for when the document changes.
	 */
	@Override
	public void documentChanged(DocumentEvent event) {
		
		//only listen if the user has entered test state with CRTL ALT T
		if(TestSuggester.testState == false) {
			return;
		}
		
		//get the event text
		String insertion = event.getText();
		if(insertion == "") return;
		
		//if we had a previous line in state
		if(line != null) {
			if(!QueryDocListener.getLine().contains(line)) {
				exitTestState();
			}
		}
		
		//was the document change an undo. make sure we don't make a query again.
		line = QueryDocListener.getLine();
		String check_undo = line;
		check_undo = check_undo.trim();
		if (check_undo.startsWith(c)) check_undo = check_undo.substring(1);
		if (check_undo.endsWith(c)) check_undo = check_undo.substring(0, check_undo.length()-1);
		if (InputHandler.previous_queries.contains(check_undo)) {
			InputHandler.previous_queries.remove(InputHandler.previous_queries.indexOf(check_undo));
			return;
		}
		
		//if we select the formatter, ignore
		if(line.contains(TestSuggester.format)) return;
		
		//otherwise, lets check if we have a correctly formatted query
		if (insertion.length() >= 1) {
			//check format
			String newline = line.trim();
			if (!(newline.endsWith(c))) return;
		
			promptTestCase(event, line);
			
			//exit from test state
			exitTestState();
		}
		
		//exit state on new line or query
		if(insertion.contains("\n") || insertion.endsWith("?")) {
			exitTestState();
		}
		
	}
	
	/**
	 * After inserting test input/output types, prompt user for a test case.
	 */
	private void promptTestCase(DocumentEvent event, String line) {
		//get insert location information 
		IDocument document = DocumentHandler.getDocument();
		int lineNum;
		int lineOffset = -1;
		int lineLength = -1;
		try {
			lineNum = document.getLineOfOffset(event.getOffset());
			lineOffset = document.getLineOffset(lineNum);
			lineLength = document.getLineLength(lineNum);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		
		if(lineOffset == -1|| lineLength == -1) {
			System.err.println("Error: Could not get test input/output line from document.");
			return;
		}
		
		//remove insert
		DocumentHandler.removeAt(lineOffset, lineLength);
		
		//construct function 
		String function = TestHandler.functionStart+defaultTestCase+TestHandler.functionEnd;
		
		//add to document
		int err = DocumentHandler.addFunction(function);
		if(err == -1) {
			System.err.println("Error: Test function could not be added.");
			return;
		}
		
		//add junit import
		if(imports == null) {
			imports = new ArrayList<String>();
			imports.add("import org.junit.Assert;");
			imports.add("import org.junit.Test;");
		}
		DocumentHandler.addImportStatements(imports);
		
		//state change to editing function
		functionState = true;
	}
	
	public void exitTestState() {
		TestSuggester.testState = false;
		line = null;
	}


	@Override
	public void documentAboutToBeChanged(DocumentEvent arg0) {}

}
