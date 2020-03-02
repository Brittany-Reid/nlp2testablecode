package nlp3code.tester;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import nlp3code.Activator;
import nlp3code.DocHandler;
import nlp3code.Evaluator;
import nlp3code.InputHandler;
import nlp3code.code.Snippet;
import nlp3code.cycler.CycleAnswersHandler;
import nlp3code.listeners.QueryDocListener;
import nlp3code.recommenders.TypeRecommender;
import nlp3code.tester.Tester;

/**
 * This class handles functionality to press CTRL+ALT+D to accept a test function.
 */
public class TestHandler extends AbstractHandler{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if(TypeRecommender.canTest == false) return false;
		if(TypeRecommender.testing == false) return false;
		
		//get test case
		List<String> test = extractTestCase();
		if(test == null || test.isEmpty()) return null;
		String testCase = test.get(0);
		List<String> imports = test.subList(1, test.size());
		
		List<Snippet> tested = doTests(testCase, imports);
		if(tested != null) {
			insertSnippets(tested);
		}
		
		TypeRecommender.canTest = false;
		
		return null;
	}
	
	public static void insertSnippets(List<Snippet> snippets) {
		
		replaceSnippet(snippets.get(0));
		InputHandler.previousIndex = 0;
		InputHandler.previousSnippets = snippets;
		
		//enable cycling
		QueryDocListener.currentDocument.addDocumentListener(InputHandler.cycleDocListener);
		//associate a document
		InputHandler.cycleDocListener.currentDocument = QueryDocListener.currentDocument;
		CycleAnswersHandler.changedDoc = false;
	}
	
	/**
	 * Replace the current snippet with another. Adapted from CycleAnswersHandler but is less strict.
	 * The user can add imports, so: we can count the new space and adapt but I think that's easy to break
	 * Instead: look for the last string in the document and replace it.
	 * If it doesn't exist, we fail. 
	 * If a duplicate exists, we replace the first found which isn't perfect but.
	 */
	public static void replaceSnippet(Snippet snippet) {
		//find the previous snippet
		//this should be a search for the snippet fragment but atm we know fragment 1 is always a snippet
		String toFind = InputHandler.previousInfo + InputHandler.previousSnippet.getFragment(1).getFormattedCode();
		IDocument document = DocHandler.getDocument();
		
		FindReplaceDocumentAdapter searcher = new FindReplaceDocumentAdapter(document);
		IRegion reigion = null;
		try {
			reigion = searcher.find(0, toFind, true, true, false, false);
		} catch (BadLocationException e) {
			System.out.println("Could not find previous snippet to replace, exception.");
			return;
		}
		if(reigion == null) {
			System.out.println("Could not find previous snippet to replace, null.");
			return;
		}
		int offset = reigion.getOffset();
		int length = reigion.getLength();
		
		//replace this range
		DocHandler.addSnippet(InputHandler.generateQueryComment(InputHandler.previousQuery), snippet, offset, length);
		
		InputHandler.previousIndex = 0;
		InputHandler.previousSnippet = snippet;
	}
	
	public static List<Snippet> doTests(String test, List<String> imports) {
		//get surrounding code
		String[] surrounding = DocHandler.getSurrounding(InputHandler.previousOffset, InputHandler.previousLength);
		
		//get previous query snippets
		List<Snippet> snippets = InputHandler.previousSnippets;
		AtomicReference<List<Snippet>> runnableSnippets = new AtomicReference<>();
		
		//load runnable for testing
		//progress of evaluation
		IRunnableWithProgress process = new IRunnableWithProgress() {
		@Override
	    	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				monitor.beginTask("Testing Snippets", 100);
				
				//test snippets
				runnableSnippets.set(Evaluator.testSnippets(monitor, snippets, surrounding[0], surrounding[1], test, imports));
			}
		};
		
		//begin
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().setActive();
        try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(process);
		} catch (InvocationTargetException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(IllegalStateException e) {
			//ignore
		}
				
		
		return runnableSnippets.get();
	}
	
	/**
	 * Extract test case from file.
	 * @return
	 */
	public static List<String> extractTestCase() {
		String document = DocHandler.getDocument().get();
		int start = document.indexOf(TypeHandler.functionStart);
		int end = document.indexOf(TypeHandler.functionEnd);
		
		//if we can't find either
		if(start == -1 || end == -1) {
			System.err.println("Unable to find function start or end.");
			return null;
		}
		
		//get function content
		start += TypeHandler.functionStart.length();
		if(end > document.length()) return null;
		String content = document.substring(start, end);
		
		//get imports
		DocHandler.getImportOffset(null);
		List<String> imports = DocHandler.imports;
		
		//remove function from document
		start -= TypeHandler.functionStart.length();
		DocHandler.replace("", start, TypeHandler.functionStart.length()+content.length()+TypeHandler.functionEnd.length());
		
		List<String> result = new ArrayList<>();
		result.add(content);
		if(imports != null) result.addAll(imports);
		
		return result;
	}
}