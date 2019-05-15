package nlp2code;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import edu.stanford.nlp.simple.Sentence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * class DataHandler
 * 	 Handles offline stack overflow database. Should read in an XML file,
 *   and provide functionality to access the resulting data structure.
 */
class DataHandler{
	static Logger logger;
	//ids to surrounding text
	static HashMap<Integer, String> searchSpace = new HashMap<Integer, String>();
	//ids to code snippets
	static HashMap<Integer, List<String>> snippets = new HashMap<Integer, List<String>>();
	//ids to titles
	static HashMap<Integer, String> titles = new HashMap<Integer, String>();
	static HashMap<String, List<Integer>> titlewords = new HashMap<String, List<Integer>>();
	static int NUM_POSTS = 20;
	
	
	//loads data from xml file
	public static void LoadData() {
		URL url;
		String id;
		String body;
		String code;
		String wholeCode;
		String surrounding;
		String[] strings;
		Integer num = 0;
		List<String> codeSnippets;
		try {
			// Using this url assumes nlp2code exists in the 'plugin' folder for eclipse.
			// This is true when testing the plugin (in a temporary platform) and after installing the plugin.
			url = new URL("platform:/plugin/nlp2code/data/answers.xml");
			InputStream inputStream = url.openConnection().getInputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
			String inputLine = in.readLine();
			//skip xml ver and post
			inputLine = in.readLine();
			inputLine = in.readLine();
			
			//for each entry
			while(inputLine != null) {
				num++;
				
				//for answers
				if(inputLine.contains(" ParentId=\"")) {
					codeSnippets = new ArrayList<String>();
					surrounding = "";
					
					//get body
					body = inputLine.substring(inputLine.indexOf(" Body=\"")+7, inputLine.length());
					body = body.substring(0, body.indexOf("\""));
					
					//get id
					id = inputLine.substring(inputLine.indexOf(" ParentId=\"")+11, inputLine.length());
					id = id.substring(0, id.indexOf("\""));
					
					//if doesnt't contain a code snippet, skip this answer
					if(body.contains("&lt;pre&gt;&lt;code&gt;") == false) {
						inputLine = in.readLine();
						continue;
					}
					
					//format body
					body = Jsoup.parse(body.replaceAll("&#xA;", "-xA2nl-")).text().replaceAll("-xA2nl-", "\n");
					body = formatResponse(body);
					
					
					//split by space before <code> and space after </code>
					strings = body.split("(?=<pre><code>)|(?<=</code></pre>)");
					
					//sort for hashmap
					wholeCode = "";
					for(int j=0; j<strings.length; j++) {
						//contains code tag, is a code snippet
						if(strings[j].contains("<pre><code>")) {
							code = strings[j];
							code = code.replaceAll("<pre><code>", "");
							code = code.replaceAll("</code></pre>", "");
							wholeCode += code;
						}
						else {
							surrounding += " " + strings[j];
						}
					}
					
					//if not already existing
					if(searchSpace.containsKey(Integer.parseInt(id)) == false) {
						codeSnippets.add(wholeCode);
						snippets.put(Integer.parseInt(id), codeSnippets);
						//searchSpace.put(Integer.parseInt(id), surrounding);
					}
					else {
						//get previous surrounding
						//String oldSurrounding = searchSpace.get(Integer.parseInt(id));
						//surrounding += " " + oldSurrounding;
						//merge code snippets with old
						if(snippets.get(Integer.parseInt(id)) != null) {
							codeSnippets.addAll(snippets.get(Integer.parseInt(id)));
						}
						//delete old post entry
						snippets.remove(Integer.parseInt(id));
						//replace old entry
						//searchSpace.replace(Integer.parseInt(id), surrounding);
						snippets.put(Integer.parseInt(id), codeSnippets);
					}
				}
				inputLine = in.readLine();
			}
			in.close();
			
		} catch (IOException e) {
		    e.printStackTrace();
		}
	}
	
	//load question data
	public static void LoadQuestions() {
		//get logger
		logger = Activator.getLogger();
		
		Stemmer stemmer;
		URL url;
		String id, body, title, inputLine;
		Integer num;
		try {
			// Using this url assumes nlp2code exists in the 'plugin' folder for eclipse.
			// This is true when testing the plugin (in a temporary platform) and after installing the plugin.
			url = new URL("platform:/plugin/nlp2code/data/questions.xml");
			InputStream inputStream = url.openConnection().getInputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
			inputLine = in.readLine();
			
			num = 0;
			while(inputLine != null) {
				num++;
				
				if(inputLine.contains(" PostTypeId=\"1\"")) {
					
					//get id
					id = inputLine.substring(inputLine.indexOf(" Id=\"")+5, inputLine.length());
					id = id.substring(0, id.indexOf("\""));
					
					//get title
					title = inputLine.substring(inputLine.indexOf(" Title=\"")+8, inputLine.length());
					title = title.substring(0, title.indexOf("\""));
					title = title.toLowerCase();
					
					String[] splitTitle;
					
					//lemma
					Sentence sentence = new Sentence(title);
					splitTitle = new String[sentence.lemmas().size()];
					for(int i=0; i<sentence.lemmas().size(); i++) {
						splitTitle[i] = sentence.lemma(i);
					}
					//splitTitle = title.split(" ");
					
					//for each word in title
					for(int i=0; i<splitTitle.length; i++) {
						
						//stem
//						stemmer = new Stemmer();
//						stemmer.add(splitTitle[i].toCharArray(), splitTitle[i].length());
//						stemmer.stem();
//						splitTitle[i] = stemmer.toString();
						
						List<Integer> ids = new ArrayList<Integer>();
						if(titlewords.containsKey(splitTitle[i]) ==  false) {
							ids.add(Integer.parseInt(id));
							titlewords.put(splitTitle[i], ids);
						}
						else {
							ids = titlewords.get(splitTitle[i]);
							ids.add(Integer.parseInt(id));
							titlewords.replace(splitTitle[i], ids);
						}
					}
					
					//titles.put(Integer.parseInt(id), title);
				}
				
				inputLine = in.readLine();
			}
			in.close();
			
		}catch (IOException e) {
		    e.printStackTrace();
		}
	}
	
	
	/*Gets code snippets from post ids associated with a recommended task*/
	public static Vector<String> getRecommendedSnippets(String task){
		Vector<String> code = new Vector<String>();
		List<String> retrievedSnippets;
		String[] ids;
		Integer key;
		
		//get IDs
		ids = TaskRecommender.queries_map.get(task).split(",");
		if (ids.length == 0) return null;
		
		//use ids to get code snippets
		for(int i=0; i<ids.length; i++) {
			key = Integer.parseInt(ids[i]);
			retrievedSnippets = snippets.get(key);
			if(retrievedSnippets != null) {
				for(int j=0; j<retrievedSnippets.size(); j++) {
					code.add(addInfo(retrievedSnippets.get(j), key));
				}
			}
		}
		
		return code;
	}

	/*accepts a query, returns vector of snippets*/
	public static Vector<String> getSnippets(String query) {
		Vector<String> snippets = new Vector<String>();
		Vector<String> retrieved;
		
		//if query is a recommended task
		if (TaskRecommender.queries_map.containsKey(query)) {
			//get recommended snippets
			retrieved = getRecommendedSnippets(query);
			if(retrieved.equals(null) == false) {
				snippets.addAll(retrieved);
			}
			retrieved.clear();
		}
		
		if(snippets.size() < 10) {
			retrieved = searchSnippets(query);
			if(retrieved.equals(null) == false) {
				snippets.addAll(retrieved);
			}
		}
		
		return snippets;
	}
	
	/*gets code snippets using a query from ids*/
	public static Vector<String> searchSnippets(String query){
		Vector<Integer> results;
		Vector<String> code = new Vector<String>();
		List<String> retrievedSnippets;
		Integer key;
		Integer count;
		
		//search for query and return list of ids
		final long startTime = System.currentTimeMillis();
		results = getThreads(query);
		final long endTime = System.currentTimeMillis();
		if(results == null) {
			logger.debug(query + ", 0, 0, " + (endTime - startTime)+ "ms\n");
			return null;
		}
		if(results.size() < 1) {
			logger.debug(query + ", 0, 0, " + (endTime - startTime)+ "ms\n");
			return null;
		}
		
		count = 0;
		//for each id, get code snippets
		for(int i=0; i<results.size(); i++) {
			//until we reach limit
			//if(count > NUM_POSTS) break;
			key = results.get(i);
			//add to code
			retrievedSnippets = snippets.get(key);
			if(retrievedSnippets != null) {
				for(int j=0; j<retrievedSnippets.size(); j++) {
					code.add(addInfo(retrievedSnippets.get(j), key));
					count++;
				}
			}
		}
		logger.debug(query + ", " + results.size() + ", " + code.size() + ", " + (endTime - startTime)+ "ms\n");
		
		CycleAnswersHandler.previous_index = 0;
		
		return code;
	}
	
	/*formats xml within a stack overflow post*/
	private static String formatResponse(String post) {
		//Fix xml reserved escape chars:
		post = post.replaceAll("&;quot;", "\"");
		post = post.replaceAll("&quot;", "\"");
		post = post.replaceAll("&quot", "\"");
		post = post.replaceAll("&;apos;", "'");
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
	
	/*append url as comment to code snippet*/
	private static String addInfo(String snippet, Integer key){
		String authouredSnippet;
		String comment = "//";
		
		authouredSnippet = comment+"https://stackoverflow.com/questions/"+key+"\n" + snippet;
		
		return authouredSnippet;
	}
	
	/*Accept query string, return ordered non-duplicate array of words*/
	private static String[] processQuery(String query) {
		String[] result, temp;
		Stemmer stemmer;
		Integer n;
		Set<String> wordSet = new HashSet<String>();
		
		//check for redundant langauge info
		if(query.contains("in java")) {
			query.replace("in java", "");
		}
		
		//split by space
		//temp = query.split(" ");
		
		//lemma
		Sentence sentence = new Sentence(query);
		temp = new String[sentence.lemmas().size()];
		n = 0;
		for(String lemma : sentence.lemmas()) {
			temp[n] = lemma;
			n++;
		}
		
		//if(temp == null) return null;
		if(temp.length < 1) return temp;
		
		
		//for each word
		for(int i=0; i<temp.length; i++) {
			//add to word set, removing duplicates
			if(temp[i] != "java") {
				wordSet.add(temp[i]);
			}
		}
		
		//add to result array
		result = new String[wordSet.size()];
		
		n = 0;
		for(String s : wordSet) {
//			//stem
//			stemmer = new Stemmer();
//			stemmer.add(s.toCharArray(), s.length());
//			stemmer.stem();
//			s = stemmer.toString();
			result[n] = s;
			n++;
		}
		
		//order
		if(result.length > 1) {
			Arrays.sort(result, Comparator.comparingInt(String::length).reversed());
		}
		
		return result;
	}
	
	/*Accepts query string and searches for threads that match*/
	public static Vector<Integer> getThreads(String query){
		Vector<Integer> threadIDs = new Vector<Integer>();
		String[] words = processQuery(query);
		if(words == null) return null;
		if(words.length < 1) return null;

		//for each word, search
		for(int i=0; i<words.length; i++) {
			//get list of ids with word
			List<Integer> retrieved = titlewords.get(words[i]);
			//first run, add to threadIDs
			if(i == 0) {
				if(retrieved != null) {
					Vector<Integer> temp = new Vector<>(retrieved);
					threadIDs.addAll(temp);
				}
			}
			//subsequent runs
			else {
				//add previous to hashmap for O(1) check
				HashMap<Integer, Integer> ti = new HashMap<Integer, Integer>();
				for(int j=0; j<threadIDs.size(); j++) {
					//
					ti.put(threadIDs.elementAt(j), 1);
				}
				//use hashmap to check if new word results exist within old results, add to temp
				Vector<Integer> temp = new Vector<Integer>();
				if(retrieved != null) {
					for(int j=0; j<retrieved.size(); j++) {
						if(ti.containsKey(retrieved.get(j))) {
							temp.add(retrieved.get(j));
						}
					}
				}
				//replace threadIDs with temp
				threadIDs = temp;
			}
				
		}
		
		return threadIDs;
	}
}