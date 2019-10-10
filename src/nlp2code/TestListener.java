package nlp2code;

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
 * This class 
 */
public class TestListener implements IDocumentListener {
	String line = null;
	public static String c = "$";
	
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
		
		//otherwise, lets check if we have a correctly formatted query
		if (insertion.length() >= 1) {
			String newline = line.trim();
			if (!(newline.endsWith(c))) return;
			//if formatted correctly, preform the query
			doTest(event, line);
			exitTestState();
		}
		
		
		
		//exit state on new line
		if(insertion.contains("\n") || insertion.endsWith("?")) {
			exitTestState();
		}
		
	}
	
	public void doTest(DocumentEvent event, String line) {
		//get insert location information 
  		try {
			IDocument document = QueryDocListener.getDocument();
			int lineNum = document.getLineOfOffset(event.getOffset());
			int lineOffset = document.getLineOffset(lineNum);
			int lineLength = document.getLineLength(lineNum);
			if (lineOffset < 0 || lineOffset > document.getLength()) return;
			if (lineLength > document.getLength() || lineOffset + lineLength > document.getLength()) return;
			
			document.replace(lineOffset, lineLength, "");
			((ITextEditor)QueryDocListener.editorPart).selectAndReveal(lineOffset, 0);
			constructTestFunction();
			System.out.println(line.substring(0, line.length()-2));
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
  		
	}
	
	private void constructTestFunction() {
		String function = "\n\n\t@Test\n"
				+ "\tpublic void nlp3code_test(){\n"
				+ "\t\t//--START EDITING\n"
				+ "\t\t//--END EDITING\n"
				+ "\t}";
		
		int pos = 0;
		
		IDocument document = QueryDocListener.getDocument();
		ASTParser parser = ASTParser.newParser(AST.JLS11);
		parser.setSource(document.get().toCharArray());
		CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        AST ast = cu.getAST();
        
        //get list of methods
        MethodVisitor mv = new MethodVisitor();
        cu.getRoot().accept(mv);
        List<MethodDeclaration> methodNodes = mv.methods;
        if(methodNodes == null || methodNodes.size() == 0) return;
        MethodDeclaration last = methodNodes.get(methodNodes.size()-1);
        pos = last.getStartPosition() + last.getLength();
        
        try {
			document.replace(pos, 0, function);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void exitTestState() {
		TestSuggester.testState = false;
		line = null;
	}


	@Override
	public void documentAboutToBeChanged(DocumentEvent arg0) {}

}
