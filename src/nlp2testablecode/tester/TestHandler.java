package nlp2testablecode.tester;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import nlp2testablecode.Activator;
import nlp2testablecode.DocHandler;
import nlp2testablecode.Evaluator;
import nlp2testablecode.InputHandler;
import nlp2testablecode.code.Snippet;
import nlp2testablecode.cycler.CycleAnswersHandler;
import nlp2testablecode.listeners.QueryDocListener;
import nlp2testablecode.listeners.TypeDocListener;
import nlp2testablecode.recommenders.TypeRecommender;

/**
 * This class handles functionality to press CTRL+ALT+D to accept a test function.
 */
public class TestHandler extends AbstractHandler{

	/**
	 * Actions performed when CTRL+ALT+D is pressed.
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if(TypeRecommender.canTest == false) return false;
		if(TypeRecommender.testing == false) return false;
		
		//ignore if testing is false
		if(Activator.testing ==  false) {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			MessageDialog.openInformation(shell, "Info", "Testing has been disabled. Enable testing in Activator.java. This will allow arbitrary code to run on your system!");
			return false;
		}
		
		
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
				runnableSnippets.set(Evaluator.testSnippets(monitor, snippets, surrounding[0], surrounding[1], test, imports, TypeDocListener.types));
			}
		};
		
		//begin
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().setActive();
        try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(process);
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		} catch(IllegalStateException e) {
			//ignore
		}
				
		
		return runnableSnippets.get();
	}
	
	/**
	 * Extract test case from file and then remove it.
	 * @return A List containing the test case, then individual import statements.
	 */
	public static List<String> extractTestCase() {
		List<String> result = new ArrayList<>();
		
		//get function
		MethodDeclaration function = DocHandler.findFunction(Tester.JUNITTESTNAME);
		if(function == null) return null;
		
		//get function body
		Block body = function.getBody();
		
		//get statements
		@SuppressWarnings("unchecked")
		List<Statement> statements = body.statements();
		
		//reconstruct testcase
		String testCase = "";
		for(Statement statement : statements) {
			testCase += statement.toString();
		}
		
		//if still empty, no test case error!
		if(testCase.isEmpty()) {
			System.err.println("Unable to find a testcase.");
			return null;
		}
		
		//get imports
		DocHandler.getImportOffset(null);
		List<String> imports = DocHandler.imports;
		
		//add to result
		result.add(testCase);
		if(imports != null) result.addAll(imports);
		
		//then remove from file
		int offset = function.getStartPosition();
		int length = function.getLength();
		
		DocHandler.replace("", offset, length);
	
		return result;
	}
}