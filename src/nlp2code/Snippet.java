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
public class Snippet implements Comparable<Snippet>{
	private Boolean showDeletions = true;
	private List<Pair<String, Boolean>> code;
	private String codeString;
	private String formattedCodeString;
	private int id;
	List<Diagnostic<? extends JavaFileObject>> diagnostics;
	private boolean compiled = false;
	private int errors = -1;
	private int LOC = -1;
	private static String deletionMessage = " //removed by NLP3Code";
	
	
	/**
	 * Constructs a snippet object from a String of code;
	 * @param code The code to store in this snippet.
	 */
	public Snippet(String code, int id){
		changed();
		codeString = code;
		this.id = id;
	}
	
	/**
	 * Copy constructor to facilitate deep copies.
	 * @param that The snippet to make a copy of.
	 */
	public Snippet(Snippet that) {
		if(that.code == null) code = null;
		else {
			code = new ArrayList<>();
			for(Pair<String, Boolean> line : that.code) {
				code.add(new MutablePair<String, Boolean>(line.getLeft(), line.getRight()));
			}
		}
		codeString = that.codeString;
		formattedCodeString = that.formattedCodeString;
		id = that.id;
		diagnostics = that.diagnostics;
		compiled = that.compiled;
		errors = that.errors;
		LOC = that.LOC;
	}
	
	@Override
	public int compareTo(Snippet b) {
		//empty snippets vs non-empty compiling
		if(b.getCode() == "" && getCode() != "") return -1;
		if(getCode() == "" && b.getCode() != "") return 1;
		
		//handle negative error value
		if(b.getErrors() == -1 && errors != -1) return -1;
		if(b.getErrors() != -1 && errors == -1) return 1;
		
		//compare error value
		return Integer.compare(errors, b.getErrors());
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
	public void updateErrors(Integer errors, List<Diagnostic<? extends JavaFileObject>> diagnostics) {
		this.errors = errors;
		this.diagnostics = diagnostics;
		compiled = true;
	}
	
	/**
	 * Function to return the stored code string, without any formatting.
	 * @return A String code.
	 */
	public String getCode() {
		if(codeString != null) return codeString;
		return constructCode();
	}
	
	/**
	 * Returns the String line at the given point.
	 * @param lineNum The line to return, starting at 1
	 */
	public String getLine(int lineNum) {
		if(code == null) constructLines(codeString);
		return code.get(lineNum-1).getLeft();
	}
	
	/**
	 * Returns the number of errors. Returns -1 if snippet hasn't been compiled.
	 * @return
	 */
	public int getErrors() {
		return errors;
	}
	
	public boolean isCompiled() {
		return compiled;
	}
	
	/**
	 * Returns if the given line number is deleted.
	 * @param line The line to check.
	 * @return A boolean representing deletion state.
	 */
	public boolean getDeleted(int line) {
		if(code == null) constructLines(codeString);
		return code.get(line-1).getRight();
	}
	/**
	 * Deletes a given line, starting at line 1.
	 * @param lineNum
	 */
	public void deleteLine(int lineNum) {
		
		//get a copy of lines
		if(code == null) constructLines(codeString);
		List<Pair<String, Boolean>> modified = code;
		
		//discard all cached information
		changed();
		
		//set this line to deleted
		Pair<String, Boolean> line = modified.get(lineNum-1);
		line.setValue(true);
		
		code = modified;
	}
	
	/**
	 * Returns the total size of the code, including deleted lines.
	 */
	public int size() {
		if(code != null) return code.size();
		
		code = constructLines(codeString);
		return code.size();
	}
	
	/**
	 * Return the number of non-deleted lines.
	 */
	public int getLOC() {
		if(LOC != -1) return LOC;
		
		if(codeString == null) constructCode();
		if(code == null) constructLines(codeString);
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
		formattedCodeString = null;
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
		//if we have a cahced  code string
		if(formattedCodeString != null) return formattedCodeString;

		//otherwise, construct
		String source = "//https://stackoverflow.com/questions" + id + "\n";
		String line;
		
		//get spacing
		String spacing = QueryDocListener.getWhitespaceBefore();
		if(spacing == null) spacing = "";
		
		formattedCodeString = spacing + source;

		//if we have a list of lines, construct formattedCodeString from this
		if(code != null) {
			for(Pair<String, Boolean> linePair : code) {
				//add lines if not deleted
				if(linePair.getRight() == false) {
					line = linePair.getLeft();
					formattedCodeString += spacing + line + "\n";
				}
				else if(showDeletions){
					line = linePair.getLeft();
					formattedCodeString += spacing + "//" + line + deletionMessage + "\n";
				}
			}
		}
		//otherwise, we must use the codeString
		else {
			BufferedReader bufReader = new BufferedReader(new StringReader(getCode()));
			try {
				while ( (line = bufReader.readLine()) != null) {
					formattedCodeString += spacing + line + "\n";
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
//		
		return formattedCodeString;
	}
	
	/**
	 * Converts a String into a List of Pairs representing lines and their deletion state.
	 */
	private List<Pair<String, Boolean>> constructLines(String code){
		List<Pair<String, Boolean>> lines = new ArrayList<>();
		String line;
		LOC = 0;
		
		BufferedReader bufReader = new BufferedReader(new StringReader(code));
		Boolean deleted = false;
		try {
			while ( (line = bufReader.readLine()) != null) {
				if(deleted == false) {
					LOC++;
				}
				Pair<String, Boolean> linePair = new MutablePair<String, Boolean>(line, deleted);
				lines.add(linePair);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.code = lines;
		return lines;
	}
	
	/**
	 * Converts line List into a String with no formatting.
	 */
	private String constructCode() {
		codeString = "";
		LOC = 0;
		
		for(Pair<String, Boolean> line : code) {
			if(line.getRight() == false) {
				String lineString = line.getLeft();
				
				//skip unneccessary lines 
				if(lineString.equals("")) continue;
				if(lineString.trim().startsWith("//")) continue;
				codeString += line.getLeft() + "\n";
				LOC++;
			}
		}
		
		return codeString;
	}
}
