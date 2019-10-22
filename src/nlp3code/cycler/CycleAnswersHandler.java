package nlp3code.cycler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import nlp3code.DocHandler;
import nlp3code.InputHandler;
import nlp3code.code.Snippet;

/**
 * Implements the required functionality to cycle through code snippets received after a search.
 * Extension of AbstractHandler, and called through a keyBinding.
 */
public class CycleAnswersHandler extends AbstractHandler {
	// Index in list of snippets to know which one is currently being displayed.
	static int previousIndex = 0;
	// Keeps track on if the document has been changed after cycling. Used to make sure that
	// After editing the document, you can't cycle anymore.
	public static boolean changedDoc = false;
	//when we are inserting, the cycle doc listener will ignore it
	public static boolean inserting = false;

	/**
	 * Called when the cycle answer button is activated.
	 * If the document hasn't been edited since the previous search, choose the next code snippet
	 * in the list of retrieved snippets and insert that into the document.
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		//theres no document to change
		if(InputHandler.cycleDocListener.currentDocument == null) return null;
		
		//if we're in a different document
		if(!DocHandler.getDocument().equals(InputHandler.cycleDocListener.currentDocument)) {
			InputHandler.cycleDocListener.currentDocument.removeDocumentListener(InputHandler.cycleDocListener);
			InputHandler.cycleDocListener.currentDocument = null;
			InputHandler.clear();
			return null;
		}
		
		// After the document has been edited (after cycling through snippets), disable cycling functionality.
		if (changedDoc == true) {
			return null;
		}
		
		//if we have no previous snippets
		if(InputHandler.previousSnippets == null) return null;
		
		//if less than 2 snippets, do nothing
		if(InputHandler.previousSnippets.size() <= 1) {
			System.out.println("Failed to cycle snippets, only 1 snippet.");
			return null;
		}
		
		//get index, cycling back to 0 at end
		if(previousIndex+1 >= InputHandler.previousSnippets.size()) {
			previousIndex = 0;
		}
		else {
			previousIndex++;
		}
		
		//get the new snippet
		Snippet newSnippet = InputHandler.previousSnippets.get(previousIndex);
		
		inserting = true;
		
		//replace
		replaceSnippet(newSnippet);
		
		//reset state
		inserting = false;
		
		return null;
	}
	
	/**
	 * Replace the current snippet with another.
	 */
	public static void replaceSnippet(Snippet snippet) {
		
		//get the previous range
		int offset = InputHandler.previousOffset;
		int length = InputHandler.previousLength;
		
		//replace this range
		DocHandler.addSnippet(InputHandler.generateQueryComment(InputHandler.previousQuery), snippet, offset, length);
		
		InputHandler.previousIndex = previousIndex;
		InputHandler.previousSnippet = snippet;
	}

}
