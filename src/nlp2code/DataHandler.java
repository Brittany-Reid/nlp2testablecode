package nlp2code;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import edu.stanford.nlp.simple.Sentence;

import java.util.ArrayList;

/**
 * class DataHandler
 * 	 Handles offline stack overflow database. Should read in an XML file,
 *   and provide functionality to access the resulting data structure.
 */
public class DataHandler{
	static Logger logger = Activator.getLogger();
	//ids to surrounding text
	static HashMap<Integer, String> searchSpace = new HashMap<Integer, String>();
	private static HashMap<Integer, List<Snippet>> snippets = new HashMap<Integer, List<Snippet>>();
	//ids to titles
	static HashMap<Integer, String> titles = new HashMap<Integer, String>();
	static HashMap<String, List<Integer>> titlewords = new HashMap<String, List<Integer>>();
	static int NUM_POSTS = 20;
	static Integer processing = 1;
	
	/**
	 * Load data from questions.xml. This file contains stack overflow threads, and this
	 * function constructs the index table that lets us find thread IDs by word so that
	 * we can later return relevant code snippets from these threads.
	 */
	public static void loadQuestions() {
		String line, id, title;
		try {
			/* This URL assumes the plugin exists in the plugin folder for eclipse.*/
			URL url = new URL("platform:/plugin/nlp2code/data/questions.xml");
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
			int num = 0;
			while((line = reader.readLine()) != null) {
				num = num + 1;
				if(line.contains(" PostTypeId=\"1\"")) {
					//get id
					id = line.substring(line.indexOf(" Id=\"")+5, line.length());
					id = id.substring(0, id.indexOf("\""));
					
					//get title
					title = line.substring(line.indexOf(" Title=\"")+8, line.length());
					title = title.substring(0, title.indexOf("\""));
					title = title.toLowerCase();	
					
					//get title words
					String[] words = title.split(" ");
					words = stem(words);
					
					//for each word in title
					for(int i=0; i<words.length; i++) {
						
						List<Integer> ids = new ArrayList<Integer>();
						if(titlewords.containsKey(words[i]) ==  false) {
							ids.add(Integer.parseInt(id));
							titlewords.put(words[i], ids);
						}
						else {
							ids = titlewords.get(words[i]);
							ids.add(Integer.parseInt(id));
							titlewords.replace(words[i], ids);
						}
					}
					//add to title set
					//we don't use these, save space for now
					//titles.put(Integer.parseInt(id), title);
				}
			}
			reader.close();
		} catch (Exception e) {
			//some error reading data
			e.printStackTrace();
		}
	}

	/**
	 * Load data from answers.xml, the file containing answer posts. This function stores
	 * code snippets in a hashmap that can be accessed using their thread ID.
	 */
	public static void loadAnswers() {
		String line, body, id, code, partialCode;
		List<Snippet> codeSnippets;
		Integer integerID;
		try {
			/* This URL assumes the plugin exists in the plugin folder for eclipse.*/
			URL url = new URL("platform:/plugin/nlp2code/data/answers.xml");
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
			//skip xml ver and post
			line = reader.readLine();
			line = reader.readLine();
			int num = 0;
			while((line = reader.readLine()) != null) {
				num = num + 1;
				//ensure this is an answer
				if(line.contains(" ParentId=\"")) {
					//get body
					body = line.substring(line.indexOf(" Body=\"")+7, line.length());
					body = body.substring(0, body.indexOf("\""));
					
					//get id
					id = line.substring(line.indexOf(" ParentId=\"")+11, line.length());
					id = id.substring(0, id.indexOf("\""));
					
					//if doesnt't contain a code snippet, skip this answer
					if(!body.contains("&lt;pre&gt;&lt;code&gt;")) continue;
					
					//format body
					body = Jsoup.parse(body.replaceAll("&#xA;", "-xA2nl-")).text().replaceAll("-xA2nl-", "\n");
					body = formatResponse(body);
					
					//split by space before <code> and space after </code>
					String[] answerFragments = body.split("(?=<pre><code>)|(?<=</code></pre>)");
					
					//join all code together
					code = "";
					for(int j=0; j<answerFragments.length; j++) {
						//contains code tag, is a code snippet
						if(answerFragments[j].contains("<pre><code>")) {
							partialCode = answerFragments[j];
							partialCode = partialCode.replaceAll("<pre><code>", "");
							partialCode = partialCode.replaceAll("</code></pre>", "");
							code += partialCode;
						}
					}
					
					//construct the code snippet
					//we do this in pre-processing to save time on request
					integerID = Integer.parseInt(id);
					Snippet snippet = new Snippet(code, integerID);
					
					codeSnippets = new ArrayList<>();
					//if id doesn't already exist, initialize the list
					if(snippets.containsKey(integerID) == false) {
						codeSnippets.add(snippet);
						snippets.put(integerID, codeSnippets);
					}
					else {
						//get a copy of the existing list for ID
						if(snippets.get(integerID) != null) {
							codeSnippets.addAll(snippets.get(integerID));
						}
						//delete the old entry
						snippets.remove(integerID);
						//replace old entry
						snippets.put(integerID, codeSnippets);
					}
				}
			}
			reader.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
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
	 * Returns a List of thread IDs that have titles containing the given word.
	 * @param word The word to search with.
	 * @return A List of Integers.
	 */
	public static List<Integer> getThreadsWith(String word){
		return titlewords.get(word);
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
		String[] words;
		Sentence sentence = new Sentence(string);
		
		words = new String[sentence.lemmas().size()];
		for(int i=0; i<sentence.lemmas().size(); i++) {
			words[i] = sentence.lemma(i);
		}

		return words;
	}
}