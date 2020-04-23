package nlp2testablecode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import nlp2testablecode.code.Snippet;

/**
 * class Searcher
 * 	Replaces the online search functionality of NLP2Code.
 * 	Can process natural language tasks using stemming or lemmatization, and by removing stop words.
 * 	Interfaces with DataHandler to search for queries.
 */
public class Searcher {
	//optional limit on returned queries
	public static int limit = -1;
	
	/**
	 * Searches database for a snippet that matches the given query.
	 * @param query The query to search with.
	 * @return A list of Snippet objects.
	 */
	static public List<Snippet> getSnippets(String query){
		Set<Snippet> retrievedSet = new LinkedHashSet<>(); //preserve order using linked
		List<Snippet> retrieved;
		List<Snippet> snippets = null;
		
		retrieved = getRecommendedSnippets(query);
		if(retrieved != null) {
			//add to map
			for(Snippet s : retrieved) {
				//work with copy of snippets
				Snippet copy = new Snippet(s);
				retrievedSet.add(copy);
			}
		}
		
		retrieved = searchSnippets(query);
		if(retrieved == null) {
			return null;
		}
		for(Snippet s : retrieved) {
			//work with copy of snippets
			Snippet copy = new Snippet(s);
			retrievedSet.add(copy);
		}
		
		//add the set to snippets
		snippets = new ArrayList<>(retrievedSet);
		
		//if we want to restrict the number of snippets per query
		if(limit != -1) {
			if(snippets.size() > limit) {
				snippets = snippets.subList(0, limit);
			}
		}
		
		return snippets;
	}

	
	/**
	 * Gets code snippets from post IDs associated with a recommended task.
	 * @param task The task string to find posts with.
	 * @return A List of retieved Snippet objects.
	 */
	private static List<Snippet> getRecommendedSnippets(String task){
		List<Snippet> snippets = new ArrayList<>();
		
		//get ids
		String tasks = DataHandler.queriesMap.get(task);
		//not in query map
		if(tasks == null) {
			return null;
		}
		
		String[] ids = tasks.split(",");
		
		//use ids to get code snippets
		for(int i=0; i<ids.length; i++) {
			int id = Integer.parseInt(ids[i]);
			snippets.addAll(DataHandler.getSnippet(id));
		}
		
		return snippets;
	}

	/**
	 * Searches for Snippets using a query.
	 * @param query The query to search with.
	 * @return A List of Snippet objects.
	 */
	private static List<Snippet> searchSnippets(String query){
		List<Snippet> snippets = new ArrayList<>();
		
		//get thread ids
		List<Integer> ids = getThreads(query);
		if(ids == null) {
			return null;
		}
		if(ids.size() < 1) {
			return new ArrayList<Snippet>();
		}
		
		//use ids to find snippets
		for(int i=0; i<ids.size(); i++) {
			int id = ids.get(i);
			snippets.addAll(DataHandler.getSnippet(id));
		}
		
		//CycleAnswersHandler.previous_index = 0;
		
		return snippets;
	}
	
	/**
	 * Given a query, returns a List of thread IDs.
	 * @param query The query to search with.
	 * @return A List of Integer thread IDs.
	 */
	private static List<Integer> getThreads(String query){
		List<Integer> threads = new ArrayList<>();
		boolean firstRun = true;
		
		//process query
		String[] words = processQuery(query);
		if(words.length < 1) return null;
		
		//search for each word
		for(String w : words) {
			List<Integer> retrieved = DataHandler.getThreadsWith(w);
			//if we got no results, return an empty thread
			if(retrieved == null) return new ArrayList<Integer>();
			
			//first run, add to threads
			if(firstRun == true) {
				threads.addAll(retrieved);
				firstRun = false;
			}
			//otherwise
			else {
				//hold in a set for o(1) look up
				Set<Integer> retrievedSet = new LinkedHashSet<Integer>();
				for(int id : threads) {
					retrievedSet.add(id);
				}
				//add to threads if contained in both
				threads = new ArrayList<>();
				for(int id : retrieved) {
					if(retrievedSet.contains(id)) {
						threads.add(id);
					}
				}
			}
		}
		
		return threads;
	}
	
	/**
	 * Accept query string, return ordered non-duplicate array of words.
	 * @return The Array of String words. Returns null on error. May be empty.
	 */
	public static String[] processQuery(String query) {
		Set<String> wordSet = new HashSet<String>();
		String[] words;
		
		if(query == "") return new String[0];
		
		//split our query by space
		words = query.split(" ");
		
		//remove stop words from query
		words = removeStopWords(words);
		if(words.length < 1) return words;
		
		//add our words to a hashset to avoid duplicates
		for(int i=0; i<words.length; i++) {
			wordSet.add(words[i]);
		}
		
		words = new String[wordSet.size()];
		words = wordSet.toArray(words);
		//process words
		
		if(DataHandler.processing == DataHandler.STEM) {
			words = DataHandler.stem(words);
		}
		if(DataHandler.processing == DataHandler.LEMMATIZE) {
			String sentence = "";
			for(String word : words) {
				sentence += " " + word;
			}
			words = DataHandler.lemmatize(sentence);
		}
		
		return words;
	}
	
	
	/**
	 * Removed stop words, as specified.
	 * @param words The array of words to filter.
	 * @return The filtered String array. This may be empty.
	 */
	private static String[] removeStopWords(String[] words) {
		List<String> wordList = new ArrayList<String>();
		
		//for each word, check it against our list of stop words
		for(String word : words) {
			if(!DataHandler.stopWords.contains(word)) {
				wordList.add(word);
			}
		}
		
		words = new String[wordList.size()];
		words = wordList.toArray(words);
		
		return words;
	}
}
