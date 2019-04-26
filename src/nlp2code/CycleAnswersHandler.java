package nlp2code;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * class CycleAnswersHandler
 *   Implements the required functionality to cycle through code snippets received after a search.
 */
public class CycleAnswersHandler extends AbstractHandler {

	// Index in vector of snippets to know which one is currently being displayed.
	static int previous_index = 0;
	// Keeps track on if the document has been changed after cycling. Used to make sure that
	// After editing the document, you can't cycle anymore.
	static boolean changed_doc = false;

	
	/*
	 * Function execute
	 * 	 Called when the cycle answer button is activated.
	 * 	 If the document hasn't been edited since the previous search, choose the next code snippet
	 *   in the list of retrieved snippets and insert that into the document.
	 */
	@Override
	public Object execute(ExecutionEvent arg0) {
		
		// After the document has been edited (after cycling through snippets), disable cycling functionality.
		if (changed_doc == true) {
			changed_doc = false;
			InputHandler.previous_length = 0;
			InputHandler.previous_offset = 0;
			previous_index = 0;
			System.out.println("Document was changed.");
			return null;
		}
		
		//if only 1 snippet
		if(InputHandler.previous_search.size() <= 1) {
			System.out.println("Failed to cycle snippets, only 1 snippet.");
			return null;
		}
		
		try {
			// Get the current active editor.
		    IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		    
		    // If the current active editor is a text editor (i.e. not any other workbench component)
		    if ( part instanceof ITextEditor ) {
		    	// Use text editor context to locate the document of the editor and get the input stream to that editor.
		        ITextEditor editor = (ITextEditor)part;
		        IDocumentProvider prov = editor.getDocumentProvider();
		        IDocument doc = prov.getDocument( editor.getEditorInput() );
		        int offset = InputHandler.previous_offset;
		        int length = InputHandler.previous_length;
		        
		        //if larger than current length, a change has occurred
		        if (offset + length > doc.getLength()) {
		        	System.out.println("Failed to cycle snippets, length has changed.");
		        	return null;
		        }
		        //get code in snippet area
		        String text = doc.get(offset, length);
		        
		        //if exists in lists of snippets
				if (InputHandler.previous_search.contains(text)) {
		        	int index = previous_index;
		        	index++;
		        	
		        	//if past end, return to start
		        	if (index >= InputHandler.previous_search.size() || index < 0) index = 0;
		        	
		        	// Replace old snippet with new snippet.
		        	doc.replace(offset, length, InputHandler.previous_search.get(index));
		            InputHandler.previous_length = InputHandler.previous_search.get(index).length();
		            previous_index = index;
		            System.out.println(previous_index);
		            if (InputHandler.previous_offset + InputHandler.previous_length > doc.getLength()) return null;
		            editor.selectAndReveal(InputHandler.previous_offset + InputHandler.previous_length, 0);
		            return null;
		        }
				else {
					//doesn't exist, must have changed
					System.out.println("Failed to cycle snippets, snippet has changed.");
				}
		    }
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
}
