package nlp3code.listeners;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;

import nlp3code.DocHandler;
import nlp3code.InputHandler;

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
