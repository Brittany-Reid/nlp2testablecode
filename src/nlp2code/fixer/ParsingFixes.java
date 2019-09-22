package nlp2code.fixer;

import java.util.ArrayList;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import nlp2code.Snippet;

/**
 * This class contains code fixes for parsing errors.
 */
public class ParsingFixes {
	
	/**
	 * Handles the insert to complete error. Returns null on error.
	 * @param snippet The snippet to fix.
	 * @param diagnostic The diagnostic containing error information.
	 * @param offset The offset value calculated from surrounding code.
	 * @return A fixed snippet, or on error, null.
	 */
	public static Snippet insertToComplete(Snippet snippet, Diagnostic<? extends JavaFileObject> diagnostic, int offset) {
		
		//System.out.println(diagnostic.getMessage(null));
		
		
		//get the string to insert
		List<String> arguments = getArguments(diagnostic.getMessage(null));
		if(arguments == null || arguments.size() < 1) return snippet;
		String insertion = arguments.get(0);
		
		//insert the string
		String code = Fixer.insertAt(snippet.getCode(), insertion, diagnostic.getStartPosition(), diagnostic.getEndPosition(), offset);
		if(code == null) return null;
		snippet.setCode(code);

		
		return snippet;
	}
	
	/**
	 * Deletes the given token between start and end.
	 */
	public static Snippet deleteToken(Snippet snippet, Diagnostic<? extends JavaFileObject> diagnostic, int offset) {
		
		String code = Fixer.deleteAt(snippet.getCode(), diagnostic.getStartPosition(), diagnostic.getEndPosition(), offset);
		if(code == null) return null;
		snippet.setCode(code);
		
		return snippet;
	}
	
	public static Snippet insertAfter(Snippet snippet, Diagnostic<? extends JavaFileObject> diagnostic, int offset) {
		List<String> arguments = getArguments(diagnostic.getMessage(null));
		if(arguments == null || arguments.size() < 1) return snippet;
		String insertion = arguments.get(0);
		
		//insert the string
		String code = Fixer.insertAt(snippet.getCode(), insertion, diagnostic.getStartPosition(), diagnostic.getEndPosition(), offset);
		if(code == null) return null;
		snippet.setCode(code);
		
		return null;
	}
	
	
	/**
	 * Extracts arguments based on the given error message.
	 * @param message The message to extract from.
	 * @return
	 */
	public static List<String> getArguments(String message) {
		List<String> arguments = new ArrayList<>();
		int start, end;
		String argument;
		
		//for insert at, get the insertion
		if(message.startsWith("Syntax error, insert ")){
			start = message.indexOf('\"') + 1;
			end = message.indexOf("\" to complete");
			argument = message.substring(start, end);
			arguments.add(argument);
		}
		//for insert after
		if(message.startsWith("Syntax error on token ") && message.endsWith("expected after this token")) {
			start = message.indexOf("\"") + 1; //first quote ["] in [")", ]
			message = message.substring(start);
			start = message.indexOf("\"") + 3; //second and trailing [", ]
			end = message.indexOf(" expected after this token");
			argument = message.substring(start, end);
			arguments.add(argument);
		}
		
		return arguments;
	}
	
	
	/**
	 * Handles missing semi-colon fix. Returns null on error.
	 * @param snippet The snippet to fix.
	 * @param diagnostic The diagnostic containing error information.
	 * @param offset The offset value calculated from surrounding code.
	 * @return A fixed snippet, or on error, null.
	 */
	public static Snippet missingSemiColon(Snippet snippet, Diagnostic<? extends JavaFileObject> diagnostic, int offset) {
		String code = Fixer.insertAt(snippet.getCode(), ";", diagnostic.getStartPosition(), diagnostic.getEndPosition(), offset);
		if(code == null) return null;
		
		snippet.setCode(code);
		
		return snippet;
	}
}
