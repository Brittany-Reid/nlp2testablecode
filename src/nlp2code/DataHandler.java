package nlp2code;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * class DataHandler
 * 	 Handles offline stack overflow database. Should read in an XML file,
 *   and provide functionality to access the resulting data structure.
 */
class DataHandler{
	//ids to surrounding text
	static HashMap<Integer, String> searchSpace = new HashMap<Integer, String>();
	//ids to code snippets
	static HashMap<Integer, List<String>> snippets = new HashMap<Integer, List<String>>();
	//ids to titles
	static HashMap<Integer, String> titles = new HashMap<Integer, String>();
	static int NUM_POSTS = 5;
	
	
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
			while(inputLine != null && num < 2000000) {
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
						searchSpace.put(Integer.parseInt(id), surrounding);
					}
					else {
						//get previous surrounding
						String oldSurrounding = searchSpace.get(Integer.parseInt(id));
						surrounding += " " + oldSurrounding;
						//merge code snippets with old
						if(snippets.get(Integer.parseInt(id)) != null) {
							codeSnippets.addAll(snippets.get(Integer.parseInt(id)));
						}
						//delete old post entry
						snippets.remove(Integer.parseInt(id));
						//replace old entry
						searchSpace.replace(Integer.parseInt(id), surrounding);
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
					
					titles.put(Integer.parseInt(id), title);
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
					code.add(retrievedSnippets.get(j));
				}
			}
		}
		
		return code;
	}

	/*Gets code snippets using a query*/
	public static Vector<String> getSnippets(String query){
		Vector<Integer> results;
		Vector<String> code = new Vector<String>();
		List<String> retrievedSnippets;
		Integer key;
		
		//search for query and return list of ids
		final long startTime = System.currentTimeMillis();
		results = getThreads(query);
		if(results == null) return null;
		final long endTime = System.currentTimeMillis();
		System.out.print("Number of results:" + results.size() + " Time: " + (endTime - startTime) + "ms \n");
		if(results.size() < 1) return null;
		
		//for each id, get code snippets
		for(int i=0; i<results.size(); i++) {
			//until we reach limit
			if(i == NUM_POSTS) break;
			key = results.get(i);
			//add to code
			retrievedSnippets = snippets.get(key);
			if(retrievedSnippets != null) {
				for(int j=0; j<retrievedSnippets.size(); j++) {
					code.add(addInfo(retrievedSnippets.get(j), key));
				}
			}
		}
		
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
		
		authouredSnippet = comment+"http://stackoverflow.com/questions/"+key+"\n" + snippet;
		
		return authouredSnippet;
	}
	
	//search for query
	public static Vector<Integer> getThreads(String query){
		Vector<Integer> threadIDs = new Vector<Integer>();
		String[] words = query.split(" ");
		if(words == null) return null;
		if(words.length < 1) return null;
		
		//first run
		for(Integer key : titles.keySet()) {
			String post = titles.get(key);
			if(post.contains(words[0])) {
				threadIDs.add(key);
			}
		}
		
		//order words
		if(words.length > 1) {
			Arrays.sort(words, Comparator.comparingInt(String::length).reversed());
		}
		
		//for remaining words
		for(int i=1; i<words.length; i++) {
			//run through thread ids
			for(int j=0; j<threadIDs.size(); j++) {
				//get the post
				String post = titles.get(threadIDs.get(j));
				//remove from thread id vector if word doesnt exist
				if(post.contains(words[i]) == false) {
					threadIDs.remove(j);
				}
			}
		}
		
		return threadIDs;
	}
}