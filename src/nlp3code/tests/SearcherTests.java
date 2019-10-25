package nlp3code.tests;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.junit.Test;
import nlp3code.Searcher;
import nlp3code.code.Snippet;
import nlp3code.Activator;
import nlp3code.DataHandler;

public class SearcherTests {
	//was going to use the logger set up for junit tests but it wont recognize the file :/
	Logger logger = Activator.logger;
	
	//data tests
	
	@Test
	public void searchResultsNoProcessing() {
		System.out.println("NO PROCESSING: ");
		DataHandler.limit = 9999999999L;
		DataHandler.processing = DataHandler.NONE;
		DataHandler.clear();
		DataHandler.loadStopWords();
		DataHandler.loadTasks(null);
		DataHandler.loadQuestions(null);
		DataHandler.loadAnswers(null);
		int total = 0;
		long start = System.currentTimeMillis();
		for(String query : DataHandler.queries) {
			List<Snippet> snippets = Searcher.getSnippets(query);
			total += snippets.size();
			System.out.print("TASK: " + query + ", " + snippets.size()+"\n");
		}
		long end = System.currentTimeMillis() - start;
		System.out.print("TIME: " + end + "ms\n");
		DataHandler.processing = DataHandler.STEM;
		System.out.print("TOTAL: " + total+"\n");
	}
	
	@Test
	public void searchResultsStemming() {
		System.out.println("STEMMING: ");
		DataHandler.limit = 9999999999L;
		DataHandler.processing = DataHandler.STEM;
		DataHandler.clear();
		DataHandler.loadStopWords();
		DataHandler.loadTasks(null);
		DataHandler.loadQuestions(null);
		DataHandler.loadAnswers(null);
		int total = 0;
		long start = System.currentTimeMillis();
		for(String query : DataHandler.queries) {
			List<Snippet> snippets = Searcher.getSnippets(query);
			total += snippets.size();
			System.out.print("TASK: " + query + ", " + snippets.size()+"\n");
		}
		long end = System.currentTimeMillis() - start;
		System.out.print("TIME: " + end + "ms\n");
		DataHandler.processing = DataHandler.STEM;
		System.out.print("TOTAL: " + total+"\n");
	}
	
	@Test
	public void searchResultsLemma() {
		System.out.println("LEMMA: ");
		DataHandler.limit = 9999999999L;
		DataHandler.processing = DataHandler.LEMMATIZE;
		DataHandler.clear();
		DataHandler.loadStopWords();
		DataHandler.loadTasks(null);
		DataHandler.loadQuestions(null);
		DataHandler.loadAnswers(null);
		int total = 0;
		long start = System.currentTimeMillis();
		for(String query : DataHandler.queries) {
			List<Snippet> snippets = Searcher.getSnippets(query);
			total += snippets.size();
			System.out.print("TASK: " + query + ", " + snippets.size()+"\n");
		}
		long end = System.currentTimeMillis() - start;
		System.out.print("TIME: " + end + "ms\n");
		DataHandler.processing = DataHandler.STEM;
		System.out.print("TOTAL: " + total+"\n");
	}
	
	
	//feature tests

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
		DataHandler.loadTasks(null);
		DataHandler.loadStopWords();
		DataHandler.loadAnswers(null);
		DataHandler.loadQuestions(null);
		
		List<Snippet> snippets = Searcher.getSnippets("switch statement");
		assertNotNull(snippets);
	}

}
