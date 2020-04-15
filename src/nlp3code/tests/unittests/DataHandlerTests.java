package nlp3code.tests.unittests;

import static org.junit.Assert.*;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.After;
import org.junit.Test;

import nlp3code.DataHandler;
import nlp3code.code.Snippet;

public class DataHandlerTests {
	
	/**
	 * Reset Database after each test.
	 */
	@After
	public void after() {
		DataHandler.clear();
		DataHandler.processing = DataHandler.LEMMATIZE;
		DataHandler.setQuestionsFile("data/questions.xml");
		DataHandler.setAnswersFile("data/answers.xml");
		DataHandler.setStopWordsFile("data/stopwords.txt");
		DataHandler.setTasksFile("data/task,id50.txt");
	}
	
	/**
	 * Test the default load.
	 */
	@Test
	public void testLoadData() throws Exception{
		DataHandler.loadData(null);
		
		assertTrue(DataHandler.loaded);
	}
	
	/**
	 * Test monitored default load.
	 */
	@Test
	public void testMonitor() throws Exception{
		DataHandler.limit = 10000L;
		
		//load data job
		Job loadData = new Job("Loading Data") {
	        @Override
	        protected IStatus run(IProgressMonitor monitor) {
	        	monitor.beginTask("Loading Data", 100);
	        	DataHandler.loadData(monitor);
	            return Status.OK_STATUS;
	        }

	    };
	    
	    //schedule job, this will run in the background
	    loadData.setPriority(Job.BUILD);
	    loadData.schedule();
	    
	    while(DataHandler.loaded != true) {
	    	Thread.sleep(100);
	    }
	    
	    assertTrue(DataHandler.loaded);
	}
	
	
	/**
	 * Successful question loading.
	 */
	@Test
	public void loadQuestionsTest() throws Exception{
		DataHandler.limit = 10000L;
		DataHandler.loadQuestions(null);
		assertEquals(10000, DataHandler.getNumQuestions());
	}
	
	/**
	 * Successful answer loading.
	 */
	@Test
	public void loadAnswersTest() throws Exception{
		DataHandler.limit = 10000L;
		DataHandler.loadAnswers(null);
		assertTrue(DataHandler.getNumAnswers() > 0);
	}
	
	/**
	 * Successful task loading.
	 */
	@Test
	public void loadTasksTest() throws Exception{
		DataHandler.loadTasks(null);
		assertFalse(DataHandler.queries.isEmpty());
	}
	
	/**
	 * Successful stopwords loading.
	 */
	@Test
	public void loadStopWordsTest() throws Exception{
		DataHandler.loadStopWords();
		assertFalse(DataHandler.stopWords.isEmpty());
	}
	
	/**
	 * Test missing question file.
	 */
	@Test
	public void missingQuestionFile() {
		DataHandler.setQuestionsFile("data/questions1.xml");
		DataHandler.loadData(null);
		
		assertFalse(DataHandler.loaded);
	}
	
	/**
	 * Test missing answer file.
	 */
	@Test
	public void missingAnswerFile() {
		DataHandler.setAnswersFile("data/answers1.xml");
		DataHandler.loadData(null);
		
		assertFalse(DataHandler.loaded);
	}
	
	/**
	 * Test missing stop word file.
	 */
	@Test
	public void missingStopWordsFile() {
		DataHandler.setStopWordsFile("data/stopwords1.txt");
		DataHandler.loadData(null);
		
		assertFalse(DataHandler.loaded);
	}
	
	/**
	 * Test missing task file.
	 */
	@Test
	public void missingTaskFile() {
		DataHandler.setTasksFile("data/task,id501.txt");
		DataHandler.loadData(null);
		
		assertFalse(DataHandler.loaded);
	}
	
	@Test
	public void loadAndFind() throws Exception{
		DataHandler.limit = 1000L;
		DataHandler.loadQuestions(null);
		DataHandler.loadAnswers(null);
		assertEquals(1000, DataHandler.getNumQuestions());
		
		//without filtering stop words
		List<Integer> threads = DataHandler.getThreadsWith("string");
		assertNotNull(threads);
		
		//retrieve snippets
		List<Snippet> snippets = DataHandler.getSnippet(threads.get(0));
		assertNotNull(snippets);
		assertFalse(snippets.isEmpty());
		
	}
	
	@Test
	public void filterStops() throws Exception{
		DataHandler.clear();
		DataHandler.limit = 1000L;
		DataHandler.loadStopWords();
		DataHandler.loadQuestions(null);
		List<Integer> threads = DataHandler.getThreadsWith("an");
		assertNull(threads);
	}
	
	@Test
	public void testLemmatization() throws Exception{
		DataHandler.processing = DataHandler.STEM;
		DataHandler.limit = 10000L;
		DataHandler.loadQuestions(null);
		assertEquals(10000, DataHandler.getNumQuestions());
	}
}
