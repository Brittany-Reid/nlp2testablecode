package nlp2code.test;

import static org.junit.Assert.*;
import nlp2code.DataHandler;

import org.junit.Test;

public class DataHandlerTest {

	@Test
	public void testLemmatization() {
		String sentence = "String";
		String[] lemmas = DataHandler.lemmatize(sentence);
		assertEquals("String", lemmas[0]);
	}

}
