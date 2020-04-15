package nlp3code;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.jsoup.Jsoup;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import nlp3code.code.Snippet;

/**
 * Handles loading of data sources, including stack overflow answers, questions, stop words and task recommendations.
 */
public class DataHandler {
	//option to limit number of questions and answers loaded
	public static Long limit = 9999999999L;
	
	//constants
	public static final int STEM = 0;
	public static final int LEMMATIZE = 1;
	public static final int NONE = 3;
	
	//option to use stemming, lemmatization or none
	public static int processing = LEMMATIZE;
	
	//map of snippets to their IDs for snippet look up
	private static HashMap<Integer, List<Snippet>> snippets = new HashMap<Integer, List<Snippet>>();
	//stores our index of words to IDs
	private static HashMap<String, List<Integer>> titleWords = new HashMap<String, List<Integer>>();
	//stores a list of stop words
	public static HashSet<String> stopWords = new HashSet<String>();
	// Stores tasks from the task database file.
	public static HashMap<String,String> queriesMap = new HashMap<String,String>();
	// Stores the list of recommendation tasks/queries for each invocation of the content assist tool.
	public static ArrayList<String> queries = new ArrayList<String>();
	// number of questions loaded
	private static int questions;
	// CoreNLP pipeline
	private static StanfordCoreNLP pipeline = null;
	// if a data base has been updated
	public static boolean loaded = false;
	// data sources
	private static String questionsFile = "data/questions.xml";
	private static String answersFile = "data/answers.xml";
	private static String stopWordsFile = "data/stopwords.txt";
	private static String tasksFile = "data/task,id50.txt";
	
	
	/**
	 * Function for loading all databases. Will report progress through a given monitor.
	 * @param monitor Optional monitor that updates with progress.
	 */
	public static void loadData(IProgressMonitor monitor) {
		//get new submonitor
		SubMonitor sub = null;
		if(monitor != null) {
			sub = SubMonitor.convert(monitor, 100);
		}
		
		try {
			//load stop words
			if(sub != null) {
				sub.split(5);
			}
			loadStopWords();
			
			//load questions
			SubMonitor subQ= null;
			if(sub != null) {
				subQ = sub.split(40);
			}
			loadQuestions(subQ);
			if(monitor != null) {
				monitor.worked(1);
			}
			
			//load answers
			SubMonitor subA= null;
			if(sub != null) {
				subA = sub.split(50);
			}
			loadAnswers(subA);
			
			//load tasks
			if(sub != null) {
				sub.split(5);
			}
			loadTasks(null);
			
	    	loaded = true;
		} catch (IOException e) {
			loaded = false;
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns a List of thread IDs that have titles containing the given word.
	 * @param word The word to search with.
	 * @return A List of Integers.
	 */
	public static List<Integer> getThreadsWith(String word){
		return titleWords.get(word);
	}
	
	/**
	 * Set a non-default file to load from.
	 * @param file Relative path to file to load from (eg, data/questions.xml)
	 */
	public static void setQuestionsFile(String file) {
		questionsFile = file;
	}
	
	/**
	 * Set a non-default file to load from.
	 * @param file Relative path to file to load from (eg, data/questions.xml)
	 */
	public static void setAnswersFile(String file) {
		answersFile = file;
	}
	
	/**
	 * Set a non-default file to load from.
	 * @param file Relative path to file to load from (eg, data/questions.xml)
	 */
	public static void setStopWordsFile(String file) {
		stopWordsFile = file;
	}
	
	/**
	 * Set a non-default file to load from.
	 * @param file Relative path to file to load from (eg, data/questions.xml)
	 */
	public static void setTasksFile(String file) {
		tasksFile = file;
	}
	
	/**
	 * Loads stop words into memory.
	 */
	public static void loadStopWords() throws IOException{
		URL url = getURL(stopWordsFile);
		
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
			
			String line;
			while((line = reader.readLine()) != null) {
				stopWords.add(line.trim().toLowerCase());
			}
			
			reader.close();
			
			//remove redundant language info
			stopWords.add("java");
		} catch (IOException e) {
			System.err.println("Error reading " + stopWordsFile);
			throw(e);
		}
	}
	
	/**
	 * Loads questions from an XML file.
	 * @param monitor Optional monitor that updates with progress.
	 */
	public static void loadQuestions(IProgressMonitor monitor) throws IOException{
		SubMonitor sub = null;
		if(monitor != null) sub = SubMonitor.convert(monitor, 100);
		
		URL url = getURL(questionsFile);
		
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
			
			int num = 0;
			int tick = 1519528/100;
			String line, id, title;
			while((line = reader.readLine()) != null && num < limit) {
				if(!line.contains(" PostTypeId=\"1\"")) continue;
				
				if(num % tick == 0) {
					if(sub != null) sub.split(1);
				}
				
				//get id
				id = line.substring(line.indexOf(" Id=\"")+5, line.length());
				id = id.substring(0, id.indexOf("\""));
				
				//get title
				title = line.substring(line.indexOf(" Title=\"")+8, line.length());
				title = title.substring(0, title.indexOf("\""));
				title = title.toLowerCase();	
				
				//get title words
				String[] words = title.split(" ");
				
				if(processing == STEM) words = stem(words);
				if(processing == LEMMATIZE) words = lemmatize(title);
				
				//for each word in title
				for(int i=0; i<words.length; i++) {
					//filer out stop words
					if(stopWords.contains(words[i])) continue;
					
					List<Integer> ids = new ArrayList<Integer>();
					if(titleWords.containsKey(words[i]) ==  false) {
						ids.add(Integer.parseInt(id));
						titleWords.put(words[i], ids);
					}
					else {
						ids = titleWords.get(words[i]);
						ids.add(Integer.parseInt(id));
						titleWords.replace(words[i], ids);
					}
				}
				
				num++;
			}
			questions = num;
			reader.close();
		} catch (IOException e) {
			System.err.println("Error reading " + questionsFile);
			throw(e);
		}
		
		if(sub != null) sub.done();
	}
	
	/**
	 * Loads answers from an XML file.
	 * @param monitor Optional monitor that updates with progress.
	 */
	public static void loadAnswers(IProgressMonitor monitor) throws IOException{
		SubMonitor sub = null;
		if(monitor != null) sub = SubMonitor.convert(monitor, 100);
		
		URL url = getURL(answersFile);
		String codeStart = "&lt;pre&gt;&lt;code&gt;";
		String codeEnd = "&lt;/code&gt;&lt;/pre&gt;";
		
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
			
			int num = 0;
			int tick = 1386337/100;
			String line, body, id, code, partialCode;
			List<Snippet> codeSnippets;
			int integerID;
			while((line = reader.readLine()) != null && num < limit) {
				if(!line.contains(" ParentId=\"")) continue;
				
				if(num % tick == 0) {
					if(sub != null) sub.split(1);
				}
				
				//get body
				body = line.substring(line.indexOf(" Body=\"")+7, line.length());
				body = body.substring(0, body.indexOf("\""));
				
				//if doesnt't contain a code snippet, skip this answer
				if(!body.contains(codeStart)) continue;
				
				//get id
				id = line.substring(line.indexOf(" ParentId=\"")+11, line.length());
				id = id.substring(0, id.indexOf("\""));
				
				//split by space before <code> and space after </code>
				String[] answerFragments = body.split("(?=" + codeStart+ ")|(?<=" + codeEnd + ")");
				
				//join all code together
				code = "";
				for(int j=0; j<answerFragments.length; j++) {
					//contains code tag, is a code snippet
					if(answerFragments[j].contains(codeStart)) {
						partialCode = answerFragments[j];
						partialCode = partialCode.replaceAll(codeStart, "");
						partialCode = partialCode.replaceAll(codeEnd, "");
						code += partialCode;
					}
				}
				
				code = Jsoup.parse(code).wholeText();
				code = formatResponse(code);
				
				//create the snippet
				integerID = Integer.parseInt(id);
				Snippet snippet = new Snippet(code, integerID);
				
				//if the thread already exists
				if(snippets.containsKey(integerID)) {
					//get the list, add, and replace
					codeSnippets = snippets.get(integerID);
					codeSnippets.add(snippet);
					snippets.replace(integerID, codeSnippets);
				}
				//otherwise create new list and add
				else {
					codeSnippets = new ArrayList<>();
					codeSnippets.add(snippet);
					snippets.put(integerID, codeSnippets);
				}
				
				num++;
			}
			
			
			reader.close();
		} catch (IOException e) {
			System.err.println("Error reading answersFile");
			throw(e);
		}
		
		if(sub != null) sub.done();
	}
	
	/**
	 * Loads in task suggestions from a text file.
	 * @param monitor Optional monitor that updates with progress.
	 * @throws IOException 
	 */
	public static void loadTasks(IProgressMonitor monitor) throws IOException {
		URL url = getURL(tasksFile);
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
			
			String line, task, ids;
			while((line = reader.readLine()) != null) {
				task = line.substring(0, line.indexOf(","));
				
				//Guarantee no accidental uppercase letters
		    	task.toLowerCase();
		    	ids = line.substring(line.indexOf(",")+1);
			    queriesMap.put(task, ids);
			    queries.add(task);
			}
			
			reader.close();
		} catch (IOException e) {
			System.err.println("Error reading " + tasksFile);
			throw(e);
		}
	}
	
	/**
	 * Stemming functionality.
	 * @param words A String Array of words to stem.
	 * @return A String Array of stemmed words.
	 */
	public static String[] stem(String[] words) {
		Stemmer stemmer = new Stemmer();
		
		//stem each word
		for(int i=0; i<words.length; i++) {
			stemmer.add(words[i].toCharArray(), words[i].length());
			stemmer.stem();
			words[i]= stemmer.toString();
		}
		
		return words;
	}
	
	/**
	 * Lemmatization functionality.
	 * @param string A sentence String.
	 * @return An array of String lemmas.
	 */
	public static String[] lemmatize(String string) {
		if(pipeline == null) {
			Properties props = new Properties();
		    props.put("annotators", "tokenize, ssplit, pos, lemma");
		    pipeline = new StanfordCoreNLP(props);
		}
		
		String[] words;
		
		CoreDocument document = new CoreDocument(string);
		
		pipeline.annotate(document);
		
		List<String> lemmas = document.tokens().stream().map(cl -> cl.lemma()).collect(Collectors.toList());
		
		words = new String[lemmas.size()];
		for(int i=0; i<lemmas.size(); i++) {
			words[i] = lemmas.get(i);
		}
		
		return words;
	}
	
	/**
	 * Retrieves a List of Snippet objects from the map given an ID. The returned list will 
	 * contain copies of the original Snippets so that no context-dependent changes can persist.
	 * @param id The id to search with.
	 * @return A List of Snippets for the given ID. Can be empty.
	 */
	public static List<Snippet> getSnippet(int id) {
		List<Snippet> retrieved = new ArrayList<>();
		
		//get code list
		List<Snippet> code = snippets.get(id);
		if(code == null) return retrieved;
		
		//add copies to list
		for(Snippet snippet : code) {
			retrieved.add(new Snippet(snippet));
		}
		
		return retrieved;
	}
	
	/**
	 * Returns the URL from the root directory of the plug-in.
	 */
	public static URL getURL(String url) {
		
		URL result = null;
		try {
			//get the file from the plug-in
			result = new URL("platform:/plugin/nlp3code/" + url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	
	/**
	 * Return the number of stored questions.
	 * @return
	 */
	public static int getNumQuestions() {
		return questions;
	}
	
	/**
	 * Return the number of stored answers.
	 * @return
	 */
	public static int getNumAnswers() {
		return snippets.size();
	}
	
	/**
	 * Clear all databases and resets state.
	 */
	public static void clear() {
		stopWords.clear();
		titleWords.clear();
		snippets.clear();
		queries.clear();
		queriesMap.clear();
		loaded = false;
	}
	
	/**
	 * formats xml within a stack overflow post
	 */
	private static String formatResponse(String post) {
		//Fix xml reserved escape chars:
		post = post.replaceAll("&quot;", "\"");
		post = post.replaceAll("&quot", "\"");
		post = post.replaceAll("&apos;", "'");
		post = post.replaceAll("&apos", "'");
		post = post.replaceAll("&;lt;","<");
		post = post.replaceAll("&lt;","<");
		post = post.replaceAll("&lt", "<");
		post = post.replaceAll("&;gt;",">");
		post = post.replaceAll("&gt;", ">");
		post = post.replaceAll("&gt", ">");
		post = post.replaceAll("&;amp;", "&");
		post = post.replaceAll("&amp;", "&");
		post = post.replaceAll("&amp", "&");
		return post;
	}
}
