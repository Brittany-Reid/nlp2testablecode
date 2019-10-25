package nlp3code.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import nlp3code.DataHandler;
import nlp3code.code.Snippet;

public class DataHandlerTests {
	
	@Test
	public void loadStopWordsTest() {
		DataHandler.loadStopWords();
	}
	
	@Test
	public void loadQuestionsTest() {
		DataHandler.limit = 10000L;
		DataHandler.loadQuestions(null);
		assertEquals(10000, DataHandler.getNumQuestions());
	}
	
	@Test
	public void loadAnswersTest() {
		DataHandler.limit = 10000L;
		DataHandler.loadAnswers(null);
		assertTrue(DataHandler.getNumAnswers() > 0);
	}
	
	@Test
	public void loadTasksTest() {
		DataHandler.loadTasks(null);
	}
	
	@Test
	public void loadAndFind() {
		DataHandler.limit = 1000L;
		DataHandler.loadQuestions(null);
		assertEquals(1000, DataHandler.getNumQuestions());
		//without filtering stop words
		List<Integer> threads = DataHandler.getThreadsWith("an");
		assertNotNull(threads);
		//retrieve snippets
		List<Snippet> snippets = DataHandler.getSnippet(threads.get(0));
		assertNotNull(snippets);
	}
	
	@Test
	public void filterStops() {
		DataHandler.clear();
		DataHandler.limit = 1000L;
		DataHandler.loadStopWords();
		DataHandler.loadQuestions(null);
		List<Integer> threads = DataHandler.getThreadsWith("an");
		assertNull(threads);
	}
	
	@Test
	public void testLemmatization() {
		DataHandler.processing = DataHandler.LEMMATIZE;
		DataHandler.limit = 10000L;
		DataHandler.loadQuestions(null);
		DataHandler.processing = DataHandler.STEM;
		assertEquals(10000, DataHandler.getNumQuestions());
	}
}
