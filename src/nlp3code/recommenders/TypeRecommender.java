package nlp3code.recommenders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;

import nlp3code.Evaluator;
import nlp3code.InputHandler;
import nlp3code.code.Snippet;
import nlp3code.tester.TypeSuggestions;

public class TypeRecommender implements  IJavaCompletionProposalComputer{
	public static boolean canTest = false;
	public static boolean testing = false;
	public static boolean canRecommend = false;
	public static boolean suggesting = false;
	public static String testChar = "$";
	public static List<Snippet> generated = null;

	@Override
	public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext context, IProgressMonitor arg1) {
		List<ICompletionProposal> proposals = new ArrayList<>();
		
		if(canRecommend == false) {
			return proposals;
		}
		
		//need at least 1 compilable snippet
		if(Evaluator.compiled < 1) {
			return proposals;
		}
		
		suggesting = true;
		
		String line = TaskRecommender.extractQuery(testChar);
		if(line ==  null) {
			return proposals;
		}
		
		//get offset from the context
		JavaContentAssistInvocationContext javaContext= (JavaContentAssistInvocationContext) context;
		int offset = javaContext.getInvocationOffset();
		
		proposals = generateProposals(line, offset);
		
		return proposals;
	}

	@Override
	public List<IContextInformation> computeContextInformation(ContentAssistInvocationContext arg0,
			IProgressMonitor arg1) {
		return null;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public void sessionEnded() {
	}

	@Override
	public void sessionStarted() {
	}
	
	/**
	 * Returns a list of Input/Output type suggestions sorted by occurrence.
	 * @return A List of Strings.
	 */
	public static List<String> sortIOTypes(List<Snippet> snippets){
		List<String> ioTypes = new ArrayList<String>();
		Map<String, Integer> occurences = new HashMap<>();
		
		String content;
		for(Snippet snippet : snippets) {
			
			if(snippet.getArgumentTypes() == null || snippet.getReturn() == null) continue;
			
			//construct entry
			content = snippet.getReturn();
			List<String> args = snippet.getArgumentTypes();
			Collections.sort(args);
			for(String s : args) {
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
	
	/**
	 * Returns a list of ICompletionProposals based on the result of the previous query.
	 */
	private List<ICompletionProposal> generateProposals(String line, int offset) {
		List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
		
		//top, format
		String content = "Format: Return, Input, ...., Input";
		ICompletionProposal proposal = new CompletionProposal("", offset-line.length(), 0, 0, null, content + testChar, null, null);
		proposals.add(proposal);
		
		List<Snippet> snippets = generated;
		if(generated == null) {
			snippets = TypeSuggestions.getTypeSuggestions(InputHandler.previousSnippets, InputHandler.before, InputHandler.after, null);
		}
		
		//get list of types sorted
		List<String> ioTypes = sortIOTypes(snippets);
		
		//add to proposals
		for(String s : ioTypes) {
			content = s;
			proposal = new CompletionProposal(content+ testChar, offset-line.length(), line.length(), content.length()+1);
			proposals.add(proposal);
		}
		
		return proposals;
	}
	
}
