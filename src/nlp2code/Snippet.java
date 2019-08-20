package nlp2code;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import org.apache.commons.lang3.*;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A snippet is an object that contains a code snippet and related information.
 * Code is represented by a List of lines, containing deletion state.
 * As well as a String, containing code. The goal is to prioritize
 * quick string access for compiling, and construct lines on demand.
 */
public class Snippet {
	private Boolean showDeletions = true;
	private List<Pair<String, Boolean>> code;
	private String codeString;
	private int id;
	List<Diagnostic<? extends JavaFileObject>> diagnostics;
	private boolean compiled = false;
	private int errors = -1;
	private int LOC = -1;
	
	
	/**
	 * Constructs a snippet object from a String of code;
	 * @param code The code to store in this snippet.
	 */
	Snippet(String code, int id){
		changed();
		codeString = code;
		this.id = id;
	}
	
	/**
	 * Stores the given code, overwriting previous code.
	 * @param code The code to store.
	 */
	public void setCode(String code) {
		changed();
		codeString = code;
	}
	
	/**
	 * Update error information.
	 * @param errors The error value to use.
	 */
	public void updateErrors(Integer errors) {
		this.errors = errors;
		compiled = true;
	}
	
	/**
	 * Function to return the stored code.
	 * @return A String code.
	 */
	public String getCode() {
		if(codeString != null) return codeString;
		return constructCode();
	}
	
	/**
	 * Returns the total size of the code, including deleted lines.
	 */
	public int size() {
		if(code != null) return code.size();
		
		code = constructLines(codeString);
		return code.size();
	}
	
	public int getLOC() {
		if(LOC != -1) return LOC;
		
		//otherwise reconstruct code
		constructCode();
		return LOC;
	}
	
	/**
	 * Returns the code string with SO source appended.
	 */
	public String getFormattedCode() {
		return formatCode();
	}
	
	/**
	 * Function to return the question ID this snippet belongs to.
	 * @return The Integer ID.
	 */
	public int getID() {
		return id;
	}
	
	/**
	 * Private function to reset values on change.
	 */
	private void changed() {
		code = null;
		codeString = null;
		compiled = false;
		diagnostics = null;
		errors = -1;
		LOC = -1;
	}
	
	/**
	 * Using whitespace data from QueryDocListener, adds a fixed offset to each 
	 * line of code in a snippet.Fixes alignment issues.
	 * @return String snippet with a fixed offset.
	 */
	private String formatCode() {
		String source = "//https://stackoverflow.com/questions" + id + "\n";
		String fixed = "";
		
		String spacing = QueryDocListener.getWhitespaceBefore();
		BufferedReader bufReader = new BufferedReader(new StringReader(source + getCode()));
		String line;
		try {
			while ( (line = bufReader.readLine()) != null) {
				line = spacing + line + "\n";
				fixed += line;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return fixed;
	}
	
	/**
	 * Converts a String into a List of Pairs representing lines and their deletion state.
	 */
	private List<Pair<String, Boolean>> constructLines(String code){
		List<Pair<String, Boolean>> lines = new ArrayList<>();
		String line;
		
		BufferedReader bufReader = new BufferedReader(new StringReader(code));
		Boolean deleted = false;
		try {
			while ( (line = bufReader.readLine()) != null) {
				Pair<String, Boolean> linePair = new MutablePair<String, Boolean>(line, deleted);
				lines.add(linePair);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		return lines;
	}
	
	/**
	 * Converts line List into a String.
	 */
	private String constructCode() {
		codeString = "";
		LOC = 0;
		
		for(Pair<String, Boolean> line : code) {
			if(line.getRight() == false) {
				codeString += line.getLeft() + "\n";
				LOC++;
			}
			else if(showDeletions){
				codeString += "//" + line.getLeft() + "\n";
				LOC++;
			}
		}
		
		return codeString;
	}
}
