package nlp2code;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import nlp2code.tester.Tester;

/**Pressing CTRL+ALT+D to accept a test function.*/
public class TestHandler extends AbstractHandler{
	public static String functionStart = "\r\n\r\n	@Test\r\n	public void nlp3code_test(){\r\n		//--START EDITING\r\n";
	public static String functionEnd = "		//--END EDITING\r\n	}";

	@Override
	public Object execute(ExecutionEvent arg0) throws ExecutionException {
		
		//must be in the state of writing a function
		if(TestListener.functionState == false) return null;
		
		//get the test and remove the function
		String test = extractInput();
		
		doTests(test);
		
		//reset the state
		TestListener.functionState = false;
		
		return null;
	}
	
	public String extractInput() {
		String document = DocumentHandler.getDocument().get();
		int start = document.indexOf(functionStart);
		int end = document.indexOf(functionEnd);
		
		//if we can't find either
		if(start == -1 || end == -1) {
			System.err.println("Unable to find function start or end.");
			return null;
		}
		
		//get function content
		start += functionStart.length();
		if(end > document.length()) return null;
		String content = document.substring(start, end);
		
		//remove function from document
		start -= functionStart.length();
		int err = DocumentHandler.removeAt(start, functionStart.length()+content.length()+functionEnd.length());
		if(err == -1) System.err.println("No Document");
		if(err == -2) System.err.println("Range beyond document.");
		
		return content;
	}

	public void doTests(String test) {
		//get surrounding code
		String[] surrounding = DocumentHandler.getSurrounding();
		
		//get previous query snippets
		List<Snippet> snippets = InputHandler.previous_search;
		
		//test snippets
		snippets = Evaluator.testSnippets(snippets, surrounding[0], surrounding[1], test);
		
		//replace previous search
		InputHandler.previous_search = snippets;
		
		InputHandler.previousInfo = QueryDocListener.generateNewInfo(InputHandler.previous_query);
		
		DocumentHandler.replaceSnippet(snippets.get(0));
	}
}
