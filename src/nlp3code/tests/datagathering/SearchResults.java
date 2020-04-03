package nlp3code.tests.datagathering;

import static org.junit.Assert.assertFalse;

import java.util.List;

import org.junit.After;
import org.junit.Test;

import nlp3code.DataHandler;
import nlp3code.Searcher;
import nlp3code.code.Snippet;

public class SearchResults {
	
	@After
	public void after() {
		DataHandler.clear();
		DataHandler.processing = DataHandler.LEMMATIZE;
		DataHandler.limit = 9999999999L;
	}

	/**
	 * integer and int are not correctly lemmatized by corenlp
	 */
	@Test
	public void lemmaInt() {
		assertFalse(DataHandler.lemmatize("integer")[0] == DataHandler.lemmatize("int")[0]);
		System.out.println(DataHandler.lemmatize("integer")[0]);
		System.out.println(DataHandler.lemmatize("int")[0]);
	}
	
	/**
	 * Number of results per task without stop words or query processing.
	 * @throws Exception
	 */
	@Test
	public void searchResultsNoProcessingNoStops() throws Exception{
		System.out.println("NO PROCESSING NO STOPS: ");
		DataHandler.processing = DataHandler.NONE;
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
		System.out.print("TOTAL: " + total+"\n");
	}
	
	@Test
	public void searchResultsStemmingNoStops() throws Exception{
		System.out.println("STEMMING NO STOPS: ");
		DataHandler.processing = DataHandler.STEM;
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
		System.out.print("TOTAL: " + total+"\n");
	}
	
	@Test
	public void searchResultsLemmaNoStops() throws Exception{
		System.out.println("LEMMA NO STOPS: ");
		DataHandler.processing = DataHandler.LEMMATIZE;
		DataHandler.clear();
		//DataHandler.loadStopWords();
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
	public void searchResultsNoProcessing() throws Exception{
		System.out.println("NO PROCESSING ONLY STOPS: ");
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
		System.out.print("TOTAL: " + total+"\n");
	}
	
	@Test
	public void searchResultsStemming() throws Exception{
		System.out.println("STEMMING: ");
		DataHandler.processing = DataHandler.STEM;
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
	public void searchResultsLemma() throws Exception{
		System.out.println("LEMMA: ");
		DataHandler.processing = DataHandler.LEMMATIZE;
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
		System.out.print("TOTAL: " + total+"\n");
	}

}
