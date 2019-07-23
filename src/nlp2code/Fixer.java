package nlp2code;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

/**
 * 	class Fixer
 *  Handles error fix functionality
 */

class Fixer{
	public static Integer beforeLines; //Stores lines before code snippet
	
	/* Code fix function
	 * Reads passed diagnostic collector, and uses information within to fix errors.
	*/
	public static String fix(String code, DiagnosticCollector<JavaFileObject> diagnostics, String before) {
		String fixed = code;
		String[] lines;
		
		//get number of lines before
		beforeLines = before.split("\n", -1).length -1;
		
		
		for(Diagnostic<?extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
			String errorCode = diagnostic.getCode();
			//insert to complete
			if(errorCode.equals("1610612976")){
				String message = diagnostic.getMessage(null);
				Integer line = (int)diagnostic.getLineNumber() - beforeLines - 1;
				
				//semi-colon
				if(message.startsWith("Syntax error, insert \";\" to complete ")) {
					fixed = semiColon(fixed, line);
				}
			}
		}
		
		return fixed;
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