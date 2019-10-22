package nlp3code.code;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import nlp3code.InputHandler;
import nlp3code.listeners.QueryDocListener;

/**
 * A fragment represents a collection of lines of code, as well as a context ID that informs insertion.
 */
public class Fragment {
	private List<Line> lines = null;
	private String codeString = null;
	private String formattedCode = null;
	private int type;
	private int LOC = -1;
	//the comment attached to deleted lines for all snippets
	private static String deletionMessage = " //removed by NLP3Code";
	
	/*types of fragments, this defines how a fragment will be integrated within the users code:
	 * 	we only use snippet and imports for now
	 */
	
	//default, snippets will insert at the insertion point
	public static final int SNIPPET = 0;
	//import blocks, will insert at the beginning of the file
	public static final int IMPORTS = 1;
	//UNUSED
	public static final int METHOD = 2;
	//UNUSED
	public static final int CLASS = 3;
	//UNUSED
	public static final int FIELD = 4;
	
	/**
	 * Constructor for an empty fragment.
	 * @param type The type of fragment, one of the class constants.
	 */
	Fragment(int type){
		this.type = type;
	}
	
	/**
	 * Copy constructor
	 */
	Fragment(Fragment that){
		this.codeString = that.codeString;
		this.formattedCode = that.formattedCode;
		this.type = that.type;
		this.LOC = that.LOC;
		if(that.lines != null) {
			this.lines = new ArrayList<>();
			for(Line line : that.lines) {
				this.lines.add(new Line(line));
			}
		}
	}
	
	public boolean isDeleted(int n) {
		return lines.get(n).isDeleted();
	}
	
	public String getLine(int n) {
		return lines.get(n).get();
	}
	
	/**
	 * String constructor for SNIPPET
	 * @param code
	 */
	Fragment(String code){
		codeString = code;
		lines = null;
		type = SNIPPET;
	}
	
	private void constructLines() {
		if(codeString == null) {
			lines = null;
			LOC = 0;
			return;
		}
		BufferedReader bReader = new BufferedReader(new StringReader(codeString));
		
		lines = new ArrayList<>();
		String line;
		LOC = 0;
		try {
			//read string
			while ( (line = bReader.readLine()) != null) {
				if(!line.trim().equals("") && !line.trim().startsWith("//")){
					LOC++;
				}
				lines.add(new Line(line));
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Clears cache.
	 */
	private void changed() {
		this.codeString = null;
		this.formattedCode = null;
		this.LOC = -1;
	}
	
	/**
	 * Adds a line to the list of lines.
	 */
	public void addLine(String line) {
		if(lines == null) constructLines();
		if(lines != null) {
			//filter duplicates for import blocks
			if(type == IMPORTS) {
				for(Line l : lines) {
					if(l.get().equals(line)) return;
				}
			}
		}else {
			lines  = new ArrayList<>();
		}
		
		lines.add(new Line(line));
		changed();
	}
	
	/**
	 * Deletes a line at the given index.
	 * @param n The int index.
	 */
	public void deleteLine(int n) {
		if(lines == null) constructLines();
		if(lines == null) return;
		if(n >= 0 && n < lines.size()) {
			lines.get(n).delete();
		}
		changed();
	}
	
	/**
	 * Returns a String of code without any formatting.
	 */
	public String getCode() {
		if(codeString != null) return codeString;
		if(lines != null) return constructCode();
		return "";
	}
	
	/**
	 * Returns a String of code how it would be inserted into the workspace.
	 */
	public String getFormattedCode() {
		if(formattedCode != null) return formattedCode;
		return constructFormattedCode();
	}
	
	public int getType() {
		return type;
	}
	
	/**
	 * The size of the fragment, as the size of the line list.
	 */
	public int size() {
		if(lines == null) constructLines();
		if(lines == null) return 0;
		return lines.size();
	}
	
	/**
	 * The number of non-deleted lines of code.
	 */
	public int getLOC() {
		if(LOC != -1) return LOC;
		
		if(codeString == null) constructCode();
		if(lines == null) constructLines();
		return LOC;
	}
	
	
	private String constructCode() {
		if(lines == null) {
			LOC = 0;
			return "";
		}
		
		codeString = "";
		LOC = 0;
		
		for(Line line : lines) {
			//ignore deleted
			if(line.isDeleted() == true) continue;
			//skip unnecessary lines 
			if(line.get().equals("")) continue;
			if(line.get().trim().startsWith("//")) continue;
			
			codeString += line.get() + "\n";
			LOC++;
		}
		
		return codeString;
	}
	
	private String constructFormattedCode() {
		if(lines == null) {
			return "";
		}
		
		formattedCode = "";
		
		//get current whitespace
		String whitespace = InputHandler.whitespaceBefore;
		if(whitespace == null) whitespace = "";
		if(type != SNIPPET) whitespace = "";
		
		for(Line line : lines) {
			String lString = line.get();
			if(line.isDeleted() == true) {
				//ignore imports so we dont clog this up
				if(type == IMPORTS) continue;
				
				//add comment to deleted
				lString = "//" + lString + deletionMessage;
			}
			formattedCode += whitespace + lString + "\n";
		}
		
		return formattedCode;
	}
	
	/**
	 * Delete a line containing an element. This is used to remove duplicate imports.
	 */
	public void deleteLineContaining(String element) {
		if(lines == null) constructLines();
		if(lines == null) return;
		for(Line line : lines) {
			if(line.get().contains(element)) {
				line.delete();
			}
		}
		changed();
	}
}
