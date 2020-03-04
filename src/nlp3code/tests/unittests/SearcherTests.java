package nlp3code.tests.unittests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Test;

import nlp3code.DataHandler;
import nlp3code.Searcher;
import nlp3code.code.Snippet;

public class SearcherTests {

	/**
	 * Reset Database after each test.
	 */
	@After
	public void after() {
		Searcher.limit = -1;
		DataHandler.clear();
		DataHandler.processing = DataHandler.LEMMATIZE;
	}
	
	/**
	 * Test that queries are processed correctly.
	 */
	@Test
	public void testProcessQuery() throws Exception{
		DataHandler.clear();
		DataHandler.loadStopWords();
		
		String query;
		String[] words;
		
		//case 1, a stop word
		query = "this is a test";
		words = Searcher.processQuery(query);
		assertEquals(1, words.length);
		
		//case 2, only a stop word, empty output
		query = "a";
		words = Searcher.processQuery(query);
		assertEquals(words.length, 0);
		
		//case 3, empty query
		query = "";
		words = Searcher.processQuery(query);
		assertEquals(words.length, 0);
		
		//reset database
		DataHandler.clear();
	}
	
	/**
	 * Test that there are no results for a stop word.
	 */
	@Test
	public void searchStopWord() throws Exception{
		DataHandler.limit = 10000L;
		DataHandler.loadStopWords();
		DataHandler.loadAnswers(null);
		DataHandler.loadQuestions(null);
		
		List<Snippet> snippets = Searcher.getSnippets("a");
		assertNull(snippets);
	}
	
	/**
	 * Search for multiple words.
	 */
	@Test
	public void multiwordSearchTest() throws Exception{
		DataHandler.limit = 10000L;
		DataHandler.loadStopWords();
		DataHandler.loadAnswers(null);
		DataHandler.loadQuestions(null);
		
		List<Snippet> snippets = Searcher.getSnippets("integer string");
		assertNotNull(snippets);
	}
	
	/**
	 * Search for a task.
	 */
	@Test
	public void taskSearch() throws Exception{
		DataHandler.limit = 10000L;
		DataHandler.loadTasks(null);
		DataHandler.loadStopWords();
		DataHandler.loadAnswers(null);
		DataHandler.loadQuestions(null);
		
		List<Snippet> snippets = Searcher.getSnippets("switch statement");
		assertNotNull(snippets);
		assertFalse(snippets.isEmpty());
	}
	
	/**
	 * Check that limit works.
	 */
	@Test
	public void limitedSearch() throws Exception{
		DataHandler.limit = 10000L;
		DataHandler.loadTasks(null);
		DataHandler.loadStopWords();
		DataHandler.loadAnswers(null);
		DataHandler.loadQuestions(null);
		
		Searcher.limit = 1;
		List<Snippet> snippets = Searcher.getSnippets("switch statement");
		assertNotNull(snippets);
		assertFalse(snippets.isEmpty());
		assertTrue(snippets.size() <= Searcher.limit);
	}
	
	/**
	 * Check that limit works even when never reached.
	 */
	@Test
	public void limitButNeverHits() throws Exception{
		DataHandler.limit = 10000L;
		DataHandler.loadTasks(null);
		DataHandler.loadStopWords();
		DataHandler.loadAnswers(null);
		DataHandler.loadQuestions(null);
		
		Searcher.limit = 1000;
		List<Snippet> snippets = Searcher.getSnippets("switch statement");
		assertNotNull(snippets);
		assertFalse(snippets.isEmpty());
		assertTrue(snippets.size() <= Searcher.limit);
		
		//should never reach limit
		assertFalse(snippets.size() == Searcher.limit);
	}
	
	@Test
	public void recommendedTask() throws Exception{
		DataHandler.limit = 100000L;
		DataHandler.loadTasks(null);
		DataHandler.loadStopWords();
		DataHandler.loadAnswers(null);
		DataHandler.loadQuestions(null);
		
		Searcher.limit = 1000;
		List<Snippet> snippets = Searcher.getSnippets("switch statement");
		assertNotNull(snippets);
		assertFalse(snippets.isEmpty());
		assertTrue(snippets.size() <= Searcher.limit);
		
		//should never reach limit
		assertFalse(snippets.size() == Searcher.limit);
	}
	
	@Test
	public void stemTest() throws Exception{
		DataHandler.processing = DataHandler.STEM;
		String[] processed = Searcher.processQuery("going");
		assertEquals(processed[0], "go");
	}
}
