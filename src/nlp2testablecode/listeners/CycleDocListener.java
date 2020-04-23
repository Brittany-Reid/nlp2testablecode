package nlp2testablecode.listeners;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;

import nlp2testablecode.InputHandler;
import nlp2testablecode.cycler.CycleAnswersHandler;

/**
 * class CycleDocListener
 *   Document listener that listens for when a document is edited.
 *   When a document is edited, it notified cycleAnswerHandler to disallow
 *   cycling through answers anymore.
 */
public class CycleDocListener implements IDocumentListener {
	public IDocument currentDocument;
	
	/**
	 * Called when a document is changed.
	 * Disables cycling through code snippets by signaling cycleAnswersHandler when the document has been edited.
	 */
	@Override
	public void documentChanged(DocumentEvent event) {
		//when we are inserting, the cycle doc listener will ignore it
		if(CycleAnswersHandler.inserting == true) return;
		
		String text = event.getText();
		
		//remove document listener
		IDocument document = event.getDocument();
		document.removeDocumentListener(InputHandler.cycleDocListener);
		
		//set changed to true
		CycleAnswersHandler.changedDoc = true;
		
		//clear, this will prevent cycling
		InputHandler.clear();
	}

	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {
		// TODO Auto-generated method stub
		
	}
	
}
