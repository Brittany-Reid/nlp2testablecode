package nlp2code;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;

/**
 * class CycleDocListener
 *   Document listener that listens for when a document is edited.
 *   When a document is edited, it notified cycleAnswerHandler to disallow
 *   cycling through answers anymore.
 */
public class CycleDocListener implements IDocumentListener {
	
	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {		
	}

	/*
	 * Function documentChanged
	 *   Called when a document is changed.
	 *   Disables cycling through code snippets by signalling cycleAnswersHandler when the document has been edited.
	 */
	@Override
	public void documentChanged(DocumentEvent event) {
		boolean match = false;
		String text = event.getText();
		
		//if triggered by import statements being added
		if(text.equals(InputHandler.previousImports)) {
			match = true;
		}
		
		//if triggered by snippets being inserted
		for(Snippet s : InputHandler.previous_search) {
			if((InputHandler.previousInfo + s.getFormattedCode()).equals(text)) {
				match = true;
			}
		}
		
		if(match == false) {
			IDocument doc = event.getDocument();
			doc.removeDocumentListener(InputHandler.doclistener);
			CycleAnswersHandler.changed_doc = true;
		}
		
//		if (!InputHandler.previous_search.contains(text)) {
//			IDocument doc = event.getDocument();
//			doc.removeDocumentListener(InputHandler.doclistener);
//			CycleAnswersHandler.changed_doc = true;
//		}
	}
}