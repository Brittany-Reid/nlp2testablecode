package nlp2code;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
public class DataHandler{
	static Logger logger;
	//ids to surrounding text
	static HashMap<Integer, String> searchSpace = new HashMap<Integer, String>();
	//ids to code snippets
	private static HashMap<Integer, List<String>> snippets = new HashMap<Integer, List<String>>();
	//ids to titles
	static HashMap<Integer, String> titles = new HashMap<Integer, String>();
	static HashMap<String, List<Integer>> titlewords = new HashMap<String, List<Integer>>();
	static int NUM_POSTS = 20;
	static Integer processing = 1;
	
	
	/**
	 * Loads answer data from the answer.xml file.
	 */
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
	
	/**
	 * Loads question data from the question.xml file.
	 */
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
//					Sentence sentence = new Sentence(title);
//					splitTitle = new String[sentence.lemmas().size()];
//					for(int i=0; i<sentence.lemmas().size(); i++) {
//						splitTitle[i] = sentence.lemma(i);
//					}
					splitTitle = title.split(" ");
					
					//for each word in title
					for(int i=0; i<splitTitle.length; i++) {
						
						//stem
						stemmer = new Stemmer();
						stemmer.add(splitTitle[i].toCharArray(), splitTitle[i].length());
						stemmer.stem();
						splitTitle[i] = stemmer.toString().toLowerCase();
						
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
	
	/**
	 * Retrieves a List of Snippet objects from the map given an ID.
	 * @param id The id to search with.
	 * @return A List of Snippets for the given ID. Can be empty.
	 */
	public static List<Snippet> getSnippet(int id) {
		List<Snippet> retrieved = new ArrayList<>();
		
		//get code list
		List<String> code = snippets.get(id);
		if(code == null) return retrieved;
		
		for(String snippet : code) {
			retrieved.add(new Snippet(snippet, id));
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