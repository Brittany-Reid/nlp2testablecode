package nlp2code;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

import nlp2code.compiler.IMCompiler;

/**
 * 	class Fixer
 *  Handles error fix functionality
 */
public class Fixer{
	private static Integer newErrorCount;
	private static Boolean order = false;
	private static Boolean neutrality = false;
	private static Boolean loop = false;
	public static Integer beforeLines; //Stores lines before code snippet
	private static IMCompiler compiler;
	
	/**
	 * Sets options for fixing.
	 * @param o : Order of deletion, False for down, True for up.
	 * @param n : False if we only accept deletions that give us strict improvement, 
	 * 	True if we also accept deletions that make no change.
	 * @param l : Looping, True if we run deletion over a snippet until no more improvements can be made
	 *  False if we only pass over the snippet once.
	 */
	public static void setOptions(Boolean o, Boolean n, Boolean l) {
		order = o;
		neutrality = n;
		loop = l;
		compiler = new IMCompiler(Evaluator.javaCompiler, Evaluator.options);
	}
	
	/**
	 * Runs our heuristic fixes on the supplied snippet.
	 */
	public static String heuristicFixes(String before, String code, String after, Integer errors) {
		String finalSnippet = code;
		Boolean hasFix = false;
		newErrorCount = errors;
		
		//get before lines
		beforeLines = before.split("\n", -1).length -1;
		
		//get diagnostics for this snippet
		DiagnosticCollector<JavaFileObject> diagnostics = Evaluator.getDiagnostics(code);
		
		//use diagnostics to determine which fix
		for(Diagnostic<?extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
			//get the error code
			String errorCode = diagnostic.getCode();
			
			//insert to complete
			if(errorCode.equals("1610612976")){
				String message = diagnostic.getMessage(null);
				Integer line = (int)diagnostic.getLineNumber() - beforeLines - 1;
				
				//semi-colon
				if(message.startsWith("Syntax error, insert \";\" to complete ")) {
					finalSnippet = semiColon(finalSnippet, line);
					if(finalSnippet != code) hasFix = true;
				}
			}
		}
		
		//if a fix was applied, compile to get new error count
		if(hasFix == true) {
			IMCompiler compiler = new IMCompiler(Evaluator.javaCompiler, Evaluator.options);
			compiler.addSource(Evaluator.className, before+finalSnippet+after);
			try {
				compiler.compileAll();
			} catch (Exception e) {
				e.printStackTrace();
			}
			newErrorCount = compiler.getErrors();
		}
		
		return finalSnippet;
	}
	
	/**
	 * Deletion fix, deletes lines to reduce compiler errors.
	 */
	public static String tryDeletion(String before, String code, String after, Integer errors) {
		compiler = new IMCompiler(Evaluator.javaCompiler, Evaluator.options);
		
		//don't log compiler errors
		IMCompiler.logging = false;
		Integer lines = code.split("\n").length;
		String modified;
		
		//get our current best
		String finalSnippet = code;
		Integer minErrors = errors;
		
		//runs over the snippet
		Boolean done = false;
		while(done == false) {
			//a single loop
			if(!loop) done = true;
			//order of deletion
			Integer toDelete = 1;
			if(order == true) toDelete = lines - 1;
			done = true;
			//delete the first line
			modified = deleteLine(finalSnippet, toDelete);
			while(modified != null) {
				//compile modified snippet
				compiler.addSource(Evaluator.className, before+modified+after);
				try {
					compiler.compileAll();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				//get errors
				Integer candidateErrors = compiler.getErrors();
				//if deletion removed errors
				if(candidateErrors < minErrors && neutrality == false){
					//new best
					finalSnippet = modified;
					minErrors = candidateErrors;
					if(loop) done = false;
				}
				//if deletion removed errors or remained the same
				else if(candidateErrors <= minErrors && neutrality == true){
					//new best
					finalSnippet = modified;
					minErrors = candidateErrors;
					//logger.debug("Reduced errors to: " + errors + "\n----------\n");
					if(loop) done = false;
				}
				//if we dont accept this modification
				else {
					//skip line
					if(order == false) toDelete++;
				}
				if(order == true) toDelete--;
				//break loop if we've reduced errors to 0
				if(minErrors == 0) break;
				
				//next
				modified = deleteLine(finalSnippet, toDelete);
				
				compiler.clearSaved();
			}
		}
		
		//set this back
		IMCompiler.logging = true;
		
		newErrorCount = minErrors;
		return finalSnippet;
	}

	
	/**
	 * Deletion algorithm, attempts to reduce compiler errors.
	 */
	public static String delete(String code, String before, String after, Integer errors) {
		String modified = null;
		
		
		
		return modified;
	}
	
	/**
	 * Returns last fix error count.
	 */
	public static Integer getLastFixErrorCount() {
		return newErrorCount;
	}

	/**
	 * Private: deletes a specified line.
	 */
	private static String deleteLine(String code, Integer line) {
		String modified;
		String[] lines;
		Integer length;
		
		//split the snippet by nl for lines
		lines = code.split("\n");
		length = lines.length;
		
		//if selected line longer, return null
		if(line >= length || line < 0) {
			return null;
		}
		
		//delete line
		modified = "";
		for(Integer i = 0; i<length; i++) {
			if(i != line) {
				modified += lines[i] + "\n";
			}
		}
		
		return modified;
	}


	/* Semi-colon fix function
	 * Inserts semi-colon at line in passed string code
	 */
	private static String semiColon(String code, Integer line) {
		String[] lines;
		String start;
		String end;
		
		//get lines
		lines = code.split("\n");
		//ignore error messages after snippet
		if(line >= lines.length) return code;
		//ignore error messages before snippet
		if(line < 0) return code;
		//TODO if this happens, wrong line? 
		if(lines[line].length() < 1) return code;
		
		//get insertion point
		Integer index = findEnd(lines[line]);
		
		//create split
		start = lines[line].substring(0, index);
		if(index != lines[line].length()) {
			end = lines[line].substring(index, lines[line].length());
		}
		else {
			end = "";
		}

		//add comma to error line
		if(!start.endsWith(";") && !start.endsWith("{") && !start.endsWith(":")) {
			lines[line] = start + ";" + end;
		}
		
		code = "";
		for(String l : lines) {
			code = code + l + "\n";
		}
		
		return code;
	}
	
	/* Finds end of a line of code, ignoring comments
	 * Will find end of line as space before trailing comment
	 * Only handles // so far
	 */
	private static int findEnd(String line) {
		Integer index = -1;
		char previous;
		boolean quote = false;
		boolean dquote = false;
		char c = ' ';
		
		//parse by char
		for(int i=0; i<line.length(); i++) {
			previous = c;
			c = line.charAt(i);
			
			//if in quotes
			if(c == '\'' && previous != '\\') {
				if(quote) quote = false;
				else if(!quote) quote = true;
			}
			//if in double quotes
			if(c == '\"' && previous != '\\') {
				if(dquote) dquote = false;
				else if(!dquote) dquote = true;
			}
			
			//found comment
			if(c == '/' && previous == '/' && quote ==  false && dquote == false && index == -1) {
				index = i-1;
				if(index != 0 && line.charAt(index-1) == ' ') {
					index = index-1;
				}
			}
			
		}
		
		//if found no comments
		if(index == -1) {
			index = line.length();
		}
		
		return index;
	}
}