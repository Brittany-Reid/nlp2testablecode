package nlp3code.tests.unittests;

import org.eclipse.jface.text.Document;
import org.junit.Test;

import nlp3code.DocHandler;

public class DocHandlerTests {
	String contents = "This is a document.\n This is line two.\n";
	
	@Test
	public void initParser() {
		DocHandler.initializeEclipseParser();
	}
	
	@Test
	public void documentChanges() {
		Document document = new Document(contents);
	}
}
