package nlp3code.listeners;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;

import nlp3code.DocHandler;
import nlp3code.InputHandler;
import nlp3code.cycler.CycleAnswersHandler;
import nlp3code.recommenders.TypeRecommender;

/**
 * This listener controls if we can begin the testing process.
 */
public class BeginTestingListener implements IDocumentListener{
	
	/**
	 * When the document is changed, we can no longer test.
	 */
	@Override
	public void documentChanged(DocumentEvent event) {
		//ignore programmatic inserts
		if(CycleAnswersHandler.inserting == true) return;
		
		//if we are recommending, typing is ignored
		if(TypeRecommender.suggesting == true) return;
		
		//String text = event.getText();
		
		//check if code still exists
		String code = DocHandler.getAtOffset(event.getDocument(), InputHandler.previousOffset, InputHandler.previousLength);
		if(code == InputHandler.previousInfo + InputHandler.previousSnippet.getCode()) {
			return;
		}
		
		//anything that doesnt alter the code but could just be making room is fine
		if(event.getText().trim().equals("")) return;
		
		//remove document listener
		IDocument document = event.getDocument();
		document.removeDocumentListener(InputHandler.beginTestingListener);
		
		//stop suggestions
		TypeRecommender.canRecommend = false;
	}

	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {	
	}
	
}
