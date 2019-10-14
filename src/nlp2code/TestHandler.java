package nlp2code;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/**Pressing CTRL+ALT+D to accept a test function.*/
public class TestHandler extends AbstractHandler{
	public static String functionStart = "\r\n\r\n	@Test\r\n	public void nlp3code_test(){\r\n		//--START EDITING\r\n";
	public static String functionEnd = "		//--END EDITING\r\n	}";

	@Override
	public Object execute(ExecutionEvent arg0) throws ExecutionException {
		
		//must be in the state of writing a function
		if(TestListener.functionState == false) return null;
		
		String test = extractInput();
		
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

}
