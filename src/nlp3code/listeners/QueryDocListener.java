package nlp3code.listeners;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.stmt.BlockStmt;

import nlp3code.Activator;
import nlp3code.DocHandler;
import nlp3code.Evaluator;
import nlp3code.InputHandler;
import nlp3code.Searcher;
import nlp3code.code.Snippet;
import nlp3code.fixer.Integrator;

public class QueryDocListener implements IDocumentListener{
	//the current document associated with the listener
	public static IDocument currentDocument = null;
	
	/**
	 * Function that activates every time the current edited document is changed.  
	 * Simply put, this document listener listens for ?{query}? format queries in the document,
	 * and conducts a query whenever this format is identified in the document.
	 * This allows for easy query-making without using any external buttons or widgets.
	 */
	@Override
    public void documentChanged(DocumentEvent event) 
    {	
		currentDocument = DocHandler.getDocument();
		DocHandler.documentChanged();
		
		String text = event.getText();
		if(text == "" || text.length() < 1) return;
		
		//get the line
		String line = DocHandler.getCurrentLine();
		if(line == null) return;
		
		String trimmed = line.trim();
		
		//check this isn't a previous query from an undo
		String checkUndo = trimmed;
		if (checkUndo.startsWith("?")) checkUndo = checkUndo.substring(1);
		if (checkUndo.endsWith("?")) checkUndo = checkUndo.substring(0, checkUndo.length()-1);
		if(InputHandler.previousQueries.contains(checkUndo)) {
			InputHandler.previousQueries.remove(checkUndo);
			return;
		}
		
		//otherwise, lets check if we have a correctly formatted query
		if (!(trimmed.endsWith("?"))) return;
		
		//if formatted correctly, preform the query
		InputHandler.doQuery(event.getOffset(), line);
    }
	
	

	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {
		// TODO Auto-generated method stub
	}
}
