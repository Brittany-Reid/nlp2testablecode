package nlp2testablecode.fixer;

import nlp2testablecode.DocHandler;
import nlp2testablecode.Evaluator;
import nlp2testablecode.code.Snippet;
import nlp2testablecode.compiler.IMCompiler;

public class Deleter {
	private static Boolean order = true;
	private static Boolean neutrality = true;
	private static Boolean loop = true;
	public static IMCompiler compiler = null;
	
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
		compiler = Evaluator.compiler;
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
		if(compiler == null) initializeCompiler();
		
		Snippet best = new Snippet(snippet);
		Snippet current;
		while(done == false) {
			//default end condition
			done = true;
			
			int line = startLine;
			//iterate list of lines
			for(int j=0; j<snippet.size(); j++) {
				
				//make sure we havent already deleted this line and that its not empty or a comment
				if(!best.isDeleted(line) && !best.getLine(line).equals("") && !best.getLine(line).trim().startsWith("//")) {
					//get copy of best
					current = new Snippet(best);
					
					//delete line
					current.deleteLine(line);
					
					//if code is empty we know it has 0 errors
					if(current.getCode().trim().equals("")) {
						errors = -1;
					}
					else {
						//compile
						compiler.clearSaved();
						compiler.addSource(DocHandler.getFileName(), Snippet.insert(current, before+after, before.length()));
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
						
						//if we reduced errors to 0 (or empty snippet), break from loop
						if(best.getErrors() <= 0) break;
						
						//try another loop only on improvement
						if(loop == true) done = false;
					}
					
					//increment line
					line += i;
				}
			}
		}
		
		return best;
	}
	
	private static void initializeCompiler() {
		if(compiler != null) return;
		if(Evaluator.compiler == null) Evaluator.compiler = Evaluator.initializeCompiler(false);
		compiler = Evaluator.compiler;
	}
}
