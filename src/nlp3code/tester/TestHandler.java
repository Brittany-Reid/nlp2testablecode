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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import nlp3code.DocHandler;
import nlp3code.Evaluator;
import nlp3code.InputHandler;
import nlp3code.code.Snippet;
import nlp3code.cycler.CycleAnswersHandler;
import nlp3code.listeners.QueryDocListener;
import nlp3code.recommenders.TypeRecommender;
import nlp3code.tester.Tester;

/**Pressing CTRL+ALT+D to accept a test function.*/
public class TestHandler extends AbstractHandler{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if(TypeRecommender.canTest == false) return false;
		if(TypeRecommender.testing == false) return false;
		
		//get test case
		String test = extractTestCase();
		List<Snippet> tested = doTests(test);
		if(tested != null) {
			insertSnippets(tested);
		}
		
		TypeRecommender.canTest = false;
		
		return null;
	}
	
	public static void insertSnippets(List<Snippet> snippets) {
		
		CycleAnswersHandler.replaceSnippet(snippets.get(0));
		InputHandler.previousIndex = 0;
		InputHandler.previousSnippets = snippets;
		
		//enable cycling
		QueryDocListener.currentDocument.addDocumentListener(InputHandler.cycleDocListener);
		//associate a document
		InputHandler.cycleDocListener.currentDocument = QueryDocListener.currentDocument;
		CycleAnswersHandler.changedDoc = false;
	}
	
	public static List<Snippet> doTests(String test) {
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
				runnableSnippets.set(Evaluator.testSnippets(monitor, snippets, surrounding[0], surrounding[1], test));
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
	public static String extractTestCase() {
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
		
		//remove function from document
		start -= TypeHandler.functionStart.length();
		DocHandler.replace("", start, TypeHandler.functionStart.length()+content.length()+TypeHandler.functionEnd.length());
		
		return content;
	}
}