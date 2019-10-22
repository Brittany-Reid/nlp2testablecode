package nlp3code.tests;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import nlp3code.Searcher;
import nlp3code.code.Snippet;
import nlp3code.DataHandler;

public class SearcherTests {

	@Test
	public void testProcessQuery() {
		DataHandler.clear();
		DataHandler.loadStopWords();
		
		String query;
		String[] words;
		
		//case 1, a stop word
		query = "this is a test";
		words = Searcher.processQuery(query);
		assertEquals(1, words.length);
		
		//case 2, only a stop word, empty outout
		query = "a";
		words = Searcher.processQuery(query);
		assertEquals(words.length, 0);
		
		//case 3, empty query
		query = "";
		words = Searcher.processQuery(query);
		assertEquals(words.length, 0);
	}
	
	@Test
	public void searchStopWord() {
		DataHandler.limit = 10000L;
		DataHandler.clear();
		DataHandler.loadStopWords();
		DataHandler.loadAnswers(null);
		DataHandler.loadQuestions(null);
		
		List<Snippet> snippets = Searcher.getSnippets("a");
		assertNull(snippets);
	}
	
	@Test
	public void searchTest() {
		DataHandler.limit = 10000L;
		DataHandler.clear();
		DataHandler.loadStopWords();
		DataHandler.loadAnswers(null);
		DataHandler.loadQuestions(null);
		
		List<Snippet> snippets = Searcher.getSnippets("integer");
		assertNotNull(snippets);
	}
	
	@Test
	public void multiwordSearchTest() {
		DataHandler.limit = 10000L;
		DataHandler.clear();
		DataHandler.loadStopWords();
		DataHandler.loadAnswers(null);
		DataHandler.loadQuestions(null);
		
		List<Snippet> snippets = Searcher.getSnippets("integer string");
		assertNotNull(snippets);
	}
	
	@Test
	public void taskSearch() {
		DataHandler.limit = 10000L;
		DataHandler.clear();
		DataHandler.loadStopWords();
		DataHandler.loadAnswers(null);
		DataHandler.loadQuestions(null);
		
		List<Snippet> snippets = Searcher.getSnippets("switch statement");
		assertNotNull(snippets);
	}

}
