package nlp3code.fixer;


import java.util.ArrayList;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import org.eclipse.jdt.core.compiler.IProblem;

import nlp3code.DocHandler;
import nlp3code.Evaluator;
import nlp3code.code.Snippet;
import nlp3code.compiler.IMCompiler;

public class Fixer {
	private static Boolean order = false;
	private static Boolean neutrality = false;
	private static Boolean loop = false;
	public static Integer beforeLines; //Stores lines before code snippet
	public static IMCompiler compiler;
	public static int offset;
	public static int length;

	/**
	 * Using error information, attempts to fix errors.
	 * @param snippet The snippet to try fixing.
	 * @param before Code from the user's file before the snippet.
	 * @param after Code from the user's file after the snippet.
	 * @return The fixed Snippet.
	 */
	public static Snippet errorFixes(Snippet snippet, String before, String after) {
		//configure compiler
		if(compiler == null) compiler = Evaluator.compiler;
		IMCompiler.logging = false;
		
		int errors = snippet.getErrors();
		offset = before.length();
		length = snippet.getCode().length();
		
		//get the initial list of errors
		List<Diagnostic<? extends JavaFileObject>> diagnostics = snippet.getDiagnostics();
		//get the first error
		int num = 0;
		Diagnostic<? extends JavaFileObject> diagnostic = diagnostics.get(num);
		Snippet modified = null;		
		
		//cache the import statements so we only reconstruct before if this has changed
		List<String> importCache = new ArrayList<>(snippet.getImportList());
		//previous error cache
		List<Diagnostic<? extends JavaFileObject> > processedErrors = new ArrayList<>();
		Diagnostic<? extends JavaFileObject> previousError = null;
		int steps = errors;
		
		//we attempt to fix each error once
		for(int i=0; i<steps; i++) {
//			System.out.println("STEP " + i + ", ERROR: ");
//			System.out.println(diagnostic.getMessage(null));
//			System.out.println(diagnostic.getCode());
			
			//create a copy of the snippet
			Snippet current = new Snippet(snippet);
			
			//handle the error
			modified = handleError(current, diagnostic, before, after);
			//if we couldn't make a change
			if(modified == null) {
				//add to processed
				processedErrors.add(diagnostic);
				
				//get next error
				num++;
				if(num >= diagnostics.size()) break;
				diagnostic = diagnostics.get(num);
			}
			//if we did make a change
			else {
				String proposedBefore = before;
				//if there are imports and they changed
				if(modified.getImportList().size() > 0 && !importCache.equals(modified.getImportList())) {
					importCache = new ArrayList<>(modified.getImportList());
				}
				
				//compile
				compiler.clearSaved();
				compiler.addSource(DocHandler.getFileName(), snippet.insert(modified, before+after, before.length()));
				compiler.compileAll();
				modified.updateErrors(compiler.getErrors(), compiler.getDiagnostics().getDiagnostics());
				int testErrors = compiler.getErrors();
				
				//if fix improved our snippet, confirm changes
				if(testErrors < errors) {
					
					before = proposedBefore;
					offset = before.length();
					length = snippet.getCode().length();
					
					//copy modified back to snippet
					snippet = new Snippet(modified);
					diagnostics = snippet.getDiagnostics();
					
					//if we reached 0, we're done
					if(testErrors == 0) {
						break;
					}
					
					//we can resolve multiple errors, so just incrementing or retaining num means we can skip errors
					if(errors - testErrors > 1 && processedErrors.size() > 0) {
						num = 0;
						int s = 0;
						//for each diagnostic
						for(int j=0; j<diagnostics.size(); j++) {
							//does it match a processed?
							for(int k=s; k<processedErrors.size(); k++) {
								if(diagnostics.get(j).equals(processedErrors.get(k))){
									s=k; //dont look at matching processed errors again
									num = j + 1;
								}
							}
						}
					}
					
					errors = testErrors;
				}
				
				//get next error
				if(num >= diagnostics.size()) break;
				diagnostic = diagnostics.get(num);
			}
		}
		
		IMCompiler.logging = true;
		return snippet;
	}
	
	/**
	 * Handles an individual error.
	 * @param snippet The snippet to try fixing.
	 * @param diagnostic The error to fix.
	 * @return The modified snippet, or on error, null.
	 */
	public static Snippet handleError(Snippet snippet, Diagnostic<?extends JavaFileObject> diagnostic, String before, String after) {
		//get the error code
		int id = Integer.parseInt(diagnostic.getCode());
		int start = (int) diagnostic.getStartPosition();
		int end = (int) diagnostic.getEndPosition();
		//if the diagnostc is outside our snippet
		if(start < offset || start > offset+length || end > offset+length) {
			return null;
		}
//		String message2 = diagnostic.getMessage(null);
//		System.out.println(message2);
//		System.out.println(id);
//		
		//process the error
		switch(id) {
			case IProblem.ParsingError:
				snippet = ParsingFixes.parsingError(snippet, diagnostic, offset);
//				String message = diagnostic.getMessage(null);
//				System.out.println(message);
				//Syntax error on token "(", ; expected
				break;
			case IProblem.MissingSemiColon:
				snippet = ParsingFixes.missingSemiColon(snippet, diagnostic, offset);
				break;
			case IProblem.ParsingErrorInsertToComplete:
				ParsingFixes.insertToComplete(snippet, diagnostic, offset);
				//String message = diagnostic.getMessage(null);
//				if(message.startsWith("Syntax error, insert \";\" to complete ")) {
//					snippet = ParsingFixes.missingSemiColon(snippet, diagnostic, offset);
//				}
				break;
			case IProblem.UndefinedType:
				snippet = UnresolvedElementFixes.fixUnresolvedType(snippet, diagnostic, offset, before, after);
				break;
			case IProblem.UnresolvedVariable:
				snippet = UnresolvedElementFixes.fixUnresolvedVariable(snippet, diagnostic, offset, before, after);
				break;
			case IProblem.UndefinedName:
				snippet = UnresolvedElementFixes.fixUnresolved(snippet, diagnostic, offset, before, after);
				break;
			case IProblem.ParsingErrorDeleteToken:
				snippet = ParsingFixes.deleteToken(snippet, diagnostic, offset);
				break;
//			case IProblem.ParsingErrorInsertTokenAfter:
//				snippet = ParsingFixes.insertAfter(snippet, diagnostic, offset);
//				break;
			default:
				return null;
		}
		return snippet;
	}
	
	public static boolean isInRange(Snippet snippet, Diagnostic<?extends JavaFileObject> diagnostic, String before, String after) {
		String code = Snippet.insert(snippet, before+after, before.length());
		long line = diagnostic.getLineNumber();
		long column = diagnostic.getColumnNumber();
		
		String[] lines = code.split("\n");
		String toFind = "";
		for(int i= (int)line-1; i<lines.length; i++) {
			toFind += lines[i] + "\n";
		}
		
		int offset = (int) (code.indexOf(toFind) + line);
		if(offset > before.length() && offset < (before+snippet.getCode()).length()) return true;
		return false;
	}
	
	/**
	 * Skeleton: This function will return the sub-string covered by the diagnostic.
	 * @return
	 */
	public static String getCovered(String code, long start, long end, int offset) {
		String covered;
		
		//update start and end with offset
		start = start - offset;
		end = end - offset;
		if(start < 0) return null;
		//sometimes the compiler fails parsing spectacularly, the offset will be weird
		//but in general if the error is not inside our snippet, ignore it
		if(end >= code.length()) return null;
		
		covered = code.substring((int)start, (int)end+1);
		
		return covered;
	}
	
	/**
	 * Inserts a given String within another String.
	 * @param code The String to modify.
	 * @param toInsert The string to insert.
	 * @param start The start position from a diagnostic.
	 * @param end The end position from a diagnostic.
	 * @param offset Offset to account for beginning code
	 * @return The modified snippet.
	 */
	public static String insertAt(String code, String toInsert, long start, long end, int offset) {
		String modified = null;
		
		//update start and end with offset
		start = start - offset;
		end = end - offset;
		if(start < 0) return null;
		//sometimes the compiler fails parsing spectacularly, the offset will be weird
		//but in general if the error is not inside our snippet, ignore it
		if(end >= code.length()) return null;
		
		modified = code.substring(0, (int)start+1);
		modified += toInsert;
		modified += code.substring((int)end+1, code.length());
		
		return modified;
	}
	
	public static String deleteAt(String code, long start, long end, int offset) {
		String modified = null;
		
		//update start and end with offset
		start = start - offset;
		end = end - offset;
		if(start < 0) return null;
		//sometimes the compiler fails parsing spectacularly, the offset will be weird
		//but in general if the error is not inside our snippet, ignore it
		if(end >= code.length()) return null;
		
		modified = code.substring(0, (int)start);
//		System.out.println(modified);
//		System.out.println("[" + code.substring((int)start, (int)end+1) + "]");
		modified += code.substring((int)end+1, code.length());
		
		return modified;
	}
	
	/**
	 * Adds a line of code at the given line number. Unlike insertAt, this
	 * function uses a line-based representation of code.
	 */
	public static String addLineAt(String code, String toInsert, int line) {
		
		//toInsert must end with a newline
		if(!toInsert.endsWith("\n")) toInsert += "\n";
		
		//get our lines
		String[] lines = code.split("\n");
		
		//reconstruct our code
		String modified = "";
		for(int i=0; i<lines.length; i++) {
			if(i+1 == line) {
				modified += toInsert;
			}
			modified += lines[i] + "\n";
		}
		
		return modified;
	}
}