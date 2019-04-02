package nlp2code;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import org.jsoup.Jsoup;

/**
 * class DataHandler
 * 	 Handles offline stack overflow database. Should read in an XML file,
 *   and provide functionality to access the resulting data structure.
 */
class DataHandler{
	//a hashmap of surrounding text and a list of code snippets 
	static HashMap<String, List <String>> posts = new HashMap<String, List<String>>();
	//a hashmap of post id to surrounding text
	static HashMap<Integer, String> postIds = new HashMap<Integer, String>();
	
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
			url = new URL("platform:/plugin/nlp2code/data/taskAnswers.xml");
			InputStream inputStream = url.openConnection().getInputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
			String inputLine = in.readLine();
			//skip xml ver and post
			inputLine = in.readLine();
			inputLine = in.readLine();
			
			//for each entry
			while(inputLine != null) {
				num++;
				if(inputLine.contains(" ParentId=\"")) {
					codeSnippets = new ArrayList<String>();
					surrounding = "";
					
					//get id
					id = inputLine.substring(inputLine.indexOf(" ParentId=\"")+11, inputLine.length());
					id = id.substring(0, id.indexOf("\""));
					
					//get body
					body = inputLine.substring(inputLine.indexOf(" Body=\"")+7, inputLine.length());
					body = body.substring(0, body.indexOf("\""));
					
					//use Jsoup to covert from html
					body = Jsoup.parse(body.replaceAll("&#xA;", "-xA2nl-")).text().replaceAll("-xA2nl-", "\n");
					
					//split by space before <code> and space after </code>
					strings = body.split("(?=<code>)|(?<=</code>)");
					
					//sort for hashmap
					wholeCode = "";
					for(int j=0; j<strings.length; j++) {
						//contains code tag, is a code snippet
						if(strings[j].contains("<code>")) {
							code = strings[j];
							code = code.replaceAll("<code>", "");
							code = code.replaceAll("</code>", "");
							wholeCode += code;
						}
						else {
							surrounding += " " + strings[j];
						}
					}
					
					//if not already existing
					if(postIds.containsKey(Integer.parseInt(id)) == false) {
						codeSnippets.add(wholeCode);
						postIds.put(Integer.parseInt(id), surrounding);
						posts.put(surrounding, codeSnippets);
					}
					else {
						//get previous surrounding
						String oldSurrounding = postIds.get(Integer.parseInt(id));
						surrounding += " " + oldSurrounding;
						//merge code snippets with old
						if(posts.get(oldSurrounding) != null) {
							codeSnippets.addAll(posts.get(oldSurrounding));
						}
						//delete old post entry
						posts.remove(oldSurrounding);
						//replace old entry
						postIds.replace(Integer.parseInt(id), surrounding);
						posts.put(surrounding, codeSnippets);
					}
				}
				inputLine = in.readLine();
			}
			in.close();
			
		} catch (IOException e) {
		    e.printStackTrace();
		}
	}
		
}