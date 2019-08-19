package nlp2code.test;

import static org.junit.Assert.*;

import org.junit.Test;
import nlp2code.Searcher;
import nlp2code.DataHandler;

public class SearcherTest {

	
	@Test
	public void testProcessQuery() {
		String query;
		String[] words;
		
		//case 1, a stop word
		query = "this is a test";
		words = Searcher.processQuery(query);
		assertEquals(words.length, 3);
		
		//case 2, only a stop word, empty outout
		query = "a";
		words = Searcher.processQuery(query);
		assertEquals(words.length, 0);
		
		//case 3, empty query
		query = "";
		words = Searcher.processQuery(query);
		assertEquals(words.length, 0);
	}

}
