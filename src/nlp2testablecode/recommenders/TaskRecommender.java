package nlp2testablecode.recommenders;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;

import nlp2testablecode.Activator;
import nlp2testablecode.DataHandler;
import nlp2testablecode.DocHandler;
import nlp2testablecode.InputHandler;

public class TaskRecommender implements  IJavaCompletionProposalComputer {
	//the max number of recommendations from the task for a given query
	public static int MAX_NUM_RECOMMENDATIONS = 100;
	private int offset = 0;
	private int length = 0;
	private int whitespace = 0;
	public static String queryChar = "?";
	
	
	/** 
	 *  Implements an interface to compile a list of proposed autocompletions for a given incomplete query.
	 *  Each proposal is defined as a string proposal to insert into a specific section of the current editor.
	 *  Must not return null proposals.
	 */
	@Override
	public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext context, IProgressMonitor arg1) {
		List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
		
		//must initialize db
		if(Activator.initialized == false) {
			//immediately set to true
			Activator.initialized = true;
			
			Activator.setup();
			
			//then ignore this call
			return proposals;
		}
		
		//if we haven't finished loading yet
		if(DataHandler.loaded == false) return proposals;
		
		String line = extractQuery(queryChar);
		if(line ==  null) return proposals;
	
		//get offset from the context
		JavaContentAssistInvocationContext javaContext= (JavaContentAssistInvocationContext) context;
		offset = javaContext.getInvocationOffset();
		
		// For each task that matches the line, create a new proposal and add it to the proposals arraylist. 
		for (String searchResult : findTasks(line)) {
			ICompletionProposal proposal = new CompletionProposal(searchResult.substring(0, 1).toUpperCase() + searchResult.substring(1) + "?",
					offset-line.length(), // replacement offset
					line.length(), // replace the full text
					searchResult.length()+1); // length of string to replace
			proposals.add(proposal);
		}
		
		return proposals;
	}
	
	static String extractQuery(String queryChar) {
		String line = DocHandler.getCurrentLine();
		if(line == null) return null;
		
		//if the line ends with a newline
		boolean eol = false;
		if (line.endsWith("\n")) eol = true;
		
		line = line.trim();
		
		// Calculate offset of partial query. Used when inserting a recommendation to ensure the query is inserted correctly.
		if (line.startsWith(queryChar,0)) {
			if (line.length() > 1)
				line=line.substring(1, line.length());
			else
				line = "";
		}
		
		line = line.toLowerCase();
		
		return line;
	}
	
	/**
	 *  Adds the document listener to detect ?{query}? type searches in the case that the current editor
	 *  doesn't already have the listener.
	 */
	private void addListenerToCurrentEditor() {
		IDocument document = DocHandler.getDocument();
		if (!InputHandler.documents.contains(document)) {
			document.addDocumentListener(InputHandler.queryDocListener);
			InputHandler.clear();
		}
	}
	
	/**
	 * Given a string to search for, return all strings in static hashmap queries that contain the search string.
	 */
	private List<String> findTasks(String search){
		List<String> result = new ArrayList<String>();
		for (String query : DataHandler.queries) {
			if (query.contains(search) || search == "") {
				// If we reach the maximum number of queries to recommend, stop.
				if (result.size() == MAX_NUM_RECOMMENDATIONS)
					return result;
				result.add(query);
			}
		}
		return result;
	}

	@Override
	public List<IContextInformation> computeContextInformation(ContentAssistInvocationContext arg0,
			IProgressMonitor arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sessionEnded() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sessionStarted() {
		// TODO Auto-generated method stub
		
	}

}
