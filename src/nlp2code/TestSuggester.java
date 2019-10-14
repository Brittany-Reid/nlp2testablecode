package nlp2code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Adds content assist proposals for test input/output types. The displayed proposals are 
 * not filtered as the user types.
 */
public class TestSuggester implements IJavaCompletionProposalComputer {
	public static boolean testState = false;
	private int line_num = 0;
	private int line_offset = 0;
	private int line_length = 0;
	private int extra_offset = 0;
	public static String format = "Return, Input, ...., Input";
	
	@Override
	public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext context, IProgressMonitor monitor) {
		List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
		
		//if in the state of writing a test function, ignore this command
		if(TestListener.functionState == true) return proposals; 
		
		IWorkbenchPart part = QueryDocListener.editorPart;
		
		//if we never ran a query before
		if(part == null) return proposals;
		
		//if no snippets in last run compiled
		if(Evaluator.compiled < 1) return proposals;
		
		//if code is the same
		if(changed() == true) return proposals;
		
		try {
			String line = "";
			//active editor must be a text editor
			if ( part instanceof ITextEditor ) {
				// Use text editor context to locate the document of the editor and get the input stream to that editor.
		        ITextEditor editor = (ITextEditor)part;
		        IDocumentProvider prov = editor.getDocumentProvider();
		        IDocument doc = prov.getDocument( editor.getEditorInput() );
				ISelectionProvider selectionProvider = ((ITextEditor)editor).getSelectionProvider();
				if (selectionProvider.equals(null)) return proposals;
			    ISelection selection = selectionProvider.getSelection();
			    if (selection.equals(null)) return proposals;
			    if (!(selection instanceof ITextSelection)) return proposals;
			    ITextSelection textSelection = (ITextSelection)selection;
			    
			    //get line num
			    line_num = textSelection.getStartLine();
			    //get lengths and offsets
		        int offset = InputHandler.previous_offset;
		        int length = InputHandler.previous_length;
			    line_length = doc.getLineLength(line_num);
				line_offset = doc.getLineOffset(line_num);
			    
				//get the current line
		        line = doc.get(line_offset, line_length);
				
				//calculate whitespace
				String whitespace_before;
				if (line.indexOf(line.trim()) < 0) {
					whitespace_before = line;
				} else {
					whitespace_before = line.substring(0, line.indexOf(line.trim()));
				}
				extra_offset = whitespace_before.length();
				
				if (line.endsWith("\n")) line_length-=1;
		     
				proposals = generateProposals();
				testState = true;
			}
		} catch(BadLocationException e) {
			e.printStackTrace();
		}
		
		return proposals;
	}
	
	/**
	 * Returns true if the document was edited. For now we don't care about changes after the snippet.
	 * @return
	 */
	private boolean changed() {
		//check if code between previous area is still the same
		IWorkbenchPart part = QueryDocListener.editorPart;
		if ( part instanceof ITextEditor ) {
			ITextEditor editor = (ITextEditor)part;
	        IDocumentProvider prov = editor.getDocumentProvider();
	        IDocument doc = prov.getDocument( editor.getEditorInput() );
	        int offset = InputHandler.previous_offset;
	        int length = InputHandler.previous_length;
	        
	        //if the size is less the old length up to the end of the snippet
	        if (offset + length > doc.getLength()) {
	        	return true;
	        }
	        
	        String text = "";
	        //get code in snippet area
	        try {
				text = doc.get(offset, length);
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
	        for(Snippet s : InputHandler.previous_search) {
	        	if((InputHandler.previousInfo + s.getFormattedCode()).equals(text)) {
	        		return false;
	        	}
	        }
		}
		return true;
	}
	
	/**
	 * Returns a list of ICompletionProposals based on the result of the previous query.
	 */
	private List<ICompletionProposal> generateProposals() {
		List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
		
		//top, format
		String content = "Return, Input, ...., Input";
		ICompletionProposal proposal = new CompletionProposal(content + TestListener.c, line_offset + extra_offset, line_length - extra_offset, content.length());
		proposals.add(proposal);
		
		//get list of types sorted
		List<String> ioTypes = sortIOTypes();
		
		//add to proposals
		for(String s : ioTypes) {
			content = s;
			proposal = new CompletionProposal(content+TestListener.c, line_offset + extra_offset, line_length - extra_offset, content.length());
			proposals.add(proposal);
		}
		
		return proposals;
	}
	
	/**
	 * Returns a list of Input/Output type suggestions sorted by occurrence.
	 * @return A List of Strings.
	 */
	private List<String> sortIOTypes(){
		List<String> ioTypes = new ArrayList<String>();
		Map<String, Integer> occurences = new HashMap<>();
		
		String content;
		for(Snippet snippet : InputHandler.previous_search) {
			
			if(snippet.getArgumentTypes() == null || snippet.getReturnType() == null) continue;
			
			//construct entry
			content = snippet.getReturnType();
			for(String s : snippet.getArgumentTypes()) {
				content += ", " + s;
			}
			
			//add to occurrences map
			if(!occurences.containsKey(content)) {
				occurences.put(content, 1);
			}
			else {
				occurences.replace(content, occurences.get(content)+1);
			}		
		}
		
		//sort
		List<Entry<String, Integer>> list = new ArrayList<>(occurences.entrySet());
        list.sort(Entry.<String, Integer>comparingByValue().reversed());
        
        //add keys to list
        for (Entry<String, Integer> entry : list) {
        	ioTypes.add(entry.getKey());
        }
		
		return ioTypes;
	}
	
	// Function implementations required for the interface that are not used.
	@Override
	public List<IContextInformation> computeContextInformation(ContentAssistInvocationContext arg0, IProgressMonitor arg1) { return null; }
	@Override
	public String getErrorMessage() { return null; }
	@Override
	public void sessionStarted() { }
	@Override
	public void sessionEnded() {}
}
