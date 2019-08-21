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
	 * Deletion algorithm, attempts to reduce compiler errors.
	 */
	public static Snippet deletion(Snippet snippet, String before, String after) {
		boolean done = false;
		boolean accept;
		int startLine = 1;
		int i = 1;
		int errors;
		
		//if reverse order
		if(order == true) {
			startLine = snippet.size();
			i = -1;
		}
		
		//configure compiler
		if(compiler == null) compiler = Evaluator.compiler;
		//disable logging
		IMCompiler.logging = false;
		
		Snippet best = new Snippet(snippet);
		Snippet current;
		int line = startLine;
		while(done == false) {
			//default end condition
			done = true;
			
			//iterate list of lines
			for(int j=0; j<snippet.size(); j++) {
				
				//make sure we havent already deleted this line and that its not empty or a comment
				if(!best.getDeleted(line) && !best.getLine(line).equals("") && !best.getLine(line).trim().startsWith("//")) {
					//get copy of best
					current = new Snippet(best);
					
					//delete line
					current.deleteLine(line);
					
					//if code is empty we know it has 0 errors
					if(current.getCode() == "") {
						errors = 0;
					}
					else {
						//compile
						compiler.clearSaved();
						compiler.addSource(Evaluator.className, before+current.getCode()+after);
						compiler.compileAll();
					}
					
					//test errors
					errors = compiler.getErrors();
					accept = false;
					
					//acceptance scheme 1: strict improvement
					if(errors < best.getErrors() && neutrality == false) {
						accept = true;
					}
					//scheme 2: neutrality
					if(errors <= best.getErrors() && neutrality == true) {
						accept = true;
					}
					
					//accept?
					if(accept) {
						current.updateErrors(errors, compiler.getDiagnostics().getDiagnostics());
						best = new Snippet(current);		
						
						//if we reduced errors to 0, break from loop
						if(best.getErrors() == 0) break;
						
						//try another loop only on improvement
						if(loop == true) done = false;
					}
					
					//increment line
					line += i;
				}
			}
		}
		
		//reenable logging
		IMCompiler.logging = true;
		
		return best;
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
	 * Returns last fix error count.
	 */
	public static Integer getLastFixErrorCount() {
		return newErrorCount;
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