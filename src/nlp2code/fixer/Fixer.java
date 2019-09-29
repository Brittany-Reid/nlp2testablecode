package nlp2code.fixer;

import java.util.ArrayList;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import org.eclipse.jdt.core.compiler.IProblem;

import nlp2code.Evaluator;
import nlp2code.Snippet;
import nlp2code.compiler.IMCompiler;

public class Fixer {
	private static Boolean order = false;
	private static Boolean neutrality = false;
	private static Boolean loop = false;
	public static Integer beforeLines; //Stores lines before code snippet
	public static IMCompiler compiler;
	public static int offset;
	public static int length;
	
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
	 * Integrates a snippet depending on existing code.
	 */
	public static Snippet integrate(Snippet snippet, String before, String after) {
		//configure compiler
		if(compiler == null) compiler = Evaluator.compiler;
		IMCompiler.logging = false;
				
		//get the context
		int context = Integrator.getType(snippet);
		Snippet copy = new Snippet(snippet);
		
		//if just a snippet, make no changes
		if(context == Integrator.SNIPPET) return snippet;
		
		//handle method
		if(context == Integrator.METHOD) {
			copy = Integrator.integrateMethod(copy, before, after);
		}
		//handle class
		else if(context == Integrator.CLASS) {
			copy = Integrator.integrateClass(copy, before, after);
		}
		else {
			copy = null;
		}
		
		//compile
		if(copy != null) {
			int errors = snippet.getErrors();
			//compile
			compiler.clearSaved();
			compiler.addSource(Evaluator.className, before+copy.getCode()+after);
			//System.out.println(proposedBefore+modified.getCode()+after);
			compiler.compileAll();
			int testErrors = compiler.getErrors();
			if(testErrors < errors) {
				copy.updateErrors(testErrors, compiler.getDiagnostics().getDiagnostics());
				snippet = copy;
			}
			else {
				//System.out.println(snippet.getCode());
			}
		}

		
		IMCompiler.logging = true;
		return snippet;
	}
	
	/**
	 * Deletion algorithm, attempts to reduce compiler errors.
	 * @param snippet The snippet to modify.
	 * @param before Code from the user's file before the snippet.
	 * @param after Code from the user's file after the snippet.
	 * @return A modified snippet.
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
						errors = compiler.getErrors();
					}
				
					//test errors
					accept = false;
					
					//acceptance scheme 1: strict improvement
					if(errors < best.getErrors() && neutrality == false) {
						accept = true;
					}
					//scheme 2: neutrality
					else if(errors <= best.getErrors() && neutrality == true) {
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
					proposedBefore = Snippet.addImportToBefore(modified, before);
					importCache = new ArrayList<>(modified.getImportList());
				}
				
				//compile
				compiler.clearSaved();
				compiler.addSource(Evaluator.className, proposedBefore+modified.getCode()+after);
				//System.out.println(proposedBefore+modified.getCode()+after);
				compiler.compileAll();
				int testErrors = compiler.getErrors();
				
				//if fix improved our snippet, confirm changes
				if(testErrors < errors) {
					
					before = proposedBefore;
					offset = before.length();
					length = snippet.getCode().length();
					
					//copy modified back to snippet
					snippet = new Snippet(modified);
					snippet.updateErrors(testErrors, compiler.getDiagnostics().getDiagnostics());
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
